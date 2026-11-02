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
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.MeshBuilder

private const val HY_FRAME = "STRUCTURE"
private const val HY_POWER = "HYDRAULICS"
private const val HY_LIFT = "LIFT ARMS"
private const val HY_TOOL = "TOOL"
private const val HY_CAB = "CAB"

private const val HY_BASE = "BASE"
private const val HY_BOOM = "BOOM"
private const val HY_STICK = "STICK"
private const val HY_BUCKET = "BUCKET"
private const val HY_HOUSE = "CAB"

fun hydraulicArm(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "hydra",
            width = 2.6f,
            depth = 1.9f,
            top = 0.3f,
            system = HY_FRAME,
            assembly = HY_BASE,
            accentMaterial = Materials.emitAmber,
        ),
    )

    parts.add(
        Part(
            id = "hy_slew", name = "Slew ring and turntable", system = HY_FRAME, assembly = HY_BASE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.56f, 0.20f, 30, bevel = 0.02f), Mat4.translation(V3(0f, 0.40f, 0f)))
                addMesh(Geo.torus(0.56f, 0.032f, 32, 6), Mat4.translation(V3(0f, 0.30f, 0f)))
                addMesh(Geo.slab(0.96f, 0.88f, 0.14f, 0.10f, 0.03f), Mat4.translation(V3(0f, 0.55f, 0f)))
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    addMesh(
                        Geo.cylinder(0.026f, 0.030f, 8),
                        Mat4.translation(V3((0.52f * kotlin.math.cos(a)).toFloat(), 0.515f, (0.52f * kotlin.math.sin(a)).toFloat())),
                    )
                }
            }.build(),
            material = Materials.paintSlate,
        ),
    )

    parts.add(
        Part(
            id = "hy_counterweight", name = "Counterweight", system = HY_FRAME, assembly = HY_BASE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.52f, 0.66f, 0.52f, 0.10f, 0.03f), Mat4.translation(V3(-0.62f, 0.82f, 0f)))
                addMesh(Geo.box(0.54f, 0.07f, 0.68f), Mat4.translation(V3(-0.62f, 0.60f, 0f)))
            }.build(),
            material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "hy_counter_stripe", name = "Counterweight hazard stripe", system = HY_FRAME, assembly = HY_BASE,
            mesh = Geo.box(0.53f, 0.045f, 0.035f),
            material = Materials.emitAmber,
            origin = V3(-0.62f, 1.06f, 0.33f),
        ),
    )

    parts.add(
        Part(
            id = "hy_cab", name = "Operator cab", system = HY_CAB, assembly = HY_HOUSE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.60f, 0.56f, 0.58f, 0.06f, 0.02f), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.slab(0.62f, 0.58f, 0.06f, 0.08f, 0.015f), Mat4.translation(V3(0f, 0.32f, 0f)))
                addMesh(Geo.box(0.44f, 0.10f, 0.30f), Mat4.translation(V3(0f, -0.20f, 0.02f)))
                addMesh(Geo.box(0.34f, 0.34f, 0.10f), Mat4.translation(V3(0f, 0.05f, -0.23f)))
                addMesh(Geo.cylinder(0.055f, 0.16f, 12), Mat4.translation(V3(0.22f, 0.42f, -0.16f)))
            }.build(),
            material = Materials.paintCream,
            origin = V3(-0.10f, 1.00f, -0.62f),
        ),
    )
    parts.add(
        Part(
            id = "hy_cab_glass", name = "Cab glazing", system = HY_CAB, assembly = HY_HOUSE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.50f, 0.34f, 0.020f), Mat4.translation(V3(0f, 0.03f, 0.29f)))
                addMesh(Geo.box(0.020f, 0.30f, 0.38f), Mat4.translation(V3(0.305f, 0.04f, 0.03f)))
            }.build(),
            material = Materials.glassDark, parent = "hy_cab",
        ),
    )
    parts.add(
        Part(
            id = "hy_beacon", name = "Rotating beacon", system = HY_CAB, assembly = HY_HOUSE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.055f, 0.035f, 14), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.sphere(0.052f, 14, 8), Mat4.translation(V3(0f, 0.045f, 0f)))
            }.build(),
            material = Materials.emitAmber, parent = "hy_cab", origin = V3(-0.22f, 0.36f, 0.16f),
        ),
    )

    parts.add(
        Part(
            id = "hy_boom", name = "Boom", system = HY_LIFT, assembly = HY_BOOM,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.slab(1.66f, 0.055f, 0.28f, 0.025f, 0.010f),
                        Mat4.translation(V3(0.84f, 0f, side * 0.175f)),
                    )
                }
                addMesh(Geo.cylinderBetween(V3(0.04f, 0.12f, 0f), V3(1.64f, 0.08f, 0f), 0.045f, 10))
                addMesh(Geo.cylinderBetween(V3(0.04f, -0.12f, 0f), V3(1.64f, -0.08f, 0f), 0.045f, 10))
                for (i in 0 until 4) {
                    addMesh(
                        Geo.box(0.045f, 0.26f, 0.38f),
                        Mat4.translation(V3(0.36f + i * 0.34f, 0f, 0f)),
                    )
                }
                addMesh(
                    Geo.cylinder(0.105f, 0.44f, 16).transformed(Mat4.rotateX(90f)),
                    Mat4.translation(V3(0f, 0f, 0f)),
                )
                addMesh(
                    Geo.cylinder(0.075f, 0.34f, 14).transformed(Mat4.rotateX(90f)),
                    Mat4.translation(V3(1.66f, 0f, 0f)),
                )
            }.build(),
            material = Materials.paintOrange,
            origin = V3(0.02f, 0.86f, 0f),
            rotation = Mat4.rotateZ(-32f),
            animate = { ctx -> Mat4.rotateZ(-32f - 16f * ctx.control) },
        ),
    )
    parts.add(
        Part(
            id = "hy_boom_light", name = "Boom work light", system = HY_CAB, assembly = HY_BOOM,
            mesh = Geo.box(0.16f, 0.05f, 0.10f),
            material = Materials.emitWhite, parent = "hy_boom", origin = V3(1.50f, -0.18f, 0f),
        ),
    )

    parts.add(
        Part(
            id = "hy_ram", name = "Boom ram", system = HY_POWER, assembly = HY_BOOM,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.cylinderBetween(V3(0.02f, 0.52f, 0.30f), V3(0.76f, 0.94f, 0.30f), 0.105f, 16),
                )
                addMesh(
                    Geo.cylinderBetween(V3(0.70f, 0.90f, 0.30f), V3(1.16f, 1.19f, 0.30f), 0.045f, 12),
                )
                addMesh(Geo.torus(0.11f, 0.018f, 18, 5), Mat4.translation(V3(0.03f, 0.52f, 0.30f)))
                addMesh(
                    Geo.tube(
                        listOf(V3(0.10f, 0.55f, 0.34f), V3(0.20f, 0.66f, 0.40f), V3(0.34f, 0.72f, 0.42f)),
                        0.018f, 6, subdivide = 2,
                    ),
                )
            }.build(),
            material = Materials.darkSteel,
        ),
    )

    parts.add(
        Part(
            id = "hy_stick", name = "Stick", system = HY_LIFT, assembly = HY_STICK,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(1.24f, 0.30f, 0.30f, 0.04f, 0.012f), Mat4.translation(V3(0.62f, 0f, 0f)))
                addMesh(
                    Geo.cylinder(0.085f, 0.36f, 14).transformed(Mat4.rotateX(90f)),
                    Mat4.translation(V3(0f, 0f, 0f)),
                )
                addMesh(
                    Geo.cylinder(0.070f, 0.34f, 14).transformed(Mat4.rotateX(90f)),
                    Mat4.translation(V3(1.26f, 0f, 0f)),
                )
                addMesh(Geo.box(0.60f, 0.035f, 0.32f), Mat4.translation(V3(0.55f, 0.17f, 0f)))
            }.build(),
            material = Materials.paintSlate,
            origin = V3(1.66f, 0f, 0f), parent = "hy_boom",
            animate = { ctx -> Mat4.rotateZ(96f - 24f * ctx.control) },
        ),
    )
    parts.add(
        Part(
            id = "hy_stick_ram", name = "Stick ram", system = HY_POWER, assembly = HY_STICK,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinderBetween(V3(0f, 0f, 0f), V3(0.52f, 0f, 0f), 0.075f, 12))
                addMesh(Geo.cylinderBetween(V3(0.48f, 0f, 0f), V3(0.92f, 0f, 0f), 0.032f, 10))
            }.build(),
            material = Materials.nickel,
            origin = V3(0.86f, 0.20f, 0.25f), parent = "hy_boom",
            rotation = Mat4.rotateZ(-18f),
        ),
    )

    parts.add(
        Part(
            id = "hy_bucket", name = "Bucket", system = HY_TOOL, assembly = HY_BUCKET,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.09f, 0.58f, 0.54f, 0.04f, 0.015f), Mat4.translation(V3(0.06f, 0f, 0f)))
                addMesh(Geo.slab(0.54f, 0.56f, 0.06f, 0.05f, 0.015f), Mat4.translation(V3(0.30f, -0.29f, 0f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.slab(0.54f, 0.06f, 0.52f, 0.04f, 0.015f),
                        Mat4.translation(V3(0.30f, -0.12f, side * 0.26f)),
                    )
                }
                addMesh(Geo.box(0.10f, 0.30f, 0.58f), Mat4.translation(V3(0.02f, 0.10f, 0f)))
                addMesh(
                    Geo.cylinder(0.075f, 0.58f, 14).transformed(Mat4.rotateX(90f)),
                    Mat4.translation(V3(0f, 0.20f, 0f)),
                )
            }.build(),
            material = Materials.darkSteel,
            origin = V3(1.26f, 0f, 0f), parent = "hy_stick",
            animate = { ctx -> Mat4.rotateZ(44f - 22f * ctx.control) },
        ),
    )
    parts.add(
        Part(
            id = "hy_teeth", name = "Bucket teeth", system = HY_TOOL, assembly = HY_BUCKET,
            mesh = MeshBuilder().apply {
                for (i in 0 until 5) {
                    val z = -0.24f + i * 0.12f
                    addMesh(
                        Geo.taperedCylinder(0.052f, 0.018f, 0.20f, 8)
                            .transformed(
                                Mat4.translation(V3(0.58f, -0.30f, z))
                                    .multiply(Mat4.rotateZ(-90f)),
                            ),
                    )
                }
            }.build(),
            material = Materials.nickel, parent = "hy_bucket",
        ),
    )
    parts.add(
        Part(
            id = "hy_linkage", name = "Bucket linkage", system = HY_TOOL, assembly = HY_BUCKET,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinderBetween(
                            V3(0.92f, -0.22f, side * 0.20f),
                            V3(1.16f, 0.08f, side * 0.24f),
                            0.036f, 8,
                        ),
                    )
                    addMesh(
                        Geo.cylinder(0.048f, 0.07f, 10).transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(1.16f, 0.08f, side * 0.26f)),
                    )
                }
            }.build(),
            material = Materials.nickel, parent = "hy_stick",
        ),
    )

    parts.add(
        Part(
            id = "hy_hoses", name = "Hydraulic hoses", system = HY_POWER, assembly = HY_BOOM,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.08f, 0.20f, 0.22f),
                            V3(0.52f, 0.21f, 0.24f),
                            V3(1.02f, 0.20f, 0.23f),
                            V3(1.48f, 0.16f, 0.20f),
                        ),
                        0.022f, 7, subdivide = 3,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.12f, 0.16f, -0.24f),
                            V3(0.62f, 0.18f, -0.25f),
                            V3(1.10f, 0.15f, -0.24f),
                            V3(1.46f, 0.12f, -0.20f),
                        ),
                        0.018f, 7, subdivide = 3,
                    ),
                )
            }.build(),
            material = Materials.rubber, parent = "hy_boom",
        ),
    )

    parts.add(
        Part(
            id = "hy_guard", name = "Deck handrails", system = HY_FRAME, assembly = HY_BASE,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.cylinderBetween(V3(-0.38f, 0.62f, side * 0.38f), V3(-0.38f, 1.06f, side * 0.38f), 0.020f, 6))
                    addMesh(Geo.cylinderBetween(V3(-0.38f, 1.06f, side * 0.20f), V3(-0.38f, 1.06f, side * 0.48f), 0.020f, 6))
                }
                addMesh(Geo.cylinderBetween(V3(-0.38f, 1.06f, -0.40f), V3(-0.38f, 1.06f, 0.40f), 0.020f, 6))
            }.build(),
            material = Materials.nickel,
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "teeth", "Bucket teeth",
            "Five replaceable teeth break the surface first. Their narrow tips concentrate the digging force, and a worn tooth is a ten-minute swap.",
            "hy_teeth", V3(0.60f, -0.30f, 0.10f),
            CameraPose(24f, 8f, 1.48f), readoutLabel = "TOOL",
        ),
        Hotspot(
            "bucket", "Bucket",
            "The bucket curls through the soil with a rolling motion, not a scoop. Its curved back keeps the load centred and the linkage out of the dirt.",
            "hy_bucket", V3(0.26f, -0.18f, 0.20f),
            CameraPose(34f, 12f, 1.32f), readoutLabel = "TOOL",
        ),
        Hotspot(
            "stick", "Stick",
            "The stick multiplies reach and speed. Its pin bosses carry the bucket linkage and the stick ram in double shear.",
            "hy_stick", V3(0.62f, 0.16f, 0.10f),
            CameraPose(46f, 18f, 1.22f), readoutLabel = "LIFT",
        ),
        Hotspot(
            "ram", "Boom ram",
            "A single double-acting cylinder raises the boom. Oil pressure on one side lifts; pressure on the other lowers it with the load.",
            "hy_ram", V3(0.38f, 0.72f, 0.30f),
            CameraPose(58f, 18f, 1.26f), readoutLabel = "HYDRAULICS",
        ),
        Hotspot(
            "slew", "Slew ring",
            "The whole upper structure turns on a geared bearing. A hydraulic motor walks a pinion around the ring, and a brake holds the house when the levers are neutral.",
            "hy_slew", V3(0.46f, 0.40f, 0.20f),
            CameraPose(54f, 30f, 1.24f), readoutLabel = "SWING",
        ),
        Hotspot(
            "cab", "Operator cab",
            "The cab is isolated on rubber mounts. The operator sees the bucket, the boom and the ground through a single curved pane.",
            "hy_cab", V3(0.10f, 0.16f, 0.28f),
            CameraPose(40f, 18f, 1.30f), readoutLabel = "CONTROL",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Plant the base",
            "The lower frame carries the axles and the outriggers. The upper house turns on the slew ring above it, so the machine can dig without moving its wheels.",
            HY_BASE, CameraPose(34f, 22f, 1.08f), 0.15f, 0.15f,
        ),
        GuideStep(
            "Swing the house",
            "A hydraulic motor drives a pinion around the ring gear. The swing brake locks the house the moment the operator lets go of the lever.",
            HY_BASE, CameraPose(56f, 28f, 1.20f), 0.10f, 0.35f,
        ),
        GuideStep(
            "Raise the boom",
            "The boom ram pushes the boom up. Its geometry trades lifting force for reach: a steeper boom lifts less but dumps higher.",
            HY_POWER, CameraPose(52f, 18f, 1.18f), 0.35f, 0.60f,
        ),
        GuideStep(
            "Curl the bucket",
            "The stick ram pulls the stick in while the bucket ram curls the bucket. The two motions combine into a flat-bottomed cut.",
            HY_TOOL, CameraPose(30f, 10f, 1.28f), 0.20f, 0.85f,
        ),
        GuideStep(
            "Watch the pressure",
            "Relief valves cap the pressure before a hose can burst. The ram on the boom sees the highest load when the bucket is full and the stick is extended.",
            HY_POWER, CameraPose(60f, 20f, 1.24f), 0.45f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the machine to separate the house, the boom, the stick, the bucket, the rams and the linkage. The pins are the only parts that wear.",
            null, CameraPose(38f, 24f, 0.96f), 1f, 0.5f,
        ),
    )

    return ObjectSpec(
        id = "hydraulic_arm",
        index = 14,
        title = "HYDRA-LIFT 6",
        designation = "HL-6 / HYDRAULIC EXCAVATOR",
        category = "Hydraulics",
        categoryShort = "HYD",
        tagline = "An arm that thinks in oil.",
        summary = "A tracked excavator upper house: slew ring, operator cab, counterweight, boom, stick, bucket and the rams that move all of it.",
        details = listOf(
            "HYDRA-LIFT 6 is a compact excavator built around a single idea: move the load with oil, not with gears. Three rams do almost all of the work, and the slew motor turns the whole house.",
            "The bucket is the only part that touches the ground. Its teeth are consumables, its linkage is geometry, and the boom is a box beam that has to resist bending in two directions at once.",
        ),
        specs = listOf(
            SpecRow("Operating mass", "6.2 t"),
            SpecRow("Engine", "48 kW diesel"),
            SpecRow("Max reach", "6.4 m"),
            SpecRow("Bucket", "0.22 m³"),
            SpecRow("Pressure", "245 bar"),
        ),
        tags = listOf("excavator", "hydraulic", "arm", "bucket", "construction", "machine"),
        accent = 0xFFE08A4A.toInt(),
        control = ControlSpec(
            label = "REACH",
            lowLabel = "DIG",
            highLabel = "DUMP",
            defaultValue = 0.5f,
            hint = "Boom angle, stick extension and bucket curl",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(32f, 18f, 1.10f),
        thumbnailPose = CameraPose(34f, 20f, 1.12f),
        thumbnailTime = 2.4f,
        thumbnailControl = 0.62f,
        readouts = { ctx ->
            listOf(
                Readout("REACH", String.format("%.1f", 5.6f + 1.0f * ctx.control), "m", accent = true),
                Readout("PRESSURE", String.format("%.0f", 190f + 55f * ctx.control), "bar"),
                Readout("LOAD", String.format("%.1f", 2.4f - 0.7f * ctx.control), "t"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.18f, 0xFFE08A4A.toInt(), 0f, 0.18f),
                SceneEffect.Glow(V3(-0.10f, 1.26f, -0.44f), 0.20f, 0xFFFFB562.toInt(), 0.30f),
                SceneEffect.Flow(
                    points = listOf(
                        V3(0.30f, 1.10f, 0.24f),
                        V3(0.80f, 1.24f, 0.24f),
                        V3(1.40f, 1.38f, 0.22f),
                    ),
                    color = 0xFFE08A4A.toInt(),
                    count = 5,
                    speed = 0.22f + 0.3f * ctx.control,
                    phase = 0.2f,
                    dotRadius = 0.022f,
                ),
            )
        },
    )
}
