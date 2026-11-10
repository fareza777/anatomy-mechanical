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
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val LENS_OPTIC = "OPTICS"
private const val LENS_BODY = "BODY"
private const val LENS_IRIS = "IRIS"
private const val LENS_MOUNT = "MOUNT"

private const val L_GLASS = "ELEMENTS"
private const val L_SHELL = "BARREL"
private const val L_APERTURE = "APERTURE"
private const val L_FOCUS = "FOCUS"
private const val L_BAYONET = "BAYONET"
private const val L_STAND = "CRADLE"

private const val AXIS_Y = 1.30f

private fun arcBarrel(
    x: Float,
    length: Float,
    outer: Float,
    inner: Float,
    start: Float = 150f,
    sweep: Float = 240f,
    segments: Int = 40,
): Mesh =
    Geo.arcShell(outer, inner, length, segments, start, sweep, true)
        .transformed(Mat4.translation(V3(x, AXIS_Y, 0f)).multiply(Mat4.rotateZ(-90f)))

private fun ringAt(x: Float, outer: Float, inner: Float, width: Float, segments: Int = 36): Mesh =
    Geo.hollowCylinder(outer, inner, width, segments)
        .transformed(Mat4.translation(V3(x, AXIS_Y, 0f)).multiply(Mat4.rotateZ(-90f)))

private fun torusAt(x: Float, major: Float, minor: Float): Mesh =
    Geo.torus(major, minor, 36, 6)
        .transformed(Mat4.translation(V3(x, AXIS_Y, 0f)).multiply(Mat4.rotateZ(-90f)))

private fun ringLocal(x: Float, outer: Float, inner: Float, width: Float, segments: Int = 36): Mesh =
    Geo.hollowCylinder(outer, inner, width, segments)
        .transformed(Mat4.translation(V3(x, 0f, 0f)).multiply(Mat4.rotateZ(-90f)))

private fun torusLocal(x: Float, major: Float, minor: Float): Mesh =
    Geo.torus(major, minor, 36, 6)
        .transformed(Mat4.translation(V3(x, 0f, 0f)).multiply(Mat4.rotateZ(-90f)))

/**
 * A lens element: a solid of revolution with two curved faces. The profile runs from the
 * centre of the front surface out to the rim and back along the rear surface.
 */
private fun lensElement(radius: Float, thickness: Float, curvature: Float): Mesh {
    val rings = 7
    val profile = ArrayList<Pair<Float, Float>>(2 * (rings + 1))
    for (i in 0..rings) {
        val t = i.toFloat() / rings
        val r = radius * t
        val sag = thickness * 0.5f * (1f - curvature * t * t)
        profile.add(r to sag)
    }
    for (i in rings downTo 0) {
        val t = i.toFloat() / rings
        val r = radius * t
        val sag = -thickness * 0.5f * (1f - curvature * t * t)
        profile.add(r to sag)
    }
    return Geo.lathe(profile, 26)
        .transformed(Mat4.rotateZ(-90f))
}

private fun placeElement(x: Float, radius: Float, thickness: Float, curvature: Float): Mesh =
    lensElement(radius, thickness, curvature).transformed(Mat4.translation(V3(x, AXIS_Y, 0f)))

private fun irisBlades(): Mesh = MeshBuilder().apply {
    for (i in 0 until 9) {
        val angle = 40f * i
        addMesh(
            Geo.slab(0.012f, 0.30f, 0.105f, 0.01f, 0.004f),
            Mat4.translation(V3(0f, 0.14f, 0f))
                .multiply(Mat4.rotateX(angle)),
        )
    }
}.build()

fun cameraLens(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "aperture",
            width = 3.5f,
            depth = 2.35f,
            top = 0.3f,
            system = LENS_BODY,
            assembly = L_STAND,
            accentMaterial = Materials.emitBlue,
        )
    )

    val shell = MeshBuilder().apply {
        addMesh(arcBarrel(-0.62f, 0.66f, 0.545f, 0.49f))
        addMesh(arcBarrel(0.02f, 1.10f, 0.585f, 0.515f))
        addMesh(arcBarrel(0.75f, 0.60f, 0.615f, 0.535f))
        // Machined joining rings.
        listOf(-0.95f, -0.29f, 0.57f, 1.05f).forEach { x ->
            addMesh(ringAt(x, 0.63f, 0.585f, 0.055f))
        }
        addMesh(torusAt(1.11f, 0.60f, 0.045f))
        addMesh(torusAt(-0.99f, 0.57f, 0.035f))
        // Inner light trap.
        addMesh(arcBarrel(0.02f, 1.04f, 0.49f, 0.475f, 150f, 240f, 36))
        // Top engravings / index marks.
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0)
            addMesh(
                Geo.box(0.016f, 0.012f, 0.09f),
                Mat4.translation(
                    V3(
                        0.10f,
                        AXIS_Y + 0.575f * cos(angle).toFloat(),
                        0.575f * sin(angle).toFloat(),
                    )
                ).multiply(Mat4.rotateX(-i * 30f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "lens_shell", name = "Cutaway lens barrel", system = LENS_BODY, assembly = L_SHELL,
            mesh = shell, material = Materials.paintSlate, doubleSided = true,
            explode = V3(0f, 1.0f, 0f),
        )
    )

    // Eight glass elements, largest at the rear group.
    val stack = MeshBuilder().apply {
        listOf(
            -0.78f to 0.385f,
            -0.58f to 0.425f,
            -0.36f to 0.395f,
            -0.12f to 0.355f,
            0.10f to 0.305f,
            0.32f to 0.275f,
            0.55f to 0.245f,
            0.82f to 0.225f,
            0.99f to 0.31f,
        ).forEachIndexed { index, (x, radius) ->
            val thickness = if (index == 0 || index == 8) 0.13f else 0.10f
            val curvature = if (index % 2 == 0) 1.15f else 0.85f
            addMesh(
                placeElement(x, radius, thickness, curvature),
                Mat4.identity(),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "lens_stack", name = "Optical element stack", system = LENS_OPTIC, assembly = L_GLASS,
            mesh = stack, material = Materials.glass, doubleSided = true,
            explode = V3(0f, 0.2f, 0.9f),
        )
    )
    val coatings = MeshBuilder().apply {
        listOf(
            Triple(-0.78f, 0.385f, 0.13f),
            Triple(-0.36f, 0.395f, 0.10f),
            Triple(0.10f, 0.305f, 0.10f),
            Triple(0.55f, 0.245f, 0.10f),
            Triple(0.99f, 0.31f, 0.13f),
        ).forEachIndexed { index, (x, radius, thickness) ->
            addMesh(
                Geo.torus(radius * 0.98f, 0.012f, 30, 5),
                Mat4.translation(V3(x, AXIS_Y, 0f)).multiply(Mat4.rotateZ(90f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "lens_coatings", name = "Coated rim rings", system = LENS_OPTIC, assembly = L_GLASS,
            mesh = coatings, material = Materials.emitBlue,
            explode = V3(0f, 0.2f, 0.9f),
        )
    )
    val spacers = MeshBuilder().apply {
        listOf(-0.68f, -0.47f, -0.24f, 0.0f, 0.21f, 0.44f, 0.68f).forEach { x ->
            addMesh(ringAt(x, 0.455f, 0.37f, 0.035f, 30))
        }
    }.build()
    parts.add(
        Part(
            id = "lens_spacers", name = "Element spacers", system = LENS_OPTIC, assembly = L_GLASS,
            mesh = spacers, material = Materials.darkSteel,
            explode = V3(0f, 0.25f, 0.8f),
        )
    )

    parts.add(
        Part(
            id = "lens_iris", name = "Nine-blade iris", system = LENS_IRIS, assembly = L_APERTURE,
            mesh = irisBlades(), material = Materials.darkSteel,
            origin = V3(0f, AXIS_Y, 0f),
            animate = { ctx ->
                val scale = 1.12f - 0.50f * ctx.control
                Mat4.scale(V3(1f, scale, scale))
                    .multiply(Mat4.rotateX(ctx.machineTime * (5f + 11f * ctx.control)))
            },
            explode = V3(0f, 0.4f, 0.95f),
        )
    )
    parts.add(
        Part(
            id = "lens_iris_ring", name = "Aperture ring", system = LENS_IRIS, assembly = L_APERTURE,
            mesh = MeshBuilder().apply {
                addMesh(ringAt(0.0f, 0.49f, 0.455f, 0.10f, 34))
                addMesh(torusAt(0.055f, 0.47f, 0.02f))
            }.build(),
            material = Materials.brass,
            explode = V3(0f, 0.4f, 0.95f),
        )
    )

    val focusRing = MeshBuilder().apply {
        addMesh(ringLocal(0.80f, 0.665f, 0.60f, 0.26f, 40))
        for (i in 0 until 28) {
            val angle = Math.toRadians(i * 360.0 / 28.0)
            addMesh(
                Geo.box(0.26f, 0.035f, 0.05f),
                Mat4.translation(
                    V3(
                        0.80f,
                        0.665f * cos(angle).toFloat(),
                        0.665f * sin(angle).toFloat(),
                    )
                ).multiply(Mat4.rotateX(-i * (360f / 28f))),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "lens_focus", name = "Focus ring", system = LENS_BODY, assembly = L_FOCUS,
            mesh = focusRing, material = Materials.graphite,
            origin = V3(0f, AXIS_Y, 0f),
            animate = { ctx -> Mat4.rotateX(38f * sin(ctx.machineTime * 0.5f) * ctx.control) },
            explode = V3(0f, 1.2f, 0f),
        )
    )

    val mount = MeshBuilder().apply {
        addMesh(ringAt(-1.10f, 0.555f, 0.44f, 0.18f, 36))
        for (i in 0 until 3) {
            val angle = Math.toRadians(120.0 * i + 30.0)
            addMesh(
                Geo.slab(0.16f, 0.16f, 0.055f, 0.025f, 0.008f),
                Mat4.translation(
                    V3(
                        -1.20f,
                        AXIS_Y + 0.40f * cos(angle).toFloat(),
                        0.40f * sin(angle).toFloat(),
                    )
                ).multiply(Mat4.rotateX(Math.toDegrees(angle).toFloat())),
            )
        }
        // Gold contacts and the red alignment dot.
        for (i in 0 until 6) {
            addMesh(
                Geo.box(0.02f, 0.05f, 0.16f),
                Mat4.translation(V3(-1.215f, AXIS_Y - 0.28f, -0.24f + i * 0.09f)),
            )
        }
        addMesh(Geo.cylinder(0.035f, 0.025f, 16), Mat4.translation(V3(-1.205f, AXIS_Y + 0.47f, 0.20f)).multiply(Mat4.rotateZ(90f)))
        // Rear electronics block.
        addMesh(Geo.slab(0.20f, 0.22f, 0.14f, 0.03f, 0.01f), Mat4.translation(V3(-1.02f, AXIS_Y - 0.46f, 0.10f)))
    }.build()
    parts.add(
        Part(
            id = "lens_mount", name = "Bayonet mount", system = LENS_MOUNT, assembly = L_BAYONET,
            mesh = mount, material = Materials.titanium,
            explode = V3(0f, 0.6f, 0f),
        )
    )
    parts.add(
        Part(
            id = "lens_contacts", name = "Gold contacts", system = LENS_MOUNT, assembly = L_BAYONET,
            mesh = MeshBuilder().apply {
                for (i in 0 until 6) {
                    addMesh(
                        Geo.box(0.015f, 0.05f, 0.15f),
                        Mat4.translation(V3(-1.225f, AXIS_Y - 0.28f, -0.24f + i * 0.09f)),
                    )
                }
                addMesh(Geo.cylinder(0.03f, 0.02f, 14), Mat4.translation(V3(-1.215f, AXIS_Y + 0.47f, 0.20f)).multiply(Mat4.rotateZ(90f)))
            }.build(),
            material = Materials.gold,
            explode = V3(0f, 0.6f, 0f),
        )
    )

    val cradle = MeshBuilder().apply {
        listOf(-0.36f, 0.36f).forEach { x ->
            addMesh(torusAt(x, 0.64f, 0.035f))
            addMesh(Geo.slab(0.12f, 0.14f, 0.42f, 0.03f, 0.012f), Mat4.translation(V3(x, 0.62f, 0.62f)))
            addMesh(Geo.slab(0.12f, 0.14f, 0.42f, 0.03f, 0.012f), Mat4.translation(V3(x, 0.62f, -0.62f)))
            addMesh(Geo.slab(0.30f, 0.44f, 0.06f, 0.05f, 0.015f), Mat4.translation(V3(x, 0.35f, 0.62f)))
            addMesh(Geo.slab(0.30f, 0.44f, 0.06f, 0.05f, 0.015f), Mat4.translation(V3(x, 0.35f, -0.62f)))
        }
    }.build()
    parts.add(
        Part(
            id = "lens_cradle", name = "Display cradle", system = LENS_BODY, assembly = L_STAND,
            mesh = cradle, material = Materials.darkSteel,
            explode = V3(0f, -0.8f, 0f),
        )
    )

    val hood = MeshBuilder().apply {
        addMesh(ringAt(1.23f, 0.66f, 0.60f, 0.12f, 40))
        addMesh(torusAt(1.29f, 0.635f, 0.025f))
        addMesh(ringAt(-1.10f, 0.60f, 0.55f, 0.05f, 36))
    }.build()
    parts.add(
        Part(
            id = "lens_hood", name = "Front bezel and thread", system = LENS_BODY, assembly = L_SHELL,
            mesh = hood, material = Materials.brass,
            explode = V3(1.0f, 0.2f, 0f),
        )
    )

    parts.add(
        Part(
            id = "lens_sensor", name = "Sensor reference plane", system = LENS_OPTIC, assembly = L_GLASS,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.slab(0.02f, 0.46f, 0.32f, 0.03f, 0.006f),
                    Mat4.translation(V3(-1.32f, AXIS_Y, 0f)),
                )
            }.build(),
            material = Materials.emitBlue,
            explode = V3(-0.6f, 0f, 0f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "front", "Front element",
            "The big front element gathers light first. Its curved surfaces are coated to cut reflections; a single stray reflection would wash out the contrast.",
            "lens_stack", V3(0.99f, AXIS_Y, 0f),
            CameraPose(20f, 10f, 1.3f), readoutLabel = "FRONT GROUP",
        ),
        Hotspot(
            "iris", "Nine-blade iris",
            "Nine overlapping blades form a nearly circular hole. Each stop halves the light, and the shape of the blades shows up as bokeh in out-of-focus highlights.",
            "lens_iris", V3(0f, 0f, 0f),
            CameraPose(4f, 6f, 1.5f), readoutLabel = "APERTURE",
        ),
        Hotspot(
            "stack", "Element stack",
            "Nine elements in six groups. Some are cemented pairs that cancel colour fringing; the rear group focuses the cone onto the sensor.",
            "lens_stack", V3(-0.36f, AXIS_Y, 0.1f),
            CameraPose(28f, 14f, 1.18f), readoutLabel = "OPTICS",
        ),
        Hotspot(
            "focus", "Focus ring",
            "The ring drives a helical cam that moves two groups. Internal focus means the barrel does not change length while it focuses.",
            "lens_focus", V3(0.0f, 0.40f, 0.45f),
            CameraPose(38f, 16f, 1.2f), readoutLabel = "FOCUS",
        ),
        Hotspot(
            "mount", "Bayonet mount",
            "Three steel tabs lock the lens to the body. Gold contacts carry power and data, and the red dot aligns the mount in the dark.",
            "lens_mount", V3(-1.12f, AXIS_Y, 0.34f),
            CameraPose(72f, 14f, 1.24f), readoutLabel = "MOUNT",
        ),
        Hotspot(
            "cradle", "Service cradle",
            "The cradle holds the barrel on its nodal axis so the cutaway stays readable. In service, the same ring is used as a tripod collar.",
            "lens_cradle", V3(0.0f, 0.55f, 0.62f),
            CameraPose(44f, 18f, 1.12f), readoutLabel = "SUPPORT",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Light enters",
            "The front element is wider than the rest of the barrel so it can collect light from a wide angle. The hood keeps stray light off that surface.",
            LENS_OPTIC, CameraPose(24f, 10f, 1.12f), 0.15f, 0.8f,
        ),
        GuideStep(
            "Bend the rays",
            "Each element bends the cone a little. Cemented pairs correct colour, while the rear group converges the beam onto a sensor only a few millimetres wide.",
            LENS_OPTIC, CameraPose(34f, 16f, 1.14f), 0.35f, 0.75f,
        ),
        GuideStep(
            "Set the aperture",
            "Nine blades slide over each other. Wide open, the lens gathers more light and isolates the subject; stopped down, more of the scene is sharp.",
            LENS_IRIS, CameraPose(4f, 6f, 1.55f), 0.0f, 0.35f,
        ),
        GuideStep(
            "Focus",
            "Turning the ring moves a small group along a helix. Because the front of the barrel does not move, the balance of the lens stays neutral on a gimbal.",
            LENS_BODY, CameraPose(40f, 18f, 1.14f), 0.2f, 0.85f,
        ),
        GuideStep(
            "Mount and talk",
            "The bayonet carries the load, the contacts carry data. The body tells the lens the aperture and focus commands, and the lens reports its position back.",
            LENS_MOUNT, CameraPose(74f, 12f, 1.26f), 0.1f, 0.9f,
        ),
        GuideStep(
            "The assembly",
            "Explode the barrel to separate the shell, element stack, iris, focus ring and mount. Each group is collimated as a module before final assembly.",
            null, CameraPose(38f, 22f, 0.98f), 1f, 0.8f,
        ),
    )

    return ObjectSpec(
        id = "camera_lens",
        index = 8,
        title = "APERTURE 85",
        designation = "A-85 / 85 mm PRIME",
        category = "Optics",
        categoryShort = "LENS",
        tagline = "A portrait lens, opened to the light.",
        summary = "An 85 mm prime lens cut in half: nine-element glass stack, nine-blade iris, focus helical, bayonet mount and gold contacts, with light rays traced through the barrel.",
        details = listOf(
            "APERTURE 85 is a fast 85 mm prime. The cutaway shows the front group, the iris plane and the rear group that converges light onto the sensor.",
            "The focus ring moves two internal groups along a helical cam, so the barrel never extends. Gold contacts at the mount carry power and data between lens and body.",
        ),
        specs = listOf(
            SpecRow("Focal length", "85 mm"),
            SpecRow("Aperture", "f/1.4 – f/16"),
            SpecRow("Elements", "9 in 6 groups"),
            SpecRow("Closest focus", "0.85 m"),
            SpecRow("Mass", "820 g"),
        ),
        tags = listOf("optics", "camera", "lens", "aperture", "photography", "glass"),
        accent = 0xFF9FB4FF.toInt(),
        control = ControlSpec(
            label = "APERTURE",
            lowLabel = "f/1.4",
            highLabel = "f/16",
            defaultValue = 0.32f,
            hint = "Iris opening, light and depth of field",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(26f, 12f, 1.08f),
        thumbnailPose = CameraPose(30f, 12f, 1.10f),
        thumbnailTime = 3.0f,
        thumbnailControl = 0.25f,
        readouts = { ctx ->
            val f = 1.4f * 2f.pow(3.5f * ctx.control)
            val light = 1f / (f / 1.4f).pow(2f)
            listOf(
                Readout("APERTURE", String.format("f/%.1f", f), accent = true),
                Readout("LIGHT", String.format("%.0f", light * 100f), "%"),
                Readout("FOCUS", "0.85", "m"),
            )
        },
        effects = { ctx ->
            val spread = 0.34f - 0.18f * ctx.control
            listOf(
                SceneEffect.Flow(
                    points = listOf(
                        V3(1.55f, AXIS_Y + spread, spread * 0.6f),
                        V3(0.85f, AXIS_Y + spread * 0.45f, spread * 0.3f),
                        V3(0.10f, AXIS_Y + spread * 0.18f, 0f),
                        V3(-1.15f, AXIS_Y, 0f),
                    ),
                    color = 0xFF9FB4FF.toInt(),
                    count = 12,
                    speed = 0.28f,
                    phase = 0.2f,
                    dotRadius = 0.018f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(1.55f, AXIS_Y - spread, -spread * 0.6f),
                        V3(0.85f, AXIS_Y - spread * 0.45f, -spread * 0.3f),
                        V3(0.10f, AXIS_Y - spread * 0.18f, 0f),
                        V3(-1.15f, AXIS_Y, 0f),
                    ),
                    color = 0xFFB8F6EC.toInt(),
                    count = 12,
                    speed = 0.28f,
                    phase = 0.65f,
                    dotRadius = 0.018f,
                ),
                SceneEffect.Glow(V3(0.10f, AXIS_Y, 0f), 0.30f, 0xFF9FB4FF.toInt(), 0.18f),
                SceneEffect.Glow(V3(0.98f, AXIS_Y, 0f), 0.42f, 0xFFD8E2FF.toInt(), 0.14f),
            )
        },
    )
}
