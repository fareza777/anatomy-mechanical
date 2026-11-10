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

private const val HL_FRAME = "FRAME"
private const val HL_HABITAT = "HABITAT"
private const val HL_POWER = "POWER"
private const val HL_THERMAL = "THERMAL"
private const val HL_DOCKING = "DOCKING"
private const val HL_COMMAND = "COMMAND"

private const val HL_PLINTH = "PLINTH"
private const val HL_SPINE = "SPINE"
private const val HL_RING_A = "RING A"
private const val HL_RING_B = "RING B"
private const val HL_ARRAY = "ARRAY"
private const val HL_RADIATOR = "RADIATOR"
private const val HL_PORT = "PORT"
private const val HL_MAST = "MAST"
private const val HL_CARGO = "CARGO"

private const val HL_SPINE_Y = 1.44f
private const val HL_RADIUS_A = 1.02f
private const val HL_RADIUS_B = 0.94f

private val hlShell = Material(
    "haloShell", "Habitat shell", 0xFFC8D6D1.toInt(), specular = 0.34f, shininess = 22f,
    fillLight = 0.09f, rimLight = 0.15f, patina = 0.022f, edgeWear = 0.04f,
    pattern = SurfacePattern.PANEL, patternStrength = 0.38f,
)
private val hlTrim = Material(
    "haloTrim", "Gold thermal trim", 0xFFC7A360.toInt(), specular = 0.66f, shininess = 38f,
    fillLight = 0.10f, rimLight = 0.18f, patina = 0.060f, edgeWear = 0.05f,
    pattern = SurfacePattern.PATINA, patternStrength = 0.24f,
)
private val hlWindow = Material(
    "haloWindow", "Viewport light", 0xFF9DF6E7.toInt(), emissive = true, specular = 0f,
)
private val hlRadiator = Material(
    "haloRadiator", "Radiator panel", 0xFFDEE6DF.toInt(), specular = 0.22f, shininess = 16f,
    fillLight = 0.08f, rimLight = 0.14f, patina = 0.020f,
    pattern = SurfacePattern.GRID, patternStrength = 0.35f,
)

private fun hlRingMesh(radius: Float, width: Float, spokes: Int): Mesh = MeshBuilder().apply {
    addMesh(machineRingX(0f, radius, radius - 0.11f, width, 40))
    addMesh(machineRingX(0f, radius - 0.19f, radius - 0.24f, width * 0.55f, 36))
    for (i in 0 until spokes) {
        val angle = i * 360f / spokes
        val inner = 0.20f
        val outer = radius - 0.08f
        val length = outer - inner
        addMesh(
            Geo.box(width * 0.50f, length, 0.075f),
            Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, inner + length * 0.5f, 0f))),
        )
    }
    for (i in 0 until spokes) {
        val angle = i * 360f / spokes + 180f / spokes
        addMesh(
            Geo.box(width * 0.30f, 0.16f, 0.05f),
            Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, radius - 0.12f, 0f))),
        )
    }
}.build()

private fun hlModuleMesh(radius: Float, width: Float, count: Int): Mesh = MeshBuilder().apply {
    for (i in 0 until count) {
        val angle = i * 360f / count
        val place = Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, radius, 0f)))
        addMesh(
            Geo.cylinder(0.150f, width * 1.30f, 12, bevel = 0.030f).transformed(Mat4.rotateZ(90f)),
            place,
        )
        addMesh(
            Geo.box(width * 0.95f, 0.20f, 0.05f),
            place.multiply(Mat4.translation(V3(0f, 0.21f, 0f))),
        )
        addMesh(
            Geo.torus(0.152f, 0.012f, 14, 4).transformed(Mat4.rotateZ(90f)),
            place.multiply(Mat4.translation(V3(width * 0.55f, 0f, 0f))),
        )
    }
}.build()

private fun hlWindowMesh(radius: Float, width: Float, count: Int): Mesh = MeshBuilder().apply {
    for (i in 0 until count) {
        val angle = i * 360f / count
        addMesh(
            Geo.hollowCylinder(0.154f, 0.138f, width * 0.30f, 12).transformed(Mat4.rotateZ(90f)),
            Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, radius, 0f))),
        )
        addMesh(
            Geo.box(width * 0.60f, 0.022f, 0.022f),
            Mat4.rotateX(angle + 5f).multiply(Mat4.translation(V3(0f, radius + 0.10f, 0f))),
        )
    }
}.build()

private fun hlCarMesh(radius: Float): Mesh = MeshBuilder().apply {
    for (angle in listOf(0f, 180f)) {
        val place = Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, radius, 0f)))
        addMesh(Geo.slab(0.18f, 0.24f, 0.14f, cornerRadius = 0.035f, bevel = 0.015f), place)
        addMesh(
            Geo.box(0.12f, 0.09f, 0.02f),
            place.multiply(Mat4.translation(V3(0f, 0f, 0.13f))),
        )
        addMesh(
            Geo.box(0.10f, 0.030f, 0.10f),
            place.multiply(Mat4.translation(V3(0f, 0.10f, 0f))),
        )
    }
}.build()

private fun hlPylonMesh(): Mesh = MeshBuilder().apply {
    for (x in listOf(-1.32f, 1.32f)) {
        addMesh(Geo.cylinder(0.14f, 1.14f, 16, bevel = 0.02f), Mat4.translation(V3(x, 0.87f, 0f)))
        addMesh(
            Geo.cylinder(0.26f, 0.10f, 20, bevel = 0.02f).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(x, HL_SPINE_Y, 0f)),
        )
        addMesh(
            Geo.slab(0.38f, 0.38f, 0.08f, cornerRadius = 0.05f, bevel = 0.02f),
            Mat4.translation(V3(x, 0.34f, 0f)),
        )
        for (z in listOf(-0.11f, 0.11f)) {
            addMesh(Geo.box(0.05f, 0.30f, 0.045f), Mat4.translation(V3(x, 0.52f, z)))
        }
    }
}.build()

private fun hlSpineMesh(): Mesh = MeshBuilder().apply {
    addMesh(machineCylinderX(0f, 0.135f, 3.10f, 20, bevel = 0.02f))
    for (x in listOf(-0.98f, -0.22f, 0.52f, 1.12f)) {
        addMesh(machineRingX(x, 0.175f, 0.135f, 0.07f, 20))
    }
    for (x in listOf(-1.18f, 1.18f)) {
        addMesh(machineRingX(x, 0.20f, 0.14f, 0.10f, 20))
    }
    addMesh(
        Geo.tube(
            listOf(
                V3(-1.20f, 0.14f, 0.10f),
                V3(-0.40f, 0.22f, 0.16f),
                V3(0.45f, 0.22f, 0.16f),
                V3(1.20f, 0.14f, 0.10f),
            ),
            0.022f, 6, subdivide = 4,
        ),
    )
}.build()

private fun hlCoreMesh(): Mesh = MeshBuilder().apply {
    addMesh(machineCylinderX(0f, 0.24f, 0.70f, 24, bevel = 0.03f))
    addMesh(machineRingX(-0.22f, 0.27f, 0.235f, 0.06f, 24))
    addMesh(machineRingX(0.22f, 0.27f, 0.235f, 0.06f, 24))
    for (i in 0 until 6) {
        val angle = i * 60f
        addMesh(
            Geo.box(0.44f, 0.05f, 0.03f),
            Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, 0.255f, 0f))),
        )
    }
}.build()

private fun hlArrayMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.06f, 0.30f, 12).transformed(Mat4.rotateX(90f)))
    addMesh(
        Geo.box(0.16f, 0.10f, 0.16f),
        Mat4.translation(V3(0f, 0f, 0f)),
    )
    for (side in listOf(-1f, 1f)) {
        val z = side * 1.02f
        addMesh(Geo.box(0.98f, 0.030f, 0.64f), Mat4.translation(V3(0f, 0f, z)))
        addMesh(Geo.box(1.02f, 0.045f, 0.035f), Mat4.translation(V3(0f, 0f, side * 0.70f)))
        for (x in listOf(-0.49f, 0.49f)) {
            addMesh(Geo.box(0.035f, 0.040f, 0.64f), Mat4.translation(V3(x, 0f, z)))
        }
        addMesh(Geo.box(0.55f, 0.035f, 0.035f), Mat4.translation(V3(0f, 0f, side * 0.52f)))
    }
}.build()

private fun hlRadiatorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(1.45f, 0.60f, 0.028f), Mat4.translation(V3(0f, 0.52f, 0f)))
    addMesh(Geo.box(0.06f, 0.26f, 0.06f), Mat4.translation(V3(0f, 0.18f, 0f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(1.45f, 0.006f, 0.040f),
            Mat4.translation(V3(0f, 0.30f + i * 0.12f, 0.022f)),
        )
    }
    addMesh(Geo.box(0.12f, 0.06f, 0.06f), Mat4.translation(V3(0f, 0.05f, 0f)))
}.build()

private fun hlDockMesh(): Mesh = MeshBuilder().apply {
    addMesh(machineCylinderX(0f, 0.40f, 0.16f, 28, bevel = 0.03f))
    addMesh(machineCylinderX(0.14f, 0.30f, 0.26f, 24))
    addMesh(machineTorusX(0.20f, 0.36f, 0.035f))
    addMesh(machineRingX(0.21f, 0.44f, 0.32f, 0.08f, 28))
    for (i in 0 until 6) {
        val angle = Math.toRadians((i * 60.0))
        addMesh(
            Geo.hexBolt(0.035f, 0.03f).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(
                V3(0.21f, (0.37 * cos(angle)).toFloat(), (0.37 * sin(angle)).toFloat()),
            ),
        )
    }
}.build()

private fun hlPetalMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 4) {
        val angle = i * 90f + 45f
        val place = Mat4.rotateX(angle).multiply(Mat4.translation(V3(0.26f, 0.34f, 0f)))
        addMesh(Geo.box(0.18f, 0.22f, 0.07f), place)
        addMesh(
            Geo.box(0.05f, 0.05f, 0.05f),
            place.multiply(Mat4.translation(V3(0f, 0.13f, 0f))),
        )
    }
}.build()

private fun hlMastMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.095f, 0.10f, 14), Mat4.translation(V3(0f, 0.05f, 0f)))
    addMesh(Geo.cylinder(0.048f, 0.92f, 12, bevel = 0.01f), Mat4.translation(V3(0f, 0.50f, 0f)))
    for (y in listOf(0.28f, 0.62f, 0.92f)) {
        addMesh(Geo.torus(0.075f, 0.014f, 16, 4), Mat4.translation(V3(0f, y, 0f)))
    }
    addMesh(Geo.cylinder(0.11f, 0.06f, 14), Mat4.translation(V3(0f, 0.97f, 0f)))
    addMesh(
        Geo.tube(
            listOf(V3(0f, 0.90f, 0f), V3(0.10f, 0.70f, 0.06f), V3(0.06f, 0.20f, 0.10f)),
            0.012f, 5, subdivide = 3,
        ),
    )
}.build()

private fun hlDishMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.050f, 0.10f, 12), Mat4.translation(V3(0f, 0.05f, 0f)))
    addMesh(
        Geo.lathe(
            listOf(0.02f to 0.02f, 0.16f to 0.04f, 0.29f to 0.10f, 0.33f to 0.16f),
            28, capBottom = true,
        ).transformed(Mat4.rotateX(180f)),
        Mat4.translation(V3(0f, 0.10f, 0f)),
    )
    addMesh(Geo.cylinder(0.018f, 0.24f, 8), Mat4.translation(V3(0f, 0.30f, 0f)))
    addMesh(Geo.cylinder(0.040f, 0.055f, 10, bevel = 0.01f), Mat4.translation(V3(0f, 0.43f, 0f)))
    addMesh(Geo.torus(0.33f, 0.012f, 28, 4), Mat4.translation(V3(0f, 0.11f, 0f)))
}.build()

private fun hlCargoMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.46f, 0.32f, 0.10f, cornerRadius = 0.03f, bevel = 0.015f))
    addMesh(Geo.box(0.15f, 0.17f, 0.15f), Mat4.translation(V3(-0.11f, 0.14f, 0f)))
    addMesh(Geo.box(0.13f, 0.12f, 0.11f), Mat4.translation(V3(0.11f, 0.12f, 0.02f)))
    addMesh(Geo.box(0.42f, 0.02f, 0.28f), Mat4.translation(V3(0f, 0.05f, 0f)))
    addMesh(Geo.torus(0.05f, 0.012f, 12, 4), Mat4.rotateX(90f).multiply(Mat4.translation(V3(-0.20f, 0.10f, 0f))))
}.build()

private fun hlFlywheelMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.150f, 0.055f, 20, bevel = 0.008f))
    for (i in 0 until 6) {
        addMesh(
            Geo.box(0.022f, 0.036f, 0.115f),
            Mat4.rotateY(i * 60f).multiply(Mat4.translation(V3(0f, 0.006f, 0.072f))),
        )
    }
    addMesh(Geo.cylinder(0.032f, 0.10f, 10))
}.build()

private fun hlBeaconMesh(): Mesh = MeshBuilder().apply {
    addMesh(machineTorusX(0f, 0.235f, 0.020f))
    for (i in 0 until 8) {
        addMesh(
            Geo.box(0.030f, 0.050f, 0.030f),
            Mat4.rotateX(i * 45f).multiply(Mat4.translation(V3(0f, 0.235f, 0f))),
        )
    }
}.build()

fun haloHabitat(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "halo",
            width = 4.40f,
            depth = 2.30f,
            top = 0.30f,
            system = HL_FRAME,
            assembly = HL_PLINTH,
            accentMaterial = Materials.emitCyan,
        ),
    )

    parts.add(
        Part(
            id = "hl_pylons", name = "Bearing pylons", system = HL_FRAME, assembly = HL_SPINE,
            mesh = hlPylonMesh(), material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "hl_spine", name = "Spin axis and utility spine", system = HL_FRAME, assembly = HL_SPINE,
            mesh = hlSpineMesh(), material = Materials.titanium,
            origin = V3(0f, HL_SPINE_Y, 0f),
        ),
    )
    parts.add(
        Part(
            id = "hl_core", name = "Command core", system = HL_COMMAND, assembly = HL_RING_A,
            mesh = hlCoreMesh(), material = hlTrim,
            origin = V3(0f, HL_SPINE_Y, 0f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 9f) },
        ),
    )

    val ringA = V3(-0.80f, HL_SPINE_Y, 0f)
    parts.add(
        Part(
            id = "hl_ring_a", name = "Habitat ring A structure", system = HL_HABITAT, assembly = HL_RING_A,
            mesh = hlRingMesh(HL_RADIUS_A, 0.20f, 8), material = hlShell,
            origin = ringA,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 6.5f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_modules_a", name = "Habitat modules A", system = HL_HABITAT, assembly = HL_RING_A,
            mesh = hlModuleMesh(HL_RADIUS_A, 0.20f, 8), material = hlShell,
            origin = ringA,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 6.5f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_windows_a", name = "Viewport strips A", system = HL_HABITAT, assembly = HL_RING_A,
            mesh = hlWindowMesh(HL_RADIUS_A, 0.20f, 8), material = hlWindow,
            origin = ringA,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 6.5f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_cars_a", name = "Ring A elevator cars", system = HL_HABITAT, assembly = HL_RING_A,
            mesh = hlCarMesh(HL_RADIUS_A + 0.19f), material = Materials.titanium,
            origin = ringA,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 17f) },
        ),
    )

    val ringB = V3(0.74f, HL_SPINE_Y, 0f)
    parts.add(
        Part(
            id = "hl_ring_b", name = "Habitat ring B structure", system = HL_HABITAT, assembly = HL_RING_B,
            mesh = hlRingMesh(HL_RADIUS_B, 0.17f, 6), material = Materials.brushedSteel,
            origin = ringB,
            animate = { ctx -> Mat4.rotateX(-ctx.machineTime * 4.2f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_modules_b", name = "Habitat modules B", system = HL_HABITAT, assembly = HL_RING_B,
            mesh = hlModuleMesh(HL_RADIUS_B, 0.17f, 6), material = Materials.paintCream,
            origin = ringB,
            animate = { ctx -> Mat4.rotateX(-ctx.machineTime * 4.2f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_windows_b", name = "Viewport strips B", system = HL_HABITAT, assembly = HL_RING_B,
            mesh = hlWindowMesh(HL_RADIUS_B, 0.17f, 6), material = Materials.emitAmber,
            origin = ringB,
            animate = { ctx -> Mat4.rotateX(-ctx.machineTime * 4.2f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_cars_b", name = "Ring B service car", system = HL_HABITAT, assembly = HL_RING_B,
            mesh = hlCarMesh(HL_RADIUS_B + 0.18f), material = Materials.graphite,
            origin = ringB,
            animate = { ctx -> Mat4.rotateX(-ctx.machineTime * 22f) },
        ),
    )

    parts.add(
        Part(
            id = "hl_array_port", name = "Port solar wing", system = HL_POWER, assembly = HL_ARRAY,
            mesh = hlArrayMesh(), material = Materials.solarCell,
            origin = V3(-1.56f, HL_SPINE_Y, 0f),
            animate = { ctx -> Mat4.rotateX(14f * sin(ctx.machineTime * 0.35f)) },
        ),
    )
    parts.add(
        Part(
            id = "hl_array_starboard", name = "Starboard solar wing", system = HL_POWER, assembly = HL_ARRAY,
            mesh = hlArrayMesh(), material = Materials.solarCell,
            origin = V3(1.56f, HL_SPINE_Y, 0f),
            animate = { ctx -> Mat4.rotateX(-14f * sin(ctx.machineTime * 0.35f + 0.6f)) },
        ),
    )

    for (side in listOf(-1f, 1f)) {
        parts.add(
            Part(
                id = if (side < 0f) "hl_radiator_port" else "hl_radiator_starboard",
                name = if (side < 0f) "Port radiator" else "Starboard radiator",
                system = HL_THERMAL, assembly = HL_RADIATOR,
                mesh = hlRadiatorMesh(), material = hlRadiator,
                origin = V3(0f, HL_SPINE_Y, side * 0.54f),
                animate = { ctx -> Mat4.rotateX(side * 5f * sin(ctx.machineTime * 0.5f + 0.4f)) },
            ),
        )
    }

    parts.add(
        Part(
            id = "hl_dock", name = "Axial docking tunnel", system = HL_DOCKING, assembly = HL_PORT,
            mesh = hlDockMesh(), material = Materials.titanium,
            origin = V3(-1.74f, HL_SPINE_Y, 0f),
        ),
    )
    parts.add(
        Part(
            id = "hl_dock_petals", name = "Docking petals", system = HL_DOCKING, assembly = HL_PORT,
            mesh = hlPetalMesh(), material = hlTrim,
            origin = V3(-1.94f, HL_SPINE_Y, 0f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 12f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_dock_beacon", name = "Docking beacon", system = HL_DOCKING, assembly = HL_PORT,
            mesh = hlBeaconMesh(), material = hlWindow,
            origin = V3(-1.98f, HL_SPINE_Y, 0f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 48f) },
        ),
    )

    parts.add(
        Part(
            id = "hl_mast", name = "Comms mast", system = HL_COMMAND, assembly = HL_MAST,
            mesh = hlMastMesh(), material = Materials.titanium,
            origin = V3(1.24f, HL_SPINE_Y, 0f),
        ),
    )
    parts.add(
        Part(
            id = "hl_dish", name = "High gain antenna", system = HL_COMMAND, assembly = HL_MAST,
            mesh = hlDishMesh(), material = hlShell,
            origin = V3(1.24f, HL_SPINE_Y + 1.02f, 0f),
            animate = { ctx ->
                Mat4.rotateY(ctx.machineTime * 8f)
                    .multiply(Mat4.rotateZ(10f * sin(ctx.machineTime * 0.6f)))
            },
        ),
    )

    parts.add(
        Part(
            id = "hl_cargo", name = "Spine cargo carrier", system = HL_FRAME, assembly = HL_CARGO,
            mesh = hlCargoMesh(), material = Materials.paintOrange,
            origin = V3(0f, HL_SPINE_Y - 0.42f, 0f),
            animate = { ctx -> Mat4.translation(V3(1.00f * sin(ctx.machineTime * 0.55f), 0f, 0f)) },
        ),
    )
    parts.add(
        Part(
            id = "hl_flywheel", name = "Control moment gyro", system = HL_COMMAND, assembly = HL_MAST,
            mesh = hlFlywheelMesh(), material = hlTrim,
            origin = V3(0.32f, HL_SPINE_Y + 0.40f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 320f) },
        ),
    )
    parts.add(
        Part(
            id = "hl_status_ring", name = "Habitat status ring", system = HL_FRAME, assembly = HL_SPINE,
            mesh = hlBeaconMesh(), material = hlWindow,
            origin = V3(0.92f, HL_SPINE_Y, 0f),
            animate = { ctx -> Mat4.rotateX(-ctx.machineTime * 55f) },
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "ring_a", "Counter rotating habitat ring",
            "Two rings spin in opposite directions so the station can turn without pushing its whole mass. Each rim carries pressurised modules, windows and an elevator car that runs on the guide rail.",
            "hl_modules_a", V3(0f, HL_RADIUS_A, 0f),
            CameraPose(26f, 12f, 1.04f), readoutLabel = "CREW QUARTERS",
        ),
        Hotspot(
            "arrays", "Solar wings",
            "The wings rotate about the spin axis to follow the sun. Each panel is a lattice of cells on a thin composite backing, and the gold bus bars carry power back to the spine.",
            "hl_array_port", V3(0f, 0f, -0.9f),
            CameraPose(42f, 22f, 1.02f), readoutLabel = "PRIMARY POWER",
        ),
        Hotspot(
            "core", "Command core",
            "The core sits on the non-rotating spine, where microgravity instruments and the flight computers live. It stays still while the habitat rings turn around it.",
            "hl_core", V3(0f, 0.25f, 0f),
            CameraPose(34f, 14f, 1.10f), readoutLabel = "FLIGHT CONTROL",
        ),
        Hotspot(
            "dock", "Axial docking port",
            "Crew and cargo arrive at the axial port. The petals rotate clear of the approach corridor and the tunnel equalises pressure before the inner hatch opens.",
            "hl_dock", V3(0f, 0f, 0f),
            CameraPose(52f, 10f, 1.06f), readoutLabel = "DOCKING",
        ),
        Hotspot(
            "radiators", "Thermal radiators",
            "Every kilowatt the station uses has to leave as heat. The radiator wings rock on their hinges to keep the panels edge-on to the sun while still radiating into deep space.",
            "hl_radiator_port", V3(0f, 0.65f, 0f),
            CameraPose(20f, 20f, 1.02f), readoutLabel = "HEAT REJECTION",
        ),
        Hotspot(
            "dish", "Deep space antenna",
            "The dish tracks Earth while the station turns. A gimbal under the bowl keeps the link pointed even as the rings and the solar wings move around it.",
            "hl_dish", V3(0f, 0.20f, 0f),
            CameraPose(40f, 18f, 1.06f), readoutLabel = "COMMS LINK",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Spin up",
            "The habitat rings are spun to one third of a gravity at the rim. Because they turn in opposite directions, the station keeps its attitude while both rings carry the same load.",
            HL_HABITAT, CameraPose(30f, 14f, 1.04f), 0.12f, 0.55f,
        ),
        GuideStep(
            "Make power",
            "Solar wings track the sun and feed the bus bars along the spine. Batteries in the truss carry the station through the night side of every orbit.",
            HL_POWER, CameraPose(44f, 22f, 1.02f), 0.30f, 0.62f,
        ),
        GuideStep(
            "Reject heat",
            "Radiators rock on their hinges to balance sun exposure against heat rejection. Coolant loops run from the core out to every module.",
            HL_THERMAL, CameraPose(22f, 20f, 1.02f), 0.22f, 0.48f,
        ),
        GuideStep(
            "Dock",
            "A visiting craft lines up with the axial port. The petals rotate clear, the tunnel seals and pressure equalises before the hatch opens.",
            HL_DOCKING, CameraPose(52f, 10f, 1.08f), 0.16f, 0.40f,
        ),
        GuideStep(
            "Live and work",
            "Elevator cars run around the rim between the modules. Inside, the floors curve so that spin gravity points down through the deck.",
            HL_HABITAT, CameraPose(28f, 8f, 1.10f), 0.08f, 0.72f,
        ),
        GuideStep(
            "The assembly",
            "Explode the rings, solar wings, radiators, docking port and mast to see how the station is put together.",
            null, CameraPose(32f, 16f, 0.94f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "halo_habitat",
        index = 26,
        title = "HALO H-9",
        designation = "HL-9 / ORBITAL HABITAT",
        category = "Space",
        categoryShort = "ORB",
        tagline = "A small city that turns to make its own gravity.",
        summary = "A sectional orbital habitat: two counter-rotating rings of pressurised modules, a non-rotating command spine, tracking solar wings, rocking radiators, an axial docking port, a deep space antenna and rim elevator cars.",
        details = listOf(
            "HALO H-9 is a station built around a single idea: spin the crew quarters and keep everything else still. The two habitat rings turn in opposite directions, so the structure can carry angular momentum without spinning the docking port and the antennas with it.",
            "The rings hold modules, windows and elevator cars. Outside them, the solar wings and radiator panels track and rock independently, while the core keeps the flight computers, life support and the docking tunnel in microgravity.",
        ),
        specs = listOf(
            SpecRow("Crew", "312 long duration"),
            SpecRow("Ring gravity", "0.34 g"),
            SpecRow("Rotation", "1.8 rpm"),
            SpecRow("Power", "1.4 MW solar"),
            SpecRow("Docking", "Axial, 2 ports"),
        ),
        tags = listOf("space", "station", "habitat", "orbit", "solar", "docking", "centrifuge", "future"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "ROTATION",
            lowLabel = "DOCKED",
            highLabel = "SPIN-UP",
            defaultValue = 0.68f,
            hint = "Ring spin, solar tracking, radiator deployment and docking traffic",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(26f, 18f, 1.14f),
        thumbnailPose = CameraPose(28f, 19f, 1.10f),
        thumbnailTime = 2.5f,
        thumbnailControl = 0.72f,
        focusOffset = V3(0f, 0.05f, 0f),
        readouts = { ctx ->
            val rpm = 0.6f + 1.6f * ctx.control
            val power = 780f + 620f * ctx.control
            val crew = 180f + 150f * ctx.control
            listOf(
                Readout("SPIN", String.format("%.1f", rpm), "rpm", accent = true),
                Readout("CREW", String.format("%.0f", crew), ""),
                Readout("POWER", String.format("%.0f", power), "kW"),
            )
        },
        effects = { ctx ->
            val activity = (0.25f + 0.75f * ctx.control).coerceIn(0f, 1f)
            val orbitA = (0..48).map { index ->
                val angle = Math.toRadians(index * 7.5)
                V3(
                    -0.80f,
                    HL_SPINE_Y + (HL_RADIUS_A * cos(angle)).toFloat(),
                    (HL_RADIUS_A * sin(angle)).toFloat(),
                )
            }
            val orbitB = (0..44).map { index ->
                val angle = Math.toRadians(index * (360.0 / 44.0))
                V3(
                    0.74f,
                    HL_SPINE_Y + (HL_RADIUS_B * cos(angle)).toFloat(),
                    (HL_RADIUS_B * sin(angle)).toFloat(),
                )
            }
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 2.05f, 0xFF6EF2DF.toInt(), 0f, 0.12f),
                SceneEffect.Glow(V3(0f, HL_SPINE_Y, 0f), 0.52f, 0xFF8CF7E6.toInt(), 0.14f + 0.16f * activity),
                SceneEffect.Flow(
                    orbitA, 0xFF7FF2E2.toInt(), count = 22, speed = 0.30f * activity + 0.06f,
                    phase = 0.10f, dotRadius = 0.015f,
                ),
                SceneEffect.Flow(
                    orbitB, 0xFFFFC884.toInt(), count = 16, speed = 0.24f * activity + 0.05f,
                    phase = 0.52f, dotRadius = 0.013f,
                ),
                SceneEffect.Flow(
                    listOf(V3(-1.30f, HL_SPINE_Y + 0.18f, 0f), V3(0f, HL_SPINE_Y + 0.18f, 0f), V3(1.20f, HL_SPINE_Y + 0.18f, 0f)),
                    0xFF6EF2DF.toInt(), count = 12, speed = 0.26f, phase = 0.30f, dotRadius = 0.016f,
                ),
                SceneEffect.Beam(V3(-1.74f, HL_SPINE_Y, 0f), V3(-2.42f, HL_SPINE_Y, 0f), 0xFF7FF2E2.toInt(), 0.10f + 0.14f * activity),
                SceneEffect.Glow(V3(-1.98f, HL_SPINE_Y, 0f), 0.24f, 0xFFB9FFF3.toInt(), 0.20f + 0.22f * activity),
                SceneEffect.Spark(V3(1.24f, HL_SPINE_Y + 1.44f, 0f), 0xFFB9FFF3.toInt(), 0.020f, 0.18f + 0.18f * activity),
            )
        },
        descriptionTitle = "The spinning habitat",
    )
}
