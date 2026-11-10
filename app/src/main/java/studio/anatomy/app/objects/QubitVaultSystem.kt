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
import kotlin.math.cos
import kotlin.math.sin

private const val QV_FRAME = "FRAME"
private const val QV_CRYO = "CRYOGENICS"
private const val QV_QUBIT = "QUBIT CORE"
private const val QV_OPTICS = "OPTICS"
private const val QV_CONTROL = "CONTROL"
private const val QV_VACUUM = "VACUUM"

private const val QV_PLINTH = "PLINTH"
private const val QV_TOWER = "CRYOSTAT"
private const val QV_STACK = "DILUTION STACK"
private const val QV_CHIP = "QUBIT CHIP"
private const val QV_BENCH = "OPTICAL BENCH"
private const val QV_RACK = "CONTROL RACK"

private val qvCenter = V3(0.10f, 1.46f, -0.06f)
private val qvChip = V3(0.10f, 0.94f, -0.06f)
private val qvGold = Material(
    "qubitGold", "OFHC gold stage", 0xFFD6AA5D.toInt(), specular = 0.72f, shininess = 42f,
    fillLight = 0.10f, rimLight = 0.18f, patina = 0.060f, edgeWear = 0.05f,
    pattern = SurfacePattern.PATINA, patternStrength = 0.24f,
)
private val qvCopper = Material(
    "qubitCopper", "Copper coax", 0xFFC2764C.toInt(), specular = 0.64f, shininess = 34f,
    fillLight = 0.10f, rimLight = 0.17f, patina = 0.065f,
    pattern = SurfacePattern.BRUSHED, patternStrength = 0.26f,
)
private val qvSapphire = Material(
    "qubitSapphire", "Sapphire window", 0xFF91D8FF.toInt(), specular = 0.76f, shininess = 58f,
    alpha = 44,
)
private val qvQubit = Material(
    "qubitCore", "Qubit lattice", 0xFFA18BFF.toInt(), emissive = true, specular = 0f,
)
private val qvScreen = Material(
    "qubitScreen", "Control display", 0xFF80F6E5.toInt(), emissive = true, specular = 0f,
)
private val qvCeramic = Material(
    "qubitCeramic", "Thermal ceramic", 0xFFDDD6C8.toInt(), specular = 0.20f, shininess = 14f,
    fillLight = 0.08f, rimLight = 0.14f, patina = 0.025f,
    pattern = SurfacePattern.SPECKLE, patternStrength = 0.18f,
)

private fun qvPoint(radius: Float, angle: Float, y: Float = 0f): V3 =
    V3(
        qvCenter.x + (cos(Math.toRadians(angle.toDouble())) * radius).toFloat(),
        y,
        qvCenter.z + (sin(Math.toRadians(angle.toDouble())) * radius).toFloat(),
    )

private fun qvFrameMesh(): Mesh = MeshBuilder().apply {
    val radius = 0.86f
    for (i in 0 until 4) {
        val angle = i * 90f + 45f
        val top = qvPoint(radius, angle, 2.66f)
        val bottom = qvPoint(radius, angle, 0.34f)
        addMesh(Geo.cylinderBetween(bottom, top, 0.026f, 8))
        addMesh(Geo.cylinder(0.075f, 0.05f, 12, bevel = 0.008f), Mat4.translation(bottom + V3(0f, 0.02f, 0f)))
        addMesh(Geo.cylinder(0.075f, 0.05f, 12, bevel = 0.008f), Mat4.translation(top - V3(0f, 0.02f, 0f)))
    }
    addMesh(
        Geo.torus(radius, 0.026f, 40, 5),
        Mat4.translation(V3(qvCenter.x, 2.60f, qvCenter.z)),
    )
    addMesh(
        Geo.torus(radius, 0.026f, 40, 5),
        Mat4.translation(V3(qvCenter.x, 0.40f, qvCenter.z)),
    )
}.build()

private fun qvShellMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.725f, 1.92f, 36), Mat4.translation(V3(qvCenter.x, 1.54f, qvCenter.z)))
    addMesh(Geo.torus(0.725f, 0.018f, 40, 4), Mat4.translation(V3(qvCenter.x, 0.58f, qvCenter.z)))
    addMesh(Geo.torus(0.725f, 0.018f, 40, 4), Mat4.translation(V3(qvCenter.x, 2.50f, qvCenter.z)))
    for (i in 0 until 8) {
        val p = qvPoint(0.735f, i * 45f, 1.54f)
        addMesh(Geo.box(0.030f, 0.36f, 0.012f), Mat4.translation(p))
    }
}.build()

private fun qvStackMesh(): Mesh = MeshBuilder().apply {
    val radii = listOf(0.56f, 0.49f, 0.42f, 0.35f, 0.28f)
    val heights = listOf(0.70f, 0.50f, 0.30f, 0.10f, -0.10f)
    radii.forEachIndexed { index, radius ->
        addMesh(Geo.cylinder(radius, 0.045f, 32, bevel = 0.010f), Mat4.translation(V3(0f, heights[index], 0f)))
        addMesh(Geo.torus(radius, 0.011f, 36, 4), Mat4.translation(V3(0f, heights[index] + 0.026f, 0f)))
        for (i in 0 until 4) {
            val angle = i * 90f + index * 8f
            addMesh(
                Geo.box(radius * 0.72f, 0.012f, 0.045f),
                Mat4.rotateY(angle).multiply(Mat4.translation(V3(0f, heights[index] + 0.035f, radius * 0.36f))),
            )
        }
    }
    // Central still column and the five copper thermal links.
    addMesh(Geo.cylinder(0.038f, 1.05f, 16), Mat4.translation(V3(0f, 0.28f, 0f)))
    for (i in 0 until 4) {
        val p = qvPoint(0.16f, i * 90f + 45f, 0f)
        addMesh(
            Geo.cylinderBetween(
                V3(p.x - qvCenter.x, 0.64f, p.z - qvCenter.z),
                V3(p.x - qvCenter.x, -0.18f, p.z - qvCenter.z),
                0.012f, 6,
            ),
        )
    }
}.build()

private fun qvTopMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.78f, 0.10f, 36, bevel = 0.018f), Mat4.translation(V3(qvCenter.x, 2.64f, qvCenter.z)))
    addMesh(Geo.torus(0.78f, 0.016f, 40, 4), Mat4.translation(V3(qvCenter.x, 2.69f, qvCenter.z)))
    for (i in 0 until 8) {
        val p = qvPoint(0.70f, i * 45f, 2.70f)
        addMesh(Geo.hexBolt(0.030f, 0.028f), Mat4.translation(p))
    }
    addMesh(Geo.cylinder(0.12f, 0.16f, 20, bevel = 0.018f), Mat4.translation(V3(qvCenter.x, 2.75f, qvCenter.z)))
    addMesh(
        Geo.tube(
            listOf(
                V3(qvCenter.x, 2.72f, qvCenter.z),
                V3(qvCenter.x, 2.86f, qvCenter.z + 0.10f),
                V3(qvCenter.x, 2.92f, qvCenter.z + 0.22f),
            ),
            0.034f, 8, subdivide = 3,
        ),
    )
}.build()

private fun qvCoaxMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 8) {
        val angle = i * 45f + 10f
        val start = qvPoint(0.56f, angle, 2.57f)
        val mid1 = qvPoint(0.62f, angle, 2.22f)
        val mid2 = qvPoint(0.48f, angle, 1.74f)
        val end = qvPoint(0.12f, angle * 0.4f, 1.00f)
        addMesh(
            Geo.tube(
                listOf(start, mid1, mid2, end),
                if (i % 2 == 0) 0.016f else 0.012f,
                6, subdivide = 3, caps = true,
            ),
        )
        addMesh(Geo.cylinder(0.027f, 0.045f, 10, bevel = 0.004f), Mat4.translation(end))
    }
}.build()

private fun qvLaserBenchMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.44f, 0.30f, 0.09f, cornerRadius = 0.03f, bevel = 0.008f), Mat4.translation(V3(-1.02f, 0.43f, 0.42f)))
    addMesh(Geo.box(0.28f, 0.12f, 0.18f), Mat4.translation(V3(-1.02f, 0.56f, 0.42f)))
    addMesh(Geo.cylinder(0.085f, 0.12f, 16, bevel = 0.012f), Mat4.translation(V3(-1.02f, 0.66f, 0.42f)))
    addMesh(Geo.torus(0.086f, 0.012f, 18, 4), Mat4.translation(V3(-1.02f, 0.72f, 0.42f)))
    addMesh(Geo.tube(listOf(V3(-1.02f, 0.67f, 0.42f), V3(-0.75f, 0.60f, 0.30f), V3(-0.26f, 0.55f, 0.10f)), 0.012f, 5, subdivide = 3))
    for (i in 0 until 3) {
        addMesh(Geo.box(0.018f, 0.14f, 0.018f), Mat4.translation(V3(-1.22f + i * 0.11f, 0.63f, 0.42f)))
    }
}.build()

private fun qvRackMesh(): Mesh = MeshBuilder().apply {
    for (side in listOf(-1f, 1f)) {
        val x = qvCenter.x + side * 0.98f
        val z = -0.02f
        addMesh(Geo.slab(0.34f, 0.46f, 0.78f, cornerRadius = 0.025f, bevel = 0.010f), Mat4.translation(V3(x, 0.68f, z)))
        addMesh(Geo.box(0.23f, 0.16f, 0.012f), Mat4.translation(V3(x, 0.88f, z + 0.238f)))
        for (i in 0 until 4) {
            addMesh(Geo.box(0.19f, 0.018f, 0.016f), Mat4.translation(V3(x, 0.56f + i * 0.10f, z + 0.238f)))
        }
        addMesh(Geo.box(0.045f, 0.035f, 0.014f), Mat4.translation(V3(x + 0.10f, 0.93f, z + 0.245f)))
    }
}.build()

private fun qvVacuumMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.20f, 0.62f, 20, bevel = 0.02f), Mat4.translation(V3(-1.05f, 0.62f, -0.63f)))
    addMesh(Geo.torus(0.20f, 0.014f, 22, 4), Mat4.translation(V3(-1.05f, 0.92f, -0.63f)))
    addMesh(Geo.cylinder(0.14f, 0.18f, 16), Mat4.translation(V3(-1.05f, 0.28f, -0.63f)))
    addMesh(Geo.tube(listOf(V3(-1.05f, 0.92f, -0.63f), V3(-1.02f, 1.20f, -0.55f), V3(-0.88f, 1.38f, -0.38f), V3(-0.66f, 1.43f, -0.24f)), 0.028f, 6, subdivide = 3))
    addMesh(Geo.tube(listOf(V3(-1.05f, 0.30f, -0.63f), V3(-0.96f, 0.34f, -0.42f), V3(-0.72f, 0.40f, -0.20f)), 0.022f, 6, subdivide = 2))
}.build()

private fun qvScannerHeadMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.065f, 0.035f, 18, bevel = 0.005f))
    addMesh(Geo.cylinder(0.018f, 0.060f, 10), Mat4.translation(V3(0f, 0.040f, 0f)))
    for (i in 0 until 4) {
        addMesh(
            Geo.box(0.105f, 0.012f, 0.018f),
            Mat4.rotateY(i * 45f).multiply(Mat4.translation(V3(0.058f, 0.023f, 0f))),
        )
    }
}.build()

private fun qvVacuumRotorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.110f, 0.022f, 20, bevel = 0.004f), Mat4.translation(V3(0f, 0.018f, 0f)))
    addMesh(Geo.cylinder(0.018f, 0.040f, 10), Mat4.translation(V3(0f, 0.045f, 0f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.020f, 0.018f, 0.095f),
            Mat4.rotateY(i * 72f).multiply(Mat4.translation(V3(0f, 0.030f, 0.052f))),
        )
    }
}.build()

private fun qvScanGantryMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.92f, 0.018f, 42, 4), Mat4.translation(V3(0f, 0.46f, 0f)))
    addMesh(Geo.box(0.32f, 0.012f, 0.020f), Mat4.translation(V3(0.72f, 0.46f, 0f)))
    addMesh(Geo.box(0.060f, 0.040f, 0.046f), Mat4.translation(V3(0.86f, 0.46f, 0f)))
    for (i in 0 until 4) {
        val angle = i * 90f
        addMesh(
            Geo.box(0.060f, 0.050f, 0.060f),
            Mat4.rotateY(angle).multiply(Mat4.translation(V3(0f, 0.46f, 0.92f))),
        )
        addMesh(
            Geo.box(0.16f, 0.010f, 0.018f),
            Mat4.rotateY(angle).multiply(Mat4.translation(V3(0f, 0.42f, 0.83f))),
        )
    }
}.build()

private fun qvRackScanMesh(): Mesh = MeshBuilder().apply {
    for (side in listOf(-1f, 1f)) {
        val x = qvCenter.x + side * 0.98f
        addMesh(Geo.box(0.20f, 0.012f, 0.010f), Mat4.translation(V3(x, 0.72f, 0.230f)))
        addMesh(Geo.box(0.045f, 0.010f, 0.010f), Mat4.translation(V3(x - 0.072f, 0.72f, 0.238f)))
    }
}.build()

fun qubitVaultSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "qubit",
            width = 2.96f,
            depth = 2.16f,
            top = 0.30f,
            system = QV_FRAME,
            assembly = QV_PLINTH,
            accentMaterial = Materials.emitBlue,
        ),
    )

    parts.add(
        Part(
            id = "qv_frame", name = "Cryostat gantry", system = QV_FRAME, assembly = QV_TOWER,
            mesh = qvFrameMesh(), material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "qv_shell", name = "Sapphire vacuum bell", system = QV_CRYO, assembly = QV_TOWER,
            mesh = qvShellMesh(), material = qvSapphire,
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "qv_scan_gantry", name = "Cryostat scan gantry", system = QV_CRYO, assembly = QV_TOWER,
            mesh = qvScanGantryMesh(), material = qvScreen,
            origin = qvCenter,
            animate = { ctx ->
                Mat4.rotateX(13f * sin(ctx.machineTime * 0.82f))
                    .multiply(Mat4.rotateZ(9f * sin(ctx.machineTime * 0.64f + 1.1f)))
                    .multiply(Mat4.rotateY(ctx.machineTime * 42f))
            },
        ),
    )
    parts.add(
        Part(
            id = "qv_top", name = "Top flange and insert", system = QV_CRYO, assembly = QV_TOWER,
            mesh = qvTopMesh(), material = qvGold,
        ),
    )
    parts.add(
        Part(
            id = "qv_stages", name = "Dilution refrigerator stages", system = QV_CRYO, assembly = QV_STACK,
            mesh = qvStackMesh(), material = qvGold,
            origin = qvCenter,
            animate = { ctx ->
                Mat4.translation(V3(0f, 0.032f * sin(ctx.machineTime * 1.7f), 0f))
                    .multiply(Mat4.rotateY(ctx.machineTime * 28f))
            },
        ),
    )
    parts.add(
        Part(
            id = "qv_stage_rods", name = "Thermal isolation rods", system = QV_CRYO, assembly = QV_STACK,
            mesh = MeshBuilder().apply {
                for (i in 0 until 8) {
                    val p = qvPoint(0.30f, i * 45f, 0f)
                    addMesh(Geo.cylinderBetween(V3(p.x, 2.38f, p.z), V3(p.x, 1.40f, p.z), 0.012f, 6))
                }
            }.build(),
            material = qvCopper,
        ),
    )
    parts.add(
        Part(
            id = "qv_chip", name = "Superconducting qubit lattice", system = QV_QUBIT, assembly = QV_CHIP,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.31f, 0.31f, 0.060f, cornerRadius = 0.025f, bevel = 0.010f, segments = 2))
                addMesh(Geo.box(0.20f, 0.012f, 0.20f), Mat4.translation(V3(0f, 0.038f, 0f)))
                for (i in 0 until 4) {
                    val angle = i * 90f
                    addMesh(Geo.box(0.10f, 0.012f, 0.012f), Mat4.rotateY(angle).multiply(Mat4.translation(V3(0.0f, 0.040f, 0.16f))))
                }
                val pins = ArrayList<V3>()
                for (x in -1..1) for (z in -1..1) pins.add(V3(x * 0.14f, 0.0f, z * 0.14f))
                addMesh(Geo.boltSet(pins, 0.018f, 0.025f), Mat4.translation(V3(0f, 0.06f, 0f)))
            }.build(),
            material = qvQubit,
            origin = qvChip,
            animate = { ctx ->
                val pulse = 1f + 0.075f * sin(ctx.machineTime * 5.1f)
                Mat4.rotateY(ctx.machineTime * 34f)
                    .multiply(Mat4.scale(V3(pulse, pulse, pulse)))
            },
        ),
    )
    parts.add(
        Part(
            id = "qv_chip_ring", name = "Qubit shield ring", system = QV_QUBIT, assembly = QV_CHIP,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.25f, 0.014f, 32, 5))
                addMesh(Geo.torus(0.30f, 0.008f, 32, 4), Mat4.translation(V3(0f, -0.03f, 0f)))
            }.build(),
            material = qvCopper,
            origin = qvChip,
            animate = { ctx ->
                Mat4.translation(V3(0f, 0.020f * sin(ctx.machineTime * 2.4f), 0f))
                    .multiply(Mat4.rotateX(22f * sin(ctx.machineTime * 1.25f)))
                    .multiply(Mat4.rotateZ(16f * cos(ctx.machineTime * 1.05f)))
            },
        ),
    )
    parts.add(
        Part(
            id = "qv_coax", name = "Coaxial control harness", system = QV_QUBIT, assembly = QV_CHIP,
            mesh = qvCoaxMesh(), material = qvCopper,
        ),
    )
    parts.add(
        Part(
            id = "qv_laser", name = "Optical readout bench", system = QV_OPTICS, assembly = QV_BENCH,
            mesh = qvLaserBenchMesh(), material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "qv_laser_beam", name = "Readout beam path", system = QV_OPTICS, assembly = QV_BENCH,
            mesh = Geo.tube(
                listOf(V3(-1.02f, 0.68f, 0.42f), V3(-0.72f, 0.61f, 0.30f), V3(-0.30f, 0.53f, 0.08f), V3(0.08f, 0.98f, 0.02f)),
                0.011f, 5, subdivide = 3,
            ),
            material = qvScreen,
        ),
    )
    parts.add(
        Part(
            id = "qv_readout_scanner", name = "Readout scanner mirror", system = QV_OPTICS, assembly = QV_BENCH,
            mesh = qvScannerHeadMesh(), material = qvScreen,
            origin = V3(-1.02f, 0.82f, 0.42f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 220f) },
        ),
    )
    parts.add(
        Part(
            id = "qv_racks", name = "Control and readout racks", system = QV_CONTROL, assembly = QV_RACK,
            mesh = qvRackMesh(), material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "qv_rack_screens", name = "Rack instrument faces", system = QV_CONTROL, assembly = QV_RACK,
            mesh = MeshBuilder().apply {
                for (side in listOf(-1f, 1f)) {
                    val x = qvCenter.x + side * 0.98f
                    addMesh(Geo.box(0.20f, 0.075f, 0.008f), Mat4.translation(V3(x, 0.86f, 0.214f)))
                    addMesh(Geo.box(0.20f, 0.012f, 0.008f), Mat4.translation(V3(x, 0.72f, 0.214f)))
                    addMesh(Geo.box(0.09f, 0.012f, 0.008f), Mat4.translation(V3(x, 0.64f, 0.214f)))
                }
            }.build(),
            material = qvScreen,
        ),
    )
    parts.add(
        Part(
            id = "qv_rack_scan", name = "Rack scan indicator", system = QV_CONTROL, assembly = QV_RACK,
            mesh = qvRackScanMesh(), material = qvScreen,
            animate = { ctx -> Mat4.translation(V3(0f, 0.10f * sin(ctx.machineTime * 2.2f), 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "qv_vacuum", name = "Turbo vacuum pump", system = QV_VACUUM, assembly = QV_BENCH,
            mesh = qvVacuumMesh(), material = Materials.brushedSteel,
        ),
    )
    parts.add(
        Part(
            id = "qv_vacuum_glow", name = "Vacuum gauges", system = QV_VACUUM, assembly = QV_BENCH,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.032f, 0.012f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(-1.05f, 0.75f, -0.42f)))
                addMesh(Geo.cylinder(0.026f, 0.012f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(-1.05f, 0.54f, -0.42f)))
            }.build(),
            material = qvScreen,
        ),
    )
    parts.add(
        Part(
            id = "qv_vacuum_rotor", name = "Vacuum pump rotor", system = QV_VACUUM, assembly = QV_BENCH,
            mesh = qvVacuumRotorMesh(), material = qvScreen,
            origin = V3(-1.05f, 0.94f, -0.63f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 300f) },
        ),
    )
    parts.add(
        Part(
            id = "qv_base_bus", name = "Cryogenic and data bus", system = QV_FRAME, assembly = QV_PLINTH,
            mesh = MeshBuilder().apply {
                addMesh(Geo.tube(listOf(V3(-1.40f, 0.37f, 0.83f), V3(-0.89f, 0.39f, 0.80f), V3(-0.21f, 0.36f, 0.86f), V3(0.54f, 0.37f, 0.82f)), 0.020f, 6, subdivide = 4))
                addMesh(Geo.box(0.62f, 0.040f, 0.16f), Mat4.translation(V3(0.02f, 0.32f, 0.91f)))
                addMesh(Geo.cylinder(0.032f, 0.018f, 12), Mat4.translation(V3(-1.36f, 0.36f, 0.83f)))
                addMesh(Geo.cylinder(0.032f, 0.018f, 12), Mat4.translation(V3(1.32f, 0.36f, 0.83f)))
            }.build(),
            material = qvCopper,
        ),
    )
    parts.add(
        Part(
            id = "qv_light", name = "Cryostat status ring", system = QV_FRAME, assembly = QV_TOWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.76f, 0.014f, 40, 4), Mat4.translation(V3(qvCenter.x, 0.46f, qvCenter.z)))
            }.build(),
            material = qvScreen,
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "chip", "Qubit lattice",
            "The chip holds a grid of superconducting qubits. Each qubit is a tiny resonant circuit that can sit in superposition and become entangled with its neighbours.",
            "qv_chip", V3(0f, 0.02f, 0.08f),
            CameraPose(30f, 14f, 1.06f), readoutLabel = "QUANTUM PROCESSOR",
        ),
        Hotspot(
            "stages", "Dilution refrigerator",
            "The gold plates are thermal stages. Each stage gets colder than the one above it, down to a few thousandths of a kelvin, where the qubits become superconducting.",
            "qv_stages", V3(0f, 0.20f, 0.43f),
            CameraPose(28f, 18f, 1f), readoutLabel = "MILLIKELVIN STACK",
        ),
        Hotspot(
            "coax", "Coaxial control harness",
            "Eight coaxial lines carry microwave pulses down to the chip and bring the readout signals back up. Attenuators on every stage absorb heat before it reaches the qubits.",
            "qv_coax", V3(0.18f, 0.92f, 0.40f),
            CameraPose(48f, 20f, 1f), readoutLabel = "MICROWAVE CONTROL",
        ),
        Hotspot(
            "shell", "Vacuum and radiation shield",
            "The sapphire bell keeps air out, while the nested shields block room-temperature radiation. Even a tiny infrared leak can disturb the quantum state.",
            "qv_shell", V3(0f, 0.65f, 0.68f),
            CameraPose(150f, 16f, 1f), readoutLabel = "THERMAL ISOLATION",
        ),
        Hotspot(
            "laser", "Optical readout bench",
            "The readout laser is split, modulated and delivered through optical fibres. It probes the qubit state without adding a room-temperature electrical connection.",
            "qv_laser", V3(0f, 0.18f, 0.00f),
            CameraPose(38f, 14f, 1f), readoutLabel = "STATE READOUT",
        ),
        Hotspot(
            "racks", "Control and error correction",
            "The racks turn algorithms into microwave pulses and decode the measurement results. Error correction combines many physical qubits into one logical qubit.",
            "qv_racks", V3(0.98f, 0.22f, 0.22f),
            CameraPose(18f, 14f, 1f), readoutLabel = "CLASSICAL CONTROL",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Cool the stack",
            "The pulse-tube cooler and dilution refrigerator remove heat stage by stage. The chip waits at the bottom of the stack, colder than deep space.",
            QV_CRYO, CameraPose(28f, 18f, 1.04f), 0.16f, 0.30f,
        ),
        GuideStep(
            "Write a pulse",
            "A microwave pulse at the qubit frequency rotates its state. The coaxial harness filters and attenuates every line so noise cannot leak into the chip.",
            QV_QUBIT, CameraPose(30f, 14f, 1.08f), 0.08f, 0.62f,
        ),
        GuideStep(
            "Entangle",
            "A coupler between neighbouring qubits transfers a controlled interaction. The pair becomes entangled, so one measurement instantly constrains the other state.",
            QV_QUBIT, CameraPose(34f, 18f, 1.06f), 0.10f, 0.84f,
        ),
        GuideStep(
            "Read",
            "The readout resonator shifts its frequency depending on the qubit state. A microwave tone measures that shift and the control rack turns it into a classical bit.",
            QV_OPTICS, CameraPose(38f, 14f, 1.03f), 0.06f, 0.52f,
        ),
        GuideStep(
            "Correct",
            "Physical qubits are noisy, so many of them are combined into one logical qubit. The control stack detects and corrects errors faster than they accumulate.",
            QV_CONTROL, CameraPose(18f, 14f, 1.02f), 0.04f, 0.42f,
        ),
        GuideStep(
            "The assembly",
            "Explode the cryostat, stage stack, chip, optical bench, racks and vacuum pump to see the machine behind the quantum state.",
            null, CameraPose(32f, 18f, 0.96f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "qubit_vault",
        index = 24,
        title = "QUBIT Q-0",
        designation = "QV-0 / CRYOGENIC QUANTUM CORE",
        category = "Computing",
        categoryShort = "QNT",
        tagline = "Cold enough to think in many states at once.",
        summary = "A machine-room quantum computer: a sapphire cryostat, five-stage dilution refrigerator made from gold and copper, a superconducting qubit lattice, a coaxial control harness, an optical readout bench and a rack-scale classical controller.",
        details = listOf(
            "QUBIT Q-0 is a superconducting quantum computer. The dilution refrigerator cools its processor into the millikelvin range, where electrical resistance disappears and a qubit can hold a quantum state long enough to compute.",
            "Microwave pulses write and couple quantum states, while readout resonators report the result. The gold stages, sapphire vacuum bell and dense coaxial harness are all part of the same problem: keeping heat and noise away from the chip.",
        ),
        specs = listOf(
            SpecRow("Qubits", "128 superconducting"),
            SpecRow("Base temp", "9.4 mK"),
            SpecRow("Coherence", "284 µs"),
            SpecRow("Readout", "Dispersive"),
            SpecRow("Cryostat", "5-stage"),
        ),
        tags = listOf("quantum", "computer", "qubit", "cryostat", "computing", "physics", "laboratory", "future"),
        accent = 0xFFA88CFF.toInt(),
        control = ControlSpec(
            label = "COHERENCE",
            lowLabel = "IDLE",
            highLabel = "ENTANGLED",
            defaultValue = 0.52f,
            hint = "Qubit excitation, readout pulse and error-correction load",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(30f, 15f, 1.03f),
        thumbnailPose = CameraPose(31f, 17f, 1.05f),
        thumbnailTime = 2.6f,
        thumbnailControl = 0.68f,
        focusOffset = V3(0f, 0.08f, 0f),
        readouts = { ctx ->
            val temp = 9.4f - 2.7f * ctx.control
            val fidelity = 97.2f + 2.4f * ctx.control
            val depth = 140f + 460f * ctx.control
            listOf(
                Readout("QUBITS", "128", "", accent = true),
                Readout("TEMP", String.format("%.1f", temp), "mK"),
                Readout("FIDELITY", String.format("%.1f", fidelity), "%"),
            )
        },
        effects = { ctx ->
            val activity = (0.25f + 0.75f * ctx.control).coerceIn(0f, 1f)
            val flicker = 0.75f + 0.25f * sin(ctx.machineTime * 5.3f)
            val beamPath = listOf(
                V3(-1.02f, 0.68f, 0.42f), V3(-0.72f, 0.61f, 0.30f),
                V3(-0.30f, 0.53f, 0.08f), V3(0.08f, 0.98f, 0.02f),
            )
            val chipOrbit = (0..24).map { index ->
                val angle = Math.toRadians(index * (360.0 / 24.0))
                V3(
                    qvCenter.x + (0.34 * cos(angle)).toFloat(),
                    0.99f + 0.02f * sin(ctx.machineTime * 2.0f + index * 0.26f),
                    qvCenter.z + (0.34 * sin(angle)).toFloat(),
                )
            } + V3(qvCenter.x + 0.34f, 0.99f, qvCenter.z)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.55f, 0xFFA88CFF.toInt(), 0f, 0.13f),
                SceneEffect.Glow(V3(0.10f, 0.98f, -0.06f), 0.28f, 0xFFC5B8FF.toInt(), 0.20f + 0.24f * activity),
                SceneEffect.Glow(V3(0.10f, 1.82f, -0.06f), 0.50f, 0xFFA88CFF.toInt(), 0.06f + 0.10f * activity),
                SceneEffect.Beam(V3(-1.02f, 0.72f, 0.42f), V3(0.10f, 1.00f, -0.06f), 0xFF9BEFE8.toInt(), 0.14f + 0.28f * activity),
                SceneEffect.Spark(V3(0.10f, 2.76f, -0.06f), 0xFFC8BBFF.toInt(), 0.025f, 0.22f + 0.28f * flicker),
                SceneEffect.Flow(
                    listOf(V3(-1.20f, 0.38f, 0.82f), V3(-0.25f, 0.38f, 0.86f), V3(0.85f, 0.38f, 0.84f)),
                    0xFFA88CFF.toInt(), count = 10, speed = 0.24f, dotRadius = 0.020f,
                ),
                SceneEffect.Spark(V3(1.08f, 0.91f, 0.22f), 0xFF8AF8EA.toInt(), 0.020f, 0.18f + 0.20f * activity),
                SceneEffect.Flow(
                    beamPath, 0xFF9BEFE8.toInt(), count = 10, speed = 0.48f, phase = 0.12f, dotRadius = 0.012f,
                ),
                SceneEffect.Flow(
                    chipOrbit, 0xFFC7B8FF.toInt(), count = 14, speed = 0.36f, phase = 0.20f, dotRadius = 0.011f,
                ),
            )
        },
        descriptionTitle = "The cold quantum machine",
    )
}
