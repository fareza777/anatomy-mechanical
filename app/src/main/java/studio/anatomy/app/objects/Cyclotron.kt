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

private const val CY_MAGNET = "MAGNET"
private const val CY_VACUUM = "VACUUM"
private const val CY_RF = "RF DRIVE"
private const val CY_BEAM = "BEAM"
private const val CY_EXTRACT = "EXTRACTION"
private const val CY_CRYO = "CRYOGENICS"
private const val CY_CONTROL = "CONTROL"

private const val CY_PLINTH = "PLINTH"
private const val CY_YOKE = "YOKE"
private const val CY_CHAMBER = "CHAMBER"
private const val CY_CAVITY = "RF CAVITY"
private const val CY_LINE = "BEAMLINE"
private const val CY_TARGET = "TARGET"
private const val CY_PLANT = "CRYO PLANT"
private const val CY_RACK = "CONTROL RACK"

private const val CY_Y = 1.06f

private val cyIron = Material(
    "cyclotronIron", "Laminated iron", 0xFF57656B.toInt(), specular = 0.38f, shininess = 20f,
    fillLight = 0.09f, rimLight = 0.15f, patina = 0.035f, edgeWear = 0.04f,
    pattern = SurfacePattern.PANEL, patternStrength = 0.50f,
)
private val cyGlass = Material(
    "cyclotronGlass", "Chamber glass", 0xFF9FE7F2.toInt(), specular = 0.74f, shininess = 60f,
    alpha = 40, specTint = 0xFFD8FFFB.toInt(),
)
private val cyScreen = Material(
    "cyclotronScreen", "Console display", 0xFF8CF6E8.toInt(), emissive = true, specular = 0f,
)
private val cyBeam = Material(
    "cyclotronBeam", "Ion beam", 0xFFA8D4FF.toInt(), emissive = true, specular = 0f,
)
private val cyTungsten = Material(
    "cyclotronTarget", "Tungsten target", 0xFF9BA6AC.toInt(), specular = 0.50f, shininess = 24f,
    fillLight = 0.09f, rimLight = 0.16f, patina = 0.050f, edgeWear = 0.05f,
)
private val cyCeramic = Material(
    "cyclotronCeramic", "Ceramic insulator", 0xFFD9D3C4.toInt(), specular = 0.22f, shininess = 14f,
    fillLight = 0.08f, rimLight = 0.14f, pattern = SurfacePattern.SPECKLE, patternStrength = 0.18f,
)

private fun cyPoint(radius: Float, angleDeg: Float): V3 {
    val angle = Math.toRadians(angleDeg.toDouble())
    return V3((radius * cos(angle)).toFloat(), 0f, (radius * sin(angle)).toFloat())
}

private fun cyYokeMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(1.12f, 0.28f, 44, bevel = 0.045f), Mat4.translation(V3(0f, 0.44f, 0f)))
    addMesh(Geo.cylinder(1.12f, 0.24f, 44, bevel = 0.040f), Mat4.translation(V3(0f, 1.70f, 0f)))
    addMesh(Geo.cylinder(1.05f, 0.05f, 40, bevel = 0.010f), Mat4.translation(V3(0f, 0.60f, 0f)))
    addMesh(Geo.cylinder(1.05f, 0.05f, 40, bevel = 0.010f), Mat4.translation(V3(0f, 1.56f, 0f)))
    for (i in 0 until 4) {
        val p = cyPoint(1.34f, 45f + i * 90f)
        addMesh(Geo.cylinder(0.115f, 1.32f, 14, bevel = 0.020f), Mat4.translation(V3(p.x, 1.07f, p.z)))
        addMesh(Geo.slab(0.34f, 0.34f, 0.06f, cornerRadius = 0.03f, bevel = 0.015f), Mat4.translation(V3(p.x, 0.33f, p.z)))
    }
    addMesh(Geo.torus(1.34f, 0.075f, 44, 6), Mat4.translation(V3(0f, 1.84f, 0f)))
    addMesh(Geo.cylinder(1.31f, 0.10f, 44, bevel = 0.020f), Mat4.translation(V3(0f, 1.90f, 0f)))
    for (i in 0 until 8) {
        val p = cyPoint(1.10f, i * 45f + 22.5f)
        addMesh(Geo.hexBolt(0.040f, 0.030f), Mat4.translation(V3(p.x, 1.945f, p.z)))
    }
}.build()

private fun cyCoilMesh(): Mesh = MeshBuilder().apply {
    for (y in listOf(0.68f, 1.50f)) {
        addMesh(Geo.torus(1.17f, 0.115f, 48, 8), Mat4.translation(V3(0f, y, 0f)))
        for (i in 0 until 8) {
            addMesh(
                Geo.box(0.16f, 0.34f, 0.10f),
                Mat4.rotateY(-i * 45f).multiply(Mat4.translation(V3(0f, y, 1.20f))),
            )
        }
    }
}.build()

private fun cyChamberMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.hollowCylinder(1.05f, 0.985f, 0.34f, 48), Mat4.translation(V3(0f, CY_Y, 0f)))
    addMesh(Geo.torus(1.05f, 0.018f, 48, 4), Mat4.translation(V3(0f, CY_Y - 0.17f, 0f)))
    addMesh(Geo.torus(1.05f, 0.018f, 48, 4), Mat4.translation(V3(0f, CY_Y + 0.17f, 0f)))
    for (i in 0 until 8) {
        val angle = i * 45f
        addMesh(
            Geo.box(0.11f, 0.20f, 0.06f),
            Mat4.rotateY(-angle).multiply(Mat4.translation(V3(0f, CY_Y, 1.05f))),
        )
    }
}.build()

private fun cyExtractionMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.tube(
            listOf(
                V3(0.55f, CY_Y, 0.62f),
                V3(1.02f, CY_Y - 0.01f, 0.70f),
                V3(1.35f, CY_Y - 0.08f, 0.74f),
                V3(1.52f, CY_Y - 0.20f, 0.74f),
            ),
            0.052f, 10, subdivide = 4,
        ),
    )
    addMesh(Geo.box(0.20f, 0.05f, 0.10f), Mat4.translation(V3(0.92f, CY_Y, 0.66f)))
    addMesh(Geo.torus(0.075f, 0.014f, 16, 4), Mat4.translation(V3(1.44f, CY_Y - 0.15f, 0.74f)))
    addMesh(Geo.cylinder(0.10f, 0.14f, 14, bevel = 0.015f), Mat4.translation(V3(1.58f, CY_Y - 0.22f, 0.74f)))
}.build()

private fun cyCavityMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.44f, 0.46f, 0.46f), Mat4.translation(V3(1.46f, CY_Y, 0f)))
    addMesh(Geo.cylinder(0.10f, 0.30f, 16).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(1.20f, CY_Y, 0f)))
    addMesh(Geo.torus(0.24f, 0.020f, 20, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(1.24f, CY_Y, 0f)))
    addMesh(Geo.box(0.10f, 0.12f, 0.56f), Mat4.translation(V3(1.46f, CY_Y + 0.26f, 0f)))
    for (i in 0 until 4) {
        addMesh(
            Geo.box(0.05f, 0.05f, 0.05f),
            Mat4.translation(V3(1.66f, CY_Y + 0.18f, -0.17f + i * 0.11f)),
        )
    }
}.build()

private fun cyFinMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 5) {
        addMesh(Geo.box(0.020f, 0.34f, 0.34f), Mat4.translation(V3(-0.16f + i * 0.08f, 0f, 0f)))
    }
}.build()

private fun cyPistonMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.075f, 0.24f, 14, bevel = 0.010f).transformed(Mat4.rotateZ(-90f)))
    addMesh(Geo.cylinder(0.105f, 0.06f, 16, bevel = 0.010f).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-0.14f, 0f, 0f)))
    addMesh(Geo.torus(0.078f, 0.010f, 14, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0.12f, 0f, 0f)))
}.build()

private fun cyTargetMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.27f, 0.05f, 24, bevel = 0.010f).transformed(Mat4.rotateZ(90f)))
    addMesh(Geo.torus(0.27f, 0.012f, 24, 4).transformed(Mat4.rotateZ(90f)))
    for (i in 0 until 6) {
        addMesh(
            Geo.cylinder(0.045f, 0.02f, 12).transformed(Mat4.rotateZ(90f)),
            Mat4.rotateX(i * 60f).multiply(Mat4.translation(V3(0.055f, 0.19f, 0f))),
        )
    }
    addMesh(Geo.cylinder(0.05f, 0.08f, 10).transformed(Mat4.rotateZ(90f)))
}.build()

private fun cyDoorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.06f, 0.52f, 0.44f), Mat4.translation(V3(0f, 0.30f, 0f)))
    addMesh(Geo.cylinder(0.035f, 0.12f, 10).transformed(Mat4.rotateZ(90f)))
    addMesh(Geo.box(0.020f, 0.10f, 0.30f), Mat4.translation(V3(0.04f, 0.42f, 0f)))
    addMesh(Geo.box(0.05f, 0.05f, 0.05f), Mat4.translation(V3(0f, 0.56f, 0f)))
}.build()

private fun cyCryoMesh(): Mesh = MeshBuilder().apply {
    for (z in listOf(-0.78f, 0.78f)) {
        addMesh(Geo.cylinder(0.20f, 0.62f, 20, bevel = 0.030f), Mat4.translation(V3(-1.42f, 0.62f, z)))
        addMesh(Geo.torus(0.20f, 0.016f, 20, 4), Mat4.translation(V3(-1.42f, 0.90f, z)))
        addMesh(Geo.cylinder(0.24f, 0.06f, 20), Mat4.translation(V3(-1.42f, 0.34f, z)))
        addMesh(
            Geo.tube(
                listOf(V3(-1.42f, 1.10f, z), V3(-1.26f, 1.28f, z * 0.9f), V3(-0.88f, 1.34f, z * 0.7f)),
                0.028f, 6, subdivide = 3,
            ),
        )
        addMesh(Geo.cylinder(0.032f, 0.10f, 10), Mat4.translation(V3(-1.42f, 0.97f, z)))
    }
    addMesh(Geo.box(0.60f, 0.10f, 1.30f), Mat4.translation(V3(-1.45f, 0.33f, 0f)))
}.build()

private fun cyRotorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.115f, 0.030f, 16, bevel = 0.006f), Mat4.translation(V3(0f, 0.02f, 0f)))
    for (i in 0 until 6) {
        addMesh(
            Geo.box(0.018f, 0.016f, 0.090f),
            Mat4.rotateY(i * 60f).multiply(Mat4.translation(V3(0f, 0.030f, 0.050f))),
        )
    }
}.build()

private fun cyRackMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.62f, 0.44f, 0.86f, cornerRadius = 0.04f, bevel = 0.020f), Mat4.translation(V3(1.02f, 0.62f, -0.98f)))
    for (i in 0 until 4) {
        addMesh(Geo.box(0.48f, 0.10f, 0.012f), Mat4.translation(V3(1.02f, 0.44f + i * 0.16f, -0.755f)))
    }
}.build()

private fun cyScreenMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 4) {
        addMesh(Geo.box(0.42f, 0.07f, 0.008f), Mat4.translation(V3(1.02f, 0.44f + i * 0.16f, -0.748f)))
    }
    addMesh(Geo.box(0.05f, 0.03f, 0.02f), Mat4.translation(V3(1.24f, 0.92f, -0.75f)))
}.build()

private fun cyRailMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(2.10f, 0.06f, 0.10f), Mat4.translation(V3(0.10f, 2.04f, 0f)))
    for (x in listOf(-0.86f, 1.02f)) {
        addMesh(Geo.cylinder(0.05f, 0.60f, 10), Mat4.translation(V3(x, 1.74f, 0f)))
    }
    addMesh(Geo.box(0.20f, 0.05f, 0.20f), Mat4.translation(V3(-0.86f, 1.44f, 0f)))
}.build()

private fun cyTrolleyMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.26f, 0.10f, 0.20f), Mat4.translation(V3(0f, -0.05f, 0f)))
    addMesh(Geo.cylinder(0.020f, 0.42f, 8), Mat4.translation(V3(0f, -0.28f, 0f)))
    addMesh(Geo.box(0.10f, 0.08f, 0.10f), Mat4.translation(V3(0f, -0.50f, 0f)))
    addMesh(Geo.torus(0.05f, 0.012f, 12, 4), Mat4.translation(V3(0f, -0.54f, 0f)))
}.build()

private fun cyTrimMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 8) {
        val angle = i * 45f
        addMesh(
            Geo.torus(0.085f, 0.022f, 14, 4).transformed(Mat4.rotateX(90f)),
            Mat4.rotateY(-angle).multiply(Mat4.translation(V3(0f, 0f, 1.10f))),
        )
    }
}.build()

private fun cyValveMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.020f, 0.075f, 8).transformed(Mat4.rotateX(90f)))
    addMesh(
        Geo.torus(0.075f, 0.014f, 16, 4).transformed(Mat4.rotateX(90f)),
        Mat4.translation(V3(0f, 0f, 0.040f)),
    )
    for (i in 0 until 3) {
        addMesh(
            Geo.box(0.11f, 0.012f, 0.012f),
            Mat4.translation(V3(0f, 0f, 0.040f)).multiply(Mat4.rotateZ(i * 60f)),
        )
    }
}.build()

private fun cySweepMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.86f, 0.020f, 0.020f), Mat4.translation(V3(0.45f, 0f, 0f)))
    addMesh(Geo.box(0.10f, 0.050f, 0.050f), Mat4.translation(V3(0.91f, 0f, 0f)))
    addMesh(Geo.cylinder(0.030f, 0.040f, 8))
}.build()

private fun cyGaugeMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.10f, 0.010f, 0.006f), Mat4.translation(V3(0.05f, 0f, 0f)))
    addMesh(Geo.cylinder(0.015f, 0.010f, 8).transformed(Mat4.rotateX(90f)))
}.build()

private fun cyIrisMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.11f, 0.022f, 20, 4).transformed(Mat4.rotateZ(90f)))
    for (i in 0 until 6) {
        addMesh(
            Geo.box(0.02f, 0.09f, 0.045f),
            Mat4.rotateX(i * 60f).multiply(Mat4.translation(V3(0f, 0.065f, 0f))),
        )
    }
    addMesh(Geo.box(0.10f, 0.02f, 0.02f), Mat4.translation(V3(0.09f, 0f, 0f)))
}.build()

fun cyclotronSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "cyclotron",
            width = 3.90f,
            depth = 2.60f,
            top = 0.30f,
            system = CY_MAGNET,
            assembly = CY_PLINTH,
            accentMaterial = Materials.emitBlue,
        ),
    )

    parts.add(
        Part(
            id = "cy_yoke", name = "Magnet yoke and poles", system = CY_MAGNET, assembly = CY_YOKE,
            mesh = cyYokeMesh(), material = cyIron,
        ),
    )
    parts.add(
        Part(
            id = "cy_coils", name = "Superconducting coils", system = CY_MAGNET, assembly = CY_YOKE,
            mesh = cyCoilMesh(), material = Materials.copperBright,
        ),
    )
    parts.add(
        Part(
            id = "cy_trim", name = "Trim coil ring", system = CY_MAGNET, assembly = CY_YOKE,
            mesh = cyTrimMesh(), material = cyScreen,
            origin = V3(0f, CY_Y, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 22f) },
        ),
    )

    parts.add(
        Part(
            id = "cy_chamber", name = "Vacuum chamber", system = CY_VACUUM, assembly = CY_CHAMBER,
            mesh = cyChamberMesh(), material = cyGlass,
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "cy_chamber_floor", name = "Chamber base plate", system = CY_VACUUM, assembly = CY_CHAMBER,
            mesh = Geo.cylinder(0.98f, 0.03f, 40).transformed(Mat4.translation(V3(0f, CY_Y - 0.14f, 0f))),
            material = Materials.darkSteel,
        ),
    )

    parts.add(
        Part(
            id = "cy_cavity", name = "RF accelerating cavity", system = CY_RF, assembly = CY_CAVITY,
            mesh = cyCavityMesh(), material = Materials.copperBright,
        ),
    )
    parts.add(
        Part(
            id = "cy_cavity_fins", name = "Cavity tuning fins", system = CY_RF, assembly = CY_CAVITY,
            mesh = cyFinMesh(), material = cyCeramic,
            origin = V3(1.46f, CY_Y, 0f),
            animate = { ctx -> Mat4.translation(V3(0.022f * sin(ctx.machineTime * 2.2f), 0f, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "cy_cavity_piston", name = "Cavity piston", system = CY_RF, assembly = CY_CAVITY,
            mesh = cyPistonMesh(), material = Materials.titanium,
            origin = V3(1.68f, CY_Y, 0f),
            animate = { ctx -> Mat4.translation(V3(0.045f * sin(ctx.machineTime * 2.6f), 0f, 0f)) },
        ),
    )

    parts.add(
        Part(
            id = "cy_extraction", name = "Extraction septum", system = CY_EXTRACT, assembly = CY_LINE,
            mesh = cyExtractionMesh(), material = Materials.titanium,
        ),
    )
    parts.add(
        Part(
            id = "cy_iris", name = "Beam iris", system = CY_EXTRACT, assembly = CY_LINE,
            mesh = cyIrisMesh(), material = Materials.brushedSteel,
            origin = V3(1.50f, CY_Y - 0.20f, 0.74f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 34f) },
        ),
    )
    parts.add(
        Part(
            id = "cy_target", name = "Rotating target wheel", system = CY_EXTRACT, assembly = CY_TARGET,
            mesh = cyTargetMesh(), material = cyTungsten,
            origin = V3(1.60f, CY_Y - 0.26f, 0.74f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 95f) },
        ),
    )
    parts.add(
        Part(
            id = "cy_door", name = "Target shield door", system = CY_EXTRACT, assembly = CY_TARGET,
            mesh = cyDoorMesh(), material = Materials.paintOrange,
            origin = V3(1.86f, CY_Y - 0.05f, 0.74f),
            animate = { ctx -> Mat4.rotateZ(-16f + 20f * sin(ctx.machineTime * 0.42f)) },
        ),
    )

    parts.add(
        Part(
            id = "cy_cryo", name = "Cryogenic plant", system = CY_CRYO, assembly = CY_PLANT,
            mesh = cyCryoMesh(), material = Materials.brushedSteel,
        ),
    )
    for (side in listOf(-1f, 1f)) {
        parts.add(
            Part(
                id = if (side < 0f) "cy_rotor_port" else "cy_rotor_starboard",
                name = if (side < 0f) "Port cryo fan" else "Starboard cryo fan",
                system = CY_CRYO, assembly = CY_PLANT,
                mesh = cyRotorMesh(), material = cyScreen,
                origin = V3(-1.42f, 0.95f, side * 0.78f),
                animate = { ctx -> Mat4.rotateY(ctx.machineTime * 260f) },
            ),
        )
    }

    for (side in listOf(-1f, 1f)) {
        parts.add(
            Part(
                id = if (side < 0f) "cy_valve_port" else "cy_valve_starboard",
                name = if (side < 0f) "Port cryo valve" else "Starboard cryo valve",
                system = CY_CRYO, assembly = CY_PLANT,
                mesh = cyValveMesh(), material = Materials.brass,
                origin = V3(-1.42f, 0.60f, side * 0.99f),
                animate = { ctx -> Mat4.rotateZ(ctx.machineTime * 35f) },
            ),
        )
    }
    parts.add(
        Part(
            id = "cy_sweep", name = "Chamber diagnostic sweep", system = CY_VACUUM, assembly = CY_CHAMBER,
            mesh = cySweepMesh(), material = cyScreen,
            origin = V3(0f, CY_Y, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 40f) },
        ),
    )
    parts.add(
        Part(
            id = "cy_gauge", name = "Beam current gauge", system = CY_CONTROL, assembly = CY_RACK,
            mesh = cyGaugeMesh(), material = Materials.brass,
            origin = V3(0.92f, 0.94f, -0.742f),
            animate = { ctx -> Mat4.rotateZ(120f * sin(ctx.machineTime * 0.8f)) },
        ),
    )

    parts.add(
        Part(
            id = "cy_rack", name = "Beam control rack", system = CY_CONTROL, assembly = CY_RACK,
            mesh = cyRackMesh(), material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "cy_rack_screens", name = "Rack instrument faces", system = CY_CONTROL, assembly = CY_RACK,
            mesh = cyScreenMesh(), material = cyScreen,
        ),
    )
    parts.add(
        Part(
            id = "cy_rack_scan", name = "Rack scan bar", system = CY_CONTROL, assembly = CY_RACK,
            mesh = Geo.box(0.42f, 0.012f, 0.012f).transformed(Mat4.translation(V3(1.02f, 0.60f, -0.742f))),
            material = cyScreen,
            animate = { ctx -> Mat4.translation(V3(0f, 0.26f * sin(ctx.machineTime * 2.2f), 0f)) },
        ),
    )

    parts.add(
        Part(
            id = "cy_rail", name = "Service gantry", system = CY_CONTROL, assembly = CY_RACK,
            mesh = cyRailMesh(), material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "cy_trolley", name = "Gantry hoist trolley", system = CY_CONTROL, assembly = CY_RACK,
            mesh = cyTrolleyMesh(), material = Materials.titanium,
            origin = V3(0.10f, 2.00f, 0f),
            animate = { ctx -> Mat4.translation(V3(0.55f * sin(ctx.machineTime * 0.5f), 0f, 0f)) },
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "chamber", "Acceleration chamber",
            "The glass ring is the vacuum chamber. Protons spiral outward inside it, pushed twice per turn by the radio-frequency cavity until they reach the extraction radius.",
            "cy_chamber", V3(0f, 0f, 1.05f),
            CameraPose(34f, 16f, 1.06f), readoutLabel = "VACUUM CHAMBER",
        ),
        Hotspot(
            "coils", "Superconducting coils",
            "Two coils carry a large current with almost no resistance. Their field bends the beam into a circle, and the field strength sets the energy the beam reaches at each radius.",
            "cy_coils", V3(0f, 0.68f, 1.20f),
            CameraPose(52f, 18f, 1.02f), readoutLabel = "MAGNET COILS",
        ),
        Hotspot(
            "cavity", "RF accelerating cavity",
            "The cavity drives an oscillating voltage across the gap. The piston and tuning fins trim its resonant frequency so each pass adds energy at exactly the right moment.",
            "cy_cavity", V3(0.10f, 0.10f, 0.22f),
            CameraPose(58f, 14f, 1.10f), readoutLabel = "RF DRIVE",
        ),
        Hotspot(
            "target", "Extraction and target",
            "At full radius the beam crosses the septum, leaves the chamber and lands on a rotating target wheel. The wheel spreads the heat so no single spot overheats.",
            "cy_target", V3(0.10f, 0.10f, 0f),
            CameraPose(40f, 14f, 1.10f), readoutLabel = "BEAM TARGET",
        ),
        Hotspot(
            "cryo", "Cryogenic plant",
            "Liquid helium keeps the coils superconducting. The cryo plant and its transfer lines hold the windings near four kelvin so the field stays stable during a run.",
            "cy_cryo", V3(0f, 0.30f, 0.78f),
            CameraPose(24f, 18f, 1.02f), readoutLabel = "CRYO LOOP",
        ),
        Hotspot(
            "rack", "Beam control",
            "The rack tunes the cavity, trims the coils and watches beam current. Interlocks dump the beam into a collimator if vacuum, temperature or field drifts out of range.",
            "cy_rack", V3(0f, 0.20f, 0.23f),
            CameraPose(30f, 14f, 1.04f), readoutLabel = "CONTROL",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Inject",
            "A hydrogen source and an ion gun feed protons into the centre of the chamber, where the magnetic field is weakest and the orbit is smallest.",
            CY_VACUUM, CameraPose(34f, 18f, 1.06f), 0.14f, 0.30f,
        ),
        GuideStep(
            "Bend the beam",
            "The superconducting coils bend the protons into a circle. Stronger field means a tighter orbit, so the beam radius grows as its energy rises.",
            CY_MAGNET, CameraPose(52f, 18f, 1.02f), 0.26f, 0.46f,
        ),
        GuideStep(
            "Kick twice per turn",
            "The RF cavity switches polarity as the beam crosses its gap. Each pass adds a small amount of energy, and the orbit steps outward a little further.",
            CY_RF, CameraPose(58f, 14f, 1.12f), 0.12f, 0.66f,
        ),
        GuideStep(
            "Extract",
            "At the target radius a septum separates the circulating beam from the exit path. The iris and the shield door control when the beam is allowed out.",
            CY_EXTRACT, CameraPose(40f, 14f, 1.08f), 0.10f, 0.78f,
        ),
        GuideStep(
            "Hit the target",
            "The beam lands on the rotating tungsten wheel. Target material is swapped without venting the chamber, and the shield door closes before anyone enters.",
            CY_EXTRACT, CameraPose(38f, 12f, 1.12f), 0.08f, 0.88f,
        ),
        GuideStep(
            "The assembly",
            "Explode the yoke, coils, chamber, cavity, beamline and cryo plant to see how the accelerator is put together.",
            null, CameraPose(32f, 16f, 0.94f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "cyclotron",
        index = 27,
        title = "HELIX C-40",
        designation = "HX-40 / SUPERCONDUCTING CYCLOTRON",
        category = "Physics",
        categoryShort = "PHY",
        tagline = "A spiral staircase for protons.",
        summary = "A sectional cyclotron: laminated iron yoke, two superconducting coils, a glass vacuum chamber, an RF accelerating cavity with a moving piston, an extraction septum, a rotating tungsten target and a cryogenic plant.",
        details = listOf(
            "HELIX C-40 accelerates protons in a spiral. A strong magnetic field keeps the beam turning, while an RF cavity kicks it twice per revolution; with every kick the orbit grows until it reaches the extraction radius.",
            "The machine is mostly support hardware: the iron yoke returns the magnetic flux, the coils make the field, the cryo plant keeps them cold, and the target wheel absorbs the beam once it leaves the chamber.",
        ),
        specs = listOf(
            SpecRow("Beam energy", "40 MeV"),
            SpecRow("Field", "1.9 T"),
            SpecRow("Beam current", "420 µA"),
            SpecRow("Coil temp", "4.2 K"),
            SpecRow("Extraction", "Septum, 2 turns"),
        ),
        tags = listOf("physics", "accelerator", "cyclotron", "proton", "magnet", "cryogenic", "research", "laboratory"),
        accent = 0xFF9FD2FF.toInt(),
        control = ControlSpec(
            label = "BEAM CURRENT",
            lowLabel = "STANDBY",
            highLabel = "FULL",
            defaultValue = 0.55f,
            hint = "RF drive, coil current, cryo load and extraction rate",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(38f, 18f, 1.06f),
        thumbnailPose = CameraPose(39f, 19f, 1.04f),
        thumbnailTime = 2.4f,
        thumbnailControl = 0.70f,
        focusOffset = V3(0f, 0.06f, 0f),
        readouts = { ctx ->
            val energy = 8f + 34f * ctx.control
            val field = 0.9f + 1.1f * ctx.control
            val current = 40f + 400f * ctx.control
            listOf(
                Readout("ENERGY", String.format("%.1f", energy), "MeV", accent = true),
                Readout("FIELD", String.format("%.2f", field), "T"),
                Readout("BEAM", String.format("%.0f", current), "µA"),
            )
        },
        effects = { ctx ->
            val activity = (0.20f + 0.80f * ctx.control).coerceIn(0f, 1f)
            val spiral = (0..72).map { index ->
                val t = index / 72f
                val angle = Math.toRadians((18f + 700f * t).toDouble())
                val radius = 0.16f + 0.76f * t
                V3(
                    (radius * cos(angle)).toFloat(),
                    CY_Y + 0.02f * sin(t * 3.1f + ctx.machineTime * 0.6f),
                    (radius * sin(angle)).toFloat(),
                )
            }
            val spiralOuter = (0..48).map { index ->
                val t = index / 48f
                val angle = Math.toRadians((40f + 520f * t).toDouble())
                val radius = 0.30f + 0.70f * t
                V3(
                    (radius * cos(angle)).toFloat(),
                    CY_Y + 0.09f,
                    (radius * sin(angle)).toFloat(),
                )
            }
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.90f, 0xFF9FD2FF.toInt(), 0f, 0.12f),
                SceneEffect.Glow(V3(0f, CY_Y, 0f), 0.90f, 0xFFB9DCFF.toInt(), 0.10f + 0.14f * activity),
                SceneEffect.Flow(
                    spiral, 0xFFA8D4FF.toInt(), count = 26, speed = 0.55f * activity + 0.10f,
                    phase = 0.06f, dotRadius = 0.016f,
                ),
                SceneEffect.Flow(
                    spiralOuter, 0xFF8FB8FF.toInt(), count = 18, speed = 0.42f * activity + 0.08f,
                    phase = 0.34f, dotRadius = 0.012f,
                ),
                SceneEffect.Beam(
                    V3(0.55f, CY_Y, 0.62f), V3(1.62f, CY_Y - 0.26f, 0.74f),
                    0xFFA8D4FF.toInt(), 0.10f + 0.18f * activity,
                ),
                SceneEffect.Glow(V3(1.60f, CY_Y - 0.26f, 0.74f), 0.26f, 0xFFD6E8FF.toInt(), 0.16f + 0.22f * activity),
                SceneEffect.Spark(V3(1.50f, CY_Y + 0.18f, 0f), 0xFF9FD2FF.toInt(), 0.020f, 0.16f + 0.22f * activity),
                SceneEffect.Flow(
                    listOf(V3(-1.42f, 0.95f, 0.78f), V3(-1.26f, 1.24f, 0.70f), V3(-0.90f, 1.32f, 0.50f)),
                    0xFF8CF6E8.toInt(), count = 8, speed = 0.20f, phase = 0.22f, dotRadius = 0.014f,
                ),
            )
        },
        descriptionTitle = "The spiral accelerator",
    )
}
