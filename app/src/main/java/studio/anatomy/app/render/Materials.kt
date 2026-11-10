package studio.anatomy.app.render

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class FaceMode { NORMAL, SELECTED, DIMMED, GHOST }

enum class SurfacePattern { NONE, BRUSHED, PANEL, GRID, SPECKLE, PATINA, HAZARD }

class Material(
    val key: String,
    val name: String,
    base: Int,
    val specular: Float = 0.35f,
    val shininess: Float = 26f,
    val alpha: Int = 255,
    val emissive: Boolean = false,
    val specTint: Int = 0xFFF7FFFC.toInt(),
    val fillLight: Float = 0.08f,
    val rimLight: Float = 0.12f,
    val patina: Float = 0.028f,
    val edgeWear: Float = 0f,
    val pattern: SurfacePattern = SurfacePattern.NONE,
    val patternStrength: Float = 0f,
) {
    val dark: Int
    val light: Int
    val edge: Int
    val highlightDark: Int
    val highlightLight: Int
    val highlightEdge: Int
    val ghostFill: Int
    val dimDark: Int
    val dimLight: Int

    init {
        // A gentler shadow mix keeps unlit faces readable against the dark stage; before,
        // a cream link turned away from the key light vanished into the backdrop.
        val shadow = ColorMath.mix(base, 0xFF04090C.toInt(), 0.34f)
        val warmLight = ColorMath.mix(base, 0xFFF4E7CC.toInt(), 0.54f)
        dark = ColorMath.withAlpha(shadow, alpha)
        light = ColorMath.withAlpha(warmLight, alpha)
        edge = ColorMath.withAlpha(ColorMath.mix(base, 0xFF000000.toInt(), 0.64f), (alpha * 0.92f).toInt())
        highlightDark = ColorMath.withAlpha(ColorMath.mix(base, 0xFF052A2A.toInt(), 0.62f), alpha)
        highlightLight = ColorMath.withAlpha(ColorMath.mix(ColorMath.mix(base, Shading.ACCENT, 0.55f), 0xFFFFFFFF.toInt(), 0.42f), alpha)
        highlightEdge = ColorMath.withAlpha(0xFFC8FFF7.toInt(), (alpha * 0.95f).toInt())
        ghostFill = ColorMath.withAlpha(ColorMath.mix(base, Shading.ACCENT, 0.55f), 34)
        dimDark = ColorMath.withAlpha(0xFF080D10.toInt(), alpha)
        dimLight = ColorMath.withAlpha(0xFF5A6A6E.toInt(), alpha)
    }

    fun fillColor(mode: FaceMode, lightAmount: Float): Int {
        // 18 bands keep large curved shells smooth while staying cheap enough for the
        // path-batched renderer. The extra steps read as a fine surface finish.
        val stepped = kotlin.math.round(lightAmount.coerceIn(0f, 1f) * 18f) / 18f
        val low = when (mode) {
            FaceMode.NORMAL -> dark
            FaceMode.SELECTED -> highlightDark
            FaceMode.DIMMED -> dimDark
            FaceMode.GHOST -> ghostFill
        }
        val high = when (mode) {
            FaceMode.NORMAL -> light
            FaceMode.SELECTED -> highlightLight
            FaceMode.DIMMED -> dimLight
            FaceMode.GHOST -> ghostFill
        }
        return ColorMath.mix(low, high, stepped)
    }

    fun edgeColor(mode: FaceMode): Int {
        val base = when (mode) {
            FaceMode.NORMAL -> edge
            FaceMode.SELECTED -> highlightEdge
            FaceMode.DIMMED -> ColorMath.withAlpha(0xFF19242A.toInt(), 120)
            FaceMode.GHOST -> ColorMath.withAlpha(Shading.ACCENT, 100)
        }
        if (edgeWear <= 0f || mode == FaceMode.GHOST) return base
        val alpha = (base ushr 24) and 0xFF
        val worn = ColorMath.withAlpha(0xFFE6F1EB.toInt(), alpha)
        return ColorMath.mix(base, worn, edgeWear.coerceIn(0f, 1f) * 0.45f)
    }

    fun alphaFor(mode: FaceMode): Int {
        val baseAlpha = when (mode) {
            FaceMode.NORMAL -> alpha
            FaceMode.SELECTED -> if (alpha < 200) (alpha + 70).coerceAtMost(220) else 255
            FaceMode.DIMMED -> (alpha * 0.82f).toInt()
            FaceMode.GHOST -> 46
        }
        return baseAlpha
    }
}

object Shading {
    const val ACCENT = 0xFF6EF2DF.toInt()
    const val AMBER = 0xFFF2B25C.toInt()
    const val INK = 0xFF060A0E.toInt()

    private val lightDirection = floatArrayOf(-0.44f, 0.61f, 0.66f)
    private val halfDirection: FloatArray

    init {
        val hx = lightDirection[0]
        val hy = lightDirection[1]
        val hz = lightDirection[2] + 1f
        val length = sqrt(hx * hx + hy * hy + hz * hz)
        halfDirection = floatArrayOf(hx / length, hy / length, hz / length)
    }

    fun shade(
        material: Material,
        nx: Float,
        ny: Float,
        nz: Float,
        mode: FaceMode,
        time: Float,
        faceSeed: Float = 0.5f,
        occlusion: Float = 1f,
    ): Int {
        if (material.emissive) {
            val level = 0.52f + 0.48f * nz.coerceIn(0f, 1f)
            return material.fillColor(
                if (mode == FaceMode.GHOST) FaceMode.GHOST else mode,
                (level * (0.86f + 0.14f * occlusion)).coerceIn(0f, 1f),
            )
        }
        if (mode == FaceMode.GHOST) {
            return material.fillColor(FaceMode.GHOST, 0.5f)
        }
        val dot = (nx * lightDirection[0] + ny * lightDirection[1] + nz * lightDirection[2]).coerceAtLeast(0f)
        val hemisphere = 0.5f + 0.5f * ny
        // Key, cool fill and a grazing rim: three cheap directional responses
        // instead of one flat light. No extra paths or shader state are added.
        val fill = (nx * -0.55f + ny * 0.12f + nz * 0.28f).coerceAtLeast(0f)
        val rim = (1f - abs(nz)).coerceIn(0f, 1f)
        var t = 0.15f + 0.62f * dot + 0.12f * hemisphere
        t += fill * material.fillLight
        t += rim * rim * material.rimLight
        val spec = if (material.specular > 0f) {
            val hDot = (nx * halfDirection[0] + ny * halfDirection[1] + nz * halfDirection[2]).coerceAtLeast(0f)
            hDot.pow(material.shininess) * material.specular
        } else 0f
        val patina = (faceSeed - 0.5f) * material.patina
        var value = (t + spec * 0.78f + patina) * occlusion
        // A tiny normal-driven grain breaks up the perfectly flat facets. It is
        // deliberately absent from rubber and matt paint, stronger on polished metal.
        val seed = (material.key.hashCode() and 0xFF) * 0.017f
        val grainA = sin((nx * 13.7f + ny * 47.1f + nz * 29.3f + seed) * 2.1f)
        val grainB = sin((nx * 31.4f - ny * 17.9f + nz * 11.3f + seed) * 3.7f)
        val grain = 0.005f + 0.019f * material.specular + 0.012f * material.patina
        value += grainA * grainB * grain
        return material.fillColor(mode, value.coerceIn(0f, 1f))
    }

    fun alphaFor(material: Material, mode: FaceMode): Int {
        val base = material.alphaFor(mode)
        return when (mode) {
            FaceMode.GHOST -> if (material.alpha < 120) 70 else 40
            else -> base
        }
    }
}

object ColorMath {
    fun mix(a: Int, b: Int, t: Float): Int {
        val amount = t.coerceIn(0f, 1f)
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val aa = (a ushr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val ba = (b ushr 24) and 0xFF
        val r = (ar + (br - ar) * amount).toInt().coerceIn(0, 255)
        val g = (ag + (bg - ag) * amount).toInt().coerceIn(0, 255)
        val bl = (ab + (bb - ab) * amount).toInt().coerceIn(0, 255)
        val al = (aa + (ba - aa) * amount).toInt().coerceIn(0, 255)
        return (al shl 24) or (r shl 16) or (g shl 8) or bl
    }

    fun scale(color: Int, factor: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = (((color shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = (((color shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    }

    fun luminance(color: Int): Float {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}

object Materials {
    val titanium = Material("titanium", "Titanium", 0xFF9DADB1.toInt(), specular = 0.62f, shininess = 34f, fillLight = 0.10f, rimLight = 0.16f, patina = 0.045f, edgeWear = 0.06f, pattern = SurfacePattern.BRUSHED, patternStrength = 0.34f)
    val brushedSteel = Material("steel", "Brushed steel", 0xFF6C7E84.toInt(), specular = 0.46f, shininess = 22f, fillLight = 0.09f, rimLight = 0.14f, patina = 0.050f, edgeWear = 0.05f, pattern = SurfacePattern.BRUSHED, patternStrength = 0.30f)
    val darkSteel = Material("darksteel", "Dark steel", 0xFF404E54.toInt(), specular = 0.40f, shininess = 20f, fillLight = 0.10f, rimLight = 0.16f, patina = 0.040f, edgeWear = 0.04f)
    val graphite = Material("graphite", "Graphite", 0xFF263237.toInt(), specular = 0.32f, shininess = 18f, fillLight = 0.07f, rimLight = 0.13f, patina = 0.032f, pattern = SurfacePattern.SPECKLE, patternStrength = 0.20f)
    val ink = Material("ink", "Black oxide", 0xFF10181C.toInt(), specular = 0.24f, shininess = 14f)
    val bedPlate = Material("bedPlate", "Anodised bed", 0xFF2A3539.toInt(), specular = 0.48f, shininess = 26f, fillLight = 0.09f, rimLight = 0.13f, patina = 0.030f, edgeWear = 0.05f, pattern = SurfacePattern.PANEL, patternStrength = 0.55f)
    val deckInset = Material("deckInset", "Recessed deck", 0xFF121A1E.toInt(), specular = 0.30f, shininess = 18f, fillLight = 0.06f, rimLight = 0.10f, patina = 0.025f, pattern = SurfacePattern.PANEL, patternStrength = 0.40f)
    val copper = Material("copper", "Copper", 0xFFA96A45.toInt(), specular = 0.56f, shininess = 30f, fillLight = 0.10f, rimLight = 0.17f, patina = 0.075f, edgeWear = 0.08f, pattern = SurfacePattern.PATINA, patternStrength = 0.28f)
    val gold = Material("gold", "Gold", 0xFFC09A55.toInt(), specular = 0.62f, shininess = 38f, fillLight = 0.10f, rimLight = 0.18f, patina = 0.065f, edgeWear = 0.07f, pattern = SurfacePattern.PATINA, patternStrength = 0.24f)
    val brass = Material("brass", "Brass", 0xFF8C6F3F.toInt(), specular = 0.54f, shininess = 32f, fillLight = 0.09f, rimLight = 0.16f, patina = 0.080f, edgeWear = 0.07f, pattern = SurfacePattern.PATINA, patternStrength = 0.26f)
    val ceramic = Material("ceramic", "Ceramic", 0xFFCAD4CE.toInt(), specular = 0.34f, shininess = 30f, fillLight = 0.08f, rimLight = 0.14f, patina = 0.025f, pattern = SurfacePattern.SPECKLE, patternStrength = 0.18f)
    val rubber = Material("rubber", "Rubber", 0xFF1A2023.toInt(), specular = 0.08f, shininess = 10f, fillLight = 0.05f, rimLight = 0.10f, patina = 0.040f, pattern = SurfacePattern.SPECKLE, patternStrength = 0.40f)
    val glass = Material("glass", "Sapphire glass", 0xFF8FE9E4.toInt(), specular = 0.72f, shininess = 60f, alpha = 42, specTint = 0xFFD8FFFB.toInt())
    val glassDark = Material("glassDark", "Smoked glass", 0xFF3D747A.toInt(), specular = 0.62f, shininess = 50f, alpha = 30)
    val emitCyan = Material("emitCyan", "Cyan emitter", 0xFF7CFFEA.toInt(), emissive = true, specular = 0f)
    val emitAmber = Material("emitAmber", "Amber emitter", 0xFFFFB562.toInt(), emissive = true, specular = 0f)
    val emitBlue = Material("emitBlue", "Plasma emitter", 0xFF8FB4FF.toInt(), emissive = true, specular = 0f)
    val emitRed = Material("emitRed", "Heat emitter", 0xFFFF7A5C.toInt(), emissive = true, specular = 0f)
    val emitWhite = Material("emitWhite", "Hot emitter", 0xFFF4FFF9.toInt(), emissive = true, specular = 0f)
    val accentPaint = Material("accentPaint", "Signal paint", 0xFF2C7A75.toInt(), specular = 0.25f, shininess = 18f, fillLight = 0.09f, rimLight = 0.14f, patina = 0.025f)
    val paintCream = Material("paintCream", "Ivory enamel", 0xFFF0E9D6.toInt(), specular = 0.24f, shininess = 18f, fillLight = 0.09f, rimLight = 0.15f, patina = 0.018f, edgeWear = 0.035f, pattern = SurfacePattern.SPECKLE, patternStrength = 0.14f)
    val paintOrange = Material("paintOrange", "Safety orange", 0xFFD2763D.toInt(), specular = 0.28f, shininess = 20f, fillLight = 0.08f, rimLight = 0.14f, patina = 0.035f, edgeWear = 0.06f, pattern = SurfacePattern.HAZARD, patternStrength = 0.55f)
    val paintSlate = Material("paintSlate", "Slate paint", 0xFF51646A.toInt(), specular = 0.30f, shininess = 18f, fillLight = 0.09f, rimLight = 0.14f, patina = 0.032f, edgeWear = 0.04f)
    val solarCell = Material("solarCell", "Solar cell", 0xFF142A46.toInt(), specular = 0.78f, shininess = 62f, fillLight = 0.11f, rimLight = 0.16f, patina = 0.020f, pattern = SurfacePattern.GRID, patternStrength = 0.62f)
    val solarBus = Material("solarBus", "Cell busbar", 0xFFB78A4E.toInt(), specular = 0.50f, shininess = 30f, fillLight = 0.09f, rimLight = 0.16f, patina = 0.035f, edgeWear = 0.05f)
    val copperBright = Material("copperBright", "Bare copper", 0xFFC6804E.toInt(), specular = 0.64f, shininess = 36f, fillLight = 0.10f, rimLight = 0.18f, patina = 0.070f, edgeWear = 0.06f, pattern = SurfacePattern.BRUSHED, patternStrength = 0.24f)
    val nickel = Material("nickel", "Polished nickel", 0xFFB6C4CA.toInt(), specular = 0.66f, shininess = 42f, fillLight = 0.10f, rimLight = 0.18f, patina = 0.035f, edgeWear = 0.04f, pattern = SurfacePattern.BRUSHED, patternStrength = 0.26f)
    val bluedSteel = Material("bluedSteel", "Blued steel", 0xFF4E6E9C.toInt(), specular = 0.58f, shininess = 34f, fillLight = 0.09f, rimLight = 0.17f, patina = 0.040f, edgeWear = 0.05f)
    val ruby = Material("ruby", "Ruby", 0xFFD94A5E.toInt(), specular = 0.52f, shininess = 46f)
    val fieldLine = Material("fieldLine", "Field line", 0xFF7E9CFF.toInt(), emissive = true, specular = 0f)
    val labelPaper = Material("label", "Label paper", 0xFFB9C1B9.toInt(), specular = 0.05f, shininess = 4f)
}
