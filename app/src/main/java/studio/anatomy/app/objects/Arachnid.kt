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

private const val AR_FRAME = "CHASSIS"
private const val AR_HYD = "HYDRAULICS"
private const val AR_DRIVE = "DRIVE"
private const val AR_SENSE = "SENSING"

private const val AR_BODY = "CEPHALOTHORAX"
private const val AR_ABDOMEN = "ABDOMEN"
private const val AR_HEAD = "SENSOR HEAD"
private const val AR_LEGS = "LEG ARRAY"
private const val AR_STAND = "DISPLAY"

private const val AR_BODY_Y = 1.04f
private val LEG_Z = floatArrayOf(-0.36f, -0.12f, 0.12f, 0.36f)

private fun ball(rx: Float, ry: Float, rz: Float, segments: Int = 22, rings: Int = 11): Mesh =
    Geo.sphere(1f, segments, rings).transformed(Mat4.scale(V3(rx, ry, rz)))

private fun hipMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.082f, 0.12f, 14, bevel = 0.008f))
    addMesh(Geo.torus(0.086f, 0.014f, 16, 5), Mat4.translation(V3(0f, 0.052f, 0f)))
    addMesh(Geo.cylinderBetween(V3(0f, 0f, 0f), V3(0.20f, 0f, 0f), 0.052f, 10))
    addMesh(
        Geo.cylinder(0.060f, 0.15f, 12).transformed(Mat4.rotateX(90f)),
        Mat4.translation(V3(0.20f, 0f, 0f)),
    )
    listOf(-1f, 1f).forEach { side ->
        addMesh(
            Geo.slab(0.11f, 0.020f, 0.12f, 0.008f, 0.003f),
            Mat4.translation(V3(0.16f, 0f, side * 0.062f)),
        )
    }
}.build()

private fun femurMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinderBetween(V3(0f, 0f, 0f), V3(0.82f, 0f, 0f), 0.048f, 10))
    addMesh(
        Geo.slab(0.58f, 0.15f, 0.052f, 0.018f, 0.007f),
        Mat4.translation(V3(0.44f, 0.055f, 0f)),
    )
    addMesh(Geo.cylinderBetween(V3(0.07f, 0.085f, 0f), V3(0.58f, 0.085f, 0f), 0.033f, 8))
    addMesh(Geo.cylinderBetween(V3(0.56f, 0.085f, 0f), V3(0.80f, 0.030f, 0f), 0.017f, 8))
    addMesh(Geo.sphere(0.055f, 12, 6), Mat4.translation(V3(0f, 0f, 0f)))
    listOf(0.24f, 0.46f, 0.68f).forEach { x ->
        addMesh(
            Geo.torus(0.060f, 0.011f, 14, 4).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
    }
}.build()

private fun tibiaMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinderBetween(V3(0f, 0f, 0f), V3(0.84f, 0f, 0f), 0.042f, 10))
    addMesh(Geo.cylinderBetween(V3(0f, 0.052f, 0f), V3(0.52f, 0.052f, 0f), 0.020f, 8))
    addMesh(Geo.sphere(0.055f, 12, 6))
    listOf(0.20f, 0.40f, 0.60f).forEach { x ->
        addMesh(
            Geo.torus(0.050f, 0.010f, 14, 4).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
    }
}.build()

private fun tarsusMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinderBetween(V3(0f, 0f, 0f), V3(0.28f, 0f, 0f), 0.034f, 8))
    addMesh(Geo.sphere(0.046f, 12, 6))
    listOf(-1f, 1f).forEach { side ->
        addMesh(
            Geo.cylinderBetween(
                V3(0.24f, 0.015f, side * 0.020f),
                V3(0.37f, -0.10f, side * 0.070f),
                0.014f, 6,
            ),
        )
        addMesh(Geo.sphere(0.020f, 8, 5), Mat4.translation(V3(0.37f, -0.10f, side * 0.070f)))
    }
}.build()

fun arachnid(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "arachne",
            width = 3.4f,
            depth = 3.0f,
            top = 0.3f,
            system = AR_FRAME,
            assembly = AR_STAND,
            accentMaterial = Materials.emitAmber,
        ),
    )

    val body = MeshBuilder().apply {
        addMesh(
            Geo.slab(0.82f, 1.02f, 0.30f, 0.15f, 0.045f, 3),
            Mat4.translation(V3(0f, 0f, 0f)),
        )
        addMesh(
            Geo.slab(0.66f, 0.82f, 0.10f, 0.11f, 0.03f, 2),
            Mat4.translation(V3(0f, 0.18f, -0.02f)),
        )
        addMesh(
            Geo.slab(0.54f, 0.50f, 0.06f, 0.06f, 0.02f),
            Mat4.translation(V3(0f, -0.18f, 0f)),
        )
        addMesh(
            Geo.slab(0.58f, 0.22f, 0.16f, 0.06f, 0.02f),
            Mat4.translation(V3(0f, 0.02f, 0.56f)),
        )
        listOf(-1f, 1f).forEach { side ->
            LEG_Z.forEach { z ->
                addMesh(
                    Geo.cylinder(0.072f, 0.10f, 12).transformed(Mat4.rotateZ(90f)),
                    Mat4.translation(V3(side * 0.43f, -0.04f, z)),
                )
            }
        }
    }.build()
    parts.add(
        Part(
            id = "ar_body", name = "Cephalothorax chassis", system = AR_FRAME, assembly = AR_BODY,
            mesh = body, material = Materials.paintSlate,
            origin = V3(0f, AR_BODY_Y, 0f),
            animate = { ctx ->
                Mat4.translation(
                    V3(
                        0f,
                        0.014f * sin(ctx.machineTime * 1.9f) * (0.35f + 0.65f * ctx.control),
                        0f,
                    ),
                )
            },
        ),
    )

    parts.add(
        Part(
            id = "ar_panels", name = "Service panels", system = AR_FRAME, assembly = AR_BODY,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.slab(0.44f, 0.30f, 0.028f, 0.03f, 0.008f),
                    Mat4.translation(V3(0f, 0.238f, 0.22f)),
                )
                addMesh(
                    Geo.slab(0.30f, 0.42f, 0.026f, 0.03f, 0.008f),
                    Mat4.translation(V3(0f, 0.238f, -0.22f)),
                )
                listOf(-0.10f, 0.10f).forEach { x ->
                    addMesh(
                        Geo.box(0.018f, 0.010f, 0.24f),
                        Mat4.translation(V3(x, 0.254f, 0.22f)),
                    )
                }
                addMesh(
                    Geo.cylinder(0.085f, 0.06f, 16, bevel = 0.008f),
                    Mat4.translation(V3(0f, 0.250f, -0.36f)),
                )
            }.build(),
            material = Materials.darkSteel, parent = "ar_body",
        ),
    )

    parts.add(
        Part(
            id = "ar_core", name = "Hydraulic accumulator", system = AR_HYD, assembly = AR_BODY,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.112f, 0.20f, 18, bevel = 0.012f), Mat4.translation(V3(0f, 0.32f, 0.02f)))
                addMesh(Geo.sphere(0.112f, 18, 8), Mat4.translation(V3(0f, 0.42f, 0.02f)))
                addMesh(Geo.torus(0.118f, 0.012f, 20, 5), Mat4.translation(V3(0f, 0.28f, 0.02f)))
                addMesh(
                    Geo.cylinderBetween(V3(0f, 0.42f, 0.02f), V3(0.26f, 0.18f, 0.02f), 0.018f, 7),
                )
            }.build(),
            material = Materials.copperBright, parent = "ar_body",
        ),
    )
    parts.add(
        Part(
            id = "ar_core_glow", name = "Pressure indicator", system = AR_HYD, assembly = AR_BODY,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.070f, 0.008f, 18, 4), Mat4.translation(V3(0f, 0.445f, 0.02f)))
                addMesh(Geo.cylinder(0.020f, 0.014f, 12), Mat4.translation(V3(0f, 0.500f, 0.02f)))
            }.build(),
            material = Materials.emitAmber, parent = "ar_body",
        ),
    )

    parts.add(
        Part(
            id = "ar_abdomen", name = "Abdomen shell", system = AR_FRAME, assembly = AR_ABDOMEN,
            mesh = MeshBuilder().apply {
                addMesh(ball(0.46f, 0.38f, 0.62f, 28, 14))
                addMesh(ball(0.34f, 0.30f, 0.20f, 20, 10), Mat4.translation(V3(0f, 0.02f, -0.54f)))
                addMesh(
                    Geo.taperedCylinder(0.10f, 0.05f, 0.16f, 16),
                    Mat4.translation(V3(0f, -0.02f, -0.70f)).multiply(Mat4.rotateX(-90f)),
                )
            }.build(),
            material = Materials.graphite,
            origin = V3(0f, 0.02f, -0.86f), parent = "ar_body",
        ),
    )
    parts.add(
        Part(
            id = "ar_abdomen_plates", name = "Abdomen armour bands", system = AR_FRAME, assembly = AR_ABDOMEN,
            mesh = MeshBuilder().apply {
                listOf(-0.34f, -0.06f, 0.22f).forEachIndexed { index, z ->
                    addMesh(
                        Geo.torus(0.45f - index * 0.035f, 0.022f, 26, 5)
                            .transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(0f, 0.01f, z)),
                    )
                }
                addMesh(
                    Geo.slab(0.40f, 0.24f, 0.030f, 0.04f, 0.010f),
                    Mat4.translation(V3(0f, 0.30f, 0.06f)),
                )
            }.build(),
            material = Materials.darkSteel, parent = "ar_abdomen",
        ),
    )
    parts.add(
        Part(
            id = "ar_vents", name = "Heat vents", system = AR_HYD, assembly = AR_ABDOMEN,
            mesh = MeshBuilder().apply {
                for (i in 0 until 5) {
                    addMesh(
                        Geo.box(0.34f, 0.012f, 0.026f),
                        Mat4.translation(V3(0f, 0.235f, -0.24f + i * 0.075f)),
                    )
                }
            }.build(),
            material = Materials.emitAmber, parent = "ar_abdomen",
        ),
    )

    parts.add(
        Part(
            id = "ar_head", name = "Sensor head", system = AR_SENSE, assembly = AR_HEAD,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.slab(0.54f, 0.34f, 0.22f, 0.08f, 0.03f, 2),
                    Mat4.translation(V3(0f, 0f, 0.04f)),
                )
                addMesh(
                    Geo.slab(0.44f, 0.12f, 0.10f, 0.03f, 0.012f),
                    Mat4.translation(V3(0f, 0.01f, 0.22f)),
                )
                addMesh(
                    Geo.slab(0.36f, 0.16f, 0.04f, 0.03f, 0.01f),
                    Mat4.translation(V3(0f, 0.12f, 0.10f)),
                )
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinder(0.048f, 0.16f, 14, bevel = 0.006f)
                            .transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(side * 0.20f, 0.10f, -0.02f)),
                    )
                }
            }.build(),
            material = Materials.paintCream,
            origin = V3(0f, 0.03f, 0.66f), parent = "ar_body",
        ),
    )
    parts.add(
        Part(
            id = "ar_eyes", name = "Ruby eye cluster", system = AR_SENSE, assembly = AR_HEAD,
            mesh = MeshBuilder().apply {
                listOf(-0.15f, -0.05f, 0.05f, 0.15f).forEach { x ->
                    addMesh(Geo.sphere(0.038f, 14, 8), Mat4.translation(V3(x, 0.035f, 0.175f)))
                }
                addMesh(Geo.cylinder(0.055f, 0.03f, 14).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0.01f, -0.055f, 0.16f)))
            }.build(),
            material = Materials.ruby, parent = "ar_head",
        ),
    )
    parts.add(
        Part(
            id = "ar_lenses", name = "Stereo lenses", system = AR_SENSE, assembly = AR_HEAD,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinder(0.062f, 0.035f, 18).transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(side * 0.125f, 0.085f, 0.185f)),
                    )
                    addMesh(
                        Geo.torus(0.062f, 0.010f, 18, 4).transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(side * 0.125f, 0.085f, 0.200f)),
                    )
                }
            }.build(),
            material = Materials.glassDark, parent = "ar_head",
        ),
    )
    parts.add(
        Part(
            id = "ar_fangs", name = "Chelicerae and fangs", system = AR_SENSE, assembly = AR_HEAD,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinderBetween(
                            V3(side * 0.13f, -0.10f, 0.14f),
                            V3(side * 0.10f, -0.26f, 0.24f),
                            0.032f, 8,
                        ),
                    )
                    addMesh(
                        Geo.taperedCylinder(0.026f, 0.0f, 0.16f, 10)
                            .transformed(
                                Mat4.translation(V3(side * 0.095f, -0.33f, 0.28f))
                                    .multiply(Mat4.rotateZ(side * 8f)),
                            ),
                    )
                }
            }.build(),
            material = Materials.nickel, parent = "ar_head",
        ),
    )

    val femur = femurMesh()
    val tibia = tibiaMesh()
    val tarsus = tarsusMesh()
    val hip = hipMesh()
    listOf(-1f, 1f).forEach { side ->
        LEG_Z.forEachIndexed { i, z ->
            val leg = (if (side < 0f) 0 else 4) + i
            val phase = leg * 0.78f
            val base = "ar_leg${leg}"
            val zSign = if (side < 0f) -1f else 1f
            parts.add(
                Part(
                    id = "${base}_hip", name = "Leg ${leg + 1} hip", system = AR_FRAME, assembly = AR_LEGS,
                    mesh = hip, material = Materials.darkSteel,
                    origin = V3(side * 0.40f, -0.10f, z), parent = "ar_body",
                    rotation = if (side < 0f) Mat4.rotateY(180f) else Mat4.identity(),
                    explode = V3(side * 0.5f, 0.2f, 0f),
                ),
            )
            parts.add(
                Part(
                    id = "${base}_femur", name = "Leg ${leg + 1} femur", system = AR_HYD, assembly = AR_LEGS,
                    mesh = femur, material = Materials.titanium,
                    origin = V3(0.20f, 0f, 0f), parent = "${base}_hip",
                    animate = { ctx ->
                        Mat4.rotateZ(
                            zSign * (-16f + 6.5f * sin(ctx.machineTime * 2.2f + phase) *
                                (0.35f + 0.65f * ctx.control)),
                        )
                    },
                    explode = V3(side * 0.6f, 0.3f, 0f),
                ),
            )
            parts.add(
                Part(
                    id = "${base}_tibia", name = "Leg ${leg + 1} tibia", system = AR_HYD, assembly = AR_LEGS,
                    mesh = tibia, material = Materials.titanium,
                    origin = V3(0.82f, 0f, 0f), parent = "${base}_femur",
                    animate = { ctx ->
                        Mat4.rotateZ(
                            zSign * (84f + 5.5f * sin(ctx.machineTime * 2.2f + phase + 1.1f) *
                                (0.35f + 0.65f * ctx.control)),
                        )
                    },
                    explode = V3(side * 0.75f, 0.2f, 0f),
                ),
            )
            parts.add(
                Part(
                    id = "${base}_tarsus", name = "Leg ${leg + 1} foot", system = AR_DRIVE, assembly = AR_LEGS,
                    mesh = tarsus, material = Materials.darkSteel,
                    origin = V3(0.84f, 0f, 0f), parent = "${base}_tibia",
                    animate = { ctx ->
                        Mat4.rotateZ(
                            zSign * (12f + 4.0f * sin(ctx.machineTime * 2.2f + phase + 2.2f) *
                                (0.35f + 0.65f * ctx.control)),
                        )
                    },
                    explode = V3(side * 0.9f, 0.1f, 0f),
                ),
            )
        }
    }

    val hotspots = listOf(
        Hotspot(
            "eyes", "Eight-eyed sensor cluster",
            "Four ruby detectors see in the near infrared, two sapphire lenses build a stereo depth map, and the pair below watches the ground directly under the fangs.",
            "ar_eyes", V3(0.05f, 0.035f, 0.06f),
            CameraPose(18f, 6f, 1.45f), readoutLabel = "VISION",
        ),
        Hotspot(
            "fangs", "Chelicerae",
            "The fangs are hollow injectors, not weapons: they feed a penetrating oil into seized joints on the machines this unit services.",
            "ar_fangs", V3(0.11f, -0.24f, 0.06f),
            CameraPose(24f, 4f, 1.5f), readoutLabel = "TOOLING",
        ),
        Hotspot(
            "core", "Piston accumulator",
            "A nitrogen-backed accumulator smooths the eight leg circuits. It stores enough pressure for three steps if the pump stalls.",
            "ar_core", V3(0f, 0.08f, 0.02f),
            CameraPose(42f, 34f, 1.32f), readoutLabel = "PRESSURE",
        ),
        Hotspot(
            "abdomen", "Power abdomen",
            "The abdomen carries the battery, the pump and the heat exchangers. Its segmented shell flexes with the body and sheds heat through the amber louvres.",
            "ar_abdomen", V3(0f, 0.10f, -0.30f),
            CameraPose(140f, 22f, 1.26f), readoutLabel = "POWER",
        ),
        Hotspot(
            "femur", "Femur and hydraulic ram",
            "Each femur is a titanium tube with a ram along its spine. The ram lifts the leg; the femur angle decides how wide the stance becomes.",
            "ar_leg0_femur", V3(0.42f, 0.07f, 0f),
            CameraPose(60f, 16f, 1.24f), readoutLabel = "ACTUATION",
        ),
        Hotspot(
            "foot", "Claw foot",
            "Two hooked claws and a rubber pad let the foot grip pipe, grating or glass. A contact sensor in the pad stops the leg the moment it lands.",
            "ar_leg0_tarsus", V3(0.30f, -0.06f, 0f),
            CameraPose(52f, 8f, 1.4f), readoutLabel = "CONTACT",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Eight legs, one gait",
            "The controller walks four legs at a time. The other four stay planted, so the body is always supported by a stable quadrupod.",
            AR_LEGS, CameraPose(38f, 24f, 1.08f), 0.10f, 0.15f,
        ),
        GuideStep(
            "Lift and reach",
            "The femur ram lifts the leg while the tibia folds. The foot lands ahead of the body, and the stance phase pulls the chassis forward.",
            AR_HYD, CameraPose(62f, 14f, 1.20f), 0.35f, 0.55f,
        ),
        GuideStep(
            "Watch the ground",
            "The ruby eyes and the stereo lenses feed a terrain map. The body stays level while individual feet find their own height.",
            AR_SENSE, CameraPose(20f, 6f, 1.42f), 0.05f, 0.4f,
        ),
        GuideStep(
            "Keep the pressure",
            "The accumulator and the pump share the load. Pressure drops on lift and recovers on the stance, which is why the gait sounds uneven.",
            AR_HYD, CameraPose(44f, 32f, 1.28f), 0.20f, 0.8f,
        ),
        GuideStep(
            "Sprint",
            "At full stride the diagonal pairs overlap and the body bobs. The controller trades stability for speed but never lifts more than four feet.",
            AR_DRIVE, CameraPose(36f, 28f, 1.14f), 0.0f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the shell to separate the cephalothorax, abdomen, sensor head and eight identical leg modules. Every leg is field-replaceable.",
            null, CameraPose(42f, 26f, 0.98f), 1f, 0.5f,
        ),
    )

    return ObjectSpec(
        id = "arachnid",
        index = 12,
        title = "ARACHNE-8",
        designation = "A-08 / MECHANICAL ARACHNID",
        category = "Robotics",
        categoryShort = "ROB",
        tagline = "Eight legs, one very steady body.",
        summary = "An eight-legged walking machine: a machined cephalothorax, a powered abdomen, a ruby-eyed sensor head and eight hydraulically articulated legs.",
        details = listOf(
            "ARACHNE-8 is a service walker built for places a wheeled robot cannot reach. It climbs stairs, crosses grating and keeps its body level while every foot finds its own contact.",
            "The legs are identical modules: hip, femur, tibia and clawed tarsus. A single pump feeds eight independent rams, and the accumulator in the back keeps the pressure steady between steps.",
        ),
        specs = listOf(
            SpecRow("Mass", "84 kg"),
            SpecRow("Stance", "2.6 m span"),
            SpecRow("Payload", "22 kg"),
            SpecRow("Pressure", "210 bar"),
            SpecRow("Battery", "2.4 kWh"),
        ),
        tags = listOf("spider", "arachnid", "walker", "robotics", "hydraulic", "eight legs", "legged"),
        accent = 0xFFF2B25C.toInt(),
        control = ControlSpec(
            label = "STRIDE",
            lowLabel = "PROWL",
            highLabel = "SPRINT",
            defaultValue = 0.45f,
            hint = "Gait frequency, body bob and hydraulic effort",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(38f, 24f, 1.22f),
        thumbnailPose = CameraPose(40f, 22f, 1.24f),
        thumbnailTime = 2.6f,
        thumbnailControl = 0.62f,
        readouts = { ctx ->
            listOf(
                Readout("STRIDE", String.format("%.1f", 1.1f + 1.9f * ctx.control), "Hz", accent = true),
                Readout("PRESSURE", String.format("%.0f", 120f + 90f * ctx.control), "bar"),
                Readout("POWER", String.format("%.0f", 45f + 48f * ctx.control), "%"),
            )
        },
        effects = { ctx ->
            val pulse = 0.35f + 0.65f * ctx.control
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.55f, 0xFFF2B25C.toInt(), 0f, 0.20f),
                SceneEffect.Glow(V3(0f, 1.31f, 0.80f), 0.22f, 0xFFD94A5E.toInt(), 0.35f),
                SceneEffect.Glow(
                    V3(0f, AR_BODY_Y + 0.46f, -0.80f),
                    0.30f,
                    0xFFF2B25C.toInt(),
                    0.10f + 0.22f * pulse,
                ),
            )
        },
    )
}
