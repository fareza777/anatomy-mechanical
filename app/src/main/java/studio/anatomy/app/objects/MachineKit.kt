package studio.anatomy.app.objects

import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.model.Part
import studio.anatomy.app.render.Geo
import studio.anatomy.app.render.Material
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.MeshBuilder
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

    // A few real, merged decals give the shared plinth the read of an engineered
    // instrument instead of a smooth box. They are tiny geometry: no textures,
    // no extra draw calls, and the renderer's existing face batching absorbs them.
    val plateX = width * 0.28f
    val plateZ = depth * 0.30f
    val detailY = top + 0.024f
    val seams = MeshBuilder().apply {
        for (x in listOf(-width * 0.25f, 0f, width * 0.25f)) {
            addMesh(
                Geo.box(0.010f, 0.006f, depth * 0.34f),
                Mat4.translation(V3(x, detailY, -depth * 0.12f)),
            )
        }
        for (z in listOf(-depth * 0.28f, depth * 0.28f)) {
            addMesh(
                Geo.box(width * 0.44f, 0.006f, 0.010f),
                Mat4.translation(V3(0f, detailY, z)),
            )
        }
        // Recessed data-plate bezel and four tiny fasteners.
        addMesh(
            Geo.slab(0.29f, 0.17f, 0.012f, cornerRadius = 0.018f, bevel = 0.003f),
            Mat4.translation(V3(plateX, top + 0.018f, plateZ)),
        )
        for (dx in listOf(-0.115f, 0.115f)) {
            for (dz in listOf(-0.060f, 0.060f)) {
                addMesh(
                    Geo.hexBolt(0.012f, 0.008f),
                    Mat4.translation(V3(plateX + dx, top + 0.028f, plateZ + dz)),
                )
            }
        }
        for (i in 0 until 3) {
            addMesh(
                Geo.box(0.12f, 0.004f, 0.007f),
                Mat4.translation(V3(plateX - 0.015f, top + 0.031f, plateZ - 0.026f + i * 0.026f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "${prefix}_panel_seams", name = "Deck panel seams", system = system, assembly = assembly,
            mesh = seams, material = Materials.graphite,
        )
    )
    val dataPlate = Geo.slab(
        0.215f, 0.105f, 0.008f,
        cornerRadius = 0.009f, bevel = 0.002f, segments = 2,
    ).transformed(Mat4.translation(V3(plateX, top + 0.024f, plateZ)))
    parts.add(
        Part(
            id = "${prefix}_data_plate", name = "Serial data plate", system = system, assembly = assembly,
            mesh = dataPlate, material = Materials.labelPaper,
        )
    )
    val hazard = MeshBuilder().apply {
        val baseX = -width * 0.33f
        val baseZ = depth * 0.31f
        for (i in 0 until 4) {
            addMesh(
                Geo.box(0.14f, 0.006f, 0.018f),
                Mat4.translation(V3(baseX + i * 0.085f, top + 0.025f, baseZ))
                    .multiply(Mat4.rotateY(-34f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "${prefix}_hazard", name = "Service hazard stripe", system = system, assembly = assembly,
            mesh = hazard, material = Materials.paintOrange,
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
