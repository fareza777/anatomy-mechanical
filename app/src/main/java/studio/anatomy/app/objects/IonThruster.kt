package studio.anatomy.app.objects

import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
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

private const val IO_FRAME = "STRUCTURE"
private const val IO_PLASMA = "PLASMA CHAMBER"
private const val IO_MAGNET = "MAGNETICS"
private const val IO_FEED = "PROPELLANT"
private const val IO_POWER = "POWER"
private const val IO_STAND = "DISPLAY"

private const val IO_CHAMBER_Y = 1.36f

private fun gridMesh(radius: Float): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(radius, 0.014f, 30, 5))
    for (i in 0 until 4) {
        addMesh(
            Geo.box(radius * 1.94f, 0.014f, 0.014f),
            Mat4.rotateY(i * 45f),
        )
    }
    for (i in 0 until 2) {
        addMesh(
            Geo.torus(radius * 0.52f, 0.010f, 22, 4),
            Mat4.translation(V3(0f, 0f, 0f)),
        )
    }
}.build()

private fun coilMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.355f, 0.030f, 30, 6))
    addMesh(Geo.torus(0.355f, 0.012f, 30, 4), Mat4.translation(V3(0f, 0.042f, 0f)))
    listOf(0f, 90f, 180f, 270f).forEach { angle ->
        val a = Math.toRadians(angle.toDouble())
        addMesh(
            Geo.box(0.075f, 0.060f, 0.030f),
            Mat4.translation(V3((0.355f * cos(a)).toFloat(), 0f, (0.355f * sin(a)).toFloat()))
                .multiply(Mat4.rotateY(angle.toFloat())),
        )
    }
}.build()

fun ionThruster(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "ion",
            width = 1.7f,
            depth = 1.7f,
            top = 0.3f,
            system = IO_FRAME,
            assembly = IO_STAND,
            accentMaterial = Materials.emitBlue,
        ),
    )

    parts.add(
        Part(
            id = "io_chamber", name = "Discharge chamber", system = IO_PLASMA, assembly = "THRUSTER",
            mesh = MeshBuilder().apply {
                addMesh(Geo.hollowCylinder(0.315f, 0.265f, 0.72f, 28))
                addMesh(Geo.cylinder(0.355f, 0.06f, 28, bevel = 0.010f), Mat4.translation(V3(0f, -0.39f, 0f)))
                addMesh(Geo.cylinder(0.355f, 0.06f, 28, bevel = 0.010f), Mat4.translation(V3(0f, 0.39f, 0f)))
                addMesh(Geo.torus(0.320f, 0.016f, 28, 5), Mat4.translation(V3(0f, -0.30f, 0f)))
                addMesh(Geo.torus(0.320f, 0.016f, 28, 5), Mat4.translation(V3(0f, 0.30f, 0f)))
                listOf(0f, 120f, 240f).forEach { angle ->
                    val a = Math.toRadians(angle.toDouble())
                    addMesh(
                        Geo.box(0.035f, 0.62f, 0.030f),
                        Mat4.translation(V3((0.318f * cos(a)).toFloat(), 0f, (0.318f * sin(a)).toFloat()))
                            .multiply(Mat4.rotateY(angle.toFloat())),
                    )
                }
            }.build(),
            material = Materials.titanium, origin = V3(0f, IO_CHAMBER_Y, 0f),
        ),
    )

    parts.add(
        Part(
            id = "io_core", name = "Hollow cathode", system = IO_PLASMA, assembly = "THRUSTER",
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.125f, 0.80f, 20, bevel = 0.02f))
                addMesh(Geo.torus(0.135f, 0.018f, 22, 5), Mat4.translation(V3(0f, 0.34f, 0f)))
                addMesh(Geo.cylinder(0.070f, 0.10f, 16), Mat4.translation(V3(0f, 0.44f, 0f)))
            }.build(),
            material = Materials.nickel, origin = V3(0f, IO_CHAMBER_Y, 0f), parent = "io_chamber",
        ),
    )

    val coil = coilMesh()
    (0 until 4).forEach { index ->
        parts.add(
            Part(
                id = "io_coil_${index + 1}", name = "Magnet coil ${index + 1}", system = IO_MAGNET, assembly = "THRUSTER",
                mesh = coil,
                material = if (index % 2 == 0) Materials.emitBlue else Materials.copperBright,
                origin = V3(0f, IO_CHAMBER_Y - 0.27f + index * 0.18f, 0f),
                parent = "io_chamber",
                explode = V3(0f, 0.7f + index * 0.15f, 0f),
            ),
        )
    }

    (0 until 3).forEach { index ->
        parts.add(
            Part(
                id = "io_grid_${index + 1}", name = "Accelerator grid ${index + 1}", system = IO_MAGNET, assembly = "GRIDS",
                mesh = gridMesh(0.315f - index * 0.012f),
                material = if (index == 1) Materials.emitBlue else Materials.nickel,
                origin = V3(0f, IO_CHAMBER_Y + 0.44f + index * 0.062f, 0f),
                parent = "io_chamber",
                explode = V3(0f, 1.25f + index * 0.18f, 0f),
            ),
        )
    }

    parts.add(
        Part(
            id = "io_nozzle", name = "Plasma nozzle", system = IO_PLASMA, assembly = "THRUSTER",
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.lathe(
                        listOf(0.165f to 0f, 0.20f to 0.08f, 0.27f to 0.17f, 0.345f to 0.24f),
                        30, capBottom = false, capTop = false,
                    ),
                )
                addMesh(Geo.torus(0.345f, 0.018f, 30, 5), Mat4.translation(V3(0f, 0.24f, 0f)))
                addMesh(Geo.torus(0.19f, 0.012f, 26, 4), Mat4.translation(V3(0f, 0.05f, 0f)))
            }.build(),
            material = Materials.darkSteel,
            origin = V3(0f, IO_CHAMBER_Y + 0.58f, 0f), parent = "io_chamber",
            doubleSided = true,
        ),
    )

    (0 until 4).forEach { index ->
        val angle = 45f + index * 90f
        val a = Math.toRadians(angle.toDouble())
        parts.add(
            Part(
                id = "io_strut_${index + 1}", name = "Support strut ${index + 1}", system = IO_FRAME, assembly = "FRAME",
                mesh = Geo.cylinderBetween(
                    V3((0.55f * cos(a)).toFloat(), 0.30f, (0.55f * sin(a)).toFloat()),
                    V3((0.26f * cos(a)).toFloat(), 0.98f, (0.26f * sin(a)).toFloat()),
                    0.032f, 8,
                ),
                material = Materials.darkSteel,
            ),
        )
    }

    parts.add(
        Part(
            id = "io_tank", name = "Xenon tank", system = IO_FEED, assembly = "FEED",
            mesh = Geo.lathe(
                listOf(
                    0f to -0.29f, 0.13f to -0.27f, 0.175f to -0.18f,
                    0.175f to 0.18f, 0.13f to 0.27f, 0f to 0.29f,
                ),
                26, capBottom = false, capTop = false,
            ),
            material = Materials.paintSlate, origin = V3(0.92f, 0.74f, -0.10f),
        ),
    )
    parts.add(
        Part(
            id = "io_tank_bands", name = "Tank straps", system = IO_FEED, assembly = "FEED",
            mesh = MeshBuilder().apply {
                listOf(-0.12f, 0.12f).forEach { y ->
                    addMesh(Geo.torus(0.183f, 0.018f, 24, 5), Mat4.translation(V3(0f, y, 0f)))
                }
                addMesh(Geo.cylinder(0.045f, 0.10f, 12), Mat4.translation(V3(0f, 0.34f, 0f)))
            }.build(),
            material = Materials.nickel, parent = "io_tank",
        ),
    )
    parts.add(
        Part(
            id = "io_feed", name = "Propellant feed line", system = IO_FEED, assembly = "FEED",
            mesh = Geo.tube(
                listOf(
                    V3(0.92f, 1.08f, -0.10f),
                    V3(0.84f, 1.28f, -0.02f),
                    V3(0.50f, 1.20f, 0.10f),
                    V3(0.30f, 1.06f, 0.08f),
                ),
                0.028f, 8, subdivide = 3,
            ),
            material = Materials.copperBright,
        ),
    )

    parts.add(
        Part(
            id = "io_psu", name = "Power conditioning unit", system = IO_POWER, assembly = "POWER",
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.52f, 0.40f, 0.34f, 0.04f, 0.015f), Mat4.translation(V3(0f, 0f, 0f)))
                for (i in 0 until 5) {
                    addMesh(
                        Geo.box(0.48f, 0.020f, 0.32f),
                        Mat4.translation(V3(0f, 0.11f + i * 0.045f, 0f)),
                    )
                }
                addMesh(Geo.box(0.18f, 0.10f, 0.22f), Mat4.translation(V3(0f, -0.22f, 0f)))
            }.build(),
            material = Materials.graphite, origin = V3(-0.82f, 0.58f, -0.28f),
        ),
    )
    parts.add(
        Part(
            id = "io_psu_lights", name = "Power indicators", system = IO_POWER, assembly = "POWER",
            mesh = MeshBuilder().apply {
                for (i in 0 until 4) {
                    addMesh(
                        Geo.box(0.030f, 0.012f, 0.020f),
                        Mat4.translation(V3(-0.15f + i * 0.10f, 0.06f, 0.205f)),
                    )
                }
            }.build(),
            material = Materials.emitBlue, parent = "io_psu",
        ),
    )
    parts.add(
        Part(
            id = "io_cable", name = "High-voltage cable", system = IO_POWER, assembly = "POWER",
            mesh = Geo.tube(
                listOf(
                    V3(-0.82f, 0.84f, -0.28f),
                    V3(-0.62f, 1.20f, -0.18f),
                    V3(-0.30f, 1.18f, -0.06f),
                    V3(-0.20f, 1.00f, 0.0f),
                ),
                0.030f, 7, subdivide = 3,
            ),
            material = Materials.rubber,
        ),
    )

    parts.add(
        Part(
            id = "io_sensor", name = "Beam current probe", system = IO_MAGNET, assembly = "GRIDS",
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.055f, 0.16f, 16, bevel = 0.008f), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.torus(0.058f, 0.010f, 18, 4), Mat4.translation(V3(0f, 0.06f, 0f)))
            }.build(),
            material = Materials.brass,
            origin = V3(0.36f, IO_CHAMBER_Y + 0.26f, 0.20f),
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "grids", "Accelerator grids",
            "Three perforated grids sit only millimetres apart. The first accelerates the ions, the second focuses them, and the third stops electrons from following the beam back into the ship.",
            "io_grid_2", V3(0.10f, 0f, 0.10f),
            CameraPose(22f, 18f, 1.42f), readoutLabel = "BEAM",
        ),
        Hotspot(
            "coils", "Magnet coils",
            "Four coils shape the discharge. The magnetic field makes the electrons spiral, which lets them ionise the xenon without eroding the chamber walls.",
            "io_coil_2", V3(0.28f, 0f, 0.10f),
            CameraPose(40f, 22f, 1.26f), readoutLabel = "MAGNETICS",
        ),
        Hotspot(
            "chamber", "Discharge chamber",
            "Xenon enters here and is stripped of an electron by a hollow cathode. The resulting plasma is dense enough to conduct, but thin enough to be pushed by fields rather than pressure.",
            "io_chamber", V3(0.12f, 0.10f, 0.20f),
            CameraPose(34f, 16f, 1.18f), readoutLabel = "PLASMA",
        ),
        Hotspot(
            "nozzle", "Plasma nozzle",
            "The nozzle is a magnetic throat, not a metal one. It lets the beam expand without touching the walls, which is why the plume is invisible in vacuum.",
            "io_nozzle", V3(0.12f, 0.10f, 0.18f),
            CameraPose(24f, 10f, 1.40f), readoutLabel = "EXHAUST",
        ),
        Hotspot(
            "tank", "Xenon tank",
            "A composite bottle of heavy noble gas. It takes only 250 grams to run the thruster for a month, but the tank has to hold it at 200 bar.",
            "io_tank", V3(0f, 0.10f, 0.12f),
            CameraPose(46f, 18f, 1.34f), readoutLabel = "PROPELLANT",
        ),
        Hotspot(
            "psu", "Power conditioning",
            "Ion engines run on electricity, not chemistry. This unit turns 28 volts into the 1,200 volts the grids need, at 92 percent efficiency.",
            "io_psu", V3(0.10f, 0.18f, 0.16f),
            CameraPose(60f, 16f, 1.30f), readoutLabel = "POWER",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Ionise the gas",
            "Xenon atoms are stripped of an electron. The plasma is neutral overall but conductive, so it can be steered by electric and magnetic fields.",
            IO_PLASMA, CameraPose(30f, 16f, 1.10f), 0.15f, 0.35f,
        ),
        GuideStep(
            "Shape the field",
            "The coils create a magnetic bottle. Electrons spiral inside it to ionise more gas, while the ions are left free to accelerate.",
            IO_MAGNET, CameraPose(44f, 24f, 1.18f), 0.30f, 0.55f,
        ),
        GuideStep(
            "Accelerate",
            "The grids apply a strong electric field. Ions leave the last grid at around 30 km/s, which is ten times faster than a chemical rocket's exhaust.",
            IO_MAGNET, CameraPose(20f, 14f, 1.42f), 0.45f, 0.85f,
        ),
        GuideStep(
            "Fire the beam",
            "The beam is invisible in vacuum, but it is real: 4 newtons of thrust, day after day, for as long as the power lasts.",
            IO_PLASMA, CameraPose(28f, 18f, 1.16f), 0.1f, 1f,
        ),
        GuideStep(
            "Feed it",
            "A mass flow controller meters xenon into the chamber. At full power the engine drinks 0.16 milligrams per second, which is barely a breath.",
            IO_FEED, CameraPose(48f, 20f, 1.22f), 0.0f, 0.7f,
        ),
        GuideStep(
            "The assembly",
            "Explode the thruster to see the chamber, the three grids, the magnets, the tank and the power stage. The grids are the only parts that wear.",
            null, CameraPose(36f, 22f, 0.98f), 1f, 0.6f,
        ),
    )

    return ObjectSpec(
        id = "ion_thruster",
        index = 13,
        title = "ION-X9",
        designation = "IX-9 / GRIDDED ION THRUSTER",
        category = "Aerospace",
        categoryShort = "AER",
        tagline = "Thrust measured in grams, patience measured in years.",
        summary = "A gridded ion thruster: a hollow cathode, a magnetically shielded discharge chamber, three accelerator grids and a xenon feed system.",
        details = listOf(
            "ION-X9 is a deep-space engine. It produces less thrust than the weight of a coin, but it can run for years on a tank of xenon, which makes it ideal for station-keeping and slow cargo transfers.",
            "The visible plume is mostly a diagnostic: in vacuum the beam is invisible. The glowing rings are the magnet coils, and the grids at the top are where the ions leave the machine.",
        ),
        specs = listOf(
            SpecRow("Thrust", "4.5 N"),
            SpecRow("Specific impulse", "3,200 s"),
            SpecRow("Beam power", "4.8 kW"),
            SpecRow("Xenon flow", "0.16 mg/s"),
            SpecRow("Grid diameter", "31 cm"),
        ),
        tags = listOf("ion", "thruster", "spacecraft", "plasma", "electric propulsion", "aerospace"),
        accent = 0xFF8FB4FF.toInt(),
        control = ControlSpec(
            label = "BEAM",
            lowLabel = "IDLE",
            highLabel = "FULL",
            defaultValue = 0.65f,
            hint = "Discharge power, beam current and plume length",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(30f, 14f, 1.08f),
        thumbnailPose = CameraPose(32f, 16f, 1.10f),
        thumbnailTime = 2.2f,
        thumbnailControl = 0.8f,
        readouts = { ctx ->
            listOf(
                Readout("THRUST", String.format("%.1f", 0.3f + 4.2f * ctx.control), "N", accent = true),
                Readout("ISP", String.format("%.0f", 2500f + 700f * ctx.control), "s"),
                Readout("BEAM", String.format("%.1f", 0.6f + 4.2f * ctx.control), "kW"),
            )
        },
        effects = { ctx ->
            val intensity = 0.35f + 0.65f * ctx.control
            listOf(
                SceneEffect.Plume(
                    origin = V3(0f, IO_CHAMBER_Y + 0.84f, 0f),
                    direction = V3(0f, 1f, 0f),
                    length = 0.9f + 1.5f * ctx.control,
                    radius = 0.14f + 0.12f * ctx.control,
                    intensity = intensity,
                    palette = PlumePalette.PLASMA,
                ),
                SceneEffect.Glow(
                    V3(0f, IO_CHAMBER_Y + 0.68f, 0f), 0.42f, 0xFF8FB4FF.toInt(), 0.30f + 0.4f * intensity,
                ),
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 0.74f, 0xFF8FB4FF.toInt(), 0f, 0.22f),
            )
        },
    )
}
