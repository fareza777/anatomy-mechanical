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

private const val ARM_FRAME = "FRAME"
private const val ARM_DRIVE = "DRIVE"
private const val ARM_SHOULDER = "SHOULDER"
private const val ARM_LINK = "ARM"
private const val ARM_WRIST = "WRIST"
private const val ARM_GRIPPER = "GRIPPER"

private const val A_BASE = "BASE"
private const val A_TURRET = "TURRET"
private const val A_UPPER = "UPPER ARM"
private const val A_FOREARM = "FOREARM"
private const val A_END = "END EFFECTOR"

private fun armBlade(span: Float, rootChord: Float, tipChord: Float, rootThickness: Float, tipThickness: Float): Mesh =
    Geo.roundedBlade(
        span = span,
        rootChord = rootChord,
        tipChord = tipChord,
        rootThickness = rootThickness,
        tipThickness = tipThickness,
        twistDegrees = 0f,
        sweep = 0f,
        sections = 4,
        perimeter = 8,
    )

fun robotArm(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "axis",
            width = 3.2f,
            depth = 2.9f,
            top = 0.3f,
            system = ARM_FRAME,
            assembly = A_BASE,
            accentMaterial = Materials.paintOrange,
        )
    )

    val pedestal = MeshBuilder().apply {
        addMesh(Geo.slab(1.95f, 1.75f, 0.52f, 0.2f, 0.06f, 3), Mat4.translation(V3(0f, 0.56f, 0f)))
        addMesh(Geo.cylinder(1.02f, 0.16f, 36, bevel = 0.04f), Mat4.translation(V3(0f, 0.86f, 0f)))
        addMesh(Geo.torus(0.93f, 0.045f, 36, 6), Mat4.translation(V3(0f, 0.92f, 0f)))
        addMesh(Geo.cylinder(0.62f, 0.05f, 32), Mat4.translation(V3(0f, 0.95f, 0f)))
    }.build()
    parts.add(
        Part(
            id = "arm_pedestal", name = "Pedestal casting", system = ARM_FRAME, assembly = A_BASE,
            mesh = pedestal, material = Materials.darkSteel,
        )
    )
    parts.add(
        Part(
            id = "arm_pedestal_bolts", name = "Anchor bolts", system = ARM_FRAME, assembly = A_BASE,
            mesh = Geo.boltSet(
                listOf(
                    V3(-0.72f, 0.83f, -0.62f), V3(0.72f, 0.83f, -0.62f),
                    V3(-0.72f, 0.83f, 0.62f), V3(0.72f, 0.83f, 0.62f),
                ),
                0.075f, 0.05f,
            ),
            material = Materials.titanium,
        )
    )

    val turret = MeshBuilder().apply {
        addMesh(Geo.cylinder(0.82f, 0.52f, 36, bevel = 0.07f), Mat4.translation(V3(0f, 0.2f, 0f)))
        addMesh(Geo.hollowCylinder(0.8f, 0.7f, 0.1f, 36), Mat4.translation(V3(0f, 0.48f, 0f)))
        addMesh(Geo.cylinder(0.7f, 0.08f, 32), Mat4.translation(V3(0f, 0.54f, 0f)))
        addMesh(Geo.slab(0.18f, 0.76f, 0.92f, 0.06f, 0.025f), Mat4.translation(V3(-0.57f, 0.44f, 0f)))
        addMesh(Geo.slab(0.18f, 0.76f, 0.92f, 0.06f, 0.025f), Mat4.translation(V3(0.57f, 0.44f, 0f)))
        addMesh(machineCylinderX(0f, 0.35f, 1.3f, 30, 0.03f), Mat4.translation(V3(0f, 0.82f, 0f)))
        addMesh(machineTorusX(0.62f, 0.26f, 0.035f), Mat4.translation(V3(0f, 0.82f, 0f)))
        addMesh(machineTorusX(-0.62f, 0.26f, 0.035f), Mat4.translation(V3(0f, 0.82f, 0f)))
        for (i in 0 until 4) {
            addMesh(
                Geo.slab(0.24f, 0.03f, 0.08f, 0.008f, 0.004f),
                Mat4.translation(V3(-0.38f + i * 0.25f, 0.28f, 0.8f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "arm_turret", name = "Yaw turret", system = ARM_DRIVE, assembly = A_TURRET,
            mesh = turret, material = Materials.paintCream, origin = V3(0f, 0.94f, 0f),
            animate = { ctx ->
                val amp = 0.35f + 0.65f * ctx.control
                Mat4.rotateY(14f * sin(ctx.machineTime * 0.34f) * amp)
            },
            explode = V3(0f, 0.7f, 0f),
        )
    )
    parts.add(
        Part(
            id = "arm_turret_bolts", name = "Turret fasteners", system = ARM_DRIVE, assembly = A_TURRET,
            mesh = Geo.boltSet(
                listOf(
                    V3(-0.58f, 0.58f, -0.55f), V3(0.58f, 0.58f, -0.55f),
                    V3(-0.58f, 0.58f, 0.55f), V3(0.58f, 0.58f, 0.55f),
                ),
                0.052f, 0.035f,
            ),
            material = Materials.titanium, origin = V3(0f, 0.94f, 0f),
            animate = { ctx ->
                val amp = 0.35f + 0.65f * ctx.control
                Mat4.rotateY(14f * sin(ctx.machineTime * 0.34f) * amp)
            },
            explode = V3(0f, 0.7f, 0f),
        )
    )

    val shoulder = MeshBuilder().apply {
        addMesh(machineCylinderX(0f, 0.34f, 1.1f, 30, 0.025f))
        addMesh(machineCylinderX(0.58f, 0.25f, 0.13f, 24), Mat4.identity())
        addMesh(machineCylinderX(-0.58f, 0.25f, 0.13f, 24), Mat4.identity())
        addMesh(machineTorusX(0.64f, 0.19f, 0.025f), Mat4.identity())
        addMesh(machineTorusX(-0.64f, 0.19f, 0.025f), Mat4.identity())
        addMesh(armBlade(1.86f, 0.68f, 0.46f, 0.44f, 0.28f), Mat4.translation(V3(0f, 0.22f, 0f)))
        addMesh(
            Geo.slab(0.2f, 0.018f, 1.42f, 0.004f, 0f),
            Mat4.translation(V3(0f, 1.12f, 0.35f)),
        )
        addMesh(
            Geo.slab(0.3f, 0.06f, 0.55f, 0.02f, 0.008f),
            Mat4.translation(V3(0f, 0.35f, -0.3f)),
        )
        addMesh(
            Geo.tube(
                listOf(V3(0.22f, 0.2f, -0.34f), V3(0.3f, 0.8f, -0.42f), V3(0.18f, 1.5f, -0.34f), V3(0.05f, 2.02f, -0.25f)),
                0.035f, 7, subdivide = 4,
            ),
            Mat4.identity(),
        )
    }.build()
    val shoulderAngle = { ctx: AnimContext ->
        val amp = 0.35f + 0.65f * ctx.control
        -48f + 18f * sin(ctx.machineTime * 0.45f) * amp
    }
    parts.add(
        Part(
            id = "arm_shoulder", name = "Shoulder pitch link", system = ARM_SHOULDER, assembly = A_UPPER,
            mesh = shoulder, material = Materials.paintOrange, origin = V3(0f, 0.82f, 0f),
            parent = "arm_turret", doubleSided = true,
            animate = { ctx -> Mat4.rotateZ(shoulderAngle(ctx)) },
            explode = V3(0f, 1.0f, 0.4f),
        )
    )
    parts.add(
        Part(
            id = "arm_shoulder_bolts", name = "Shoulder bearing ring", system = ARM_SHOULDER, assembly = A_UPPER,
            mesh = Geo.merge(
                boltCircleX(8, 0.23f, 0.705f, 0.032f, 0.05f),
                boltCircleX(8, 0.23f, -0.705f, 0.032f, 0.05f),
            ),
            material = Materials.brass, origin = V3(0f, 0.82f, 0f),
            parent = "arm_turret",
            animate = { ctx -> Mat4.rotateZ(shoulderAngle(ctx)) },
            explode = V3(0f, 1.0f, 0.4f),
        )
    )

    val elbowAngle = { ctx: AnimContext ->
        val amp = 0.35f + 0.65f * ctx.control
        66f + 26f * sin(ctx.machineTime * 0.45f + 1.2f) * amp
    }
    val elbow = MeshBuilder().apply {
        addMesh(machineCylinderX(0f, 0.29f, 0.86f, 28, 0.025f))
        addMesh(machineTorusX(0.44f, 0.2f, 0.025f), Mat4.identity())
        addMesh(machineTorusX(-0.44f, 0.2f, 0.025f), Mat4.identity())
        addMesh(armBlade(1.55f, 0.5f, 0.33f, 0.34f, 0.22f), Mat4.translation(V3(0f, 0.18f, 0f)))
        addMesh(
            Geo.slab(0.16f, 0.012f, 1.14f, 0.004f, 0f),
            Mat4.translation(V3(0f, 0.98f, 0.26f)),
        )
        addMesh(
            Geo.tube(
                listOf(V3(-0.16f, 0.12f, -0.24f), V3(-0.24f, 0.7f, -0.3f), V3(-0.14f, 1.36f, -0.24f), V3(0f, 1.68f, -0.16f)),
                0.028f, 7, subdivide = 4,
            ),
            Mat4.identity(),
        )
        addMesh(
            Geo.slab(0.22f, 0.12f, 0.3f, 0.03f, 0.01f),
            Mat4.translation(V3(0f, 1.65f, 0.16f)),
        )
    }.build()
    parts.add(
        Part(
            id = "arm_elbow", name = "Elbow and forearm", system = ARM_LINK, assembly = A_FOREARM,
            mesh = elbow, material = Materials.paintCream, origin = V3(0f, 2.12f, 0f),
            parent = "arm_shoulder", doubleSided = true,
            animate = { ctx -> Mat4.rotateZ(elbowAngle(ctx)) },
            explode = V3(0f, 0.2f, 1.0f),
        )
    )
    parts.add(
        Part(
            id = "arm_elbow_bolts", name = "Elbow bearing ring", system = ARM_LINK, assembly = A_FOREARM,
            mesh = boltCircleX(8, 0.19f, 0.47f, 0.028f, 0.04f),
            material = Materials.brass, origin = V3(0f, 2.12f, 0f),
            parent = "arm_shoulder",
            animate = { ctx -> Mat4.rotateZ(elbowAngle(ctx)) },
            explode = V3(0f, 0.2f, 1.0f),
        )
    )

    val wristAngle = { ctx: AnimContext ->
        val amp = 0.35f + 0.65f * ctx.control
        -24f + 20f * sin(ctx.machineTime * 0.62f + 2.4f) * amp
    }
    val wrist = MeshBuilder().apply {
        addMesh(Geo.cylinder(0.25f, 0.34f, 28, bevel = 0.03f), Mat4.translation(V3(0f, 0.17f, 0f)))
        addMesh(Geo.torus(0.21f, 0.032f, 28, 6), Mat4.translation(V3(0f, 0.32f, 0f)))
        addMesh(Geo.slab(0.64f, 0.48f, 0.28f, 0.07f, 0.02f), Mat4.translation(V3(0f, 0.48f, 0f)))
        addMesh(Geo.cylinder(0.1f, 0.04f, 20), Mat4.translation(V3(0f, 0.63f, 0.14f)))
        addMesh(
            Geo.cylinderBetween(V3(-0.16f, 0.48f, -0.16f), V3(-0.1f, 0.38f, -0.24f), 0.022f, 7),
            Mat4.identity(),
        )
        addMesh(
            Geo.cylinderBetween(V3(0.16f, 0.48f, -0.16f), V3(0.1f, 0.38f, -0.24f), 0.022f, 7),
            Mat4.identity(),
        )
    }.build()
    parts.add(
        Part(
            id = "arm_wrist", name = "Wrist and tool flange", system = ARM_WRIST, assembly = A_END,
            mesh = wrist, material = Materials.paintOrange, origin = V3(0f, 1.75f, 0f),
            parent = "arm_elbow", doubleSided = true,
            animate = { ctx -> Mat4.rotateZ(wristAngle(ctx)) },
            explode = V3(0f, 0.3f, 1.4f),
        )
    )
    parts.add(
        Part(
            id = "arm_sensor", name = "Tool sensor", system = ARM_WRIST, assembly = A_END,
            mesh = Geo.cylinder(0.055f, 0.03f, 18), material = Materials.emitCyan,
            origin = V3(0f, 0.605f, 0.12f), parent = "arm_wrist",
            explode = V3(0f, 0.3f, 1.4f),
        )
    )

    fun finger(id: String, side: Float): Part {
        val mesh = MeshBuilder().apply {
            addMesh(Geo.slab(0.15f, 0.22f, 0.62f, 0.04f, 0.014f), Mat4.translation(V3(side * 0.23f, 0.29f, 0f)))
            addMesh(Geo.slab(0.17f, 0.25f, 0.2f, 0.03f, 0.012f), Mat4.translation(V3(side * 0.23f, 0.62f, -0.07f)))
            addMesh(Geo.box(0.145f, 0.1f, 0.16f), Mat4.translation(V3(side * 0.23f, 0.51f, -0.12f)))
        }.build()
        return Part(
            id = id, name = if (side < 0f) "Left gripper jaw" else "Right gripper jaw",
            system = ARM_GRIPPER, assembly = A_END,
            // The jaw mesh is already authored in the wrist's local frame, so the part
            // must not add another 0.53 m offset or it floats off the tool flange.
            mesh = mesh, material = Materials.titanium, origin = V3(0f, 0f, 0f),
            parent = "arm_wrist",
            animate = { ctx ->
                val amp = 0.4f + 0.6f * ctx.control
                val open = 0.02f + 0.09f * (0.5f + 0.5f * sin(ctx.machineTime * 0.9f)) * amp
                Mat4.translation(V3(side * open, 0f, 0f))
            },
            explode = V3(side * 0.3f, 0.3f, 1.5f),
        )
    }

    parts.add(finger("arm_finger_left", -1f))
    parts.add(finger("arm_finger_right", 1f))

    val hotspots = listOf(
        Hotspot(
            "base", "Cast pedestal",
            "The pedestal carries the arm's overturning moment into the floor. Eight anchor bolts tie the casting to a steel foundation plate, isolating the drives from the slab.",
            "arm_pedestal", V3(0f, 0.5f, 0.72f),
            CameraPose(46f, 20f, 1.2f), readoutLabel = "FOUNDATION",
        ),
        Hotspot(
            "yaw", "Yaw drive",
            "A servo and crossed-roller bearing turn the whole turret. The ring gear is preloaded so the arm can sweep a full circle without backlash.",
            "arm_turret", V3(0f, 0.45f, 0.76f),
            CameraPose(32f, 28f, 1.12f), readoutLabel = "AXIS 1 / YAW",
        ),
        Hotspot(
            "shoulder", "Shoulder pitch",
            "The largest joint uses a dual-motor, zero-backlash reduction. Its bearing ring is visible through the cheek plates; the arm's own weight is balanced by a gas spring inside the casting.",
            "arm_shoulder", V3(0f, 0.05f, 0.42f),
            CameraPose(56f, 14f, 1.05f), readoutLabel = "AXIS 2 / PITCH",
        ),
        Hotspot(
            "upper", "Hollow upper link",
            "The cream shell is a thin carbon-aluminium sandwich around a machined spine. Cables run inside the link, so the arm can turn without snagging a harness.",
            "arm_shoulder", V3(0.12f, 1.18f, 0.3f),
            CameraPose(42f, 24f, 1.12f), readoutLabel = "UPPER ARM",
        ),
        Hotspot(
            "elbow", "Elbow and forearm",
            "The elbow actuator sits inside the forearm root. A brass bearing ring preloads the joint, while the slim forearm keeps inertia low for fast pick-and-place cycles.",
            "arm_elbow", V3(0f, 0.15f, 0.3f),
            CameraPose(30f, 22f, 1.18f), readoutLabel = "AXIS 4 / ELBOW",
        ),
        Hotspot(
            "gripper", "Servo gripper",
            "Two independent jaws close on the workpiece. The cyan sensor confirms a part is present before the arm moves; the rubber tips spread the clamping load.",
            "arm_wrist", V3(0f, 0.55f, 0.22f),
            CameraPose(24f, 18f, 1.45f), readoutLabel = "END EFFECTOR",
        ),
    )

    val steps = listOf(
        GuideStep(
            "A stable root",
            "The cast pedestal is the reference for every axis. Load paths run from the yaw bearing through the ribs and down into the anchor bolts.",
            ARM_FRAME, CameraPose(48f, 18f, 1.06f), 0.25f, 0.7f,
        ),
        GuideStep(
            "Yaw and shoulder",
            "The turret swings the whole arm, while the shoulder sets its reach. These two axes carry the largest loads and use the stiffest bearings.",
            ARM_SHOULDER, CameraPose(36f, 26f, 1.12f), 0f, 0.8f,
        ),
        GuideStep(
            "The upper link",
            "The hollow link hides its cable harness. Its tapered section keeps bending stiffness high where the moment is greatest, then thins toward the elbow.",
            ARM_LINK, CameraPose(56f, 16f, 1.08f), 0.35f, 0.9f,
        ),
        GuideStep(
            "Elbow and wrist",
            "The forearm and wrist make the fine moves. Rotating the elbow lifts the tool, while the wrist orients the gripper in three more axes.",
            ARM_WRIST, CameraPose(28f, 20f, 1.2f), 0f, 1f,
        ),
        GuideStep(
            "Take hold",
            "The gripper closes until the sensor sees contact. Explode the end effector to inspect the jaws, tool flange and sensor package.",
            ARM_GRIPPER, CameraPose(22f, 16f, 1.35f), 1f, 0.65f,
        ),
        GuideStep(
            "The whole envelope",
            "Every joint is an independent module. Explode the arm to see how the yaw ring, shoulder bearing, elbow cartridge and wrist flange stack.",
            null, CameraPose(40f, 24f, 0.98f), 1f, 1f,
        ),
    )

    return ObjectSpec(
        id = "robot_arm",
        index = 2,
        title = "AXIS-6",
        designation = "AX-6 / ARTICULATED ROBOT",
        category = "Robotics",
        categoryShort = "ROBO",
        tagline = "Six axes, one quiet reach.",
        summary = "A six-axis industrial arm sectioned at every joint: yaw ring, shoulder bearing, elbow cartridge, wrist flange and servo gripper.",
        details = listOf(
            "AXIS-6 is a medium-payload articulated arm. Each joint uses a sealed servo actuator and a preloaded reduction, so the arm can move slowly through a full envelope without visible backlash or vibration.",
            "The cream links are hollow composite shells over a machined internal spine. Cables, brakes and encoder lines run inside the structure; the gripper jaws are independently driven and instrumented.",
        ),
        specs = listOf(
            SpecRow("Payload", "14 kg"),
            SpecRow("Reach", "1.85 m"),
            SpecRow("Axes", "6 rotary"),
            SpecRow("Repeatability", "±0.05 mm"),
            SpecRow("Mass", "168 kg"),
        ),
        tags = listOf("robotics", "robot arm", "automation", "six axis", "gripper", "factory"),
        accent = 0xFFF2B25C.toInt(),
        control = ControlSpec(
            label = "ENVELOPE",
            lowLabel = "TIGHT",
            highLabel = "FULL",
            defaultValue = 0.62f,
            hint = "Joint amplitude and gripper travel",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(40f, 22f, 1.3f),
        thumbnailPose = CameraPose(46f, 18f, 1.46f),
        thumbnailTime = 6.8f,
        thumbnailControl = 0.86f,
        readouts = { ctx ->
            val amp = 0.35f + 0.65f * ctx.control
            val reach = 1.32f + 0.5f * amp
            val joint = kotlin.math.abs(-48f + 18f * sin(ctx.machineTime * 0.45f) * amp)
            val open = 12f + 10f * (0.5f + 0.5f * sin(ctx.machineTime * 0.9f)) * amp
            listOf(
                Readout("REACH", String.format("%.2f", reach), "m", accent = true),
                Readout("SHOULDER", "${joint.toInt()}", "°"),
                Readout("GRIP", "${open.toInt()}", "mm"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Ring(V3(0f, 0.34f, 0f), 1.62f, 0xFFF2B25C.toInt(), 0f, 0.3f),
                SceneEffect.Glow(V3(0f, 0.86f, 0f), 0.72f, 0xFFF2B25C.toInt(), 0.16f),
                SceneEffect.Spark(V3(0f, 4.36f, 0.12f), 0xFF6EF2DF.toInt(), 0.045f, 0.5f),
            )
        },
    )
}
