package studio.anatomy.app

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import studio.anatomy.app.ads.AdBanner
import studio.anatomy.app.ads.Ads
import studio.anatomy.app.ads.InterstitialAds
import studio.anatomy.app.ads.RewardedAds
import studio.anatomy.app.billing.RemoveAds
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.data.AnatomyPrefs
import studio.anatomy.app.data.LocalStudioSettings
import studio.anatomy.app.data.StudioSettings
import studio.anatomy.app.ui.AiLabScreen
import studio.anatomy.app.ui.CatalogScreen
import studio.anatomy.app.ui.SystemSheet
import studio.anatomy.app.ui.studio.StudioScreen
import studio.anatomy.app.ui.theme.AnatomyTheme
import studio.anatomy.app.ui.theme.LocalAnatomyColors

@Composable
fun AnatomyApp() {
    val context = LocalContext.current
    val prefs = remember { AnatomyPrefs(context) }
    remember { Ads.adFreeUntil = prefs.adFreeUntil }
    val settings = remember { StudioSettings(prefs) }
    val systemDark = isSystemInDarkTheme()
    var appearance by remember { mutableStateOf(prefs.appearance) }
    var textScale by remember { mutableFloatStateOf(prefs.textScale) }
    val darkTheme = when (appearance) {
        AnatomyPrefs.APPEARANCE_DARK -> true
        AnatomyPrefs.APPEARANCE_SYSTEM -> systemDark
        else -> false
    }

    CompositionLocalProvider(LocalStudioSettings provides settings) {
        AnatomyTheme(darkTheme = darkTheme, textScale = textScale) {
            SystemBars(darkTheme)
            AnatomyShell(
                prefs = prefs,
                appearance = appearance,
                textScale = textScale,
                onAppearance = {
                    appearance = it
                    prefs.appearance = it
                },
                onTextScale = {
                    textScale = it
                    prefs.textScale = it
                },
            )
        }
    }
}

@Composable
private fun SystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val colors = LocalAnatomyColors.current
    val window = (view.context as? Activity)?.window
    SideEffect {
        if (window != null) {
            // MIUI keeps a white three-button bar unless the window supplies its own color.
            window.navigationBarColor = colors.ink.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val lightNav = android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                val flags = window.decorView.systemUiVisibility
                window.decorView.systemUiVisibility = if (darkTheme) flags and lightNav.inv() else flags or lightNav
            }
        }
    }
}

@Composable
private fun AnatomyShell(
    prefs: AnatomyPrefs,
    appearance: String,
    textScale: Float,
    onAppearance: (String) -> Unit,
    onTextScale: (Float) -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val context = LocalContext.current
    val specs = remember { AnatomyCatalog.specs }
    var favorites by remember { mutableStateOf(prefs.favorites()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var aiOpen by rememberSaveable { mutableStateOf(false) }
    var systemOpen by rememberSaveable { mutableStateOf(false) }
    var adFreeUntil by remember { mutableStateOf(prefs.adFreeUntil) }
    var bannerVisible by remember { mutableStateOf(false) }
    val purchased = RemoveAds.owned

    val selected = AnatomyCatalog.byId(selectedId)
    val route = when {
        selected != null -> "studio:${selected.id}"
        aiOpen -> "ai"
        else -> "catalog"
    }

    Column(Modifier.fillMaxSize().background(colors.ink)) {
        Box(
            Modifier
                .weight(1f)
                .then(
                    if (bannerVisible) {
                        Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                    } else {
                        Modifier
                    }
                ),
        ) {
            Crossfade(
                targetState = route,
                animationSpec = tween(240),
                label = "anatomy-route",
            ) { key ->
            when {
                key.startsWith("studio:") -> {
                    val spec = AnatomyCatalog.byId(key.removePrefix("studio:")) ?: specs.firstOrNull()
                    if (spec != null) {
                        StudioScreen(
                            spec = spec,
                            isFavorite = spec.id in favorites,
                            onBack = {
                                selectedId = null
                                val host = context as? Activity
                                if (host != null) {
                                    InterstitialAds.maybeShow(host, context.applicationContext)
                                }
                            },
                            onToggleFavorite = { favorites = prefs.toggleFavorite(spec.id) },
                        )
                    }
                }

                key == "ai" -> AiLabScreen(
                    onBack = { aiOpen = false },
                    onOpenSystem = {
                        aiOpen = false
                        systemOpen = true
                    },
                )

                else -> CatalogScreen(
                    specs = specs,
                    favorites = favorites,
                    onOpen = { id ->
                        selectedId = id
                        prefs.lastObjectId = id
                        aiOpen = false
                    },
                    onToggleFavorite = { id -> favorites = prefs.toggleFavorite(id) },
                    onOpenAI = {
                        aiOpen = true
                        selectedId = null
                    },
                    onOpenSystem = { systemOpen = true },
                )
            }
            }
        }
        if (!purchased && adFreeUntil <= System.currentTimeMillis()) {
            AdBanner(
                modifier = Modifier.navigationBarsPadding(),
                onVisibleChange = { bannerVisible = it },
            )
        }
    }

    if (systemOpen) {
        SystemSheet(
            appearance = appearance,
            textScale = textScale,
            favoritesCount = favorites.size,
            onAppearance = onAppearance,
            onTextScale = onTextScale,
            onClearFavorites = {
                prefs.clearFavorites()
                favorites = emptySet()
            },
            onDismiss = { systemOpen = false },
            adFreeUntil = adFreeUntil,
            onRewardAd = {
                val host = context as? Activity
                if (host == null) {
                    Toast.makeText(context, "No activity available for the ad.", Toast.LENGTH_SHORT).show()
                } else {
                    RewardedAds.show(
                        activity = host,
                        onReward = {
                            adFreeUntil = Ads.grantAdFree(prefs)
                            systemOpen = false
                            Toast.makeText(context, "Ad-free for 12 hours.", Toast.LENGTH_SHORT).show()
                        },
                        onUnavailable = {
                            Toast.makeText(context, "No ad ready yet. Try again in a moment.", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            },
            removeAdsOwned = purchased,
            removeAdsPrice = RemoveAds.price,
            onRemoveAds = {
                val host = context as? Activity
                if (host == null) {
                    Toast.makeText(context, "Purchases need the app window.", Toast.LENGTH_SHORT).show()
                } else {
                    RemoveAds.purchase(host) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onOpenAI = {
                systemOpen = false
                selectedId = null
                aiOpen = true
            },
        )
    }
}
