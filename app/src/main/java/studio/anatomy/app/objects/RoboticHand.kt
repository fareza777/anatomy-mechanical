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
import kotlin.math.min
import kotlin.math.sin

private const val HAND_FRAME = "FRAME"
private const val HAND_DRIVE = "DRIVES"
private const val HAND_LINK = "LINKAGE"
private const val HAND_SENSE = "SENSING"

private const val H_FRAME = "PALM"
private const val H_SERVO = "SERVO BANK"
private const val H_TENDON = "TENDONS"
private const val H_FINGER = "FINGERS"
private const val H_THUMB = "THUMB"
private const val H_WRIST = "WRIST"

private const val PALM_Y = 1.02f
private const val KNUCKLE_Y = 0.225f

private val FINGER_X = floatArrayOf(-0.205f, -0.070f, 0.070f, 0.205f)
private val FINGER_SPLAY = floatArrayOf(7f, 2f, -2f, -7f)
private val FINGER_NAMES = listOf("Index", "Middle", "Ring", "Little")
private val FINGER_L1 = floatArrayOf(0.245f, 0.262f, 0.246f, 0.214f)
private val FINGER_L2 = floatArrayOf(0.168f, 0.178f, 0.168f, 0.148f)
private val FINGER_L3 = floatArrayOf(0.112f, 0.117f, 0.110f, 0.096f)
private val FINGER_W = floatArrayOf(0.114f, 0.110f, 0.104f, 0.094f)
private val FINGER_T = floatArrayOf(0.096f, 0.093f, 0.088f, 0.080f)

/** A cross-section of the palm shell, lofted between levels. */
private class ShellSection(val y: Float, val width: Float, val depth: Float)

/**
 * Lofts rounded-rectangle rings into a solid, so the palm and the phalanges taper
 * instead of reading as stacked boxes.
 */
private fun loftedShell(sections: List<ShellSection>, radius: Float, segments: Int = 3): Mesh {
    val builder = MeshBuilder()
    val rings = sections.map { section ->
        val corner = radius.coerceAtMost(min(section.width, section.depth) * 0.46f)
        Geo.roundedRectRing(section.width, section.depth, corner, segments)
            .map { V3(it.x, section.y, it.y) }
    }
    for (s in 0 until rings.size - 1) {
        val lower = rings[s]
        val upper = rings[s + 1]
        for (i in lower.indices) {
            val j = (i + 1) % lower.size
            val nx = (lower[i].x + lower[j].x) * 0.5f
            val nz = (lower[i].z + lower[j].z) * 0.5f
            val normal = V3(nx, 0f, nz)
            if (normal.lengthSq() < 1e-7f) continue
            builder.addQuad(lower[i], lower[j], upper[j], upper[i], normal.normalized())
        }
    }
    builder.addFace(rings.first(), -V3.Y)
    builder.addFace(rings.last(), V3.Y)
    return builder.build()
}

/** A rounded pad in the XY plane: width across the finger, height along it, thin in Z. */
private fun padXY(width: Float, height: Float, thickness: Float, radius: Float): Mesh =
    Geo.slab(width, height, thickness, radius, thickness * 0.4f)
        .transformed(Mat4.rotateX(90f))

private fun ellipsoid(rx: Float, ry: Float, rz: Float, segments: Int = 22, rings: Int = 11): Mesh =
    Geo.sphere(1f, segments, rings).transformed(Mat4.scale(V3(rx, ry, rz)))

/**
 * One phalanx: tapered shell, knuckle barrel with pins, volar grip pad, dorsal
 * tendon guide and a domed tip on the distal segment.
 */
private fun phalanxMesh(length: Float, width: Float, thickness: Float, distal: Boolean): Mesh {
    val builder = MeshBuilder()
    val bodyEnd = if (distal) length * 0.74f else length
    builder.addMesh(
        loftedShell(
            listOf(
                ShellSection(0.010f, width * 0.90f, thickness * 0.90f),
                ShellSection(0.048f, width, thickness),
                ShellSection(bodyEnd * 0.58f, width * 0.97f, thickness * 0.98f),
                ShellSection(bodyEnd - 0.016f, width * 0.89f, thickness * 0.91f),
                ShellSection(bodyEnd, width * 0.83f, thickness * 0.85f),
            ),
            radius = width * 0.42f,
            segments = 2,
        )
    )
    // Knuckle barrel and its pin caps.
    builder.addMesh(
        Geo.cylinder(thickness * 0.56f, width * 0.90f, 16, bevel = 0.006f)
            .transformed(Mat4.rotateZ(90f)),
        Mat4.translation(V3(0f, 0.008f, 0f)),
    )
    // Rubber grip strip on the palm side.
    builder.addMesh(
        padXY(width * 0.58f, length * 0.56f, 0.020f, width * 0.15f),
        Mat4.translation(V3(0f, length * 0.53f, thickness * 0.42f)),
    )
    if (distal) {
        builder.addMesh(
            Geo.sphere(1f, 20, 10)
                .transformed(Mat4.scale(V3(width * 0.44f, length * 0.30f, thickness * 0.46f)))
                .transformed(Mat4.translation(V3(0f, bodyEnd, 0f))),
        )
    }
    return builder.build()
}

private fun fingerCurl(ctx: AnimContext, joint: Int, finger: Int): Float {
    val grip = ctx.control
    val bias = 4f * (3 - finger)
    // The live cycle flexes each digit a few degrees around the grip set-point, so RUN
    // is visibly alive instead of freezing the hand at a static pose.
    val breathe = sin(ctx.machineTime * 2.9f + finger * 0.85f + joint * 0.55f) *
        (4.5f + 7.0f * grip)
    return when (joint) {
        0 -> 8f + 46f * grip + bias + breathe
        1 -> 6f + 58f * grip + bias * 0.6f + breathe * 1.15f
        else -> 4f + 44f * grip + breathe * 1.3f
    }
}

fun roboticHand(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "grip",
            width = 1.05f,
            depth = 0.92f,
            top = 0.3f,
            system = HAND_FRAME,
            assembly = H_FRAME,
            accentMaterial = Materials.emitCyan,
        )
    )

    // Palm: narrower at the wrist, full at the knuckles, rounded at every edge.
    val palm = loftedShell(
        listOf(
            ShellSection(-0.280f, 0.380f, 0.150f),
            ShellSection(-0.230f, 0.500f, 0.190f),
            ShellSection(-0.050f, 0.585f, 0.210f),
            ShellSection(0.110f, 0.610f, 0.198f),
            ShellSection(0.195f, 0.580f, 0.168f),
        ),
        radius = 0.078f,
        segments = 2,
    )
    parts.add(
        Part(
            id = "hand_palm", name = "Palm chassis", system = HAND_FRAME, assembly = H_FRAME,
            mesh = palm, material = Materials.paintCream,
            origin = V3(0f, PALM_Y, 0f),
            explode = V3(0f, 0f, -0.25f),
        )
    )

    // Soft volar pad, thenar mound and heel pad give the palm a hand silhouette.
    val palmPad = MeshBuilder().apply {
        addMesh(ellipsoid(0.198f, 0.150f, 0.050f, 24, 12), Mat4.translation(V3(0f, -0.035f, 0.102f)))
        addMesh(
            ellipsoid(0.078f, 0.132f, 0.052f, 20, 10),
            Mat4.translation(V3(-0.236f, -0.075f, 0.096f))
                .multiply(Mat4.rotateY(-10f))
                .multiply(Mat4.rotateZ(-14f)),
        )
        addMesh(ellipsoid(0.054f, 0.096f, 0.044f, 18, 9), Mat4.translation(V3(0.246f, -0.048f, 0.086f)))
    }.build()
    parts.add(
        Part(
            id = "hand_palm_pad", name = "Palm contact pad", system = HAND_SENSE, assembly = H_FRAME,
            mesh = palmPad, material = Materials.graphite,
            parent = "hand_palm",
            explode = V3(0f, 0f, 0.1f),
        )
    )

    // Dorsal frame leaves a window onto the servo bank.
    val back = MeshBuilder().apply {
        addMesh(padXY(0.470f, 0.042f, 0.020f, 0.014f), Mat4.translation(V3(0f, 0.160f, -0.098f)))
        addMesh(padXY(0.380f, 0.042f, 0.020f, 0.014f), Mat4.translation(V3(0f, -0.243f, -0.090f)))
        addMesh(padXY(0.038f, 0.360f, 0.020f, 0.014f), Mat4.translation(V3(-0.246f, -0.035f, -0.098f)))
        addMesh(padXY(0.038f, 0.360f, 0.020f, 0.014f), Mat4.translation(V3(0.246f, -0.035f, -0.098f)))
        addMesh(padXY(0.400f, 0.300f, 0.012f, 0.020f), Mat4.translation(V3(0f, -0.035f, -0.072f)))
    }.build()
    parts.add(
        Part(
            id = "hand_back", name = "Dorsal cover and frame", system = HAND_FRAME, assembly = H_FRAME,
            mesh = back, material = Materials.graphite,
            parent = "hand_palm",
            explode = V3(0f, 0f, -0.45f),
        )
    )

    // Knuckle line: one cross shaft with a clevis for every finger.
    val knuckles = MeshBuilder().apply {
        addMesh(
            Geo.cylinder(0.034f, 0.560f, 18, bevel = 0.008f).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(0f, KNUCKLE_Y, 0.040f)),
        )
        FINGER_X.forEachIndexed { index, x ->
            val width = FINGER_W[index]
            addMesh(
                padXY(0.096f, 0.082f, 0.036f, 0.014f),
                Mat4.translation(V3(x, KNUCKLE_Y + 0.028f, 0.048f)),
            )
            addMesh(
                Geo.cylinder(0.016f, 0.104f, 10).transformed(Mat4.rotateZ(90f)),
                Mat4.translation(V3(x, KNUCKLE_Y + 0.052f, 0.052f)),
            )
            addMesh(
                Geo.cylinder(width * 0.30f, 0.026f, 10).transformed(Mat4.rotateZ(90f)),
                Mat4.translation(V3(x + 0.058f, KNUCKLE_Y + 0.028f, 0.048f)),
            )
            addMesh(
                Geo.cylinder(width * 0.30f, 0.026f, 10).transformed(Mat4.rotateZ(90f)),
                Mat4.translation(V3(x - 0.058f, KNUCKLE_Y + 0.028f, 0.048f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "hand_knuckles", name = "Knuckle shaft", system = HAND_LINK, assembly = H_FRAME,
            mesh = knuckles, material = Materials.darkSteel,
            parent = "hand_palm",
            explode = V3(0f, 0.15f, -0.25f),
        )
    )

    // Servo bank sits inside the palm; its cans are visible through the dorsal window.
    val servos = MeshBuilder().apply {
        FINGER_X.forEach { x ->
            addMesh(
                Geo.cylinder(0.048f, 0.100f, 14, bevel = 0.006f).transformed(Mat4.rotateX(90f)),
                Mat4.translation(V3(x, -0.020f, -0.040f)),
            )
            addMesh(
                Geo.cylinder(0.018f, 0.050f, 12).transformed(Mat4.rotateX(90f)),
                Mat4.translation(V3(x, -0.020f, -0.100f)),
            )
            addMesh(
                Geo.torus(0.046f, 0.007f, 18, 5).transformed(Mat4.rotateX(90f)),
                Mat4.translation(V3(x, -0.020f, -0.098f)),
            )
        }
        addMesh(
            Geo.slab(0.440f, 0.018f, 0.085f, 0.012f, 0.003f),
            Mat4.translation(V3(0f, -0.158f, -0.050f)),
        )
    }.build()
    parts.add(
        Part(
            id = "hand_servos", name = "Servo bank", system = HAND_DRIVE, assembly = H_SERVO,
            mesh = servos, material = Materials.brass,
            parent = "hand_palm",
            explode = V3(0f, 0f, -0.55f),
        )
    )
    parts.add(
        Part(
            id = "hand_servo_leds", name = "Servo indicators", system = HAND_DRIVE, assembly = H_SERVO,
            mesh = MeshBuilder().apply {
                FINGER_X.forEach { x ->
                    addMesh(
                        Geo.cylinder(0.009f, 0.010f, 8).transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(x, -0.020f, -0.104f)),
                    )
                }
                addMesh(
                    Geo.slab(0.300f, 0.007f, 0.007f, 0.003f, 0f),
                    Mat4.translation(V3(0f, 0.148f, -0.108f)),
                )
            }.build(),
            material = Materials.emitCyan,
            parent = "hand_palm",
            explode = V3(0f, 0f, -0.6f),
        )
    )

    // Tendons leave each servo, cross the palm and enter the finger at the knuckle.
    val tendons = MeshBuilder().apply {
        FINGER_X.forEach { x ->
            addMesh(
                Geo.tube(
                    listOf(
                        V3(x, -0.045f, -0.100f),
                        V3(x, 0.060f, -0.112f),
                        V3(x * 0.94f, 0.170f, -0.082f),
                        V3(x, KNUCKLE_Y + 0.030f, -0.030f),
                    ),
                    0.009f, 5, subdivide = 3,
                ),
            )
        }
        addMesh(
            Geo.tube(
                listOf(
                    V3(0.010f, -0.265f, -0.020f),
                    V3(0.060f, -0.400f, -0.080f),
                    V3(0.085f, -0.560f, -0.180f),
                ),
                0.030f, 6, subdivide = 3,
            ),
        )
    }.build()
    parts.add(
        Part(
            id = "hand_tendons", name = "Tendon cables", system = HAND_LINK, assembly = H_TENDON,
            mesh = tendons, material = Materials.copperBright,
            parent = "hand_palm",
            explode = V3(0f, 0f, -0.35f),
        )
    )

    // Four fingers: three tapered phalanges chained through the parent hierarchy.
    FINGER_X.forEachIndexed { finger, x ->
        val width = FINGER_W[finger]
        val baseOrigin = V3(x, KNUCKLE_Y + 0.020f, 0.058f)
        parts.add(
            Part(
                id = "hand_f${finger}_base", name = "${FINGER_NAMES[finger]} proximal",
                system = HAND_LINK, assembly = H_FINGER,
                mesh = phalanxMesh(FINGER_L1[finger], width, FINGER_T[finger], distal = false),
                material = Materials.titanium,
                origin = baseOrigin, parent = "hand_palm",
                rotation = Mat4.rotateZ(FINGER_SPLAY[finger]),
                animate = { ctx -> Mat4.rotateX(fingerCurl(ctx, 0, finger)) },
                explode = V3(x * 0.8f, 0.25f, 0.3f),
            )
        )
        parts.add(
            Part(
                id = "hand_f${finger}_mid", name = "${FINGER_NAMES[finger]} middle",
                system = HAND_LINK, assembly = H_FINGER,
                mesh = phalanxMesh(FINGER_L2[finger], width * 0.96f, FINGER_T[finger] * 0.95f, distal = false),
                material = Materials.titanium,
                origin = V3(0f, FINGER_L1[finger], 0f), parent = "hand_f${finger}_base",
                animate = { ctx -> Mat4.rotateX(fingerCurl(ctx, 1, finger)) },
                explode = V3(x * 0.8f, 0.35f, 0.35f),
            )
        )
        parts.add(
            Part(
                id = "hand_f${finger}_tip", name = "${FINGER_NAMES[finger]} distal",
                system = HAND_LINK, assembly = H_FINGER,
                mesh = phalanxMesh(FINGER_L3[finger], width * 0.90f, FINGER_T[finger] * 0.88f, distal = true),
                material = Materials.titanium,
                origin = V3(0f, FINGER_L2[finger], 0f), parent = "hand_f${finger}_mid",
                animate = { ctx -> Mat4.rotateX(fingerCurl(ctx, 2, finger)) },
                explode = V3(x * 0.8f, 0.45f, 0.4f),
            )
        )
        parts.add(
            Part(
                id = "hand_sensor_$finger", name = "${FINGER_NAMES[finger]} touch sensor",
                system = HAND_SENSE, assembly = H_FINGER,
                mesh = Geo.sphere(0.024f, 12, 7),
                material = Materials.emitCyan,
                origin = V3(0f, FINGER_L3[finger] * 0.80f, FINGER_T[finger] * 0.42f),
                parent = "hand_f${finger}_tip",
                explode = V3(x * 0.8f, 0.5f, 0.45f),
            )
        )
    }

    // Thumb: sits on the thenar mound and points across the palm so it can oppose.
    parts.add(
        Part(
            id = "hand_thumb_base", name = "Thumb metacarpal", system = HAND_LINK, assembly = H_THUMB,
            mesh = phalanxMesh(0.182f, 0.118f, 0.104f, distal = false),
            material = Materials.titanium,
            origin = V3(-0.292f, -0.150f, 0.170f), parent = "hand_palm",
            rotation = Mat4.rotateZ(52f).multiply(Mat4.rotateX(4f)),
            animate = { ctx ->
                Mat4.rotateX(
                    5f + 26f * ctx.control +
                        6.0f * sin(ctx.machineTime * 2.6f + 0.4f),
                )
            },
            explode = V3(-0.35f, 0.0f, 0.35f),
        )
    )
    parts.add(
        Part(
            id = "hand_thumb_mid", name = "Thumb proximal", system = HAND_LINK, assembly = H_THUMB,
            mesh = phalanxMesh(0.150f, 0.110f, 0.098f, distal = false),
            material = Materials.titanium,
            origin = V3(0f, 0.182f, 0f), parent = "hand_thumb_base",
            animate = { ctx ->
                Mat4.rotateX(
                    7f + 30f * ctx.control +
                        7.0f * sin(ctx.machineTime * 2.6f + 1.1f),
                )
            },
            explode = V3(-0.35f, 0.1f, 0.4f),
        )
    )
    parts.add(
        Part(
            id = "hand_thumb_tip", name = "Thumb distal", system = HAND_LINK, assembly = H_THUMB,
            mesh = phalanxMesh(0.120f, 0.102f, 0.092f, distal = true),
            material = Materials.titanium,
            origin = V3(0f, 0.150f, 0f), parent = "hand_thumb_mid",
            animate = { ctx ->
                Mat4.rotateX(
                    5f + 24f * ctx.control +
                        8.0f * sin(ctx.machineTime * 2.6f + 1.8f),
                )
            },
            explode = V3(-0.35f, 0.2f, 0.45f),
        )
    )
    parts.add(
        Part(
            id = "hand_thumb_sensor", name = "Thumb sensor", system = HAND_SENSE, assembly = H_THUMB,
            mesh = Geo.sphere(0.023f, 12, 7), material = Materials.emitCyan,
            origin = V3(0f, 0.090f, 0.045f), parent = "hand_thumb_tip",
            explode = V3(-0.35f, 0.25f, 0.5f),
        )
    )

    // Wrist: a short forearm cuff angled back from the palm, with cable glands.
    val cuffStart = V3(0f, -0.265f, -0.010f)
    val cuffEnd = V3(0f, -0.440f, -0.055f)
    val wrist = MeshBuilder().apply {
        addMesh(Geo.cylinderBetween(cuffStart, cuffEnd, 0.140f, 18))
        addMesh(Geo.cylinderBetween(V3(0f, -0.412f, -0.047f), V3(0f, -0.440f, -0.055f), 0.148f, 18))
        addMesh(
            Geo.tube(
                listOf(
                    V3(0.050f, -0.420f, -0.095f),
                    V3(0.135f, -0.545f, -0.170f),
                    V3(0.050f, -0.655f, -0.120f),
                ),
                0.028f, 6, subdivide = 3,
            ),
        )
        addMesh(
            Geo.slab(0.120f, 0.085f, 0.090f, 0.020f, 0.010f),
            Mat4.translation(V3(0.050f, -0.670f, -0.112f)),
        )
    }.build()
    parts.add(
        Part(
            id = "hand_wrist", name = "Wrist and forearm cuff", system = HAND_FRAME, assembly = H_WRIST,
            mesh = wrist, material = Materials.graphite,
            parent = "hand_palm",
            explode = V3(0f, -0.15f, -0.7f),
        )
    )

    // Titanium collar that the display fork clamps onto.
    val collar = MeshBuilder().apply {
        addMesh(
            Geo.cylinderBetween(
                V3(0f, -0.408f, -0.032f),
                V3(0f, -0.462f, -0.050f),
                0.168f, 18,
            ),
        )
        addMesh(Geo.torus(0.176f, 0.018f, 20, 6), Mat4.translation(V3(0f, -0.428f, -0.038f)))
        addMesh(
            Geo.cylinder(0.026f, 0.340f, 12, bevel = 0.005f).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(0f, -0.438f, -0.044f)),
        )
        listOf(-1f, 1f).forEach { side ->
            addMesh(
                Geo.cylinder(0.036f, 0.030f, 12).transformed(Mat4.rotateZ(90f)),
                Mat4.translation(V3(side * 0.150f, -0.438f, -0.044f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "hand_collar", name = "Mounting collar", system = HAND_FRAME, assembly = H_WRIST,
            mesh = collar, material = Materials.titanium,
            parent = "hand_palm",
            explode = V3(0f, -0.25f, -0.8f),
        )
    )

    val stand = MeshBuilder().apply {
        addMesh(Geo.cylinder(0.125f, 0.045f, 18, bevel = 0.008f), Mat4.translation(V3(0f, 0.322f, -0.095f)))
        addMesh(Geo.cylinderBetween(V3(0f, 0.345f, -0.095f), V3(0f, 0.510f, -0.070f), 0.052f, 16))
        addMesh(Geo.sphere(0.052f, 16, 8), Mat4.translation(V3(0f, 0.510f, -0.070f)))
        listOf(-1f, 1f).forEach { side ->
            addMesh(
                Geo.cylinderBetween(
                    V3(0f, 0.510f, -0.065f),
                    V3(side * 0.168f, 0.558f, -0.030f),
                    0.028f, 12,
                ),
            )
            addMesh(Geo.sphere(0.028f, 12, 6), Mat4.translation(V3(side * 0.168f, 0.558f, -0.030f)))
        }
    }.build()
    parts.add(
        Part(
            id = "hand_stand", name = "Display stand", system = HAND_FRAME, assembly = H_WRIST,
            mesh = stand, material = Materials.darkSteel,
            explode = V3(0f, -0.6f, 0f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "servos", "Servo bank",
            "Five small servos sit inside the palm, one per digit. Each pulls a tendon instead of turning a joint directly, which keeps the fingers light.",
            "hand_servos", V3(-0.066f, -0.020f, -0.132f),
            CameraPose(48f, 26f, 1.24f), readoutLabel = "ACTUATION",
        ),
        Hotspot(
            "tendons", "Tendon cables",
            "Braided cables run from each servo over a guide at the knuckle. Pulling a cable curls the finger; the spring in each joint opens it again.",
            "hand_tendons", V3(-0.066f, 0.100f, -0.118f),
            CameraPose(40f, 30f, 1.2f), readoutLabel = "TENDONS",
        ),
        Hotspot(
            "proximal", "Proximal joint",
            "The first joint carries the largest moment. Its cross-roller bearing is preloaded so the finger cannot wobble under a load.",
            "hand_f1_base", V3(0f, 0.105f, 0.050f),
            CameraPose(36f, 18f, 1.28f), readoutLabel = "JOINT",
        ),
        Hotspot(
            "sensor", "Fingertip sensor",
            "A capacitive pad and a force sensor sit under the rubber tip. The hand knows the moment it touches something, before the servo stalls.",
            "hand_sensor_1", V3(0f, 0.0f, 0.0f),
            CameraPose(26f, 12f, 1.45f), readoutLabel = "TOUCH",
        ),
        Hotspot(
            "thumb", "Opposable thumb",
            "The thumb rotates across the palm so it can meet every finger. That opposition is what turns a gripper into a hand.",
            "hand_thumb_base", V3(0f, 0.090f, 0.045f),
            CameraPose(16f, 10f, 1.32f), readoutLabel = "THUMB",
        ),
        Hotspot(
            "wrist", "Wrist cuff",
            "The cuff carries the cable bundle and the mounting collar. A compliant wrist lets the hand absorb impacts without passing them to the arm.",
            "hand_wrist", V3(0f, -0.360f, -0.030f),
            CameraPose(62f, 22f, 1.18f), readoutLabel = "WRIST",
        ),
    )

    val steps = listOf(
        GuideStep(
            "A hand, not a clamp",
            "Five digits, fifteen joints. The palm is the chassis; everything else hangs from it in a chain of parents and children.",
            HAND_FRAME, CameraPose(34f, 18f, 1.06f), 0.25f, 0.6f,
        ),
        GuideStep(
            "Drive from the palm",
            "Five servos live in the palm, not the fingers. Moving the mass backward makes the hand faster and the fingers thinner.",
            HAND_DRIVE, CameraPose(48f, 28f, 1.18f), 0.45f, 0.8f,
        ),
        GuideStep(
            "Pull, don't push",
            "Each tendon runs over a guide at the knuckle. Pulling shortens the cable and curls the joint; the return spring straightens it.",
            HAND_LINK, CameraPose(38f, 30f, 1.12f), 0.2f, 1f,
        ),
        GuideStep(
            "Feel the grip",
            "Fingertip sensors measure force and slip. The controller stops closing when the force curve says the object is held, not crushed.",
            HAND_SENSE, CameraPose(26f, 14f, 1.3f), 0.55f, 0.9f,
        ),
        GuideStep(
            "Oppose the thumb",
            "The thumb swings across the palm. Without opposition, the other four fingers can only pinch, never hold.",
            HAND_LINK, CameraPose(16f, 12f, 1.24f), 0.75f, 0.85f,
        ),
        GuideStep(
            "The assembly",
            "Explode the hand to separate the palm, servos, tendon set, finger chains and wrist. Each finger is a field-replaceable module.",
            null, CameraPose(36f, 22f, 0.98f), 1f, 0.7f,
        ),
    )

    return ObjectSpec(
        id = "robotic_hand",
        index = 11,
        title = "GRIP H-5",
        designation = "H-5 / TENDON-DRIVEN HAND",
        category = "Robotics",
        categoryShort = "ROBO",
        tagline = "Five fingers, fifteen joints, one grip.",
        summary = "An anthropomorphic robot hand opened at the palm: five tendon-driven digits, servo bank, knuckle bearings, fingertip force sensors and a wrist cuff.",
        details = listOf(
            "GRIP H-5 is a tendon-driven hand. Five servos stay in the palm while braided cables run through the fingers, so the moving parts stay light enough to react quickly.",
            "Each finger is a chain of three phalanges with a spring-return joint. Fingertip sensors measure force and slip, letting the controller hold an egg and a hammer with the same command.",
        ),
        specs = listOf(
            SpecRow("Digits", "5 / 15 joints"),
            SpecRow("Grip force", "180 N"),
            SpecRow("Payload", "5 kg"),
            SpecRow("Mass", "1.4 kg"),
            SpecRow("Latency", "8 ms"),
        ),
        tags = listOf("robotics", "hand", "gripper", "tendon", "prosthetic", "dexterous", "servo"),
        accent = 0xFFB8F6EC.toInt(),
        control = ControlSpec(
            label = "GRIP",
            lowLabel = "OPEN",
            highLabel = "CLOSED",
            defaultValue = 0.14f,
            hint = "Finger curl, tendon travel and grip force",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(24f, 12f, 1.14f),
        thumbnailPose = CameraPose(26f, 13f, 1.16f),
        thumbnailTime = 3.6f,
        thumbnailControl = 0.30f,
        readouts = { ctx ->
            val force = 5f + 175f * ctx.control
            val opening = 92f - 68f * ctx.control
            listOf(
                Readout("GRIP", String.format("%.0f", ctx.control * 100f), "%", accent = true),
                Readout("FORCE", String.format("%.0f", force), "N"),
                Readout("APERTURE", String.format("%.0f", opening), "mm"),
            )
        },
        effects = { ctx ->
            val glow = 0.1f + 0.32f * ctx.control
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 0.62f, 0xFFB8F6EC.toInt(), 0f, 0.22f),
                SceneEffect.Glow(V3(0f, PALM_Y, 0.12f), 0.40f, 0xFF6EF2DF.toInt(), 0.12f),
                SceneEffect.Glow(V3(-0.295f, PALM_Y - 0.05f, 0.18f), 0.18f, 0xFF6EF2DF.toInt(), glow),
                SceneEffect.Spark(V3(0.205f, PALM_Y + 0.62f, 0.22f), 0xFFB8F6EC.toInt(), 0.03f, 0.4f),
            )
        },
    )
}
