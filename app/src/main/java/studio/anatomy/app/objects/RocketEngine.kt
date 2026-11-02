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

private const val ROCKET_STAND = "STAND"
private const val ROCKET_MOUNT = "MOUNT"
private const val ROCKET_FEED = "FEED SYSTEM"
private const val ROCKET_POWER = "POWERHEAD"
private const val ROCKET_NOZZLE = "NOZZLE"

private const val R_STAND = "TEST STAND"
private const val R_GIMBAL = "GIMBAL"
private const val R_PUMP = "TURBOPUMP"
private const val R_CHAMBER = "CHAMBER"
private const val R_INJECTOR = "INJECTOR"
private const val R_BELL = "NOZZLE"
private const val R_LINES = "PLUMBING"

private const val PIVOT_Y = 3.22f
private const val THROAT_Y = 2.16f
private const val EXIT_Y = 1.05f

private fun engineGimbal(ctx: AnimContext): Mat4 {
    val amp = 0.35f + 0.65f * ctx.control
    val yaw = 0.85f * sin(ctx.machineTime * 0.42f) * amp
    val pitch = 0.62f * sin(ctx.machineTime * 0.33f + 1.1f) * amp
    return Mat4.translation(V3(0f, PIVOT_Y, 0f))
        .multiply(Mat4.rotateX(pitch))
        .multiply(Mat4.rotateZ(yaw))
        .multiply(Mat4.translation(V3(0f, -PIVOT_Y, 0f)))
}

private fun bellProfile(): List<Pair<Float, Float>> = listOf(
    0.92f to EXIT_Y,
    0.86f to 1.13f,
    0.78f to 1.23f,
    0.68f to 1.35f,
    0.57f to 1.48f,
    0.46f to 1.61f,
    0.37f to 1.74f,
    0.29f to 1.87f,
    0.235f to 1.99f,
    0.205f to 2.08f,
    0.198f to THROAT_Y,
)

private fun bellRadiusAt(y: Float): Float {
    val profile = bellProfile()
    if (y <= profile.first().second) return profile.first().first
    if (y >= profile.last().second) return profile.last().first
    for (i in 0 until profile.size - 1) {
        val (r0, y0) = profile[i]
        val (r1, y1) = profile[i + 1]
        if (y in y0..y1) {
            val t = (y - y0) / (y1 - y0)
            return r0 + (r1 - r0) * t
        }
    }
    return profile.last().first
}

private fun nozzleShell(): Mesh = Geo.lathe(bellProfile(), 46, capBottom = false, capTop = false)

private fun coolingRibs(): Mesh = MeshBuilder().apply {
    for (i in 0 until 16) {
        val angle = Math.toRadians(i * 22.5)
        val points = ArrayList<V3>()
        for (step in 0 until 7) {
            val y = 1.16f + step * 0.165f
            val radius = bellRadiusAt(y) + 0.018f
            points.add(V3((radius * cos(angle)).toFloat(), y, (radius * sin(angle)).toFloat()))
        }
        addMesh(Geo.tube(points, 0.022f, 5, subdivide = 1, caps = true))
    }
}.build()

private fun chamberMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.lathe(
            listOf(
                0.198f to THROAT_Y,
                0.28f to 2.22f,
                0.33f to 2.30f,
                0.355f to 2.44f,
                0.345f to 2.60f,
                0.31f to 2.76f,
                0.255f to 2.88f,
                0.17f to 2.96f,
            ),
            38,
        ),
    )
    for (y in listOf(2.30f, 2.48f, 2.66f, 2.82f)) {
        addMesh(Geo.torus(bellRadiusAt(y).coerceAtLeast(0.24f) + 0.02f, 0.022f, 34, 6), Mat4.translation(V3(0f, y, 0f)))
    }
}.build()

private fun injectorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.lathe(listOf(0.17f to 2.96f, 0.26f to 3.0f, 0.28f to 3.05f, 0.18f to 3.10f, 0.02f to 3.12f), 30))
    addMesh(Geo.cylinder(0.335f, 0.055f, 36, bevel = 0.01f), Mat4.translation(V3(0f, 2.975f, 0f)))
    addMesh(Geo.torus(0.315f, 0.026f, 34, 6), Mat4.translation(V3(0f, 2.945f, 0f)))
    // Injector face orifices, visible from below.
    for (ring in 0 until 2) {
        val radius = 0.12f + ring * 0.11f
        val count = 8 + ring * 6
        for (i in 0 until count) {
            val angle = Math.toRadians(i * 360.0 / count + ring * 12.0)
            addMesh(
                Geo.cylinder(0.014f, 0.03f, 8),
                Mat4.translation(
                    V3(
                        (radius * cos(angle)).toFloat(),
                        2.938f,
                        (radius * sin(angle)).toFloat(),
                    )
                ),
            )
        }
    }
}.build()

private fun pumpMesh(): Mesh = MeshBuilder().apply {
    // Main pump body, on a horizontal shaft beside the chamber.
    addMesh(
        Geo.cylinder(0.235f, 0.40f, 30, bevel = 0.02f)
            .transformed(Mat4.translation(V3(0.92f, 2.52f, 0.05f)).multiply(Mat4.rotateZ(-90f))),
    )
    addMesh(
        Geo.torus(0.30f, 0.085f, 30, 8)
            .transformed(Mat4.translation(V3(0.82f, 2.52f, 0.05f)).multiply(Mat4.rotateZ(-90f))),
    )
    addMesh(
        Geo.cylinder(0.185f, 0.30f, 26, bevel = 0.015f)
            .transformed(Mat4.translation(V3(1.30f, 2.52f, 0.05f)).multiply(Mat4.rotateZ(-90f))),
    )
    addMesh(Geo.torus(0.19f, 0.03f, 26, 6), Mat4.translation(V3(1.30f, 2.52f, 0.05f)).multiply(Mat4.rotateX(90f)))
    addMesh(
        Geo.cylinder(0.075f, 0.10f, 18),
        Mat4.translation(V3(1.47f, 2.52f, 0.05f)).multiply(Mat4.rotateZ(-90f)),
    )
    // Turbine exhaust barrel.
    addMesh(
        Geo.cylinder(0.13f, 0.36f, 22, bevel = 0.015f),
        Mat4.translation(V3(0.62f, 2.32f, -0.18f)).multiply(Mat4.rotateX(24f)),
    )
    // Mounting saddle to the thrust frame.
    addMesh(
        Geo.slab(0.30f, 0.44f, 0.10f, 0.04f, 0.015f),
        Mat4.translation(V3(0.98f, 2.78f, 0.05f)),
    )
}.build()

private fun preburnerMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.cylinder(0.155f, 0.34f, 24, bevel = 0.02f)
            .transformed(Mat4.translation(V3(1.02f, 2.98f, 0.05f)).multiply(Mat4.rotateZ(-90f))),
    )
    addMesh(Geo.torus(0.155f, 0.024f, 24, 6), Mat4.translation(V3(1.02f, 2.98f, 0.05f)).multiply(Mat4.rotateZ(90f)))
    addMesh(
        Geo.cylinder(0.065f, 0.12f, 14),
        Mat4.translation(V3(0.84f, 2.90f, 0.05f)).multiply(Mat4.rotateX(-35f)),
    )
}.build()

private fun feedLines(): Mesh = MeshBuilder().apply {
    // Oxidiser: from the stand leg up to the pump inlet.
    addMesh(
        Geo.tube(
            listOf(
                V3(1.16f, 0.72f, 0.92f),
                V3(1.16f, 1.55f, 0.88f),
                V3(1.30f, 2.18f, 0.62f),
                V3(1.30f, 2.40f, 0.24f),
                V3(1.22f, 2.50f, 0.08f),
            ),
            0.052f, 8, subdivide = 4,
        ),
    )
    addMesh(Geo.torus(0.062f, 0.016f, 18, 5), Mat4.translation(V3(1.16f, 1.30f, 0.9f)).multiply(Mat4.rotateX(90f)))
    // Fuel: pump outlet to the injector dome.
    addMesh(
        Geo.tube(
            listOf(
                V3(0.72f, 2.66f, 0.05f),
                V3(0.52f, 2.86f, 0.02f),
                V3(0.30f, 3.02f, 0.0f),
                V3(0.14f, 3.08f, 0.0f),
            ),
            0.044f, 8, subdivide = 4,
        ),
    )
    addMesh(Geo.torus(0.108f, 0.02f, 20, 5), Mat4.translation(V3(0.72f, 2.66f, 0.05f)).multiply(Mat4.rotateZ(90f)))
    // Hot gas from the preburner into the turbine.
    addMesh(
        Geo.tube(
            listOf(
                V3(1.02f, 2.80f, 0.05f),
                V3(1.14f, 2.70f, 0.02f),
                V3(1.24f, 2.60f, 0.03f),
                V3(1.30f, 2.55f, 0.05f),
            ),
            0.05f, 7, subdivide = 3,
        ),
    )
    // Regenerative coolant return along the bell.
    addMesh(
        Geo.tube(
            listOf(
                V3(0.57f, 1.48f, -0.62f),
                V3(0.44f, 1.75f, -0.60f),
                V3(0.34f, 2.06f, -0.44f),
                V3(0.27f, 2.40f, -0.34f),
            ),
            0.034f, 6, subdivide = 3,
        ),
    )
    // Pogo suppression accumulator on the feed line.
    addMesh(
        Geo.cylinder(0.10f, 0.28f, 20, bevel = 0.02f),
        Mat4.translation(V3(1.34f, 1.82f, 0.80f)),
    )
}.build()

private fun standMesh(): Mesh = MeshBuilder().apply {
    val feet = listOf(-1.18f to -1.18f, 1.18f to -1.18f, -1.18f to 1.18f, 1.18f to 1.18f)
    feet.forEach { (x, z) ->
        val foot = V3(x, 0.32f, z)
        val head = V3(x * 0.82f, 3.52f, z * 0.82f)
        addMesh(Geo.cylinderBetween(foot, head, 0.085f, 12))
        addMesh(Geo.taperedCylinder(0.20f, 0.11f, 0.10f, 18), Mat4.translation(foot))
        addMesh(Geo.torus(0.115f, 0.022f, 18, 5), Mat4.translation(foot + V3(0f, 0.055f, 0f)))
    }
    // Cross bracing between neighbouring legs.
    val corners = listOf(V3(-1.18f, 0f, -1.18f), V3(1.18f, 0f, -1.18f), V3(1.18f, 0f, 1.18f), V3(-1.18f, 0f, 1.18f))
    for (level in listOf(1.0f, 2.1f)) {
        for (i in 0 until 4) {
            val a = corners[i] * (1f - level / 5.2f) + V3(0f, level, 0f)
            val b = corners[(i + 1) % 4] * (1f - level / 5.2f) + V3(0f, level, 0f)
            addMesh(Geo.cylinderBetween(a, b, 0.032f, 8))
        }
    }
    // Top ring and thrust plate.
    addMesh(Geo.hollowCylinder(1.10f, 0.96f, 0.14f, 44), Mat4.translation(V3(0f, 3.55f, 0f)))
    addMesh(Geo.torus(1.08f, 0.03f, 40, 6), Mat4.translation(V3(0f, 3.62f, 0f)))
    addMesh(Geo.cylinder(0.62f, 0.10f, 36, bevel = 0.015f), Mat4.translation(V3(0f, 3.42f, 0f)))
    addMesh(Geo.torus(0.60f, 0.035f, 34, 6), Mat4.translation(V3(0f, 3.47f, 0f)))
    for (i in 0 until 4) {
        val angle = Math.toRadians(45.0 + i * 90.0)
        val edge = V3((1.02 * cos(angle)).toFloat(), 3.44f, (1.02 * sin(angle)).toFloat())
        addMesh(Geo.cylinderBetween(V3.ZERO + V3(0f, 3.44f, 0f) + V3((0.30 * cos(angle)).toFloat(), 0f, (0.30 * sin(angle)).toFloat()), edge, 0.045f, 8))
    }
    // Flame deflector under the bell.
    addMesh(
        Geo.slab(1.90f, 1.55f, 0.14f, 0.16f, 0.04f, 3),
        Mat4.translation(V3(0f, 0.38f, 0f)),
    )
    addMesh(
        Geo.taperedCylinder(0.52f, 0.22f, 0.14f, 28),
        Mat4.translation(V3(0f, 0.50f, 0f)),
    )
    addMesh(Geo.torus(0.50f, 0.03f, 30, 6), Mat4.translation(V3(0f, 0.56f, 0f)))
}.build()

private fun gimbalMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.115f, 0.62f, 22, bevel = 0.015f).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(0f, PIVOT_Y, 0f)))
    addMesh(Geo.cylinder(0.105f, 0.56f, 22, bevel = 0.015f).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, PIVOT_Y, 0f)))
    addMesh(Geo.hollowCylinder(0.40f, 0.30f, 0.09f, 34), Mat4.translation(V3(0f, 3.10f, 0f)))
    for (i in 0 until 4) {
        val angle = Math.toRadians(i * 90.0 + 45.0)
        val point = V3((0.36 * cos(angle)).toFloat(), 3.10f, (0.36 * sin(angle)).toFloat())
        addMesh(Geo.cylinderBetween(point, V3((0.26 * cos(angle)).toFloat(), 3.24f, (0.26 * sin(angle)).toFloat()), 0.035f, 8))
    }
}.build()

private fun actuators(): Mesh = MeshBuilder().apply {
    listOf(-1f, 1f).forEach { side ->
        val low = V3(side * 0.66f, 3.38f, side * 0.30f)
        val high = V3(side * 0.28f, 2.92f, side * 0.18f)
        addMesh(Geo.cylinderBetween(low, high, 0.058f, 12))
        addMesh(Geo.cylinderBetween(high, high + (high - low) * 0.18f, 0.075f, 12))
        addMesh(Geo.sphere(0.075f, 12, 6), Mat4.translation(low))
        addMesh(Geo.sphere(0.065f, 12, 6), Mat4.translation(high))
    }
}.build()

private fun sensorMesh(): Mesh = MeshBuilder().apply {
    listOf(0.0f, 0.8f, 1.7f).forEachIndexed { index, offset ->
        val angle = Math.toRadians(40.0 + offset * 57.0)
        val radius = 0.355f
        addMesh(
            Geo.cylinder(0.035f, 0.045f, 10),
            Mat4.translation(
                V3(
                    (radius * cos(angle)).toFloat(),
                    2.42f + index * 0.16f,
                    (radius * sin(angle)).toFloat(),
                )
            ).multiply(Mat4.rotateZ(90f)),
        )
    }
}.build()

fun rocketEngine(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "kestrel",
            width = 3.35f,
            depth = 3.15f,
            top = 0.3f,
            system = ROCKET_STAND,
            assembly = R_STAND,
            accentMaterial = Materials.emitRed,
        )
    )

    parts.add(
        Part(
            id = "rk_stand", name = "Static test stand", system = ROCKET_STAND, assembly = R_STAND,
            mesh = standMesh(), material = Materials.darkSteel,
            explode = V3(0f, -0.9f, 0f),
        )
    )
    parts.add(
        Part(
            id = "rk_gimbal", name = "Gimbal block and actuators", system = ROCKET_MOUNT, assembly = R_GIMBAL,
            mesh = MeshBuilder().apply {
                addMesh(gimbalMesh())
                addMesh(actuators())
            }.build(),
            material = Materials.titanium,
            explode = V3(0f, 0.65f, 0f),
        )
    )

    val engineAnimate: (AnimContext) -> Mat4 = { ctx -> engineGimbal(ctx) }

    parts.add(
        Part(
            id = "rk_chamber", name = "Thrust chamber", system = ROCKET_POWER, assembly = R_CHAMBER,
            mesh = chamberMesh(), material = Materials.copperBright,
            animate = engineAnimate,
            explode = V3(0f, 0.45f, 0.2f),
        )
    )
    parts.add(
        Part(
            id = "rk_injector", name = "Injector dome", system = ROCKET_POWER, assembly = R_INJECTOR,
            mesh = injectorMesh(), material = Materials.brass,
            animate = engineAnimate,
            explode = V3(0f, 0.95f, 0.2f),
        )
    )
    parts.add(
        Part(
            id = "rk_nozzle", name = "Regenerative nozzle", system = ROCKET_NOZZLE, assembly = R_BELL,
            mesh = nozzleShell(), material = Materials.graphite,
            animate = engineAnimate, doubleSided = true,
            explode = V3(0f, -0.65f, 0f),
        )
    )
    parts.add(
        Part(
            id = "rk_ribs", name = "Cooling channels", system = ROCKET_NOZZLE, assembly = R_BELL,
            mesh = coolingRibs(), material = Materials.copper,
            animate = engineAnimate, doubleSided = true,
            explode = V3(0f, -0.65f, 0f),
        )
    )

    parts.add(
        Part(
            id = "rk_pump", name = "Turbopump", system = ROCKET_FEED, assembly = R_PUMP,
            mesh = pumpMesh(), material = Materials.titanium,
            animate = engineAnimate,
            explode = V3(0.85f, 0.15f, 0.25f),
        )
    )
    parts.add(
        Part(
            id = "rk_rotor", name = "Pump inducer", system = ROCKET_FEED, assembly = R_PUMP,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.165f, 0.045f, 24), Mat4.translation(V3(1.55f, 2.52f, 0.05f)).multiply(Mat4.rotateZ(-90f)))
                for (i in 0 until 7) {
                    addMesh(
                        Geo.slab(0.03f, 0.20f, 0.06f, 0.012f, 0.005f),
                        Mat4.translation(V3(1.58f, 2.52f, 0.05f))
                            .multiply(Mat4.rotateX(51.4f * i))
                            .multiply(Mat4.translation(V3(0f, 0.10f, 0f))),
                    )
                }
            }.build(),
            material = Materials.brass,
            animate = { ctx ->
                val center = V3(1.56f, 2.52f, 0.05f)
                engineGimbal(ctx)
                    .multiply(Mat4.translation(center))
                    .multiply(Mat4.rotateX(ctx.machineTime * 320f))
                    .multiply(Mat4.translation(-center))
            },
            explode = V3(1.1f, 0.15f, 0.25f),
        )
    )
    parts.add(
        Part(
            id = "rk_preburner", name = "Preburner", system = ROCKET_FEED, assembly = R_PUMP,
            mesh = preburnerMesh(), material = Materials.darkSteel,
            animate = engineAnimate,
            explode = V3(0.95f, 0.65f, 0.1f),
        )
    )
    parts.add(
        Part(
            id = "rk_lines", name = "Feed and coolant lines", system = ROCKET_FEED, assembly = R_LINES,
            mesh = feedLines(), material = Materials.brushedSteel,
            animate = engineAnimate,
            explode = V3(0.75f, 0.1f, 0.9f),
        )
    )
    parts.add(
        Part(
            id = "rk_sensors", name = "Chamber sensors", system = ROCKET_POWER, assembly = R_CHAMBER,
            mesh = sensorMesh(), material = Materials.emitAmber,
            animate = engineAnimate,
            explode = V3(0f, 0.4f, 0.5f),
        )
    )
    parts.add(
        Part(
            id = "rk_preburner_glow", name = "Preburner glow ring", system = ROCKET_FEED, assembly = R_PUMP,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.165f, 0.02f, 22, 5), Mat4.translation(V3(0.90f, 2.98f, 0.05f)).multiply(Mat4.rotateZ(90f)))
            }.build(),
            material = Materials.emitRed,
            animate = engineAnimate,
            explode = V3(0.95f, 0.65f, 0.1f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "bell", "Regenerative nozzle",
            "The bell expands hot gas from throat to exit, trading pressure for velocity. Its inner wall is lined with cooling channels that carry fuel before it ever reaches the injector.",
            "rk_nozzle", V3(0.52f, 1.55f, 0.3f),
            CameraPose(62f, 6f, 1.12f), readoutLabel = "EXPANSION",
        ),
        Hotspot(
            "channels", "Cooling channels",
            "Sixteen raised channels braze onto the bell. Cryogenic fuel runs through them, picks up heat, then enters the injector hot enough to vaporise instantly.",
            "rk_ribs", V3(0.55f, 1.35f, 0.35f),
            CameraPose(48f, 10f, 1.18f), readoutLabel = "REGEN COOLING",
        ),
        Hotspot(
            "injector", "Injector dome",
            "Fuel and oxidiser meet in a shower of fine jets. The pattern is tuned so propellant burns completely before it touches the chamber wall.",
            "rk_injector", V3(0f, 3.0f, 0.30f),
            CameraPose(40f, 26f, 1.2f), readoutLabel = "INJECTION",
        ),
        Hotspot(
            "chamber", "Thrust chamber",
            "A copper chamber lined with coolant passages. Pressure here, not the nozzle, makes the thrust; the bell only converts that pressure into speed.",
            "rk_chamber", V3(0.24f, 2.55f, 0.30f),
            CameraPose(34f, 20f, 1.2f), readoutLabel = "COMBUSTION",
        ),
        Hotspot(
            "pump", "Turbopump",
            "A small turbine drives the pump that feeds a much larger engine. At full throttle the pump moves hundreds of litres per second at more than 200 bar.",
            "rk_pump", V3(1.05f, 2.55f, 0.30f),
            CameraPose(58f, 20f, 1.16f), readoutLabel = "POWER",
        ),
        Hotspot(
            "gimbal", "Gimbal and actuators",
            "The whole engine hangs from a cross-shaped gimbal. Two actuators tilt the chamber a degree or two, and the thrust vector steers the vehicle.",
            "rk_gimbal", V3(0.1f, 3.24f, 0.26f),
            CameraPose(46f, 30f, 1.1f), readoutLabel = "THRUST VECTOR",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Propellant in",
            "Cryogenic oxidiser and fuel arrive through insulated lines. A small accumulator damps pressure oscillations before they can shake the whole stack.",
            ROCKET_FEED, CameraPose(56f, 16f, 1.02f), 0.35f, 0.7f,
        ),
        GuideStep(
            "Spin the pump",
            "A preburner makes hot gas that spins the turbine. The turbine turns the pump on the same shaft, and the pump pushes propellant far above tank pressure.",
            ROCKET_FEED, CameraPose(60f, 18f, 1.1f), 0.7f, 0.9f,
        ),
        GuideStep(
            "Inject",
            "Propellant enters the dome and leaves through hundreds of small orifices. The impinging jets atomise the liquids into a mist that burns in a controlled front.",
            ROCKET_POWER, CameraPose(40f, 26f, 1.18f), 0.25f, 0.8f,
        ),
        GuideStep(
            "Combust",
            "Pressure peaks in the chamber while the walls stay cool, protected by a film of fuel. Copper conducts heat away so fast that the liner survives 3,500 K gas.",
            ROCKET_POWER, CameraPose(34f, 18f, 1.16f), 0.1f, 1f,
        ),
        GuideStep(
            "Expand",
            "The throat chokes the flow; the bell expands it. Every metre of nozzle trades pressure for exhaust velocity, and velocity is what becomes thrust.",
            ROCKET_NOZZLE, CameraPose(58f, 4f, 1.08f), 0f, 1f,
        ),
        GuideStep(
            "Steer",
            "The gimbal tilts the whole engine. Explode the assembly to separate the stand, gimbal, pump, chamber, injector and bell.",
            null, CameraPose(44f, 26f, 0.98f), 1f, 0.8f,
        ),
    )

    return ObjectSpec(
        id = "rocket_engine",
        index = 6,
        title = "KESTREL R-9",
        designation = "R-9 / REGENERATIVE ROCKET",
        category = "Aerospace",
        categoryShort = "AERO",
        tagline = "Pressure, turned into speed.",
        summary = "A regeneratively cooled rocket engine on a static test stand: turbopump, preburner, injector dome, copper chamber, cooling channels and gimbal.",
        details = listOf(
            "KESTREL R-9 is a pump-fed liquid rocket engine. A preburner spins the turbopump, the pump pushes propellant into the injector, and combustion raises chamber pressure until the throat chokes the flow.",
            "The nozzle is regeneratively cooled: fuel travels through the wall channels before injection, so the same propellant that burns also keeps the copper from melting. The whole engine hangs on a gimbal and is steered by two actuators.",
        ),
        specs = listOf(
            SpecRow("Thrust", "845 kN"),
            SpecRow("Chamber", "210 bar"),
            SpecRow("Isp", "327 s"),
            SpecRow("Mixture", "2.36 O/F"),
            SpecRow("Dry mass", "1,150 kg"),
        ),
        tags = listOf("aerospace", "rocket", "engine", "turbopump", "nozzle", "propulsion", "space"),
        accent = 0xFFFF8A5C.toInt(),
        control = ControlSpec(
            label = "THROTTLE",
            lowLabel = "IDLE",
            highLabel = "FULL",
            defaultValue = 0.58f,
            hint = "Turbopump speed, chamber pressure and plume",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(40f, 18f, 1.02f),
        thumbnailPose = CameraPose(42f, 16f, 1.04f),
        thumbnailTime = 3.2f,
        thumbnailControl = 0.52f,
        readouts = { ctx ->
            val thrust = 55f + 790f * ctx.control
            val chamber = 32f + 178f * ctx.control
            val isp = 258f + 69f * ctx.control
            listOf(
                Readout("THRUST", String.format("%.0f", thrust), "kN", accent = true),
                Readout("CHAMBER", String.format("%.0f", chamber), "bar"),
                Readout("ISP", String.format("%.0f", isp), "s"),
            )
        },
        effects = { ctx ->
            val intensity = ctx.control
            listOf(
                SceneEffect.Plume(
                    origin = V3(0f, EXIT_Y - 0.02f, 0f),
                    direction = V3(0f, -1f, 0f),
                    length = 0.34f + intensity * 0.52f,
                    radius = 0.34f + intensity * 0.22f,
                    intensity = (0.25f + intensity * 0.85f).coerceIn(0f, 1f),
                    palette = PlumePalette.PLASMA,
                ),
                SceneEffect.Glow(V3(0f, EXIT_Y - 0.06f, 0f), 0.62f, 0xFFFFB25C.toInt(), 0.28f + 0.45f * intensity),
                SceneEffect.Glow(V3(0f, 2.58f, 0f), 0.34f, 0xFFFF7A4A.toInt(), 0.1f + 0.16f * intensity),
                SceneEffect.Glow(V3(0f, 0.40f, 0f), 0.9f, 0xFFFF8A5C.toInt(), 0.12f + 0.2f * intensity),
                SceneEffect.Spark(V3(0f, EXIT_Y - 0.3f, 0f), 0xFFFFD9A0.toInt(), 0.045f, 0.5f),
                SceneEffect.Flow(
                    points = listOf(
                        V3(0.55f, 1.15f, -0.62f),
                        V3(0.40f, 1.70f, -0.56f),
                        V3(0.30f, 2.20f, -0.40f),
                    ),
                    color = 0xFFFFB25C.toInt(),
                    count = 7,
                    speed = 0.42f,
                    phase = 0.1f,
                    dotRadius = 0.018f,
                ),
            )
        },
    )
}
