package studio.anatomy.app.objects

import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.ControlSpec
import studio.anatomy.app.model.GuideStep
import studio.anatomy.app.model.Hotspot
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.model.Part
import studio.anatomy.app.model.Readout
import studio.anatomy.app.model.SceneEffect
import studio.anatomy.app.model.SpecRow
import studio.anatomy.app.render.Geo
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val LV_FRAME = "STRUCTURE"
private const val LV_TRACTION = "TRACTION"
private const val LV_CABIN = "CABIN"
private const val LV_CONTROL = "CONTROL"

private const val LV_PLINTH = "PLINTH"
private const val LV_SHAFT = "SHAFT"
private const val LV_MACHINE = "MACHINE ROOM"
private const val LV_CAR = "CAR"
private const val LV_CW = "COUNTERWEIGHT"
private const val LV_LANDING = "LANDINGS"
private const val LV_VISITOR = "VISITOR"

private const val TAU = 6.2831855f
private const val DECK_TOP = 0.30f
private const val VISITOR_OUT_Z = 1.08f
private const val VISITOR_IN_Z = 0.02f
private const val VISITOR_OUT_X = 0.10f
private const val VISITOR_IN_X = 0f

private const val CAR_LOW = 0.72f
private const val CAR_TRAVEL = 1.68f
private const val CW_LOW = 0.88f
private const val CW_TRAVEL = 1.52f
private const val SHEAVE_Y = 3.40f
private const val CABIN_HITCH = 0.45f

private fun liftPhase(ctx: AnimContext): Float =
    // The studio clock starts at 0.72 s. Offset the lift so a freshly opened object
    // begins with the passenger still outside and the doors open, ready to walk in.
    (ctx.machineTime - 0.72f) * (0.48f + 0.22f * ctx.control)

private class VisitorPose(
    val x: Float,
    val z: Float,
    val y: Float,
    val yaw: Float,
    val stride: Float,
    val walk: Float,
)

private fun smooth01(t: Float): Float {
    val v = t.coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}

private fun liftCycle(ctx: AnimContext): Float {
    val raw = liftPhase(ctx) / TAU
    return raw - floor(raw)
}

/**
 * One complete demonstration cycle: dwell at the lobby, climb, dwell at the top,
 * descend, then dwell again while the passenger leaves. The dwell is not padding;
 * it gives the doors and the humanoid time to finish their business before motion.
 */
private fun liftProgress(ctx: AnimContext): Float {
    val c = liftCycle(ctx)
    return when {
        c < 0.22f -> 0f
        c < 0.48f -> smooth01((c - 0.22f) / 0.26f)
        c < 0.60f -> 1f
        c < 0.86f -> 1f - smooth01((c - 0.60f) / 0.26f)
        else -> 0f
    }
}

private fun visitorPose(ctx: AnimContext): VisitorPose {
    val c = liftCycle(ctx)
    val p = liftProgress(ctx)
    val entry = smooth01((c - 0.02f) / 0.18f)
    val exit = smooth01((c - 0.87f) / 0.12f)
    val inside = when {
        c < 0.22f -> entry
        c >= 0.86f -> 1f - exit
        else -> 1f
    }
    val entryWalk = smooth01((c - 0.02f) / 0.03f) * (1f - smooth01((c - 0.17f) / 0.03f))
    val exitWalk = smooth01((c - 0.87f) / 0.03f) * (1f - smooth01((c - 0.96f) / 0.03f))
    val walk = if (c >= 0.84f) exitWalk else entryWalk
    val walkPhase = ctx.machineTime * 9f
    val stride = sin(walkPhase) * walk
    val bob = abs(sin(walkPhase)) * 0.008f * walk
    val floorInside = 0.36f + CAR_TRAVEL * p
    val x = VISITOR_OUT_X + (VISITOR_IN_X - VISITOR_OUT_X) * inside
    val z = VISITOR_OUT_Z + (VISITOR_IN_Z - VISITOR_OUT_Z) * inside
    val y = DECK_TOP + (floorInside - DECK_TOP) * inside + bob
    // Turn around while the car is parked at the lobby, before walking back out.
    val turn = smooth01((c - 0.84f) / 0.04f)
    val yaw = 180f * (1f - turn)
    return VisitorPose(x, z, y, yaw, stride, walk)
}

private fun doorOpening(p: Float): Float {
    val edge = kotlin.math.min(p, 1f - p)
    val t = (1f - edge / 0.16f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun carCableLength(p: Float): Float =
    (SHEAVE_Y - (CAR_LOW + CAR_TRAVEL * p + CABIN_HITCH)).coerceAtLeast(0.18f)

private fun counterweightY(p: Float): Float = CW_LOW + (1f - p) * CW_TRAVEL

private fun counterweightCableLength(p: Float): Float =
    (SHEAVE_Y - (counterweightY(p) + 0.38f)).coerceAtLeast(0.18f)

/** A machined traction sheave: flanges, rope grooves, spokes and side fasteners. */
private fun sheaveMesh(radius: Float, width: Float): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.cylinder(radius, width, 40, bevel = 0.012f).transformed(Mat4.rotateX(90f)),
    )
    addMesh(
        Geo.cylinder(radius * 0.88f, width * 1.05f, 36).transformed(Mat4.rotateX(90f)),
    )
    listOf(-0.08f, 0f, 0.08f).forEach { groove ->
        addMesh(
            Geo.torus(radius * 0.72f, 0.013f, 36, 5).transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(0f, 0f, groove * width * 5f)),
        )
    }
    listOf(-1f, 1f).forEach { side ->
        addMesh(
            Geo.cylinder(radius * 1.04f, width * 0.10f, 40, bevel = 0.006f)
                .transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(0f, 0f, side * width * 0.50f)),
        )
    }
    for (i in 0 until 6) {
        val angle = Math.toRadians(i * 60.0)
        addMesh(
            Geo.cylinderBetween(
                V3.ZERO,
                V3((radius * 0.76f * cos(angle)).toFloat(), (radius * 0.76f * sin(angle)).toFloat(), 0f),
                0.020f, 6,
            ),
        )
    }
    addMesh(
        Geo.cylinder(radius * 0.24f, width * 1.45f, 20, bevel = 0.008f)
            .transformed(Mat4.rotateX(90f)),
    )
    val rimBolts = ArrayList<V3>()
    for (i in 0 until 10) {
        val angle = Math.toRadians(i * 36.0 + 18.0)
        rimBolts.add(
            V3(
                (radius * 0.42f * cos(angle)).toFloat(),
                (radius * 0.42f * sin(angle)).toFloat(),
                width * 0.54f,
            ),
        )
    }
    addMesh(Geo.boltSet(rimBolts, 0.018f, 0.024f, axis = V3.Z))
}.build()

private fun sheaveGrooveMesh(radius: Float, width: Float): Mesh = MeshBuilder().apply {
    listOf(-0.30f, 0f, 0.30f).forEach { groove ->
        addMesh(
            Geo.torus(radius * 0.72f, 0.018f, 36, 5).transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(0f, 0f, groove * width)),
        )
    }
}.build()

private fun ropeBundle(offsets: FloatArray): Mesh = MeshBuilder().apply {
    offsets.forEach { x ->
        addMesh(
            Geo.cylinder(0.013f, 1f, 6).transformed(Mat4.translation(V3(x, -0.5f, 0f))),
        )
        addMesh(
            Geo.cylinder(0.006f, 1f, 5).transformed(Mat4.translation(V3(x + 0.018f, -0.5f, 0.008f))),
        )
    }
}.build()

private fun towerMesh(): Mesh = MeshBuilder().apply {
    val postX = 0.95f
    val postZ = 0.62f
    listOf(-1f, 1f).forEach { sx ->
        listOf(-1f, 1f).forEach { sz ->
            val x = sx * postX
            val z = sz * postZ
            addMesh(Geo.box(0.10f, 3.28f, 0.10f), Mat4.translation(V3(x, 1.94f, z)))
            addMesh(Geo.box(0.18f, 3.28f, 0.024f), Mat4.translation(V3(x, 1.94f, z - sz * 0.045f)))
            addMesh(Geo.box(0.024f, 3.28f, 0.18f), Mat4.translation(V3(x - sx * 0.045f, 1.94f, z)))
            addMesh(Geo.box(0.22f, 0.035f, 0.22f), Mat4.translation(V3(x, 0.315f, z)))
            addMesh(Geo.hexBolt(0.035f, 0.026f), Mat4.translation(V3(x, 0.345f, z)))
        }
    }
    listOf(0.42f, 0.90f, 1.38f, 1.86f, 2.34f, 2.82f, 3.26f).forEach { y ->
        // The front is deliberately left open: the car and its doors are the exhibit.
        addMesh(Geo.box(1.86f, 0.055f, 0.055f), Mat4.translation(V3(0f, y, -postZ)))
        addMesh(Geo.box(0.055f, 0.055f, 1.20f), Mat4.translation(V3(postX, y, 0f)))
        addMesh(Geo.box(0.055f, 0.055f, 1.20f), Mat4.translation(V3(-postX, y, 0f)))
    }
    addMesh(Geo.box(1.86f, 0.055f, 0.055f), Mat4.translation(V3(0f, 3.26f, postZ)))
    // Rear and side bracing. The front stays open so the car and doors read clearly.
    addMesh(
        Geo.cylinderBetween(
            V3(-postX + 0.10f, 0.42f, -postZ),
            V3(postX - 0.10f, 0.90f, -postZ),
            0.023f, 6,
        ),
    )
    addMesh(
        Geo.cylinderBetween(
            V3(postX - 0.10f, 2.82f, -postZ),
            V3(-postX + 0.10f, 3.26f, -postZ),
            0.023f, 6,
        ),
    )
    listOf(-1f, 1f).forEach { side ->
        addMesh(
            Geo.cylinderBetween(
                V3(side * postX, 0.42f, -postZ + 0.08f),
                V3(side * postX, 1.38f, postZ - 0.08f),
                0.021f, 6,
            ),
        )
    }
    // A fine rear guard mesh; it gives the shaft depth without hiding the car.
    for (i in 0..7) {
        val x = -0.84f + i * 0.24f
        addMesh(Geo.box(0.016f, 2.86f, 0.012f), Mat4.translation(V3(x, 1.81f, -0.695f)))
    }
    for (i in 0..10) {
        val y = 0.38f + i * 0.28f
        addMesh(Geo.box(1.88f, 0.016f, 0.012f), Mat4.translation(V3(0f, y, -0.695f)))
    }
    // Counterweight guide rails.
    listOf(-0.24f, 0.24f).forEach { z ->
        addMesh(Geo.box(0.055f, 2.9f, 0.035f), Mat4.translation(V3(-1.15f, 1.79f, z)))
    }
}.build()

private fun guideMesh(): Mesh = MeshBuilder().apply {
    listOf(-0.69f, 0.69f).forEach { x ->
        listOf(-0.50f, 0.50f).forEach { z ->
            addMesh(Geo.box(0.050f, 2.95f, 0.060f), Mat4.translation(V3(x, 1.80f, z)))
            addMesh(Geo.box(0.022f, 2.95f, 0.032f), Mat4.translation(V3(x - kotlin.math.sign(x) * 0.034f, 1.80f, z)))
            for (i in 0..5) {
                addMesh(Geo.box(0.09f, 0.04f, 0.13f), Mat4.translation(V3(x, 0.45f + i * 0.50f, z)))
            }
        }
    }
}.build()

private fun guideFaceMesh(): Mesh = MeshBuilder().apply {
    listOf(-0.69f, 0.69f).forEach { x ->
        listOf(-0.50f, 0.50f).forEach { z ->
            addMesh(
                Geo.box(0.010f, 2.86f, 0.020f),
                Mat4.translation(V3(x - kotlin.math.sign(x) * 0.023f, 1.80f, z)),
            )
        }
    }
}.build()

private fun landingMesh(): Mesh = MeshBuilder().apply {
    for (floor in 0 until 4) {
        val y = 0.36f + floor * 0.56f
        // Split the sill into two side plates. A full-width bar across the open front
        // turned the cutaway into a cage and hid the car doors.
        listOf(-1f, 1f).forEach { side ->
            addMesh(
                Geo.slab(0.25f, 0.22f, 0.055f, cornerRadius = 0.025f, bevel = 0.010f),
                Mat4.translation(V3(side * 0.72f, y, 0.68f)),
            )
            addMesh(Geo.box(0.22f, 0.018f, 0.10f), Mat4.translation(V3(side * 0.72f, y + 0.034f, 0.68f)))
            addMesh(Geo.box(0.05f, 0.17f, 0.12f), Mat4.translation(V3(side * 0.72f, y - 0.06f, 0.68f)))
        }
    }
}.build()

private fun carBodyMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(1.16f, 0.82f, 0.08f, cornerRadius = 0.05f, bevel = 0.02f), Mat4.translation(V3(0f, -0.36f, 0f)))
    addMesh(Geo.slab(1.16f, 0.82f, 0.08f, cornerRadius = 0.05f, bevel = 0.02f), Mat4.translation(V3(0f, 0.36f, 0f)))
    addMesh(Geo.box(1.08f, 0.66f, 0.05f), Mat4.translation(V3(0f, 0f, -0.395f)))
    listOf(-1f, 1f).forEach { side ->
        addMesh(Geo.box(0.050f, 0.66f, 0.82f), Mat4.translation(V3(side * 0.55f, 0f, 0f)))
        addMesh(Geo.box(0.050f, 0.66f, 0.050f), Mat4.translation(V3(side * 0.545f, 0f, 0.415f)))
    }
    addMesh(Geo.box(1.10f, 0.060f, 0.060f), Mat4.translation(V3(0f, 0.32f, 0.415f)))
    addMesh(Geo.box(1.10f, 0.050f, 0.060f), Mat4.translation(V3(0f, -0.33f, 0.415f)))
    addMesh(Geo.box(0.38f, 0.030f, 0.28f), Mat4.translation(V3(0f, 0.405f, 0f)))
    addMesh(Geo.box(0.20f, 0.020f, 0.18f), Mat4.translation(V3(0f, 0.425f, 0f)))
}.build()

private fun doorGlassMesh(): Mesh = Geo.box(0.52f, 0.60f, 0.018f)

private fun doorFrameMesh(handleX: Float): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.54f, 0.026f, 0.026f), Mat4.translation(V3(0f, 0.30f, 0f)))
    addMesh(Geo.box(0.54f, 0.026f, 0.026f), Mat4.translation(V3(0f, -0.30f, 0f)))
    addMesh(Geo.box(0.026f, 0.60f, 0.026f), Mat4.translation(V3(-0.257f, 0f, 0f)))
    addMesh(Geo.box(0.026f, 0.60f, 0.026f), Mat4.translation(V3(0.257f, 0f, 0f)))
    addMesh(Geo.box(0.020f, 0.16f, 0.034f), Mat4.translation(V3(handleX, -0.02f, 0.026f)))
}.build()

private fun counterweightMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.54f, 0.34f, 0.07f, cornerRadius = 0.03f, bevel = 0.012f), Mat4.translation(V3(0f, 0.32f, 0f)))
    addMesh(Geo.slab(0.54f, 0.34f, 0.07f, cornerRadius = 0.03f, bevel = 0.012f), Mat4.translation(V3(0f, -0.32f, 0f)))
    for (i in 0 until 5) {
        addMesh(Geo.box(0.46f, 0.095f, 0.27f), Mat4.translation(V3(0f, -0.22f + i * 0.11f, 0f)))
        addMesh(Geo.box(0.40f, 0.012f, 0.28f), Mat4.translation(V3(0f, -0.27f + i * 0.11f, 0.01f)))
    }
    listOf(-1f, 1f).forEach { side ->
        addMesh(Geo.box(0.040f, 0.70f, 0.040f), Mat4.translation(V3(side * 0.23f, 0f, 0.12f)))
        addMesh(Geo.box(0.040f, 0.70f, 0.040f), Mat4.translation(V3(side * 0.23f, 0f, -0.12f)))
        addMesh(Geo.box(0.08f, 0.10f, 0.06f), Mat4.translation(V3(side * 0.25f, 0.05f, -0.17f)))
    }
    addMesh(Geo.cylinderBetween(V3(-0.16f, 0.38f, 0f), V3(0.16f, 0.38f, 0f), 0.022f, 6))
}.build()

private fun counterweightTrimMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 4) {
        addMesh(Geo.box(0.47f, 0.012f, 0.285f), Mat4.translation(V3(0f, -0.265f + i * 0.11f, 0f)))
    }
    listOf(-1f, 1f).forEach { side ->
        addMesh(Geo.box(0.05f, 0.04f, 0.05f), Mat4.translation(V3(side * 0.24f, 0.26f, 0f)))
    }
}.build()

private fun visitorLimbMesh(length: Float, radius: Float): Mesh = MeshBuilder().apply {
    addMesh(Geo.sphere(radius * 1.35f, 10, 6))
    addMesh(Geo.cylinderBetween(V3.ZERO, V3(0f, -length, 0f), radius, 8))
    addMesh(
        Geo.cylinder(radius * 0.82f, 0.018f, 10).transformed(Mat4.rotateX(90f)),
        Mat4.translation(V3(0f, -length, 0f)),
    )
}.build()

private fun visitorTorsoMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.25f, 0.17f, 0.22f, cornerRadius = 0.035f, bevel = 0.012f), Mat4.translation(V3(0f, 0.30f, 0f)))
    addMesh(Geo.box(0.20f, 0.08f, 0.14f), Mat4.translation(V3(0f, 0.18f, 0f)))
    addMesh(Geo.box(0.29f, 0.035f, 0.10f), Mat4.translation(V3(0f, 0.39f, 0f)))
    addMesh(
        Geo.torus(0.085f, 0.012f, 18, 4).transformed(Mat4.rotateX(90f)),
        Mat4.translation(V3(0f, 0.33f, 0.095f)),
    )
}.build()

private fun visitorHeadMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.14f, 0.13f, 0.13f, cornerRadius = 0.03f, bevel = 0.010f))
    addMesh(Geo.cylinder(0.012f, 0.06f, 8), Mat4.translation(V3(0f, 0.095f, 0f)))
}.build()

private fun visitorFootMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.075f, 0.035f, 0.13f), Mat4.translation(V3(0f, -0.0175f, 0.035f)))
}.build()

fun liftSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "vertical",
            width = 3.15f,
            depth = 2.55f,
            top = 0.30f,
            system = LV_FRAME,
            assembly = LV_PLINTH,
            accentMaterial = Materials.emitAmber,
        ),
    )

    parts.add(
        Part(
            id = "lv_plinth_panels", name = "Control plinth panels", system = LV_CONTROL, assembly = LV_PLINTH,
            mesh = MeshBuilder().apply {
                listOf(-0.92f, 0f, 0.92f).forEach { x ->
                    addMesh(Geo.box(0.54f, 0.21f, 0.022f), Mat4.translation(V3(x, 0.205f, 1.278f)))
                    for (i in 0 until 4) {
                        addMesh(
                            Geo.box(0.36f, 0.012f, 0.018f),
                            Mat4.translation(V3(x, 0.145f + i * 0.038f, 1.294f)),
                        )
                    }
                }
            }.build(),
            material = Materials.deckInset,
        ),
    )
    parts.add(
        Part(
            id = "lv_plinth_labels", name = "Plinth status lights", system = LV_CONTROL, assembly = LV_PLINTH,
            mesh = MeshBuilder().apply {
                listOf(-0.66f, 0.66f).forEach { x ->
                    addMesh(Geo.box(0.16f, 0.012f, 0.020f), Mat4.translation(V3(x, 0.255f, 1.294f)))
                }
            }.build(),
            material = Materials.emitAmber,
        ),
    )

    parts.add(
        Part(
            id = "lv_tower", name = "Shaft frame and braces", system = LV_FRAME, assembly = LV_SHAFT,
            mesh = towerMesh(), material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "lv_guides", name = "Car guide rails", system = LV_FRAME, assembly = LV_SHAFT,
            mesh = guideMesh(), material = Materials.bluedSteel,
        ),
    )
    parts.add(
        Part(
            id = "lv_guide_faces", name = "Machined guide faces", system = LV_FRAME, assembly = LV_SHAFT,
            mesh = guideFaceMesh(), material = Materials.nickel,
        ),
    )

    parts.add(
        Part(
            id = "lv_landings", name = "Landing thresholds", system = LV_FRAME, assembly = LV_LANDING,
            mesh = landingMesh(), material = Materials.brushedSteel,
        ),
    )
    parts.add(
        Part(
            id = "lv_landing_strips", name = "Landing safety edges", system = LV_CONTROL, assembly = LV_LANDING,
            mesh = MeshBuilder().apply {
                for (floor in 0 until 4) {
                    val y = 0.36f + floor * 0.56f
                    listOf(-1f, 1f).forEach { side ->
                        addMesh(Geo.box(0.23f, 0.014f, 0.026f), Mat4.translation(V3(side * 0.72f, y + 0.039f, 0.795f)))
                    }
                }
            }.build(),
            material = Materials.emitAmber,
        ),
    )
    parts.add(
        Part(
            id = "lv_call_panels", name = "Landing call stations", system = LV_CONTROL, assembly = LV_LANDING,
            mesh = MeshBuilder().apply {
                for (floor in 0 until 4) {
                    val y = 0.36f + floor * 0.56f
                    addMesh(Geo.box(0.12f, 0.21f, 0.035f), Mat4.translation(V3(1.04f, y + 0.30f, 0.64f)))
                }
            }.build(),
            material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "lv_call_lights", name = "Call button halos", system = LV_CONTROL, assembly = LV_LANDING,
            mesh = MeshBuilder().apply {
                for (floor in 0 until 4) {
                    val y = 0.36f + floor * 0.56f
                    addMesh(Geo.cylinder(0.024f, 0.012f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(1.04f, y + 0.35f, 0.662f)))
                }
            }.build(),
            material = Materials.emitCyan,
        ),
    )

    parts.add(
        Part(
            id = "lv_machine_deck", name = "Machine-room deck", system = LV_FRAME, assembly = LV_MACHINE,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.slab(2.52f, 1.72f, 0.14f, cornerRadius = 0.08f, bevel = 0.025f, segments = 3),
                    Mat4.translation(V3(0f, 3.02f, 0f)),
                )
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(2.20f, 0.07f, 0.10f), Mat4.translation(V3(0f, 2.94f, side * 0.72f)))
                }
            }.build(),
            material = Materials.bedPlate,
        ),
    )
    parts.add(
        Part(
            id = "lv_machine_tread", name = "Tread plate and machine rails", system = LV_FRAME, assembly = LV_MACHINE,
            mesh = MeshBuilder().apply {
                for (i in 0..10) {
                    addMesh(Geo.box(2.24f, 0.018f, 0.045f), Mat4.translation(V3(0f, 3.101f, -0.72f + i * 0.14f)))
                }
                listOf(-1f, 1f).forEach { side ->
                    val x = side * 1.18f
                    addMesh(Geo.cylinderBetween(V3(x, 3.10f, -0.76f), V3(x, 3.62f, -0.76f), 0.018f, 6))
                    addMesh(Geo.cylinderBetween(V3(x, 3.10f, 0.76f), V3(x, 3.62f, 0.76f), 0.018f, 6))
                    addMesh(Geo.cylinderBetween(V3(x, 3.62f, -0.76f), V3(x, 3.62f, 0.76f), 0.018f, 6))
                }
            }.build(),
            material = Materials.nickel,
        ),
    )

    parts.add(
        Part(
            id = "lv_sheave", name = "Traction sheave", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = sheaveMesh(0.33f, 0.15f), material = Materials.nickel,
            origin = V3(0.10f, SHEAVE_Y, 0f),
            animate = { ctx -> Mat4.rotateZ(-liftProgress(ctx) * 300f) },
        ),
    )
    parts.add(
        Part(
            id = "lv_sheave_grooves", name = "Rope grooves", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = sheaveGrooveMesh(0.33f, 0.15f), material = Materials.graphite,
            parent = "lv_sheave",
        ),
    )
    parts.add(
        Part(
            id = "lv_idler", name = "Counterweight idler", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = sheaveMesh(0.23f, 0.12f), material = Materials.nickel,
            origin = V3(-1.15f, SHEAVE_Y, 0f),
            animate = { ctx -> Mat4.rotateZ(-liftProgress(ctx) * 430f) },
        ),
    )
    parts.add(
        Part(
            id = "lv_idler_grooves", name = "Idler grooves", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = sheaveGrooveMesh(0.23f, 0.12f), material = Materials.graphite,
            parent = "lv_idler",
        ),
    )
    parts.add(
        Part(
            id = "lv_motor", name = "Gearless traction motor", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.cylinder(0.19f, 0.54f, 24, bevel = 0.02f).transformed(Mat4.rotateZ(90f)),
                    Mat4.translation(V3(-0.62f, 3.49f, -0.40f)),
                )
                for (i in 0..6) {
                    addMesh(
                        Geo.torus(0.198f, 0.012f, 24, 4).transformed(Mat4.rotateZ(90f)),
                        Mat4.translation(V3(-0.86f + i * 0.08f, 3.49f, -0.40f)),
                    )
                }
                addMesh(Geo.box(0.28f, 0.30f, 0.30f), Mat4.translation(V3(-0.20f, 3.44f, -0.40f)))
                addMesh(Geo.box(0.10f, 0.10f, 0.36f), Mat4.translation(V3(-0.18f, 3.38f, -0.20f)))
            }.build(),
            material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "lv_motor_trim", name = "Motor terminals and brake", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.16f, 0.12f, 0.16f), Mat4.translation(V3(-0.42f, 3.70f, -0.40f)))
                addMesh(Geo.cylinder(0.15f, 0.035f, 20).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(-0.28f, 3.49f, -0.40f)))
            }.build(),
            material = Materials.copperBright,
        ),
    )
    parts.add(
        Part(
            id = "lv_deflector", name = "Deflector beam", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(1.30f, 0.055f, 0.065f), Mat4.translation(V3(-0.52f, 3.35f, -0.13f)))
                listOf(0.10f, -1.15f).forEach { x ->
                    addMesh(Geo.box(0.10f, 0.12f, 0.10f), Mat4.translation(V3(x, 3.30f, -0.13f)))
                }
            }.build(),
            material = Materials.graphite,
        ),
    )

    parts.add(
        Part(
            id = "lv_car", name = "Lift car", system = LV_CABIN, assembly = LV_CAR,
            mesh = carBodyMesh(), material = Materials.paintCream,
            origin = V3(0f, CAR_LOW, 0f),
            animate = { ctx -> Mat4.translation(V3(0f, CAR_TRAVEL * liftProgress(ctx), 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_car_accent", name = "Car waistline", system = LV_CABIN, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.022f, 0.19f, 0.72f), Mat4.translation(V3(side * 0.578f, -0.20f, 0f)))
                }
                addMesh(Geo.box(0.98f, 0.16f, 0.022f), Mat4.translation(V3(0f, -0.20f, 0.398f)))
            }.build(),
            material = Materials.accentPaint, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_glass", name = "Car side glazing", system = LV_CABIN, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.012f, 0.275f, 0.55f), Mat4.translation(V3(side * 0.580f, 0.075f, 0f)))
                }
            }.build(),
            material = Materials.glassDark, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_interior", name = "Car interior fittings", system = LV_CABIN, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.68f, 0.46f, 0.014f), Mat4.translation(V3(0f, 0.02f, -0.365f)))
                addMesh(Geo.cylinderBetween(V3(-0.44f, 0.08f, -0.31f), V3(0.44f, 0.08f, -0.31f), 0.020f, 6))
                addMesh(Geo.box(0.18f, 0.26f, 0.025f), Mat4.translation(V3(0.38f, 0.02f, -0.34f)))
                addMesh(Geo.box(0.68f, 0.015f, 0.36f), Mat4.translation(V3(0f, 0.345f, 0f)))
            }.build(),
            material = Materials.darkSteel, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_light", name = "Car ceiling light", system = LV_CABIN, assembly = LV_CAR,
            mesh = Geo.box(0.46f, 0.014f, 0.24f), material = Materials.emitWhite,
            origin = V3(0f, 0.354f, 0f), parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_buttons", name = "Car operating panel", system = LV_CONTROL, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                for (i in 0 until 4) {
                    addMesh(Geo.cylinder(0.018f, 0.012f, 10).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0.38f, 0.09f - i * 0.045f, -0.323f)))
                }
            }.build(),
            material = Materials.emitAmber, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_header", name = "Car floor indicator", system = LV_CONTROL, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.24f, 0.055f, 0.020f), Mat4.translation(V3(0f, 0.342f, 0.438f)))
                addMesh(Geo.box(0.14f, 0.014f, 0.006f), Mat4.translation(V3(0f, 0.342f, 0.450f)))
            }.build(),
            material = Materials.emitCyan, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_crosshead", name = "Car crosshead and cable hitch", system = LV_TRACTION, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.88f, 0.07f, 0.12f), Mat4.translation(V3(0f, 0.45f, 0f)))
                listOf(-0.18f, 0f, 0.18f).forEach { x ->
                    addMesh(Geo.box(0.055f, 0.10f, 0.06f), Mat4.translation(V3(x, 0.49f, 0f)))
                    addMesh(Geo.torus(0.030f, 0.010f, 12, 4).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(x, 0.50f, 0f)))
                }
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.055f, 0.26f, 0.06f), Mat4.translation(V3(side * 0.50f, 0.30f, 0f)))
                }
            }.build(),
            material = Materials.nickel, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_car_shoes", name = "Car guide shoes", system = LV_CABIN, assembly = LV_CAR,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { sx ->
                    listOf(-1f, 1f).forEach { sz ->
                        addMesh(Geo.box(0.085f, 0.13f, 0.095f), Mat4.translation(V3(sx * 0.63f, 0f, sz * 0.49f)))
                    }
                }
                addMesh(Geo.box(0.76f, 0.06f, 0.10f), Mat4.translation(V3(0f, -0.405f, 0f)))
            }.build(),
            material = Materials.graphite, parent = "lv_car",
        ),
    )
    parts.add(
        Part(
            id = "lv_door_left", name = "Left car door", system = LV_CABIN, assembly = LV_CAR,
            mesh = doorGlassMesh(), material = Materials.glassDark,
            origin = V3(-0.285f, 0f, 0.444f), parent = "lv_car",
            animate = { ctx -> Mat4.translation(V3(-0.185f * doorOpening(liftProgress(ctx)), 0f, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_door_right", name = "Right car door", system = LV_CABIN, assembly = LV_CAR,
            mesh = doorGlassMesh(), material = Materials.glassDark,
            origin = V3(0.285f, 0f, 0.444f), parent = "lv_car",
            animate = { ctx -> Mat4.translation(V3(0.185f * doorOpening(liftProgress(ctx)), 0f, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_door_left_frame", name = "Left door frame", system = LV_CABIN, assembly = LV_CAR,
            mesh = doorFrameMesh(0.20f), material = Materials.nickel,
            origin = V3(-0.285f, 0f, 0.444f), parent = "lv_car",
            animate = { ctx -> Mat4.translation(V3(-0.185f * doorOpening(liftProgress(ctx)), 0f, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_door_right_frame", name = "Right door frame", system = LV_CABIN, assembly = LV_CAR,
            mesh = doorFrameMesh(-0.20f), material = Materials.nickel,
            origin = V3(0.285f, 0f, 0.444f), parent = "lv_car",
            animate = { ctx -> Mat4.translation(V3(0.185f * doorOpening(liftProgress(ctx)), 0f, 0f)) },
        ),
    )

    parts.add(
        Part(
            id = "lv_cables_car", name = "Car suspension ropes", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = ropeBundle(floatArrayOf(-0.18f, 0f, 0.18f)), material = Materials.nickel,
            origin = V3(0f, SHEAVE_Y, 0f),
            animate = { ctx -> Mat4.scale(V3(1f, carCableLength(liftProgress(ctx)), 1f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_cables_counterweight", name = "Counterweight rope", system = LV_TRACTION, assembly = LV_MACHINE,
            mesh = ropeBundle(floatArrayOf(0f)), material = Materials.nickel,
            origin = V3(-1.15f, SHEAVE_Y, 0f),
            animate = { ctx -> Mat4.scale(V3(1f, counterweightCableLength(liftProgress(ctx)), 1f)) },
        ),
    )

    parts.add(
        Part(
            id = "lv_counterweight", name = "Counterweight stack", system = LV_TRACTION, assembly = LV_CW,
            mesh = counterweightMesh(), material = Materials.graphite,
            origin = V3(-1.15f, CW_LOW, 0f),
            animate = { ctx -> Mat4.translation(V3(0f, (1f - liftProgress(ctx)) * CW_TRAVEL, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_counterweight_trim", name = "Counterweight guide shoes", system = LV_TRACTION, assembly = LV_CW,
            mesh = counterweightTrimMesh(), material = Materials.nickel,
            parent = "lv_counterweight",
        ),
    )
    parts.add(
        Part(
            id = "lv_cw_hitch", name = "Counterweight rope hitch", system = LV_TRACTION, assembly = LV_CW,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.18f, 0.06f, 0.10f), Mat4.translation(V3(0f, 0.40f, 0f)))
                addMesh(Geo.torus(0.030f, 0.010f, 12, 4).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0.42f, 0f)))
            }.build(),
            material = Materials.nickel, parent = "lv_counterweight",
        ),
    )

    parts.add(
        Part(
            id = "lv_buffers", name = "Car and counterweight buffers", system = LV_CONTROL, assembly = LV_SHAFT,
            mesh = MeshBuilder().apply {
                listOf(-0.32f, 0.32f).forEach { x ->
                    addMesh(Geo.cylinder(0.055f, 0.18f, 12), Mat4.translation(V3(x, 0.40f, 0f)))
                    addMesh(Geo.sphere(0.055f, 12, 6), Mat4.translation(V3(x, 0.49f, 0f)))
                }
                addMesh(Geo.cylinder(0.055f, 0.16f, 12), Mat4.translation(V3(-1.15f, 0.39f, 0f)))
                addMesh(Geo.sphere(0.055f, 12, 6), Mat4.translation(V3(-1.15f, 0.47f, 0f)))
            }.build(),
            material = Materials.paintOrange,
        ),
    )

    // The passenger is a procedural humanoid. It is deliberately small enough to
    // clear the car header and door opening, but still reads as a figure at the
    // default camera distance.
    parts.add(
        Part(
            id = "lv_visitor_root", name = "Humanoid visitor", system = LV_CABIN, assembly = LV_VISITOR,
            mesh = Mesh.EMPTY, material = Materials.paintCream,
            scale = V3(1.06f, 1.06f, 1.06f),
            animate = { ctx ->
                val pose = visitorPose(ctx)
                Mat4.translation(V3(pose.x, pose.y, pose.z)).multiply(Mat4.rotateY(pose.yaw))
            },
        ),
    )
    parts.add(
        Part(
            id = "lv_visitor_torso", name = "Visitor torso", system = LV_CABIN, assembly = LV_VISITOR,
            mesh = visitorTorsoMesh(), material = Materials.paintOrange, parent = "lv_visitor_root",
        ),
    )
    parts.add(
        Part(
            id = "lv_visitor_head", name = "Visitor head", system = LV_CABIN, assembly = LV_VISITOR,
            mesh = visitorHeadMesh(), material = Materials.paintCream,
            origin = V3(0f, 0.45f, 0f), parent = "lv_visitor_root",
            animate = { ctx -> Mat4.rotateY(4f * sin(ctx.machineTime * 1.2f)) },
        ),
    )
    parts.add(
        Part(
            id = "lv_visitor_visor", name = "Visitor face sensor", system = LV_CABIN, assembly = LV_VISITOR,
            mesh = Geo.box(0.09f, 0.035f, 0.012f), material = Materials.emitCyan,
            origin = V3(0f, 0.005f, 0.071f), parent = "lv_visitor_head",
        ),
    )
    parts.add(
        Part(
            id = "lv_visitor_chest", name = "Visitor chest plate", system = LV_CABIN, assembly = LV_VISITOR,
            mesh = Geo.box(0.15f, 0.03f, 0.012f), material = Materials.accentPaint,
            origin = V3(0f, 0.335f, 0.096f), parent = "lv_visitor_root",
        ),
    )
    parts.add(
        Part(
            id = "lv_visitor_pack", name = "Visitor service pack", system = LV_CABIN, assembly = LV_VISITOR,
            mesh = Geo.box(0.15f, 0.16f, 0.09f), material = Materials.graphite,
            origin = V3(0f, 0.30f, -0.11f), parent = "lv_visitor_root",
        ),
    )
    listOf(-1f, 1f).forEach { side ->
        val suffix = if (side < 0f) "l" else "r"
        parts.add(
            Part(
                id = "lv_visitor_arm_$suffix", name = "Visitor ${suffix.uppercase()} upper arm",
                system = LV_CABIN, assembly = LV_VISITOR,
                mesh = visitorLimbMesh(0.10f, 0.025f), material = Materials.paintSlate,
                origin = V3(side * 0.165f, 0.38f, 0f), parent = "lv_visitor_root",
                animate = { ctx -> Mat4.rotateX(-side * visitorPose(ctx).stride * 22f) },
            ),
        )
        parts.add(
            Part(
                id = "lv_visitor_forearm_$suffix", name = "Visitor ${suffix.uppercase()} forearm",
                system = LV_CABIN, assembly = LV_VISITOR,
                mesh = visitorLimbMesh(0.09f, 0.021f), material = Materials.darkSteel,
                origin = V3(0f, -0.10f, 0f), parent = "lv_visitor_arm_$suffix",
                animate = { ctx ->
                    val stride = visitorPose(ctx).stride
                    Mat4.rotateX(-10f - (side * -stride).coerceAtLeast(0f) * 14f)
                },
            ),
        )
        parts.add(
            Part(
                id = "lv_visitor_hand_$suffix", name = "Visitor ${suffix.uppercase()} hand",
                system = LV_CABIN, assembly = LV_VISITOR,
                mesh = Geo.sphere(0.032f, 10, 6), material = Materials.nickel,
                origin = V3(0f, -0.09f, 0f), parent = "lv_visitor_forearm_$suffix",
            ),
        )
        parts.add(
            Part(
                id = "lv_visitor_thigh_$suffix", name = "Visitor ${suffix.uppercase()} thigh",
                system = LV_CABIN, assembly = LV_VISITOR,
                mesh = visitorLimbMesh(0.12f, 0.028f), material = Materials.paintSlate,
                origin = V3(side * 0.07f, 0.23f, 0f), parent = "lv_visitor_root",
                animate = { ctx -> Mat4.rotateX(side * -visitorPose(ctx).stride * 22f) },
            ),
        )
        parts.add(
            Part(
                id = "lv_visitor_shin_$suffix", name = "Visitor ${suffix.uppercase()} shin",
                system = LV_CABIN, assembly = LV_VISITOR,
                mesh = visitorLimbMesh(0.10f, 0.024f), material = Materials.darkSteel,
                origin = V3(0f, -0.12f, 0f), parent = "lv_visitor_thigh_$suffix",
                animate = { ctx ->
                    val bend = (-side * visitorPose(ctx).stride).coerceAtLeast(0f)
                    Mat4.rotateX(-8f - bend * 16f)
                },
            ),
        )
        parts.add(
            Part(
                id = "lv_visitor_foot_$suffix", name = "Visitor ${suffix.uppercase()} foot",
                system = LV_CABIN, assembly = LV_VISITOR,
                mesh = visitorFootMesh(), material = Materials.paintSlate,
                origin = V3(0f, -0.10f, 0f), parent = "lv_visitor_shin_$suffix",
            ),
        )
    }

    val hotspots = listOf(
        Hotspot(
            "car", "The car",
            "The cabin is a rigid box on four guide shoes. It carries the doors, the light, the control panel and the crosshead where the ropes terminate.",
            "lv_car", V3(0f, 0.08f, 0.30f),
            CameraPose(28f, 12f, 1.34f), readoutLabel = "CABIN",
        ),
        Hotspot(
            "doors", "Centre-opening doors",
            "Two glass leaves slide apart whenever the car reaches a landing. The door operator is mounted on the car roof, so the landing needs no mechanism.",
            "lv_door_left", V3(0f, -0.04f, 0.02f),
            CameraPose(18f, 8f, 1.42f), readoutLabel = "DOORS",
        ),
        Hotspot(
            "sheave", "Traction sheave",
            "The grooved sheave is the whole machine: the motor turns it, friction lifts one rope run and lowers the other. There is no drum to wind the cable onto.",
            "lv_sheave", V3(0f, 0f, 0.12f),
            CameraPose(28f, 32f, 1.30f), readoutLabel = "TRACTION",
        ),
        Hotspot(
            "counterweight", "Counterweight",
            "The counterweight weighs roughly the car plus half its rated load. It travels the opposite way and keeps the motor from lifting the full mass.",
            "lv_counterweight", V3(0f, 0.18f, 0.18f),
            CameraPose(142f, 18f, 1.22f), readoutLabel = "BALANCE",
        ),
        Hotspot(
            "guides", "Guide rails",
            "Four machined rails constrain the car to a vertical line. Their faces are ground flat; the guide shoes slide on them and absorb any sway.",
            "lv_guides", V3(0.69f, 1.45f, 0.50f),
            CameraPose(66f, 18f, 1.20f), readoutLabel = "GUIDANCE",
        ),
        Hotspot(
            "gearless", "Gearless motor",
            "A permanent-magnet motor turns the sheave directly. No gearbox means less noise, less oil and precise floor levelling.",
            "lv_motor", V3(-0.40f, 0.12f, 0.08f),
            CameraPose(42f, 30f, 1.26f), readoutLabel = "DRIVE",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Build the shaft",
            "Four steel posts and a rear guard form the hoistway. The guide rails are aligned to a few tenths of a millimetre so the car can travel without touching anything else.",
            LV_SHAFT, CameraPose(34f, 20f, 1.02f), 0.10f, 0.20f,
        ),
        GuideStep(
            "Hang the car",
            "The car rides on four guide shoes. Its crosshead spreads the rope load across the roof, and the safety gear sits just below the floor.",
            LV_CAR, CameraPose(26f, 14f, 1.24f), 0.22f, 0.42f,
        ),
        GuideStep(
            "Balance the load",
            "The counterweight moves in the opposite direction. When the car is at the top, the weight is at the bottom, and the motor only has to overcome friction and imbalance.",
            LV_CW, CameraPose(144f, 20f, 1.16f), 0.22f, 0.58f,
        ),
        GuideStep(
            "Turn the sheave",
            "The gearless motor drives the grooved sheave. Rope friction does the lifting; the idler redirects the counterweight run.",
            LV_MACHINE, CameraPose(34f, 30f, 1.12f), 0.18f, 0.72f,
        ),
        GuideStep(
            "Open the doors",
            "At each landing the car doors part and the landing threshold lines up with the cabin floor. The light in the ceiling and the indicator above the doors tell the passenger the car is ready.",
            LV_CABIN, CameraPose(18f, 8f, 1.36f), 0.08f, 0.84f,
        ),
        GuideStep(
            "A passenger rides",
            "The humanoid visitor walks through the open doors while the car waits at the lobby. The lift closes up, carries the visitor to the top, and opens again when it comes back down.",
            LV_CABIN, CameraPose(24f, 10f, 1.26f), 0f, 0.40f,
        ),
        GuideStep(
            "The assembly",
            "Explode the model to separate the plinth, shaft, machine room, car and counterweight. The ropes stay with the machine room while the car and weight travel on their guides.",
            null, CameraPose(38f, 24f, 0.98f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "lift_system",
        index = 17,
        title = "VERTICA L-9",
        designation = "VL-9 / TRACTION LIFT",
        category = "Architecture",
        categoryShort = "ARC",
        tagline = "Up, down, and back again.",
        summary = "A miniature traction lift in cutaway: a working car, a humanoid visitor who walks in and rides to the top, four guide rails, a counterweight and the grooved sheave that moves both.",
        details = listOf(
            "VERTICA L-9 is a working section through a traction lift. A humanoid passenger walks in while the car waits at the lobby, rides to the top, and walks back out when the car returns. The doors part at both ends of the run, and the counterweight travels the opposite way on its own guides.",
            "There is no drum and no gearbox. A permanent-magnet motor turns a grooved sheave; friction pulls the car ropes one way and the counterweight rope the other. The weight of the car is balanced by the weight of the stack, so the motor mostly fights friction and imbalance.",
        ),
        specs = listOf(
            SpecRow("Rated load", "630 kg"),
            SpecRow("Travel", "18 m / 4 stops"),
            SpecRow("Rated speed", "1.6 m/s"),
            SpecRow("Motor", "7.5 kW gearless"),
            SpecRow("Ropes", "3 × 8 mm"),
            SpecRow("Passenger", "1 humanoid"),
        ),
        tags = listOf("lift", "elevator", "traction", "tower", "cabin", "counterweight", "architecture", "vertical"),
        accent = 0xFFE4B45C.toInt(),
        control = ControlSpec(
            label = "TRAVEL",
            lowLabel = "LOBBY",
            highLabel = "ROOF",
            defaultValue = 0.55f,
            hint = "Car cycle speed, door timing and sheave rotation",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(36f, 14f, 1.18f),
        thumbnailPose = CameraPose(38f, 16f, 1.20f),
        thumbnailTime = 2.8f,
        thumbnailControl = 0.62f,
        readouts = { ctx ->
            listOf(
                Readout("STOPS", "04", "", accent = true),
                Readout("SPEED", "1.6", "m/s"),
                Readout("LOAD", "630", "kg"),
            )
        },
        effects = { ctx ->
            val p = liftProgress(ctx)
            val visitor = visitorPose(ctx)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.72f, 0xFFE4B45C.toInt(), 0f, 0.18f),
                SceneEffect.Glow(V3(0f, CAR_LOW + CAR_TRAVEL * p + 0.35f, 0f), 0.30f, 0xFFFFE0A8.toInt(), 0.32f),
                SceneEffect.Glow(V3(0.10f, 3.40f, 0.10f), 0.22f, 0xFFE4B45C.toInt(), 0.20f),
                SceneEffect.Glow(
                    V3(visitor.x, visitor.y + 0.46f, visitor.z + 0.04f),
                    0.14f,
                    0xFF6EF2DF.toInt(),
                    0.28f,
                ),
            )
        },
    )
}
