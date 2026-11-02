package studio.anatomy.app.objects

import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V2
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
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val DELTA_FRAME = "FRAME"
private const val DELTA_DRIVE = "DRIVES"
private const val DELTA_LINK = "LINKAGE"
private const val DELTA_TOOL = "EFFECTOR"

private const val D_FRAME = "PORTAL"
private const val D_MOTOR = "SERVO PODS"
private const val D_ARM = "UPPER ARMS"
private const val D_ROD = "PARALLELOGRAM"
private const val D_PLATFORM = "PLATFORM"
private const val D_TOOL = "TOOL"

private const val PIVOT_Y = 3.12f
private const val PIVOT_R = 0.95f
private const val UPPER_LEN = 0.90f
private const val FORE_LEN = 1.85f
private const val PLATFORM_R = 0.30f
private const val REST_Y = 1.52f
private const val ROD_OFFSET = 0.085f

private val ARM_ANGLES = floatArrayOf(0f, 120f, 240f)

private data class DeltaPose(
    val radial: V3,
    val tangent: V3,
    val pivot: V3,
    val elbow: V3,
    val target: V3,
    val restElbow: V3,
    val restTarget: V3,
    val alpha: Float,
)

private fun platformCenter(ctx: AnimContext): V3 {
    val amp = 0.35f + 0.65f * ctx.control
    val clock = ctx.machineTime * (0.55f + 0.38f * amp)
    val orbit = 0.14f * amp
    return V3(
        orbit * cos(clock * 0.82f),
        REST_Y + 0.16f * sin(clock * 1.12f) * amp,
        orbit * sin(clock * 0.82f),
    )
}

private fun radialFor(index: Int): V3 {
    val angle = Math.toRadians(ARM_ANGLES[index].toDouble())
    return V3(sin(angle).toFloat(), 0f, cos(angle).toFloat())
}

private fun solveElbow(pivot: V3, target: V3, radial: V3, tangent: V3): V3 {
    val delta = target - pivot
    val raw = delta.length()
    if (raw < 1e-4f) return pivot + radial * UPPER_LEN
    val dir = delta * (1f / raw)
    val distance = raw.coerceIn(0.35f, UPPER_LEN + FORE_LEN - 0.002f)
    val along = (UPPER_LEN * UPPER_LEN - FORE_LEN * FORE_LEN + distance * distance) / (2f * distance)
    val height = sqrt(max(0f, UPPER_LEN * UPPER_LEN - along * along))
    val perpendicular = tangent.cross(dir).normalized()
    val first = pivot + dir * along + perpendicular * height
    val second = pivot + dir * along - perpendicular * height
    // Keep the elbow outboard: it is the pose that reads as a delta arm rather than a folded one.
    return if (first.dot(radial) >= second.dot(radial)) first else second
}

private fun poseFor(center: V3, index: Int): DeltaPose {
    val radial = radialFor(index)
    val tangent = V3(radial.z, 0f, -radial.x)
    val pivot = radial * PIVOT_R + V3(0f, PIVOT_Y, 0f)
    val restCenter = V3(0f, REST_Y, 0f)
    val target = center + radial * PLATFORM_R + V3(0f, 0.055f, 0f)
    val restTarget = restCenter + radial * PLATFORM_R + V3(0f, 0.055f, 0f)
    val elbow = solveElbow(pivot, target, radial, tangent)
    val restElbow = solveElbow(pivot, restTarget, radial, tangent)
    val arm = elbow - pivot
    val alpha = atan2(-arm.y, arm.dot(radial))
    return DeltaPose(radial, tangent, pivot, elbow, target, restElbow, restTarget, alpha)
}

private fun rotationBetween(from: V3, to: V3): Mat4 {
    val a = from.normalized()
    val b = to.normalized()
    val dot = a.dot(b).coerceIn(-1f, 1f)
    val axis = a.cross(b)
    return when {
        axis.lengthSq() < 1e-7f -> if (dot > 0f) Mat4.identity() else Mat4.rotate(V3.X, 180f)
        else -> Mat4.rotate(axis, Math.toDegrees(acos(dot.toDouble())).toFloat())
    }
}

/** Rotates +Y onto an arbitrary direction, used to lay cylinders along a tangent. */
private fun alignY(direction: V3): Mat4 {
    val dir = direction.normalized()
    val dot = V3.Y.dot(dir).coerceIn(-1f, 1f)
    val axis = V3.Y.cross(dir)
    return when {
        axis.lengthSq() < 1e-7f -> if (dot > 0f) Mat4.identity() else Mat4.rotateX(180f)
        else -> Mat4.rotate(axis, Math.toDegrees(acos(dot.toDouble())).toFloat())
    }
}

private fun motorPod(radial: V3, tangent: V3): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.cylinder(0.225f, 0.42f, 28, bevel = 0.025f),
        alignY(tangent),
    )
    addMesh(
        Geo.torus(0.215f, 0.028f, 26, 6),
        Mat4.translation(tangent * 0.19f).multiply(alignY(tangent)),
    )
    addMesh(
        Geo.torus(0.215f, 0.028f, 26, 6),
        Mat4.translation(tangent * -0.19f).multiply(alignY(tangent)),
    )
    addMesh(
        Geo.cylinder(0.15f, 0.05f, 22),
        Mat4.translation(tangent * 0.225f).multiply(alignY(tangent)),
    )
    // Cable gland pointing back toward the portal.
    addMesh(
        Geo.cylinder(0.055f, 0.16f, 12),
        Mat4.translation(V3(0f, 0.16f, 0f) - radial * 0.16f).multiply(alignY(V3.Y)),
    )
}.build()

private fun upperArmMesh(index: Int): Mesh {
    val angle = ARM_ANGLES[index]
    val spin = Mat4.rotateY(angle)
    return MeshBuilder().apply {
        // Local frame: +Z is radial, +X is tangential. The whole mesh is pre-turned to its station.
        addMesh(
            Geo.cylinder(0.15f, 0.44f, 22, bevel = 0.02f).transformed(Mat4.rotateZ(90f)),
            spin,
        )
        addMesh(
            Geo.slab(0.10f, 0.80f, 0.17f, 0.03f, 0.012f),
            spin.multiply(Mat4.translation(V3(-0.16f, 0f, 0.44f))),
        )
        addMesh(
            Geo.slab(0.10f, 0.80f, 0.17f, 0.03f, 0.012f),
            spin.multiply(Mat4.translation(V3(0.16f, 0f, 0.44f))),
        )
        addMesh(
            Geo.slab(0.34f, 0.07f, 0.11f, 0.02f, 0.008f),
            spin.multiply(Mat4.translation(V3(0f, 0.07f, 0.62f))),
        )
        addMesh(
            Geo.slab(0.30f, 0.05f, 0.08f, 0.02f, 0.006f),
            spin.multiply(Mat4.translation(V3(0f, -0.065f, 0.78f))),
        )
        addMesh(
            Geo.cylinder(0.125f, 0.46f, 20, bevel = 0.015f).transformed(Mat4.rotateZ(90f)),
            spin.multiply(Mat4.translation(V3(0f, 0f, 0.86f))),
        )
        addMesh(
            Geo.torus(0.12f, 0.018f, 20, 5).transformed(Mat4.rotateZ(90f)),
            spin.multiply(Mat4.translation(V3(0.235f, 0f, 0.86f))),
        )
    }.build()
}

private fun forearmMesh(pose: DeltaPose): Mesh = MeshBuilder().apply {
    val start = V3.ZERO
    val end = pose.restTarget - pose.restElbow
    listOf(-ROD_OFFSET, ROD_OFFSET).forEach { offset ->
        val side = pose.tangent * offset
        addMesh(Geo.sphere(0.052f, 12, 7), Mat4.translation(side))
        addMesh(Geo.sphere(0.052f, 12, 7), Mat4.translation(end + side))
        addMesh(Geo.cylinderBetween(start + side, end + side, 0.026f, 8))
        addMesh(
            Geo.cylinder(0.033f, 0.12f, 12, bevel = 0.006f),
            Mat4.translation(end * 0.55f + side).multiply(alignY(end.normalized())),
        )
    }
}.build()

private fun platformMesh(): Mesh = MeshBuilder().apply {
    val triangle = listOf(
        V2(-0.26f, -0.15f),
        V2(0.26f, -0.15f),
        V2(0f, 0.30f),
    )
    addMesh(Geo.prism(triangle, 0.075f, 0.018f), Mat4.identity())
    for (i in 0 until 3) {
        val radial = radialFor(i)
        addMesh(
            Geo.cylinder(0.072f, 0.07f, 18, bevel = 0.012f),
            Mat4.translation(radial * 0.28f),
        )
        addMesh(
            Geo.cylinder(0.026f, 0.10f, 10),
            Mat4.translation(radial * 0.28f + V3(0f, 0.055f, 0f)),
        )
    }
    addMesh(
        Geo.torus(0.125f, 0.022f, 22, 6),
        Mat4.translation(V3(0f, 0.055f, 0f)),
    )
    addMesh(Geo.cylinder(0.105f, 0.16f, 22, bevel = 0.015f), Mat4.translation(V3(0f, 0.11f, 0f)))
    // Vacuum tool: spindle, cone and cup.
    addMesh(Geo.taperedCylinder(0.105f, 0.045f, 0.17f, 22), Mat4.translation(V3(0f, -0.04f, 0f)))
    addMesh(Geo.taperedCylinder(0.075f, 0.11f, 0.07f, 22), Mat4.translation(V3(0f, -0.16f, 0f)))
    addMesh(Geo.torus(0.105f, 0.014f, 22, 5), Mat4.translation(V3(0f, -0.195f, 0f)))
}.build()

fun deltaRobot(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "triad",
            width = 3.25f,
            depth = 3.0f,
            top = 0.3f,
            system = DELTA_FRAME,
            assembly = D_FRAME,
            accentMaterial = Materials.emitCyan,
        )
    )

    val frame = MeshBuilder().apply {
        for (i in 0 until 3) {
            val radial = radialFor(i)
            val foot = radial * 1.44f + V3(0f, 0.32f, 0f)
            val head = radial * PIVOT_R + V3(0f, PIVOT_Y, 0f)
            addMesh(Geo.cylinderBetween(foot, head, 0.075f, 12))
            addMesh(Geo.taperedCylinder(0.16f, 0.10f, 0.10f, 18), Mat4.translation(foot))
            addMesh(Geo.torus(0.09f, 0.02f, 16, 5), Mat4.translation(foot + V3(0f, 0.055f, 0f)))
        }
        for (i in 0 until 3) {
            val next = (i + 1) % 3
            val a = radialFor(i) * PIVOT_R + V3(0f, PIVOT_Y, 0f)
            val b = radialFor(next) * PIVOT_R + V3(0f, PIVOT_Y, 0f)
            addMesh(Geo.cylinderBetween(a, b, 0.062f, 12))
            addMesh(
                Geo.cylinderBetween(
                    radialFor(i) * 1.28f + V3(0f, 1.18f, 0f),
                    radialFor(next) * 1.28f + V3(0f, 1.18f, 0f),
                    0.034f, 8,
                ),
            )
            addMesh(
                Geo.cylinderBetween(
                    radialFor(i) * 1.14f + V3(0f, 2.14f, 0f),
                    radialFor(next) * 1.14f + V3(0f, 2.14f, 0f),
                    0.030f, 8,
                ),
            )
        }
        addMesh(Geo.cylinder(0.19f, 0.13f, 28, bevel = 0.02f), Mat4.translation(V3(0f, PIVOT_Y + 0.02f, 0f)))
        addMesh(Geo.cylinder(0.11f, 0.22f, 20), Mat4.translation(V3(0f, PIVOT_Y - 0.12f, 0f)))
        addMesh(Geo.torus(0.14f, 0.022f, 24, 6), Mat4.translation(V3(0f, PIVOT_Y - 0.08f, 0f)))
    }.build()
    parts.add(
        Part(
            id = "delta_frame", name = "Portal frame", system = DELTA_FRAME, assembly = D_FRAME,
            mesh = frame, material = Materials.graphite,
        )
    )

    for (i in 0 until 3) {
        val radial = radialFor(i)
        val tangent = V3(radial.z, 0f, -radial.x)
        val pivot = radial * PIVOT_R + V3(0f, PIVOT_Y, 0f)
        parts.add(
            Part(
                id = "delta_motor_${i + 1}", name = "Direct-drive servo ${i + 1}",
                system = DELTA_DRIVE, assembly = D_MOTOR,
                mesh = motorPod(radial, tangent), material = Materials.darkSteel,
                origin = pivot,
                explode = V3(radial.x * 0.6f, 0.5f, radial.z * 0.6f),
            )
        )
        parts.add(
            Part(
                id = "delta_pod_ring_${i + 1}", name = "Encoder ring ${i + 1}",
                system = DELTA_DRIVE, assembly = D_MOTOR,
                mesh = MeshBuilder().apply {
                    addMesh(
                        Geo.torus(0.185f, 0.02f, 26, 5),
                        Mat4.translation(tangent * 0.215f).multiply(alignY(tangent)),
                    )
                }.build(),
                material = Materials.brass,
                origin = pivot,
                explode = V3(radial.x * 0.6f, 0.5f, radial.z * 0.6f),
            )
        )
        parts.add(
            Part(
                id = "delta_arm_${i + 1}", name = "Upper arm ${i + 1}",
                system = DELTA_DRIVE, assembly = D_ARM,
                mesh = upperArmMesh(i),
                material = Materials.paintCream,
                origin = pivot,
                animate = { ctx ->
                    val pose = poseFor(platformCenter(ctx), i)
                    Mat4.rotate(pose.tangent, Math.toDegrees(pose.alpha.toDouble()).toFloat())
                },
                explode = V3(radial.x * 0.85f, 0.9f, radial.z * 0.85f),
            )
        )
        val rest = poseFor(V3(0f, REST_Y, 0f), i)
        parts.add(
            Part(
                id = "delta_rod_${i + 1}", name = "Parallelogram rods ${i + 1}",
                system = DELTA_LINK, assembly = D_ROD,
                mesh = forearmMesh(rest),
                material = Materials.titanium,
                origin = rest.restElbow,
                animate = { ctx ->
                    val pose = poseFor(platformCenter(ctx), i)
                    val start = pose.restTarget - pose.restElbow
                    val end = pose.target - pose.elbow
                    Mat4.translation(pose.elbow - pose.restElbow).multiply(rotationBetween(start, end))
                },
                explode = V3(radial.x * 0.9f, 0.15f, radial.z * 0.9f),
            )
        )
    }

    parts.add(
        Part(
            id = "delta_platform", name = "Level platform", system = DELTA_TOOL, assembly = D_PLATFORM,
            mesh = platformMesh(), material = Materials.paintSlate,
            origin = V3(0f, REST_Y, 0f),
            animate = { ctx ->
                Mat4.translation(platformCenter(ctx) - V3(0f, REST_Y, 0f))
            },
            explode = V3(0f, -0.55f, 0f),
        )
    )
    parts.add(
        Part(
            id = "delta_tool_light", name = "Tool beacon", system = DELTA_TOOL, assembly = D_TOOL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.09f, 0.018f, 22, 5), Mat4.translation(V3(0f, -0.245f, 0f)))
            }.build(),
            material = Materials.emitCyan,
            origin = V3(0f, REST_Y, 0f),
            animate = { ctx ->
                Mat4.translation(platformCenter(ctx) - V3(0f, REST_Y, 0f))
            },
            explode = V3(0f, -0.55f, 0f),
        )
    )

    val controller = MeshBuilder().apply {
        addMesh(
            Geo.slab(0.62f, 0.46f, 0.38f, 0.08f, 0.03f, 2),
            Mat4.translation(V3(-1.42f, 0.51f, 0.62f)),
        )
        addMesh(
            Geo.slab(0.48f, 0.34f, 0.04f, 0.05f, 0.012f),
            Mat4.translation(V3(-1.42f, 0.71f, 0.62f)).multiply(Mat4.rotateX(24f)),
        )
        addMesh(
            Geo.box(0.24f, 0.02f, 0.03f),
            Mat4.translation(V3(-1.42f, 0.585f, 0.86f)),
        )
        addMesh(
            Geo.tube(
                listOf(
                    V3(-1.42f, 0.72f, 0.42f),
                    V3(-1.30f, 1.25f, 0.22f),
                    V3(-1.26f, 2.20f, 0.12f),
                    V3(-1.02f, PIVOT_Y - 0.1f, 0.16f),
                ),
                0.026f, 6, subdivide = 3,
            ),
        )
    }.build()
    parts.add(
        Part(
            id = "delta_controller", name = "Motion controller", system = DELTA_FRAME, assembly = D_FRAME,
            mesh = controller, material = Materials.graphite,
        )
    )
    parts.add(
        Part(
            id = "delta_controller_light", name = "Controller indicators", system = DELTA_FRAME, assembly = D_FRAME,
            mesh = MeshBuilder().apply {
                for (i in 0 until 3) {
                    addMesh(Geo.box(0.05f, 0.015f, 0.05f), Mat4.translation(V3(-1.58f + i * 0.15f, 0.585f, 0.865f)))
                }
            }.build(),
            material = Materials.emitCyan,
        )
    )

    val hotspots = listOf(
        Hotspot(
            "portal", "Portal frame",
            "Three steel legs carry a stiff triangular crown. The motors sit on the crown, so the arm loads never bend the working envelope.",
            "delta_frame", V3(-1.26f, 2.55f, 0.55f),
            CameraPose(62f, 18f, 1.04f), readoutLabel = "PORTAL",
        ),
        Hotspot(
            "servos", "Direct-drive servos",
            "Each axis is a sealed servo pod with an absolute encoder. Direct drive removes the gearbox, so the arm can reverse in milliseconds without backlash.",
            "delta_motor_1", V3(0f, 0.06f, 0.22f),
            CameraPose(20f, 26f, 1.2f), readoutLabel = "SERVO 1",
        ),
        Hotspot(
            "uppers", "Carbon upper arms",
            "Three lightweight arms swing on the servo shafts. Their low inertia is what lets a delta robot hit 200 cycles per minute without overheating.",
            "delta_arm_1", V3(0f, 0f, 0.52f),
            CameraPose(34f, 18f, 1.16f), readoutLabel = "UPPER ARM",
        ),
        Hotspot(
            "rods", "Parallelogram rods",
            "Two thin rods per arm form a parallelogram. That linkage keeps the platform perfectly level through the whole stroke, so the tool never tilts.",
            "delta_rod_1", V3(0.08f, -0.55f, 0.55f),
            CameraPose(24f, 20f, 1.18f), readoutLabel = "LINKAGE",
        ),
        Hotspot(
            "platform", "Level platform",
            "The triangular platform carries the tool. Three pairs of ball joints isolate it from bending, and the vacuum cup grips light parts.",
            "delta_platform", V3(0.05f, 0.02f, 0f),
            CameraPose(20f, 14f, 1.32f), readoutLabel = "EFFECTOR",
        ),
        Hotspot(
            "controller", "Motion controller",
            "The controller solves the inverse kinematics for all three arms every millisecond, so the platform follows a straight line even though the arms only rotate.",
            "delta_controller", V3(-1.42f, 0.69f, 0.86f),
            CameraPose(48f, 24f, 1.24f), readoutLabel = "CONTROL",
        ),
    )

    val steps = listOf(
        GuideStep(
            "A stiff portal",
            "The frame is a tripod: three legs, a triangular crown and a central hub. Stiffness here matters more than mass, because every servo reacts against it.",
            DELTA_FRAME, CameraPose(54f, 16f, 1.02f), 0.15f, 0.7f,
        ),
        GuideStep(
            "Three direct drives",
            "Each servo turns one upper arm. Absolute encoders know the arm angle at power-up, so the robot can start without a homing cycle.",
            DELTA_DRIVE, CameraPose(30f, 24f, 1.12f), 0.35f, 0.85f,
        ),
        GuideStep(
            "Swing, don't reach",
            "The upper arms only rotate. Their tips trace a sphere around each servo; the forearms turn that arc into a much flatter path for the platform.",
            DELTA_LINK, CameraPose(22f, 18f, 1.2f), 0.1f, 1f,
        ),
        GuideStep(
            "The parallelogram",
            "Two rods per arm stay parallel. That is the whole trick: whatever the arms do, the platform keeps the same orientation, so the tool stays square to the work.",
            DELTA_LINK, CameraPose(16f, 22f, 1.24f), 0.2f, 0.9f,
        ),
        GuideStep(
            "The tool",
            "The vacuum cup picks a part, the platform lifts and orbits, then the cup releases. The beacon ring shows when vacuum is present.",
            DELTA_TOOL, CameraPose(18f, 12f, 1.35f), 0.55f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the robot to separate the portal, three servo pods, upper arms, rod pairs and platform. Every arm is a field-replaceable module.",
            null, CameraPose(38f, 24f, 0.98f), 1f, 0.75f,
        ),
    )

    return ObjectSpec(
        id = "delta_robot",
        index = 5,
        title = "TRIAD D-3",
        designation = "D-3 / DELTA PICKER",
        category = "Robotics",
        categoryShort = "ROBO",
        tagline = "Three arms, one level platform.",
        summary = "A three-axis delta picker with direct-drive servos, carbon upper arms, parallelogram linkage and a vacuum-tooled platform that stays level through the whole stroke.",
        details = listOf(
            "TRIAD D-3 is a delta robot: three rotary actuators on a fixed crown drive three upper arms, and each arm pulls a pair of parallel rods attached to a common platform. The platform never rotates, which makes it ideal for fast pick-and-place.",
            "The model runs a real two-link solution for each arm every frame, so the platform can orbit and lift while all three forearms stay the correct length. The controller box on the deck feeds the cable harness that climbs the rear leg.",
        ),
        specs = listOf(
            SpecRow("Axes", "3 translational"),
            SpecRow("Working envelope", "1,130 mm"),
            SpecRow("Payload", "8 kg"),
            SpecRow("Cycle rate", "210 / min"),
            SpecRow("Repeatability", "±0.05 mm"),
        ),
        tags = listOf("robotics", "delta robot", "picker", "parallel kinematics", "servo", "automation"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "CYCLE",
            lowLabel = "SLOW",
            highLabel = "FAST",
            defaultValue = 0.62f,
            hint = "Platform orbit, lift and cycle rate",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(36f, 21f, 1.04f),
        thumbnailPose = CameraPose(38f, 18f, 1.06f),
        thumbnailTime = 3.8f,
        thumbnailControl = 0.78f,
        readouts = { ctx ->
            val center = platformCenter(ctx)
            val amp = 0.35f + 0.65f * ctx.control
            val rate = (0.55f + 0.38f * amp) / (2f * PI.toFloat()) * 60f * 24f
            val lift = (center.y - REST_Y) * 1000f
            val orbit = sqrt(center.x * center.x + center.z * center.z) * 1000f
            listOf(
                Readout("CYCLE", String.format("%.0f", rate), "/min", accent = true),
                Readout("LIFT", String.format("%+.0f", lift), "mm"),
                Readout("ORBIT", String.format("%.0f", orbit), "mm"),
            )
        },
        effects = { ctx ->
            val center = platformCenter(ctx)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.82f, 0xFF6EF2DF.toInt(), 0f, 0.26f),
                SceneEffect.Glow(center + V3(0f, -0.26f, 0f), 0.34f, 0xFF6EF2DF.toInt(), 0.24f),
                SceneEffect.Glow(radialFor(0) * PIVOT_R + V3(0f, PIVOT_Y, 0f), 0.4f, 0xFFF2B25C.toInt(), 0.12f),
                SceneEffect.Spark(center + V3(0f, -0.34f, 0f), 0xFFB8F6EC.toInt(), 0.035f, 0.45f),
            )
        },
    )
}
