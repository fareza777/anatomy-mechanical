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

private const val SP_FRAME = "FRAME"
private const val SP_DRIVE = "DRIVE"
private const val SP_BEARING = "BEARINGS"
private const val SP_COOL = "COOLANT"
private const val SP_BASE = "BASE"
private const val SP_COLUMN = "COLUMN"
private const val SP_CARTRIDGE = "CARTRIDGE"
private const val SP_TOOL = "TOOL"

private const val SP_HOUSING_Y = 1.48f

fun spindle(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "vector",
            width = 1.5f,
            depth = 1.5f,
            top = 0.3f,
            system = SP_FRAME,
            assembly = SP_BASE,
            accentMaterial = Materials.emitCyan,
        ),
    )

    parts.add(
        Part(
            id = "sp_column", name = "C-frame column", system = SP_FRAME, assembly = SP_COLUMN,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.34f, 0.42f, 1.62f, 0.05f, 0.02f, 2), Mat4.translation(V3(0f, 1.12f, -0.52f)))
                addMesh(Geo.slab(0.60f, 0.56f, 0.10f, 0.06f, 0.02f), Mat4.translation(V3(0f, 0.33f, -0.52f)))
                addMesh(Geo.slab(0.40f, 0.92f, 0.24f, 0.05f, 0.02f), Mat4.translation(V3(0f, 1.95f, -0.14f)))
                addMesh(Geo.slab(0.36f, 0.34f, 0.22f, 0.04f, 0.015f), Mat4.translation(V3(0f, 1.80f, 0.04f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.box(0.035f, 1.30f, 0.30f),
                        Mat4.translation(V3(side * 0.18f, 1.10f, -0.52f)),
                    )
                }
            }.build(),
            material = Materials.paintSlate,
        ),
    )

    parts.add(
        Part(
            id = "sp_housing", name = "Spindle cartridge", system = SP_DRIVE, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.lathe(
                        listOf(
                            0.14f to -0.36f, 0.19f to -0.31f, 0.19f to -0.08f,
                            0.245f to -0.02f, 0.245f to 0.14f, 0.20f to 0.20f,
                            0.20f to 0.30f, 0.12f to 0.36f,
                        ),
                        30, capBottom = true, capTop = true,
                    ),
                )
                for (i in 0 until 6) {
                    addMesh(
                        Geo.torus(0.255f, 0.010f, 28, 4),
                        Mat4.translation(V3(0f, -0.26f + i * 0.075f, 0f)),
                    )
                }
                addMesh(Geo.torus(0.205f, 0.014f, 26, 5), Mat4.translation(V3(0f, 0.32f, 0f)))
            }.build(),
            material = Materials.titanium, origin = V3(0f, SP_HOUSING_Y, 0f),
        ),
    )
    parts.add(
        Part(
            id = "sp_stator", name = "Stator windings", system = SP_DRIVE, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                for (i in 0 until 4) {
                    addMesh(Geo.torus(0.253f, 0.022f, 28, 5), Mat4.translation(V3(0f, -0.20f + i * 0.11f, 0f)))
                }
            }.build(),
            material = Materials.copperBright, parent = "sp_housing",
        ),
    )
    parts.add(
        Part(
            id = "sp_status", name = "Status ring", system = SP_DRIVE, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    addMesh(
                        Geo.box(0.028f, 0.060f, 0.018f),
                        Mat4.translation(V3((0.215f * kotlin.math.cos(a)).toFloat(), 0.26f, (0.215f * kotlin.math.sin(a)).toFloat()))
                            .multiply(Mat4.rotateY(i * 60f)),
                    )
                }
            }.build(),
            material = Materials.emitCyan, parent = "sp_housing",
        ),
    )

    parts.add(
        Part(
            id = "sp_shaft", name = "Rotor shaft", system = SP_DRIVE, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.050f, 1.30f, 18, bevel = 0.006f), Mat4.translation(V3(0f, -0.12f, 0f)))
                addMesh(Geo.box(0.018f, 0.90f, 0.012f), Mat4.translation(V3(0f, -0.08f, 0.052f)))
                addMesh(Geo.torus(0.058f, 0.008f, 20, 4), Mat4.translation(V3(0f, 0.30f, 0f)))
            }.build(),
            material = Materials.nickel, parent = "sp_housing",
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * (40f + 2200f * ctx.control)) },
        ),
    )
    parts.add(
        Part(
            id = "sp_bearings", name = "Ceramic bearing packs", system = SP_BEARING, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                listOf(-0.28f, 0.18f).forEach { y ->
                    addMesh(Geo.torus(0.098f, 0.028f, 24, 6), Mat4.translation(V3(0f, y, 0f)))
                    for (i in 0 until 10) {
                        val a = Math.toRadians(i * 36.0)
                        addMesh(
                            Geo.sphere(0.020f, 10, 6),
                            Mat4.translation(V3((0.098f * kotlin.math.cos(a)).toFloat(), y, (0.098f * kotlin.math.sin(a)).toFloat())),
                        )
                    }
                }
            }.build(),
            material = Materials.ceramic, parent = "sp_housing",
        ),
    )

    parts.add(
        Part(
            id = "sp_collet", name = "Collet nut", system = SP_TOOL, assembly = SP_TOOL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.taperedCylinder(0.150f, 0.105f, 0.16f, 24), Mat4.translation(V3(0f, -0.05f, 0f)))
                addMesh(Geo.cylinder(0.155f, 0.07f, 24, bevel = 0.008f), Mat4.translation(V3(0f, 0.06f, 0f)))
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    addMesh(
                        Geo.box(0.020f, 0.065f, 0.012f),
                        Mat4.translation(V3((0.148f * kotlin.math.cos(a)).toFloat(), 0.06f, (0.148f * kotlin.math.sin(a)).toFloat()))
                            .multiply(Mat4.rotateY(i * 60f)),
                    )
                }
            }.build(),
            material = Materials.nickel, parent = "sp_shaft", origin = V3(0f, -0.66f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "sp_tool", name = "Carbide end mill", system = SP_TOOL, assembly = SP_TOOL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.036f, 0.30f, 16), Mat4.translation(V3(0f, -0.16f, 0f)))
                for (i in 0 until 4) {
                    val a = Math.toRadians(i * 90.0)
                    addMesh(
                        Geo.box(0.014f, 0.30f, 0.022f),
                        Mat4.translation(V3((0.031f * kotlin.math.cos(a)).toFloat(), -0.16f, (0.031f * kotlin.math.sin(a)).toFloat()))
                            .multiply(Mat4.rotateY(i * 90f)),
                    )
                }
                addMesh(Geo.taperedCylinder(0.034f, 0.0f, 0.08f, 12), Mat4.translation(V3(0f, -0.35f, 0f)))
            }.build(),
            material = Materials.darkSteel, parent = "sp_collet", origin = V3(0f, -0.10f, 0f),
        ),
    )

    parts.add(
        Part(
            id = "sp_coolant", name = "Coolant nozzles", system = SP_COOL, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(side * 0.30f, 1.18f, 0.18f),
                                V3(side * 0.20f, 0.90f, 0.14f),
                                V3(side * 0.10f, 0.70f, 0.08f),
                                V3(side * 0.06f, 0.56f, 0.04f),
                            ),
                            0.020f, 7, subdivide = 3,
                        ),
                    )
                    addMesh(
                        Geo.taperedCylinder(0.026f, 0.012f, 0.06f, 8),
                        Mat4.translation(V3(side * 0.06f, 0.53f, 0.04f)),
                    )
                }
            }.build(),
            material = Materials.copperBright,
        ),
    )
    parts.add(
        Part(
            id = "sp_valve", name = "Coolant valve", system = SP_COOL, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.055f, 0.10f, 14), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.torus(0.070f, 0.012f, 18, 4).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0.06f, 0f)))
            }.build(),
            material = Materials.ruby, origin = V3(0.34f, 1.24f, 0.18f),
        ),
    )

    parts.add(
        Part(
            id = "sp_encoder", name = "Encoder", system = SP_DRIVE, assembly = SP_CARTRIDGE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.100f, 0.14f, 18, bevel = 0.010f), Mat4.translation(V3(0f, 0.46f, 0f)))
                addMesh(Geo.torus(0.104f, 0.010f, 20, 4), Mat4.translation(V3(0f, 0.52f, 0f)))
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.08f, 0.47f, -0.04f),
                            V3(0.20f, 0.52f, -0.20f),
                            V3(0.16f, 0.52f, -0.44f),
                            V3(0.06f, 0.42f, -0.50f),
                        ),
                        0.018f, 6, subdivide = 2,
                    ),
                )
            }.build(),
            material = Materials.graphite, parent = "sp_housing",
        ),
    )
    parts.add(
        Part(
            id = "sp_light", name = "Work light", system = SP_FRAME, assembly = SP_CARTRIDGE,
            mesh = Geo.torus(0.185f, 0.012f, 26, 4),
            material = Materials.emitWhite, origin = V3(0f, 1.06f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "sp_terminal", name = "Terminal box", system = SP_FRAME, assembly = SP_COLUMN,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.22f, 0.26f, 0.16f, 0.02f, 0.008f), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.cylinder(0.028f, 0.10f, 10), Mat4.translation(V3(0f, -0.10f, 0.0f)))
            }.build(),
            material = Materials.graphite, origin = V3(0.22f, 1.32f, -0.52f),
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "collet", "Collet nut",
            "The nut squeezes a split collet onto the tool shank. A quarter turn is enough to hold a 12 mm cutter at 24,000 rpm.",
            "sp_collet", V3(0.10f, 0.02f, 0.08f),
            CameraPose(20f, 8f, 1.5f), readoutLabel = "TOOL HOLD",
        ),
        Hotspot(
            "tool", "End mill",
            "Four flutes cut on the side and the end. The carbide teeth are ground to a helix so the load stays constant as the cutter rotates.",
            "sp_tool", V3(0.02f, -0.20f, 0.03f),
            CameraPose(24f, 6f, 1.55f), readoutLabel = "CUTTING",
        ),
        Hotspot(
            "bearings", "Hybrid bearings",
            "Ceramic balls run between steel races. They are lighter than steel balls, so they centrifugal-load the outer race far less at high speed.",
            "sp_bearings", V3(0.10f, 0.18f, 0.0f),
            CameraPose(36f, 14f, 1.34f), readoutLabel = "ROTATION",
        ),
        Hotspot(
            "stator", "Motor stator",
            "The stator is built into the cartridge. Its copper windings drive the rotor directly, with no belt and no gearbox between the motor and the tool.",
            "sp_stator", V3(0.25f, 0.0f, 0.05f),
            CameraPose(42f, 16f, 1.28f), readoutLabel = "DRIVE",
        ),
        Hotspot(
            "encoder", "Encoder",
            "A magnetic encoder reads the shaft angle 4,096 times per revolution. The drive uses it to hold the commanded speed under a changing cut.",
            "sp_encoder", V3(0.06f, 0.46f, 0.06f),
            CameraPose(34f, 22f, 1.38f), readoutLabel = "FEEDBACK",
        ),
        Hotspot(
            "coolant", "Coolant nozzles",
            "Two nozzles aim a jet at the flute tips. Coolant carries heat away and flushes chips out of the slot before they are recut.",
            "sp_coolant", V3(0.16f, 0.72f, 0.08f),
            CameraPose(52f, 10f, 1.34f), readoutLabel = "COOLING",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Spin the shaft",
            "The rotor is the tool holder. Copper windings in the housing drive permanent magnets on the shaft, so the cutter turns without a gearbox.",
            SP_DRIVE, CameraPose(26f, 12f, 1.08f), 0.10f, 0.55f,
        ),
        GuideStep(
            "Hold the tool",
            "A split collet grips the cutter. The nut pulls the collet into a taper, which closes it evenly around the shank.",
            SP_TOOL, CameraPose(22f, 8f, 1.40f), 0.0f, 0.35f,
        ),
        GuideStep(
            "Carry the load",
            "Two hybrid bearing packs take the radial and axial loads. Ceramic balls are light, stiff and immune to the welding that ruins steel bearings.",
            SP_BEARING, CameraPose(38f, 16f, 1.28f), 0.25f, 0.75f,
        ),
        GuideStep(
            "Stay cool",
            "The stator is oil-cooled through the jacket. The nozzles at the tool tip handle the cutting heat; the jacket handles the motor heat.",
            SP_COOL, CameraPose(50f, 12f, 1.26f), 0.10f, 0.9f,
        ),
        GuideStep(
            "Measure",
            "The encoder closes the speed loop. Runout is measured at the tool tip, because a perfect spindle with a bent cutter still cuts a bad hole.",
            SP_DRIVE, CameraPose(32f, 20f, 1.32f), 0.0f, 0.6f,
        ),
        GuideStep(
            "The assembly",
            "Explode the cartridge to see the housing, stator, shaft, bearings, collet and cutter. The bearings and the collet are the service parts.",
            null, CameraPose(34f, 22f, 0.96f), 1f, 0.5f,
        ),
    )

    return ObjectSpec(
        id = "spindle",
        index = 15,
        title = "VECTOR-5",
        designation = "V-05 / HIGH-SPEED SPINDLE",
        category = "Precision",
        categoryShort = "PRE",
        tagline = "24,000 rpm in a sealed cartridge.",
        summary = "A motorised machine-tool spindle: C-frame column, oil-cooled stator, hybrid ceramic bearings, collet nut and a four-flute carbide end mill.",
        details = listOf(
            "VECTOR-5 is the business end of a machining centre. It has to spin a cutter fast enough to shear metal cleanly, and stiff enough that the cutter does not chatter.",
            "There is no gearbox. The rotor is the tool holder, the stator is wound into the housing, and two bearing packs locate the shaft. The whole cartridge can be swapped in minutes.",
        ),
        specs = listOf(
            SpecRow("Speed", "24,000 rpm"),
            SpecRow("Power", "5.5 kW"),
            SpecRow("Torque", "3.5 N·m"),
            SpecRow("Runout", "< 2 µm"),
            SpecRow("Tool shank", "12 mm"),
        ),
        tags = listOf("spindle", "cnc", "machining", "bearing", "precision", "motor"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec(
            label = "SPEED",
            lowLabel = "IDLE",
            highLabel = "MAX",
            defaultValue = 0.55f,
            hint = "Shaft speed, coolant flow and cutter load",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(26f, 12f, 1.06f),
        thumbnailPose = CameraPose(28f, 14f, 1.08f),
        thumbnailTime = 2.0f,
        thumbnailControl = 0.85f,
        readouts = { ctx ->
            listOf(
                Readout("SPEED", String.format("%.0f", 24000f * ctx.control), "rpm", accent = true),
                Readout("RUNOUT", String.format("%.1f", 1.8f), "µm"),
                Readout("POWER", String.format("%.1f", 0.6f + 4.9f * ctx.control), "kW"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 0.66f, 0xFF6EF2DF.toInt(), 0f, 0.20f),
                SceneEffect.Glow(
                    V3(0f, 0.42f, 0f),
                    0.24f,
                    0xFF6EF2DF.toInt(),
                    0.18f + 0.35f * ctx.control,
                ),
            )
        },
    )
}
