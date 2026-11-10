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
import kotlin.math.sin

private const val JP_WELL = "WELL"
private const val JP_PUMP = "JET PUMP"
private const val JP_PRESSURE = "PRESSURE"
private const val JP_PIPE = "RISER"
private const val JP_TANK = "ROOF TANK"
private const val JP_HOUSE = "HOUSE"
private const val JP_CONTROL = "CONTROL"

private const val JP_GROUND = "CUTAWAY GROUND"
private const val JP_BORE = "WELL BORE"
private const val JP_SET = "PUMP SET"
private const val JP_VESSEL = "PRESSURE VESSEL"
private const val JP_PIPEWORK = "PIPEWORK"
private const val JP_STORAGE = "STORAGE"
private const val JP_BUILDING = "HOUSE"
private const val JP_PANEL = "CONTROL PANEL"

private val JP_WATER = Material(
    "jpWater", "Well water", 0xFF3D9FBF.toInt(),
    specular = 0.72f, shininess = 58f, alpha = 118, specTint = 0xFFD8FFFF.toInt(),
)
private val JP_SOIL = Material(
    "jpSoil", "Cut subsoil", 0xFF6D5A46.toInt(),
    specular = 0.12f, shininess = 8f,
)
private val JP_CLAY = Material(
    "jpClay", "Clay strata", 0xFF94775A.toInt(),
    specular = 0.14f, shininess = 10f,
)

private const val TANK_X = 1.16f
private const val TANK_Z = -0.10f
private const val TANK_BOTTOM = 1.94f
private const val TANK_RADIUS = 0.28f
private const val TANK_HEIGHT = 0.52f

private fun tankLevel(ctx: AnimContext): Float =
    (0.82f - 0.36f * ctx.control + 0.08f * sin(ctx.machineTime * 0.42f)).coerceIn(0.16f, 0.96f)

private fun pumpPressure(ctx: AnimContext): Float =
    (0.52f + 0.36f * sin(ctx.machineTime * 0.58f)).coerceIn(0.12f, 0.92f)

private fun impellerAngle(ctx: AnimContext): Float =
    ctx.machineTime * (460f + 760f * ctx.control)

private fun surfacePumpMesh(): Mesh = MeshBuilder().apply {
    // Cast pump body and suction chamber, deliberately split open at the front.
    addMesh(
        Geo.slab(0.42f, 0.34f, 0.26f, 0.05f, 0.02f),
        Mat4.translation(V3(-0.82f, 1.15f, 0.06f)),
    )
    addMesh(
        Geo.cylinder(0.105f, 0.20f, 22, bevel = 0.012f).transformed(Mat4.rotateX(90f)),
        Mat4.translation(V3(-0.98f, 1.17f, 0.12f)),
    )
    addMesh(
        Geo.torus(0.108f, 0.014f, 24, 5).transformed(Mat4.rotateX(90f)),
        Mat4.translation(V3(-1.00f, 1.17f, 0.03f)),
    )
    listOf(-0.82f, -0.68f).forEach { x ->
        addMesh(Geo.cylinder(0.018f, 0.24f, 8), Mat4.translation(V3(x, 1.00f, 0.06f)))
    }
    addMesh(
        Geo.tube(
            listOf(
                V3(-1.12f, 1.06f, 0.06f),
                V3(-1.05f, 1.10f, 0.06f),
                V3(-0.96f, 1.12f, 0.06f),
            ),
            0.030f, 8, subdivide = 3,
        ),
    )
}.build()

private fun motorMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.cylinder(0.115f, 0.42f, 24, bevel = 0.018f).transformed(Mat4.rotateZ(90f)),
        Mat4.translation(V3(-0.55f, 1.19f, 0.06f)),
    )
    for (i in 0 until 7) {
        addMesh(
            Geo.torus(0.118f, 0.010f, 22, 4).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(-0.70f + i * 0.048f, 1.19f, 0.06f)),
        )
    }
    addMesh(
        Geo.box(0.14f, 0.10f, 0.14f),
        Mat4.translation(V3(-0.46f, 1.30f, 0.06f)),
    )
    addMesh(
        Geo.tube(
            listOf(
                V3(-0.46f, 1.34f, 0.06f),
                V3(-0.36f, 1.38f, 0.02f),
                V3(-0.26f, 1.38f, -0.12f),
                V3(-0.20f, 1.28f, -0.16f),
            ),
            0.020f, 6, subdivide = 3,
        ),
    )
}.build()

private fun fanMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.036f, 0.032f, 14).transformed(Mat4.rotateZ(90f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.014f, 0.135f, 0.055f),
            Mat4.translation(V3(0f, 0f, 0f))
                .multiply(Mat4.rotateX(72f * i))
                .multiply(Mat4.translation(V3(0f, 0f, 0.075f))),
        )
    }
}.build()

private fun tankShellMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(TANK_RADIUS, TANK_HEIGHT, 34), Mat4.translation(V3(0f, TANK_HEIGHT * 0.5f, 0f)))
}.build()

private fun tankWaterMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(TANK_RADIUS - 0.022f, 1f, 32), Mat4.translation(V3(0f, 0.5f, 0f)))
}.build()

private fun tankLidMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.taperedCylinder(TANK_RADIUS + 0.025f, 0.10f, 0.12f, 30),
        Mat4.translation(V3(0f, TANK_HEIGHT + 0.04f, 0f)),
    )
    addMesh(Geo.cylinder(0.045f, 0.08f, 14), Mat4.translation(V3(0f, TANK_HEIGHT + 0.13f, 0f)))
    addMesh(Geo.torus(0.095f, 0.012f, 18, 4), Mat4.translation(V3(0f, TANK_HEIGHT + 0.10f, 0f)))
}.build()

private fun tankStandMesh(): Mesh = MeshBuilder().apply {
    val roof = 1.68f
    val bottom = TANK_BOTTOM - 0.02f
    listOf(-1f, 1f).forEach { sx ->
        listOf(-1f, 1f).forEach { sz ->
            val x = sx * 0.22f
            val z = sz * 0.20f
            addMesh(
                Geo.cylinderBetween(
                    V3(x, roof, z),
                    V3(x * 0.78f, bottom, z * 0.72f),
                    0.022f, 8,
                ),
            )
            addMesh(Geo.box(0.075f, 0.018f, 0.075f), Mat4.translation(V3(x * 0.78f, bottom, z * 0.72f)))
        }
    }
    addMesh(Geo.box(0.50f, 0.035f, 0.46f), Mat4.translation(V3(0f, roof + 0.02f, 0f)))
    addMesh(Geo.box(0.52f, 0.028f, 0.10f), Mat4.translation(V3(0f, 1.84f, 0f)))
}.build()

private fun floatMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.25f, 0.012f, 0.012f), Mat4.translation(V3(-0.125f, 0f, 0f)))
    addMesh(Geo.sphere(0.045f, 14, 8), Mat4.translation(V3(-0.26f, 0f, 0f)))
    addMesh(Geo.cylinder(0.022f, 0.030f, 10).transformed(Mat4.rotateX(90f)))
}.build()

private fun gaugeMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.090f, 0.030f, 26).transformed(Mat4.rotateX(90f)))
    addMesh(Geo.torus(0.088f, 0.010f, 26, 4).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, 0.012f)))
    addMesh(Geo.cylinder(0.012f, 0.042f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, 0.028f)))
}.build()

private fun gaugeNeedleMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.010f, 0.075f, 0.006f), Mat4.translation(V3(0f, 0.032f, 0f)))
    addMesh(Geo.box(0.050f, 0.008f, 0.006f), Mat4.translation(V3(0f, -0.014f, 0f)))
}.build()

private fun tapWheelMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.050f, 0.009f, 18, 4).transformed(Mat4.rotateX(90f)))
    for (i in 0 until 4) {
        addMesh(
            Geo.box(0.088f, 0.008f, 0.008f),
            Mat4.rotateZ(45f * i),
        )
    }
    addMesh(Geo.cylinder(0.012f, 0.026f, 10).transformed(Mat4.rotateX(90f)))
}.build()

fun jetPumpSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "aqua",
            width = 3.62f,
            depth = 2.55f,
            top = 0.30f,
            system = JP_WELL,
            assembly = JP_GROUND,
            accentMaterial = Materials.emitCyan,
        ),
    )

    // The ground is deliberately open at the front: a section, not a buried box.
    parts.add(
        Part(
            id = "jp_soil",
            name = "Cutaway subsoil",
            system = JP_WELL, assembly = JP_GROUND,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(1.22f, 0.08f, 0.76f, 0.03f, 0.01f), Mat4.translation(V3(-1.16f, 0.68f, -0.42f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.slab(0.08f, 0.92f, 0.76f, 0.03f, 0.01f),
                        Mat4.translation(V3(-1.16f + side * 0.57f, 0.68f, 0.02f)),
                    )
                }
                addMesh(Geo.slab(1.22f, 0.92f, 0.045f, 0.02f, 0.008f), Mat4.translation(V3(-1.16f, 0.322f, 0.02f)))
                addMesh(Geo.slab(0.08f, 0.92f, 0.08f, 0.02f, 0.008f), Mat4.translation(V3(-1.16f, 1.03f, -0.42f)))
                // A darker well shaft behind the pump casing.
                addMesh(
                    Geo.cylinder(0.185f, 0.70f, 20, capTop = false, capBottom = false),
                    Mat4.translation(V3(-1.16f, 0.68f, 0.04f)),
                )
            }.build(),
            material = JP_SOIL,
        ),
    )
    parts.add(
        Part(
            id = "jp_strata",
            name = "Clay strata",
            system = JP_WELL, assembly = JP_GROUND,
            mesh = MeshBuilder().apply {
                listOf(0.42f, 0.56f, 0.70f, 0.84f, 0.96f).forEach { y ->
                    listOf(-1f, 1f).forEach { side ->
                        addMesh(
                            Geo.box(0.070f, 0.018f, 0.86f),
                            Mat4.translation(V3(-1.16f + side * 0.57f, y, 0.02f)),
                        )
                    }
                    addMesh(Geo.box(1.10f, 0.018f, 0.025f), Mat4.translation(V3(-1.16f, y, -0.385f)))
                }
            }.build(),
            material = JP_CLAY,
        ),
    )
    parts.add(
        Part(
            id = "jp_well_cap",
            name = "Well cap and flange",
            system = JP_WELL, assembly = JP_BORE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.185f, 0.055f, 24), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.torus(0.175f, 0.014f, 24, 4), Mat4.translation(V3(0f, 0.02f, 0f)))
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    addMesh(
                        Geo.hexBolt(0.018f, 0.018f),
                        Mat4.translation(V3((0.13f * kotlin.math.cos(a)).toFloat(), 0.045f, (0.13f * kotlin.math.sin(a)).toFloat())),
                    )
                }
            }.build(),
            material = Materials.paintOrange,
            origin = V3(-1.16f, 1.075f, 0.04f),
        ),
    )
    parts.add(
        Part(
            id = "jp_well_casing",
            name = "Glass well casing",
            system = JP_WELL, assembly = JP_BORE,
            mesh = Geo.cylinder(0.135f, 0.68f, 24, capTop = false, capBottom = false),
            material = Materials.glass,
            origin = V3(-1.16f, 0.69f, 0.04f),
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "jp_casing_bands",
            name = "Casing bands and slots",
            system = JP_WELL, assembly = JP_BORE,
            mesh = MeshBuilder().apply {
                listOf(-0.31f, -0.08f, 0.15f, 0.31f).forEach { y ->
                    addMesh(Geo.torus(0.138f, 0.010f, 24, 4), Mat4.translation(V3(0f, y, 0f)))
                }
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    addMesh(
                        Geo.box(0.018f, 0.52f, 0.012f),
                        Mat4.translation(V3((0.136f * kotlin.math.cos(a)).toFloat(), 0f, (0.136f * kotlin.math.sin(a)).toFloat()))
                            .multiply(Mat4.rotateY(i * 45f)),
                    )
                }
            }.build(),
            material = Materials.brushedSteel,
            origin = V3(-1.16f, 0.69f, 0.04f),
        ),
    )

    parts.add(
        Part(
            id = "jp_pump_body",
            name = "Jet pump and ejector",
            system = JP_PUMP, assembly = JP_SET,
            mesh = MeshBuilder().apply {
                listOf(-0.16f, 0.14f).forEach { y ->
                    addMesh(Geo.cylinder(0.105f, 0.032f, 20, bevel = 0.006f), Mat4.translation(V3(0f, y, 0f)))
                }
                for (i in 0 until 3) {
                    val a = Math.toRadians(30.0 + i * 120.0)
                    addMesh(
                        Geo.box(0.022f, 0.34f, 0.022f),
                        Mat4.translation(V3((0.092f * kotlin.math.cos(a)).toFloat(), 0f, (0.092f * kotlin.math.sin(a)).toFloat())),
                    )
                }
                addMesh(Geo.cylinder(0.042f, 0.42f, 14), Mat4.translation(V3(0f, 0.02f, 0f)))
                addMesh(Geo.taperedCylinder(0.070f, 0.038f, 0.11f, 16), Mat4.translation(V3(0f, -0.22f, 0f)))
                addMesh(Geo.cylinder(0.050f, 0.08f, 14), Mat4.translation(V3(0f, -0.30f, 0f)))
                for (i in 0 until 4) {
                    addMesh(
                        Geo.box(0.028f, 0.060f, 0.018f),
                        Mat4.rotateY(i * 90f).multiply(Mat4.translation(V3(0.060f, -0.25f, 0f))),
                    )
                }
            }.build(),
            material = Materials.titanium,
            origin = V3(-1.16f, 0.57f, 0.04f),
        ),
    )
    parts.add(
        Part(
            id = "jp_impeller",
            name = "Pump impeller",
            system = JP_PUMP, assembly = JP_SET,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.032f, 0.15f, 16), Mat4.translation(V3(0f, 0.06f, 0f)))
                for (i in 0 until 6) {
                    addMesh(
                        Geo.box(0.075f, 0.012f, 0.040f),
                        Mat4.rotateY(60f * i).multiply(Mat4.translation(V3(0.060f, 0.06f, 0f))),
                    )
                    addMesh(
                        Geo.box(0.070f, 0.010f, 0.030f),
                        Mat4.rotateY(60f * i + 18f).multiply(Mat4.translation(V3(0.060f, 0.13f, 0f))),
                    )
                }
                addMesh(Geo.torus(0.105f, 0.012f, 22, 4), Mat4.translation(V3(0f, 0.02f, 0f)))
            }.build(),
            material = Materials.brass,
            origin = V3(-1.16f, 0.57f, 0.04f),
            animate = { ctx -> Mat4.rotateY(impellerAngle(ctx)) },
        ),
    )
    parts.add(
        Part(
            id = "jp_intake",
            name = "Intake screen",
            system = JP_PUMP, assembly = JP_SET,
            mesh = MeshBuilder().apply {
                listOf(-0.05f, 0f, 0.05f).forEach { y ->
                    addMesh(Geo.torus(0.078f, 0.008f, 20, 4), Mat4.translation(V3(0f, y, 0f)))
                }
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    addMesh(
                        Geo.box(0.012f, 0.16f, 0.012f),
                        Mat4.translation(V3((0.078f * kotlin.math.cos(a)).toFloat(), 0f, (0.078f * kotlin.math.sin(a)).toFloat())),
                    )
                }
            }.build(),
            material = Materials.darkSteel,
            origin = V3(-1.16f, 0.38f, 0.04f),
        ),
    )
    parts.add(
        Part(
            id = "jp_sub_motor",
            name = "Submersible motor",
            system = JP_PUMP, assembly = JP_SET,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.074f, 0.22f, 20, bevel = 0.012f), Mat4.translation(V3(0f, 0f, 0f)))
                for (i in 0 until 5) {
                    addMesh(Geo.torus(0.077f, 0.008f, 20, 4), Mat4.translation(V3(0f, -0.08f + i * 0.04f, 0f)))
                }
                addMesh(Geo.cylinder(0.050f, 0.055f, 14), Mat4.translation(V3(0f, 0.14f, 0f)))
            }.build(),
            material = Materials.brushedSteel,
            origin = V3(-1.16f, 0.88f, 0.04f),
        ),
    )
    parts.add(
        Part(
            id = "jp_cable",
            name = "Pump cable",
            system = JP_PUMP, assembly = JP_SET,
            mesh = Geo.tube(
                listOf(
                    V3(-1.16f, 0.92f, 0.02f),
                    V3(-1.02f, 1.02f, -0.03f),
                    V3(-0.88f, 1.10f, -0.14f),
                    V3(-0.72f, 1.03f, -0.24f),
                    V3(-0.58f, 0.84f, -0.36f),
                ),
                0.017f, 6, subdivide = 3,
            ),
            material = Materials.rubber,
        ),
    )

    parts.add(
        Part(
            id = "jp_surface_pump",
            name = "Surface jet pump",
            system = JP_PUMP, assembly = JP_SET,
            mesh = surfacePumpMesh(),
            material = Materials.paintOrange,
        ),
    )
    parts.add(
        Part(
            id = "jp_motor",
            name = "Pump motor",
            system = JP_PUMP, assembly = JP_SET,
            mesh = motorMesh(),
            material = Materials.paintSlate,
        ),
    )
    parts.add(
        Part(
            id = "jp_fan",
            name = "Motor cooling fan",
            system = JP_PUMP, assembly = JP_SET,
            mesh = fanMesh(),
            material = Materials.nickel,
            origin = V3(-0.35f, 1.19f, 0.06f),
            animate = { ctx -> Mat4.rotateX(impellerAngle(ctx) * 1.4f) },
        ),
    )

    parts.add(
        Part(
            id = "jp_pressure_shell",
            name = "Pressure vessel",
            system = JP_PRESSURE, assembly = JP_VESSEL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.165f, 0.60f, 28), Mat4.translation(V3(0f, 0.30f, 0f)))
                addMesh(Geo.taperedCylinder(0.165f, 0.070f, 0.09f, 24), Mat4.translation(V3(0f, 0.635f, 0f)))
                addMesh(Geo.taperedCylinder(0.165f, 0.070f, 0.09f, 24).transformed(Mat4.rotateZ(180f)), Mat4.translation(V3(0f, -0.035f, 0f)))
                listOf(0.08f, 0.52f).forEach { y ->
                    addMesh(Geo.torus(0.168f, 0.014f, 24, 4), Mat4.translation(V3(0f, y, 0f)))
                }
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    addMesh(
                        Geo.box(0.014f, 0.46f, 0.014f),
                        Mat4.translation(V3((0.166f * kotlin.math.cos(a)).toFloat(), 0.30f, (0.166f * kotlin.math.sin(a)).toFloat())),
                    )
                }
            }.build(),
            material = Materials.glass,
            origin = V3(-1.47f, 0.70f, -0.58f),
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "jp_bladder",
            name = "Moving diaphragm",
            system = JP_PRESSURE, assembly = JP_VESSEL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.sphere(0.135f, 22, 10).transformed(Mat4.scale(V3(1f, 0.30f, 1f))))
                addMesh(Geo.torus(0.132f, 0.012f, 22, 4))
            }.build(),
            material = Materials.rubber,
            origin = V3(-1.47f, 0.84f, -0.58f),
            animate = { ctx -> Mat4.translation(V3(0f, 0.10f * pumpPressure(ctx), 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "jp_pressure_pipe",
            name = "Pressure pipe",
            system = JP_PRESSURE, assembly = JP_PIPEWORK,
            mesh = Geo.tube(
                listOf(
                    V3(-1.16f, 1.09f, 0.04f),
                    V3(-1.24f, 1.24f, -0.04f),
                    V3(-1.35f, 1.22f, -0.24f),
                    V3(-1.43f, 0.94f, -0.40f),
                    V3(-1.44f, 0.72f, -0.56f),
                ),
                0.026f, 7, subdivide = 3,
            ),
            material = Materials.copperBright,
        ),
    )
    parts.add(
        Part(
            id = "jp_gauge",
            name = "Pressure gauge",
            system = JP_PRESSURE, assembly = JP_VESSEL,
            mesh = gaugeMesh(),
            material = Materials.paintCream,
            origin = V3(-1.02f, 1.29f, 0.20f),
        ),
    )
    parts.add(
        Part(
            id = "jp_gauge_needle",
            name = "Gauge needle",
            system = JP_PRESSURE, assembly = JP_VESSEL,
            mesh = gaugeNeedleMesh(),
            material = Materials.emitAmber,
            origin = V3(-1.02f, 1.29f, 0.235f),
            animate = { ctx -> Mat4.rotateZ(-122f + 244f * pumpPressure(ctx)) },
        ),
    )

    parts.add(
        Part(
            id = "jp_separator",
            name = "Control separator",
            system = JP_CONTROL, assembly = JP_SET,
            mesh = Geo.box(0.34f, 0.36f, 0.18f),
            material = Materials.paintCream,
            origin = V3(-0.89f, 0.80f, -0.20f),
        ),
    )
    parts.add(
        Part(
            id = "jp_control_lights",
            name = "Pressure switch lights",
            system = JP_CONTROL, assembly = JP_SET,
            mesh = MeshBuilder().apply {
                for (i in 0 until 3) {
                    addMesh(Geo.box(0.050f, 0.018f, 0.010f), Mat4.translation(V3(0f, 0.06f - i * 0.055f, 0.092f)))
                }
            }.build(),
            material = Materials.emitCyan,
            parent = "jp_separator",
        ),
    )

    parts.add(
        Part(
            id = "jp_riser",
            name = "Riser to the roof tank",
            system = JP_PIPE, assembly = JP_PIPEWORK,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-1.12f, 1.08f, 0.04f),
                            V3(-1.04f, 1.26f, 0.20f),
                            V3(-0.62f, 1.38f, 0.34f),
                            V3(-0.58f, 2.22f, 0.34f),
                            V3(0.54f, 2.24f, 0.34f),
                            V3(1.04f, 2.16f, 0.08f),
                            V3(1.16f, 2.08f, -0.02f),
                        ),
                        0.027f, 8, subdivide = 3,
                    ),
                )
                listOf(-0.58f, 0.54f).forEach { x ->
                    addMesh(Geo.torus(0.031f, 0.008f, 16, 4).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(x, 1.45f, 0.34f)))
                }
                addMesh(Geo.cylinder(0.044f, 0.05f, 16), Mat4.translation(V3(1.16f, 2.07f, -0.02f)))
            }.build(),
            material = Materials.copperBright,
        ),
    )
    parts.add(
        Part(
            id = "jp_riser_brackets",
            name = "Pipe brackets",
            system = JP_PIPE, assembly = JP_PIPEWORK,
            mesh = MeshBuilder().apply {
                listOf(1.52f, 1.86f, 2.10f).forEach { y ->
                    addMesh(
                        Geo.box(0.18f, 0.022f, 0.045f),
                        Mat4.translation(V3(-0.49f, y, 0.34f)),
                    )
                    addMesh(
                        Geo.box(0.022f, 0.022f, 0.20f),
                        Mat4.translation(V3(-0.42f, y, 0.26f)),
                    )
                }
            }.build(),
            material = Materials.titanium,
        ),
    )

    parts.add(
        Part(
            id = "jp_house",
            name = "House shell",
            system = JP_HOUSE, assembly = JP_BUILDING,
            mesh = MeshBuilder().apply {
                // Separate walls keep the small house readable through the tank stand and
                // leave a real door opening instead of a flat, solid slab.
                addMesh(Geo.box(0.82f, 1.30f, 0.055f), Mat4.translation(V3(1.16f, 0.95f, -0.405f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.box(0.055f, 1.30f, 0.62f),
                        Mat4.translation(V3(1.16f + side * 0.385f, 0.95f, -0.10f)),
                    )
                }
                addMesh(Geo.box(0.82f, 0.20f, 0.055f), Mat4.translation(V3(1.16f, 0.41f, 0.205f)))
                addMesh(Geo.box(0.82f, 0.24f, 0.055f), Mat4.translation(V3(1.16f, 1.44f, 0.205f)))
                addMesh(Geo.box(0.24f, 1.10f, 0.055f), Mat4.translation(V3(0.87f, 1.05f, 0.205f)))
                addMesh(Geo.box(0.24f, 1.10f, 0.055f), Mat4.translation(V3(1.45f, 1.05f, 0.205f)))
                addMesh(Geo.box(0.055f, 1.10f, 0.055f), Mat4.translation(V3(1.16f, 1.05f, 0.205f)))
                addMesh(Geo.box(0.86f, 0.055f, 0.66f), Mat4.translation(V3(1.16f, 0.325f, -0.10f)))
                addMesh(
                    Geo.slab(0.94f, 0.74f, 0.075f, 0.04f, 0.015f),
                    Mat4.translation(V3(1.16f, 1.68f, -0.10f)),
                )
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.035f, 0.12f, 0.72f), Mat4.translation(V3(1.16f + side * 0.45f, 1.74f, -0.10f)))
                }
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.box(0.035f, 0.12f, 0.72f),
                        Mat4.translation(V3(1.16f + side * 0.45f, 1.74f, -0.10f)),
                    )
                }
                addMesh(Geo.box(0.86f, 0.10f, 0.035f), Mat4.translation(V3(1.16f, 1.74f, 0.25f)))
            }.build(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "jp_house_trim",
            name = "House trim",
            system = JP_HOUSE, assembly = JP_BUILDING,
            mesh = MeshBuilder().apply {
                // Door.
                addMesh(Geo.box(0.24f, 0.52f, 0.018f), Mat4.translation(V3(1.00f, 0.63f, 0.218f)))
                addMesh(Geo.box(0.30f, 0.028f, 0.040f), Mat4.translation(V3(1.00f, 0.92f, 0.228f)))
                // Two warm windows.
                listOf(1.30f, 1.46f).forEach { x ->
                    addMesh(Geo.box(0.12f, 0.20f, 0.016f), Mat4.translation(V3(x, 1.00f, 0.222f)))
                    addMesh(Geo.box(0.15f, 0.022f, 0.028f), Mat4.translation(V3(x, 1.12f, 0.232f)))
                    addMesh(Geo.box(0.15f, 0.022f, 0.028f), Mat4.translation(V3(x, 0.88f, 0.232f)))
                }
            }.build(),
            material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "jp_window_light",
            name = "House window light",
            system = JP_HOUSE, assembly = JP_BUILDING,
            mesh = MeshBuilder().apply {
                listOf(1.30f, 1.46f).forEach { x ->
                    addMesh(Geo.box(0.10f, 0.18f, 0.008f), Mat4.translation(V3(x, 1.00f, 0.232f)))
                }
            }.build(),
            material = Materials.emitAmber,
            animate = { ctx ->
                val pulse = 0.86f + 0.14f * sin(ctx.machineTime * 1.8f)
                Mat4.scale(V3(1f, pulse, 1f))
            },
        ),
    )

    parts.add(
        Part(
            id = "jp_tank_stand",
            name = "Roof tank cradle",
            system = JP_TANK, assembly = JP_STORAGE,
            mesh = tankStandMesh(),
            material = Materials.darkSteel,
            origin = V3(TANK_X, 0f, TANK_Z),
        ),
    )
    parts.add(
        Part(
            id = "jp_tank_shell",
            name = "Rooftop tank",
            system = JP_TANK, assembly = JP_STORAGE,
            mesh = tankShellMesh(),
            material = Materials.glass,
            origin = V3(TANK_X, TANK_BOTTOM, TANK_Z),
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "jp_tank_water",
            name = "Tank water level",
            system = JP_TANK, assembly = JP_STORAGE,
            mesh = tankWaterMesh(),
            material = JP_WATER,
            origin = V3(TANK_X, TANK_BOTTOM, TANK_Z),
            animate = { ctx -> Mat4.scale(V3(1f, tankLevel(ctx), 1f)) },
        ),
    )
    parts.add(
        Part(
            id = "jp_tank_bands",
            name = "Tank bands and lid",
            system = JP_TANK, assembly = JP_STORAGE,
            mesh = MeshBuilder().apply {
                listOf(0.055f, TANK_HEIGHT - 0.055f).forEach { y ->
                    addMesh(Geo.torus(TANK_RADIUS + 0.006f, 0.014f, 30, 5), Mat4.translation(V3(0f, y, 0f)))
                }
                addMesh(tankLidMesh())
                addMesh(Geo.box(0.34f, 0.035f, 0.040f), Mat4.translation(V3(-0.16f, 0.34f, 0.285f)))
            }.build(),
            material = Materials.nickel,
            origin = V3(TANK_X, TANK_BOTTOM, TANK_Z),
        ),
    )
    parts.add(
        Part(
            id = "jp_float",
            name = "Float valve",
            system = JP_TANK, assembly = JP_STORAGE,
            mesh = floatMesh(),
            material = Materials.brass,
            origin = V3(TANK_X + 0.24f, TANK_BOTTOM + 0.44f, TANK_Z),
            animate = { ctx -> Mat4.rotateZ(20f - 32f * tankLevel(ctx)) },
        ),
    )
    parts.add(
        Part(
            id = "jp_tank_outlet",
            name = "House supply pipe",
            system = JP_PIPE, assembly = JP_PIPEWORK,
            mesh = Geo.tube(
                listOf(
                    V3(TANK_X, TANK_BOTTOM + 0.04f, TANK_Z + 0.28f),
                    V3(1.49f, 1.86f, 0.28f),
                    V3(1.49f, 0.96f, 0.28f),
                    V3(1.42f, 0.88f, 0.30f),
                ),
                0.023f, 7, subdivide = 3,
            ),
            material = Materials.copperBright,
        ),
    )

    parts.add(
        Part(
            id = "jp_tap",
            name = "Garden tap",
            system = JP_HOUSE, assembly = JP_BUILDING,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.038f, 0.10f, 16).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(1.42f, 0.84f, 0.34f)))
                addMesh(Geo.cylinder(0.028f, 0.10f, 14), Mat4.translation(V3(1.42f, 0.79f, 0.38f)))
                addMesh(Geo.torus(0.042f, 0.010f, 16, 4).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(1.42f, 0.84f, 0.39f)))
            }.build(),
            material = Materials.brass,
        ),
    )
    parts.add(
        Part(
            id = "jp_tap_wheel",
            name = "Tap handwheel",
            system = JP_HOUSE, assembly = JP_BUILDING,
            mesh = tapWheelMesh(),
            material = Materials.emitCyan,
            origin = V3(1.42f, 0.94f, 0.40f),
            animate = { ctx -> Mat4.rotateZ(ctx.machineTime * (55f + 90f * ctx.control)) },
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "pump", "Jet pump and ejector",
            "The surface pump drives a jet of water down the well. The ejector turns that jet into a low-pressure throat, which pulls more water up from the aquifer.",
            "jp_pump_body", V3(0.02f, 0.05f, 0.08f),
            CameraPose(24f, 12f, 1.32f), readoutLabel = "PUMP",
        ),
        Hotspot(
            "impeller", "Impeller",
            "Six curved vanes spin inside the pump bowl. The impeller adds pressure to the water; the diffuser around it converts the fast flow into a steady head.",
            "jp_impeller", V3(0.04f, 0.08f, 0.08f),
            CameraPose(20f, 8f, 1.42f), readoutLabel = "ROTATION",
        ),
        Hotspot(
            "pressure", "Pressure vessel",
            "A trapped air charge pushes back against the water. The rubber diaphragm moves as the pump cycles, so the house sees a steady pressure instead of a pulse.",
            "jp_pressure_shell", V3(0.02f, 0.34f, 0.10f),
            CameraPose(32f, 20f, 1.30f), readoutLabel = "PRESSURE",
        ),
        Hotspot(
            "gauge", "Pressure gauge",
            "The needle follows the tank pressure. The pump cuts in near 1.6 bar and cuts out near 2.8 bar, which is why the pointer sweeps instead of standing still.",
            "jp_gauge", V3(0f, 0f, 0.04f),
            CameraPose(18f, 10f, 1.48f), readoutLabel = "BAR",
        ),
        Hotspot(
            "riser", "Riser to the tank",
            "The riser is the long climb from the wellhead to the roof. Its bends are swept, not square, so the moving water keeps its momentum and the pipe stays quiet.",
            "jp_riser", V3(-0.60f, 1.70f, 0.34f),
            CameraPose(36f, 18f, 1.14f), readoutLabel = "RISER",
        ),
        Hotspot(
            "tank", "Rooftop storage tank",
            "The tank is a buffer, not a pump. It lets the well pump run at its efficient rate while the house draws water whenever a tap opens.",
            "jp_tank_shell", V3(0.10f, 0.28f, 0.20f),
            CameraPose(28f, 12f, 1.26f), readoutLabel = "STORAGE",
        ),
        Hotspot(
            "float", "Float valve",
            "The float rides the surface. As the tank fills, the arm lifts and the inlet closes; as the house draws water, the arm drops and the valve opens again.",
            "jp_float", V3(-0.20f, 0f, 0f),
            CameraPose(24f, 8f, 1.46f), readoutLabel = "LEVEL",
        ),
        Hotspot(
            "tap", "House tap",
            "The tap is the demand side of the system. Every litre that leaves the tank is replaced by the pump, but not instantly: the tank absorbs the difference.",
            "jp_tap", V3(0.02f, 0f, 0.05f),
            CameraPose(24f, 8f, 1.38f), readoutLabel = "DEMAND",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Reach the aquifer",
            "A well is a vertical bore with a casing. The intake sits below the water table, and the casing keeps the bore from collapsing into the pump.",
            JP_WELL, CameraPose(30f, 14f, 1.04f), 0.15f, 0.25f,
        ),
        GuideStep(
            "Make a jet",
            "The surface pump pushes part of its flow back down the well. The ejector uses that jet to entrain well water, so the pump can lift from a deeper level than suction alone allows.",
            JP_PUMP, CameraPose(26f, 10f, 1.20f), 0.10f, 0.45f,
        ),
        GuideStep(
            "Smooth the pressure",
            "The pressure vessel stores energy in trapped air. The moving diaphragm absorbs each pump stroke and gives the house a steady flow when a tap opens.",
            JP_PRESSURE, CameraPose(34f, 18f, 1.24f), 0.25f, 0.60f,
        ),
        GuideStep(
            "Climb the riser",
            "The riser carries water to the roof. It is sized for low friction, because every metre of height costs pressure and every bend costs energy.",
            JP_PIPE, CameraPose(38f, 16f, 1.10f), 0.35f, 0.78f,
        ),
        GuideStep(
            "Float the fill",
            "The float valve closes the tank inlet when the surface reaches the set point. The tank then waits, full, until the house draws water.",
            JP_TANK, CameraPose(28f, 12f, 1.22f), 0.10f, 0.55f,
        ),
        GuideStep(
            "Open the tap",
            "The tap drops the pressure and the tank level falls. The pressure switch sees the drop, starts the pump, and the cycle begins again.",
            JP_HOUSE, CameraPose(24f, 10f, 1.30f), 0f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the system to separate the ground, well, pump set, pressure vessel, riser, tank and house. Every joint is a service point.",
            null, CameraPose(38f, 24f, 0.98f), 1f, 0.60f,
        ),
    )

    return ObjectSpec(
        id = "jet_pump_system",
        index = 18,
        title = "AQUA JET J-1",
        designation = "AJ-1 / WELL JET PUMP",
        category = "Home",
        categoryShort = "HOME",
        tagline = "From the water table to the roof.",
        summary = "A cutaway household water system: well casing, jet pump and ejector, moving pressure diaphragm, roof tank with float valve, riser and a working house tap.",
        details = listOf(
            "AQUA JET J-1 is a small waterworks in section. The well pump lifts water from below the ground, the pressure vessel smooths the flow, and the rooftop tank stores enough water for the house to draw at its own pace.",
            "The jet pump is the trick: part of the discharge is sent back down the well through a nozzle. The resulting jet pulls more water into the throat, so the surface pump can work against a deeper source without cavitating.",
        ),
        specs = listOf(
            SpecRow("Well depth", "18 m"),
            SpecRow("Pump", "0.75 kW jet"),
            SpecRow("Storage", "1,000 L"),
            SpecRow("Pressure", "1.6 – 2.8 bar"),
            SpecRow("Flow", "22 L/min"),
        ),
        tags = listOf("water", "pump", "jet pump", "well", "tank", "house", "plumbing", "home"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "DEMAND",
            lowLabel = "TANK FULL",
            highLabel = "TAP OPEN",
            defaultValue = 0.45f,
            hint = "House draw, tank level and pump cycling",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(36f, 16f, 1.30f),
        thumbnailPose = CameraPose(38f, 18f, 1.32f),
        thumbnailTime = 2.6f,
        thumbnailControl = 0.48f,
        readouts = { ctx ->
            val level = tankLevel(ctx)
            val pressure = 1.6f + 1.2f * pumpPressure(ctx)
            listOf(
                Readout("LEVEL", String.format("%.0f", level * 100f), "%", accent = true),
                Readout("PRESSURE", String.format("%.1f", pressure), "bar"),
                Readout("FLOW", String.format("%.0f", 6f + 18f * ctx.control), "L/min"),
            )
        },
        effects = { ctx ->
            val level = tankLevel(ctx)
            val flow = 0.10f + 0.18f * ctx.control
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.82f, 0xFF6EF2DF.toInt(), 0f, 0.20f),
                SceneEffect.Flow(
                    points = listOf(
                        V3(-1.16f, 0.48f, 0.04f),
                        V3(-1.15f, 0.95f, 0.04f),
                        V3(-1.08f, 1.15f, 0.10f),
                        V3(-0.88f, 1.18f, 0.08f),
                    ),
                    color = 0xFF7CE8FF.toInt(),
                    count = 8,
                    speed = 0.35f + 0.4f * ctx.control,
                    phase = 0.1f,
                    dotRadius = 0.018f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(-0.94f, 1.12f, 0.10f),
                        V3(-1.04f, 1.28f, 0.22f),
                        V3(-0.60f, 1.40f, 0.34f),
                        V3(-0.58f, 2.20f, 0.34f),
                        V3(0.52f, 2.22f, 0.34f),
                        V3(1.02f, 2.14f, 0.08f),
                    ),
                    color = 0xFF7CE8FF.toInt(),
                    count = 13,
                    speed = flow,
                    phase = 0.28f,
                    dotRadius = 0.020f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(1.42f, 0.80f, 0.38f),
                        V3(1.42f, 0.55f, 0.38f),
                        V3(1.42f, 0.34f, 0.38f),
                    ),
                    color = 0xFF7CE8FF.toInt(),
                    count = 7,
                    speed = 0.6f + 0.5f * ctx.control,
                    phase = 0.62f,
                    dotRadius = 0.016f,
                ),
                SceneEffect.Glow(V3(TANK_X, TANK_BOTTOM + TANK_HEIGHT * level - 0.02f, TANK_Z), 0.32f, 0xFF6EF2DF.toInt(), 0.18f + 0.12f * level),
                SceneEffect.Glow(V3(-1.16f, 0.57f, 0.12f), 0.22f, 0xFF7CE8FF.toInt(), 0.18f),
                SceneEffect.Glow(V3(1.16f, 1.72f, -0.10f), 0.24f, 0xFFFFB562.toInt(), 0.12f),
            )
        },
    )
}
