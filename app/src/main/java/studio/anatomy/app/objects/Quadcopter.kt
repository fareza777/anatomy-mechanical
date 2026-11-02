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
import kotlin.math.sin

private const val SKY_FRAME = "AIRFRAME"
private const val SKY_POWER = "PROPULSION"
private const val SKY_CAMERA = "CAMERA"
private const val SKY_ENERGY = "ENERGY"

private const val S_BODY = "FUSELAGE"
private const val S_ARMS = "ARMS"
private const val S_MOTORS = "MOTORS"
private const val S_PROPS = "PROPELLERS"
private const val S_GIMBAL = "GIMBAL"
private const val S_PACK = "POWER PACK"
private const val S_GEAR = "LANDING GEAR"

private const val HOVER_Y = 1.55f
private val MOTOR_SPOTS = listOf(
    -1.12f to -1.12f,
    1.12f to -1.12f,
    -1.12f to 1.12f,
    1.12f to 1.12f,
)

private fun rotorAngle(ctx: AnimContext): Float =
    ctx.machineTime * (520f + 900f * ctx.control)

private fun bodyTilt(ctx: AnimContext): Mat4 {
    val amp = ctx.control
    val pitch = 5.5f * amp + 0.8f * sin(ctx.machineTime * 0.9f) * amp
    val roll = 3.2f * sin(ctx.machineTime * 0.72f + 1.4f) * amp
    val yaw = 5f * sin(ctx.machineTime * 0.31f) * (0.3f + 0.7f * amp)
    return Mat4.translation(V3(0f, HOVER_Y, 0f))
        .multiply(Mat4.rotateX(pitch))
        .multiply(Mat4.rotateZ(roll))
        .multiply(Mat4.rotateY(yaw))
        .multiply(Mat4.translation(V3(0f, -HOVER_Y, 0f)))
}

private fun motorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.135f, 0.15f, 26, bevel = 0.02f), Mat4.translation(V3(0f, 0.075f, 0f)))
    addMesh(Geo.torus(0.13f, 0.02f, 24, 5), Mat4.translation(V3(0f, 0.14f, 0f)))
    addMesh(Geo.cylinder(0.10f, 0.05f, 22), Mat4.translation(V3(0f, 0.185f, 0f)))
    addMesh(Geo.cylinder(0.09f, 0.04f, 20), Mat4.translation(V3(0f, 0.01f, 0f)))
}.build()

private fun rotorBlades(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.055f, 0.035f, 18), Mat4.translation(V3(0f, 0.02f, 0f)))
    for (i in 0 until 2) {
        addMesh(
            Geo.roundedBlade(
                span = 0.58f,
                rootChord = 0.115f,
                tipChord = 0.078f,
                rootThickness = 0.016f,
                tipThickness = 0.009f,
                twistDegrees = 9f,
                sweep = 0.02f,
                sections = 3,
                perimeter = 5,
            ).transformed(Mat4.rotateZ(90f)),
            Mat4.rotateY(180f * i).multiply(Mat4.translation(V3(0f, 0.02f, 0f))),
        )
    }
}.build()

private fun rotorTips(): Mesh = MeshBuilder().apply {
    for (i in 0 until 2) {
        addMesh(
            Geo.slab(0.10f, 0.07f, 0.018f, 0.015f, 0.005f),
            Mat4.rotateY(180f * i).multiply(Mat4.translation(V3(-0.52f, 0.02f, 0f))),
        )
    }
}.build()

fun quadcopter(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "skyfold",
            width = 3.15f,
            depth = 3.15f,
            top = 0.3f,
            system = SKY_FRAME,
            assembly = S_GEAR,
            accentMaterial = Materials.emitCyan,
        )
    )

    val airframe = MeshBuilder().apply {
        addMesh(Geo.slab(0.88f, 0.64f, 0.17f, 0.12f, 0.03f, 3), Mat4.translation(V3(0f, HOVER_Y, 0f)))
        addMesh(Geo.slab(0.70f, 0.50f, 0.13f, 0.10f, 0.03f, 3), Mat4.translation(V3(0f, 1.68f, 0.02f)))
        addMesh(Geo.slab(0.44f, 0.20f, 0.10f, 0.06f, 0.02f), Mat4.translation(V3(0f, 1.60f, 0.40f)))
        // Four carbon arms.
        MOTOR_SPOTS.forEach { (x, z) ->
            val inner = V3(x * 0.26f, HOVER_Y - 0.01f, z * 0.26f)
            val outer = V3(x * 0.92f, HOVER_Y + 0.02f, z * 0.92f)
            addMesh(Geo.cylinderBetween(inner, outer, 0.048f, 10))
            addMesh(
                Geo.slab(0.26f, 0.20f, 0.06f, 0.05f, 0.015f),
                Mat4.translation(V3(x * 0.97f, HOVER_Y + 0.04f, z * 0.97f)),
            )
        }
        // Antenna masts and rear light.
        addMesh(Geo.cylinderBetween(V3(-0.22f, 1.66f, -0.30f), V3(-0.30f, 2.02f, -0.44f), 0.014f, 6))
        addMesh(Geo.cylinderBetween(V3(0.22f, 1.66f, -0.30f), V3(0.30f, 2.02f, -0.44f), 0.014f, 6))
        addMesh(Geo.box(0.22f, 0.02f, 0.05f), Mat4.translation(V3(0f, 1.53f, -0.40f)))
    }.build()
    parts.add(
        Part(
            id = "sky_body", name = "Fuselage and arms", system = SKY_FRAME, assembly = S_BODY,
            mesh = airframe, material = Materials.graphite,
            animate = { ctx -> bodyTilt(ctx) },
        )
    )
    parts.add(
        Part(
            id = "sky_arms_accent", name = "Arm signal strips", system = SKY_FRAME, assembly = S_ARMS,
            mesh = MeshBuilder().apply {
                MOTOR_SPOTS.forEach { (x, z) ->
                    addMesh(
                        Geo.box(0.30f, 0.012f, 0.03f),
                        Mat4.translation(V3(x * 0.62f, HOVER_Y + 0.035f, z * 0.62f)),
                    )
                }
            }.build(),
            material = Materials.emitAmber,
            parent = "sky_body",
        )
    )

    MOTOR_SPOTS.forEachIndexed { index, (x, z) ->
        parts.add(
            Part(
                id = "sky_motor_${index + 1}", name = "Motor ${index + 1}",
                system = SKY_POWER, assembly = S_MOTORS,
                mesh = motorMesh(), material = if (index % 2 == 0) Materials.paintOrange else Materials.titanium,
                origin = V3(x * 1.0f, HOVER_Y + 0.02f, z * 1.0f),
                parent = "sky_body",
                explode = V3(x * 0.5f, 1.1f, z * 0.5f),
            )
        )
        val rotorOrigin = V3(x * 1.0f, HOVER_Y + 0.21f, z * 1.0f)
        parts.add(
            Part(
                id = "sky_rotor_${index + 1}", name = "Propeller ${index + 1}",
                system = SKY_POWER, assembly = S_PROPS,
                mesh = rotorBlades(), material = Materials.darkSteel,
                origin = rotorOrigin, parent = "sky_body", doubleSided = true,
                animate = { ctx -> Mat4.rotateY(rotorAngle(ctx) * (if (index % 2 == 0) 1f else -1f)) },
                explode = V3(x * 0.5f, 1.5f, z * 0.5f),
            )
        )
        parts.add(
            Part(
                id = "sky_tip_${index + 1}", name = "Propeller tips ${index + 1}",
                system = SKY_POWER, assembly = S_PROPS,
                mesh = rotorTips(), material = Materials.paintOrange,
                origin = rotorOrigin, parent = "sky_body", doubleSided = true,
                animate = { ctx -> Mat4.rotateY(rotorAngle(ctx) * (if (index % 2 == 0) 1f else -1f)) },
                explode = V3(x * 0.5f, 1.5f, z * 0.5f),
            )
        )
    }

    val gimbal = MeshBuilder().apply {
        addMesh(Geo.cylinder(0.115f, 0.10f, 22), Mat4.translation(V3(0f, 0.08f, 0f)))
        addMesh(Geo.torus(0.10f, 0.018f, 20, 5), Mat4.translation(V3(0f, 0.135f, 0f)))
        listOf(-1f, 1f).forEach { side ->
            addMesh(Geo.slab(0.05f, 0.16f, 0.20f, 0.02f, 0.008f), Mat4.translation(V3(side * 0.135f, -0.02f, 0f)))
        }
        addMesh(Geo.sphere(0.125f, 18, 9), Mat4.translation(V3(0f, -0.04f, 0.02f)))
        addMesh(Geo.cylinder(0.085f, 0.16f, 22, bevel = 0.012f).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, -0.04f, 0.15f)))
        addMesh(Geo.torus(0.088f, 0.016f, 22, 5), Mat4.translation(V3(0f, -0.04f, 0.23f)))
        addMesh(Geo.cylinder(0.062f, 0.03f, 18).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, -0.04f, 0.24f)))
    }.build()
    parts.add(
        Part(
            id = "sky_gimbal", name = "Three-axis gimbal", system = SKY_CAMERA, assembly = S_GIMBAL,
            mesh = gimbal, material = Materials.titanium,
            origin = V3(0f, HOVER_Y - 0.16f, 0.40f), parent = "sky_body",
            animate = { ctx -> Mat4.rotateX(-10f * ctx.control - 2f) },
            explode = V3(0f, -0.9f, 0.4f),
        )
    )
    parts.add(
        Part(
            id = "sky_lens", name = "Gimbal lens", system = SKY_CAMERA, assembly = S_GIMBAL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.055f, 0.05f, 18).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, -0.04f, 0.26f)))
                addMesh(Geo.torus(0.052f, 0.012f, 18, 5), Mat4.translation(V3(0f, -0.04f, 0.29f)))
            }.build(),
            material = Materials.glassDark,
            parent = "sky_gimbal",
            explode = V3(0f, -0.9f, 0.4f),
        )
    )

    val pack = MeshBuilder().apply {
        addMesh(Geo.slab(0.52f, 0.34f, 0.13f, 0.05f, 0.018f, 2), Mat4.translation(V3(0f, 1.76f, -0.16f)))
        addMesh(Geo.slab(0.44f, 0.26f, 0.03f, 0.03f, 0.01f), Mat4.translation(V3(0f, 1.83f, -0.16f)))
        addMesh(Geo.box(0.30f, 0.014f, 0.02f), Mat4.translation(V3(0f, 1.70f, -0.33f)))
        // Flight controller and GPS puck.
        addMesh(Geo.slab(0.30f, 0.22f, 0.05f, 0.03f, 0.01f), Mat4.translation(V3(0f, 1.72f, 0.16f)))
        addMesh(Geo.cylinder(0.085f, 0.045f, 22, bevel = 0.012f), Mat4.translation(V3(0f, 1.90f, -0.02f)))
        for (i in 0 until 3) {
            addMesh(Geo.box(0.04f, 0.012f, 0.04f), Mat4.translation(V3(-0.10f + i * 0.10f, 1.752f, 0.16f)))
        }
    }.build()
    parts.add(
        Part(
            id = "sky_pack", name = "Battery and flight controller", system = SKY_ENERGY, assembly = S_PACK,
            mesh = pack, material = Materials.paintSlate, parent = "sky_body",
            explode = V3(0f, 1.3f, -0.3f),
        )
    )
    parts.add(
        Part(
            id = "sky_pack_light", name = "Status indicators", system = SKY_ENERGY, assembly = S_PACK,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.30f, 0.012f, 0.02f), Mat4.translation(V3(0f, 1.7005f, -0.34f)))
                for (i in 0 until 3) {
                    addMesh(Geo.box(0.04f, 0.012f, 0.04f), Mat4.translation(V3(-0.10f + i * 0.10f, 1.754f, 0.16f)))
                }
            }.build(),
            material = Materials.emitCyan, parent = "sky_body",
            explode = V3(0f, 1.3f, -0.3f),
        )
    )

    parts.add(
        Part(
            id = "sky_skids", name = "Landing skids", system = SKY_FRAME, assembly = S_GEAR,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    val top = V3(side * 0.34f, HOVER_Y - 0.10f, 0f)
                    val bottom = V3(side * 0.46f, 0.42f, 0f)
                    addMesh(Geo.cylinderBetween(top, bottom, 0.032f, 8))
                    addMesh(Geo.cylinderBetween(V3(side * 0.46f, 0.42f, -0.62f), V3(side * 0.46f, 0.42f, 0.62f), 0.035f, 8))
                    addMesh(Geo.sphere(0.05f, 12, 6), Mat4.translation(V3(side * 0.46f, 0.42f, -0.62f)))
                    addMesh(Geo.sphere(0.05f, 12, 6), Mat4.translation(V3(side * 0.46f, 0.42f, 0.62f)))
                    addMesh(Geo.cylinderBetween(V3(side * 0.34f, HOVER_Y - 0.10f, -0.30f), V3(side * 0.46f, 0.42f, -0.44f), 0.024f, 7))
                    addMesh(Geo.cylinderBetween(V3(side * 0.34f, HOVER_Y - 0.10f, 0.30f), V3(side * 0.46f, 0.42f, 0.44f), 0.024f, 7))
                }
            }.build(),
            material = Materials.rubber, parent = "sky_body",
            explode = V3(0f, -1.0f, 0f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "props", "Folding propellers",
            "Two blades per motor, twisted so the outer half works harder. The tips are orange so the spinning disc stays visible against the sky.",
            "sky_rotor_1", V3(-0.34f, 0.03f, 0f),
            CameraPose(30f, 22f, 1.24f), readoutLabel = "PROPELLER",
        ),
        Hotspot(
            "motors", "Brushless motors",
            "Each motor is a three-phase outrunner. Electromagnets in the stator chase permanent magnets in the bell, and the only wear part is a pair of bearings.",
            "sky_motor_1", V3(0f, 0.1f, 0.06f),
            CameraPose(44f, 26f, 1.24f), readoutLabel = "DRIVE",
        ),
        Hotspot(
            "arms", "Carbon arms",
            "Four booms carry the motors away from the body. Longer arms mean a bigger disc area and a smoother ride, but a heavier frame.",
            "sky_body", V3(0.72f, HOVER_Y + 0.02f, 0.72f),
            CameraPose(52f, 24f, 1.1f), readoutLabel = "AIRFRAME",
        ),
        Hotspot(
            "gimbal", "Three-axis gimbal",
            "The camera floats on three brushless motors. The gimbal moves opposite to the airframe, so the horizon stays level even in a hard turn.",
            "sky_gimbal", V3(0f, -0.05f, 0.22f),
            CameraPose(24f, 8f, 1.4f), readoutLabel = "CAMERA",
        ),
        Hotspot(
            "pack", "Battery and brain",
            "A four-cell pack feeds the ESCs while the flight controller reads the gyros a thousand times a second. The GPS puck sits above the interference.",
            "sky_pack", V3(0f, 1.84f, 0.18f),
            CameraPose(38f, 34f, 1.16f), readoutLabel = "AVIONICS",
        ),
        Hotspot(
            "skids", "Landing skids",
            "Springy skids absorb a firm landing and keep the gimbal clear of the ground. The rubber feet stop the frame sliding on a smooth surface.",
            "sky_skids", V3(0.46f, 0.42f, 0.62f),
            CameraPose(50f, 14f, 1.2f), readoutLabel = "GEAR",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Lift",
            "Four rotors push air down; the reaction pushes the frame up. Hover happens when thrust equals weight, not when the sticks are centred.",
            SKY_POWER, CameraPose(34f, 20f, 1.05f), 0.35f, 0.7f,
        ),
        GuideStep(
            "Spin the discs",
            "The controller changes each motor's speed by a few percent. Faster rear motors pitch the nose down, and the craft accelerates forward.",
            SKY_POWER, CameraPose(30f, 24f, 1.14f), 0.55f, 0.95f,
        ),
        GuideStep(
            "Hold the gaze",
            "The gimbal cancels the airframe's motion. Its outer arm handles yaw, the middle handles roll, and the inner motor pitches the camera.",
            SKY_CAMERA, CameraPose(22f, 6f, 1.4f), 0.15f, 0.9f,
        ),
        GuideStep(
            "Stay awake",
            "Gyros, accelerometers and a barometer feed the flight controller. GPS adds position hold, so the craft can hover hands-off in a breeze.",
            SKY_ENERGY, CameraPose(40f, 32f, 1.16f), 0.1f, 0.8f,
        ),
        GuideStep(
            "Land softly",
            "Skids and rubber feet take the last centimetre of the descent. The gimbal parks itself before touchdown so the lens never touches the ground.",
            SKY_FRAME, CameraPose(48f, 14f, 1.2f), 0f, 0.6f,
        ),
        GuideStep(
            "The assembly",
            "Explode the craft to separate props, motors, arms, pack and gimbal. The arms fold, so the whole drone fits in a backpack.",
            null, CameraPose(38f, 26f, 0.98f), 1f, 0.8f,
        ),
    )

    return ObjectSpec(
        id = "quadcopter",
        index = 9,
        title = "SKYFOLD Q4",
        designation = "Q4 / FOLDING QUADCOPTER",
        category = "Aerial",
        categoryShort = "AIR",
        tagline = "Four discs holding a camera still.",
        summary = "A folding camera quadcopter: four brushless motors on carbon arms, folding propellers, three-axis gimbal, battery pack and flight controller.",
        details = listOf(
            "SKYFOLD Q4 is a camera drone built around a stabilised payload. The airframe is allowed to tilt and shake because the gimbal isolates the lens from all of it.",
            "Each arm folds against the body for transport. The motors are the only moving parts besides the gimbal, and the propellers reverse direction to brake in flight.",
        ),
        specs = listOf(
            SpecRow("Take-off mass", "895 g"),
            SpecRow("Diagonal", "350 mm"),
            SpecRow("Flight time", "34 min"),
            SpecRow("Camera", "1\" / 5.4K"),
            SpecRow("Top speed", "21 m/s"),
        ),
        tags = listOf("aerial", "drone", "quadcopter", "gimbal", "uav", "camera", "rotor"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "THROTTLE",
            lowLabel = "HOVER",
            highLabel = "SPORT",
            defaultValue = 0.45f,
            hint = "Rotor speed, airframe tilt and climb",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(36f, 24f, 1.18f),
        thumbnailPose = CameraPose(38f, 22f, 1.2f),
        thumbnailTime = 3.1f,
        thumbnailControl = 0.72f,
        readouts = { ctx ->
            val altitude = 6f + 114f * ctx.control
            val speed = 14f * ctx.control
            val battery = 98f - 44f * ctx.control
            listOf(
                Readout("ALTITUDE", String.format("%.0f", altitude), "m", accent = true),
                Readout("SPEED", String.format("%.1f", speed), "m/s"),
                Readout("BATTERY", String.format("%.0f", battery), "%", warn = battery < 25f),
            )
        },
        effects = { ctx ->
            val wash = 0.35f + 0.65f * ctx.control
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.72f, 0xFF6EF2DF.toInt(), 0f, 0.24f),
                SceneEffect.Flow(
                    points = listOf(
                        V3(-1.12f, 1.34f, -1.12f),
                        V3(-1.12f, 0.95f, -1.12f),
                        V3(-1.12f, 0.55f, -1.12f),
                    ),
                    color = 0xFFB8F6EC.toInt(),
                    count = 7,
                    speed = 0.5f * wash,
                    phase = 0.1f,
                    dotRadius = 0.02f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(1.12f, 1.34f, 1.12f),
                        V3(1.12f, 0.95f, 1.12f),
                        V3(1.12f, 0.55f, 1.12f),
                    ),
                    color = 0xFFB8F6EC.toInt(),
                    count = 7,
                    speed = 0.5f * wash,
                    phase = 0.6f,
                    dotRadius = 0.02f,
                ),
                SceneEffect.Glow(V3(0f, HOVER_Y + 0.2f, 0f), 0.5f, 0xFF6EF2DF.toInt(), 0.12f),
            )
        },
    )
}
