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
import kotlin.math.cos
import kotlin.math.sin

private const val W_ROTOR = "ROTOR"
private const val W_DRIVE = "DRIVE"
private const val W_GENERATOR = "GENERATOR"
private const val W_NACELLE = "NACELLE"
private const val W_YAW = "YAW"
private const val W_TOWER = "TOWER"

private const val W_BASE = "BASE"
private const val W_ROTOR_ASSEMBLY = "ROTOR"
private const val W_DRIVETRAIN = "DRIVETRAIN"
private const val W_SHELL = "NACELLE SHELL"
private const val W_TOWER_ASSEMBLY = "TOWER"

private fun bladeTipMarker(span: Float): Mesh = Geo.slab(0.32f, 0.2f, 0.16f, 0.03f, 0.01f)
    .transformed(Mat4.translation(V3(0f, span, 0f)))

fun windTurbine(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "helix",
            width = 3.6f,
            depth = 3.6f,
            top = 0.3f,
            system = W_TOWER,
            assembly = W_BASE,
            accentMaterial = Materials.emitCyan,
        )
    )

    val tower = MeshBuilder().apply {
        addMesh(
            Geo.taperedCylinder(0.5f, 0.32f, 2.9f, 32, capTop = true, capBottom = true),
            Mat4.translation(V3(0.9f, 1.75f, 0f)),
        )
        listOf(0.72f to 0.46f, 1.5f to 0.41f, 2.28f to 0.36f).forEach { (height, radius) ->
            addMesh(Geo.torus(radius, 0.035f, 32, 6), Mat4.translation(V3(0.9f, height, 0f)))
        }
        addMesh(
            Geo.slab(0.46f, 0.025f, 0.68f, 0.04f, 0.008f),
            Mat4.translation(V3(0.9f, 0.68f, 0.46f)),
        )
        addMesh(Geo.torus(0.1f, 0.022f, 16, 5), Mat4.translation(V3(0.9f, 0.68f, 0.48f)))
    }.build()
    parts.add(
        Part(
            id = "tower", name = "Tapered tower", system = W_TOWER, assembly = W_TOWER_ASSEMBLY,
            mesh = tower, material = Materials.paintCream,
        )
    )

    val ladder = MeshBuilder().apply {
        addMesh(
            Geo.cylinderBetween(V3(0.96f, 0.38f, 0.36f), V3(0.96f, 2.86f, 0.3f), 0.022f, 7),
            Mat4.identity(),
        )
        addMesh(
            Geo.cylinderBetween(V3(1.12f, 0.38f, 0.36f), V3(1.12f, 2.86f, 0.3f), 0.022f, 7),
            Mat4.identity(),
        )
        var y = 0.55f
        while (y < 2.8f) {
            addMesh(
                Geo.cylinderBetween(V3(0.96f, y, 0.36f), V3(1.12f, y, 0.36f), 0.018f, 6),
                Mat4.identity(),
            )
            y += 0.27f
        }
    }.build()
    parts.add(
        Part(
            id = "tower_ladder", name = "Service ladder", system = W_TOWER, assembly = W_TOWER_ASSEMBLY,
            mesh = ladder, material = Materials.titanium,
        )
    )

    val yawRing = MeshBuilder().apply {
        addMesh(Geo.cylinder(0.64f, 0.16f, 40, bevel = 0.03f), Mat4.translation(V3(0.9f, 3.05f, 0f)))
        addMesh(Geo.hollowCylinder(0.68f, 0.56f, 0.11f, 40), Mat4.translation(V3(0.9f, 3.13f, 0f)))
        addMesh(Geo.torus(0.59f, 0.026f, 40, 5), Mat4.translation(V3(0.9f, 3.12f, 0f)))
    }.build()
    parts.add(
        Part(
            id = "yaw_ring", name = "Yaw bearing", system = W_YAW, assembly = W_TOWER_ASSEMBLY,
            mesh = yawRing, material = Materials.darkSteel,
        )
    )
    parts.add(
        Part(
            id = "yaw_drives", name = "Yaw drives", system = W_YAW, assembly = W_TOWER_ASSEMBLY,
            mesh = MeshBuilder().apply {
                for (z in listOf(-0.38f, 0.38f)) {
                    addMesh(
                        Geo.cylinder(0.17f, 0.34f, 20, bevel = 0.025f),
                        Mat4.translation(V3(0.9f, 3.31f, z)),
                    )
                    addMesh(
                        Geo.torus(0.19f, 0.028f, 22, 5),
                        Mat4.translation(V3(0.9f, 3.42f, z)),
                    )
                }
            }.build(),
            material = Materials.brass,
        )
    )

    val bedplate = MeshBuilder().apply {
        addMesh(
            Geo.slab(2.8f, 1.12f, 0.18f, 0.1f, 0.025f),
            Mat4.translation(V3(0.92f, 3.32f, 0f)),
        )
        addMesh(
            Geo.slab(2.5f, 0.9f, 0.08f, 0.06f, 0.015f),
            Mat4.translation(V3(0.92f, 3.22f, 0f)),
        )
        for (x in listOf(-0.18f, 0.52f, 1.22f, 1.92f)) {
            addMesh(
                Geo.slab(0.1f, 0.9f, 0.1f, 0.02f, 0.01f),
                Mat4.translation(V3(x, 3.24f, 0f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "bedplate", name = "Nacelle bedplate", system = W_DRIVE, assembly = W_DRIVETRAIN,
            mesh = bedplate, material = Materials.darkSteel,
        )
    )

    val shaft = MeshBuilder().apply {
        addMesh(machineCylinderX(-0.34f, 0.17f, 1.55f, 24), Mat4.identity())
        addMesh(machineCylinderX(-0.95f, 0.21f, 0.4f, 24), Mat4.identity())
        addMesh(machineTorusX(-0.54f, 0.27f, 0.04f), Mat4.identity())
        addMesh(machineTorusX(-0.04f, 0.29f, 0.04f), Mat4.identity())
    }.build()
    parts.add(
        Part(
            id = "main_shaft", name = "Main shaft", system = W_DRIVE, assembly = W_DRIVETRAIN,
            mesh = shaft, material = Materials.brushedSteel, origin = V3(0f, 3.55f, 0f),
        )
    )

    val brake = MeshBuilder().apply {
        addMesh(machineCylinderX(-0.02f, 0.42f, 0.1f, 32), Mat4.identity())
        addMesh(machineTorusX(0.04f, 0.35f, 0.028f), Mat4.identity())
        addMesh(
            Geo.slab(0.2f, 0.28f, 0.34f, 0.03f, 0.01f),
            Mat4.translation(V3(0.02f, 0.5f, 0.24f)),
        )
    }.build()
    parts.add(
        Part(
            id = "brake", name = "Brake disc", system = W_DRIVE, assembly = W_DRIVETRAIN,
            mesh = brake, material = Materials.graphite, origin = V3(0f, 3.55f, 0f),
        )
    )

    val gearbox = MeshBuilder().apply {
        addMesh(machineCylinderX(0.55f, 0.48f, 0.82f, 32, bevel = 0.06f), Mat4.identity())
        addMesh(machineCylinderX(0.55f, 0.37f, 1.02f, 32), Mat4.identity())
        for (x in listOf(0.2f, 0.55f, 0.9f)) {
            addMesh(machineTorusX(x, 0.4f, 0.05f), Mat4.identity())
        }
        addMesh(machineTorusX(1.0f, 0.28f, 0.04f), Mat4.identity())
        addMesh(
            Geo.slab(0.24f, 0.22f, 0.18f, 0.03f, 0.01f),
            Mat4.translation(V3(0.55f, 0.56f, 0.12f)),
        )
        addMesh(
            Geo.cylinderBetween(V3(0.55f, 0.62f, 0.12f), V3(0.55f, 0.8f, 0.03f), 0.055f, 8),
            Mat4.identity(),
        )
    }.build()
    parts.add(
        Part(
            id = "gearbox", name = "Planetary gearbox", system = W_DRIVE, assembly = W_DRIVETRAIN,
            mesh = gearbox, material = Materials.darkSteel, origin = V3(0f, 3.55f, 0f),
        )
    )
    parts.add(
        Part(
            id = "gearbox_brass", name = "Gearbox stages", system = W_DRIVE, assembly = W_DRIVETRAIN,
            mesh = MeshBuilder().apply {
                for (x in listOf(0.2f, 0.55f, 0.9f)) {
                    addMesh(machineTorusX(x, 0.42f, 0.024f), Mat4.identity())
                }
            }.build(),
            material = Materials.brass, origin = V3(0f, 3.55f, 0f),
        )
    )

    val generator = MeshBuilder().apply {
        addMesh(machineCylinderX(1.7f, 0.44f, 1.02f, 32, bevel = 0.04f), Mat4.identity())
        addMesh(machineCylinderX(1.17f, 0.35f, 0.12f, 28), Mat4.identity())
        addMesh(machineCylinderX(2.23f, 0.37f, 0.12f, 28), Mat4.identity())
        addMesh(machineCylinderX(2.28f, 0.12f, 0.18f, 20), Mat4.identity())
    }.build()
    parts.add(
        Part(
            id = "generator", name = "Permanent-magnet generator", system = W_GENERATOR, assembly = W_DRIVETRAIN,
            mesh = generator, material = Materials.copper, origin = V3(0f, 3.55f, 0f),
        )
    )
    parts.add(
        Part(
            id = "generator_fins", name = "Cooling fins", system = W_GENERATOR, assembly = W_DRIVETRAIN,
            mesh = MeshBuilder().apply {
                for (i in 0 until 7) {
                    addMesh(machineTorusX(1.38f + i * 0.11f, 0.47f, 0.022f), Mat4.identity())
                }
            }.build(),
            material = Materials.brass, origin = V3(0f, 3.55f, 0f),
        )
    )
    parts.add(
        Part(
            id = "generator_terminal", name = "Generator terminal box", system = W_GENERATOR, assembly = W_DRIVETRAIN,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.36f, 0.42f, 0.28f, 0.04f, 0.015f), Mat4.translation(V3(1.82f, 0.6f, 0f)))
                addMesh(Geo.slab(0.24f, 0.05f, 0.1f, 0.02f, 0.005f), Mat4.translation(V3(1.82f, 0.77f, 0f)))
            }.build(),
            material = Materials.paintSlate, origin = V3(0f, 3.55f, 0f),
        )
    )

    val shell = MeshBuilder().apply {
        addMesh(
            Geo.arcShell(
                outerRadius = 0.9f,
                innerRadius = 0.81f,
                height = 2.6f,
                segments = 34,
                startAngle = 30f,
                sweepAngle = 260f,
                capEnds = true,
            ),
            Mat4.translation(V3(1.02f, 3.55f, 0f)).multiply(Mat4.rotateZ(-90f)),
        )
        addMesh(machineTorusX(-0.26f, 0.86f, 0.05f), Mat4.translation(V3(0f, 3.55f, 0f)))
        addMesh(machineTorusX(2.3f, 0.84f, 0.05f), Mat4.translation(V3(0f, 3.55f, 0f)))
        addMesh(
            Geo.slab(0.22f, 0.9f, 0.08f, 0.03f, 0.008f),
            Mat4.translation(V3(2.24f, 3.98f, 0f)),
        )
    }.build()
    parts.add(
        Part(
            id = "nacelle_shell", name = "Nacelle shell", system = W_NACELLE, assembly = W_SHELL,
            mesh = shell, material = Materials.paintCream,
        )
    )
    parts.add(
        Part(
            id = "nacelle_signal", name = "Nacelle marker", system = W_NACELLE, assembly = W_SHELL,
            mesh = MeshBuilder().apply {
                addMesh(machineTorusX(2.31f, 0.68f, 0.018f), Mat4.translation(V3(0f, 3.55f, 0f)))
                addMesh(
                    Geo.slab(0.32f, 0.025f, 0.07f, 0.006f, 0f),
                    Mat4.translation(V3(2.34f, 4.1f, 0.55f)),
                )
            }.build(),
            material = Materials.emitCyan,
        )
    )

    val rotorHubOrigin = V3(-1.16f, 3.55f, 0f)
    val hub = MeshBuilder().apply {
        addMesh(machineCylinderX(-0.12f, 0.55f, 0.46f, 36, bevel = 0.04f), Mat4.identity())
        addMesh(machineCylinderX(0.15f, 0.34f, 0.28f, 28), Mat4.identity())
        addMesh(
            machineLatheX(
                -0.32f,
                listOf(
                    0.01f to 0f, 0.36f to 0.06f, 0.53f to 0.22f,
                    0.55f to 0.42f, 0.42f to 0.65f, 0.22f to 0.82f, 0.04f to 0.94f,
                ),
                32,
                capBack = true,
            ),
            Mat4.identity(),
        )
        addMesh(machineTorusX(-0.35f, 0.5f, 0.03f), Mat4.identity())
    }.build()
    val rotorAngle = { ctx: AnimContext -> Mat4.rotateX(ctx.machineTime * 0.55f) }
    parts.add(
        Part(
            id = "rotor_hub", name = "Rotor hub and spinner", system = W_ROTOR, assembly = W_ROTOR_ASSEMBLY,
            mesh = hub, material = Materials.paintOrange, origin = rotorHubOrigin,
            animate = rotorAngle,
            explode = V3(-1.1f, 0f, 0f),
        )
    )
    val blades = MeshBuilder().apply {
        val blade = Geo.roundedBlade(
            span = 2.8f,
            rootChord = 0.52f,
            tipChord = 0.13f,
            rootThickness = 0.15f,
            tipThickness = 0.045f,
            twistDegrees = 13f,
            sweep = 0.06f,
            sections = 6,
            perimeter = 6,
        )
        for (angle in listOf(0f, 120f, 240f)) {
            val transform = Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, 0.4f, 0f)))
            addMesh(blade, transform)
            addMesh(
                Geo.cylinder(0.25f, 0.55f, 20, bevel = 0.04f),
                transform.multiply(Mat4.translation(V3(0f, 0.05f, 0f))).multiply(Mat4.rotateZ(90f)),
            )
        }
        addMesh(machineTorusX(0.05f, 0.56f, 0.035f), Mat4.identity())
    }.build()
    parts.add(
        Part(
            id = "rotor_blades", name = "Composite blades", system = W_ROTOR, assembly = W_ROTOR_ASSEMBLY,
            mesh = blades, material = Materials.paintCream, origin = rotorHubOrigin,
            animate = rotorAngle, doubleSided = true,
            explode = V3(-1.55f, 0f, 0f),
        )
    )
    parts.add(
        Part(
            id = "rotor_tips", name = "Blade tip markers", system = W_ROTOR, assembly = W_ROTOR_ASSEMBLY,
            mesh = MeshBuilder().apply {
                for (angle in listOf(0f, 120f, 240f)) {
                    addMesh(
                        Geo.slab(0.34f, 0.19f, 0.12f, 0.028f, 0.008f),
                        Mat4.rotateX(angle).multiply(Mat4.translation(V3(0f, 3.2f, 0f))),
                    )
                }
            }.build(),
            material = Materials.paintOrange, origin = rotorHubOrigin, animate = rotorAngle,
            explode = V3(-1.55f, 0f, 0f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "blades", "Turbine blades",
            "Three composite blades turn the moving air into torque. Their twist keeps the angle of attack near optimum from root to tip, while the pitch bearing lets the controller feather them in a storm.",
            "rotor_blades", V3(0f, 2.1f, 0f),
            CameraPose(36f, 18f, 1.08f), readoutLabel = "ROTOR",
        ),
        Hotspot(
            "hub", "Spinner and pitch bearings",
            "The orange spinner sheds ice and rain. Behind it, three independent pitch bearings rotate each blade by a few degrees to regulate power.",
            "rotor_hub", V3(-0.4f, 0.1f, 0f),
            CameraPose(52f, 16f, 1.18f), readoutLabel = "HUB",
        ),
        Hotspot(
            "gearbox", "Planetary gearbox",
            "The rotor turns slowly but with enormous torque. A three-stage planetary set multiplies speed roughly 97 times before the generator shaft.",
            "gearbox", V3(0.55f, 0.25f, 0.35f),
            CameraPose(30f, 24f, 1.16f), readoutLabel = "GEARBOX",
        ),
        Hotspot(
            "generator", "Permanent-magnet generator",
            "The copper-wound stator surrounds a permanent-magnet rotor. Cooling fins carry heat into the nacelle air, keeping the magnets below their demagnetisation limit.",
            "generator", V3(1.7f, 0.3f, 0.35f),
            CameraPose(24f, 20f, 1.18f), readoutLabel = "GENERATOR",
        ),
        Hotspot(
            "yaw", "Yaw bearing and drives",
            "Two electric drives walk the nacelle around the tower ring. The bearing carries the whole rotor load while allowing the turbine to face the wind.",
            "yaw_ring", V3(0.9f, 0.1f, 0.55f),
            CameraPose(46f, 30f, 1.22f), readoutLabel = "YAW",
        ),
        Hotspot(
            "tower", "Tapered steel tower",
            "The tower is a tapered steel shell with a service ladder inside. Its wall thickness changes with height to keep the natural frequency away from the rotor's passing frequency.",
            "tower", V3(0.9f, 1.2f, 0.4f),
            CameraPose(58f, 14f, 1.04f), readoutLabel = "TOWER",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Catch the wind",
            "The blades are aerofoils, not paddles. Air moving across the curved section creates lift, and the resulting torque turns the hub.",
            W_ROTOR, CameraPose(38f, 16f, 1.02f), 0f, 0.85f,
        ),
        GuideStep(
            "Slow torque, high speed",
            "The main shaft enters a planetary gearbox. Three stages multiply the rotor's speed before the generator, while the reaction torque is taken by the bedplate.",
            W_DRIVE, CameraPose(28f, 22f, 1.12f), 0.35f, 0.75f,
        ),
        GuideStep(
            "Make electricity",
            "The generator converts shaft torque into electrical power. Copper windings and a permanent-magnet rotor eliminate the need for a separate excitation supply.",
            W_GENERATOR, CameraPose(20f, 18f, 1.18f), 0f, 0.95f,
        ),
        GuideStep(
            "Face the wind",
            "The yaw drives turn the nacelle in small increments. A brake holds the position when the wind drops, and the slip ring carries power and data down the tower.",
            W_YAW, CameraPose(52f, 28f, 1.14f), 0.25f, 0.6f,
        ),
        GuideStep(
            "The whole machine",
            "Explode the nacelle to separate the rotor, drivetrain, generator and shell. Every module can be serviced independently, even 100 metres above the ground.",
            null, CameraPose(34f, 24f, 0.98f), 1f, 1f,
        ),
    )

    return ObjectSpec(
        id = "wind_turbine",
        index = 3,
        title = "HELIX W-3",
        designation = "W-3 / 3.4 MW DRIVETRAIN",
        category = "Energy",
        categoryShort = "WIND",
        tagline = "A power station, opened at the nacelle.",
        summary = "A three-blade wind turbine sectioned through the nacelle: pitch hub, main shaft, planetary gearbox, permanent-magnet generator, yaw drives and tapered tower.",
        details = listOf(
            "HELIX W-3 is a direct-to-grid wind turbine. The rotor turns at up to 14 rpm; the gearbox raises that to roughly 1,350 rpm for a compact permanent-magnet generator.",
            "The nacelle is shown with its upper shell opened. Pitch bearings at each blade root, a hydraulic brake on the high-speed shaft and two yaw drives around the tower ring are all visible.",
        ),
        specs = listOf(
            SpecRow("Rated power", "3.4 MW"),
            SpecRow("Rotor diameter", "5.8 m (model)"),
            SpecRow("Gearbox ratio", "97 : 1"),
            SpecRow("Generator", "690 V / PMSG"),
            SpecRow("Cut-in wind", "3.5 m/s"),
        ),
        tags = listOf("energy", "wind turbine", "renewable", "gearbox", "generator", "nacelle"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "WIND SPEED",
            lowLabel = "CUT-IN",
            highLabel = "RATED",
            defaultValue = 0.62f,
            hint = "Rotor speed and electrical output",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(38f, 18f, 1.02f),
        thumbnailPose = CameraPose(40f, 17f, 1.0f),
        thumbnailTime = 2.6f,
        thumbnailControl = 0.7f,
        readouts = { ctx ->
            val wind = 4f + 18f * ctx.control
            val rpm = 4.2f + 9.6f * ctx.control
            val power = 0.15f + 3.25f * ctx.control
            listOf(
                Readout("OUTPUT", String.format("%.2f", power), "MW", accent = true),
                Readout("ROTOR", String.format("%.1f", rpm), "rpm"),
                Readout("WIND", String.format("%.1f", wind), "m/s", warn = wind > 20f),
            )
        },
        effects = { ctx ->
            val lift = 0.55f + 0.45f * ctx.control
            listOf(
                SceneEffect.Flow(
                    points = listOf(
                        V3(-3.6f, 3.0f, -1.1f),
                        V3(-2.7f, 3.35f, -0.55f),
                        V3(-1.9f, 3.5f, -0.12f),
                        V3(-1.3f, 3.55f, 0f),
                    ),
                    color = 0xFFB8F6EC.toInt(),
                    count = 10,
                    speed = 0.34f,
                    phase = 0.15f,
                    dotRadius = 0.028f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(-3.6f, 4.1f, 0.9f),
                        V3(-2.7f, 3.85f, 0.5f),
                        V3(-1.8f, 3.65f, 0.12f),
                    ),
                    color = 0xFF6EF2DF.toInt(),
                    count = 8,
                    speed = 0.28f,
                    phase = 0.55f,
                    dotRadius = 0.022f,
                ),
                SceneEffect.Glow(V3(1.7f, 3.55f, 0f), 0.8f, 0xFFFFB25C.toInt(), 0.16f + 0.16f * lift),
                SceneEffect.Glow(V3(-1.16f, 3.55f, 0f), 0.65f, 0xFF6EF2DF.toInt(), 0.1f),
            )
        },
    )
}
