package studio.anatomy.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Observable mirror of [AnatomyPrefs] for the studio controls. Screens read these
 * snapshot-backed values so a settings change applies immediately, not on the next launch.
 */
class StudioSettings(private val prefs: AnatomyPrefs) {
    var renderQuality by mutableStateOf(prefs.renderQuality)
        private set
    var frameRate by mutableIntStateOf(prefs.frameRate)
        private set
    var motion by mutableStateOf(prefs.motion)
        private set
    var haptics by mutableStateOf(prefs.haptics)
        private set
    var gridDefault by mutableStateOf(prefs.gridDefault)
        private set
    var pinsDefault by mutableStateOf(prefs.pinsDefault)
        private set
    var keepAwake by mutableStateOf(prefs.keepAwake)
        private set
    var autoplay by mutableStateOf(prefs.autoplay)
        private set

    fun updateRenderQuality(value: String) {
        renderQuality = value
        prefs.renderQuality = value
    }

    fun updateFrameRate(value: Int) {
        frameRate = value.coerceIn(30, 60)
        prefs.frameRate = frameRate
    }

    fun updateMotion(value: String) {
        motion = value
        prefs.motion = value
    }

    fun updateHaptics(value: Boolean) {
        haptics = value
        prefs.haptics = value
    }

    fun updateGridDefault(value: Boolean) {
        gridDefault = value
        prefs.gridDefault = value
    }

    fun updatePinsDefault(value: Boolean) {
        pinsDefault = value
        prefs.pinsDefault = value
    }

    fun updateKeepAwake(value: Boolean) {
        keepAwake = value
        prefs.keepAwake = value
    }

    fun updateAutoplay(value: Boolean) {
        autoplay = value
        prefs.autoplay = value
    }

    fun reset() {
        prefs.resetStudioSettings()
        renderQuality = prefs.renderQuality
        frameRate = prefs.frameRate
        motion = prefs.motion
        haptics = prefs.haptics
        gridDefault = prefs.gridDefault
        pinsDefault = prefs.pinsDefault
        keepAwake = prefs.keepAwake
        autoplay = prefs.autoplay
    }
}

val LocalStudioSettings = staticCompositionLocalOf<StudioSettings?> { null }
