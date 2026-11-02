package studio.anatomy.app.objects

import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.model.Part
import studio.anatomy.app.render.Geo
import studio.anatomy.app.render.Material
import studio.anatomy.app.render.Materials
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared hardware for the display machines: a dark, engineered plinth with feet,
 * a recessed deck and a small signal rail. It keeps the catalog visually coherent
 * without making every object start with the same silhouette.
 */
internal fun displayBase(
    prefix: String,
    width: Float,
    depth: Float,
    top: Float,
    system: String,
    assembly: String,
    accentMaterial: Material = Materials.emitCyan,
): List<Part> {
    val parts = ArrayList<Part>(4)
    val deck = Geo.slab(width, depth, 0.22f, cornerRadius = 0.12f, bevel = 0.035f, segments = 3)
        .transformed(Mat4.translation(V3(0f, top - 0.11f, 0f)))
    parts.add(
        Part(
            id = "${prefix}_deck", name = "Display deck", system = system, assembly = assembly,
            mesh = deck, material = Materials.bedPlate,
        )
    )
    val inset = Geo.slab(width * 0.86f, depth * 0.82f, 0.03f, cornerRadius = 0.08f, bevel = 0.008f)
        .transformed(Mat4.translation(V3(0f, top + 0.005f, 0f)))
    parts.add(
        Part(
            id = "${prefix}_inset", name = "Recessed deck", system = system, assembly = assembly,
            mesh = inset, material = Materials.deckInset,
        )
    )
    val feet = ArrayList<studio.anatomy.app.render.Mesh>(4)
    val fx = width * 0.5f - 0.32f
    val fz = depth * 0.5f - 0.32f
    listOf(-fx to -fz, fx to -fz, -fx to fz, fx to fz).forEach { (x, z) ->
        feet.add(
            Geo.taperedCylinder(0.16f, 0.11f, 0.18f, 14)
                .transformed(Mat4.translation(V3(x, 0.09f, z)))
        )
        feet.add(
            Geo.cylinder(0.2f, 0.035f, 16)
                .transformed(Mat4.translation(V3(x, 0.015f, z)))
        )
    }
    parts.add(
        Part(
            id = "${prefix}_feet", name = "Levelling feet", system = system, assembly = assembly,
            mesh = Geo.merge(*feet.toTypedArray()), material = Materials.rubber,
        )
    )
    val rail = Geo.slab(width * 0.45f, 0.018f, 0.016f, 0.004f, 0f)
        .transformed(Mat4.translation(V3(0f, top - 0.085f, depth * 0.5f + 0.004f)))
    parts.add(
        Part(
            id = "${prefix}_signal", name = "Signal rail", system = system, assembly = assembly,
            mesh = rail, material = accentMaterial,
        )
    )
    val fasteners = ArrayList<V3>()
    val bx = width * 0.5f - 0.24f
    val bz = depth * 0.5f - 0.24f
    listOf(-bx to -bz, bx to -bz, -bx to bz, bx to bz).forEach { (x, z) ->
        fasteners.add(V3(x, top + 0.02f, z))
    }
    parts.add(
        Part(
            id = "${prefix}_fasteners", name = "Deck fasteners", system = system, assembly = assembly,
            mesh = Geo.boltSet(fasteners, 0.055f, 0.03f), material = Materials.titanium,
        )
    )
    return parts
}

internal fun boltCircleX(
    count: Int,
    radius: Float,
    x: Float,
    boltRadius: Float = 0.045f,
    height: Float = 0.08f,
    phase: Float = 0f,
): studio.anatomy.app.render.Mesh {
    val points = ArrayList<V3>(count)
    for (i in 0 until count) {
        val angle = Math.toRadians((phase + 360f * i / count).toDouble())
        points.add(
            V3(
                x,
                (radius * cos(angle)).toFloat(),
                (radius * sin(angle)).toFloat(),
            )
        )
    }
    val mesh = Geo.boltSet(points, boltRadius, height, axis = V3.X)
    return mesh
}

internal fun machineRingX(x: Float, outer: Float, inner: Float, length: Float, segments: Int = 28): studio.anatomy.app.render.Mesh =
    Geo.hollowCylinder(outer, inner, length, segments)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(Mat4.rotateZ(-90f)))

internal fun machineCylinderX(
    x: Float,
    radius: Float,
    length: Float,
    segments: Int = 24,
    bevel: Float = 0f,
): studio.anatomy.app.render.Mesh =
    Geo.cylinder(radius, length, segments, bevel = bevel)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(Mat4.rotateZ(-90f)))

internal fun machineTorusX(x: Float, major: Float, minor: Float): studio.anatomy.app.render.Mesh =
    Geo.torus(major, minor, 32, 6)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(Mat4.rotateZ(-90f)))

internal fun machineLatheX(
    x: Float,
    profile: List<Pair<Float, Float>>,
    segments: Int = 32,
    capBack: Boolean = false,
): studio.anatomy.app.render.Mesh =
    Geo.lathe(profile, segments, capBack, capTop = false)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(Mat4.rotateZ(-90f)))
