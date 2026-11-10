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
import studio.anatomy.app.render.Material
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder
import studio.anatomy.app.render.SurfacePattern
import kotlin.math.cos
import kotlin.math.sin

private const val FR_FRAME = "FRAME"
private const val FR_CORE = "PLASMA CORE"
private const val FR_MAGNET = "MAGNETS"
private const val FR_FUEL = "FUEL & HEAT"
private const val FR_POWER = "POWER BLOCK"
private const val FR_GRID = "GRID"

private const val FR_PLINTH = "PLINTH"
private const val FR_TOKAMAK = "TOKAMAK"
private const val FR_COIL_SET = "COIL SET"
private const val FR_INJECTOR = "INJECTORS"
private const val FR_PLANT = "POWER BLOCK"
private const val FR_YARD = "SWITCHYARD"

private val frPlasma = Material(
    "fusionPlasma", "Fusion plasma", 0xFFA48BFF.toInt(), emissive = true, specular = 0f,
)
private val frHotPlasma = Material(
    "fusionHotPlasma", "Hot plasma filament", 0xFFE7D8FF.toInt(), emissive = true, specular = 0f,
)
private val frGold = Material(
    "fusionGold", "Breeding blanket", 0xFFD9AD63.toInt(), specular = 0.58f, shininess = 34f,
    fillLight = 0.10f, rimLight = 0.17f, patina = 0.070f, edgeWear = 0.06f,
    pattern = SurfacePattern.BRUSHED, patternStrength = 0.30f,
)
private val frCopper = Material(
    "fusionCopper", "Copper bus", 0xFFB86945.toInt(), specular = 0.62f, shininess = 32f,
    fillLight = 0.10f, rimLight = 0.17f, patina = 0.080f, edgeWear = 0.06f,
    pattern = SurfacePattern.PATINA, patternStrength = 0.28f,
)
private val frCryo = Material(
    "fusionCryo", "Cryogenic shell", 0xFF7EDBE6.toInt(), specular = 0.52f, shininess = 42f,
    alpha = 54,
)
private val frCeramic = Material(
    "fusionCeramic", "Ceramic insulator", 0xFFD8D3C2.toInt(), specular = 0.22f, shininess = 16f,
    fillLight = 0.08f, rimLight = 0.14f, patina = 0.025f,
    pattern = SurfacePattern.SPECKLE, patternStrength = 0.20f,
)

private fun circlePoint(radius: Float, angle: Float, y: Float = 0f): V3 =
    V3((cos(Math.toRadians(angle.toDouble())) * radius).toFloat(), y, (sin(Math.toRadians(angle.toDouble())) * radius).toFloat())

private fun frCoilMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 12) {
        val angle = i * 30f
        val pose = Mat4.rotateY(angle).multiply(Mat4.translation(V3(0f, 0f, 1.02f)))
        addMesh(
            Geo.slab(0.245f, 0.105f, 0.84f, cornerRadius = 0.075f, bevel = 0.012f, segments = 3),
            pose,
        )
        // The inner copper winding makes each D-coil read as a laminated magnet.
        addMesh(
            Geo.slab(0.155f, 0.024f, 0.67f, cornerRadius = 0.055f, bevel = 0.004f, segments = 2),
            pose.multiply(Mat4.translation(V3(0f, 0f, 0.064f))),
        )
        addMesh(
            Geo.box(0.10f, 0.035f, 0.11f),
            pose.multiply(Mat4.translation(V3(0f, 0.43f, 0f))),
        )
        addMesh(
            Geo.box(0.10f, 0.035f, 0.11f),
            pose.multiply(Mat4.translation(V3(0f, -0.43f, 0f))),
        )
    }
}.build()

private fun frCryostatMesh(): Mesh = MeshBuilder().apply {
    val radius = 1.14f
    for (i in 0 until 12) {
        val angle = i * 30f
        val point = circlePoint(radius, angle)
        addMesh(
            Geo.cylinderBetween(
                V3(point.x, -0.54f, point.z),
                V3(point.x, 0.54f, point.z),
                0.022f, 7,
            ),
        )
        addMesh(Geo.hexBolt(0.048f, 0.035f), Mat4.translation(V3(point.x, 0.57f, point.z)))
    }
    addMesh(Geo.torus(radius, 0.032f, 48, 5), Mat4.translation(V3(0f, -0.55f, 0f)))
    addMesh(Geo.torus(radius, 0.032f, 48, 5), Mat4.translation(V3(0f, 0.55f, 0f)))
    addMesh(Geo.torus(radius * 0.86f, 0.020f, 42, 4), Mat4.translation(V3(0f, 0.19f, 0f)))
    addMesh(Geo.torus(radius * 0.86f, 0.020f, 42, 4), Mat4.translation(V3(0f, -0.19f, 0f)))
}.build()

private fun frVesselBands(): Mesh = MeshBuilder().apply {
    for (y in listOf(-0.26f, 0f, 0.26f)) {
        addMesh(
            Geo.torus(0.95f, 0.022f, 44, 4),
            Mat4.translation(V3(0f, y, 0f)),
        )
    }
    // Radial port stubs around the torus.
    for (i in 0 until 8) {
        val angle = 22.5f + i * 45f
        val point = circlePoint(0.98f, angle)
        addMesh(
            Geo.cylinderBetween(
                V3(point.x, 0f, point.z),
                V3(point.x * 1.15f, 0f, point.z * 1.15f),
                0.040f, 8,
            ),
        )
        addMesh(
            Geo.torus(0.045f, 0.009f, 12, 4).transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(point.x * 1.15f, 0f, point.z * 1.15f)),
        )
    }
}.build()

private fun frBlanketMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 12) {
        val angle = i * 30f
        val pose = Mat4.rotateY(angle)
            .multiply(Mat4.translation(V3(0f, 0f, 0.86f)))
            .multiply(Mat4.rotateZ(14f))
        addMesh(
            Geo.slab(0.20f, 0.34f, 0.045f, cornerRadius = 0.035f, bevel = 0.006f, segments = 2),
            pose,
        )
        addMesh(
            Geo.box(0.30f, 0.012f, 0.020f),
            pose.multiply(Mat4.translation(V3(0f, 0.11f, 0.026f))),
        )
    }
}.build()

private fun frSolenoidMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.205f, 1.22f, 24, bevel = 0.025f))
    for (i in -4..4) {
        addMesh(
            Geo.torus(0.215f, 0.018f, 24, 4),
            Mat4.translation(V3(0f, i * 0.115f, 0f)),
        )
    }
    addMesh(Geo.cylinder(0.27f, 0.08f, 28, bevel = 0.015f), Mat4.translation(V3(0f, -0.65f, 0f)))
    addMesh(Geo.cylinder(0.27f, 0.08f, 28, bevel = 0.015f), Mat4.translation(V3(0f, 0.65f, 0f)))
}.build()

private fun frDivertorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.torus(0.69f, 0.070f, 44, 6), Mat4.translation(V3(0f, -0.52f, 0f)))
    addMesh(Geo.torus(0.66f, 0.024f, 40, 4), Mat4.translation(V3(0f, -0.43f, 0f)))
    for (i in 0 until 12) {
        val angle = i * 30f
        val pose = Mat4.rotateY(angle)
            .multiply(Mat4.translation(V3(0f, -0.49f, 0.69f)))
            .multiply(Mat4.rotateX(18f))
        addMesh(Geo.slab(0.18f, 0.15f, 0.025f, cornerRadius = 0.018f, bevel = 0.004f, segments = 2), pose)
    }
}.build()

private fun frInjectorMesh(): Mesh = MeshBuilder().apply {
    listOf(18f, 138f, 258f).forEach { angle ->
        val start = circlePoint(1.30f, angle, 0.10f)
        val end = circlePoint(0.91f, angle, 0.10f)
        addMesh(Geo.cylinderBetween(start, end, 0.105f, 12))
        addMesh(
            Geo.cylinder(0.145f, 0.17f, 12, bevel = 0.018f)
                .transformed(Mat4.translation(start.x, 0.10f, start.z).multiply(Mat4.rotateZ(90f))),
        )
        addMesh(
            Geo.torus(0.145f, 0.018f, 16, 4).transformed(Mat4.rotateX(90f)),
            Mat4.translation(start.x, 0.10f, start.z),
        )
        addMesh(Geo.box(0.20f, 0.24f, 0.18f), Mat4.translation(start.x * 1.06f, -0.02f, start.z * 1.06f))
    }
}.build()

private fun frCoolingTowerMesh(): Mesh = MeshBuilder().apply {
    addMesh(
        Geo.lathe(
            listOf(
                0.36f to -0.38f,
                0.28f to -0.18f,
                0.22f to 0.04f,
                0.25f to 0.26f,
                0.31f to 0.38f,
            ),
            28, capBottom = true, capTop = false,
        ),
    )
    addMesh(Geo.torus(0.36f, 0.018f, 28, 4), Mat4.translation(V3(0f, -0.38f, 0f)))
    addMesh(Geo.torus(0.31f, 0.018f, 28, 4), Mat4.translation(V3(0f, 0.38f, 0f)))
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        addMesh(
            Geo.box(0.018f, 0.028f, 0.18f),
            Mat4.rotateY(i * 45f).multiply(Mat4.translation(V3(0f, 0.23f, 0.22f))),
        )
    }
}.build()

private fun frTurbineMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.19f, 0.58f, 22, bevel = 0.030f).transformed(Mat4.rotateZ(-90f)))
    addMesh(Geo.torus(0.195f, 0.014f, 22, 4).transformed(Mat4.rotateZ(-90f)))
    for (i in 0 until 8) {
        addMesh(
            Geo.box(0.026f, 0.17f, 0.07f),
            Mat4.rotateX(i * 45f).multiply(Mat4.translation(V3(0f, 0.13f, 0f))),
        )
    }
    addMesh(Geo.cylinder(0.075f, 0.32f, 16, bevel = 0.012f).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0.44f, 0f, 0f)))
    addMesh(Geo.torus(0.080f, 0.012f, 16, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0.58f, 0f, 0f)))
}.build()

private fun frCryoFanMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.090f, 0.028f, 18, bevel = 0.006f).transformed(Mat4.rotateZ(-90f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.020f, 0.105f, 0.032f),
            Mat4.rotateX(i * 72f)
                .multiply(Mat4.translation(V3(0f, 0.070f, 0f)))
                .multiply(Mat4.rotateX(22f)),
        )
    }
}.build()

private fun frSwitchyardMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 3) {
        val x = i * 0.16f
        addMesh(Geo.cylinder(0.026f, 0.34f, 8, bevel = 0.006f), Mat4.translation(V3(x, 0.17f, 0f)))
        for (j in 0 until 4) {
            addMesh(
                Geo.torus(0.042f, 0.008f, 10, 4),
                Mat4.translation(V3(x, 0.08f + j * 0.07f, 0f)),
            )
        }
    }
    addMesh(Geo.box(0.40f, 0.14f, 0.26f), Mat4.translation(V3(0.16f, -0.02f, 0f)))
}.build()

private fun frPylonMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.20f, 0.06f, 0.20f), Mat4.translation(V3(0f, 0.03f, 0f)))
    listOf(-1f, 1f).forEach { side ->
        addMesh(
            Geo.cylinderBetween(V3(side * 0.10f, 0.05f, 0f), V3(side * 0.18f, 0.82f, 0f), 0.024f, 6),
        )
        addMesh(
            Geo.cylinderBetween(V3(side * 0.18f, 0.82f, 0f), V3(side * 0.28f, 1.38f, 0f), 0.018f, 6),
        )
    }
    for (y in listOf(0.38f, 0.72f, 1.04f, 1.28f)) {
        addMesh(Geo.box(0.52f, 0.018f, 0.018f), Mat4.translation(V3(0f, y, 0f)))
        addMesh(Geo.box(0.34f, 0.012f, 0.012f), Mat4.translation(V3(0f, y + 0.10f, 0f)))
    }
    for (i in 0 until 4) {
        addMesh(
            Geo.cylinderBetween(V3(-0.26f + i * 0.17f, 1.37f, 0f), V3(-0.26f + i * 0.17f, 1.47f, 0f), 0.012f, 5),
        )
    }
}.build()

fun fusionRingSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "fusion",
            width = 3.48f,
            depth = 2.28f,
            top = 0.30f,
            system = FR_FRAME,
            assembly = FR_PLINTH,
            accentMaterial = Materials.emitBlue,
        ),
    )

    // Tokamak core is centred on the deck; the cage keeps the plasma visible from every angle.
    parts.add(
        Part(
            id = "fr_cryostat", name = "Cryostat support cage", system = FR_FRAME, assembly = FR_TOKAMAK,
            mesh = frCryostatMesh(), material = Materials.darkSteel,
            origin = V3(0f, 1.58f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "fr_tf_coils", name = "Toroidal field coils", system = FR_MAGNET, assembly = FR_COIL_SET,
            mesh = frCoilMesh(), material = Materials.nickel,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx ->
                Mat4.translation(V3(0f, 0.018f * sin(ctx.machineTime * 1.8f), 0f))
                    .multiply(Mat4.rotateY(ctx.machineTime * 24f))
            },
        ),
    )
    parts.add(
        Part(
            id = "fr_pf_coils", name = "Poloidal field rings", system = FR_MAGNET, assembly = FR_COIL_SET,
            mesh = MeshBuilder().apply {
                for (i in 0 until 4) {
                    addMesh(
                        Geo.torus(0.94f + i * 0.055f, 0.018f, 48, 4),
                        Mat4.translation(V3(0f, -0.29f + i * 0.19f, 0f)),
                    )
                }
            }.build(),
            material = frCopper,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx -> Mat4.rotateX(4.5f * sin(ctx.machineTime * 0.9f)) },
        ),
    )
    parts.add(
        Part(
            id = "fr_vessel_bands", name = "Vacuum vessel", system = FR_CORE, assembly = FR_TOKAMAK,
            mesh = frVesselBands(), material = Materials.darkSteel,
            origin = V3(0f, 1.58f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "fr_blanket", name = "Breeding blanket modules", system = FR_CORE, assembly = FR_TOKAMAK,
            mesh = frBlanketMesh(), material = frGold,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx -> Mat4.rotateY(-ctx.machineTime * 32f) },
        ),
    )
    parts.add(
        Part(
            id = "fr_divertor", name = "Divertor and exhaust", system = FR_CORE, assembly = FR_TOKAMAK,
            mesh = frDivertorMesh(), material = Materials.graphite,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 16f) },
        ),
    )
    parts.add(
        Part(
            id = "fr_plasma", name = "Fusion plasma torus", system = FR_CORE, assembly = FR_TOKAMAK,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.72f, 0.155f, 48, 10))
                addMesh(Geo.torus(0.72f, 0.063f, 48, 8), Mat4.translation(V3(0f, 0f, 0f)))
                for (i in 0 until 12) {
                    val angle = i * 30f
                    val p = circlePoint(0.72f, angle)
                    addMesh(
                        Geo.sphere(0.035f, 10, 6),
                        Mat4.translation(V3(p.x, 0f, p.z)),
                    )
                }
            }.build(),
            material = frPlasma,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx ->
                val pulse = 1f + 0.055f * sin(ctx.machineTime * 4.8f)
                Mat4.rotateY(ctx.machineTime * 38f).multiply(Mat4.scale(V3(pulse, pulse, pulse)))
            },
        ),
    )
    parts.add(
        Part(
            id = "fr_plasma_filaments", name = "Helical plasma filaments", system = FR_CORE, assembly = FR_TOKAMAK,
            mesh = MeshBuilder().apply {
                for (i in 0 until 3) {
                    addMesh(
                        Geo.torus(0.72f + i * 0.012f, 0.018f, 36, 5),
                        Mat4.rotateY(i * 17f),
                    )
                }
                addMesh(Geo.helix(0.73f, 0.012f, 0.14f, 1, 26), Mat4.translation(V3(0f, -0.02f, 0f)))
            }.build(),
            material = frHotPlasma,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx -> Mat4.rotateY(-ctx.machineTime * 58f) },
        ),
    )
    parts.add(
        Part(
            id = "fr_solenoid", name = "Central solenoid", system = FR_MAGNET, assembly = FR_COIL_SET,
            mesh = frSolenoidMesh(), material = frGold,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx ->
                Mat4.translation(V3(0f, 0.040f * sin(ctx.machineTime * 2.3f), 0f))
                    .multiply(Mat4.rotateY(ctx.machineTime * 30f))
            },
        ),
    )
    parts.add(
        Part(
            id = "fr_injectors", name = "Neutral beam injectors", system = FR_FUEL, assembly = FR_INJECTOR,
            mesh = frInjectorMesh(), material = Materials.titanium,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx -> Mat4.rotateY(24f * sin(ctx.machineTime * 0.62f)) },
        ),
    )
    parts.add(
        Part(
            id = "fr_injector_glow", name = "Injector apertures", system = FR_FUEL, assembly = FR_INJECTOR,
            mesh = MeshBuilder().apply {
                listOf(18f, 138f, 258f).forEach { angle ->
                    val p = circlePoint(0.91f, angle, 0.10f)
                    addMesh(Geo.cylinder(0.075f, 0.02f, 16).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(p.x, 0.10f, p.z)))
                }
            }.build(),
            material = Materials.emitCyan,
            origin = V3(0f, 1.58f, 0f),
            animate = { ctx ->
                val wave = 1f + 0.08f * sin(ctx.machineTime * 4.2f)
                Mat4.rotateY(24f * sin(ctx.machineTime * 0.62f))
                    .multiply(Mat4.scale(V3(wave, wave, wave)))
            },
        ),
    )

    // Cryogenic plant and heat rejection sit behind the machine.
    parts.add(
        Part(
            id = "fr_cryo_plant", name = "Helium cryoplant", system = FR_FUEL, assembly = FR_PLANT,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.52f, 0.38f, 0.32f, cornerRadius = 0.035f, bevel = 0.012f), Mat4.translation(V3(-1.25f, 0.47f, -0.48f)))
                addMesh(Geo.box(0.34f, 0.035f, 0.19f), Mat4.translation(V3(-1.25f, 0.65f, -0.48f)))
                for (i in 0 until 3) {
                    addMesh(
                        Geo.cylinder(0.10f, 0.16f, 12, bevel = 0.014f).transformed(Mat4.rotateZ(-90f)),
                        Mat4.translation(V3(-1.25f - 0.12f + i * 0.16f, 0.38f, -0.64f)),
                    )
                }
                addMesh(Geo.torus(0.11f, 0.012f, 16, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(-1.35f, 0.38f, -0.64f)))
            }.build(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "fr_cryo_fan", name = "Cryoplant circulation fan", system = FR_FUEL, assembly = FR_PLANT,
            mesh = frCryoFanMesh(), material = Materials.brushedSteel,
            origin = V3(-1.37f, 0.39f, -0.65f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 260f) },
        ),
    )
    parts.add(
        Part(
            id = "fr_cryo_lines", name = "Cryogenic transfer lines", system = FR_FUEL, assembly = FR_PLANT,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-1.17f, 0.54f, -0.40f),
                            V3(-0.95f, 0.92f, -0.26f),
                            V3(-0.82f, 1.30f, -0.22f),
                            V3(-0.70f, 1.57f, -0.18f),
                        ),
                        0.032f, 8, subdivide = 3,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-1.17f, 0.54f, -0.56f),
                            V3(-0.94f, 0.55f, -0.57f),
                            V3(-0.60f, 0.53f, -0.56f),
                        ),
                        0.024f, 7, subdivide = 3,
                    ),
                )
            }.build(),
            material = frCryo,
        ),
    )

    // Steam island on the right, with a cooling tower and a visible rotation in the turbo-generator.
    parts.add(
        Part(
            id = "fr_heat_exchanger", name = "Primary heat exchanger", system = FR_POWER, assembly = FR_PLANT,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.46f, 0.44f, 0.40f, cornerRadius = 0.04f, bevel = 0.012f), Mat4.translation(V3(0.92f, 0.50f, 0.55f)))
                addMesh(Geo.torus(0.16f, 0.025f, 20, 4).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0.92f, 0.54f, 0.78f)))
                addMesh(Geo.cylinder(0.055f, 0.42f, 12), Mat4.translation(V3(1.08f, 0.84f, 0.55f)))
            }.build(),
            material = Materials.brushedSteel,
        ),
    )
    parts.add(
        Part(
            id = "fr_turbine", name = "Steam turbine rotor", system = FR_POWER, assembly = FR_PLANT,
            mesh = frTurbineMesh(), material = Materials.nickel,
            origin = V3(1.33f, 0.67f, 0.55f),
            animate = { ctx -> Mat4.rotateX(ctx.machineTime * 260f) },
        ),
    )
    parts.add(
        Part(
            id = "fr_generator", name = "Generator and condenser", system = FR_POWER, assembly = FR_PLANT,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.16f, 0.34f, 20, bevel = 0.02f).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(1.78f, 0.67f, 0.55f)))
                addMesh(Geo.torus(0.17f, 0.012f, 20, 4).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(1.94f, 0.67f, 0.55f)))
                addMesh(Geo.box(0.30f, 0.16f, 0.24f), Mat4.translation(V3(1.72f, 0.42f, 0.55f)))
            }.build(),
            material = frCopper,
        ),
    )
    parts.add(
        Part(
            id = "fr_cooling_tower", name = "Cooling tower", system = FR_POWER, assembly = FR_PLANT,
            mesh = frCoolingTowerMesh(), material = Materials.paintCream,
            origin = V3(1.27f, 0.70f, -0.56f),
        ),
    )
    parts.add(
        Part(
            id = "fr_steam_lines", name = "Steam and feedwater loop", system = FR_POWER, assembly = FR_PLANT,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.76f, 0.68f, 0.55f),
                            V3(1.02f, 0.92f, 0.55f),
                            V3(1.33f, 0.69f, 0.55f),
                            V3(1.55f, 0.69f, 0.55f),
                        ),
                        0.035f, 8, subdivide = 3,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(1.27f, 0.76f, -0.34f),
                            V3(1.33f, 0.78f, 0.15f),
                            V3(1.33f, 0.72f, 0.46f),
                        ),
                        0.026f, 7, subdivide = 3,
                    ),
                )
            }.build(),
            material = frCopper,
        ),
    )

    parts.add(
        Part(
            id = "fr_switchyard", name = "High-voltage switchyard", system = FR_GRID, assembly = FR_YARD,
            mesh = frSwitchyardMesh(), material = frCeramic,
            origin = V3(-1.31f, 0.49f, 0.59f),
        ),
    )
    parts.add(
        Part(
            id = "fr_pylon", name = "Transmission pylon", system = FR_GRID, assembly = FR_YARD,
            mesh = frPylonMesh(), material = Materials.darkSteel,
            origin = V3(1.35f, 0.30f, -0.84f),
        ),
    )
    parts.add(
        Part(
            id = "fr_bus", name = "Battery and bus link", system = FR_GRID, assembly = FR_YARD,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.36f, 0.28f, 0.22f), Mat4.translation(V3(-1.02f, 0.45f, 0.59f)))
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-0.82f, 0.48f, 0.59f),
                            V3(-0.20f, 0.42f, 0.68f),
                            V3(0.52f, 0.46f, 0.66f),
                            V3(0.92f, 0.53f, 0.59f),
                        ),
                        0.020f, 6, subdivide = 4,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(1.47f, 0.49f, 0.57f),
                            V3(1.38f, 0.78f, 0.24f),
                            V3(1.35f, 1.06f, -0.18f),
                            V3(1.35f, 1.30f, -0.52f),
                        ),
                        0.018f, 6, subdivide = 4,
                    ),
                )
            }.build(),
            material = Materials.emitBlue,
        ),
    )
    parts.add(
        Part(
            id = "fr_lights", name = "Diagnostic beacons", system = FR_GRID, assembly = FR_PLINTH,
            mesh = MeshBuilder().apply {
                listOf(
                    V3(-1.47f, 0.36f, 0.93f),
                    V3(1.47f, 0.36f, 0.93f),
                    V3(0f, 0.36f, 1.02f),
                ).forEach { point ->
                    addMesh(Geo.cylinder(0.030f, 0.018f, 12), Mat4.translation(point))
                }
            }.build(),
            material = Materials.emitCyan,
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "plasma", "Burning plasma",
            "The plasma is a charged gas hotter than the core of the Sun. Magnetic fields keep it away from the wall so it can fuse hydrogen isotopes instead of melting the vessel.",
            "fr_plasma", V3(0f, 0.02f, 0.23f),
            CameraPose(28f, 14f, 1f), readoutLabel = "150 MILLION °C",
        ),
        Hotspot(
            "coils", "Toroidal field coils",
            "Twelve D-shaped coils wrap the torus with a strong magnetic field. The field twists the plasma into helical paths, preventing it from touching the first wall.",
            "fr_tf_coils", V3(0f, 0.46f, 1.02f),
            CameraPose(46f, 18f, 1f), readoutLabel = "SUPERCONDUCTING",
        ),
        Hotspot(
            "blanket", "Breeding blanket",
            "The blanket catches the fusion neutrons and turns their energy into heat. Lithium-bearing modules also breed tritium, the fuel that the plasma needs next.",
            "fr_blanket", V3(0.18f, 0.36f, 0.80f),
            CameraPose(24f, 20f, 1f), readoutLabel = "TRITIUM BREEDING",
        ),
        Hotspot(
            "divertor", "Divertor exhaust",
            "The divertor is the exhaust pipe of the tokamak. It guides heat and helium ash out of the plasma before they cool the burn.",
            "fr_divertor", V3(0f, -0.48f, 0.70f),
            CameraPose(32f, 10f, 1f), readoutLabel = "HEAT EXHAUST",
        ),
        Hotspot(
            "injectors", "Neutral beam injectors",
            "Neutral beams fire high-energy atoms into the plasma. They heat it and drive current, helping the magnetic cage sustain the burn.",
            "fr_injectors", V3(1.30f, 0.10f, 0.60f),
            CameraPose(52f, 18f, 1f), readoutLabel = "PLASMA HEATING",
        ),
        Hotspot(
            "turbine", "Steam turbo-generator",
            "Heat from the blanket makes steam, and the steam spins a turbine coupled to a generator. That is how a fusion reactor would send electricity to the grid.",
            "fr_turbine", V3(0f, 0.0f, 0.24f),
            CameraPose(38f, 16f, 1f), readoutLabel = "GROSS ELECTRIC",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Build the cage",
            "The superconducting coils are cooled to near absolute zero. Once current flows, they create the magnetic bottle that holds the plasma.",
            FR_MAGNET, CameraPose(36f, 18f, 1.04f), 0.16f, 0.30f,
        ),
        GuideStep(
            "Ignite",
            "Neutral beams heat the fuel until the nuclei overcome their repulsion. Fusion releases fast neutrons, and the plasma starts to sustain itself.",
            FR_CORE, CameraPose(28f, 14f, 1.06f), 0.10f, 0.78f,
        ),
        GuideStep(
            "Capture the heat",
            "The breeding blanket absorbs the neutron energy and makes tritium. Heat exchangers turn that energy into high-pressure steam.",
            FR_FUEL, CameraPose(30f, 18f, 1.03f), 0.18f, 0.56f,
        ),
        GuideStep(
            "Spin the generator",
            "Steam expands through the turbine and rotates the generator. The condenser returns the steam to water so the loop can repeat.",
            FR_POWER, CameraPose(38f, 16f, 1.03f), 0.12f, 0.45f,
        ),
        GuideStep(
            "Connect the grid",
            "The switchyard steps the generator voltage up for transmission. A flywheel and battery bus smooth the pulse between shots.",
            FR_GRID, CameraPose(48f, 18f, 1.02f), 0.04f, 0.40f,
        ),
        GuideStep(
            "The assembly",
            "Explode the tokamak, injectors, cryoplant and power island to see how the magnetic core, heat loop and grid fit together.",
            null, CameraPose(34f, 20f, 0.96f), 1f, 0.50f,
        ),
    )

    return ObjectSpec(
        id = "fusion_ring",
        index = 23,
        title = "FUSION F-9",
        designation = "FR-9 / TOKAMAK POWER LOOP",
        category = "Energy",
        categoryShort = "FUS",
        tagline = "A star in a magnetic bottle.",
        summary = "A working tokamak power exhibit: twelve superconducting D-coils, a glowing plasma torus, a breeding blanket, divertor exhaust, neutral-beam injectors, cryoplant, steam turbo-generator and grid connection.",
        details = listOf(
            "FUSION F-9 is a magnetic-confinement power plant. The toroidal field coils bend the plasma into a ring, while the central solenoid and poloidal coils shape and drive it. The plasma fuses hydrogen isotopes and releases energetic neutrons.",
            "The breeding blanket absorbs those neutrons, produces tritium and transfers heat to the steam loop. The turbine and generator convert that heat into electricity, while the cryoplant keeps the magnets superconducting.",
        ),
        specs = listOf(
            SpecRow("Plasma", "150 million °C"),
            SpecRow("Major radius", "0.72 m scale"),
            SpecRow("Coils", "12 TF + 4 PF"),
            SpecRow("Gain", "Q ≈ 8.4"),
            SpecRow("Grid output", "420 MW"),
        ),
        tags = listOf("fusion", "tokamak", "plasma", "energy", "reactor", "magnet", "power", "future"),
        accent = 0xFFA58BFF.toInt(),
        control = ControlSpec(
            label = "CONFINEMENT",
            lowLabel = "IGNITION",
            highLabel = "BURN",
            defaultValue = 0.62f,
            hint = "Plasma temperature, magnetic field, neutron rate and steam output",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(31f, 16f, 1.02f),
        thumbnailPose = CameraPose(33f, 18f, 1.04f),
        thumbnailTime = 2.2f,
        thumbnailControl = 0.72f,
        focusOffset = V3(0f, 0.10f, 0f),
        readouts = { ctx ->
            val temp = 92f + 68f * ctx.control
            val gain = 3.1f + 6.4f * ctx.control
            val output = 210f + 260f * ctx.control
            listOf(
                Readout("PLASMA", String.format("%.0f", temp), "MK", accent = true),
                Readout("GAIN", String.format("Q %.1f", gain), ""),
                Readout("OUTPUT", String.format("%.0f", output), "MW"),
            )
        },
        effects = { ctx ->
            val burn = (0.30f + 0.70f * ctx.control).coerceIn(0f, 1f)
            val pulse = 0.82f + 0.18f * sin(ctx.machineTime * 4.8f)
            val orbit = (0..28).map { index ->
                val angle = Math.toRadians(index * (360.0 / 28.0))
                V3((0.72 * cos(angle)).toFloat(), 1.58f, (0.72 * sin(angle)).toFloat())
            } + V3(0.72f, 1.58f, 0f)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.72f, 0xFFA58BFF.toInt(), 0f, 0.13f),
                SceneEffect.Ring(V3(0f, 1.58f, 0f), 1.28f, 0xFFA58BFF.toInt(), 0.2f, 0.10f + 0.10f * burn),
                SceneEffect.Glow(V3(0f, 1.58f, 0f), 0.82f, 0xFFBBA7FF.toInt(), 0.22f + 0.35f * burn),
                SceneEffect.Glow(V3(-0.72f, 1.62f, 0.52f), 0.30f, 0xFFC8B8FF.toInt(), 0.10f + 0.18f * burn),
                SceneEffect.Plume(
                    V3(1.27f, 1.20f, -0.56f), V3(0f, 1f, 0f),
                    0.46f, 0.16f, 0.14f + 0.22f * burn, PlumePalette.STEAM,
                ),
                SceneEffect.Plume(
                    V3(1.27f, 1.18f, -0.56f), V3(0.16f, 1f, 0.04f),
                    0.34f, 0.10f, 0.10f + 0.16f * burn, PlumePalette.STEAM,
                ),
                SceneEffect.Spark(V3(-1.31f, 0.92f, 0.59f), 0xFFC9B7FF.toInt(), 0.032f, 0.25f + 0.28f * burn),
                SceneEffect.Flow(
                    listOf(V3(0.72f, 0.53f, 0.55f), V3(1.02f, 0.83f, 0.55f), V3(1.33f, 0.69f, 0.55f)),
                    0xFFBDFFF5.toInt(), count = 8, speed = 0.22f, dotRadius = 0.024f,
                ),
                SceneEffect.Glow(V3(0f, 1.58f, 0f), 0.35f, frHotPlasma.fillColor(studio.anatomy.app.render.FaceMode.NORMAL, 1f), 0.12f * pulse),
                SceneEffect.Flow(
                    orbit, 0xFFC7B8FF.toInt(), count = 18, speed = 0.42f, phase = 0.16f, dotRadius = 0.012f,
                ),
            ) + listOf(18f, 138f, 258f).map { angle ->
                val port = circlePoint(1.30f, angle, 0.10f)
                SceneEffect.Beam(
                    V3(port.x, 1.68f, port.z), V3(0f, 1.58f, 0f),
                    0xFFDCCFFF.toInt(), 0.08f + 0.16f * burn,
                )
            }
        },
        descriptionTitle = "The magnetic star",
    )
}
