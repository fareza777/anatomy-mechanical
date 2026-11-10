package studio.anatomy.app.objects

import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
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

private const val RV_CHASSIS = "CHASSIS"
private const val RV_DRIVE = "DRIVE"
private const val RV_SUSPENSION = "SUSPENSION"
private const val RV_MAST = "MAST"
private const val RV_SCIENCE = "SCIENCE"
private const val RV_POWER = "POWER"

private const val RV_PLINTH = "PLINTH"
private const val RV_BODY = "BODY"
private const val RV_WHEELS = "WHEELS"
private const val RV_BOGIE = "BOGIE"
private const val RV_HEAD = "CAMERA MAST"
private const val RV_ARM = "ROBOTIC ARM"
private const val RV_DECK = "DECK"

private const val RV_WHEEL_RADIUS = 0.30f
private const val RV_WHEEL_Y = 0.60f
private const val RV_WHEEL_Z = 0.74f
private const val RV_BODY_Y = 1.06f

private val rvFoil = Material(
    "roverFoil", "MLI blanket", 0xFFC9A15E.toInt(), specular = 0.55f, shininess = 26f,
    fillLight = 0.10f, rimLight = 0.17f, patina = 0.070f, edgeWear = 0.05f,
    pattern = SurfacePattern.SPECKLE, patternStrength = 0.35f,
)
private val rvTread = Material(
    "roverTread", "Wheel tread", 0xFF2A3236.toInt(), specular = 0.14f, shininess = 12f,
    fillLight = 0.06f, rimLight = 0.12f, patina = 0.040f,
    pattern = SurfacePattern.SPECKLE, patternStrength = 0.55f,
)
private val rvLens = Material(
    "roverLens", "Camera lens", 0xFF3D747A.toInt(), specular = 0.72f, shininess = 56f, alpha = 60,
)
private val rvScreen = Material(
    "roverScreen", "Instrument display", 0xFF8CF6E8.toInt(), emissive = true, specular = 0f,
)
private val rvAccent = Materials.paintOrange

private fun rvWheelMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(RV_WHEEL_RADIUS, 0.18f, 28, bevel = 0.040f).transformed(Mat4.rotateX(90f)))
    addMesh(Geo.cylinder(0.155f, 0.20f, 18, bevel = 0.020f).transformed(Mat4.rotateX(90f)))
    addMesh(Geo.torus(0.235f, 0.022f, 24, 5).transformed(Mat4.rotateX(90f)))
    for (i in 0 until 12) {
        addMesh(
            Geo.box(0.055f, 0.085f, 0.19f),
            Mat4.rotateZ(i * 30f).multiply(Mat4.translation(V3(0.315f, 0f, 0f))),
        )
    }
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.10f, 0.030f, 0.030f),
            Mat4.rotateZ(i * 72f).multiply(Mat4.translation(V3(0.10f, 0f, 0.10f))),
        )
    }
}.build()

private fun rvBodyMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.slab(1.96f, 0.98f, 0.40f, cornerRadius = 0.10f, bevel = 0.035f),
        Mat4.translation(V3(0f, RV_BODY_Y, 0f)),
    )
    addMesh(
        Geo.slab(1.86f, 0.90f, 0.06f, cornerRadius = 0.08f, bevel = 0.020f),
        Mat4.translation(V3(0f, RV_BODY_Y + 0.23f, 0f)),
    )
    for (side in listOf(-1f, 1f)) {
        addMesh(
            Geo.box(1.58f, 0.24f, 0.030f),
            Mat4.translation(V3(0f, RV_BODY_Y - 0.02f, side * 0.50f)),
        )
        for (x in listOf(-0.60f, -0.20f, 0.20f, 0.60f)) {
            addMesh(
                Geo.box(0.05f, 0.22f, 0.020f),
                Mat4.translation(V3(x, RV_BODY_Y - 0.02f, side * 0.515f)),
            )
        }
    }
    addMesh(Geo.box(0.10f, 0.22f, 0.86f), Mat4.translation(V3(1.00f, RV_BODY_Y - 0.05f, 0f)))
    addMesh(Geo.box(0.10f, 0.22f, 0.86f), Mat4.translation(V3(-1.00f, RV_BODY_Y - 0.05f, 0f)))
    for (x in listOf(-0.55f, 0.0f, 0.55f)) {
        addMesh(
            Geo.box(0.36f, 0.16f, 0.015f),
            Mat4.translation(V3(x, RV_BODY_Y - 0.08f, 0.505f)),
        )
        addMesh(
            Geo.box(0.36f, 0.16f, 0.015f),
            Mat4.translation(V3(x, RV_BODY_Y - 0.08f, -0.505f)),
        )
    }
    for (z in listOf(-0.30f, 0.30f)) {
        addMesh(Geo.box(0.30f, 0.22f, 0.24f), Mat4.translation(V3(-0.62f, RV_BODY_Y + 0.10f, z)))
    }
    for (i in 0 until 3) {
        addMesh(
            Geo.box(0.28f, 0.010f, 0.030f),
            Mat4.translation(V3(-0.30f + i * 0.06f, RV_BODY_Y + 0.13f, 0.505f)).multiply(Mat4.rotateY(-32f)),
        )
    }
    for (x in listOf(-0.86f, 0.86f)) {
        addMesh(Geo.hexBolt(0.035f, 0.020f), Mat4.translation(V3(x, RV_BODY_Y + 0.26f, 0.30f)))
    }
}.build()

private fun rvSolarMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(1.30f, 0.035f, 0.80f), Mat4.translation(V3(0.08f, 0.02f, 0f)))
    for (i in 0 until 4) {
        addMesh(
            Geo.box(0.30f, 0.014f, 0.76f),
            Mat4.translation(V3(-0.36f + i * 0.30f, 0.045f, 0f)),
        )
    }
    for (x in listOf(-0.58f, 0.72f)) {
        addMesh(Geo.box(0.06f, 0.10f, 0.06f), Mat4.translation(V3(x, 0f, 0f)))
    }
    addMesh(Geo.box(0.16f, 0.05f, 0.16f), Mat4.translation(V3(-0.72f, 0f, 0f)))
}.build()

private fun rvRockerMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(1.62f, 0.075f, 0.055f), Mat4.translation(V3(0f, 0f, 0f)))
    addMesh(Geo.cylinder(0.09f, 0.07f, 14).transformed(Mat4.rotateX(90f)))
    for (x in listOf(-0.88f, 0.88f)) {
        addMesh(
            Geo.cylinder(0.07f, 0.06f, 12).transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(x, -0.38f, 0f)),
        )
        addMesh(
            Geo.cylinderBetween(V3(x, 0f, 0f), V3(x, -0.38f, 0f), 0.032f, 8),
        )
    }
    addMesh(Geo.box(0.20f, 0.10f, 0.09f), Mat4.translation(V3(-0.44f, -0.05f, 0f)))
}.build()

private fun rvDamperMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.040f, 0.30f, 10), Mat4.translation(V3(0f, 0.15f, 0f)))
    addMesh(Geo.cylinder(0.065f, 0.10f, 12, bevel = 0.01f), Mat4.translation(V3(0f, 0f, 0f)))
    addMesh(Geo.torus(0.045f, 0.010f, 10, 4), Mat4.translation(V3(0f, 0.30f, 0f)))
}.build()

private fun rvMastMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.090f, 0.06f, 14), Mat4.translation(V3(0f, 0.03f, 0f)))
    addMesh(Geo.cylinder(0.045f, 0.52f, 10, bevel = 0.008f), Mat4.translation(V3(0f, 0.31f, 0f)))
    addMesh(Geo.cylinder(0.075f, 0.05f, 12), Mat4.translation(V3(0f, 0.57f, 0f)))
    addMesh(
        Geo.tube(
            listOf(V3(0.05f, 0.04f, 0f), V3(0.08f, 0.30f, 0.02f), V3(0.06f, 0.55f, 0f)),
            0.012f, 5, subdivide = 3,
        ),
    )
}.build()

private fun rvHeadMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.20f, 0.16f, 0.22f), Mat4.translation(V3(0f, 0.02f, 0.02f)))
    addMesh(Geo.box(0.14f, 0.04f, 0.16f), Mat4.translation(V3(0f, 0.11f, 0.02f)))
    addMesh(Geo.cylinder(0.035f, 0.10f, 10), Mat4.translation(V3(0f, -0.05f, 0f)))
    for (z in listOf(-0.09f, 0.09f)) {
        addMesh(Geo.box(0.03f, 0.06f, 0.03f), Mat4.translation(V3(0.10f, 0.10f, z)))
    }
}.build()

private fun rvLensMesh(): Mesh = MeshBuilder().apply {
    for (z in listOf(-0.06f, 0.06f)) {
        addMesh(
            Geo.cylinder(0.045f, 0.05f, 16, bevel = 0.010f).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(0.13f, 0.01f, z)),
        )
        addMesh(
            Geo.torus(0.046f, 0.008f, 16, 4).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(0.145f, 0.01f, z)),
        )
    }
    addMesh(Geo.box(0.06f, 0.03f, 0.04f), Mat4.translation(V3(0.10f, 0.09f, 0f)))
}.build()

private fun rvWiperMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.14f, 0.014f, 0.014f), Mat4.translation(V3(0.07f, 0f, 0f)))
    addMesh(Geo.box(0.03f, 0.070f, 0.012f), Mat4.translation(V3(0.14f, 0f, 0f)))
    addMesh(Geo.cylinder(0.020f, 0.04f, 8), Mat4.translation(V3(0f, 0f, 0f)))
}.build()

private fun rvDishMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.045f, 0.08f, 10), Mat4.translation(V3(0f, 0.04f, 0f)))
    addMesh(
        Geo.lathe(
            listOf(0.02f to 0.02f, 0.14f to 0.035f, 0.24f to 0.09f, 0.28f to 0.14f),
            26, capBottom = true,
        ).transformed(Mat4.rotateX(180f)),
        Mat4.translation(V3(0f, 0.09f, 0f)),
    )
    addMesh(Geo.cylinder(0.014f, 0.20f, 8), Mat4.translation(V3(0f, 0.24f, 0f)))
    addMesh(Geo.torus(0.28f, 0.010f, 26, 4), Mat4.translation(V3(0f, 0.10f, 0f)))
}.build()

private fun rvHgaMastMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.040f, 0.40f, 10), Mat4.translation(V3(0f, 0.20f, 0f)))
    addMesh(Geo.cylinder(0.060f, 0.05f, 12), Mat4.translation(V3(0f, 0.40f, 0f)))
    addMesh(Geo.cylinder(0.075f, 0.04f, 12), Mat4.translation(V3(0f, 0.01f, 0f)))
}.build()

private fun rvShoulderMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.10f, 0.08f, 14).transformed(Mat4.rotateZ(90f)))
    addMesh(Geo.box(0.50f, 0.080f, 0.080f), Mat4.translation(V3(-0.25f, 0f, 0f)))
    addMesh(Geo.cylinder(0.055f, 0.10f, 12).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(-0.50f, 0f, 0f)))
    addMesh(Geo.cylinderBetween(V3(-0.10f, 0.04f, 0f), V3(-0.40f, 0.14f, 0f), 0.014f, 6))
}.build()

private fun rvElbowMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.075f, 0.07f, 12).transformed(Mat4.rotateZ(90f)))
    addMesh(Geo.box(0.40f, 0.060f, 0.060f), Mat4.translation(V3(-0.20f, 0f, 0f)))
    addMesh(Geo.cylinder(0.045f, 0.08f, 10).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(-0.42f, 0f, 0f)))
}.build()

private fun rvWristMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.045f, 0.06f, 10).transformed(Mat4.rotateZ(90f)))
    addMesh(Geo.box(0.10f, 0.09f, 0.09f), Mat4.translation(V3(-0.08f, 0f, 0f)))
    addMesh(Geo.box(0.09f, 0.02f, 0.02f), Mat4.translation(V3(-0.14f, 0.03f, 0.03f)))
    addMesh(Geo.box(0.09f, 0.02f, 0.02f), Mat4.translation(V3(-0.14f, -0.03f, -0.03f)))
    addMesh(Geo.cylinder(0.025f, 0.10f, 10).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(-0.16f, 0.06f, 0f)))
}.build()

private fun rvTurretMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.145f, 0.06f, 16, bevel = 0.010f))
    for (i in 0 until 3) {
        val place = Mat4.rotateY(i * 120f).multiply(Mat4.translation(V3(0f, 0f, 0.12f)))
        addMesh(Geo.box(0.06f, 0.10f, 0.06f), place)
        addMesh(Geo.cylinder(0.020f, 0.14f, 8), place.multiply(Mat4.translation(V3(0f, -0.10f, 0f))))
    }
    addMesh(Geo.torus(0.145f, 0.012f, 16, 4), Mat4.translation(V3(0f, 0.03f, 0f)))
}.build()

private fun rvBitMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.035f, 0.10f, 10), Mat4.translation(V3(0f, -0.16f, 0f)))
    addMesh(Geo.taperedCylinder(0.012f, 0.030f, 0.12f, 10), Mat4.translation(V3(0f, -0.27f, 0f)))
    addMesh(Geo.helix(0.022f, 0.006f, 0.10f, 3, 8), Mat4.translation(V3(0f, -0.20f, 0f)))
}.build()

private fun rvCarouselMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.17f, 0.03f, 20, bevel = 0.006f))
    addMesh(Geo.cylinder(0.04f, 0.06f, 10))
    for (i in 0 until 6) {
        addMesh(
            Geo.cylinder(0.030f, 0.10f, 8).transformed(Mat4.rotateX(90f)),
            Mat4.rotateY(i * 60f).multiply(Mat4.translation(V3(0f, 0.02f, 0.12f))),
        )
    }
}.build()

private fun rvLightMesh(): Mesh = MeshBuilder().apply {
    for (z in listOf(-0.28f, 0.28f)) {
        addMesh(
            Geo.cylinder(0.050f, 0.04f, 14, bevel = 0.010f).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(1.06f, RV_BODY_Y + 0.10f, z)),
        )
    }
    addMesh(Geo.box(0.02f, 0.04f, 0.62f), Mat4.translation(V3(1.05f, RV_BODY_Y + 0.16f, 0f)))
}.build()

fun marsRover(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "rover",
            width = 4.10f,
            depth = 2.30f,
            top = 0.30f,
            system = RV_CHASSIS,
            assembly = RV_PLINTH,
            accentMaterial = Materials.emitAmber,
        ),
    )

    parts.add(
        Part(
            id = "rv_body", name = "Pressure body and deck", system = RV_CHASSIS, assembly = RV_BODY,
            mesh = rvBodyMesh(), material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "rv_solar", name = "Solar deck", system = RV_POWER, assembly = RV_DECK,
            mesh = rvSolarMesh(), material = Materials.solarCell,
            origin = V3(0.05f, RV_BODY_Y + 0.34f, 0f),
            animate = { ctx -> Mat4.rotateX(3.5f * sin(ctx.machineTime * 0.4f)) },
        ),
    )

    val wheelPositions = listOf(
        Triple(-0.88f, -RV_WHEEL_Z, "rv_wheel_rear_left"),
        Triple(-0.88f, RV_WHEEL_Z, "rv_wheel_rear_right"),
        Triple(0.0f, -RV_WHEEL_Z, "rv_wheel_mid_left"),
        Triple(0.0f, RV_WHEEL_Z, "rv_wheel_mid_right"),
        Triple(0.88f, -RV_WHEEL_Z, "rv_wheel_front_left"),
        Triple(0.88f, RV_WHEEL_Z, "rv_wheel_front_right"),
    )
    wheelPositions.forEachIndexed { index, (x, z, id) ->
        parts.add(
            Part(
                id = id, name = "Wheel ${index + 1}", system = RV_DRIVE, assembly = RV_WHEELS,
                mesh = rvWheelMesh(), material = rvTread,
                origin = V3(x, RV_WHEEL_Y, z),
                animate = { ctx -> Mat4.rotateZ(ctx.machineTime * 42f) },
            ),
        )
    }

    for (side in listOf(-1f, 1f)) {
        parts.add(
            Part(
                id = if (side < 0f) "rv_rocker_left" else "rv_rocker_right",
                name = if (side < 0f) "Port rocker bogie" else "Starboard rocker bogie",
                system = RV_SUSPENSION, assembly = RV_BOGIE,
                mesh = rvRockerMesh(), material = Materials.titanium,
                origin = V3(0f, 1.00f, side * 0.62f),
                animate = { ctx -> Mat4.rotateZ(side * 2.0f * sin(ctx.machineTime * 0.8f)) },
            ),
        )
        for (x in listOf(-0.88f, 0.88f)) {
            parts.add(
                Part(
                    id = "rv_damper_${side.toInt()}_${x.toInt()}",
                    name = "Suspension damper", system = RV_SUSPENSION, assembly = RV_BOGIE,
                    mesh = rvDamperMesh(), material = Materials.brushedSteel,
                    origin = V3(x, 0.96f, side * 0.62f),
                    animate = { ctx -> Mat4.translation(V3(0f, 0.018f * sin(ctx.machineTime * 1.1f + x), 0f)) },
                ),
            )
        }
    }

    parts.add(
        Part(
            id = "rv_mast", name = "Camera mast", system = RV_MAST, assembly = RV_HEAD,
            mesh = rvMastMesh(), material = Materials.titanium,
            origin = V3(0.60f, RV_BODY_Y + 0.26f, 0.12f),
        ),
    )
    parts.add(
        Part(
            id = "rv_mast_pan", name = "Pan head", system = RV_MAST, assembly = RV_HEAD,
            mesh = rvHeadMesh(), material = rvAccent,
            origin = V3(0.60f, RV_BODY_Y + 0.84f, 0.12f),
            animate = { ctx -> Mat4.rotateY(28f * sin(ctx.machineTime * 0.45f)) },
        ),
    )
    parts.add(
        Part(
            id = "rv_mast_tilt", name = "Stereo camera head", system = RV_MAST, assembly = RV_HEAD,
            mesh = rvLensMesh(), material = rvLens,
            origin = V3(0f, 0.02f, 0.12f), parent = "rv_mast_pan",
            animate = { ctx -> Mat4.rotateX(10f * sin(ctx.machineTime * 0.70f + 1.2f)) },
        ),
    )
    parts.add(
        Part(
            id = "rv_wiper", name = "Lens dust wiper", system = RV_MAST, assembly = RV_HEAD,
            mesh = rvWiperMesh(), material = Materials.rubber,
            origin = V3(0.02f, 0.10f, 0.24f), parent = "rv_mast_pan",
            animate = { ctx -> Mat4.rotateZ(35f * sin(ctx.machineTime * 5.5f)) },
        ),
    )

    parts.add(
        Part(
            id = "rv_hga_mast", name = "High gain antenna mast", system = RV_MAST, assembly = RV_HEAD,
            mesh = rvHgaMastMesh(), material = Materials.titanium,
            origin = V3(-0.74f, RV_BODY_Y + 0.26f, -0.22f),
        ),
    )
    parts.add(
        Part(
            id = "rv_hga_dish", name = "High gain dish", system = RV_MAST, assembly = RV_HEAD,
            mesh = rvDishMesh(), material = Materials.paintCream,
            origin = V3(-0.74f, RV_BODY_Y + 0.70f, -0.22f),
            animate = { ctx ->
                Mat4.rotateY(ctx.machineTime * 7f)
                    .multiply(Mat4.rotateX(6f * sin(ctx.machineTime * 0.5f)))
            },
        ),
    )

    parts.add(
        Part(
            id = "rv_arm_shoulder", name = "Arm shoulder", system = RV_SCIENCE, assembly = RV_ARM,
            mesh = rvShoulderMesh(), material = Materials.titanium,
            origin = V3(-0.94f, RV_BODY_Y + 0.10f, -0.36f),
            animate = { ctx -> Mat4.rotateZ(8f * sin(ctx.machineTime * 0.50f)) },
        ),
    )
    parts.add(
        Part(
            id = "rv_arm_elbow", name = "Arm elbow", system = RV_SCIENCE, assembly = RV_ARM,
            mesh = rvElbowMesh(), material = rvAccent,
            origin = V3(-0.50f, 0f, 0f), parent = "rv_arm_shoulder",
            animate = { ctx -> Mat4.rotateZ(16f * sin(ctx.machineTime * 0.50f + 0.9f)) },
        ),
    )
    parts.add(
        Part(
            id = "rv_arm_wrist", name = "Arm wrist and gripper", system = RV_SCIENCE, assembly = RV_ARM,
            mesh = rvWristMesh(), material = Materials.brushedSteel,
            origin = V3(-0.42f, 0f, 0f), parent = "rv_arm_elbow",
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 26f) },
        ),
    )

    parts.add(
        Part(
            id = "rv_turret", name = "Sample drill turret", system = RV_SCIENCE, assembly = RV_ARM,
            mesh = rvTurretMesh(), material = Materials.titanium,
            origin = V3(0.96f, RV_BODY_Y + 0.22f, 0.34f),
            animate = { ctx -> Mat4.rotateY(20f * sin(ctx.machineTime * 0.35f)) },
        ),
    )
    parts.add(
        Part(
            id = "rv_bit", name = "Drill bit", system = RV_SCIENCE, assembly = RV_ARM,
            mesh = rvBitMesh(), material = Materials.gold,
            origin = V3(0f, 0f, 0f), parent = "rv_turret",
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 240f) },
        ),
    )
    parts.add(
        Part(
            id = "rv_carousel", name = "Sample carousel", system = RV_SCIENCE, assembly = RV_DECK,
            mesh = rvCarouselMesh(), material = Materials.brushedSteel,
            origin = V3(-0.34f, RV_BODY_Y + 0.30f, -0.26f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 7f) },
        ),
    )

    parts.add(
        Part(
            id = "rv_lights", name = "Drive lamps and hazard bar", system = RV_DRIVE, assembly = RV_BODY,
            mesh = rvLightMesh(), material = rvScreen,
        ),
    )
    parts.add(
        Part(
            id = "rv_panel_lights", name = "Deck status strip", system = RV_POWER, assembly = RV_DECK,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.32f, 0.010f, 0.012f), Mat4.translation(V3(-0.05f, RV_BODY_Y + 0.27f, 0.455f)))
                addMesh(Geo.box(0.10f, 0.010f, 0.012f), Mat4.translation(V3(0.24f, RV_BODY_Y + 0.27f, 0.455f)))
            }.build(),
            material = rvScreen,
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "wheels", "Grouser wheels",
            "Six aluminium wheels carry the rover over rock and sand. The thin grousers bite into loose ground and the hub motors drive each wheel independently.",
            "rv_wheel_front_right", V3(0f, 0.30f, 0f),
            CameraPose(34f, 10f, 1.10f), readoutLabel = "WHEEL DRIVE",
        ),
        Hotspot(
            "bogie", "Rocker bogie suspension",
            "The rocker pivots along the body and the bogie balances the middle wheel. Each wheel can climb over an obstacle while the other five stay in contact with the ground.",
            "rv_rocker_right", V3(0.88f, -0.38f, 0f),
            CameraPose(20f, 8f, 1.06f), readoutLabel = "SUSPENSION",
        ),
        Hotspot(
            "mast", "Camera mast",
            "The mast lifts the stereo cameras above the dust. Its pan and tilt axes let the rover look at a rock, then at the horizon, without moving the body.",
            "rv_mast_pan", V3(0.13f, 0.02f, 0.12f),
            CameraPose(38f, 12f, 1.12f), readoutLabel = "NAVIGATION",
        ),
        Hotspot(
            "arm", "Robotic arm",
            "Three joints and a gripper let the arm place instruments on a rock. The wrist can rotate a drill or point a spectrometer at the surface.",
            "rv_arm_elbow", V3(-0.20f, 0f, 0f),
            CameraPose(28f, 14f, 1.10f), readoutLabel = "SAMPLE HANDLING",
        ),
        Hotspot(
            "drill", "Drill turret",
            "The turret swaps between a drill, a scoop and a contact instrument. The bit spins against the rock while the turret holds the tool steady.",
            "rv_turret", V3(0f, -0.10f, 0.12f),
            CameraPose(44f, 12f, 1.10f), readoutLabel = "SURFACE SCIENCE",
        ),
        Hotspot(
            "solar", "Solar deck",
            "The deck tilts toward the sun through the day. Dust settling on the cells is the main limit on power, so the panels are tilted to shed it.",
            "rv_solar", V3(0f, 0.05f, 0f),
            CameraPose(26f, 22f, 1.04f), readoutLabel = "POWER",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Roll",
            "Six hub motors drive the wheels. The rocker bogie keeps all six in contact as the rover crosses a ridge, so traction never depends on one wheel.",
            RV_DRIVE, CameraPose(30f, 10f, 1.06f), 0.10f, 0.60f,
        ),
        GuideStep(
            "Look",
            "The mast pans across the horizon and the stereo cameras build a terrain map. The rover plans a path around rocks it cannot climb.",
            RV_MAST, CameraPose(38f, 12f, 1.10f), 0.16f, 0.52f,
        ),
        GuideStep(
            "Reach",
            "The arm unfolds and places an instrument against a rock. Joints hold position while the wrist rotates the tool into contact.",
            RV_SCIENCE, CameraPose(28f, 14f, 1.08f), 0.22f, 0.64f,
        ),
        GuideStep(
            "Sample",
            "The turret selects the drill and the bit bores a few centimetres into the rock. Powder is delivered to the carousel for analysis.",
            RV_SCIENCE, CameraPose(44f, 12f, 1.12f), 0.10f, 0.78f,
        ),
        GuideStep(
            "Power",
            "The solar deck tilts toward the sun and the battery carries the rover through the night. Every instrument is scheduled around the energy budget.",
            RV_POWER, CameraPose(26f, 22f, 1.02f), 0.08f, 0.46f,
        ),
        GuideStep(
            "The assembly",
            "Explode the body, wheels, suspension, mast, arm and deck to see how the rover is put together.",
            null, CameraPose(32f, 16f, 0.94f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "mars_rover",
        index = 28,
        title = "ARES R-6",
        designation = "AR-6 / MARS EXPLORATION ROVER",
        category = "Exploration",
        categoryShort = "EXP",
        tagline = "Six wheels, one arm and a very long walk.",
        summary = "A planetary rover in section: a sealed body on a rocker bogie suspension, six grouser wheels with hub motors, a pan and tilt camera mast, a deep space antenna, a three joint sample arm, a drill turret and a tilting solar deck.",
        details = listOf(
            "ARES R-6 is built to move slowly and look closely. Six independently driven wheels and a rocker bogie suspension keep it level over rough ground, while the mast carries the cameras high enough to see the route ahead.",
            "The arm and turret are the science end of the machine: they place instruments, drill into rock and load samples into a carousel. Everything runs from a tilting solar deck and a battery sized for the Martian night.",
        ),
        specs = listOf(
            SpecRow("Mass", "1,025 kg"),
            SpecRow("Speed", "4.2 cm/s"),
            SpecRow("Wheels", "6, hub driven"),
            SpecRow("Suspension", "Rocker bogie"),
            SpecRow("Power", "1.8 m² solar"),
        ),
        tags = listOf("rover", "mars", "exploration", "robot", "space", "science", "drill", "suspension"),
        accent = 0xFFE0A063.toInt(),
        control = ControlSpec(
            label = "DRIVE",
            lowLabel = "CREEP",
            highLabel = "TRAVERSE",
            defaultValue = 0.60f,
            hint = "Wheel torque, mast scan rate, arm activity and instrument load",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(34f, 15f, 0.95f),
        thumbnailPose = CameraPose(33f, 17f, 1.00f),
        thumbnailTime = 2.6f,
        thumbnailControl = 0.74f,
        focusOffset = V3(0f, 0.04f, 0f),
        readouts = { ctx ->
            val speed = 0.8f + 3.4f * ctx.control
            val power = 120f + 260f * ctx.control
            val solar = 38f + 52f * ctx.control
            listOf(
                Readout("SPEED", String.format("%.1f", speed), "cm/s", accent = true),
                Readout("POWER", String.format("%.0f", power), "W"),
                Readout("SOLAR", String.format("%.0f", solar), "%"),
            )
        },
        effects = { ctx ->
            val activity = (0.25f + 0.75f * ctx.control).coerceIn(0f, 1f)
            val dust = 0.5f + 0.5f * sin(ctx.machineTime * 1.6f)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.92f, 0xFFE0A063.toInt(), 0f, 0.12f),
                SceneEffect.Glow(V3(1.06f, RV_BODY_Y + 0.10f, 0.28f), 0.20f, 0xFFFFD7A8.toInt(), 0.22f + 0.22f * activity),
                SceneEffect.Glow(V3(1.06f, RV_BODY_Y + 0.10f, -0.28f), 0.20f, 0xFFFFD7A8.toInt(), 0.22f + 0.22f * activity),
                SceneEffect.Beam(
                    V3(0.60f, RV_BODY_Y + 0.96f, 0.12f), V3(1.72f, RV_BODY_Y - 0.18f, 0.34f),
                    0xFFE0A063.toInt(), 0.08f + 0.10f * activity,
                ),
                SceneEffect.Spark(V3(0.96f, RV_BODY_Y - 0.10f, 0.34f), 0xFFFFD7A8.toInt(), 0.018f, 0.14f + 0.20f * activity),
                SceneEffect.Flow(
                    listOf(V3(-1.42f, 0.40f, 0.42f), V3(-0.30f, 0.38f, 0.52f), V3(0.78f, 0.40f, 0.45f)),
                    0xFFD8A063.toInt(), count = 10, speed = 0.18f + 0.10f * dust,
                    phase = 0.20f, dotRadius = 0.026f,
                ),
                SceneEffect.Glow(V3(-0.74f, RV_BODY_Y + 0.82f, -0.22f), 0.22f, 0xFFB9FFF3.toInt(), 0.08f + 0.10f * activity),
            )
        },
        descriptionTitle = "The surface rover",
    )
}
