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
import studio.anatomy.app.render.Material
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder
import studio.anatomy.app.render.SurfacePattern
import kotlin.math.sin

private const val MV_FRAME = "GUIDEWAY"
private const val MV_TUBE = "VACUUM TUBE"
private const val MV_POD = "PASSENGER POD"
private const val MV_LEV = "LEVITATION"
private const val MV_STATION = "STATION"
private const val MV_POWER = "POWER & CONTROL"

private const val MV_PLINTH = "PLINTH"
private const val MV_GUIDE = "GUIDEWAY"
private const val MV_VESSEL = "VACUUM VESSEL"
private const val MV_CAPSULE = "CAPSULE"
private const val MV_TERMINAL = "TERMINAL"
private const val MV_ENERGY = "ENERGY"

private val mvCeramic = Material(
    "maglevCeramic", "Glazed ceramic", 0xFFE7DDC7.toInt(), specular = 0.38f, shininess = 26f,
    fillLight = 0.09f, rimLight = 0.15f, patina = 0.020f, edgeWear = 0.04f,
    pattern = SurfacePattern.SPECKLE, patternStrength = 0.16f,
)
private val mvTeal = Material(
    "maglevTeal", "Transit teal", 0xFF2A8E91.toInt(), specular = 0.52f, shininess = 32f,
    fillLight = 0.09f, rimLight = 0.16f, patina = 0.030f,
    pattern = SurfacePattern.PANEL, patternStrength = 0.28f,
)
private val mvGold = Material(
    "maglevGold", "Contact rail", 0xFFD1A259.toInt(), specular = 0.68f, shininess = 38f,
    fillLight = 0.10f, rimLight = 0.18f, patina = 0.045f, edgeWear = 0.05f,
    pattern = SurfacePattern.BRUSHED, patternStrength = 0.28f,
)
private val mvWindow = Material(
    "maglevWindow", "Smoked capsule glass", 0xFF284C57.toInt(), specular = 0.78f, shininess = 62f,
    alpha = 90,
)
private val mvBeacon = Material(
    "maglevBeacon", "Transit beacon", 0xFF75F5E7.toInt(), emissive = true, specular = 0f,
)

private fun mvPodRide(ctx: AnimContext): Mat4 {
    val t = ctx.machineTime
    val glide = sin(t * 1.05f)
    val bob = 0.020f * sin(t * 3.8f + 0.4f)
    val pitch = 1.8f * sin(t * 1.05f + 0.7f)
    val yaw = 1.1f * sin(t * 0.55f)
    return Mat4.translation(V3(0.68f * glide, bob, 0f))
        .multiply(Mat4.rotateZ(pitch))
        .multiply(Mat4.rotateY(yaw))
}

private fun mvTrackMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.088f, 3.04f, 16, bevel = 0.012f).transformed(Mat4.rotateZ(-90f)))
    addMesh(Geo.cylinder(0.040f, 3.10f, 12).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0f, 0.18f, 0f)))
    for (side in listOf(-1f, 1f)) {
        addMesh(Geo.box(3.08f, 0.038f, 0.060f), Mat4.translation(V3(0f, -0.09f, side * 0.30f)))
        addMesh(Geo.box(3.08f, 0.018f, 0.022f), Mat4.translation(V3(0f, 0.06f, side * 0.31f)))
    }
    for (i in 0 until 8) {
        val x = -1.42f + i * 0.405f
        addMesh(Geo.box(0.12f, 0.10f, 0.72f), Mat4.translation(V3(x, 0.02f, 0f)))
        addMesh(Geo.hexBolt(0.030f, 0.025f), Mat4.translation(V3(x, 0.10f, 0.31f)))
        addMesh(Geo.hexBolt(0.030f, 0.025f), Mat4.translation(V3(x, 0.10f, -0.31f)))
    }
}.build()

private fun mvTubeMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.405f, 3.10f, 36, bevel = 0.018f).transformed(Mat4.rotateZ(-90f)))
    for (i in 0 until 9) {
        val x = -1.48f + i * 0.37f
        addMesh(
            Geo.torus(0.435f, 0.022f, 34, 4).transformed(Mat4.rotateZ(-90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
        addMesh(
            Geo.torus(0.415f, 0.010f, 34, 4).transformed(Mat4.rotateZ(-90f)),
            Mat4.translation(V3(x + 0.030f, 0f, 0f)),
        )
    }
    // The longitudinal service spine is visible outside the glass.
    addMesh(Geo.box(3.10f, 0.035f, 0.045f), Mat4.translation(V3(0f, -0.41f, 0.20f)))
    for (i in 0 until 5) {
        addMesh(Geo.cylinder(0.020f, 0.07f, 8), Mat4.translation(V3(-1.18f + i * 0.59f, -0.45f, 0.20f)))
    }
}.build()

private fun mvPodMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.lathe(
            listOf(
                0.055f to -0.66f,
                0.19f to -0.55f,
                0.285f to -0.36f,
                0.315f to -0.12f,
                0.315f to 0.12f,
                0.285f to 0.36f,
                0.19f to 0.55f,
                0.055f to 0.66f,
            ),
            30, capBottom = false, capTop = false,
        ).transformed(Mat4.rotateZ(-90f)),
    )
    addMesh(Geo.torus(0.20f, 0.018f, 20, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-0.52f, 0f, 0f)))
    addMesh(Geo.torus(0.20f, 0.018f, 20, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0.52f, 0f, 0f)))
    addMesh(Geo.box(0.58f, 0.018f, 0.035f), Mat4.translation(V3(0f, 0.31f, 0.14f)))
    addMesh(Geo.box(0.58f, 0.018f, 0.035f), Mat4.translation(V3(0f, 0.31f, -0.14f)))
}.build()

private fun mvPodWindowMesh(): Mesh = MeshBuilder().apply {
    // Long canopy panes follow the capsule shoulder.
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until 3) {
            addMesh(
                Geo.slab(0.17f, 0.12f, 0.012f, cornerRadius = 0.03f, bevel = 0.004f, segments = 2),
                Mat4.translation(V3(-0.23f + i * 0.23f, 0.19f, side * 0.155f))
                    .multiply(Mat4.rotateX(side * 16f)),
            )
        }
    }
    addMesh(Geo.box(0.42f, 0.014f, 0.15f), Mat4.translation(V3(0f, 0.31f, 0f)))
}.build()

private fun mvPodSkids(): Mesh = MeshBuilder().apply {
    for (side in listOf(-1f, 1f)) {
        addMesh(Geo.slab(0.78f, 0.075f, 0.065f, cornerRadius = 0.030f, bevel = 0.008f), Mat4.translation(V3(0f, -0.30f, side * 0.20f)))
        addMesh(Geo.box(0.56f, 0.018f, 0.018f), Mat4.translation(V3(0f, -0.34f, side * 0.20f)))
    }
}.build()

private fun mvStatorMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 8) {
        val x = -1.26f + i * 0.36f
        addMesh(Geo.torus(0.452f, 0.032f, 32, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(x, 0f, 0f)))
        for (side in listOf(-1f, 1f)) {
            addMesh(
                Geo.box(0.080f, 0.16f, 0.11f),
                Mat4.translation(V3(x, -0.02f, side * 0.44f)),
            )
            addMesh(
                Geo.cylinder(0.035f, 0.10f, 10).transformed(Mat4.rotateX(90f)),
                Mat4.translation(V3(x, 0f, side * 0.52f)),
            )
        }
    }
}.build()

private fun mvPylonMesh(): Mesh = MeshBuilder().apply {
    for (x in listOf(-1.16f, 0f, 1.16f)) {
        addMesh(Geo.box(0.20f, 0.055f, 0.38f), Mat4.translation(V3(x, 0.04f, 0f)))
        for (side in listOf(-1f, 1f)) {
            addMesh(
                Geo.cylinderBetween(
                    V3(x + side * 0.26f, 0.06f, side * 0.26f),
                    V3(x + side * 0.12f, 0.85f, 0f),
                    0.026f, 7,
                ),
            )
        }
        addMesh(Geo.cylinderBetween(V3(x, 0.80f, 0f), V3(x, 1.02f, 0f), 0.045f, 10))
        addMesh(Geo.torus(0.075f, 0.010f, 14, 4), Mat4.translation(V3(x, 0.94f, 0f)))
    }
}.build()

private fun mvStationMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.52f, 0.82f, 0.08f, cornerRadius = 0.055f, bevel = 0.010f), Mat4.translation(V3(-1.27f, 0.37f, 0.48f)))
    addMesh(Geo.slab(0.42f, 0.64f, 0.20f, cornerRadius = 0.025f, bevel = 0.008f), Mat4.translation(V3(-1.27f, 0.50f, 0.48f)))
    for (side in listOf(-1f, 1f)) {
        addMesh(Geo.cylinderBetween(V3(-1.47f, 0.58f, side * 0.70f), V3(-1.47f, 1.12f, side * 0.70f), 0.026f, 8))
        addMesh(Geo.cylinderBetween(V3(-1.07f, 0.58f, side * 0.70f), V3(-1.07f, 1.12f, side * 0.70f), 0.026f, 8))
    }
    addMesh(Geo.slab(0.60f, 0.96f, 0.06f, cornerRadius = 0.035f, bevel = 0.010f), Mat4.translation(V3(-1.27f, 1.17f, 0.48f)))
    addMesh(Geo.box(0.06f, 0.32f, 0.06f), Mat4.translation(V3(-1.52f, 0.38f, 0.22f)))
    addMesh(Geo.box(0.06f, 0.32f, 0.06f), Mat4.translation(V3(-1.02f, 0.38f, 0.22f)))
}.build()

private fun mvCanopyMesh(): Mesh = MeshBuilder().apply {
    for (i in 0..2) {
        val x = -1.52f + i * 0.25f
        addMesh(
            Geo.slab(0.23f, 0.86f, 0.025f, cornerRadius = 0.012f, bevel = 0.004f, segments = 2),
            Mat4.translation(V3(x, 1.26f, 0.48f)).multiply(Mat4.rotateZ(-14f)),
        )
    }
    for (side in listOf(-1f, 1f)) {
        addMesh(Geo.cylinderBetween(V3(-1.55f, 1.14f, side * 0.78f), V3(-1.00f, 1.32f, side * 0.78f), 0.014f, 6))
    }
}.build()

private fun mvStationGlassMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.22f, 0.18f, 0.012f), Mat4.translation(V3(-1.51f, 0.92f, 0.77f)))
    addMesh(Geo.box(0.22f, 0.18f, 0.012f), Mat4.translation(V3(-1.03f, 0.92f, 0.77f)))
    for (i in 0 until 3) {
        addMesh(Geo.box(0.08f, 0.17f, 0.012f), Mat4.translation(V3(-1.43f + i * 0.16f, 0.61f, 0.79f)))
    }
    addMesh(Geo.box(0.30f, 0.014f, 0.012f), Mat4.translation(V3(-1.27f, 0.84f, 0.79f)))
}.build()

private fun mvStationSignMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.30f, 0.075f, 0.18f, cornerRadius = 0.015f, bevel = 0.006f), Mat4.translation(V3(-1.27f, 1.04f, 0.22f)))
    addMesh(Geo.box(0.19f, 0.014f, 0.012f), Mat4.translation(V3(-1.27f, 1.06f, 0.313f)))
    addMesh(Geo.box(0.12f, 0.010f, 0.012f), Mat4.translation(V3(-1.27f, 1.01f, 0.313f)))
}.build()

private fun mvAirlockMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.54f, 0.040f, 34, 6).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-1.54f, 0f, 0f)))
    addMesh(Geo.cylinder(0.50f, 0.12f, 32, bevel = 0.016f).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-1.51f, 0f, 0f)))
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val y = (0.47f * kotlin.math.cos(a)).toFloat()
        val z = (0.47f * kotlin.math.sin(a)).toFloat()
        addMesh(Geo.hexBolt(0.032f, 0.028f), Mat4.translation(V3(-1.58f, y, z)))
    }
}.build()

private fun mvBatteryMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 3) {
        val x = -0.91f + i * 0.27f
        addMesh(Geo.slab(0.22f, 0.36f, 0.62f, cornerRadius = 0.03f, bevel = 0.010f), Mat4.translation(V3(x, 0.63f, -0.58f)))
        addMesh(Geo.box(0.12f, 0.12f, 0.012f), Mat4.translation(V3(x, 0.71f, -0.394f)))
        addMesh(Geo.box(0.12f, 0.025f, 0.012f), Mat4.translation(V3(x, 0.59f, -0.394f)))
        addMesh(Geo.box(0.05f, 0.04f, 0.012f), Mat4.translation(V3(x + 0.05f, 0.51f, -0.394f)))
    }
}.build()

private fun mvVacuumMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.19f, 0.56f, 20, bevel = 0.02f), Mat4.translation(V3(1.40f, 0.62f, -0.55f)))
    addMesh(Geo.torus(0.19f, 0.014f, 22, 4), Mat4.translation(V3(1.40f, 0.90f, -0.55f)))
    addMesh(Geo.cylinder(0.075f, 0.16f, 12), Mat4.translation(V3(1.40f, 0.32f, -0.55f)))
    addMesh(Geo.tube(listOf(V3(1.40f, 0.89f, -0.55f), V3(1.43f, 1.10f, -0.40f), V3(1.42f, 1.22f, -0.12f)), 0.022f, 6, subdivide = 3))
    addMesh(Geo.tube(listOf(V3(1.40f, 0.35f, -0.55f), V3(1.20f, 0.39f, -0.55f), V3(0.95f, 0.40f, -0.55f)), 0.018f, 6, subdivide = 2))
}.build()

private fun mvControlTowerMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.34f, 0.34f, 0.56f, cornerRadius = 0.025f, bevel = 0.010f), Mat4.translation(V3(1.28f, 0.58f, 0.55f)))
    addMesh(Geo.box(0.24f, 0.15f, 0.014f), Mat4.translation(V3(1.28f, 0.76f, 0.725f)))
    addMesh(Geo.box(0.14f, 0.014f, 0.014f), Mat4.translation(V3(1.28f, 0.67f, 0.725f)))
    addMesh(Geo.cylinderBetween(V3(1.28f, 0.86f, 0.55f), V3(1.28f, 1.42f, 0.55f), 0.014f, 6))
    addMesh(Geo.torus(0.10f, 0.012f, 16, 4), Mat4.translation(V3(1.28f, 1.42f, 0.55f)))
    addMesh(Geo.box(0.26f, 0.014f, 0.014f), Mat4.translation(V3(1.28f, 1.26f, 0.55f)))
}.build()

private fun mvCableMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.tube(listOf(V3(-1.30f, 0.33f, 0.90f), V3(-0.65f, 0.34f, 0.93f), V3(0.30f, 0.33f, 0.92f), V3(1.28f, 0.32f, 0.87f)), 0.020f, 6, subdivide = 4))
    addMesh(Geo.tube(listOf(V3(-0.92f, 0.38f, -0.41f), V3(-0.85f, 0.40f, -0.58f), V3(-0.65f, 0.38f, -0.72f)), 0.015f, 5, subdivide = 2))
    addMesh(Geo.tube(listOf(V3(1.28f, 0.35f, 0.55f), V3(1.36f, 0.45f, 0.14f), V3(1.40f, 0.40f, -0.26f), V3(1.40f, 0.41f, -0.55f)), 0.015f, 5, subdivide = 3))
}.build()

private fun mvStatorPulseMesh(): Mesh = MeshBuilder().apply {
    listOf(-0.84f, 0f, 0.84f).forEach { x ->
        addMesh(
            Geo.torus(0.462f, 0.012f, 30, 4).transformed(Mat4.rotateZ(-90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
    }
}.build()

private fun mvAirlockIrisMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.34f, 0.012f, 24, 4).transformed(Mat4.rotateZ(-90f)))
    for (i in 0 until 4) {
        addMesh(
            Geo.box(0.012f, 0.24f, 0.016f),
            Mat4.rotateX(i * 45f).multiply(Mat4.translation(V3(0f, 0.12f, 0f))),
        )
    }
}.build()

private fun mvRadarMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.024f, 0.080f, 8))
    addMesh(
        Geo.slab(0.16f, 0.022f, 0.07f, cornerRadius = 0.010f, bevel = 0.003f, segments = 2),
        Mat4.translation(V3(0f, 0.060f, 0.010f)).multiply(Mat4.rotateX(-28f)),
    )
    addMesh(Geo.torus(0.055f, 0.008f, 12, 4), Mat4.translation(V3(0f, 0.075f, 0f)))
}.build()

private fun mvVacuumRotorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.10f, 0.020f, 18, bevel = 0.004f), Mat4.translation(V3(0f, 0.015f, 0f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.018f, 0.018f, 0.085f),
            Mat4.rotateY(i * 72f).multiply(Mat4.translation(V3(0f, 0.030f, 0.048f))),
        )
    }
}.build()

fun maglevTransitSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "maglev",
            width = 3.82f,
            depth = 2.18f,
            top = 0.30f,
            system = MV_FRAME,
            assembly = MV_PLINTH,
            accentMaterial = Materials.emitCyan,
        ),
    )

    parts.add(
        Part(
            id = "mv_pylons", name = "Guideway support pylons", system = MV_FRAME, assembly = MV_GUIDE,
            mesh = mvPylonMesh(), material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "mv_track", name = "Levitation guideway", system = MV_LEV, assembly = MV_GUIDE,
            mesh = mvTrackMesh(), material = Materials.brushedSteel,
            origin = V3(0f, 1.12f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "mv_stators", name = "Linear motor stators", system = MV_LEV, assembly = MV_GUIDE,
            mesh = mvStatorMesh(), material = mvGold,
            origin = V3(0f, 1.12f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "mv_drive_pulses", name = "Travelling stator field", system = MV_LEV, assembly = MV_GUIDE,
            mesh = mvStatorPulseMesh(), material = mvBeacon,
            origin = V3(0f, 1.12f, 0f),
            animate = { ctx -> Mat4.translation(V3(0.55f * sin(ctx.machineTime * 1.3f), 0f, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "mv_tube", name = "Vacuum tube", system = MV_TUBE, assembly = MV_VESSEL,
            mesh = mvTubeMesh(), material = Materials.glass,
            origin = V3(0f, 1.12f, 0f),
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "mv_airlock", name = "Terminal airlock", system = MV_TUBE, assembly = MV_TERMINAL,
            mesh = mvAirlockMesh(), material = Materials.titanium,
            origin = V3(0f, 1.12f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "mv_airlock_iris", name = "Airlock iris", system = MV_TUBE, assembly = MV_TERMINAL,
            mesh = mvAirlockIrisMesh(), material = mvBeacon,
            origin = V3(-1.54f, 1.12f, 0f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 70f) },
        ),
    )

    parts.add(
        Part(
            id = "mv_pod", name = "Passenger capsule", system = MV_POD, assembly = MV_CAPSULE,
            mesh = mvPodMesh(), material = mvCeramic,
            origin = V3(0.15f, 1.12f, 0f),
            animate = { ctx -> mvPodRide(ctx) },
        ),
    )
    parts.add(
        Part(
            id = "mv_pod_windows", name = "Capsule canopy", system = MV_POD, assembly = MV_CAPSULE,
            mesh = mvPodWindowMesh(), material = mvWindow,
            origin = V3(0.15f, 1.12f, 0f),
            animate = { ctx -> mvPodRide(ctx) },
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "mv_pod_trim", name = "Capsule trim and contacts", system = MV_POD, assembly = MV_CAPSULE,
            mesh = MeshBuilder().apply {
                for (side in listOf(-1f, 1f)) {
                    addMesh(Geo.box(0.58f, 0.012f, 0.020f), Mat4.translation(V3(0f, 0.32f, side * 0.14f)))
                }
                addMesh(Geo.torus(0.21f, 0.010f, 18, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-0.47f, 0f, 0f)))
                addMesh(Geo.torus(0.21f, 0.010f, 18, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0.47f, 0f, 0f)))
            }.build(),
            material = mvGold,
            origin = V3(0.15f, 1.12f, 0f),
            animate = { ctx -> mvPodRide(ctx) },
        ),
    )
    parts.add(
        Part(
            id = "mv_pod_skids", name = "Superconducting skids", system = MV_LEV, assembly = MV_CAPSULE,
            mesh = mvPodSkids(), material = mvBeacon,
            origin = V3(0.15f, 1.12f, 0f),
            animate = { ctx -> mvPodRide(ctx) },
        ),
    )

    parts.add(
        Part(
            id = "mv_station", name = "Terminal platform", system = MV_STATION, assembly = MV_TERMINAL,
            mesh = mvStationMesh(), material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "mv_canopy", name = "Solar station canopy", system = MV_POWER, assembly = MV_ENERGY,
            mesh = mvCanopyMesh(), material = Materials.solarCell,
        ),
    )
    parts.add(
        Part(
            id = "mv_station_glass", name = "Platform glazing", system = MV_STATION, assembly = MV_TERMINAL,
            mesh = mvStationGlassMesh(), material = mvWindow,
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "mv_station_sign", name = "Departure board", system = MV_STATION, assembly = MV_TERMINAL,
            mesh = mvStationSignMesh(), material = mvBeacon,
        ),
    )
    parts.add(
        Part(
            id = "mv_battery", name = "Wayside energy storage", system = MV_POWER, assembly = MV_ENERGY,
            mesh = mvBatteryMesh(), material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "mv_vacuum", name = "Vacuum pumping station", system = MV_TUBE, assembly = MV_ENERGY,
            mesh = mvVacuumMesh(), material = Materials.brushedSteel,
        ),
    )
    parts.add(
        Part(
            id = "mv_vacuum_rotor", name = "Vacuum pump rotor", system = MV_TUBE, assembly = MV_ENERGY,
            mesh = mvVacuumRotorMesh(), material = mvBeacon,
            origin = V3(1.40f, 0.94f, -0.55f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 300f) },
        ),
    )
    parts.add(
        Part(
            id = "mv_control", name = "Traffic control tower", system = MV_POWER, assembly = MV_ENERGY,
            mesh = mvControlTowerMesh(), material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "mv_control_screen", name = "Control display", system = MV_POWER, assembly = MV_ENERGY,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.19f, 0.10f, 0.008f), Mat4.translation(V3(1.28f, 0.78f, 0.733f)))
                addMesh(Geo.box(0.12f, 0.010f, 0.008f), Mat4.translation(V3(1.28f, 0.71f, 0.733f)))
            }.build(),
            material = mvBeacon,
        ),
    )
    parts.add(
        Part(
            id = "mv_radar", name = "Traffic radar", system = MV_POWER, assembly = MV_ENERGY,
            mesh = mvRadarMesh(), material = Materials.titanium,
            origin = V3(1.28f, 1.43f, 0.55f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 110f) },
        ),
    )
    parts.add(
        Part(
            id = "mv_cable", name = "Power and data raceway", system = MV_POWER, assembly = MV_ENERGY,
            mesh = mvCableMesh(), material = mvGold,
        ),
    )
    parts.add(
        Part(
            id = "mv_station_lights", name = "Platform light line", system = MV_STATION, assembly = MV_TERMINAL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.46f, 0.018f, 0.018f), Mat4.translation(V3(-1.27f, 1.12f, 0.02f)))
                listOf(-1.42f, -1.12f).forEach { x ->
                    addMesh(Geo.cylinder(0.026f, 0.018f, 10), Mat4.translation(V3(x, 0.43f, 0.08f)))
                }
            }.build(),
            material = mvBeacon,
        ),
    )
    parts.add(
        Part(
            id = "mv_lights", name = "Guideway beacons", system = MV_FRAME, assembly = MV_GUIDE,
            mesh = MeshBuilder().apply {
                listOf(-1.44f, -0.72f, 0.72f, 1.44f).forEach { x ->
                    addMesh(Geo.cylinder(0.026f, 0.018f, 10), Mat4.translation(V3(x, 0.38f, 0.41f)))
                }
            }.build(),
            material = Materials.emitCyan,
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "pod", "Passenger capsule",
            "The capsule rides on superconducting skids with no wheels. Its smooth shell is a pressure vessel, a passenger cabin and the moving part of the linear motor.",
            "mv_pod", V3(0f, 0.14f, 0.18f),
            CameraPose(24f, 14f, 1.05f), readoutLabel = "1,020 KM/H CRUISE",
        ),
        Hotspot(
            "triple", "Linear motor stator",
            "Copper and gold stator rings switch polarity in sequence, pulling the capsule along the track. The same magnetic field lifts it clear of the guideway.",
            "mv_stators", V3(0.18f, 0.02f, 0.45f),
            CameraPose(40f, 18f, 1f), readoutLabel = "MAGLEV DRIVE",
        ),
        Hotspot(
            "tube", "Vacuum tube",
            "The steel-and-glass tube is pumped down to a tiny fraction of atmospheric pressure. Less air means no windshield and far less drag at high speed.",
            "mv_tube", V3(0f, 0.04f, 0.42f),
            CameraPose(28f, 22f, 1f), readoutLabel = "VACUUM ENVELOPE",
        ),
        Hotspot(
            "canopy", "Solar station canopy",
            "The station roof collects power for the terminal, the pumps and the guideway coils. Wayside batteries buffer the surge when a capsule accelerates.",
            "mv_canopy", V3(0f, 0.12f, 0.30f),
            CameraPose(22f, 16f, 1f), readoutLabel = "STATION ENERGY",
        ),
        Hotspot(
            "control", "Traffic control tower",
            "The tower watches tube pressure, capsule spacing and guideway temperature. It will hold a launch if any section is outside its safe envelope.",
            "mv_control", V3(0f, 0.20f, 0.28f),
            CameraPose(48f, 16f, 1f), readoutLabel = "BLOCK CONTROL",
        ),
        Hotspot(
            "airlock", "Terminal airlock",
            "Passengers board through a pressure lock. Once the doors seal, the station can isolate its platform from the vacuum tube before a capsule arrives.",
            "mv_airlock", V3(0f, 0.0f, 0.48f),
            CameraPose(58f, 20f, 1f), readoutLabel = "PRESSURE LOCK",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Seal the tube",
            "The airlock closes and the vacuum pumps pull the tube down. Removing most of the air cuts drag and prevents a high-speed capsule from pushing a wall of air ahead of it.",
            MV_TUBE, CameraPose(30f, 20f, 1.04f), 0.10f, 0.30f,
        ),
        GuideStep(
            "Levitate",
            "The guideway coils energize and lift the capsule a few centimetres. With no wheels touching the rail, there is no rolling friction and far less wear.",
            MV_LEV, CameraPose(26f, 16f, 1.06f), 0.06f, 0.55f,
        ),
        GuideStep(
            "Accelerate",
            "The linear motor switches its stator field in a travelling wave. The capsule is pulled forward by the moving magnetic field rather than by an onboard engine.",
            MV_POD, CameraPose(24f, 14f, 1.04f), 0.04f, 0.82f,
        ),
        GuideStep(
            "Coast",
            "At cruise the capsule floats through a near-vacuum. Its stored kinetic energy carries it for kilometres while the guideway only makes small corrections.",
            MV_FRAME, CameraPose(34f, 18f, 1.02f), 0.02f, 0.65f,
        ),
        GuideStep(
            "Recover and turn",
            "The stators switch to generator mode and recover braking energy into the wayside battery. The next capsule can use that charge to accelerate.",
            MV_POWER, CameraPose(34f, 16f, 1.02f), 0.06f, 0.45f,
        ),
        GuideStep(
            "The assembly",
            "Explode the tube, guideway, capsule and terminal to see the vacuum envelope, magnetic drive and station services separately.",
            null, CameraPose(30f, 18f, 0.96f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "maglev_transit",
        index = 25,
        title = "VECTOR M-9",
        designation = "MV-9 / VACUUM TRANSIT LOOP",
        category = "Transport",
        categoryShort = "TRN",
        tagline = "A city-to-city line with no runway and no wheels.",
        summary = "A vacuum maglev terminal in section: a transparent tube, superconducting capsule, linear-motor stators, levitation skids, guideway pylons, solar station canopy, battery bank, vacuum pumps and traffic control.",
        details = listOf(
            "VECTOR M-9 is a passenger vacuum-transit system. The capsule floats on magnetic skids inside a low-pressure tube, while a linear motor in the guideway pushes it forward without wheels or a jet engine.",
            "The terminal seals around the tube and pumps the air out before launch. Solar panels and wayside batteries supply the station and recover energy during braking, and the control tower supervises pressure, spacing and guideway health.",
        ),
        specs = listOf(
            SpecRow("Cruise", "1,020 km/h"),
            SpecRow("Tube pressure", "0.001 atm"),
            SpecRow("Capsule", "28 passengers"),
            SpecRow("Levitation", "Superconducting"),
            SpecRow("Headway", "90 s"),
        ),
        tags = listOf("maglev", "transport", "vacuum", "train", "hyperloop", "station", "infrastructure", "future"),
        accent = 0xFF6EE7DF.toInt(),
        control = ControlSpec(
            label = "THROTTLE",
            lowLabel = "DOCK",
            highLabel = "CRUISE",
            defaultValue = 0.58f,
            hint = "Stator current, capsule speed, vacuum pressure and station demand",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(29f, 15f, 1.02f),
        thumbnailPose = CameraPose(30f, 17f, 1.04f),
        thumbnailTime = 2.4f,
        thumbnailControl = 0.76f,
        focusOffset = V3(0f, 0.06f, 0f),
        readouts = { ctx ->
            val speed = 180f + 900f * ctx.control
            val pressure = 0.006f - 0.0052f * ctx.control
            val load = 38f + 48f * ctx.control
            listOf(
                Readout("SPEED", String.format("%.0f", speed), "km/h", accent = true),
                Readout("TUBE", String.format("%.3f", pressure), "atm"),
                Readout("LOAD", String.format("%.0f", load), "%"),
            )
        },
        effects = { ctx ->
            val flow = 0.25f + 0.75f * ctx.control
            val blink = 0.75f + 0.25f * sin(ctx.machineTime * 4.0f)
            val podX = 0.15f + 0.68f * sin(ctx.machineTime * 1.05f)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.82f, 0xFF6EE7DF.toInt(), 0f, 0.12f),
                SceneEffect.Flow(
                    listOf(V3(-1.38f, 1.08f, 0f), V3(-0.55f, 1.08f, 0f), V3(0.62f, 1.08f, 0f), V3(1.38f, 1.08f, 0f)),
                    0xFF79F5E9.toInt(), count = 28, speed = 0.52f * flow, phase = 0.10f, dotRadius = 0.022f,
                ),
                SceneEffect.Flow(
                    listOf(V3(-1.38f, 1.16f, 0.30f), V3(-0.55f, 1.16f, 0.30f), V3(0.62f, 1.16f, 0.30f), V3(1.38f, 1.16f, 0.30f)),
                    0xFF79F5E9.toInt(), count = 18, speed = 0.48f * flow, phase = 0.42f, dotRadius = 0.014f,
                ),
                SceneEffect.Glow(V3(podX, 1.18f, 0f), 0.34f, 0xFFB8FFF4.toInt(), 0.16f + 0.12f * flow),
                SceneEffect.Beam(V3(-1.24f, 1.12f, 0f), V3(0.74f, 1.12f, 0f), 0xFF6EE7DF.toInt(), 0.05f + 0.12f * flow),
                SceneEffect.Glow(V3(0.15f, 1.17f, 0f), 0.30f, 0xFFB8FFF4.toInt(), 0.12f + 0.16f * flow),
                SceneEffect.Glow(V3(-1.27f, 1.24f, 0.48f), 0.28f, 0xFF71E8DB.toInt(), 0.12f + 0.12f * blink),
                SceneEffect.Spark(V3(1.40f, 1.24f, -0.20f), 0xFF7FF5E5.toInt(), 0.022f, 0.18f + 0.20f * blink),
                SceneEffect.Flow(
                    listOf(V3(-1.22f, 0.36f, 0.88f), V3(-0.35f, 0.36f, 0.92f), V3(1.24f, 0.36f, 0.84f)),
                    0xFFFFD18A.toInt(), count = 9, speed = 0.20f, phase = 0.34f, dotRadius = 0.018f,
                ),
            )
        },
        descriptionTitle = "The vacuum line",
    )
}
