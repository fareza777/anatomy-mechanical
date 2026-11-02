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
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder
import kotlin.math.cos
import kotlin.math.sin

private const val TT_FRAME = "FRAME"
private const val TT_ROTOR = "ROTOR"
private const val TT_CASING = "CASING"
private const val TT_GEN = "GENERATOR"
private const val TT_BASE = "BASE"
private const val TT_STACK = "DISC STACK"
private const val TT_SHAFT = "SHAFT"
private const val TT_INLET = "INLET"
private const val TT_OUT = "EXHAUST"

private const val TT_AXIS_Y = 1.02f

private fun discSet(xs: List<Float>, radius: Float): Mesh = MeshBuilder().apply {
    xs.forEach { x ->
        addMesh(
            Geo.cylinder(radius, 0.016f, 36).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
        addMesh(
            Geo.torus(radius - 0.01f, 0.008f, 32, 4).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
        addMesh(
            Geo.cylinder(0.10f, 0.055f, 18).transformed(Mat4.rotateZ(90f)),
            Mat4.translation(V3(x, 0f, 0f)),
        )
    }
}.build()

private fun ringX(x: Float, radius: Float, minor: Float): Mesh =
    Geo.torus(radius, minor, 34, 6).transformed(Mat4.translation(V3(x, 0f, 0f)).multiply(Mat4.rotateZ(90f)))

fun teslaTurbine(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "tesla",
            width = 2.9f,
            depth = 1.7f,
            top = 0.3f,
            system = TT_FRAME,
            assembly = TT_BASE,
            accentMaterial = Materials.emitAmber,
        ),
    )

    parts.add(
        Part(
            id = "tt_pedestals", name = "Bearing pedestals", system = TT_FRAME, assembly = TT_BASE,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.slab(0.34f, 0.54f, 0.64f, 0.05f, 0.02f),
                        Mat4.translation(V3(side * 0.88f, 0.62f, 0f)),
                    )
                    addMesh(
                        Geo.slab(0.46f, 0.72f, 0.08f, 0.05f, 0.02f),
                        Mat4.translation(V3(side * 0.88f, 0.34f, 0f)),
                    )
                    addMesh(
                        Geo.cylinder(0.19f, 0.20f, 20).transformed(Mat4.rotateZ(90f)),
                        Mat4.translation(V3(side * 0.88f, TT_AXIS_Y, 0f)),
                    )
                }
            }.build(),
            material = Materials.paintSlate,
        ),
    )
    parts.add(
        Part(
            id = "tt_bearings", name = "Journal bearings", system = TT_FRAME, assembly = TT_SHAFT,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinder(0.115f, 0.26f, 20).transformed(Mat4.rotateZ(90f)),
                        Mat4.translation(V3(side * 0.88f, TT_AXIS_Y, 0f)),
                    )
                    listOf(-0.08f, 0.08f).forEach { d ->
                        addMesh(
                            Geo.torus(0.12f, 0.012f, 20, 4).transformed(Mat4.rotateZ(90f)),
                            Mat4.translation(V3(side * (0.88f + d), TT_AXIS_Y, 0f)),
                        )
                    }
                }
            }.build(),
            material = Materials.nickel,
        ),
    )

    parts.add(
        Part(
            id = "tt_shaft", name = "Turbine shaft", system = TT_ROTOR, assembly = TT_SHAFT,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinderBetween(V3(-1.22f, 0f, 0f), V3(1.22f, 0f, 0f), 0.055f, 14))
                addMesh(Geo.box(0.90f, 0.018f, 0.015f), Mat4.translation(V3(0f, 0.045f, 0f)))
                addMesh(Geo.torus(0.075f, 0.012f, 20, 4).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(0.90f, 0f, 0f)))
            }.build(),
            material = Materials.nickel, origin = V3(0f, TT_AXIS_Y, 0f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * (30f + 360f * ctx.control)) },
        ),
    )

    val spinA = { ctx: studio.anatomy.app.model.AnimContext ->
        Mat4.rotateX(ctx.machineTime * (30f + 360f * ctx.control))
    }
    parts.add(
        Part(
            id = "tt_discs_a", name = "Rotor discs A", system = TT_ROTOR, assembly = TT_STACK,
            mesh = discSet(listOf(-0.36f, -0.18f, 0f, 0.18f, 0.36f), 0.52f),
            material = Materials.brass, origin = V3(0f, TT_AXIS_Y, 0f), animate = spinA,
        ),
    )
    parts.add(
        Part(
            id = "tt_discs_b", name = "Rotor discs B", system = TT_ROTOR, assembly = TT_STACK,
            mesh = discSet(listOf(-0.45f, -0.27f, -0.09f, 0.09f, 0.27f, 0.45f), 0.50f),
            material = Materials.darkSteel, origin = V3(0f, TT_AXIS_Y, 0f), animate = spinA,
        ),
    )

    parts.add(
        Part(
            id = "tt_frame", name = "Casing frame", system = TT_CASING, assembly = TT_CASING,
            mesh = MeshBuilder().apply {
                listOf(-0.60f, 0f, 0.60f).forEach { x -> addMesh(ringX(x, 0.635f, 0.026f)) }
                for (i in 0 until 4) {
                    val a = Math.toRadians(45.0 + i * 90.0)
                    val y = (0.635f * cos(a)).toFloat()
                    val z = (0.635f * sin(a)).toFloat()
                    addMesh(Geo.cylinderBetween(V3(-0.62f, y, z), V3(0.62f, y, z), 0.023f, 8))
                }
                addMesh(Geo.box(0.50f, 0.045f, 0.72f), Mat4.translation(V3(0f, 0.660f, 0f)))
            }.build(),
            material = Materials.darkSteel, origin = V3(0f, TT_AXIS_Y, 0f),
        ),
    )

    parts.add(
        Part(
            id = "tt_inlet", name = "Tangential inlet", system = TT_CASING, assembly = TT_INLET,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0f, 0.90f, 0f),
                            V3(0f, 0.60f, 0f),
                            V3(0.26f, 0.36f, 0.28f),
                            V3(0.52f, 0.12f, 0.38f),
                        ),
                        0.095f, 10, subdivide = 3,
                    ),
                )
                addMesh(Geo.torus(0.105f, 0.018f, 22, 5), Mat4.translation(V3(0f, 0.88f, 0f)))
                addMesh(Geo.torus(0.105f, 0.018f, 22, 5), Mat4.translation(V3(0.50f, 0.12f, 0.37f)))
            }.build(),
            material = Materials.copper,
        ),
    )
    parts.add(
        Part(
            id = "tt_valve", name = "Inlet valve", system = TT_CASING, assembly = TT_INLET,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.165f, 0.022f, 26, 6), Mat4.translation(V3(0f, 0.02f, 0f)))
                for (i in 0 until 3) {
                    addMesh(
                        Geo.cylinder(0.012f, 0.30f, 8).transformed(Mat4.rotateZ(90f)),
                        Mat4.translation(V3(0f, 0.02f, 0f)).multiply(Mat4.rotateY(i * 60f)),
                    )
                }
                addMesh(Geo.cylinder(0.040f, 0.10f, 12), Mat4.translation(V3(0f, -0.03f, 0f)))
            }.build(),
            material = Materials.paintOrange, origin = V3(0f, 1.94f, 0f),
        ),
    )

    parts.add(
        Part(
            id = "tt_outlet", name = "Exhaust duct", system = TT_CASING, assembly = TT_OUT,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.90f, 0f, 0f),
                            V3(1.34f, 0f, 0f),
                            V3(1.52f, -0.22f, 0f),
                            V3(1.56f, -0.52f, 0f),
                        ),
                        0.115f, 10, subdivide = 3,
                    ),
                )
                addMesh(Geo.torus(0.125f, 0.020f, 22, 5), Mat4.translation(V3(1.56f, -0.52f, 0f)))
            }.build(),
            material = Materials.darkSteel, origin = V3(0f, TT_AXIS_Y, 0f),
        ),
    )

    parts.add(
        Part(
            id = "tt_generator", name = "Generator housing", system = TT_GEN, assembly = TT_GEN,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.27f, 0.48f, 26, bevel = 0.02f).transformed(Mat4.rotateZ(90f)))
                for (i in 0 until 6) {
                    addMesh(
                        Geo.torus(0.272f, 0.012f, 26, 4).transformed(Mat4.rotateZ(90f)),
                        Mat4.translation(V3(-0.22f + i * 0.09f, 0f, 0f)),
                    )
                }
                addMesh(Geo.box(0.20f, 0.16f, 0.20f), Mat4.translation(V3(0f, 0.28f, 0f)))
                addMesh(
                    Geo.tube(
                        listOf(V3(-0.20f, 0.30f, 0f), V3(-0.24f, 0.52f, -0.10f), V3(-0.24f, 0.54f, -0.42f)),
                        0.022f, 6, subdivide = 2,
                    ),
                )
            }.build(),
            material = Materials.paintSlate, origin = V3(-1.26f, TT_AXIS_Y, 0f),
        ),
    )
    parts.add(
        Part(
            id = "tt_gen_light", name = "Generator status ring", system = TT_GEN, assembly = TT_GEN,
            mesh = Geo.torus(0.278f, 0.012f, 28, 4).transformed(Mat4.rotateZ(90f)),
            material = Materials.emitAmber, parent = "tt_generator", origin = V3(0.24f, 0f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "tt_terminal", name = "Terminal box", system = TT_GEN, assembly = TT_GEN,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.26f, 0.20f, 0.16f, 0.02f, 0.008f), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.box(0.10f, 0.02f, 0.10f), Mat4.translation(V3(0f, 0.09f, 0f)))
            }.build(),
            material = Materials.graphite, origin = V3(-1.26f, 1.62f, 0f),
        ),
    )

    parts.add(
        Part(
            id = "tt_nameplate", name = "Nameplate", system = TT_FRAME, assembly = TT_BASE,
            mesh = Geo.box(0.30f, 0.012f, 0.16f),
            material = Materials.labelPaper, origin = V3(0.88f, 0.965f, 0.24f),
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "discs", "Rotor disc stack",
            "Eleven flat discs spin a few millimetres apart. Steam enters at the rim and spirals inward, dragging each disc by skin friction rather than by impulse.",
            "tt_discs_a", V3(0.05f, 0f, 0.30f),
            CameraPose(20f, 8f, 1.35f), readoutLabel = "ROTOR",
        ),
        Hotspot(
            "shaft", "Shaft and bearings",
            "The shaft carries the rotor and the generator on the same axis. Two journal bearings take the load, and the rotor has no blades to break off.",
            "tt_shaft", V3(0.55f, 0.05f, 0.02f),
            CameraPose(46f, 12f, 1.28f), readoutLabel = "ROTATION",
        ),
        Hotspot(
            "inlet", "Tangential inlet",
            "The inlet aims the steam along the rim, not at the discs. That preserves the angular momentum the discs are about to extract.",
            "tt_inlet", V3(0.24f, 0.40f, 0.28f),
            CameraPose(64f, 20f, 1.28f), readoutLabel = "WORKING FLUID",
        ),
        Hotspot(
            "generator", "Generator",
            "The generator hangs off the same shaft. A directly coupled machine avoids a gearbox and its losses, but it must spin fast to make useful voltage.",
            "tt_generator", V3(0.06f, 0.04f, 0.24f),
            CameraPose(58f, 18f, 1.30f), readoutLabel = "OUTPUT",
        ),
        Hotspot(
            "outlet", "Exhaust duct",
            "Spent steam leaves at the centre, where the discs have already given up their angular momentum. A wide, short duct keeps the back pressure low.",
            "tt_outlet", V3(0.20f, -0.20f, 0.10f),
            CameraPose(40f, 12f, 1.32f), readoutLabel = "EXHAUST",
        ),
        Hotspot(
            "frame", "Tie-rod frame",
            "Four tie rods hold the end rings apart. The frame is open on purpose: a bladeless turbine needs no sealed casing to demonstrate its principle.",
            "tt_frame", V3(0.40f, 0.45f, 0.35f),
            CameraPose(34f, 22f, 1.20f), readoutLabel = "STRUCTURE",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Admit the steam",
            "Steam enters tangentially and starts to spiral. The inlet geometry decides the swirl, and the swirl is the energy the rotor will take out.",
            TT_INLET, CameraPose(36f, 18f, 1.08f), 0.15f, 0.45f,
        ),
        GuideStep(
            "Drag the discs",
            "The steam does not hit blades. It drags on the faces of the spinning discs through viscosity, and the boundary layer does the work.",
            TT_ROTOR, CameraPose(24f, 10f, 1.28f), 0.10f, 0.65f,
        ),
        GuideStep(
            "Give up the swirl",
            "The spiral tightens toward the centre as the steam gives up angular momentum. It leaves through the centre, slower and cooler.",
            TT_ROTOR, CameraPose(18f, 6f, 1.35f), 0.05f, 0.85f,
        ),
        GuideStep(
            "Make electricity",
            "The shaft drives the generator directly. No gearbox, no blades: just a very fast, very smooth rotation and a lot of heat to manage.",
            TT_GEN, CameraPose(62f, 18f, 1.26f), 0.20f, 1f,
        ),
        GuideStep(
            "Keep it cool",
            "The generator fins and the open frame shed heat. The clearance between discs is the critical dimension; thermal growth closes it.",
            TT_CASING, CameraPose(42f, 24f, 1.16f), 0.0f, 0.7f,
        ),
        GuideStep(
            "The assembly",
            "Explode the turbine to see the pedestals, bearings, shaft, eleven discs, the tie-rod frame, the inlet and the generator. The discs are the only moving parts.",
            null, CameraPose(36f, 22f, 0.96f), 1f, 0.6f,
        ),
    )

    return ObjectSpec(
        id = "tesla_turbine",
        index = 16,
        title = "TESLA R-3",
        designation = "TR-3 / BLADELESS TURBINE",
        category = "Energy",
        categoryShort = "NRG",
        tagline = "No blades. Just friction and patience.",
        summary = "A bladeless Tesla turbine: eleven flat discs on a single shaft, a tangential steam inlet, an open tie-rod frame and a direct-coupled generator.",
        details = listOf(
            "TESLA R-3 has no blades. The working fluid drags on a stack of flat discs, and the boundary layer between the steam and the steel is what turns the shaft.",
            "It is smooth, quiet and tolerant of wet steam, but it needs high flow and tight clearances. The open frame is a teaching aid: you can see every disc and every bearing.",
        ),
        specs = listOf(
            SpecRow("Rotor", "11 discs"),
            SpecRow("Diameter", "1.04 m"),
            SpecRow("Speed", "38,000 rpm"),
            SpecRow("Output", "120 kW"),
            SpecRow("Efficiency", "78 %"),
        ),
        tags = listOf("tesla", "turbine", "bladeless", "steam", "generator", "energy"),
        accent = 0xFFFFC46B.toInt(),
        control = ControlSpec(
            label = "FLOW",
            lowLabel = "IDLE",
            highLabel = "FULL",
            defaultValue = 0.6f,
            hint = "Steam flow, rotor speed and generator load",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(32f, 16f, 1.08f),
        thumbnailPose = CameraPose(34f, 18f, 1.10f),
        thumbnailTime = 2.3f,
        thumbnailControl = 0.8f,
        readouts = { ctx ->
            listOf(
                Readout("SPEED", String.format("%.0f", 38000f * ctx.control), "rpm", accent = true),
                Readout("OUTPUT", String.format("%.0f", 18f + 102f * ctx.control), "kW"),
                Readout("EFFICIENCY", String.format("%.0f", 62f + 16f * ctx.control), "%"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Ring(V3(0f, TT_AXIS_Y, 0f), 0.74f, 0xFFFFC46B.toInt(), ctx.machineTime * 0.3f, 0.22f),
                SceneEffect.Glow(V3(-1.26f, TT_AXIS_Y, 0f), 0.30f, 0xFFFFC46B.toInt(), 0.16f + 0.3f * ctx.control),
                SceneEffect.Flow(
                    points = listOf(
                        V3(0f, 2.10f, 0f),
                        V3(0f, 1.60f, 0f),
                        V3(0.36f, 1.30f, 0.30f),
                        V3(0.55f, 1.12f, 0.40f),
                    ),
                    color = 0xFFFFC46B.toInt(),
                    count = 8,
                    speed = 0.3f + 0.4f * ctx.control,
                    phase = 0.15f,
                    dotRadius = 0.024f,
                ),
            )
        },
    )
}
