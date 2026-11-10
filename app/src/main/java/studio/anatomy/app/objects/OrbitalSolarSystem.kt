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

private const val OS_FRAME = "ORBIT FRAME"
private const val OS_ARRAY = "SOLAR ARRAY"
private const val OS_POWER = "POWER TRAIN"
private const val OS_LINK = "MICROWAVE LINK"
private const val OS_GROUND = "GROUND SEGMENT"
private const val OS_CTRL = "ATTITUDE CONTROL"

private const val OS_PLINTH = "PLINTH"
private const val OS_SATELLITE = "SATELLITE"
private const val OS_WINGS = "SOLAR WINGS"
private const val OS_TRANSMITTER = "TRANSMITTER"
private const val OS_RECEIVER = "RECTENNA"
private const val OS_CITY = "RECEIVER CITY"

private val solarBlanket = Material(
    "orbitalSolar", "Orbital solar blanket", 0xFF2A5B86.toInt(), specular = 0.78f, shininess = 58f,
    fillLight = 0.10f, rimLight = 0.16f, patina = 0.025f,
    pattern = SurfacePattern.GRID, patternStrength = 0.58f,
)
private val solarBack = Material(
    "orbitalSolarBack", "Solar blanket back", 0xFF637178.toInt(), specular = 0.34f, shininess = 20f,
)
private val orbitalGold = Material(
    "orbitalGold", "Gold interconnect", 0xFFD3A85B.toInt(), specular = 0.72f, shininess = 44f,
)

private val satellite = V3(0f, 2.58f, 0f)
private val beamStart = V3(0f, 1.98f, 0.02f)
private val beamEnd = V3(0.82f, 0.42f, 0.15f)

private fun wingTracking(ctx: AnimContext): Mat4 =
    Mat4.rotateY(sin(ctx.machineTime * 0.24f + 0.5f) * 8.5f)

private fun wingBase(side: Float): Mesh = MeshBuilder().apply {
    val z = 0.018f
    for (row in 0..1) {
        for (col in 0..2) {
            val x = side * (0.40f + col * 0.385f)
            val y = (row - 0.5f) * 0.36f
            addMesh(
                Geo.slab(0.350f, 0.330f, 0.020f, cornerRadius = 0.014f, bevel = 0.005f),
                Mat4.translation(V3(x, y, z)),
            )
            // The back of each blanket gets a visible rib so the array has depth.
            addMesh(
                Geo.box(0.300f, 0.012f, 0.014f),
                Mat4.translation(V3(x, y - 0.12f, z - 0.020f)),
            )
        }
    }
}.build()

private fun wingGrid(side: Float): Mesh = MeshBuilder().apply {
    for (row in 0..1) {
        for (col in 0..2) {
            val x = side * (0.40f + col * 0.385f)
            val y = (row - 0.5f) * 0.36f
            addMesh(Geo.box(0.012f, 0.300f, 0.022f), Mat4.translation(V3(x - 0.160f, y, 0.032f)))
            addMesh(Geo.box(0.012f, 0.300f, 0.022f), Mat4.translation(V3(x + 0.160f, y, 0.032f)))
            addMesh(Geo.box(0.330f, 0.012f, 0.022f), Mat4.translation(V3(x, y - 0.150f, 0.032f)))
            addMesh(Geo.box(0.330f, 0.012f, 0.022f), Mat4.translation(V3(x, y + 0.150f, 0.032f)))
        }
    }
    for (col in 0..3) {
        val x = side * (0.21f + col * 0.385f)
        addMesh(Geo.box(0.010f, 0.720f, 0.026f), Mat4.translation(V3(x, 0f, 0.038f)))
    }
}.build()

private fun wingSpar(side: Float): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.cylinderBetween(
            V3(side * 0.22f, 0f, -0.010f),
            V3(side * 1.57f, 0f, -0.010f),
            0.032f, 8,
        ),
    )
    for (col in 0..3) {
        val x = side * (0.24f + col * 0.36f)
        addMesh(Geo.cylinderBetween(V3(x, -0.40f, -0.010f), V3(x, 0.40f, -0.010f), 0.012f, 6))
    }
    addMesh(Geo.cylinderBetween(V3(side * 0.19f, 0f, -0.05f), V3(side * 0.19f, 0f, 0.10f), 0.055f, 12))
    addMesh(Geo.torus(0.066f, 0.010f, 16, 4).transformed(Mat4.rotateY(90f)), Mat4.translation(V3(side * 0.19f, 0f, 0.04f)))
}.build()

private fun dishMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.lathe(listOf(0.44f to -0.10f, 0.35f to -0.02f, 0.20f to 0.05f, 0.065f to 0.10f), 34, capTop = true))
    addMesh(Geo.torus(0.44f, 0.012f, 34, 4), Mat4.translation(V3(0f, -0.10f, 0f)))
    addMesh(Geo.cylinderBetween(V3(0f, 0.05f, 0f), V3(0f, 0.32f, 0f), 0.030f, 10))
    addMesh(Geo.torus(0.095f, 0.012f, 18, 4), Mat4.translation(V3(0f, 0.16f, 0f)))
}.build()

fun orbitalSolarSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "orbit",
            width = 3.46f,
            depth = 2.28f,
            top = 0.30f,
            system = OS_FRAME,
            assembly = OS_PLINTH,
            accentMaterial = Materials.emitAmber,
        ),
    )

    // Satellite structure.
    parts.add(
        Part(
            id = "os_boom", name = "Wing spar and truss", system = OS_FRAME, assembly = OS_WINGS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinderBetween(V3(-1.65f, 0f, -0.03f), V3(1.65f, 0f, -0.03f), 0.030f, 10))
                for (i in 0..10) {
                    val x = -1.54f + i * 0.28f
                    addMesh(Geo.cylinderBetween(V3(x, -0.06f, -0.03f), V3(x, 0.06f, -0.03f), 0.011f, 5))
                }
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinderBetween(
                            V3(side * 1.57f, 0f, -0.02f),
                            V3(side * 1.64f, 0f, 0.02f),
                            0.042f, 8,
                        ),
                    )
                }
            }.build(),
            material = Materials.nickel,
            origin = satellite,
        ),
    )
    parts.add(
        Part(
            id = "os_hub", name = "Power conversion hub", system = OS_POWER, assembly = OS_SATELLITE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.50f, 0.40f, 0.36f, cornerRadius = 0.045f, bevel = 0.016f))
                addMesh(Geo.box(0.40f, 0.042f, 0.030f), Mat4.translation(V3(0f, 0.20f, 0.18f)))
                addMesh(Geo.cylinder(0.12f, 0.08f, 20), Mat4.translation(V3(0f, 0.25f, 0f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.018f, 0.18f, 0.22f), Mat4.translation(V3(side * 0.255f, 0f, 0f)))
                }
            }.build(),
            material = Materials.graphite,
            origin = satellite,
            animate = { ctx -> Mat4.rotateZ(sin(ctx.machineTime * 0.18f) * 1.4f) },
        ),
    )
    parts.add(
        Part(
            id = "os_hub_gold", name = "Gold thermal blanket", system = OS_POWER, assembly = OS_SATELLITE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.25f, 0.18f, 0.032f, cornerRadius = 0.025f, bevel = 0.006f), Mat4.translation(V3(0f, 0f, 0.196f)))
                for (i in -1..1) {
                    addMesh(Geo.box(0.17f, 0.010f, 0.012f), Mat4.translation(V3(0f, i * 0.048f, 0.216f)))
                }
            }.build(),
            material = orbitalGold,
            origin = V3.ZERO,
            parent = "os_hub",
        ),
    )
    parts.add(
        Part(
            id = "os_radiators", name = "Deployable radiators", system = OS_POWER, assembly = OS_SATELLITE,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.slab(0.055f, 0.36f, 0.62f, cornerRadius = 0.018f, bevel = 0.008f),
                        Mat4.translation(V3(side * 0.31f, -0.25f, -0.05f)),
                    )
                    addMesh(
                        Geo.box(0.010f, 0.025f, 0.48f),
                        Mat4.translation(V3(side * 0.34f, -0.25f, -0.05f)),
                    )
                }
            }.build(),
            material = Materials.paintCream,
            origin = satellite,
        ),
    )
    parts.add(
        Part(
            id = "os_thrusters", name = "Attitude thrusters", system = OS_CTRL, assembly = OS_SATELLITE,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.taperedCylinder(0.035f, 0.018f, 0.12f, 10),
                        Mat4.translation(V3(side * 0.25f, -0.22f, 0.16f)).multiply(Mat4.rotateX(90f)),
                    )
                    addMesh(
                        Geo.taperedCylinder(0.035f, 0.018f, 0.12f, 10),
                        Mat4.translation(V3(side * 0.25f, 0.22f, -0.16f)).multiply(Mat4.rotateX(-90f)),
                    )
                }
            }.build(),
            material = Materials.emitBlue,
            origin = satellite,
        ),
    )

    // Solar blankets, interconnect grid and structural spars.
    listOf(-1f, 1f).forEach { side ->
        val suffix = if (side < 0f) "l" else "r"
        parts.add(
            Part(
                id = "os_wing_$suffix", name = "${suffix.uppercase()} solar wing",
                system = OS_ARRAY, assembly = OS_WINGS,
                mesh = wingBase(side), material = solarBlanket,
                origin = satellite,
                animate = { ctx -> wingTracking(ctx) },
            ),
        )
        parts.add(
            Part(
                id = "os_wing_${suffix}_grid", name = "${suffix.uppercase()} wing interconnect",
                system = OS_ARRAY, assembly = OS_WINGS,
                mesh = wingGrid(side), material = orbitalGold,
                origin = satellite,
                animate = { ctx -> wingTracking(ctx) },
            ),
        )
        parts.add(
            Part(
                id = "os_wing_${suffix}_spar", name = "${suffix.uppercase()} wing spar",
                system = OS_FRAME, assembly = OS_WINGS,
                mesh = wingSpar(side), material = Materials.nickel,
                origin = satellite,
                animate = { ctx -> wingTracking(ctx) },
            ),
        )
    }

    // Transmitter dish, feed and gimbal.
    parts.add(
        Part(
            id = "os_mast", name = "Transmitter mast", system = OS_FRAME, assembly = OS_TRANSMITTER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinderBetween(V3(0f, -0.14f, 0f), V3(0f, -0.40f, 0f), 0.035f, 10))
                addMesh(Geo.torus(0.10f, 0.010f, 18, 4), Mat4.translation(V3(0f, -0.30f, 0f)))
            }.build(),
            material = Materials.darkSteel,
            origin = satellite,
        ),
    )
    parts.add(
        Part(
            id = "os_dish", name = "Microwave transmitter dish", system = OS_LINK, assembly = OS_TRANSMITTER,
            mesh = dishMesh(), material = Materials.nickel,
            origin = V3(0f, 2.17f, 0f),
            rotation = Mat4.rotateX(4f),
            animate = { ctx ->
                Mat4.rotateX(5f * sin(ctx.machineTime * 0.50f)) .multiply(Mat4.rotateZ(3f * sin(ctx.machineTime * 0.31f + 1f)))
            },
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "os_dish_trim", name = "Transmitter feed and gimbal", system = OS_LINK, assembly = OS_TRANSMITTER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.44f, 0.012f, 34, 4), Mat4.translation(V3(0f, -0.10f, 0f)))
                addMesh(Geo.sphere(0.050f, 16, 8), Mat4.translation(V3(0f, 0.22f, 0f)))
                addMesh(Geo.torus(0.135f, 0.014f, 22, 4), Mat4.translation(V3(0f, 0.10f, 0f)))
            }.build(),
            material = Materials.emitAmber,
            origin = V3(0f, 2.17f, 0f),
            rotation = Mat4.rotateX(4f),
            animate = { ctx ->
                Mat4.rotateX(5f * sin(ctx.machineTime * 0.50f)) .multiply(Mat4.rotateZ(3f * sin(ctx.machineTime * 0.31f + 1f)))
            },
            doubleSided = true,
        ),
    )

    // Ground rectenna.
    parts.add(
        Part(
            id = "os_rectenna", name = "Ground rectenna", system = OS_GROUND, assembly = OS_RECEIVER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.lathe(listOf(0.46f to 0f, 0.44f to 0.05f, 0.32f to 0.11f, 0.08f to 0.17f), 34, capBottom = true))
                addMesh(Geo.torus(0.46f, 0.012f, 34, 4), Mat4.translation(V3(0f, 0.0f, 0f)))
            }.build(),
            material = Materials.nickel,
            origin = V3(0.82f, 0.34f, 0.15f),
            doubleSided = true,
        ),
    )
    parts.add(
        Part(
            id = "os_rectenna_grid", name = "Rectenna dipole grid", system = OS_GROUND, assembly = OS_RECEIVER,
            mesh = MeshBuilder().apply {
                for (radius in listOf(0.18f, 0.29f, 0.40f)) {
                    addMesh(Geo.torus(radius, 0.006f, 30, 4), Mat4.translation(V3(0f, 0.04f, 0f)))
                }
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val x = (0.43f * kotlin.math.cos(angle)).toFloat()
                    val z = (0.43f * kotlin.math.sin(angle)).toFloat()
                    addMesh(Geo.cylinderBetween(V3(0f, 0.04f, 0f), V3(x, 0.035f, z), 0.007f, 5))
                }
                addMesh(Geo.cylinderBetween(V3(0f, 0.03f, 0f), V3(0f, 0.26f, 0f), 0.018f, 8))
                addMesh(Geo.sphere(0.045f, 14, 7), Mat4.translation(V3(0f, 0.27f, 0f)))
            }.build(),
            material = Materials.emitAmber,
            origin = V3(0.82f, 0.34f, 0.15f),
        ),
    )
    parts.add(
        Part(
            id = "os_ground_cable", name = "Receiver grid cable", system = OS_GROUND, assembly = OS_RECEIVER,
            mesh = Geo.tube(
                listOf(
                    V3(0.39f, 0.36f, 0.19f),
                    V3(0.17f, 0.34f, 0.28f),
                    V3(-0.30f, 0.35f, 0.40f),
                    V3(-0.79f, 0.35f, 0.40f),
                ),
                0.018f, 6, subdivide = 4,
            ),
            material = orbitalGold,
        ),
    )

    // Ground control station and the small city that consumes the power.
    parts.add(
        Part(
            id = "os_ground_control", name = "Rectenna control station", system = OS_CTRL, assembly = OS_RECEIVER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.34f, 0.26f, 0.28f, cornerRadius = 0.03f, bevel = 0.012f), Mat4.translation(V3(1.34f, 0.44f, 0.50f)))
                addMesh(Geo.box(0.20f, 0.10f, 0.016f), Mat4.translation(V3(1.34f, 0.47f, 0.645f)))
                addMesh(Geo.cylinderBetween(V3(1.34f, 0.58f, 0.50f), V3(1.34f, 0.88f, 0.50f), 0.012f, 6))
                addMesh(Geo.torus(0.075f, 0.010f, 16, 4), Mat4.translation(V3(1.34f, 0.89f, 0.50f)))
            }.build(),
            material = Materials.bedPlate,
        ),
    )
    parts.add(
        Part(
            id = "os_ground_lights", name = "Receiver status beacons", system = OS_CTRL, assembly = OS_RECEIVER,
            mesh = MeshBuilder().apply {
                listOf(V3(1.20f, 0.48f, 0.65f), V3(1.47f, 0.48f, 0.65f)).forEach { point ->
                    addMesh(Geo.cylinder(0.024f, 0.014f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(point))
                }
            }.build(),
            material = Materials.emitCyan,
        ),
    )
    parts.add(
        Part(
            id = "os_city", name = "Receiver city", system = OS_GROUND, assembly = OS_CITY,
            mesh = MeshBuilder().apply {
                listOf(
                    Triple(V3(-1.25f, 0.53f, 0.40f), 0.34f, 0.62f),
                    Triple(V3(-0.91f, 0.47f, 0.34f), 0.26f, 0.42f),
                    Triple(V3(-1.46f, 0.44f, 0.58f), 0.20f, 0.34f),
                ).forEach { (center, width, height) ->
                    addMesh(Geo.slab(width, width * 0.78f, height, cornerRadius = 0.02f, bevel = 0.008f), Mat4.translation(center))
                    addMesh(Geo.box(width * 0.55f, 0.035f, width * 0.55f), Mat4.translation(center + V3(0f, height * 0.52f, 0f)))
                }
            }.build(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "os_city_windows", name = "City window grid", system = OS_GROUND, assembly = OS_CITY,
            mesh = MeshBuilder().apply {
                for (building in 0..2) {
                    val x = listOf(-1.25f, -0.91f, -1.46f)[building]
                    val y = listOf(0.53f, 0.47f, 0.44f)[building]
                    val z = listOf(0.40f, 0.34f, 0.58f)[building]
                    val width = listOf(0.34f, 0.26f, 0.20f)[building]
                    for (row in 0..3) {
                        for (col in -1..1) {
                            addMesh(
                                Geo.box(0.028f, 0.040f, 0.010f),
                                Mat4.translation(V3(x + col * width * 0.23f, y + row * 0.055f, z + width * 0.40f)),
                            )
                        }
                    }
                }
                addMesh(Geo.box(0.48f, 0.014f, 0.020f), Mat4.translation(V3(-1.08f, 0.35f, 0.38f)))
            }.build(),
            material = Materials.emitAmber,
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "wings", "Deployable solar wing",
            "Two blanket wings collect sunlight and convert it to direct current. Their long span keeps the cells facing the Sun while the hub stays shaded and cool.",
            "os_wing_l", V3(-0.72f, 0.02f, 0.04f),
            CameraPose(24f, 18f, 1f), readoutLabel = "SOLAR ARRAY",
        ),
        Hotspot(
            "hub", "Power conversion hub",
            "The hub conditions the array voltage, runs the attitude control and carries the heat away through the white radiator panels. Everything else hangs from this central bus.",
            "os_hub", V3(0f, 0.02f, 0.20f),
            CameraPose(32f, 18f, 1f), readoutLabel = "POWER BUS",
        ),
        Hotspot(
            "transmitter", "Microwave transmitter",
            "The dish turns the generated electricity into a tight 5.8 GHz microwave beam. A gimbal keeps the beam pointed at the ground rectenna as the satellite moves.",
            "os_dish", V3(0f, -0.02f, 0.25f),
            CameraPose(36f, 24f, 1f), readoutLabel = "BEAM FORMER",
        ),
        Hotspot(
            "rectenna", "Ground rectenna",
            "Millions of small dipole antennas catch the microwave beam and rectify it back into direct current. The mesh is deliberately sparse so sunlight and rain pass through.",
            "os_rectenna", V3(0f, 0.08f, 0.20f),
            CameraPose(48f, 28f, 1f), readoutLabel = "MICROWAVE TO DC",
        ),
        Hotspot(
            "city", "Receiver city",
            "The rectenna feeds the city grid through a short high-voltage bus. At night the receiver still works, which is why an orbital solar plant is more than a daytime generator.",
            "os_city", V3(0.0f, 0.18f, 0.26f),
            CameraPose(22f, 12f, 1f), readoutLabel = "END USE",
        ),
        Hotspot(
            "radiators", "Thermal radiators",
            "Solar cells only convert part of the sunlight they receive. The rest becomes heat, and the radiators reject it into space so the electronics stay within their operating range.",
            "os_radiators", V3(0f, 0.12f, 0.22f),
            CameraPose(146f, 18f, 1f), readoutLabel = "THERMAL",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Collect",
            "The solar wings face the Sun and generate direct current. Their size is set by the power needed on the ground, not by the size of the satellite body.",
            OS_ARRAY, CameraPose(24f, 18f, 1.02f), 0.10f, 0.25f,
        ),
        GuideStep(
            "Condition",
            "The hub converts the array voltage to the transmitter's high-voltage bus and rejects waste heat through the radiator wings.",
            OS_POWER, CameraPose(32f, 18f, 1.02f), 0.08f, 0.52f,
        ),
        GuideStep(
            "Point",
            "The gimballed dish holds a narrow beam on the rectenna. Attitude thrusters and reaction wheels keep the satellite stable while the wings track the Sun.",
            OS_LINK, CameraPose(36f, 24f, 1.02f), 0.06f, 0.72f,
        ),
        GuideStep(
            "Receive",
            "The ground rectenna converts the beam back to electricity. Its open mesh lets sunlight and rain through, so the land underneath can still be used.",
            OS_GROUND, CameraPose(48f, 28f, 1.02f), 0.12f, 0.62f,
        ),
        GuideStep(
            "Distribute",
            "A short cable ties the rectenna into the city grid. The receiver can keep supplying power after sunset, drawing on the satellite's continuous view of the Sun.",
            OS_CITY, CameraPose(22f, 12f, 1.02f), 0.04f, 0.42f,
        ),
        GuideStep(
            "The assembly",
            "Explode the satellite and ground segment to separate the wings, hub, transmitter, rectenna and receiver city.",
            null, CameraPose(32f, 20f, 0.96f), 1f, 0.48f,
        ),
    )

    val beamPath = listOf(
        beamStart,
        beamStart.lerp(beamEnd, 0.28f),
        beamStart.lerp(beamEnd, 0.64f),
        beamEnd,
    )

    return ObjectSpec(
        id = "orbital_solar",
        index = 22,
        title = "HELIOS O-1",
        designation = "HO-1 / ORBITAL SOLAR POWER",
        category = "Space",
        categoryShort = "ORB",
        tagline = "Sunlight collected in orbit, delivered after dark.",
        summary = "A space solar power system in cutaway: tracking photovoltaic wings, a power hub with thermal radiators, a gimballed microwave transmitter, the ground rectenna and the city that receives the beam.",
        details = listOf(
            "HELIOS O-1 is a solar power satellite. Its wings collect sunlight above the weather and convert it to electricity; the hub conditions that power and the transmitter turns it into a narrow microwave beam aimed at a ground rectenna.",
            "The receiver converts the beam back to direct current and feeds it into the city grid. Because the satellite can stay in sunlight while the ground is in darkness, the system can deliver power after sunset.",
        ),
        specs = listOf(
            SpecRow("Array", "2.4 MW"),
            SpecRow("Beam", "5.8 GHz"),
            SpecRow("Wing span", "3.2 m scale"),
            SpecRow("Link", "96.4%"),
            SpecRow("Orbit", "Geostationary"),
        ),
        tags = listOf("space", "solar", "satellite", "microwave", "energy", "orbital", "power", "rectenna"),
        accent = 0xFFFFC46B.toInt(),
        control = ControlSpec(
            label = "SUN ANGLE",
            lowLabel = "ECLIPSE",
            highLabel = "NOON",
            defaultValue = 0.64f,
            hint = "Wing tracking, beam power and receiver output",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(32f, 17f, 1.02f),
        thumbnailPose = CameraPose(31f, 19f, 1.04f),
        thumbnailTime = 2.5f,
        thumbnailControl = 0.72f,
        focusOffset = V3(0f, 0.12f, 0f),
        readouts = { ctx ->
            val output = 1.5f + 1.5f * ctx.control
            val link = 91f + 7f * ctx.control
            listOf(
                Readout("ARRAY", String.format("%.1f", output), "MW", accent = true),
                Readout("BEAM", "5.8", "GHz"),
                Readout("LINK", String.format("%.1f", link), "%"),
            )
        },
        effects = { ctx ->
            val pulse = 0.75f + 0.25f * sin(ctx.machineTime * 3.1f)
            val power = (0.28f + 0.62f * ctx.control) * pulse
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.78f, 0xFFFFC46B.toInt(), 0f, 0.14f),
                SceneEffect.Beam(beamStart, beamEnd, 0xFFFFCC78.toInt(), power),
                SceneEffect.Flow(
                    beamPath, 0xFFFFE0A8.toInt(), count = 22,
                    speed = 0.22f, phase = 0.15f, dotRadius = 0.030f,
                ),
                SceneEffect.Glow(beamStart, 0.28f, 0xFFFFD18A.toInt(), 0.22f + 0.22f * power),
                SceneEffect.Glow(beamEnd, 0.32f, 0xFFFFB64E.toInt(), 0.28f + 0.16f * power),
                SceneEffect.Ring(V3(0.82f, 0.36f, 0.15f), 0.56f, 0xFFFFC46B.toInt(), 0.35f, 0.18f + 0.12f * pulse),
                SceneEffect.Glow(V3(-1.08f, 0.62f, 0.40f), 0.36f, 0xFFFFB65C.toInt(), 0.16f + 0.18f * ctx.control),
                SceneEffect.Spark(V3(0.16f, 2.30f, 0.08f), 0xFFFFD38C.toInt(), 0.035f, 0.32f),
            )
        },
        descriptionTitle = "The orbital power loop",
    )
}
