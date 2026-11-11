package studio.anatomy.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google UMP consent flow, matching
 * https://developers.google.com/admob/android/privacy
 *
 * On every launch: [ConsentInformation.requestConsentInfoUpdate], then
 * [UserMessagingPlatform.loadAndShowConsentFormIfRequired]. Mobile Ads is
 * initialized only when [ConsentInformation.canRequestAds] is true and the
 * remove-ads purchase is not owned.
 */
object ConsentManager {
    private const val TAG = "AnatomyConsent"

    var privacyOptionsRequired by mutableStateOf(false)
        private set

    /** Live canRequestAds from UMP, updated after gathering and privacy-options dismiss. */
    var canRequestAds by mutableStateOf(false)
        private set

    private var client: UmpConsentClient? = null
    private var coordinator: ConsentCoordinator? = null

    fun gatherConsent(activity: Activity) {
        val coord = bind(activity)
        coord.gatherConsent { error ->
            syncUi(coord)
            if (error != null) {
                Log.w(TAG, "Consent gathering: $error")
            }
        }
        syncUi(coord)
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: (String?) -> Unit = {}) {
        val coord = bind(activity)
        coord.showPrivacyOptions { error ->
            syncUi(coord)
            if (error != null) {
                Log.w(TAG, "Privacy options: $error")
            }
            onDismiss(error)
        }
    }

    private fun bind(activity: Activity): ConsentCoordinator {
        val ump = client ?: UmpConsentClient(activity.applicationContext).also { client = it }
        ump.attach(activity)
        return coordinator ?: ConsentCoordinator(
            client = ump,
            isPurchased = { Ads.purchased },
            initializeAds = {
                // Publish consent before Ads.init preloads, which also check canRequestAds.
                canRequestAds = ump.canRequestAds
                Ads.init(activity.applicationContext)
            },
        ).also { coordinator = it }
    }

    private fun syncUi(coord: ConsentCoordinator) {
        privacyOptionsRequired = coord.privacyOptionsRequired
        canRequestAds = client?.canRequestAds == true
    }
}

internal interface ConsentClient {
    val canRequestAds: Boolean
    val privacyOptionsRequired: Boolean
    fun requestConsentInfoUpdate(onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun loadAndShowConsentFormIfRequired(onComplete: (String?) -> Unit)
    fun showPrivacyOptionsForm(onDismiss: (String?) -> Unit)
}

/**
 * Google's documented sequence, with a second canRequestAds check immediately
 * after requestConsentInfoUpdate so previous-session consent can load ads
 * without waiting for the form callback. Both paths share one init guard.
 */
internal class ConsentCoordinator(
    private val client: ConsentClient,
    private val isPurchased: () -> Boolean,
    private val initializeAds: () -> Unit,
) {
    var privacyOptionsRequired: Boolean = false
        private set
    private val adsInitializeCalled = AtomicBoolean(false)

    fun gatherConsent(onComplete: (String?) -> Unit = {}) {
        client.requestConsentInfoUpdate(
            onSuccess = {
                client.loadAndShowConsentFormIfRequired { formError ->
                    finishGathering(formError, onComplete)
                }
            },
            onFailure = { error ->
                finishGathering(error, onComplete)
            },
        )
        maybeInitializeAds()
    }

    fun showPrivacyOptions(onDismiss: (String?) -> Unit) {
        client.showPrivacyOptionsForm { error ->
            privacyOptionsRequired = client.privacyOptionsRequired
            maybeInitializeAds()
            onDismiss(error)
        }
    }

    private fun finishGathering(error: String?, onComplete: (String?) -> Unit) {
        privacyOptionsRequired = client.privacyOptionsRequired
        maybeInitializeAds()
        onComplete(error)
    }

    private fun maybeInitializeAds() {
        if (isPurchased()) return
        if (!client.canRequestAds) return
        if (!adsInitializeCalled.compareAndSet(false, true)) return
        initializeAds()
    }
}

private class UmpConsentClient(context: Context) : ConsentClient {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)
    private var activityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    private fun host(): Activity? {
        val activity = activityRef?.get() ?: return null
        return activity.takeUnless { it.isFinishing || it.isDestroyed }
    }

    override val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    override val privacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun requestConsentInfoUpdate(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val host = host()
        if (host == null) {
            onFailure("no activity")
            return
        }
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            host,
            params,
            { onSuccess() },
            { error -> onFailure(error.message ?: "consent update failed") },
        )
    }

    override fun loadAndShowConsentFormIfRequired(onComplete: (String?) -> Unit) {
        val host = host()
        if (host == null) {
            onComplete("no activity")
            return
        }
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(host) { error ->
            onComplete(error?.message)
        }
    }

    override fun showPrivacyOptionsForm(onDismiss: (String?) -> Unit) {
        val host = host()
        if (host == null) {
            onDismiss("no activity")
            return
        }
        UserMessagingPlatform.showPrivacyOptionsForm(host) { error ->
            onDismiss(error?.message)
        }
    }
}
