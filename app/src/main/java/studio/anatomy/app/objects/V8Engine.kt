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
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

private const val V8_BLOCK = "BLOCK"
private const val V8_CRANK = "CRANK"
private const val V8_PISTON = "PISTONS"
private const val V8_VALVE = "VALVETRAIN"
private const val V8_INTAKE = "INTAKE"
private const val V8_EXHAUST = "EXHAUST"

private const val V8_BASE = "CRANKCASE"
private const val V8_TRAIN = "ROTATING ASSEMBLY"
private const val V8_HEADS = "CYLINDER HEADS"
private const val V8_CAMS = "CAMSHAFTS"
private const val V8_MANIFOLD = "MANIFOLD"
private const val V8_HEADERS = "HEADERS"

private const val CRANK_Y = 1.18f
private val PIN_X = floatArrayOf(-1.02f, -0.34f, 0.34f, 1.02f)
private val PIN_PHASE = floatArrayOf(0f, 90f, 270f, 180f)
private const val BANK_TILT = 0.70710677f

private fun bankAxis(side: Int): V3 = V3(0f, BANK_TILT, BANK_TILT * side)

private fun alignY(direction: V3): Mat4 {
    val dir = direction.normalized()
    val dot = V3.Y.dot(dir).coerceIn(-1f, 1f)
    val axis = V3.Y.cross(dir)
    return when {
        axis.lengthSq() < 1e-7f -> if (dot > 0f) Mat4.identity() else Mat4.rotateX(180f)
        else -> Mat4.rotate(axis, Math.toDegrees(acos(dot.toDouble())).toFloat())
    }
}

private fun alongBank(center: V3, direction: V3, mesh: Mesh): Mesh =
    mesh.transformed(Mat4.translation(center).multiply(alignY(direction)))

private fun cylinderX(radius: Float, length: Float, segments: Int = 24, bevel: Float = 0f): Mesh =
    Geo.cylinder(radius, length, segments, bevel = bevel).transformed(Mat4.rotateZ(-90f))

private fun crankAngle(ctx: AnimContext): Float = ctx.machineTime * (120f + 280f * ctx.control)

private fun pistonOffset(ctx: AnimContext, pin: Int, side: Int): Float {
    val phase = Math.toRadians((PIN_PHASE[pin] + if (side > 0) 0f else 90f).toDouble())
    return 0.145f * sin(Math.toRadians(crankAngle(ctx).toDouble()) + phase).toFloat()
}

private fun pistonMesh(axis: V3): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.155f, 0.24f, 24, bevel = 0.018f), alignY(axis))
    addMesh(Geo.torus(0.148f, 0.012f, 24, 5), Mat4.translation(axis * 0.09f).multiply(alignY(axis)))
    addMesh(Geo.torus(0.148f, 0.01f, 24, 5), Mat4.translation(axis * 0.045f).multiply(alignY(axis)))
    addMesh(Geo.torus(0.142f, 0.01f, 24, 5), Mat4.translation(axis * 0.0f).multiply(alignY(axis)))
    addMesh(Geo.cylinder(0.055f, 0.30f, 12), Mat4.translation(-axis * 0.24f).multiply(alignY(axis)))
}.build()

private fun headMesh(axis: V3, x: Float, materialBand: Boolean): Mesh = MeshBuilder().apply {
    val center = V3(x, CRANK_Y, 0f) + axis * 1.0f
    addMesh(Geo.cylinder(0.235f, 0.20f, 24, bevel = 0.02f), Mat4.translation(center).multiply(alignY(axis)))
    if (materialBand) {
        for (i in 0 until 3) {
            addMesh(
                Geo.cylinder(0.268f, 0.018f, 24),
                Mat4.translation(center - axis * (0.03f + i * 0.045f)).multiply(alignY(axis)),
            )
        }
    }
    addMesh(
        Geo.cylinder(0.075f, 0.09f, 16),
        Mat4.translation(center + axis * 0.14f).multiply(alignY(axis)),
    )
    // Valve springs.
    listOf(-0.08f, 0.08f).forEach { offset ->
        addMesh(
            Geo.cylinder(0.026f, 0.075f, 10),
            Mat4.translation(center + axis * 0.13f + V3(0f, 0f, offset)).multiply(alignY(axis)),
        )
    }
}.build()

private fun camshaftMesh(): Mesh = MeshBuilder().apply {
    addMesh(cylinderX(0.052f, 2.90f, 16), Mat4.identity())
    PIN_X.forEachIndexed { index, x ->
        val phase = Math.toRadians((PIN_PHASE[index] * 0.5f).toDouble())
        val offset = V3(0f, (0.045f * cos(phase)).toFloat(), (0.045f * sin(phase)).toFloat())
        addMesh(
            Geo.cylinder(0.085f, 0.09f, 18, bevel = 0.008f),
            Mat4.translation(V3(x, 0f, 0f) + offset).multiply(Mat4.rotateZ(-90f)),
        )
    }
    addMesh(Geo.cylinder(0.11f, 0.07f, 18), Mat4.translation(V3(-1.49f, 0f, 0f)).multiply(Mat4.rotateZ(-90f)))
    addMesh(Geo.cylinder(0.11f, 0.07f, 18), Mat4.translation(V3(1.49f, 0f, 0f)).multiply(Mat4.rotateZ(-90f)))
}.build()

fun v8Engine(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "vector",
            width = 3.95f,
            depth = 2.85f,
            top = 0.3f,
            system = V8_BLOCK,
            assembly = V8_BASE,
            accentMaterial = Materials.emitRed,
        )
    )

    val crankcase = MeshBuilder().apply {
        addMesh(
            Geo.slab(3.10f, 1.20f, 0.56f, 0.18f, 0.05f, 3),
            Mat4.translation(V3(0f, 1.02f, 0f)),
        )
        addMesh(
            Geo.slab(2.42f, 0.92f, 0.30f, 0.12f, 0.04f, 2),
            Mat4.translation(V3(0f, 0.66f, 0f)),
        )
        // Valley cover between the two banks.
        addMesh(
            Geo.slab(2.78f, 0.46f, 0.18f, 0.10f, 0.03f, 2),
            Mat4.translation(V3(0f, 1.40f, 0f)),
        )
        addMesh(
            Geo.slab(0.14f, 0.30f, 0.16f, 0.03f, 0.01f),
            Mat4.translation(V3(0f, 1.52f, 0f)),
        )
        // Front cover and rear bearing plate.
        addMesh(
            Geo.cylinder(0.52f, 0.12f, 36, bevel = 0.02f).transformed(Mat4.rotateZ(-90f)),
            Mat4.translation(V3(-1.58f, CRANK_Y, 0f)),
        )
        addMesh(
            Geo.torus(0.50f, 0.04f, 32, 6).transformed(Mat4.rotateZ(-90f)),
            Mat4.translation(V3(-1.64f, CRANK_Y, 0f)),
        )
        addMesh(
            Geo.slab(2.9f, 0.08f, 0.06f, 0.02f, 0.006f),
            Mat4.translation(V3(0f, 1.28f, 0.61f)),
        )
    }.build()
    parts.add(
        Part(
            id = "v8_crankcase", name = "Crankcase and sump", system = V8_BLOCK, assembly = V8_BASE,
            mesh = crankcase, material = Materials.graphite,
            explode = V3(0f, -0.55f, 0f),
        )
    )

    val crankshaft = MeshBuilder().apply {
        addMesh(cylinderX(0.085f, 3.02f, 20), Mat4.identity())
        PIN_X.forEachIndexed { index, x ->
            val phase = Math.toRadians(PIN_PHASE[index].toDouble())
            val throwDir = V3(0f, cos(phase).toFloat(), sin(phase).toFloat())
            listOf(-0.11f, 0.11f).forEach { dx ->
                addMesh(
                    Geo.cylinder(0.30f, 0.055f, 28, bevel = 0.01f),
                    Mat4.translation(V3(x + dx, 0f, 0f) + throwDir * 0.10f).multiply(Mat4.rotateZ(-90f)),
                )
            }
            addMesh(
                Geo.cylinder(0.070f, 0.24f, 16),
                Mat4.translation(V3(x, 0f, 0f) + throwDir * 0.21f).multiply(Mat4.rotateZ(-90f)),
            )
        }
        // Front damper pulley and rear flywheel.
        addMesh(Geo.cylinder(0.20f, 0.10f, 24), Mat4.translation(V3(-1.53f, 0f, 0f)).multiply(Mat4.rotateZ(-90f)))
        addMesh(Geo.cylinder(0.44f, 0.09f, 40, bevel = 0.015f), Mat4.translation(V3(1.62f, 0f, 0f)).multiply(Mat4.rotateZ(-90f)))
        addMesh(
            Geo.gear(0.46f, 48, 0.07f, 0.018f, 3).transformed(Mat4.translation(V3(1.68f, 0f, 0f)).multiply(Mat4.rotateZ(90f))),
        )
    }.build()
    parts.add(
        Part(
            id = "v8_crank", name = "Cross-plane crankshaft", system = V8_CRANK, assembly = V8_TRAIN,
            mesh = crankshaft, material = Materials.brushedSteel,
            origin = V3(0f, CRANK_Y, 0f),
            animate = { ctx -> Mat4.rotateX(crankAngle(ctx)) },
            explode = V3(0f, -0.2f, 0f),
        )
    )

    // Glass cylinder barrels: the pistons stay visible while they stroke.
    val barrels = MeshBuilder().apply {
        for (side in listOf(-1, 1)) {
            val axis = bankAxis(side)
            PIN_X.forEach { x ->
                val center = V3(x, CRANK_Y, 0f) + axis * 0.72f
                addMesh(
                    Geo.cylinder(0.192f, 0.74f, 26, bevel = 0.01f),
                    Mat4.translation(center).multiply(alignY(axis)),
                )
                addMesh(
                    Geo.torus(0.196f, 0.014f, 26, 5),
                    Mat4.translation(center + axis * 0.34f).multiply(alignY(axis)),
                )
                addMesh(
                    Geo.torus(0.196f, 0.014f, 26, 5),
                    Mat4.translation(center - axis * 0.34f).multiply(alignY(axis)),
                )
            }
        }
    }.build()
    parts.add(
        Part(
            id = "v8_barrels", name = "Glass cylinder barrels", system = V8_PISTON, assembly = V8_TRAIN,
            mesh = barrels, material = Materials.glass, doubleSided = true,
            explode = V3(0f, 0.55f, 0f),
        )
    )

    for (side in listOf(-1, 1)) {
        val axis = bankAxis(side)
        PIN_X.forEachIndexed { index, x ->
            val origin = V3(x, CRANK_Y, 0f) + axis * 0.50f
            parts.add(
                Part(
                    id = "v8_piston_${side}_$index",
                    name = if (side > 0) "Right piston ${index + 1}" else "Left piston ${index + 1}",
                    system = V8_PISTON, assembly = V8_TRAIN,
                    mesh = pistonMesh(axis), material = Materials.titanium,
                    origin = origin,
                    animate = { ctx -> Mat4.translation(axis * pistonOffset(ctx, index, side)) },
                    explode = V3(0f, 0.65f, axis.z * 0.5f),
                )
            )
        }
        // Head and cam cover, one part per bank.
        val head = MeshBuilder().apply {
            PIN_X.forEachIndexed { index, x ->
                addMesh(headMesh(axis, x, index % 2 == 0))
            }
        }.build()
        parts.add(
            Part(
                id = "v8_head_$side", name = if (side > 0) "Right head" else "Left head",
                system = V8_BLOCK, assembly = V8_HEADS,
                mesh = head, material = Materials.paintCream,
                explode = V3(0f, 1.05f, axis.z * 0.85f),
            )
        )
        val cover = MeshBuilder().apply {
            addMesh(
                Geo.slab(2.62f, 0.18f, 0.26f, 0.06f, 0.02f, 2),
                Mat4.translation(V3(0f, CRANK_Y, 0f) + axis * 1.14f),
            )
            for (i in 0 until 8) {
                addMesh(
                    Geo.box(0.05f, 0.02f, 0.14f),
                    Mat4.translation(V3(-1.15f + i * 0.33f, CRANK_Y, 0f) + axis * 1.14f + V3(0f, 0.085f, 0f)),
                )
            }
        }.build()
        parts.add(
            Part(
                id = "v8_cover_$side", name = if (side > 0) "Right cam cover" else "Left cam cover",
                system = V8_VALVE, assembly = V8_CAMS,
                mesh = cover, material = Materials.paintOrange,
                explode = V3(0f, 1.35f, axis.z * 1.0f),
            )
        )
        val camOrigin = V3(0f, CRANK_Y, 0f) + axis * 1.30f
        parts.add(
            Part(
                id = "v8_cam_$side", name = if (side > 0) "Right camshaft" else "Left camshaft",
                system = V8_VALVE, assembly = V8_CAMS,
                mesh = camshaftMesh(), material = Materials.brass,
                origin = camOrigin,
                animate = { ctx -> Mat4.rotateX(crankAngle(ctx) * 0.5f) },
                explode = V3(0f, 1.5f, axis.z * 1.05f),
            )
        )
    }

    // Intake plenum and eight curved runners.
    val intake = MeshBuilder().apply {
        addMesh(
            Geo.slab(2.36f, 0.44f, 0.42f, 0.18f, 0.05f, 3),
            Mat4.translation(V3(0f, 2.46f, 0f)),
        )
        addMesh(
            Geo.slab(2.10f, 0.30f, 0.08f, 0.12f, 0.02f),
            Mat4.translation(V3(0f, 2.68f, 0f)),
        )
        addMesh(
            Geo.cylinder(0.19f, 0.34f, 28, bevel = 0.02f).transformed(Mat4.rotateZ(-90f)),
            Mat4.translation(V3(-1.30f, 2.46f, 0f)),
        )
        addMesh(Geo.torus(0.20f, 0.025f, 24, 5).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-1.10f, 2.46f, 0f)))
        for (side in listOf(-1, 1)) {
            PIN_X.forEach { x ->
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(x, 2.40f, side * 0.16f),
                            V3(x, 2.26f, side * 0.40f),
                            V3(x, 2.08f, side * 0.62f),
                            V3(x, 1.96f, side * 0.74f),
                        ),
                        0.055f, 7, subdivide = 3,
                    ),
                )
            }
        }
    }.build()
    parts.add(
        Part(
            id = "v8_intake", name = "Intake plenum and runners", system = V8_INTAKE, assembly = V8_MANIFOLD,
            mesh = intake, material = Materials.paintSlate,
            explode = V3(0f, 1.9f, 0f),
        )
    )
    parts.add(
        Part(
            id = "v8_throttle", name = "Throttle body", system = V8_INTAKE, assembly = V8_MANIFOLD,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.15f, 0.10f, 24), Mat4.translation(V3(-1.52f, 2.46f, 0f)).multiply(Mat4.rotateZ(-90f)))
                addMesh(Geo.box(0.03f, 0.22f, 0.22f), Mat4.translation(V3(-1.56f, 2.46f, 0f)))
            }.build(),
            material = Materials.titanium,
            explode = V3(0f, 1.9f, 0f),
        )
    )

    for (side in listOf(-1, 1)) {
        val headers = MeshBuilder().apply {
            PIN_X.forEach { x ->
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(x, 1.94f, side * 0.86f),
                            V3(x + 0.08f, 1.78f, side * 1.02f),
                            V3(x + 0.16f, 1.58f, side * 1.08f),
                            V3(x + 0.22f, 1.42f, side * 1.04f),
                            V3(1.26f, 1.34f, side * 1.02f),
                        ),
                        0.048f, 7, subdivide = 3,
                    ),
                )
            }
            addMesh(
                Geo.cylinder(0.13f, 0.52f, 22, bevel = 0.02f),
                Mat4.translation(V3(1.42f, 1.30f, side * 1.02f)).multiply(Mat4.rotateZ(-90f)),
            )
            addMesh(
                Geo.cylinder(0.10f, 0.36f, 18),
                Mat4.translation(V3(1.30f, 1.12f, side * 1.02f)),
            )
        }.build()
        parts.add(
            Part(
                id = "v8_headers_$side", name = if (side > 0) "Right headers" else "Left headers",
                system = V8_EXHAUST, assembly = V8_HEADERS,
                mesh = headers, material = Materials.copper,
                explode = V3(0.35f, 0.3f, side * 0.9f),
            )
        )
    }

    val coils = MeshBuilder().apply {
        for (side in listOf(-1, 1)) {
            val axis = bankAxis(side)
            PIN_X.forEach { x ->
                addMesh(
                    Geo.cylinder(0.045f, 0.14f, 14),
                    Mat4.translation(V3(x, CRANK_Y, 0f) + axis * 1.23f + V3(0f, 0.16f, 0f)).multiply(alignY(axis)),
                )
            }
        }
    }.build()
    parts.add(
        Part(
            id = "v8_coils", name = "Ignition coils", system = V8_VALVE, assembly = V8_CAMS,
            mesh = coils, material = Materials.emitBlue,
            explode = V3(0f, 1.7f, 0f),
        )
    )

    val belts = MeshBuilder().apply {
        addMesh(Geo.cylinder(0.19f, 0.035f, 28), Mat4.translation(V3(-1.60f, CRANK_Y, 0f)).multiply(Mat4.rotateZ(-90f)))
        addMesh(Geo.cylinder(0.13f, 0.03f, 24), Mat4.translation(V3(-1.60f, CRANK_Y + 0.55f, 0f)).multiply(Mat4.rotateZ(-90f)))
        addMesh(
            Geo.tube(
                listOf(
                    V3(-1.60f, CRANK_Y + 0.19f, 0.02f),
                    V3(-1.60f, CRANK_Y + 0.45f, 0.02f),
                    V3(-1.60f, CRANK_Y + 0.55f, 0.02f),
                    V3(-1.60f, CRANK_Y + 0.35f, -0.02f),
                    V3(-1.60f, CRANK_Y - 0.18f, -0.02f),
                ),
                0.022f, 5, subdivide = 2,
            ),
        )
    }.build()
    parts.add(
        Part(
            id = "v8_belt", name = "Accessory belt", system = V8_CRANK, assembly = V8_TRAIN,
            mesh = belts, material = Materials.rubber,
            explode = V3(-0.5f, 0.2f, 0f),
        )
    )

    val hotspots = listOf(
        Hotspot(
            "crank", "Cross-plane crank",
            "Four throws at 90 degrees give the V8 its uneven, burbling beat. Each throw carries two connecting rods, one from each bank.",
            "v8_crank", V3(0.15f, 0.03f, 0.22f),
            CameraPose(66f, 12f, 1.14f), readoutLabel = "CRANKSHAFT",
        ),
        Hotspot(
            "piston", "Forged piston",
            "The piston converts pressure into a straight push. Three rings seal combustion gas while the skirt keeps the piston square in the bore.",
            "v8_piston_1_1", V3(0f, 0.04f, 0f),
            CameraPose(52f, 22f, 1.2f), readoutLabel = "PISTON",
        ),
        Hotspot(
            "barrel", "Glass cylinder",
            "These display barrels replace the iron liners so the whole stroke stays visible. In the real engine, coolant jackets surround each bore.",
            "v8_barrels", V3(0.34f, 1.85f, 0.55f),
            CameraPose(44f, 16f, 1.16f), readoutLabel = "COMBUSTION",
        ),
        Hotspot(
            "cam", "Camshaft",
            "The cam turns at half crankshaft speed. Each lobe pushes a valve open at exactly the right moment, then lets the spring snap it shut.",
            "v8_cam_1", V3(0f, 0f, 0f),
            CameraPose(30f, 26f, 1.22f), readoutLabel = "VALVETRAIN",
        ),
        Hotspot(
            "manifold", "Intake plenum",
            "The plenum is a pressure reservoir. Air slows down inside it, so all eight runners draw from the same calm charge instead of fighting each other.",
            "v8_intake", V3(-0.9f, 2.46f, 0.2f),
            CameraPose(52f, 28f, 1.1f), readoutLabel = "INTAKE",
        ),
        Hotspot(
            "headers", "Tuned headers",
            "Each primary pipe is a different length on purpose. The pulses arrive at the collector in sequence, helping the next cylinder empty itself.",
            "v8_headers_1", V3(1.36f, 1.34f, 1.02f),
            CameraPose(58f, 14f, 1.12f), readoutLabel = "EXHAUST",
        ),
    )

    val steps = listOf(
        GuideStep(
            "The rotating core",
            "The crank turns eight pistons into one smooth output. Firing order and throw angles spread the power pulses evenly around the circle.",
            V8_CRANK, CameraPose(62f, 14f, 1.06f), 0.45f, 0.75f,
        ),
        GuideStep(
            "Four strokes",
            "Intake, compression, combustion, exhaust. The piston is the only part that touches all four, which is why it carries the rings and the skirt.",
            V8_PISTON, CameraPose(50f, 20f, 1.1f), 0.35f, 0.95f,
        ),
        GuideStep(
            "Breathing",
            "Air enters the throttle, fills the plenum and runs down eight equal-length-ish runners. The valve opens just as the piston starts down.",
            V8_INTAKE, CameraPose(50f, 26f, 1.12f), 0.15f, 0.8f,
        ),
        GuideStep(
            "Valves and cams",
            "Two camshafts, sixteen valves. The cam profile decides how fast the valve opens and how long it stays open at high rpm.",
            V8_VALVE, CameraPose(30f, 24f, 1.16f), 0.1f, 0.9f,
        ),
        GuideStep(
            "Out with the gas",
            "The headers merge four pulses into one collector. The orange heat is real: exhaust gas leaves hotter than 800 °C.",
            V8_EXHAUST, CameraPose(56f, 12f, 1.12f), 0f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the engine to lift the heads, barrels, pistons, cams and manifolds away from the crankcase. Every layer is a service assembly.",
            null, CameraPose(42f, 26f, 0.98f), 1f, 0.78f,
        ),
    )

    return ObjectSpec(
        id = "v8_engine",
        index = 7,
        title = "VECTOR V8",
        designation = "V8-44 / CROSS-PLANE V8",
        category = "Automotive",
        categoryShort = "DRIVE",
        tagline = "Eight pistons, one uneven heartbeat.",
        summary = "A 4.4-litre cross-plane V8 cut open with glass cylinder barrels: crank, pistons, camshafts, intake plenum and tuned headers all visible and moving.",
        details = listOf(
            "VECTOR V8 is a 90-degree V8 displayed with transparent barrels. The cross-plane crank gives it the characteristic burble, and the two banks share a single intake plenum between the heads.",
            "Each piston runs a full stroke inside its glass bore while the cams turn at half speed above. The headers are tuned so each exhaust pulse helps pull the next one out of the cylinder.",
        ),
        specs = listOf(
            SpecRow("Displacement", "4.4 L"),
            SpecRow("Layout", "90° V8"),
            SpecRow("Power", "415 hp"),
            SpecRow("Torque", "540 Nm"),
            SpecRow("Redline", "7,000 rpm"),
        ),
        tags = listOf("automotive", "v8", "engine", "pistons", "camshaft", "combustion", "crank"),
        accent = 0xFFFF8A5C.toInt(),
        control = ControlSpec(
            label = "THROTTLE",
            lowLabel = "IDLE",
            highLabel = "REDLINE",
            defaultValue = 0.58f,
            hint = "Crank speed, piston stroke and power",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(44f, 22f, 1.16f),
        thumbnailPose = CameraPose(46f, 19f, 1.18f),
        thumbnailTime = 3.4f,
        thumbnailControl = 0.5f,
        readouts = { ctx ->
            val rpm = 850f + 6150f * ctx.control
            val torque = 470f - 60f * ctx.control
            val power = torque * rpm / 7127f
            listOf(
                Readout("RPM", String.format("%.0f", rpm), accent = true),
                Readout("POWER", String.format("%.0f", power), "hp"),
                Readout("TORQUE", String.format("%.0f", torque), "Nm"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Glow(V3(1.40f, 1.28f, 1.02f), 0.42f, 0xFFFF8A5C.toInt(), 0.18f + 0.22f * ctx.control),
                SceneEffect.Glow(V3(1.40f, 1.28f, -1.02f), 0.42f, 0xFFFF8A5C.toInt(), 0.18f + 0.22f * ctx.control),
                SceneEffect.Flow(
                    points = listOf(
                        V3(-2.05f, 2.46f, 0f),
                        V3(-1.55f, 2.46f, 0f),
                        V3(-1.15f, 2.46f, 0f),
                    ),
                    color = 0xFFB8F6EC.toInt(),
                    count = 7,
                    speed = 0.4f,
                    phase = 0.2f,
                    dotRadius = 0.022f,
                ),
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.78f, 0xFFFF8A5C.toInt(), 0f, 0.24f),
            )
        },
    )
}
