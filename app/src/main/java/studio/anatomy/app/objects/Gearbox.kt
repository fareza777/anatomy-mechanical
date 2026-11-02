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

private const val GB_CASE = "CASE"
private const val GB_INPUT = "INPUT"
private const val GB_TRAIN = "GEARTRAIN"
private const val GB_SHIFT = "SELECTOR"

private const val A_HOUSING = "HOUSING"
private const val A_INPUT = "INPUT SHAFT"
private const val A_COUNTER = "COUNTERSHAFT"
private const val A_OUTPUT = "OUTPUT SHAFT"
private const val A_SHIFT = "SHIFT MECHANISM"

private const val GEAR_AXIS_Y = 1.57f
private const val COUNTER_Y = 1.02f
private const val GEAR_AXIS_Z = -0.12f
private const val INPUT_R = 0.18f
private const val COUNTER_INPUT_R = 0.37f
private const val INPUT_TEETH = 18
private const val COUNTER_INPUT_TEETH = 37

/** First gear is the smallest countershaft pinion driving the largest output gear. */
private val COUNTER_R = floatArrayOf(0.13f, 0.17f, 0.21f, 0.25f, 0.29f, 0.33f)
private val OUTPUT_R = floatArrayOf(0.42f, 0.38f, 0.34f, 0.30f, 0.26f, 0.22f)
private val GEAR_X = floatArrayOf(-0.55f, -0.19f, 0.17f, 0.53f, 0.89f, 1.25f)
private val GEAR_LABELS = listOf("1st", "2nd", "3rd", "4th", "5th", "6th")
private const val FINAL_DRIVE = 3.55f
private const val ENGINE_RPM = 4200f

private fun selectedGear(ctx: AnimContext): Int =
    (1 + (ctx.control * 5.999f).toInt()).coerceIn(1, 6)

/** Overall ratio from crank to output shaft: input pair, then the selected wheel pair. */
private fun overallRatio(gear: Int): Float {
    val index = (gear - 1).coerceIn(0, 5)
    return (COUNTER_INPUT_R / INPUT_R) * (OUTPUT_R[index] / COUNTER_R[index])
}

private fun gearMesh(radius: Float, teeth: Int, thickness: Float = 0.16f): Mesh =
    Geo.gear(radius, teeth, thickness, 0.026f, segmentsPerTooth = 3)
        .transformed(Mat4.rotateZ(90f))

private fun shaftX(radius: Float, length: Float, x: Float, y: Float, z: Float, segments: Int = 18): Mesh =
    Geo.cylinder(radius, length, segments, bevel = 0.008f)
        .transformed(Mat4.translation(V3(x, y, z)).multiply(Mat4.rotateZ(-90f)))

private fun bearingRing(x: Float, y: Float, z: Float, major: Float, minor: Float = 0.045f): Mesh =
    Geo.torus(major, minor, 24, 6)
        .transformed(Mat4.translation(V3(x, y, z)).multiply(Mat4.rotateZ(90f)))

private fun hubTravel(gear: Int, hub: Int): Float {
    val first = hub * 2 + 1
    return when (gear) {
        first -> -0.075f
        first + 1 -> 0.075f
        else -> 0f
    }
}

fun gearbox(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "mesh",
            width = 3.9f,
            depth = 2.25f,
            top = 0.3f,
            system = GB_CASE,
            assembly = A_HOUSING,
            accentMaterial = Materials.emitAmber,
        )
    )

    val housing = MeshBuilder().apply {
        // Sump and lower casting.
        addMesh(
            Geo.slab(2.98f, 1.32f, 0.20f, 0.12f, 0.035f, 3),
            Mat4.translation(V3(0.35f, 0.62f, GEAR_AXIS_Z)),
        )
        addMesh(
            Geo.slab(2.52f, 1.02f, 0.26f, 0.09f, 0.03f, 2),
            Mat4.translation(V3(0.30f, 0.50f, GEAR_AXIS_Z)),
        )
        // Rear wall, end covers and the cut front lip: the gear train stays open to view.
        addMesh(
            Geo.slab(2.98f, 0.10f, 1.02f, 0.03f, 0.012f),
            Mat4.translation(V3(0.35f, 1.36f, -0.72f)),
        )
        addMesh(
            Geo.slab(2.98f, 0.11f, 0.34f, 0.04f, 0.014f),
            Mat4.translation(V3(0.35f, 0.80f, 0.45f)),
        )
        addMesh(
            Geo.slab(0.13f, 1.10f, 1.04f, 0.05f, 0.02f, 2),
            Mat4.translation(V3(-1.0f, 1.30f, GEAR_AXIS_Z)),
        )
        addMesh(
            Geo.slab(0.13f, 1.10f, 1.04f, 0.05f, 0.02f, 2),
            Mat4.translation(V3(1.82f, 1.30f, GEAR_AXIS_Z)),
        )
        // Top rails frame the open cutaway.
        addMesh(
            Geo.slab(2.98f, 0.14f, 0.09f, 0.035f, 0.012f),
            Mat4.translation(V3(0.35f, 1.98f, 0.44f)),
        )
        addMesh(
            Geo.slab(2.98f, 0.14f, 0.09f, 0.035f, 0.012f),
            Mat4.translation(V3(0.35f, 1.98f, -0.68f)),
        )
        // Cast ribs under the sump.
        for (i in 0 until 6) {
            addMesh(
                Geo.slab(0.05f, 1.06f, 0.12f, 0.02f, 0.01f),
                Mat4.translation(V3(-0.95f + i * 0.52f, 0.48f, GEAR_AXIS_Z)),
            )
        }
        // Machined cut edge.
        addMesh(
            Geo.slab(2.9f, 0.055f, 0.035f, 0.012f, 0.006f),
            Mat4.translation(V3(0.35f, 0.97f, 0.46f)),
        )
    }.build()
    parts.add(
        Part(
            id = "mesh_housing", name = "Split alloy housing", system = GB_CASE, assembly = A_HOUSING,
            mesh = housing, material = Materials.paintSlate, explode = V3(0f, -0.55f, 0f),
        )
    )

    val bell = MeshBuilder().apply {
        addMesh(
            Geo.hollowCylinder(0.66f, 0.57f, 0.52f, 42)
                .transformed(Mat4.translation(V3(-1.62f, GEAR_AXIS_Y, GEAR_AXIS_Z)).multiply(Mat4.rotateZ(90f))),
        )
        addMesh(
            Geo.torus(0.63f, 0.035f, 34, 6)
                .transformed(Mat4.translation(V3(-1.38f, GEAR_AXIS_Y, GEAR_AXIS_Z)).multiply(Mat4.rotateZ(90f))),
        )
        addMesh(
            Geo.torus(0.63f, 0.035f, 34, 6)
                .transformed(Mat4.translation(V3(-1.86f, GEAR_AXIS_Y, GEAR_AXIS_Z)).multiply(Mat4.rotateZ(90f))),
        )
        // Lower mount foot.
        addMesh(
            Geo.slab(0.26f, 0.62f, 0.20f, 0.06f, 0.025f),
            Mat4.translation(V3(-1.62f, 0.83f, GEAR_AXIS_Z)),
        )
    }.build()
    parts.add(
        Part(
            id = "mesh_bell", name = "Clutch bell housing", system = GB_CASE, assembly = A_HOUSING,
            mesh = bell, material = Materials.paintSlate, explode = V3(0f, -0.55f, 0f),
        )
    )
    val bellBolts = ArrayList<V3>()
    for (i in 0 until 10) {
        val angle = Math.toRadians((i * 36.0))
        bellBolts.add(
            V3(
                -1.89f,
                (GEAR_AXIS_Y + 0.57f * kotlin.math.cos(angle)).toFloat(),
                (GEAR_AXIS_Z + 0.57f * kotlin.math.sin(angle)).toFloat(),
            )
        )
    }
    parts.add(
        Part(
            id = "mesh_bell_bolts", name = "Bell fasteners", system = GB_CASE, assembly = A_HOUSING,
            mesh = Geo.boltSet(bellBolts, 0.045f, 0.05f, axis = V3.X),
            material = Materials.titanium, explode = V3(0f, -0.55f, 0f),
        )
    )

    // Input shaft: crank splines, a fixed pinion, and the pilot nose that rides in the output shaft.
    val inputParts = MeshBuilder().apply {
        addMesh(shaftX(0.085f, 1.06f, -1.73f, 0f, 0f), Mat4.translation(V3(0f, 0f, 0f)))
        addMesh(
            Geo.cylinder(0.115f, 0.24f, 20, bevel = 0.015f),
            Mat4.translation(V3(-2.14f, 0f, 0f)).multiply(Mat4.rotateZ(-90f)),
        )
        addMesh(gearMesh(INPUT_R, INPUT_TEETH), Mat4.translation(V3(-1.18f, 0f, 0f)))
        addMesh(bearingRing(-1.06f, 0f, 0f, 0.11f, 0.03f), Mat4.identity())
        addMesh(shaftX(0.052f, 0.20f, -1.12f, 0f, 0f, 14), Mat4.identity())
    }.build()
    val inputAngle = { ctx: AnimContext -> ctx.machineTime * 210f }
    parts.add(
        Part(
            id = "mesh_input", name = "Input shaft and pinion", system = GB_INPUT, assembly = A_INPUT,
            mesh = inputParts, material = Materials.brushedSteel,
            origin = V3(0f, GEAR_AXIS_Y, GEAR_AXIS_Z),
            animate = { ctx -> Mat4.rotateX(inputAngle(ctx)) },
            explode = V3(-0.9f, 0.5f, 0f),
        )
    )
    val splines = MeshBuilder().apply {
        for (i in 0 until 12) {
            addMesh(
                Geo.box(0.20f, 0.035f, 0.05f),
                Mat4.translation(V3(-2.15f, 0f, 0f))
                    .multiply(Mat4.rotateX(30f * i))
                    .multiply(Mat4.translation(V3(0f, 0.115f, 0f))),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "mesh_splines", name = "Crank splines", system = GB_INPUT, assembly = A_INPUT,
            mesh = splines, material = Materials.darkSteel,
            origin = V3(0f, GEAR_AXIS_Y, GEAR_AXIS_Z),
            animate = { ctx -> Mat4.rotateX(inputAngle(ctx)) },
            explode = V3(-0.9f, 0.5f, 0f),
        )
    )

    // Countershaft: one solid cluster gear set, so every pinion shares the shaft speed.
    val counter = MeshBuilder().apply {
        addMesh(shaftX(0.072f, 3.2f, 0.08f, 0f, 0f, 16), Mat4.identity())
        addMesh(gearMesh(COUNTER_INPUT_R, COUNTER_INPUT_TEETH, 0.16f), Mat4.translation(V3(-1.18f, 0f, 0f)))
        for (i in 0 until 6) {
            addMesh(
                gearMesh(COUNTER_R[i], (COUNTER_R[i] * 100f).toInt(), 0.16f),
                Mat4.translation(V3(GEAR_X[i], 0f, 0f)),
            )
        }
        addMesh(bearingRing(-1.58f, 0f, 0f, 0.11f, 0.03f), Mat4.identity())
        addMesh(bearingRing(1.68f, 0f, 0f, 0.11f, 0.03f), Mat4.identity())
    }.build()
    val counterAngle = { ctx: AnimContext -> -inputAngle(ctx) * (INPUT_TEETH.toFloat() / COUNTER_INPUT_TEETH.toFloat()) }
    parts.add(
        Part(
            id = "mesh_counter", name = "Countershaft cluster", system = GB_TRAIN, assembly = A_COUNTER,
            mesh = counter, material = Materials.brushedSteel,
            origin = V3(0f, COUNTER_Y, GEAR_AXIS_Z),
            animate = { ctx -> Mat4.rotateX(counterAngle(ctx)) },
            explode = V3(0f, 0.85f, 0f),
        )
    )

    val output = MeshBuilder().apply {
        addMesh(shaftX(0.075f, 2.85f, 0.36f, 0f, 0f, 16), Mat4.identity())
        addMesh(bearingRing(-0.92f, 0f, 0f, 0.115f, 0.032f), Mat4.identity())
        addMesh(bearingRing(1.74f, 0f, 0f, 0.115f, 0.032f), Mat4.identity())
        addMesh(
            Geo.cylinder(0.145f, 0.16f, 20),
            Mat4.translation(V3(1.90f, 0f, 0f)).multiply(Mat4.rotateZ(-90f)),
        )
    }.build()
    val outputAngle = { ctx: AnimContext ->
        -counterAngle(ctx) * (COUNTER_R[selectedGear(ctx) - 1] / OUTPUT_R[selectedGear(ctx) - 1])
    }
    parts.add(
        Part(
            id = "mesh_output", name = "Output shaft", system = GB_TRAIN, assembly = A_OUTPUT,
            mesh = output, material = Materials.brushedSteel,
            origin = V3(0f, GEAR_AXIS_Y, GEAR_AXIS_Z),
            animate = { ctx -> Mat4.rotateX(outputAngle(ctx)) },
            explode = V3(0.4f, 1.6f, 0f),
        )
    )

    // Six wheel gears. They always free-wheel at their own ratio; the selected one drives the shaft.
    for (i in 0 until 6) {
        val radius = OUTPUT_R[i]
        val freeSpeed = { ctx: AnimContext ->
            -counterAngle(ctx) * (COUNTER_R[i] / radius)
        }
        parts.add(
            Part(
                id = "mesh_out_gear_${i + 1}",
                name = "${GEAR_LABELS[i]} wheel gear",
                system = GB_TRAIN, assembly = A_OUTPUT,
                mesh = gearMesh(radius, (radius * 100f).toInt()),
                material = if (i % 2 == 0) Materials.brushedSteel else Materials.darkSteel,
                origin = V3(GEAR_X[i], GEAR_AXIS_Y, GEAR_AXIS_Z),
                animate = { ctx -> Mat4.rotateX(freeSpeed(ctx)) },
                explode = V3(0.7f, 1.6f, 0.35f),
            )
        )
    }

    // Three synchro hubs, one between each pair of wheel gears.
    val hubX = floatArrayOf(-0.37f, 0.35f, 1.07f)
    for (hub in 0 until 3) {
        val travel = { ctx: AnimContext -> hubTravel(selectedGear(ctx), hub) }
        val sleeve = MeshBuilder().apply {
            addMesh(
                Geo.hollowCylinder(0.145f, 0.085f, 0.20f, 18)
                    .transformed(Mat4.rotateZ(90f)),
            )
            addMesh(Geo.torus(0.135f, 0.022f, 20, 5).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(0.1f, 0f, 0f)))
            addMesh(Geo.torus(0.135f, 0.022f, 20, 5).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(-0.1f, 0f, 0f)))
            for (i in 0 until 3) {
                addMesh(
                    Geo.box(0.24f, 0.06f, 0.05f),
                    Mat4.rotateX(120f * i).multiply(Mat4.translation(V3(0f, 0.12f, 0f))),
                )
            }
        }.build()
        parts.add(
            Part(
                id = "mesh_synchro_${hub + 1}", name = "Synchro hub ${hub + 1}",
                system = GB_SHIFT, assembly = A_SHIFT,
                mesh = sleeve, material = Materials.brass,
                origin = V3(hubX[hub], GEAR_AXIS_Y, GEAR_AXIS_Z),
                animate = { ctx ->
                    Mat4.translation(V3(travel(ctx), 0f, 0f)).multiply(Mat4.rotateX(outputAngle(ctx)))
                },
                explode = V3(0f, 0.2f, 0.9f),
            )
        )
    }

    // Shift forks on a single rail, sliding with the selected hub.
    for (hub in 0 until 3) {
        val travel = { ctx: AnimContext -> hubTravel(selectedGear(ctx), hub) }
        val fork = MeshBuilder().apply {
            addMesh(
                Geo.hollowCylinder(0.16f, 0.115f, 0.05f, 18)
                    .transformed(Mat4.rotateZ(90f)),
                Mat4.translation(V3(0f, -0.38f, 0f)),
            )
            addMesh(
                Geo.slab(0.06f, 0.16f, 0.34f, 0.02f, 0.008f),
                Mat4.translation(V3(0f, -0.15f, 0f)),
            )
            addMesh(
                Geo.slab(0.16f, 0.20f, 0.12f, 0.03f, 0.01f),
                Mat4.translation(V3(0f, 0.05f, 0f)),
            )
            addMesh(
                Geo.cylinder(0.055f, 0.16f, 12).transformed(Mat4.rotateZ(-90f)),
                Mat4.translation(V3(0f, 0.14f, 0f)),
            )
        }.build()
        parts.add(
            Part(
                id = "mesh_fork_${hub + 1}", name = "Shift fork ${hub + 1}",
                system = GB_SHIFT, assembly = A_SHIFT,
                mesh = fork, material = Materials.titanium,
                origin = V3(hubX[hub], 1.94f, GEAR_AXIS_Z),
                animate = { ctx -> Mat4.translation(V3(travel(ctx), 0f, 0f)) },
                explode = V3(0f, 0.5f, 1.25f),
            )
        )
    }
    parts.add(
        Part(
            id = "mesh_shift_rail", name = "Selector rail", system = GB_SHIFT, assembly = A_SHIFT,
            mesh = shaftX(0.028f, 2.5f, 0.35f, 1.94f, GEAR_AXIS_Z, 12),
            material = Materials.darkSteel,
            explode = V3(0f, 0.5f, 1.25f),
        )
    )

    // Detent drum outside the rear cover turns with the selected gear.
    val drum = MeshBuilder().apply {
        addMesh(
            Geo.cylinder(0.19f, 0.56f, 24, bevel = 0.02f)
                .transformed(Mat4.rotateZ(-90f)),
        )
    }.build()
    parts.add(
        Part(
            id = "mesh_drum", name = "Detent drum", system = GB_SHIFT, assembly = A_SHIFT,
            mesh = drum, material = Materials.darkSteel,
            origin = V3(1.98f, 1.72f, GEAR_AXIS_Z),
            animate = { ctx -> Mat4.rotateX((selectedGear(ctx) - 1) * 54f) },
            explode = V3(0.9f, 0.3f, 0.8f),
        )
    )

    val plate = MeshBuilder().apply {
        addMesh(
            Geo.slab(1.16f, 0.34f, 0.03f, 0.04f, 0.008f),
            Mat4.translation(V3(0.55f, 1.985f, 0.16f)),
        )
        for (i in 0 until 6) {
            addMesh(
                Geo.box(0.055f, 0.012f, 0.22f),
                Mat4.translation(V3(-0.32f + i * 0.14f, 2.005f, 0.16f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "mesh_plate", name = "Ratio plate", system = GB_CASE, assembly = A_HOUSING,
            mesh = plate, material = Materials.brass, explode = V3(0f, -0.2f, 0.6f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "splines", "Crank splines",
            "The input shaft rides in the pilot bearing inside the output shaft. Twelve splines let the clutch disc slide axially while transmitting the engine's full torque.",
            "mesh_input", V3(-1.98f, 0.02f, 0f),
            CameraPose(66f, 16f, 1.2f), readoutLabel = "TORQUE IN",
        ),
        Hotspot(
            "pairs", "Six gear pairs",
            "Countershaft pinions grow from first to sixth while the wheel gears shrink. Every pair shares the same 0.55 m centre distance, so one sliding hub can lock any ratio.",
            "mesh_counter", V3(0.22f, 0.22f, 0.1f),
            CameraPose(34f, 26f, 1.1f), readoutLabel = "GEAR TRAIN",
        ),
        Hotspot(
            "synchro", "Synchro hubs",
            "Brass blocker rings match shaft speed before the collar engages. That is why a manual gearbox can be shifted without grinding: the teeth only touch once speeds agree.",
            "mesh_synchro_2", V3(0.02f, 0.1f, 0.14f),
            CameraPose(24f, 22f, 1.22f), readoutLabel = "SYNCHROMESH",
        ),
        Hotspot(
            "forks", "Selector forks",
            "Three forks ride one rail and slide the synchro collars. A rotating detent drum outside the case converts the lever's movement into precise axial travel.",
            "mesh_fork_2", V3(0f, 0.03f, 0.12f),
            CameraPose(40f, 30f, 1.1f), readoutLabel = "SHIFT",
        ),
        Hotspot(
            "bearings", "Tapered bearings",
            "The output shaft is supported at both ends by preloaded tapered rollers. The preload removes play under torque and keeps the gears in mesh under load.",
            "mesh_output", V3(1.74f, 0.02f, 0f),
            CameraPose(52f, 22f, 1.2f), readoutLabel = "BEARINGS",
        ),
        Hotspot(
            "housing", "Split alloy housing",
            "Two cast aluminium halves share the bearing bores, so the shafts stay aligned when the case is bolted together. The sump holds 2.1 litres of gear oil.",
            "mesh_housing", V3(1.22f, 1.02f, 0.42f),
            CameraPose(58f, 20f, 1.05f), readoutLabel = "HOUSING",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Torque arrives",
            "The clutch feeds the input shaft through twelve splines. A fixed 18-tooth pinion drives the 37-tooth countershaft gear, the only permanent reduction in the box.",
            GB_INPUT, CameraPose(64f, 16f, 1.06f), 0.4f, 0.75f,
        ),
        GuideStep(
            "Choose a ratio",
            "First gear pairs a 13-tooth pinion with a 42-tooth wheel. Sixth reverses that: 33 teeth drive 22, giving an overdrive ratio for cruising.",
            GB_TRAIN, CameraPose(36f, 24f, 1.08f), 0.55f, 0.9f,
        ),
        GuideStep(
            "Match the speeds",
            "The synchro cone rubs against the gear's cone until both turn at the same speed. Only then does the collar's dog teeth slide in and lock the gear to the shaft.",
            GB_SHIFT, CameraPose(26f, 24f, 1.24f), 0.2f, 0.55f,
        ),
        GuideStep(
            "Move the forks",
            "The lever turns a detent drum; three forks translate that rotation into axial travel. Each fork grips one collar, so only one gear can be engaged at a time.",
            GB_SHIFT, CameraPose(42f, 32f, 1.12f), 0.1f, 0.8f,
        ),
        GuideStep(
            "Torque leaves",
            "The output shaft carries the selected ratio to the final drive. The unselected gears keep spinning freely on their bearings, ready for the next shift.",
            GB_TRAIN, CameraPose(52f, 20f, 1.12f), 0f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the box to separate the bell housing, countershaft cluster, output shaft, synchro pack and selector rail. Every bearing is a service item.",
            null, CameraPose(44f, 28f, 0.98f), 1f, 0.8f,
        ),
    )

    return ObjectSpec(
        id = "gearbox",
        index = 4,
        title = "MESH-6",
        designation = "M-6 / SIX-SPEED MANUAL",
        category = "Automotive",
        categoryShort = "DRIVE",
        tagline = "Six ratios, one sliding tooth at a time.",
        summary = "A six-speed manual transaxle cut open: input splines, countershaft cluster, six wheel gears, three synchro hubs, selector forks and detent drum.",
        details = listOf(
            "MESH-6 is a constant-mesh manual gearbox. The input pinion always drives the countershaft; the six wheel gears always spin, but only the one locked by a synchro collar delivers torque to the output shaft.",
            "The cutaway keeps the front housing open so the gear train is visible. Ratios run from 6.63:1 in first to 1.37:1 in sixth, an overdrive that lets the engine cruise at low rpm.",
        ),
        specs = listOf(
            SpecRow("Layout", "6-speed manual"),
            SpecRow("Ratios", "6.63 – 1.37 : 1"),
            SpecRow("Max torque", "420 Nm"),
            SpecRow("Oil", "2.1 L / 75W-90"),
            SpecRow("Mass", "58 kg"),
        ),
        tags = listOf("automotive", "gearbox", "transmission", "synchro", "gears", "manual", "drivetrain"),
        accent = 0xFFE0A458.toInt(),
        control = ControlSpec(
            label = "GEAR",
            lowLabel = "1ST",
            highLabel = "6TH",
            defaultValue = 0.55f,
            hint = "Gear selection, synchro travel and ratio",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(44f, 24f, 1.12f),
        thumbnailPose = CameraPose(48f, 20f, 1.14f),
        thumbnailTime = 4.6f,
        thumbnailControl = 0.42f,
        readouts = { ctx ->
            val gear = selectedGear(ctx)
            val ratio = overallRatio(gear)
            val outputRpm = ENGINE_RPM / ratio
            val speed = outputRpm / FINAL_DRIVE * 2.02f * 0.06f
            listOf(
                Readout("GEAR", GEAR_LABELS[gear - 1], accent = true),
                Readout("RATIO", String.format("%.2f", ratio), ": 1"),
                Readout("SPEED", "${speed.toInt()}", "km/h"),
            )
        },
        effects = { ctx ->
            val gear = selectedGear(ctx)
            listOf(
                SceneEffect.Glow(V3(-2.08f, 1.57f, -0.12f), 0.42f, 0xFFE0A458.toInt(), 0.16f),
                SceneEffect.Flow(
                    points = listOf(
                        V3(1.45f, 0.72f, 0.18f),
                        V3(0.4f, 0.68f, 0.02f),
                        V3(-0.8f, 0.72f, -0.2f),
                    ),
                    color = 0xFFE0A458.toInt(),
                    count = 9,
                    speed = 0.18f,
                    phase = (gear - 1) * 0.07f,
                    dotRadius = 0.022f,
                ),
                SceneEffect.Ring(V3(0.35f, 0.31f, -0.12f), 1.72f, 0xFFE0A458.toInt(), 0f, 0.26f),
            )
        },
    )
}
