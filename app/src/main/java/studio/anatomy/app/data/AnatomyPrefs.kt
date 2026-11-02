package studio.anatomy.app.data

import android.content.Context

class AnatomyPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("anatomy_studio", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun toggleFavorite(id: String): Set<String> {
        val next = favorites().toMutableSet()
        if (!next.add(id)) next.remove(id)
        prefs.edit().putStringSet(KEY_FAVORITES, next).apply()
        return next
    }

    /** Epoch millis until which the rewarded ad-free pass is valid. */
    var adFreeUntil: Long
        get() = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_AD_FREE_UNTIL, value).apply()

    /** Permanent entitlement bought once through Google Play. */
    var removeAds: Boolean
        get() = prefs.getBoolean(KEY_REMOVE_ADS, false)
        set(value) = prefs.edit().putBoolean(KEY_REMOVE_ADS, value).apply()

    var lastObjectId: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().putString(KEY_LAST, value).apply()

    var aiEndpoint: String
        get() = prefs.getString(KEY_AI_ENDPOINT, DEFAULT_AI_ENDPOINT) ?: DEFAULT_AI_ENDPOINT
        set(value) = prefs.edit().putString(KEY_AI_ENDPOINT, value.trim()).apply()

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value.trim()).apply()

    var aiKey: String
        get() = prefs.getString(KEY_AI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_KEY, value.trim()).apply()

    fun clearAiKey() = prefs.edit().remove(KEY_AI_KEY).apply()

    var hasSeenIntro: Boolean
        get() = prefs.getBoolean(KEY_INTRO, false)
        set(value) = prefs.edit().putBoolean(KEY_INTRO, value).apply()

    var darkTheme: Boolean
        get() = appearance == APPEARANCE_DARK
        set(value) = run { appearance = if (value) APPEARANCE_DARK else APPEARANCE_LIGHT }

    var appearance: String
        get() = prefs.getString(KEY_APPEARANCE, APPEARANCE_LIGHT) ?: APPEARANCE_LIGHT
        set(value) = prefs.edit().putString(KEY_APPEARANCE, value).apply()

    var textScale: Float
        get() = prefs.getFloat(KEY_TEXT_SCALE, DEFAULT_TEXT_SCALE).coerceIn(1.08f, 1.4f)
        set(value) = prefs.edit().putFloat(KEY_TEXT_SCALE, value.coerceIn(1.08f, 1.4f)).apply()

    var renderQuality: String
        get() = prefs.getString(KEY_RENDER_QUALITY, QUALITY_BALANCED) ?: QUALITY_BALANCED
        set(value) = prefs.edit().putString(KEY_RENDER_QUALITY, value).apply()

    var frameRate: Int
        get() = prefs.getInt(KEY_FRAME_RATE, 30).coerceIn(30, 60)
        set(value) = prefs.edit().putInt(KEY_FRAME_RATE, value.coerceIn(30, 60)).apply()

    var motion: String
        get() = prefs.getString(KEY_MOTION, "FULL") ?: "FULL"
        set(value) = prefs.edit().putString(KEY_MOTION, value).apply()

    var haptics: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    var gridDefault: Boolean
        get() = prefs.getBoolean(KEY_GRID, true)
        set(value) = prefs.edit().putBoolean(KEY_GRID, value).apply()

    var pinsDefault: Boolean
        get() = prefs.getBoolean(KEY_PINS, true)
        set(value) = prefs.edit().putBoolean(KEY_PINS, value).apply()

    var keepAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_AWAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_AWAKE, value).apply()

    var autoplay: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()

    fun clearFavorites() = prefs.edit().remove(KEY_FAVORITES).apply()

    fun resetStudioSettings() {
        prefs.edit()
            .remove(KEY_APPEARANCE)
            .remove(KEY_TEXT_SCALE)
            .remove(KEY_RENDER_QUALITY)
            .remove(KEY_FRAME_RATE)
            .remove(KEY_MOTION)
            .remove(KEY_HAPTICS)
            .remove(KEY_GRID)
            .remove(KEY_PINS)
            .remove(KEY_KEEP_AWAKE)
            .remove(KEY_AUTOPLAY)
            .apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_LAST = "last_object"
        private const val KEY_AD_FREE_UNTIL = "ad_free_until"
        private const val KEY_REMOVE_ADS = "remove_ads"
        private const val KEY_AI_ENDPOINT = "ai_endpoint"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_AI_KEY = "ai_key"
        private const val KEY_INTRO = "intro_seen"
        @Suppress("unused")
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_RENDER_QUALITY = "render_quality"
        private const val KEY_FRAME_RATE = "frame_rate"
        private const val KEY_MOTION = "motion"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_GRID = "grid_default"
        private const val KEY_PINS = "pins_default"
        private const val KEY_KEEP_AWAKE = "keep_awake"
        private const val KEY_AUTOPLAY = "autoplay"

        const val APPEARANCE_LIGHT = "LIGHT"
        const val APPEARANCE_DARK = "DARK"
        const val APPEARANCE_SYSTEM = "SYSTEM"

        const val QUALITY_LITE = "BATTERY"
        const val QUALITY_BALANCED = "BALANCED"
        const val QUALITY_SHARP = "SHARP"

        const val DEFAULT_TEXT_SCALE = 1.22f

        const val DEFAULT_AI_ENDPOINT = "https://api.openai.com/v1/images/generations"
        const val DEFAULT_AI_MODEL = "gpt-image-1"
    }
}
