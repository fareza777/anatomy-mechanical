package studio.anatomy.app.core

import kotlin.math.min

class AnimContextFactory {
    var ambient: Float = 0f
        private set
    var machine: Float = 0.72f
        private set
    var control: Float = 0.72f
        private set
    var targetControl: Float = 0.72f
    var lastReport: String = ""
    private var lastNanos: Long = 0L
    private var targetControlSmooth: Float = 0.72f

    fun update(nowNanos: Long, playing: Boolean = true, rate: Float = 1f) {
        if (lastNanos == 0L) {
            lastNanos = nowNanos
            return
        }
        val dt = min((nowNanos - lastNanos) / 1_000_000_000f, 1f / 30f)
        lastNanos = nowNanos
        ambient += dt
        targetControlSmooth += (targetControl - targetControlSmooth) * (1f - kotlin.math.exp(-dt / 0.22f))
        control = targetControlSmooth
        if (playing) {
            machine += dt * (0.35f + control * 1.65f) * rate
        }
    }

    fun resetClock() {
        lastNanos = 0L
    }

    fun snapControl(value: Float) {
        targetControl = value
        targetControlSmooth = value
        control = value
    }
}
