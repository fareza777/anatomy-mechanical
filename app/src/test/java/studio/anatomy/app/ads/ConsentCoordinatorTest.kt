package studio.anatomy.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Consent gathering follows Google's UMP pattern:
 * requestConsentInfoUpdate → loadAndShowConsentFormIfRequired, then
 * initialize ads only when canRequestAds is true. Previous-session consent
 * may already allow ads immediately after the update request.
 */
class ConsentCoordinatorTest {
    @Test
    fun `gatherConsent requests update then loads the form`() {
        val client = FakeConsentClient()
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = {})

        coordinator.gatherConsent()

        assertEquals(1, client.requestCalls)
        assertEquals(1, client.formCalls)
    }

    @Test
    fun `does not initialize ads when canRequestAds is false`() {
        val client = FakeConsentClient(canRequestAds = false)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()

        assertEquals(0, adsInits)
    }

    @Test
    fun `initializes ads from previous-session consent after requestConsentInfoUpdate`() {
        val client = FakeConsentClient(canRequestAds = true, autoCompleteForm = false)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()

        assertEquals(1, adsInits)
        assertEquals(1, client.formCalls)
        client.completeForm()
        assertEquals(1, adsInits)
    }

    @Test
    fun `initializes ads after the form when consent is obtained this session`() {
        val client = FakeConsentClient(canRequestAds = false, autoCompleteForm = false)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()
        assertEquals(0, adsInits)

        client.canRequestAds = true
        client.completeForm()

        assertEquals(1, adsInits)
    }

    @Test
    fun `does not initialize ads twice when both consent checks return true`() {
        val client = FakeConsentClient(canRequestAds = true)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()

        assertEquals(1, adsInits)
    }

    @Test
    fun `does not initialize ads when remove_ads is owned`() {
        val client = FakeConsentClient(canRequestAds = true)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { true }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()

        assertEquals(0, adsInits)
    }

    @Test
    fun `on request failure still initializes ads if previous session allows`() {
        val client = FakeConsentClient(canRequestAds = true, requestError = "network")
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()

        assertEquals(1, adsInits)
        assertEquals(0, client.formCalls)
    }

    @Test
    fun `privacy options required is true only when UMP status is REQUIRED`() {
        val required = FakeConsentClient(privacyOptionsRequired = true)
        val coordinatorRequired = ConsentCoordinator(required, isPurchased = { false }, initializeAds = {})
        coordinatorRequired.gatherConsent()
        assertTrue(coordinatorRequired.privacyOptionsRequired)

        val notRequired = FakeConsentClient(privacyOptionsRequired = false)
        val coordinatorHidden = ConsentCoordinator(notRequired, isPurchased = { false }, initializeAds = {})
        coordinatorHidden.gatherConsent()
        assertFalse(coordinatorHidden.privacyOptionsRequired)
    }

    @Test
    fun `showPrivacyOptions delegates to the UMP form`() {
        val client = FakeConsentClient()
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = {})
        var dismissed = false

        coordinator.showPrivacyOptions { dismissed = true }

        assertEquals(1, client.privacyFormCalls)
        assertTrue(dismissed)
    }

    @Test
    fun `privacy options grant this session initializes ads once`() {
        val client = FakeConsentClient(canRequestAds = false)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = { adsInits++ })

        coordinator.gatherConsent()
        assertEquals(0, adsInits)

        client.canRequestAds = true
        coordinator.showPrivacyOptions {}

        assertEquals(1, adsInits)

        coordinator.showPrivacyOptions {}
        assertEquals(1, adsInits)
    }

    @Test
    fun `privacy options grant does not initialize ads when remove_ads is owned`() {
        val client = FakeConsentClient(canRequestAds = true)
        var adsInits = 0
        val coordinator = ConsentCoordinator(client, isPurchased = { true }, initializeAds = { adsInits++ })

        coordinator.showPrivacyOptions {}

        assertEquals(0, adsInits)
    }

    @Test
    fun `privacy options dismiss refreshes whether the settings entry is required`() {
        val client = FakeConsentClient(privacyOptionsRequired = true)
        val coordinator = ConsentCoordinator(client, isPurchased = { false }, initializeAds = {})
        coordinator.gatherConsent()
        assertTrue(coordinator.privacyOptionsRequired)

        client.privacyOptionsRequired = false
        coordinator.showPrivacyOptions {}

        assertFalse(coordinator.privacyOptionsRequired)
    }
}

internal class FakeConsentClient(
    override var canRequestAds: Boolean = false,
    override var privacyOptionsRequired: Boolean = false,
    val requestError: String? = null,
    val autoCompleteForm: Boolean = true,
) : ConsentClient {
    var requestCalls = 0
    var formCalls = 0
    var privacyFormCalls = 0
    private var pendingForm: ((String?) -> Unit)? = null

    override fun requestConsentInfoUpdate(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        requestCalls++
        val error = requestError
        if (error != null) onFailure(error) else onSuccess()
    }

    override fun loadAndShowConsentFormIfRequired(onComplete: (String?) -> Unit) {
        formCalls++
        if (autoCompleteForm) {
            onComplete(null)
        } else {
            pendingForm = onComplete
        }
    }

    override fun showPrivacyOptionsForm(onDismiss: (String?) -> Unit) {
        privacyFormCalls++
        onDismiss(null)
    }

    fun completeForm(error: String? = null) {
        val callback = pendingForm
        pendingForm = null
        requireNotNull(callback) { "no pending consent form" }.invoke(error)
    }
}
