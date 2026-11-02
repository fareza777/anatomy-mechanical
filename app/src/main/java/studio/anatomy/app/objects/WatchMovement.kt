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
import kotlin.math.cos
import kotlin.math.sin

private const val WATCH_CASE = "CASE"
private const val WATCH_POWER = "POWER"
private const val WATCH_TRAIN = "GEAR TRAIN"
private const val WATCH_ESC = "ESCAPEMENT"
private const val WATCH_DIAL = "DIAL"

private const val W_CASE = "CASE"
private const val W_BARREL = "MAINSPRING"
private const val W_WHEELS = "WHEEL TRAIN"
private const val W_ESCAPE = "ESCAPEMENT"
private const val W_BALANCE = "BALANCE"
private const val W_HANDS = "HANDS"
private const val W_DIAL = "DIAL"
private const val W_STAND = "STAND"

private const val DIAL_Y = 1.62f

private fun gearZ(radius: Float, teeth: Int, thickness: Float, depth: Float = 0.022f): Mesh =
    Geo.gear(radius, teeth, thickness, depth, 3).transformed(Mat4.rotateX(90f))

private fun discZ(radius: Float, height: Float, segments: Int = 30, bevel: Float = 0f): Mesh =
    Geo.cylinder(radius, height, segments, bevel = bevel).transformed(Mat4.rotateX(90f))

private fun torusZ(major: Float, minor: Float, majorSegments: Int = 36, minorSegments: Int = 6): Mesh =
    Geo.torus(major, minor, majorSegments, minorSegments).transformed(Mat4.rotateX(90f))

private fun watchSpeed(ctx: AnimContext): Float = 0.25f + 1.35f * ctx.control

private fun watchCenter(x: Float, y: Float, z: Float): V3 = V3(x, DIAL_Y + y, z)

private fun barrelMesh(): Mesh = MeshBuilder().apply {
    addMesh(discZ(0.40f, 0.11f, 40, bevel = 0.015f), Mat4.identity())
    addMesh(gearZ(0.415f, 60, 0.07f), Mat4.identity())
    addMesh(torusZ(0.34f, 0.022f, 36, 6), Mat4.translation(V3(0f, 0f, 0.065f)))
    addMesh(discZ(0.10f, 0.03f, 20), Mat4.translation(V3(0f, 0f, 0.09f)))
}.build()

private fun wheelMesh(radius: Float, teeth: Int, pinionTeeth: Int): Mesh = MeshBuilder().apply {
    addMesh(gearZ(radius, teeth, 0.045f), Mat4.identity())
    addMesh(discZ(radius * 0.22f, 0.09f, 20), Mat4.identity())
    addMesh(gearZ(radius * 0.20f, pinionTeeth, 0.06f), Mat4.translation(V3(0f, 0f, 0.05f)))
    addMesh(Geo.cylinder(0.022f, 0.20f, 10).transformed(Mat4.rotateX(90f)), Mat4.identity())
    for (i in 0 until 4) {
        val angle = Math.toRadians(90.0 * i)
        addMesh(
            Geo.box(radius * 0.16f, radius * 1.5f, 0.018f),
            Mat4.translation(V3((radius * 0.55f * cos(angle)).toFloat(), (radius * 0.55f * sin(angle)).toFloat(), 0.01f))
                .multiply(Mat4.rotateZ(Math.toDegrees(angle).toFloat())),
        )
    }
}.build()

private fun balanceMesh(): Mesh = MeshBuilder().apply {
    addMesh(torusZ(0.285f, 0.028f, 40, 8), Mat4.identity())
    for (i in 0 until 3) {
        addMesh(
            Geo.box(0.55f, 0.028f, 0.022f),
            Mat4.rotateZ(60f * i),
        )
    }
    addMesh(discZ(0.055f, 0.06f, 20), Mat4.identity())
    addMesh(Geo.sphere(0.036f, 12, 6), Mat4.translation(V3(0f, 0f, 0.07f)))
}.build()

private fun palletMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.045f, 0.30f, 0.025f), Mat4.translation(V3(0f, 0.12f, 0f)))
    addMesh(Geo.box(0.16f, 0.035f, 0.025f), Mat4.translation(V3(0.06f, -0.02f, 0f)).multiply(Mat4.rotateZ(-38f)))
    addMesh(Geo.box(0.18f, 0.03f, 0.025f), Mat4.translation(V3(-0.07f, -0.0f, 0f)).multiply(Mat4.rotateZ(32f)))
    addMesh(discZ(0.045f, 0.06f, 16), Mat4.identity())
}.build()

private fun handMesh(length: Float, width: Float, counterweight: Float): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.slab(width, 0.024f, length, 0.008f, 0.004f),
        Mat4.translation(V3(0f, length * 0.5f, 0f)),
    )
    addMesh(Geo.box(width * 0.55f, 0.024f, length * 0.22f), Mat4.translation(V3(0f, -length * 0.10f, 0f)))
    if (counterweight > 0f) {
        addMesh(Geo.cylinder(counterweight, 0.022f, 18).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, -length * 0.09f, 0f)))
    }
}.build()

fun watchMovement(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "chronos",
            width = 2.95f,
            depth = 2.35f,
            top = 0.3f,
            system = WATCH_CASE,
            assembly = W_STAND,
            accentMaterial = Materials.emitAmber,
        )
    )

    val caseMesh = MeshBuilder().apply {
        addMesh(
            Geo.hollowCylinder(1.03f, 0.92f, 0.28f, 52).transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(0f, DIAL_Y, 0f)),
        )
        addMesh(torusZ(1.02f, 0.05f, 48, 8), Mat4.translation(V3(0f, DIAL_Y, 0.15f)))
        addMesh(torusZ(0.95f, 0.035f, 44, 6), Mat4.translation(V3(0f, DIAL_Y, -0.16f)))
        addMesh(discZ(0.92f, 0.035f, 44), Mat4.translation(V3(0f, DIAL_Y, -0.155f)))
        // Lugs, crown and stem.
        listOf(-1f, 1f).forEach { side ->
            addMesh(
                Geo.slab(0.34f, 0.26f, 0.20f, 0.08f, 0.025f, 2),
                Mat4.translation(V3(0f, DIAL_Y + side * 1.00f, 0f)),
            )
            addMesh(
                Geo.cylinder(0.055f, 0.34f, 16).transformed(Mat4.rotateZ(90f)),
                Mat4.translation(V3(0f, DIAL_Y + side * 1.16f, 0f)),
            )
        }
        addMesh(discZ(0.13f, 0.10f, 26).transformed(Mat4.rotateY(90f)), Mat4.translation(V3(1.10f, DIAL_Y, 0f)))
        addMesh(Geo.cylinder(0.045f, 0.16f, 14).transformed(Mat4.rotateY(90f)), Mat4.translation(V3(1.00f, DIAL_Y, 0f)))
        for (i in 0 until 18) {
            addMesh(
                Geo.box(0.02f, 0.02f, 0.10f),
                Mat4.translation(V3(1.16f, DIAL_Y, 0f)).multiply(Mat4.rotateX(i * 20f)),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "watch_case", name = "Polished case and lugs", system = WATCH_CASE, assembly = W_CASE,
            mesh = caseMesh, material = Materials.darkSteel,
            explode = V3(0f, 0f, -0.6f),
        )
    )

    val plate = MeshBuilder().apply {
        listOf(
            listOf(V3(-0.55f, 0.72f, -0.03f), V3(-0.20f, 0.46f, -0.03f), V3(0.08f, 0.16f, -0.03f)),
            listOf(V3(0.18f, 0.76f, -0.03f), V3(0.44f, 0.50f, -0.03f), V3(0.55f, 0.22f, -0.03f)),
            listOf(V3(-0.78f, -0.12f, -0.03f), V3(-0.58f, -0.28f, -0.03f), V3(-0.28f, -0.54f, -0.03f)),
        ).forEach { path ->
            addMesh(Geo.tube(path, 0.042f, 6, subdivide = 3), Mat4.identity())
        }
    }.build()
    parts.add(
        Part(
            id = "watch_plate", name = "Skeleton bridges", system = WATCH_CASE, assembly = W_CASE,
            mesh = plate, material = Materials.bluedSteel,
            origin = V3(0f, DIAL_Y, 0f),
            explode = V3(0f, 0f, -0.25f),
        )
    )
    parts.add(
        Part(
            id = "watch_base", name = "Movement base plate", system = WATCH_CASE, assembly = W_CASE,
            mesh = MeshBuilder().apply {
                addMesh(discZ(0.90f, 0.05f, 44, bevel = 0.01f), Mat4.identity())
                addMesh(torusZ(0.84f, 0.02f, 42, 5), Mat4.translation(V3(0f, 0f, 0.03f)))
            }.build(),
            material = Materials.ink,
            origin = V3(0f, DIAL_Y, -0.09f),
            explode = V3(0f, 0f, -0.35f),
        )
    )

    val barrelOrigin = watchCenter(-0.28f, 0.22f, 0.02f)
    parts.add(
        Part(
            id = "watch_barrel", name = "Mainspring barrel", system = WATCH_POWER, assembly = W_BARREL,
            mesh = barrelMesh(), material = Materials.copperBright,
            origin = barrelOrigin,
            animate = { ctx -> Mat4.rotateZ(-ctx.machineTime * 12f * watchSpeed(ctx)) },
            explode = V3(-0.2f, 0f, 0.55f),
        )
    )

    val wheels = listOf(
        Triple(0.18f, -0.28f, Triple(0.30f, 45, 12)),
        Triple(0.52f, 0.00f, Triple(0.20f, 30, 10)),
        Triple(0.56f, 0.36f, Triple(0.13f, 20, 8)),
        Triple(0.34f, 0.56f, Triple(0.09f, 14, 7)),
    )
    val wheelMaterials = listOf(Materials.brass, Materials.gold, Materials.nickel, Materials.bluedSteel)
    wheels.forEachIndexed { index, (x, y, spec) ->
        val (radius, teeth, pinion) = spec
        val speedFactor = 30f / radius
        parts.add(
            Part(
                id = "watch_wheel_${index + 1}", name = "Wheel ${index + 1}",
                system = WATCH_TRAIN, assembly = W_WHEELS,
                mesh = wheelMesh(radius, teeth, pinion), material = wheelMaterials[index % wheelMaterials.size],
                origin = watchCenter(x, y, 0.05f + index * 0.006f),
                animate = { ctx ->
                    val direction = if (index % 2 == 0) 1f else -1f
                    Mat4.rotateZ(direction * ctx.machineTime * speedFactor * watchSpeed(ctx))
                },
                explode = V3(0.2f, 0.1f, 0.5f),
            )
        )
    }

    val escapeOrigin = watchCenter(-0.05f, -0.62f, 0.10f)
    parts.add(
        Part(
            id = "watch_escape", name = "Escape wheel", system = WATCH_ESC, assembly = W_ESCAPE,
            mesh = MeshBuilder().apply {
                addMesh(gearZ(0.16f, 15, 0.03f), Mat4.identity())
                addMesh(discZ(0.045f, 0.07f, 16), Mat4.identity())
                for (i in 0 until 4) {
                    addMesh(
                        Geo.box(0.24f, 0.018f, 0.014f),
                        Mat4.rotateZ(45f * i),
                    )
                }
            }.build(),
            material = Materials.nickel,
            origin = escapeOrigin,
            animate = { ctx -> Mat4.rotateZ(ctx.machineTime * 140f * watchSpeed(ctx)) },
            explode = V3(-0.1f, -0.2f, 0.55f),
        )
    )
    parts.add(
        Part(
            id = "watch_pallet", name = "Pallet fork", system = WATCH_ESC, assembly = W_ESCAPE,
            mesh = palletMesh(), material = Materials.bluedSteel,
            origin = watchCenter(-0.24f, -0.50f, 0.13f),
            animate = { ctx ->
                Mat4.rotateZ(11f * sin(ctx.machineTime * 7.5f * watchSpeed(ctx)))
            },
            explode = V3(-0.15f, -0.15f, 0.6f),
        )
    )

    parts.add(
        Part(
            id = "watch_balance", name = "Balance and hairspring", system = WATCH_ESC, assembly = W_BALANCE,
            mesh = balanceMesh(), material = Materials.nickel,
            origin = watchCenter(-0.47f, -0.40f, 0.10f),
            animate = { ctx ->
                val amplitude = 90f + 175f * ctx.control
                Mat4.rotateZ(amplitude * sin(ctx.machineTime * 7.5f * watchSpeed(ctx)))
            },
            explode = V3(-0.3f, -0.2f, 0.65f),
        )
    )
    parts.add(
        Part(
            id = "watch_hairspring", name = "Hairspring", system = WATCH_ESC, assembly = W_BALANCE,
            mesh = Geo.helix(0.17f, 0.007f, 0.026f, 5, 16).transformed(Mat4.rotateX(90f)),
            material = Materials.bluedSteel,
            origin = watchCenter(-0.47f, -0.40f, 0.055f),
            explode = V3(-0.3f, -0.2f, 0.65f),
        )
    )
    parts.add(
        Part(
            id = "watch_jewel", name = "Ruby jewel", system = WATCH_ESC, assembly = W_BALANCE,
            mesh = Geo.sphere(0.04f, 14, 7), material = Materials.ruby,
            origin = watchCenter(-0.47f, -0.40f, 0.185f),
            explode = V3(-0.3f, -0.2f, 0.65f),
        )
    )

    // Blued screws on the bridges.
    val screws = MeshBuilder().apply {
        listOf(
            V3(-0.55f, 0.72f, 0.02f),
            V3(0.44f, 0.50f, 0.02f),
            V3(-0.28f, -0.54f, 0.02f),
            V3(0.18f, 0.76f, 0.02f),
        ).forEach { point ->
            addMesh(Geo.cylinder(0.035f, 0.03f, 14), Mat4.translation(point).multiply(Mat4.rotateX(90f)))
        }
    }.build()
    parts.add(
        Part(
            id = "watch_screws", name = "Blued screws", system = WATCH_CASE, assembly = W_CASE,
            mesh = screws, material = Materials.bluedSteel,
            origin = V3(0f, DIAL_Y, 0f),
            explode = V3(0f, 0f, 0.15f),
        )
    )

    // Hands above the movement.
    val handOrigin = V3(0f, DIAL_Y, 0.24f)
    parts.add(
        Part(
            id = "watch_hour", name = "Hour hand", system = WATCH_DIAL, assembly = W_HANDS,
            mesh = handMesh(0.52f, 0.055f, 0.06f), material = Materials.bluedSteel,
            origin = handOrigin,
            animate = { ctx -> Mat4.rotateZ(-ctx.machineTime * 0.06f * watchSpeed(ctx)) },
            explode = V3(0f, 0f, 0.85f),
        )
    )
    parts.add(
        Part(
            id = "watch_minute", name = "Minute hand", system = WATCH_DIAL, assembly = W_HANDS,
            mesh = handMesh(0.78f, 0.042f, 0.05f), material = Materials.ceramic,
            origin = handOrigin + V3(0f, 0f, 0.035f),
            animate = { ctx -> Mat4.rotateZ(-ctx.machineTime * 0.7f * watchSpeed(ctx)) },
            explode = V3(0f, 0f, 0.95f),
        )
    )
    parts.add(
        Part(
            id = "watch_second", name = "Seconds hand", system = WATCH_DIAL, assembly = W_HANDS,
            mesh = handMesh(0.90f, 0.018f, 0.03f), material = Materials.emitRed,
            origin = handOrigin + V3(0f, 0f, 0.07f),
            animate = { ctx -> Mat4.rotateZ(-ctx.machineTime * 40f * watchSpeed(ctx)) },
            explode = V3(0f, 0f, 1.05f),
        )
    )
    parts.add(
        Part(
            id = "watch_hub", name = "Hands hub", system = WATCH_DIAL, assembly = W_HANDS,
            mesh = MeshBuilder().apply {
                addMesh(discZ(0.07f, 0.045f, 22), Mat4.identity())
                addMesh(torusZ(0.055f, 0.012f, 18, 5), Mat4.translation(V3(0f, 0f, 0.025f)))
            }.build(),
            material = Materials.bluedSteel,
            origin = handOrigin + V3(0f, 0f, 0.10f),
            explode = V3(0f, 0f, 1.1f),
        )
    )

    val markers = MeshBuilder().apply {
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0)
            val radius = 0.82f
            addMesh(
                Geo.box(0.045f, 0.13f, 0.035f),
                Mat4.translation(
                    V3(
                        (radius * sin(angle)).toFloat(),
                        DIAL_Y + (radius * cos(angle)).toFloat(),
                        0.19f,
                    )
                ).multiply(Mat4.rotateZ(-i * 30f)),
            )
        }
        addMesh(torusZ(0.86f, 0.012f, 48, 5), Mat4.translation(V3(0f, DIAL_Y, 0.18f)))
    }.build()
    parts.add(
        Part(
            id = "watch_markers", name = "Hour markers", system = WATCH_DIAL, assembly = W_DIAL,
            mesh = markers, material = Materials.bluedSteel,
            explode = V3(0f, 0f, 0.65f),
        )
    )

    val stand = MeshBuilder().apply {
        addMesh(
            Geo.slab(1.10f, 0.55f, 0.07f, 0.06f, 0.02f),
            Mat4.translation(V3(0f, 0.36f, -0.34f)),
        )
        addMesh(
            Geo.slab(0.10f, 0.12f, 1.55f, 0.03f, 0.01f),
            Mat4.translation(V3(0f, 1.22f, -0.32f)),
        )
        addMesh(torusZ(0.78f, 0.035f, 40, 6), Mat4.translation(V3(0f, DIAL_Y, -0.30f)))
        listOf(-1f, 1f).forEach { side ->
            addMesh(
                Geo.cylinderBetween(
                    V3(side * 0.62f, 0.40f, -0.34f),
                    V3(side * 0.34f, 1.10f, -0.32f),
                    0.028f, 8,
                ),
            )
        }
    }.build()
    parts.add(
        Part(
            id = "watch_stand", name = "Display easel", system = WATCH_CASE, assembly = W_STAND,
            mesh = stand, material = Materials.darkSteel,
            explode = V3(0f, -0.7f, -0.4f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "barrel", "Mainspring barrel",
            "The mainspring is a ribbon of steel coiled inside the barrel. Winding it stores energy; the barrel releases it through the teeth at a nearly constant torque.",
            "watch_barrel", V3(0.02f, 0.04f, 0.12f),
            CameraPose(24f, 18f, 1.22f), readoutLabel = "POWER",
        ),
        Hotspot(
            "wheels", "Wheel train",
            "Four wheels multiply the barrel's slow turn into a fast escape wheel. Each stage trades torque for speed, and the ratios set the watch's rate.",
            "watch_wheel_2", V3(0f, 0f, 0.08f),
            CameraPose(16f, 14f, 1.3f), readoutLabel = "GEAR TRAIN",
        ),
        Hotspot(
            "pallet", "Pallet fork",
            "The fork locks and unlocks the escape wheel once per swing. That tick is the only moment the train is allowed to advance.",
            "watch_pallet", V3(0f, 0.08f, 0.08f),
            CameraPose(8f, 18f, 1.35f), readoutLabel = "ESCAPEMENT",
        ),
        Hotspot(
            "balance", "Balance wheel",
            "A weighted wheel swings back and forth, and its period sets the time. The hairspring pulls it back; the pallet fork keeps it fed with tiny pushes.",
            "watch_balance", V3(0f, 0f, 0.08f),
            CameraPose(14f, 10f, 1.28f), readoutLabel = "OSCILLATOR",
        ),
        Hotspot(
            "hands", "Hands",
            "Three hands on concentric tubes. The seconds hand is driven directly; the minute and hour hands step down through the motion works.",
            "watch_hour", V3(0f, 0.40f, 0.06f),
            CameraPose(12f, 8f, 1.24f), readoutLabel = "INDICATION",
        ),
        Hotspot(
            "crown", "Crown and stem",
            "The crown winds the mainspring through a stem and clutch. Pulling it out disengages the winding and lets the hands be set.",
            "watch_case", V3(1.12f, DIAL_Y, 0.2f),
            CameraPose(72f, 12f, 1.2f), readoutLabel = "WINDING",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Store the energy",
            "Turning the crown winds a flat steel spring inside the barrel. Forty hours of running fit in a drum smaller than a coin.",
            W_BARREL, CameraPose(26f, 16f, 1.12f), 0.3f, 0.85f,
        ),
        GuideStep(
            "Multiply the speed",
            "The barrel turns once every eight hours; the escape wheel turns once a minute. The wheel train multiplies that motion through four meshing pairs.",
            W_WHEELS, CameraPose(16f, 14f, 1.18f), 0.5f, 0.9f,
        ),
        GuideStep(
            "Tick",
            "The pallet fork locks the escape wheel, then releases one tooth as the balance swings. That lock-and-release is the ticking sound.",
            W_ESCAPE, CameraPose(6f, 18f, 1.25f), 0.15f, 0.7f,
        ),
        GuideStep(
            "Keep time",
            "The balance wheel swings 250 degrees or more. Its hairspring returns it every time, and the escapement replaces the tiny energy lost to friction.",
            W_BALANCE, CameraPose(14f, 10f, 1.2f), 0.1f, 0.95f,
        ),
        GuideStep(
            "Show the time",
            "The hands ride on tubes that run through the centre of the movement. Hour, minute and second all share one axis.",
            W_DIAL, CameraPose(10f, 8f, 1.14f), 0f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the watch to separate case, bridges, barrel, wheels, escapement, balance and hands. A watchmaker services them in this order.",
            null, CameraPose(26f, 20f, 0.98f), 1f, 0.75f,
        ),
    )

    return ObjectSpec(
        id = "watch_movement",
        index = 10,
        title = "CHRONOS 01",
        designation = "C-01 / SKELETON MOVEMENT",
        category = "Precision",
        categoryShort = "TIME",
        tagline = "A hundred and fifty parts, one tick.",
        summary = "A skeleton mechanical watch opened to its movement: mainspring barrel, four-wheel train, pallet fork, oscillating balance, hairspring and coaxial hands.",
        details = listOf(
            "CHRONOS 01 is a hand-wound skeleton watch. The mainspring barrel drives a four-wheel train that ends at the escape wheel, and the balance releases it one tooth at a time.",
            "Every bridge is cut away so the motion is visible. The seconds hand sweeps, the pallet fork snaps, and the balance wheel swings at five hertz, regulated by the hairspring.",
        ),
        specs = listOf(
            SpecRow("Movement", "Manual / 21,600 vph"),
            SpecRow("Jewels", "19 rubies"),
            SpecRow("Reserve", "42 hours"),
            SpecRow("Case", "38 mm steel"),
            SpecRow("Rate", "+2 s / day"),
        ),
        tags = listOf("precision", "watch", "horology", "mechanical", "gears", "balance", "escapement"),
        accent = 0xFFE0C27A.toInt(),
        control = ControlSpec(
            label = "POWER",
            lowLabel = "STOPPED",
            highLabel = "FULL",
            defaultValue = 0.82f,
            hint = "Mainspring torque, balance amplitude and hands",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(20f, 10f, 1.1f),
        thumbnailPose = CameraPose(22f, 10f, 1.12f),
        thumbnailTime = 3.4f,
        thumbnailControl = 0.9f,
        readouts = { ctx ->
            val amplitude = 90f + 175f * ctx.control
            val reserve = 8f + 34f * ctx.control
            val rate = 1.5f + 1.2f * sin(ctx.machineTime * 0.2f)
            listOf(
                Readout("AMPLITUDE", String.format("%.0f", amplitude), "°", accent = true),
                Readout("RESERVE", String.format("%.0f", reserve), "h"),
                Readout("RATE", String.format("%+.1f", rate), "s/d"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Glow(V3(-0.47f, DIAL_Y - 0.40f, 0.22f), 0.30f, 0xFFFF5C6C.toInt(), 0.16f + 0.12f * ctx.control),
                SceneEffect.Glow(V3(-0.28f, DIAL_Y + 0.22f, 0.14f), 0.42f, 0xFFE0C27A.toInt(), 0.1f),
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.55f, 0xFFE0C27A.toInt(), 0f, 0.22f),
            )
        },
    )
}
