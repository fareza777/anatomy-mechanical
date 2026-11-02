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
import studio.anatomy.app.model.PlumePalette
import studio.anatomy.app.model.Readout
import studio.anatomy.app.model.SceneEffect
import studio.anatomy.app.model.SpecRow
import studio.anatomy.app.render.Geo
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder
import kotlin.math.cos
import kotlin.math.sin

private const val STAND = "STAND"
private const val FAN = "FAN"
private const val NACELLE = "NACELLE"
private const val COMPRESSOR = "COMPRESSOR"
private const val COMBUSTOR = "COMBUSTOR"
private const val TURBINE = "TURBINE"
private const val NOZZLE = "NOZZLE"
private const val CORE = "CORE"
private const val ACCESSORY = "ACCESSORY"

private fun rotateToX(): Mat4 = Mat4.rotateZ(-90f)

private fun ringX(x: Float, outer: Float, inner: Float, length: Float, segments: Int = 32): Mesh =
    Geo.hollowCylinder(outer, inner, length, segments)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(rotateToX()))

private fun cylinderX(x: Float, radius: Float, length: Float, segments: Int = 28, bevel: Float = 0f): Mesh =
    Geo.cylinder(radius, length, segments, bevel = bevel)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(rotateToX()))

private fun torusX(x: Float, major: Float, minor: Float, majorSegments: Int = 32, minorSegments: Int = 6): Mesh =
    Geo.torus(major, minor, majorSegments, minorSegments)
        .transformed(Mat4.translation(x, 0f, 0f).multiply(rotateToX()))

private fun latheX(x: Float, profile: List<Pair<Float, Float>>, segments: Int = 40): Mesh =
    Geo.lathe(profile, segments).transformed(Mat4.translation(x, 0f, 0f).multiply(rotateToX()))

private fun bladeRing(
    builder: MeshBuilder,
    x: Float,
    hubRadius: Float,
    span: Float,
    count: Int,
    chord: Float,
    twist: Float,
    thickness: Float,
    sweep: Float = 0.02f,
    sections: Int = 4,
    perimeter: Int = 6,
) {
    val blade = Geo.roundedBlade(
        span = span,
        rootChord = chord,
        tipChord = chord * 0.72f,
        rootThickness = thickness,
        tipThickness = thickness * 0.45f,
        twistDegrees = twist,
        sweep = sweep,
        sections = sections,
        perimeter = perimeter,
    )
    for (i in 0 until count) {
        val angle = 360f * i / count
        val transform = Mat4.translation(x, 0f, 0f)
            .multiply(Mat4.rotateX(angle))
            .multiply(Mat4.translation(V3(0f, hubRadius, 0f)))
        builder.addMesh(blade, transform)
    }
}

private fun vaneRing(
    builder: MeshBuilder,
    x: Float,
    outerRadius: Float,
    innerRadius: Float,
    count: Int,
    chord: Float,
    twist: Float,
): Mesh {
    val span = outerRadius - innerRadius
    val blade = Geo.roundedBlade(
        span = span,
        rootChord = chord,
        tipChord = chord * 0.8f,
        rootThickness = 0.035f,
        tipThickness = 0.025f,
        twistDegrees = twist,
        sweep = 0f,
        sections = 2,
        perimeter = 5,
    )
    for (i in 0 until count) {
        val angle = 360f * i / count
        val transform = Mat4.translation(x, 0f, 0f)
            .multiply(Mat4.rotateX(angle))
            .multiply(Mat4.translation(V3(0f, innerRadius, 0f)))
        builder.addMesh(blade, transform)
    }
    return builder.build()
}

private fun saddleCradle(id: String, x: Float, radius: Float, plinthTop: Float): List<Part> {
    val parts = ArrayList<Part>()
    val saddleTop = -radius - 0.12f

    val legs = MeshBuilder()
    legs.addMesh(Geo.slab(0.42f, 3.1f, 0.2f, 0.08f, 0.03f), Mat4.translation(V3(x, saddleTop, 0f)))
    legs.addMesh(
        Geo.cylinderBetween(V3(x, plinthTop + 0.05f, -1.95f), V3(x, saddleTop, -1.35f), 0.09f, 12),
    )
    legs.addMesh(
        Geo.cylinderBetween(V3(x, plinthTop + 0.05f, 1.95f), V3(x, saddleTop, 1.35f), 0.09f, 12),
    )
    legs.addMesh(
        Geo.cylinderBetween(V3(x, plinthTop + 0.05f, -1.95f), V3(x, saddleTop, 1.35f), 0.055f, 10),
    )
    legs.addMesh(
        Geo.cylinderBetween(V3(x, plinthTop + 0.05f, 1.95f), V3(x, saddleTop, -1.35f), 0.055f, 10),
    )
    legs.addMesh(Geo.slab(0.7f, 0.34f, 0.12f, 0.06f, 0.02f), Mat4.translation(V3(x, plinthTop + 0.02f, -1.95f)))
    legs.addMesh(Geo.slab(0.7f, 0.34f, 0.12f, 0.06f, 0.02f), Mat4.translation(V3(x, plinthTop + 0.02f, 1.95f)))
    parts.add(
        Part(
            id = "${id}_frame", name = "Cradle frame", system = "MOUNT", assembly = STAND,
            mesh = legs.build(), material = Materials.darkSteel, origin = V3.ZERO
        )
    )

    val saddle = MeshBuilder()
    val arcCount = 9
    for (i in 0 until arcCount) {
        val angle = 180f - 56f + 112f * i / (arcCount - 1)
        val radians = Math.toRadians(angle.toDouble())
        val pad = Geo.box(0.62f, 0.13f, 0.26f)
        val y = (radius - 0.04f) * cos(radians).toFloat()
        val z = (radius - 0.04f) * sin(radians).toFloat()
        val t = Mat4.translation(V3(x, y, z)).multiply(Mat4.rotateX(angle))
        saddle.addMesh(pad, t)
    }
    parts.add(
        Part(
            id = "${id}_saddle", name = "Saddle pad", system = "MOUNT", assembly = STAND,
            mesh = saddle.build(), material = Materials.rubber, origin = V3.ZERO
        )
    )

    val clamp = MeshBuilder()
    clamp.addMesh(
        Geo.slab(0.18f, 0.42f, 0.9f, 0.05f, 0.02f),
        Mat4.translation(V3(x, saddleTop + 0.6f, -radius + 0.1f)),
    )
    clamp.addMesh(
        Geo.slab(0.18f, 0.42f, 0.9f, 0.05f, 0.02f),
        Mat4.translation(V3(x, saddleTop + 0.6f, radius - 0.1f)),
    )
    parts.add(
        Part(
            id = "${id}_clamp", name = "Saddle clamp", system = "MOUNT", assembly = STAND,
            mesh = clamp.build(), material = Materials.titanium, origin = V3.ZERO
        )
    )
    return parts
}

private data class NacellePanel(val angle: Float, val x: Float, val width: Float, val depth: Float)

fun turbofan(): ObjectSpec {
    val parts = ArrayList<Part>()
    val deckTop = -2.98f

    parts.add(
        Part(
            id = "rail_left", name = "Anodised bed rail", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.slab(10.6f, 0.44f, 0.5f, 0.08f, 0.04f),
            material = Materials.bedPlate, origin = V3(0f, -3.28f, -1.62f)
        )
    )
    parts.add(
        Part(
            id = "rail_right", name = "Anodised bed rail", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.slab(10.6f, 0.44f, 0.5f, 0.08f, 0.04f),
            material = Materials.bedPlate, origin = V3(0f, -3.28f, 1.62f)
        )
    )
    val bearers = MeshBuilder()
    listOf(-3.9f, 0f, 3.9f).forEach { x ->
        bearers.addMesh(Geo.slab(0.52f, 3.48f, 0.36f, 0.07f, 0.035f), Mat4.translation(V3(x, -3.3f, 0f)))
        bearers.addMesh(Geo.cylinder(0.085f, 0.05f, 6), Mat4.translation(V3(x, -3.09f, -1.55f)))
        bearers.addMesh(Geo.cylinder(0.085f, 0.05f, 6), Mat4.translation(V3(x, -3.09f, 1.55f)))
    }
    parts.add(
        Part(
            id = "bearers", name = "Cross bearers", system = "STRUCTURE", assembly = STAND,
            mesh = bearers.build(), material = Materials.graphite, origin = V3.ZERO
        )
    )
    parts.add(
        Part(
            id = "deck", name = "Service deck", system = "STRUCTURE", assembly = STAND,
            mesh = run {
                val deck = MeshBuilder()
                listOf(-1.02f, 1.02f).forEach { z ->
                    deck.addMesh(
                        Geo.slab(9.2f, 1.14f, 0.12f, 0.18f, 0.022f),
                        Mat4.translation(V3(0f, 0f, z))
                    )
                }
                deck.build()
            },
            material = Materials.bedPlate, origin = V3(0f, -3.04f, 0f)
        )
    )
    val deckDetail = MeshBuilder()
    listOf(-1.45f, -0.53f, 0.53f, 1.45f).forEach { z ->
        deckDetail.addMesh(Geo.box(8.6f, 0.014f, 0.022f), Mat4.translation(V3(0f, 0.014f, z)))
    }
    listOf(-3.9f, 0f, 3.9f).forEach { x ->
        deckDetail.addMesh(Geo.box(0.03f, 0.012f, 2.5f), Mat4.translation(V3(x, 0.012f, 0f)))
    }
    parts.add(
        Part(
            id = "deck_lines", name = "Deck seams", system = "STRUCTURE", assembly = STAND,
            mesh = deckDetail.build(), material = Materials.brushedSteel, origin = V3(0f, deckTop, 0f)
        )
    )
    parts.add(
        Part(
            id = "service_channel", name = "Service channel", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.box(8.9f, 0.1f, 0.8f), material = Materials.deckInset,
            origin = V3(0f, -3.17f, 0f)
        )
    )
    val conduit = MeshBuilder()
    conduit.addMesh(Geo.cylinderBetween(V3(-4.15f, -3.1f, -0.16f), V3(4.15f, -3.1f, -0.16f), 0.07f, 12))
    listOf(-3.6f, -1.8f, 0f, 1.8f, 3.6f).forEach { x ->
        conduit.addMesh(Geo.box(0.09f, 0.14f, 0.27f), Mat4.translation(V3(x, -3.09f, -0.16f)))
    }
    parts.add(
        Part(
            id = "service_conduit", name = "Service conduit", system = "STRUCTURE", assembly = STAND,
            mesh = conduit.build(), material = Materials.copper, origin = V3.ZERO
        )
    )
    parts.add(
        Part(
            id = "channel_glow", name = "Channel guide light", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.box(7.8f, 0.016f, 0.022f), material = Materials.emitAmber,
            origin = V3(0f, -3.105f, 0.34f)
        )
    )
    parts.add(
        Part(
            id = "safety_stripe", name = "Safety stripe", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.box(9.24f, 0.026f, 0.036f), material = Materials.brass,
            origin = V3(0f, -2.985f, 1.42f)
        )
    )

    val feet = MeshBuilder()
    listOf(
        V3(-4.9f, 0f, -1.62f), V3(4.9f, 0f, -1.62f),
        V3(-4.9f, 0f, 1.62f), V3(4.9f, 0f, 1.62f),
    ).forEach { p ->
        feet.addMesh(Geo.cylinder(0.27f, 0.3f, 12), Mat4.translation(p + V3(0f, -3.56f, 0f)))
        feet.addMesh(Geo.cylinder(0.33f, 0.09f, 14), Mat4.translation(p + V3(0f, -3.74f, 0f)))
    }
    parts.add(
        Part(
            id = "feet", name = "Levelling feet", system = "STRUCTURE", assembly = STAND,
            mesh = feet.build(), material = Materials.darkSteel, origin = V3.ZERO
        )
    )

    val bolts = MeshBuilder()
    listOf(-3.9f, 0f, 3.9f).forEach { x ->
        listOf(-1.55f, 1.55f).forEach { z ->
            bolts.addMesh(Geo.cylinder(0.075f, 0.045f, 6), Mat4.translation(V3(x, -2.96f, z)))
        }
    }
    parts.add(
        Part(
            id = "base_bolts", name = "Bed fasteners", system = "STRUCTURE", assembly = STAND,
            mesh = bolts.build(), material = Materials.brass, origin = V3.ZERO
        )
    )

    val ruler = MeshBuilder()
    ruler.addMesh(Geo.box(7.4f, 0.018f, 0.12f), Mat4.translation(V3(-0.2f, 0f, 0f)))
    for (i in 0..18) {
        val major = i % 3 == 0
        val tick = Geo.box(0.014f, 0.01f, if (major) 0.1f else 0.05f)
        ruler.addMesh(tick, Mat4.translation(V3(-3.7f + i * 0.41f, 0.012f, if (major) 0.015f else 0.035f)))
    }
    parts.add(
        Part(
            id = "ruler", name = "Index rule", system = "STRUCTURE", assembly = STAND,
            mesh = ruler.build(), material = Materials.titanium,
            origin = V3(0f, deckTop + 0.006f, 1.4f)
        )
    )
    parts.add(
        Part(
            id = "index_marker", name = "Datum index", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.box(0.045f, 0.024f, 0.13f), material = Materials.emitAmber,
            origin = V3(-1.95f, deckTop + 0.022f, 1.4f)
        )
    )

    val label = MeshBuilder()
    label.addMesh(Geo.box(1.56f, 0.6f, 0.03f), Mat4.translation(V3(0f, 0f, 0f)))
    parts.add(
        Part(
            id = "data_plate", name = "Data plate", system = "STRUCTURE", assembly = STAND,
            mesh = label.build(), material = Materials.brass,
            origin = V3(-3.45f, -3.28f, 1.86f)
        )
    )
    val labelFace = MeshBuilder()
    labelFace.addMesh(Geo.box(1.4f, 0.46f, 0.036f), Mat4.translation(V3(0f, 0f, 0.012f)))
    for (i in 0 until 3) {
        labelFace.addMesh(
            Geo.box(0.82f - i * 0.14f, 0.016f, 0.01f),
            Mat4.translation(V3(-0.22f, 0.11f - i * 0.11f, 0.034f))
        )
    }
    parts.add(
        Part(
            id = "data_plate_face", name = "Data plate legend", system = "STRUCTURE", assembly = STAND,
            mesh = labelFace.build(), material = Materials.labelPaper,
            origin = V3(-3.45f, -3.28f, 1.86f)
        )
    )
    parts.add(
        Part(
            id = "status_lamp", name = "Status lamp", system = "STRUCTURE", assembly = STAND,
            mesh = Geo.merge(
                Geo.box(0.17f, 0.04f, 0.17f),
                Geo.box(0.09f, 0.05f, 0.09f).transformed(Mat4.translation(0f, 0.012f, 0f)),
            ),
            material = Materials.emitCyan, origin = V3(4.32f, deckTop + 0.03f, 1.24f)
        )
    )

    parts.addAll(saddleCradle("front", 2.35f, 2.45f, deckTop))
    parts.addAll(saddleCradle("rear", -3.15f, 1.05f, deckTop))

    val braces = MeshBuilder()
    braces.addMesh(Geo.cylinderBetween(V3(-4.6f, -3.2f, -1.5f), V3(-3.15f, -2.05f, 0f), 0.05f, 10))
    braces.addMesh(Geo.cylinderBetween(V3(4.6f, -3.2f, 1.5f), V3(2.35f, -2.58f, 0f), 0.05f, 10))
    parts.add(
        Part(
            id = "tie_bars", name = "Tie bars", system = "MOUNT", assembly = STAND,
            mesh = braces.build(), material = Materials.titanium, origin = V3.ZERO
        )
    )

    parts.add(
        Part(
            id = "fan_shroud", name = "Fan containment ring", system = "NACELLE", assembly = NACELLE,
            mesh = Geo.merge(
                ringX(3.8f, 2.5f, 2.38f, 0.46f, 28),
                ringX(4.02f, 2.56f, 2.36f, 0.06f, 28),
                ringX(3.56f, 2.42f, 2.34f, 0.05f, 24),
            ),
            material = Materials.brushedSteel,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "shroud_trim", name = "Shroud trim", system = "NACELLE", assembly = NACELLE,
            mesh = torusX(4.03f, 2.38f, 0.032f, 28, 4),
            material = Materials.copper,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "inlet_lip", name = "Inlet lip", system = "NACELLE", assembly = NACELLE,
            mesh = torusX(4.08f, 2.42f, 0.11f, 28, 4),
            material = Materials.titanium,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "fan_backplate", name = "Fan rear bulkhead", system = "NACELLE", assembly = NACELLE,
            mesh = Geo.merge(
                ringX(3.47f, 2.02f, 0.82f, 0.08f, 24),
                torusX(3.52f, 0.84f, 0.028f, 24, 4),
                torusX(3.52f, 2.0f, 0.022f, 24, 4),
            ),
            material = Materials.graphite,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "bypass_trough", name = "Bypass trough", system = "BYPASS", assembly = NACELLE,
            mesh = Geo.arcShell(2.27f, 2.15f, 2.55f, segments = 16, startAngle = -95f, sweepAngle = 190f)
                .transformed(Mat4.translation(2.55f, 0f, 0f).multiply(rotateToX())),
            material = Materials.graphite,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "bypass_edges", name = "Bypass cut edges", system = "BYPASS", assembly = NACELLE,
            mesh = run {
                val b = MeshBuilder()
                listOf(-95f, 95f).forEach { edgeAngle ->
                    val angle = Math.toRadians(edgeAngle.toDouble())
                    val y = (2.21f * cos(angle)).toFloat()
                    val z = (2.21f * sin(angle)).toFloat()
                    val points = (0..6).map { i -> V3(1.28f + 2.55f * i / 6f, y, z) }
                    b.addMesh(Geo.tube(points, 0.03f, 5, subdivide = 1, caps = true))
                }
                b.build()
            },
            material = Materials.copper,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "cowl_struts", name = "Cowling struts", system = "BYPASS", assembly = NACELLE,
            mesh = run {
                val b = MeshBuilder()
                listOf(30f, 150f, 210f, 330f).forEach { angle ->
                    val rotation = Mat4.rotateX(angle)
                    val normal = rotation.transformDirection(V3.Y)
                    b.addMesh(
                        Geo.cylinderBetween(
                            V3(3.52f, 0f, 0f) + normal * 2.32f,
                            V3(1.6f, 0f, 0f) + normal * 1.25f,
                            0.045f, 8,
                        ),
                    )
                }
                b.build()
            },
            material = Materials.titanium,
            explode = V3(0f, 1.2f, 0f),
        )
    )

    val panelMesh = MeshBuilder()
    panelMesh.addMesh(Geo.box(0.72f, 0.06f, 0.42f), Mat4.translation(V3(3.62f, 2.51f, 0f)))
    panelMesh.addMesh(Geo.box(0.58f, 0.025f, 0.3f), Mat4.translation(V3(3.62f, 2.55f, 0f)))
    parts.add(
        Part(
            id = "case_panels", name = "Access panel", system = "NACELLE", assembly = NACELLE,
            mesh = panelMesh.build(), material = Materials.darkSteel,
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "panel_bolts", name = "Panel fasteners", system = "NACELLE", assembly = NACELLE,
            mesh = Geo.merge(
                Geo.box(0.07f, 0.035f, 0.07f).transformed(Mat4.translation(V3(3.35f, 2.55f, -0.16f))),
                Geo.box(0.07f, 0.035f, 0.07f).transformed(Mat4.translation(V3(3.89f, 2.55f, 0.16f))),
            ),
            material = Materials.copper,
            explode = V3(0f, 1.5f, 0f),
        )
    )

    parts.add(
        Part(
            id = "sensor_pod", name = "Air data pod", system = "NACELLE", assembly = NACELLE,
            mesh = Geo.merge(
                Geo.sphere(0.16f, 14, 7),
                Geo.cylinder(0.03f, 0.24f, 8).transformed(Mat4.translation(V3(0f, -0.15f, 0f))),
            ),
            material = Materials.titanium, origin = V3(3.55f, 2.76f, 0f),
            explode = V3(0f, 1.5f, 0f),
        )
    )
    parts.add(
        Part(
            id = "sensor_dot", name = "Air data eye", system = "NACELLE", assembly = NACELLE,
            mesh = Geo.sphere(0.045f, 10, 6), material = Materials.emitCyan,
            origin = V3(3.55f, 2.9f, 0f),
            explode = V3(0f, 1.5f, 0f),
        )
    )

    parts.add(
        Part(
            id = "spinner", name = "Spinner", system = FAN, assembly = FAN,
            mesh = latheX(
                3.95f,
                listOf(
                    0.74f to 0f, 0.68f to 0.18f, 0.55f to 0.4f,
                    0.38f to 0.65f, 0.16f to 0.94f, 0.03f to 1.08f,
                ),
                32,
            ),
            material = Materials.titanium,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
            explode = V3(2.1f, 0f, 0f),
        )
    )
    parts.add(
        Part(
            id = "spinner_tip", name = "Spinner tip", system = FAN, assembly = FAN,
            mesh = latheX(
                4.94f,
                listOf(0.09f to 0f, 0.055f to 0.035f, 0.008f to 0.1f),
                20,
            ),
            material = Materials.copper,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
            explode = V3(2.1f, 0f, 0f),
        )
    )
    parts.add(
        Part(
            id = "spinner_band", name = "Spinner band", system = FAN, assembly = FAN,
            mesh = ringX(4.48f, 0.37f, 0.275f, 0.02f, 24),
            material = Materials.copper,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
            explode = V3(2.1f, 0f, 0f),
        )
    )
    parts.add(
        Part(
            id = "fan_hub", name = "Fan hub", system = FAN, assembly = FAN,
            mesh = Geo.merge(
                cylinderX(3.83f, 0.78f, 0.4f, 28, 0.05f),
                ringX(3.62f, 0.9f, 0.74f, 0.12f, 28),
                Geo.boltSet(
                    (0 until 6).map {
                        val a = Math.toRadians(it * 60.0)
                        V3(3.94f, (0.63f * cos(a)).toFloat(), (0.63f * sin(a)).toFloat())
                    },
                    0.035f, 0.05f, V3.X,
                ),
            ),
            material = Materials.darkSteel,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
            explode = V3(2.1f, 0f, 0f),
        )
    )

    val fanBlades = MeshBuilder()
    bladeRing(
        fanBlades,
        x = 3.66f,
        hubRadius = 0.74f,
        span = 1.43f,
        count = 18,
        chord = 1.0f,
        twist = 32f,
        thickness = 0.065f,
        sweep = 0.24f,
        sections = 4,
        perimeter = 5,
    )
    parts.add(
        Part(
            id = "fan_blades", name = "Fan blades", system = FAN, assembly = FAN,
            mesh = fanBlades.build(), material = Materials.titanium,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
            explode = V3(2.1f, 0f, 0f),
            doubleSided = true,
        )
    )
    parts.add(
        Part(
            id = "fan_tip_ring", name = "Blade tip ring", system = FAN, assembly = FAN,
            mesh = Geo.merge(
                torusX(3.66f, 2.19f, 0.038f, 28, 4),
                torusX(3.66f, 0.9f, 0.028f, 24, 4),
            ),
            material = Materials.copper,
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
            explode = V3(2.1f, 0f, 0f),
        )
    )

    parts.add(
        Part(
            id = "outlet_vanes", name = "Outlet guide vanes", system = "BYPASS", assembly = NACELLE,
            mesh = vaneRing(MeshBuilder(), 1.92f, 2.26f, 1.32f, 10, 0.34f, 18f),
            material = Materials.darkSteel,
            explode = V3(0f, 1.5f, 0f),
        )
    )

    parts.add(
        Part(
            id = "shaft", name = "HP shaft", system = CORE, assembly = CORE,
            mesh = cylinderX(-0.25f, 0.17f, 7.4f, 16, 0.02f),
            material = Materials.titanium,
        )
    )
    parts.add(
        Part(
            id = "bearings", name = "Bearings", system = CORE, assembly = CORE,
            mesh = Geo.merge(
                ringX(2.55f, 0.42f, 0.22f, 0.3f, 24),
                ringX(-1.9f, 0.38f, 0.2f, 0.26f, 24),
            ),
            material = Materials.brushedSteel,
        )
    )

    val compressorStages = listOf(
        CompressorStage("comp1", 1.22f, 0.62f, 0.47f, 16, 0.34f, 30f),
        CompressorStage("comp2", 0.72f, 0.56f, 0.4f, 18, 0.3f, 33f),
        CompressorStage("comp3", 0.22f, 0.5f, 0.34f, 20, 0.26f, 36f),
        CompressorStage("comp4", -0.28f, 0.44f, 0.3f, 22, 0.22f, 38f),
    )
    compressorStages.forEachIndexed { index, stage ->
        val mesh = MeshBuilder()
        mesh.addMesh(cylinderX(stage.x, stage.hubRadius, 0.12f, 20, 0.02f))
        mesh.addMesh(ringX(stage.x - 0.09f, stage.hubRadius + 0.03f, stage.hubRadius - 0.08f, 0.035f, 20))
        bladeRing(
            mesh,
            x = stage.x + 0.05f,
            hubRadius = stage.hubRadius,
            span = stage.span,
            count = stage.count,
            chord = stage.chord,
            twist = stage.twist,
            thickness = 0.035f,
            sweep = 0.03f,
            sections = 2,
            perimeter = 4,
        )
        parts.add(
            Part(
                id = stage.id,
                name = "Compressor stage ${index + 1}",
                system = COMPRESSOR,
                assembly = COMPRESSOR,
                mesh = mesh.build(),
                material = if (index % 2 == 0) Materials.titanium else Materials.brushedSteel,
                animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
                explode = V3(0f, 2.0f, 0f),
                doubleSided = true,
            )
        )
    }

    val stators = MeshBuilder()
    listOf(-0.02f, 0.55f, 1.1f).forEachIndexed { i, x ->
        stators.addMesh(vaneRing(MeshBuilder(), x, 1.14f, 0.86f - i * 0.1f, 8, 0.2f, -17f))
    }
    parts.add(
        Part(
            id = "compressor_stators", name = "Stator rows", system = COMPRESSOR, assembly = COMPRESSOR,
            mesh = stators.build(), material = Materials.graphite,
            explode = V3(0f, 2.0f, 0f),
        )
    )

    val compressorFrame = MeshBuilder()
    listOf(1.58f, 0.55f, -0.55f).forEach { x ->
        compressorFrame.addMesh(ringX(x, 1.26f, 1.16f, 0.1f, 24))
    }
    listOf(42f, 138f, 222f, 318f).forEach { angle ->
        val rotation = Mat4.rotateX(angle)
        val normal = rotation.transformDirection(V3.Y)
        compressorFrame.addMesh(
            Geo.slab(2.3f, 0.11f, 0.08f, 0.025f, 0f),
            Mat4.translation(normal * 1.22f + V3(0.5f, 0f, 0f)).multiply(rotation),
        )
    }
    parts.add(
        Part(
            id = "compressor_case", name = "Compressor frame", system = COMPRESSOR, assembly = COMPRESSOR,
            mesh = compressorFrame.build(), material = Materials.darkSteel,
            explode = V3(0f, 2.0f, 0f),
        )
    )

    parts.add(
        Part(
            id = "fuel_manifold", name = "Fuel manifold", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = torusX(0.1f, 1.36f, 0.045f, 28, 4),
            material = Materials.copper,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )
    val injectors = MeshBuilder()
    for (i in 0 until 12) {
        val angle = 360f * i / 12f
        val rotation = Mat4.rotateX(angle)
        val normal = rotation.transformDirection(V3.Y)
        injectors.addMesh(
            Geo.cylinderBetween(normal * 1.3f + V3(0.08f, 0f, 0f), normal * 0.8f + V3(0.16f, 0f, 0f), 0.035f, 6),
        )
        injectors.addMesh(
            Geo.cylinder(0.07f, 0.12f, 6)
                .transformed(Mat4.translation(normal * 0.58f + V3(0.22f, 0f, 0f)).multiply(Mat4.rotateX(90f))),
        )
    }
    parts.add(
        Part(
            id = "injectors", name = "Fuel injectors", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = injectors.build(), material = Materials.copper,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )

    val combustorCase = MeshBuilder()
    listOf(-0.44f, -1.12f).forEach { x ->
        combustorCase.addMesh(ringX(x, 1.18f, 1.09f, 0.24f, 24))
    }
    listOf(30f, 90f, 150f, 210f, 270f, 330f).forEach { angle ->
        val rotation = Mat4.rotateX(angle)
        val normal = rotation.transformDirection(V3.Y)
        combustorCase.addMesh(
            Geo.slab(1.02f, 0.11f, 0.06f, 0.025f, 0f),
            Mat4.translation(normal * 1.17f + V3(-0.75f, 0f, 0f)).multiply(rotation),
        )
    }
    parts.add(
        Part(
            id = "combustor_case", name = "Combustor case", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = combustorCase.build(), material = Materials.darkSteel,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )

    parts.add(
        Part(
            id = "combustor_liner", name = "Flame liner", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = ringX(-0.65f, 0.8f, 0.72f, 0.72f, 24),
            material = Materials.emitAmber,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )
    parts.add(
        Part(
            id = "combustor_bulkhead", name = "Injector bulkhead", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = ringX(-0.23f, 0.86f, 0.55f, 0.09f, 24),
            material = Materials.ceramic,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )
    parts.add(
        Part(
            id = "igniters", name = "Igniter plugs", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = Geo.merge(
                Geo.cylinderBetween(V3(-0.18f, 1.0f, 0.9f), V3(-0.26f, 0.7f, 0.6f), 0.07f, 8),
                Geo.cylinderBetween(V3(-0.18f, -1.0f, -0.9f), V3(-0.26f, -0.7f, -0.6f), 0.07f, 8),
            ),
            material = Materials.titanium,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )

    val fuelPipes = MeshBuilder()
    listOf(-1.0f, 1.0f).forEach { z ->
        fuelPipes.addMesh(
            Geo.tube(
                listOf(
                    V3(0.1f, -0.65f, z * 1.25f),
                    V3(0.1f, -1.1f, z * 1.1f),
                    V3(0.7f, -1.55f, z * 0.7f),
                    V3(1.3f, -1.7f, z * 0.3f),
                    V3(1.55f, -1.82f, 0f),
                ),
                0.042f, 6, subdivide = 3,
            ),
        )
    }
    parts.add(
        Part(
            id = "fuel_lines", name = "Fuel lines", system = COMBUSTOR, assembly = COMBUSTOR,
            mesh = fuelPipes.build(), material = Materials.copper,
            explode = V3(0f, 2.5f, 0.6f),
        )
    )

    val turbineStages = listOf(
        CompressorStage("turb1", -1.35f, 0.92f, 0.32f, 16, 0.3f, -38f),
        CompressorStage("turb2", -1.88f, 0.82f, 0.28f, 18, 0.27f, -36f),
        CompressorStage("turb3", -2.35f, 0.72f, 0.25f, 20, 0.24f, -34f),
    )
    turbineStages.forEachIndexed { index, stage ->
        val mesh = MeshBuilder()
        mesh.addMesh(cylinderX(stage.x, stage.hubRadius, 0.15f, 20, 0.025f))
        mesh.addMesh(ringX(stage.x - 0.11f, stage.hubRadius + 0.04f, stage.hubRadius - 0.1f, 0.045f, 20))
        bladeRing(
            mesh,
            x = stage.x + 0.05f,
            hubRadius = stage.hubRadius,
            span = stage.span,
            count = stage.count,
            chord = stage.chord,
            twist = stage.twist,
            thickness = 0.045f,
            sweep = 0.02f,
            sections = 2,
            perimeter = 4,
        )
        parts.add(
            Part(
                id = stage.id,
                name = "Turbine stage ${index + 1}",
                system = TURBINE,
                assembly = TURBINE,
                mesh = mesh.build(),
                material = if (index == 0) Materials.copper else Materials.darkSteel,
                animate = { ctx -> Mat4.rotateX(ctx.machineTime * 190f) },
                explode = V3(0f, -2.1f, 0f),
                doubleSided = true,
            )
        )
    }

    val turbineStators = MeshBuilder()
    listOf(-1.63f, -2.12f).forEach { x ->
        turbineStators.addMesh(vaneRing(MeshBuilder(), x, 1.14f, 0.78f, 12, 0.22f, 38f))
    }
    parts.add(
        Part(
            id = "turbine_stators", name = "Nozzle guide vanes", system = TURBINE, assembly = TURBINE,
            mesh = turbineStators.build(), material = Materials.graphite,
            explode = V3(0f, -2.1f, 0f),
        )
    )

    val turbineCase = MeshBuilder()
    listOf(-1.2f, -1.9f, -2.55f).forEach { x ->
        turbineCase.addMesh(ringX(x, 1.25f, 1.14f, 0.12f, 24))
    }
    listOf(20f, 140f, 260f).forEach { angle ->
        val rotation = Mat4.rotateX(angle)
        val normal = rotation.transformDirection(V3.Y)
        turbineCase.addMesh(
            Geo.slab(1.5f, 0.12f, 0.08f, 0.025f, 0f),
            Mat4.translation(normal * 1.22f + V3(-1.85f, 0f, 0f)).multiply(rotation),
        )
    }
    parts.add(
        Part(
            id = "turbine_case", name = "Turbine case", system = TURBINE, assembly = TURBINE,
            mesh = turbineCase.build(), material = Materials.darkSteel,
            explode = V3(0f, -2.1f, 0f),
        )
    )
    parts.add(
        Part(
            id = "turbine_heat_ring", name = "Heat shield ring", system = TURBINE, assembly = TURBINE,
            mesh = torusX(-1.17f, 1.03f, 0.035f, 28, 4),
            material = Materials.emitRed,
            explode = V3(0f, -2.1f, 0f),
        )
    )

    val nozzleProfile = listOf(
        1.02f to -2.62f,
        1.12f to -2.82f,
        0.98f to -3.05f,
        0.9f to -3.28f,
        1.02f to -3.62f,
        1.22f to -4.02f,
        1.46f to -4.5f,
        1.38f to -4.5f,
        1.15f to -4.02f,
        0.95f to -3.62f,
        0.84f to -3.28f,
        0.9f to -3.05f,
        1.02f to -2.82f,
        0.96f to -2.62f,
    )
    parts.add(
        Part(
            id = "nozzle", name = "Variable nozzle", system = NOZZLE, assembly = NOZZLE,
            mesh = Geo.merge(
                latheX(0f, nozzleProfile, 32),
                ringX(-2.62f, 1.02f, 0.95f, 0.05f, 24),
            ),
            material = Materials.darkSteel, doubleSided = true,
            explode = V3(-2.4f, 0f, 0f),
        )
    )

    val nozzleRings = MeshBuilder()
    listOf(-2.7f, -3.45f, -4.05f).forEachIndexed { i, x ->
        val r = when (i) {
            0 -> 1.1f
            1 -> 0.95f
            else -> 1.2f
        }
        nozzleRings.addMesh(ringX(x, r + 0.055f, r - 0.03f, 0.07f, 24))
    }
    parts.add(
        Part(
            id = "nozzle_rings", name = "Stiffener rings", system = NOZZLE, assembly = NOZZLE,
            mesh = nozzleRings.build(), material = Materials.copper,
            explode = V3(-2.4f, 0f, 0f),
        )
    )

    val chevrons = MeshBuilder()
    for (i in 0 until 12) {
        val angle = 360f * i / 12f
        val rotation = Mat4.rotateX(angle)
        val petal = Geo.box(0.55f, 0.045f, 0.3f)
        chevrons.addMesh(
            petal,
            Mat4.translation(V3(-4.72f, 0.72f, 0f))
                .multiply(rotation)
                .multiply(Mat4.translation(V3(0f, 0.75f, 0f)))
                .multiply(Mat4.rotateZ(-8f))
        )
    }
    parts.add(
        Part(
            id = "chevrons", name = "Serrated chevrons", system = NOZZLE, assembly = NOZZLE,
            mesh = chevrons.build(), material = Materials.titanium,
            explode = V3(-2.4f, 0f, 0f),
        )
    )

    val actuators = MeshBuilder()
    for (i in 0 until 3) {
        val angle = 45f + i * 120f
        val rotation = Mat4.rotateX(angle)
        val normal = rotation.transformDirection(V3.Y)
        val start = normal * 1.22f + V3(-2.78f, 0f, 0f)
        val end = normal * 1.02f + V3(-3.45f, 0f, 0f)
        actuators.addMesh(Geo.cylinderBetween(start, start + (end - start) * 0.55f, 0.11f, 14))
        actuators.addMesh(Geo.cylinderBetween(start + (end - start) * 0.5f, end, 0.045f, 10))
        actuators.addMesh(
            Geo.cylinder(0.14f, 0.13f, 16, bevel = 0.02f)
                .transformed(Mat4.translation(start).multiply(Mat4.rotateZ(-90f))),
        )
    }
    parts.add(
        Part(
            id = "nozzle_actuators", name = "Nozzle actuators", system = NOZZLE, assembly = NOZZLE,
            mesh = actuators.build(), material = Materials.brushedSteel,
            explode = V3(-2.4f, 0f, 0f),
        )
    )

    val flameHolder = MeshBuilder()
    flameHolder.addMesh(torusX(-3.55f, 0.58f, 0.035f, 24, 4))
    for (i in 0 until 8) {
        val angle = 360f * i / 8f
        val rotation = Mat4.rotateX(angle)
        flameHolder.addMesh(
            Geo.box(0.035f, 0.5f, 0.08f),
            Mat4.translation(V3(-3.55f, 0.34f, 0f)).multiply(rotation),
        )
    }
    parts.add(
        Part(
            id = "flame_holder", name = "Flame holder", system = NOZZLE, assembly = NOZZLE,
            mesh = flameHolder.build(), material = Materials.copper,
            explode = V3(-2.4f, 0f, 0f),
        )
    )

    parts.add(
        Part(
            id = "nozzle_glow", name = "Afterburner glow", system = NOZZLE, assembly = NOZZLE,
            mesh = torusX(-3.3f, 0.6f, 0.1f, 24, 4), material = Materials.emitAmber,
            explode = V3(-2.4f, 0f, 0f),
        )
    )

    val gearbox = MeshBuilder()
    gearbox.addMesh(Geo.slab(1.35f, 0.95f, 0.72f, 0.16f, 0.05f), Mat4.translation(V3(0f, 0f, 0f)))
    gearbox.addMesh(Geo.slab(1.05f, 0.7f, 0.06f, 0.08f, 0.015f), Mat4.translation(V3(0f, 0.39f, 0f)))
    for (i in 0 until 4) {
        gearbox.addMesh(
            Geo.hexBolt(0.045f, 0.06f),
            Mat4.translation(V3(-0.5f + (i % 2) * 1.0f, 0.43f, -0.26f + (i / 2) * 0.52f)),
        )
    }
    parts.add(
        Part(
            id = "gearbox", name = "Accessory gearbox", system = ACCESSORY, assembly = ACCESSORY,
            mesh = gearbox.build(), material = Materials.brushedSteel,
            origin = V3(1.55f, -1.72f, 0f),
            explode = V3(0f, -1.6f, 1.2f),
        )
    )
    parts.add(
        Part(
            id = "generator", name = "Generator", system = ACCESSORY, assembly = ACCESSORY,
            mesh = Geo.merge(
                cylinderX(0f, 0.22f, 0.8f, 16, 0.03f),
                ringX(0.32f, 0.25f, 0.2f, 0.08f, 16),
                ringX(-0.32f, 0.25f, 0.2f, 0.08f, 16),
            ),
            material = Materials.darkSteel,
            origin = V3(2.65f, -1.72f, 0.35f),
            explode = V3(0f, -1.6f, 1.2f),
        )
    )
    parts.add(
        Part(
            id = "accessory_sensor", name = "Vibration sensor", system = ACCESSORY, assembly = ACCESSORY,
            mesh = Geo.sphere(0.06f, 12, 8), material = Materials.emitCyan,
            origin = V3(1.2f, -1.32f, 0.42f),
            explode = V3(0f, -1.6f, 1.2f),
        )
    )

    val harness = MeshBuilder()
    harness.addMesh(
        Geo.tube(
            listOf(
                V3(2.1f, 1.4f, 0.9f),
                V3(2.4f, 1.8f, 0.5f),
                V3(1.9f, 2.1f, -0.4f),
                V3(0.4f, 2.15f, -0.8f),
                V3(-1.1f, 1.8f, -1.1f),
            ),
            0.045f, 6, subdivide = 3,
        ),
    )
    harness.addMesh(
        Geo.tube(
            listOf(
                V3(0.4f, 2.15f, -0.8f),
                V3(0.0f, 1.95f, -1.35f),
                V3(-1.9f, 1.4f, -1.3f),
            ),
            0.03f, 6, subdivide = 3,
        ),
    )
    parts.add(
        Part(
            id = "harness", name = "Engine harness", system = ACCESSORY, assembly = ACCESSORY,
            mesh = harness.build(), material = Materials.rubber,
            explode = V3(0f, -1.2f, 0.6f),
        )
    )
    parts.add(
        Part(
            id = "harness_box", name = "EEC unit", system = ACCESSORY, assembly = ACCESSORY,
            mesh = Geo.slab(0.85f, 0.6f, 0.24f, 0.07f, 0.02f), material = Materials.graphite,
            origin = V3(0.6f, 2.05f, -0.9f),
            explode = V3(0f, -1.2f, 0.6f),
        )
    )

    val bleedPipes = MeshBuilder()
    bleedPipes.addMesh(
        Geo.tube(
            listOf(
                V3(1.75f, 1.12f, 0f),
                V3(1.2f, 1.55f, 0.3f),
                V3(0.2f, 1.7f, 0.4f),
                V3(-1.1f, 1.4f, 0.3f),
                V3(-1.8f, 1.2f, 0f),
            ),
            0.058f, 6, subdivide = 3,
        ),
    )
    bleedPipes.addMesh(
        Geo.tube(
            listOf(
                V3(1.7f, -1.1f, -0.2f),
                V3(0.8f, -1.55f, -0.6f),
                V3(-0.4f, -1.55f, -0.7f),
                V3(-1.5f, -1.2f, -0.4f),
            ),
            0.05f, 6, subdivide = 3,
        ),
    )
    parts.add(
        Part(
            id = "bleed_lines", name = "Bleed lines", system = ACCESSORY, assembly = ACCESSORY,
            mesh = bleedPipes.build(), material = Materials.brushedSteel,
            explode = V3(0f, -1.2f, 0.6f),
        )
    )

    val clampPoints = MeshBuilder()
    listOf(1.9f, -1.0f).forEach { x ->
        clampPoints.addMesh(ringX(x, 1.12f, 1.04f, 0.06f, 20))
    }
    parts.add(
        Part(
            id = "line_clamps", name = "Line clamps", system = ACCESSORY, assembly = ACCESSORY,
            mesh = clampPoints.build(), material = Materials.titanium,
            explode = V3(0f, -1.2f, 0.6f),
        )
    )

    val consoleBase = MeshBuilder()
    consoleBase.addMesh(Geo.slab(1.0f, 0.78f, 0.5f, 0.12f, 0.05f), Mat4.translation(V3(-3.9f, deckTop + 0.27f, 1.02f)))
    consoleBase.addMesh(Geo.slab(0.84f, 0.62f, 0.08f, 0.08f, 0.02f), Mat4.translation(V3(-3.9f, deckTop + 0.55f, 1.02f)))
    consoleBase.addMesh(Geo.cylinder(0.11f, 0.18f, 16), Mat4.translation(V3(-3.9f, deckTop + 0.04f, 1.02f)))
    parts.add(
        Part(
            id = "console_base", name = "Operator console", system = "CONTROL", assembly = STAND,
            mesh = consoleBase.build(), material = Materials.graphite, origin = V3.ZERO
        )
    )
    val consoleScreen = MeshBuilder()
    consoleScreen.addMesh(
        Geo.slab(0.72f, 0.46f, 0.05f, 0.06f, 0.015f),
        Mat4.translation(V3(-3.9f, deckTop + 0.66f, 1.01f)).multiply(Mat4.rotateX(62f)),
    )
    parts.add(
        Part(
            id = "console_screen", name = "Diagnostics display", system = "CONTROL", assembly = STAND,
            mesh = consoleScreen.build(), material = Materials.glassDark, origin = V3.ZERO,
            doubleSided = true,
        )
    )
    val consoleLights = MeshBuilder()
    for (i in 0 until 3) {
        consoleLights.addMesh(
            Geo.box(0.06f, 0.012f, 0.06f),
            Mat4.translation(V3(-4.12f + i * 0.16f, deckTop + 0.715f, 1.16f)),
        )
    }
    consoleLights.addMesh(
        Geo.box(0.42f, 0.01f, 0.026f),
        Mat4.translation(V3(-3.9f, deckTop + 0.69f, 1.11f)).multiply(Mat4.rotateX(62f)),
    )
    parts.add(
        Part(
            id = "console_lights", name = "Console indicators", system = "CONTROL", assembly = STAND,
            mesh = consoleLights.build(), material = Materials.emitCyan, origin = V3.ZERO
        )
    )
    parts.add(
        Part(
            id = "console_cable", name = "Console umbilical", system = "CONTROL", assembly = STAND,
            mesh = Geo.tube(
                listOf(
                    V3(-4.3f, deckTop + 0.3f, 0.75f),
                    V3(-4.5f, deckTop + 0.1f, 0.3f),
                    V3(-3.9f, deckTop + 0.02f, -0.7f),
                    V3(-2.7f, deckTop + 0.1f, -1.1f),
                ),
                0.035f, 6, subdivide = 4,
            ),
            material = Materials.rubber, origin = V3.ZERO
        )
    )

    parts.add(
        Part(
            id = "lifting_lug", name = "Lifting lug", system = "MOUNT", assembly = STAND,
            mesh = Geo.merge(
                Geo.slab(0.5f, 0.18f, 0.7f, 0.07f, 0.02f),
                Geo.hollowCylinder(0.16f, 0.08f, 0.2f, 16)
                    .transformed(Mat4.rotateZ(90f)),
                Geo.cylinder(0.06f, 1.2f, 10).transformed(Mat4.translation(V3(0f, -0.7f, 0f))),
            ),
            material = Materials.titanium,
            origin = V3(-0.35f, 2.5f, 0f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "fan", "18 wide-chord fan blades",
            "Titanium blades move 1.2 tonnes of air per second. The fan produces most of the thrust through the bypass duct, while the core handles combustion.",
            "fan_blades", V3(0.1f, 1.55f, 0f),
            CameraPose(58f, 14f, 1.05f), readoutLabel = "FAN",
        ),
        Hotspot(
            "compressor", "Four-stage compressor",
            "Successive rotor and stator rows squeeze the air to more than 30 times atmospheric pressure before it reaches the combustor.",
            "compressor_case", V3(0.2f, 1.3f, 0.4f),
            CameraPose(40f, 30f, 1.15f), readoutLabel = "HP COMPRESSOR",
        ),
        Hotspot(
            "combustor", "Annular combustor",
            "Twelve fuel injectors feed a continuous ring of flame inside the liner. The flame never touches the casing; cooling air forms a protective film around it.",
            "combustor_case", V3(-0.7f, 1.35f, 0.3f),
            CameraPose(36f, 20f, 1.12f), readoutLabel = "COMBUSTOR",
        ),
        Hotspot(
            "turbine", "Three-stage turbine",
            "Hot gas expands across the turbine blades, spinning the shaft that drives the fan and compressor. Each stage extracts a little more energy from the flow.",
            "turbine_case", V3(-1.8f, 1.35f, 0f),
            CameraPose(28f, 22f, 1.12f), readoutLabel = "TURBINE",
        ),
        Hotspot(
            "nozzle", "Variable exhaust nozzle",
            "The convergent-divergent nozzle accelerates the exhaust. Three hydraulic actuators trim the throat for max dry thrust or afterburner operation.",
            "nozzle_rings", V3(-4.05f, 1.1f, 0f),
            CameraPose(24f, 16f, 1.0f), explode = 0.25f, readoutLabel = "NOZZLE",
        ),
        Hotspot(
            "gearbox", "Accessory gearbox",
            "A tower shaft taps the HP spool to drive the generator, hydraulic pump and fuel pumps. The whole pack is removable as one line-replaceable unit.",
            "gearbox", V3(0.5f, 0.5f, 0.4f),
            CameraPose(48f, 32f, 1.3f), readoutLabel = "ACCESSORIES",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Intake and bypass",
            "The inlet lip smooths the air into the fan. Eighteen blades split it: a large bypass stream leaves through the annular duct, while the core stream turns inward.",
            FAN, CameraPose(58f, 12f, 1.0f), 0f, 0.8f,
        ),
        GuideStep(
            "Compression",
            "Four rotor stages and their stators trade speed for pressure. By the last stage the air is hot, dense and ready for fuel.",
            COMPRESSOR, CameraPose(42f, 30f, 1.18f), 0.15f, 0.9f,
        ),
        GuideStep(
            "Combustion",
            "Fuel leaves twelve nozzles into a recirculating swirl. The flame liner glows amber; the surrounding case stays cool enough to touch.",
            COMBUSTOR, CameraPose(34f, 16f, 1.22f), 0.35f, 1f,
        ),
        GuideStep(
            "Turbine work",
            "The hot gas turns three turbine stages. Each disc is connected to the shaft that drives the compressor and fan; the nozzle guide vanes set the angle of attack.",
            TURBINE, CameraPose(26f, 18f, 1.16f), 0.2f, 0.95f,
        ),
        GuideStep(
            "Exhaust and thrust",
            "After the turbine, the flow is accelerated again by the nozzle. The afterburner adds fuel behind the turbine when extra thrust is needed.",
            NOZZLE, CameraPose(20f, 12f, 1.0f), 0.1f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Every module separates for maintenance: fan, compressor, combustor, turbine, nozzle and accessories. Take the engine apart to see how the shaft ties them together.",
            null, CameraPose(40f, 28f, 1.02f), 1f, 0.7f,
        ),
    )

    return ObjectSpec(
        id = "turbofan",
        index = 1,
        title = "VORTEX TF-9",
        designation = "TF-9 / HIGH-BYPASS TURBOFAN",
        category = "Aerospace",
        categoryShort = "AERO",
        tagline = "A turbofan opened to its bones.",
        summary = "A high-bypass afterburning turbofan, sectioned to reveal the fan, four compressor stages, annular combustor, three turbine stages and variable nozzle.",
        details = listOf(
            "The TF-9 is built around a single HP spool: the turbine drives both the compressor and the fan through a reduction gear. Its bypass ratio of 8.4 means most of the air never enters the core, which is why a turbofan is quiet and efficient at subsonic speeds.",
            "Every visible module is a real assembly. The fan, compressor, combustor and turbine separate along their true maintenance joints, and the variable nozzle is driven by three actuators around the exhaust case.",
        ),
        specs = listOf(
            SpecRow("Bypass ratio", "8.4 : 1"),
            SpecRow("Fan diameter", "2.46 m"),
            SpecRow("Max thrust", "142 kN"),
            SpecRow("Pressure ratio", "32 : 1"),
            SpecRow("Turbine inlet", "1,480 °C"),
        ),
        tags = listOf("aerospace", "turbofan", "jet engine", "propulsion", "afterburner"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "THROTTLE",
            lowLabel = "IDLE",
            highLabel = "MAX",
            defaultValue = 0.62f,
            hint = "Fan speed, fuel flow and afterburner",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(46f, 24f, 1.32f),
        thumbnailPose = CameraPose(48f, 24f, 1.32f),
        thumbnailTime = 3.4f,
        thumbnailControl = 0.28f,
        readouts = { ctx ->
            val thrust = 3f + 139f * ctx.control
            val n1 = 21f + 72f * ctx.control
            val egt = 380f + 500f * ctx.control
            listOf(
                Readout("THRUST", String.format("%.1f", thrust), "kN", accent = true),
                Readout("N1", "${n1.toInt()}", "%"),
                Readout("EGT", "${egt.toInt()}", "°C", warn = egt > 800f),
            )
        },
        effects = { ctx ->
            val intensity = ctx.control
            val plume = SceneEffect.Plume(
                origin = V3(-4.82f, 0f, 0f),
                direction = V3(-1f, 0f, 0f),
                length = 1.5f + intensity * 3.4f,
                radius = 0.55f + intensity * 0.4f,
                intensity = (0.25f + intensity * 0.95f).coerceIn(0f, 1f),
                palette = PlumePalette.PLASMA,
            )
            listOf(
                plume,
                SceneEffect.Glow(V3(-0.72f, 0f, 0f), 0.95f, 0xFFFFB25C.toInt(), 0.32f + 0.4f * intensity),
                SceneEffect.Glow(V3(-1.28f, 0f, 0f), 0.55f, 0xFFFF7A4A.toInt(), 0.22f + 0.5f * intensity),
                SceneEffect.Glow(V3(-3.4f, 0f, 0f), 0.6f, 0xFF6EF2DF.toInt(), 0.15f * intensity),
            )
        },
    )
}

private data class CompressorStage(
    val id: String,
    val x: Float,
    val hubRadius: Float,
    val span: Float,
    val count: Int,
    val chord: Float,
    val twist: Float,
)
