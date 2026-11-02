package studio.anatomy.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import studio.anatomy.app.data.AnatomyPrefs
import studio.anatomy.app.ui.theme.LocalAnatomyColors

/**
 * AdMob configuration. The ids below are Google's official test units, so the app shows
 * test creatives and never risks a policy strike while the production account is not ready.
 *
 * To go live: replace the three unit ids, replace APPLICATION_ID in AndroidManifest.xml,
 * and set USE_TEST_ADS to false. Never click your own production ads.
 */
object AdConfig {
    const val USE_TEST_ADS = true
    const val AD_FREE_HOURS = 12

    val BANNER_UNIT = if (USE_TEST_ADS) {
        "ca-app-pub-3940256099942544/9214589741"
    } else {
        "ca-app-pub-0000000000000000/0000000000"
    }

    val INTERSTITIAL_UNIT = if (USE_TEST_ADS) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-0000000000000000/0000000000"
    }

    val REWARDED_UNIT = if (USE_TEST_ADS) {
        "ca-app-pub-3940256099942544/5224354917"
    } else {
        "ca-app-pub-0000000000000000/0000000000"
    }
}

object Ads {
    private const val TAG = "AnatomyAds"
    private var initialized = false

    var adFreeUntil: Long = 0L
    var studioExits: Int = 0

    /** Set once the remove-ads purchase is owned; survives restarts via AnatomyPrefs. */
    var purchased: Boolean = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) {
            Log.d(TAG, "Mobile Ads initialized")
        }
        InterstitialAds.preload(context.applicationContext)
        RewardedAds.preload(context.applicationContext)
    }

    fun isAdFree(): Boolean = purchased || System.currentTimeMillis() < adFreeUntil

    fun grantAdFree(prefs: AnatomyPrefs): Long {
        val until = System.currentTimeMillis() + AdConfig.AD_FREE_HOURS * 60L * 60L * 1000L
        prefs.adFreeUntil = until
        adFreeUntil = until
        return until
    }
}

/**
 * Anchored adaptive banner. It occupies no space until a creative is actually loaded,
 * so an offline launch never leaves a blank strip on screen.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    onVisibleChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val colors = LocalAnatomyColors.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var loaded by remember { mutableStateOf(false) }
    var adHeight by remember { mutableStateOf(50) }
    val adView = remember { AdView(context) }

    DisposableEffect(adView) {
        onDispose {
            onVisibleChange(false)
            adView.destroy()
        }
    }

    LaunchedEffect(loaded) {
        onVisibleChange(loaded)
    }

    LaunchedEffect(widthDp) {
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            widthDp.coerceAtLeast(320),
        )
        if (size != null) {
            adView.setAdSize(size)
            adHeight = size.height
        }
        adView.adUnitId = AdConfig.BANNER_UNIT
        adView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                loaded = true
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                loaded = false
                Log.w(TAG, "Banner failed: ${adError.message}")
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    if (!loaded) return

    Box(
        modifier
            .fillMaxWidth()
            .height(adHeight.dp)
            .background(colors.ink),
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(adHeight.dp),
        )
    }
}

private const val TAG = "AnatomyAds"

object InterstitialAds {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L

    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loading = false
                    ad = interstitialAd
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loading = false
                    ad = null
                    Log.w(TAG, "Interstitial failed: ${adError.message}")
                }
            },
        )
    }

    /** Shows at most every third studio exit and never within 45 seconds of the last one. */
    fun maybeShow(activity: Activity, context: Context) {
        if (Ads.isAdFree()) return
        Ads.studioExits += 1
        if (Ads.studioExits % 3 != 0) return
        if (System.currentTimeMillis() - lastShownAt < 45_000L) return
        val current = ad ?: return

        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload(context)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                preload(context)
            }
        }
        current.show(activity)
        ad = null
        lastShownAt = System.currentTimeMillis()
        preload(context)
    }
}

object RewardedAds {
    private var ad: RewardedAd? = null
    private var loading = false

    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(
            context,
            AdConfig.REWARDED_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    loading = false
                    ad = rewardedAd
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loading = false
                    ad = null
                    Log.w(TAG, "Rewarded failed: ${adError.message}")
                }
            },
        )
    }

    fun show(
        activity: Activity,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
    ) {
        val current = ad
        if (current == null) {
            preload(activity.applicationContext)
            onUnavailable()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                preload(activity.applicationContext)
                onUnavailable()
            }
        }
        current.show(activity) {
            onReward()
        }
        ad = null
        preload(activity.applicationContext)
    }
}
