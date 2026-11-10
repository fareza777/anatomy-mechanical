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
import studio.anatomy.app.render.Material
import studio.anatomy.app.render.Materials
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder
import kotlin.math.sin

private const val WW_FRAME = "CIVIL WORKS"
private const val WW_TREAT = "TREATMENT"
private const val WW_STORE = "STORAGE"
private const val WW_DISTRIBUTE = "DISTRIBUTION"
private const val WW_CONTROL = "CONTROL"

private const val WW_PLINTH = "PLINTH"
private const val WW_INTAKE = "INTAKE"
private const val WW_TANKS = "PROCESS TRAIN"
private const val WW_FILTER = "FILTRATION"
private const val WW_TOWER = "TOWER"
private const val WW_VILLAGE = "HOUSES"

private val rawWater = Material(
    "rawWater", "Raw water", 0xFF4C8C8A.toInt(), specular = 0.56f, shininess = 36f, alpha = 185,
)
private val cleanWater = Material(
    "cleanWater", "Treated water", 0xFF4FD4E3.toInt(), specular = 0.70f, shininess = 58f, alpha = 158,
)
private val filterSand = Material(
    "filterSand", "Filter sand", 0xFFB7A17A.toInt(), specular = 0.12f, shininess = 8f,
)
private val filterCarbon = Material(
    "filterCarbon", "Activated carbon", 0xFF303A3B.toInt(), specular = 0.16f, shininess = 10f,
)
private val concrete = Material(
    "waterConcrete", "Settling concrete", 0xFF7D8582.toInt(), specular = 0.20f, shininess = 12f,
)

private fun waterImpeller(blades: Int, radius: Float): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.045f, 0.075f, 16, bevel = 0.008f))
    for (i in 0 until blades) {
        val angle = i * 360f / blades
        addMesh(
            Geo.box(radius, 0.036f, 0.080f),
            Mat4.rotateY(angle).multiply(Mat4.translation(V3(radius * 0.5f, 0f, 0f))),
        )
    }
}.build()

private fun houseRoof(): Mesh =
    Geo.lathe(listOf(0.34f to -0.11f, 0.012f to 0.11f), 4, capBottom = true)
        .transformed(Mat4.rotateY(45f))

private fun waterLevel(ctx: AnimContext): Float {
    val pulse = (sin(ctx.machineTime * 0.65f) + 1f) * 0.5f
    return 0.78f + 0.16f * ctx.control + 0.035f * pulse
}

fun waterWorksSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "water",
            width = 3.36f,
            depth = 2.28f,
            top = 0.30f,
            system = WW_FRAME,
            assembly = WW_PLINTH,
            accentMaterial = Materials.emitCyan,
        ),
    )

    // Intake river and screening rack.
    parts.add(
        Part(
            id = "ww_river", name = "Raw-water intake channel", system = WW_FRAME, assembly = WW_INTAKE,
            mesh = Geo.slab(0.42f, 1.05f, 0.035f, cornerRadius = 0.09f, bevel = 0.008f),
            material = rawWater,
            origin = V3(-1.34f, 0.318f, 0.32f),
        ),
    )
    parts.add(
        Part(
            id = "ww_river_bed", name = "River retaining wall", system = WW_FRAME, assembly = WW_INTAKE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.56f, 0.20f, 1.18f), Mat4.translation(V3(-1.34f, 0.16f, 0.32f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.035f, 0.16f, 1.16f), Mat4.translation(V3(-1.34f + side * 0.27f, 0.33f, 0.32f)))
                }
            }.build(),
            material = concrete,
        ),
    )
    parts.add(
        Part(
            id = "ww_screen", name = "Intake screen", system = WW_TREAT, assembly = WW_INTAKE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.28f, 0.035f, 0.32f), Mat4.translation(V3(-1.12f, 0.38f, 0.42f)))
                for (i in 0 until 7) {
                    addMesh(
                        Geo.box(0.012f, 0.20f, 0.32f),
                        Mat4.translation(V3(-1.22f + i * 0.035f, 0.47f, 0.42f)),
                    )
                }
            }.build(),
            material = Materials.nickel,
        ),
    )
    parts.add(
        Part(
            id = "ww_intake_pump", name = "Intake pump housing", system = WW_TREAT, assembly = WW_INTAKE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.145f, 0.26f, 22, bevel = 0.012f))
                addMesh(Geo.torus(0.148f, 0.012f, 22, 4), Mat4.translation(V3(0f, 0.10f, 0f)))
            }.build(),
            material = Materials.graphite,
            origin = V3(-0.96f, 0.56f, 0.39f),
        ),
    )
    parts.add(
        Part(
            id = "ww_intake_impeller", name = "Intake impeller", system = WW_TREAT, assembly = WW_INTAKE,
            mesh = waterImpeller(7, 0.12f), material = Materials.brass,
            origin = V3(-0.96f, 0.73f, 0.39f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * (55f + 95f * ctx.control)) },
        ),
    )
    parts.add(
        Part(
            id = "ww_raw_pipe", name = "Raw-water rising main", system = WW_TREAT, assembly = WW_INTAKE,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-0.96f, 0.72f, 0.39f),
                            V3(-0.92f, 0.98f, 0.28f),
                            V3(-0.80f, 1.18f, 0.08f),
                            V3(-0.71f, 1.15f, 0.03f),
                        ),
                        0.042f, 8, endRadius = 0.034f, subdivide = 3,
                    ),
                )
            }.build(),
            material = Materials.paintSlate,
        ),
    )

    // Coagulation tank with a slow paddle.
    val coagOrigin = V3(-0.70f, 0.73f, 0.04f)
    parts.add(
        Part(
            id = "ww_coag_shell", name = "Coagulation tank", system = WW_TREAT, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.hollowCylinder(0.305f, 0.285f, 0.86f, 32))
                listOf(-0.43f, 0.43f).forEach { y ->
                    addMesh(Geo.torus(0.305f, 0.014f, 30, 4), Mat4.translation(V3(0f, y, 0f)))
                }
            }.build(),
            material = Materials.glass,
            origin = coagOrigin,
        ),
    )
    parts.add(
        Part(
            id = "ww_coag_water", name = "Coagulating water", system = WW_TREAT, assembly = WW_TANKS,
            mesh = Geo.cylinder(0.268f, 0.54f, 28),
            material = rawWater,
            origin = coagOrigin + V3(0f, -0.10f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_coag_paddle", name = "Coagulation paddle", system = WW_TREAT, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.025f, 0.78f, 10), Mat4.translation(V3(0f, 0.04f, 0f)))
                for (arm in 0 until 2) {
                    addMesh(
                        Geo.box(0.22f, 0.035f, 0.020f),
                        Mat4.rotateY(arm * 180f).multiply(Mat4.translation(V3(0.12f, -0.02f, 0f))),
                    )
                    addMesh(
                        Geo.box(0.13f, 0.14f, 0.018f),
                        Mat4.rotateY(arm * 180f).multiply(Mat4.translation(V3(0.23f, -0.02f, 0f))),
                    )
                }
            }.build(),
            material = Materials.nickel,
            origin = coagOrigin + V3(0f, 0.03f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * (18f + 40f * ctx.control)) },
        ),
    )
    parts.add(
        Part(
            id = "ww_dosing_tank", name = "Coagulant dosing tank", system = WW_TREAT, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.10f, 0.30f, 18, bevel = 0.010f))
                addMesh(Geo.torus(0.10f, 0.010f, 18, 4), Mat4.translation(V3(0f, 0.13f, 0f)))
                addMesh(Geo.tube(listOf(V3(0f, 0.02f, 0.08f), V3(0.10f, 0.04f, 0.16f), V3(0.25f, 0.04f, 0.18f)), 0.014f, 6, subdivide = 2))
            }.build(),
            material = Materials.emitAmber,
            origin = V3(-0.96f, 0.55f, -0.19f),
        ),
    )

    // Clarifier: a wide settling tank with a rotating bridge and sludge cone.
    val clarifierOrigin = V3(0.02f, 0.65f, 0.06f)
    parts.add(
        Part(
            id = "ww_clarifier_shell", name = "Clarifier tank", system = WW_TREAT, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.hollowCylinder(0.48f, 0.455f, 0.68f, 36))
                addMesh(Geo.torus(0.48f, 0.015f, 34, 4), Mat4.translation(V3(0f, 0.33f, 0f)))
            }.build(),
            material = Materials.glass,
            origin = clarifierOrigin,
        ),
    )
    parts.add(
        Part(
            id = "ww_clarifier_water", name = "Settling water", system = WW_TREAT, assembly = WW_TANKS,
            mesh = Geo.cylinder(0.445f, 0.47f, 34),
            material = rawWater,
            origin = clarifierOrigin + V3(0f, -0.07f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_clarifier_bridge", name = "Rotating scraper bridge", system = WW_TREAT, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.94f, 0.035f, 0.06f), Mat4.translation(V3(0f, 0.06f, 0f)))
                addMesh(Geo.cylinder(0.035f, 0.20f, 12), Mat4.translation(V3(0f, 0.15f, 0f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.035f, 0.20f, 0.06f), Mat4.translation(V3(side * 0.40f, -0.04f, 0f)))
                    addMesh(Geo.box(0.16f, 0.028f, 0.08f), Mat4.translation(V3(side * 0.35f, -0.16f, 0f)))
                }
            }.build(),
            material = Materials.darkSteel,
            origin = clarifierOrigin + V3(0f, 0.36f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * (10f + 26f * ctx.control)) },
        ),
    )
    parts.add(
        Part(
            id = "ww_sludge_cone", name = "Sludge collection cone", system = WW_TREAT, assembly = WW_TANKS,
            mesh = Geo.lathe(listOf(0.42f to 0f, 0.08f to 0.30f), 24, capBottom = true),
            material = concrete,
            origin = clarifierOrigin + V3(0f, -0.34f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_coag_to_clarifier", name = "Clarifier inlet pipe", system = WW_TREAT, assembly = WW_TANKS,
            mesh = Geo.tube(
                listOf(
                    V3(-0.70f, 0.55f, 0.04f),
                    V3(-0.48f, 0.53f, 0.04f),
                    V3(-0.14f, 0.54f, 0.06f),
                    V3(0.02f, 0.92f, 0.06f),
                ),
                0.036f, 7, endRadius = 0.030f, subdivide = 3,
            ),
            material = Materials.paintSlate,
        ),
    )

    // Sand filter: glass shell, three visible media layers and a downflow path.
    val filterOrigin = V3(0.69f, 0.80f, 0.05f)
    parts.add(
        Part(
            id = "ww_filter_shell", name = "Sand filter", system = WW_FILTER, assembly = WW_FILTER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.hollowCylinder(0.285f, 0.265f, 0.98f, 30))
                addMesh(Geo.torus(0.285f, 0.012f, 30, 4), Mat4.translation(V3(0f, 0.49f, 0f)))
                addMesh(Geo.torus(0.285f, 0.012f, 30, 4), Mat4.translation(V3(0f, -0.49f, 0f)))
            }.build(),
            material = Materials.glass,
            origin = filterOrigin,
        ),
    )
    parts.add(
        Part(
            id = "ww_filter_water", name = "Filter water column", system = WW_FILTER, assembly = WW_FILTER,
            mesh = Geo.cylinder(0.250f, 0.34f, 28),
            material = cleanWater,
            origin = filterOrigin + V3(0f, 0.22f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_filter_sand", name = "Fine sand media", system = WW_FILTER, assembly = WW_FILTER,
            mesh = Geo.cylinder(0.248f, 0.20f, 28),
            material = filterSand,
            origin = filterOrigin + V3(0f, -0.18f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_filter_carbon", name = "Activated carbon layer", system = WW_FILTER, assembly = WW_FILTER,
            mesh = Geo.cylinder(0.248f, 0.12f, 28),
            material = filterCarbon,
            origin = filterOrigin + V3(0f, -0.34f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_filter_gravel", name = "Gravel support bed", system = WW_FILTER, assembly = WW_FILTER,
            mesh = Geo.cylinder(0.250f, 0.10f, 28),
            material = concrete,
            origin = filterOrigin + V3(0f, -0.43f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_filter_pipes", name = "Filter inlet and outlet", system = WW_FILTER, assembly = WW_FILTER,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.02f, 0.94f, 0.06f),
                            V3(0.35f, 1.20f, 0.06f),
                            V3(0.69f, 1.27f, 0.05f),
                            V3(0.69f, 1.05f, 0.05f),
                        ),
                        0.034f, 7, endRadius = 0.028f, subdivide = 3,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.69f, 0.42f, 0.05f),
                            V3(0.86f, 0.38f, 0.05f),
                            V3(1.05f, 0.42f, 0.05f),
                        ),
                        0.032f, 7, endRadius = 0.028f, subdivide = 3,
                    ),
                )
            }.build(),
            material = Materials.paintSlate,
        ),
    )

    // Clear-water tank and UV disinfection.
    val clearOrigin = V3(1.20f, 0.68f, 0.05f)
    parts.add(
        Part(
            id = "ww_clear_tank", name = "Clear-water tank", system = WW_STORE, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.hollowCylinder(0.245f, 0.225f, 0.72f, 28))
                addMesh(Geo.torus(0.245f, 0.010f, 28, 4), Mat4.translation(V3(0f, 0.36f, 0f)))
            }.build(),
            material = Materials.glass,
            origin = clearOrigin,
        ),
    )
    parts.add(
        Part(
            id = "ww_clear_water", name = "Disinfected water", system = WW_STORE, assembly = WW_TANKS,
            mesh = Geo.cylinder(0.218f, 0.54f, 26),
            material = cleanWater,
            origin = clearOrigin + V3(0f, -0.02f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "ww_uv_lamp", name = "UV disinfection lamp", system = WW_STORE, assembly = WW_TANKS,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.032f, 0.44f, 12), Mat4.translation(V3(0f, 0.04f, 0f)))
                addMesh(Geo.torus(0.040f, 0.008f, 14, 4), Mat4.translation(V3(0f, 0.27f, 0f)))
            }.build(),
            material = Materials.emitBlue,
            origin = clearOrigin + V3(0f, 0.12f, 0f),
        ),
    )

    // High-lift pump and the rising main to the tower.
    parts.add(
        Part(
            id = "ww_highlift", name = "High-lift pump", system = WW_DISTRIBUTE, assembly = WW_TOWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.17f, 0.34f, 22, bevel = 0.014f))
                addMesh(Geo.box(0.28f, 0.09f, 0.22f), Mat4.translation(V3(0f, -0.23f, 0f)))
                addMesh(Geo.torus(0.174f, 0.012f, 22, 4), Mat4.translation(V3(0f, 0.13f, 0f)))
            }.build(),
            material = Materials.graphite,
            origin = V3(1.20f, 0.58f, -0.43f),
        ),
    )
    parts.add(
        Part(
            id = "ww_highlift_impeller", name = "High-lift impeller", system = WW_DISTRIBUTE, assembly = WW_TOWER,
            mesh = waterImpeller(8, 0.14f), material = Materials.brass,
            origin = V3(1.20f, 0.77f, -0.43f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * (65f + 130f * ctx.control)) },
        ),
    )
    parts.add(
        Part(
            id = "ww_distribution", name = "Distribution main", system = WW_DISTRIBUTE, assembly = WW_TOWER,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(1.20f, 0.55f, -0.43f),
                            V3(1.25f, 1.12f, -0.52f),
                            V3(0.86f, 1.62f, -0.62f),
                            V3(-0.32f, 1.78f, -0.62f),
                            V3(-1.08f, 1.70f, -0.62f),
                        ),
                        0.040f, 8, endRadius = 0.032f, subdivide = 3,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-1.08f, 1.55f, -0.62f),
                            V3(-0.76f, 1.03f, -0.34f),
                            V3(-0.34f, 0.74f, 0.28f),
                            V3(0.16f, 0.46f, 0.58f),
                            V3(0.48f, 0.42f, 0.66f),
                        ),
                        0.028f, 7, endRadius = 0.022f, subdivide = 4,
                    ),
                )
            }.build(),
            material = Materials.paintSlate,
        ),
    )
    parts.add(
        Part(
            id = "ww_valves", name = "Distribution valves", system = WW_DISTRIBUTE, assembly = WW_TOWER,
            mesh = MeshBuilder().apply {
                listOf(-0.38f, 0.66f).forEach { x ->
                    addMesh(
                        Geo.torus(0.070f, 0.014f, 18, 4).transformed(Mat4.rotateX(90f)),
                        Mat4.translation(V3(x, 0.58f, 0.58f)),
                    )
                    addMesh(Geo.cylinder(0.022f, 0.12f, 8), Mat4.translation(V3(x, 0.53f, 0.58f)))
                }
            }.build(),
            material = Materials.emitAmber,
        ),
    )

    // Water tower.
    val towerOrigin = V3(-1.08f, 1.73f, -0.62f)
    parts.add(
        Part(
            id = "ww_tower_legs", name = "Tower support legs", system = WW_STORE, assembly = WW_TOWER,
            mesh = MeshBuilder().apply {
                listOf(-0.32f, 0.32f).forEach { x ->
                    listOf(-0.32f, 0.32f).forEach { z ->
                        addMesh(
                            Geo.cylinderBetween(
                                V3(-1.08f + x, 0.31f, -0.62f + z),
                                V3(-1.08f + x * 0.75f, 1.43f, -0.62f + z * 0.75f),
                                0.016f, 7,
                            ),
                        )
                    }
                }
                for (level in 0 until 3) {
                    val t = level / 3f
                    val y = 0.40f + t * 0.95f
                    val s = 0.32f - t * 0.08f
                    listOf(-1f, 1f).forEach { side ->
                        addMesh(Geo.box(0.016f, 0.016f, s * 2f), Mat4.translation(V3(-1.08f + side * s, y, -0.62f)))
                        addMesh(Geo.box(s * 2f, 0.016f, 0.016f), Mat4.translation(V3(-1.08f, y, -0.62f + side * s)))
                    }
                }
            }.build(),
            material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "ww_tower_shell", name = "Elevated water tank", system = WW_STORE, assembly = WW_TOWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.hollowCylinder(0.36f, 0.338f, 0.54f, 32))
                addMesh(Geo.lathe(listOf(0.36f to 0f, 0.06f to 0.18f), 32, capBottom = true), Mat4.translation(V3(0f, 0.27f, 0f)))
                listOf(-0.23f, 0.23f).forEach { y ->
                    addMesh(Geo.torus(0.36f, 0.014f, 30, 4), Mat4.translation(V3(0f, y, 0f)))
                }
            }.build(),
            material = Materials.glass,
            origin = towerOrigin,
        ),
    )
    parts.add(
        Part(
            id = "ww_tower_water", name = "Tower storage water", system = WW_STORE, assembly = WW_TOWER,
            mesh = Geo.cylinder(0.325f, 0.40f, 30).transformed(Mat4.translation(V3(0f, 0.20f, 0f))),
            material = cleanWater,
            origin = towerOrigin + V3(0f, -0.27f, 0f),
            animate = { ctx ->
                val level = waterLevel(ctx)
                Mat4.scale(V3(1f, level, 1f))
            },
        ),
    )
    parts.add(
        Part(
            id = "ww_tower_ladder", name = "Tower ladder and beacon", system = WW_STORE, assembly = WW_TOWER,
            mesh = MeshBuilder().apply {
                listOf(-0.09f, 0.09f).forEach { x ->
                    addMesh(Geo.cylinder(0.010f, 0.36f, 6), Mat4.translation(V3(x, 0.03f, 0.37f)))
                }
                for (i in 0 until 5) {
                    addMesh(Geo.box(0.18f, 0.010f, 0.010f), Mat4.translation(V3(0f, -0.11f + i * 0.07f, 0.37f)))
                }
                addMesh(Geo.sphere(0.035f, 12, 7), Mat4.translation(V3(0f, 0.31f, 0f)))
            }.build(),
            material = Materials.emitCyan,
            origin = towerOrigin,
        ),
    )

    // Small distribution village.
    parts.add(
        Part(
            id = "ww_houses", name = "Distribution houses", system = WW_DISTRIBUTE, assembly = WW_VILLAGE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.48f, 0.42f, 0.38f, cornerRadius = 0.025f, bevel = 0.010f), Mat4.translation(V3(0.50f, 0.49f, 0.69f)))
                addMesh(Geo.slab(0.36f, 0.34f, 0.30f, cornerRadius = 0.020f, bevel = 0.008f), Mat4.translation(V3(1.08f, 0.45f, 0.60f)))
                addMesh(Geo.box(0.055f, 0.16f, 0.055f), Mat4.translation(V3(0.65f, 0.71f, 0.57f)))
            }.build(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "ww_roofs", name = "Clay-tiled roofs", system = WW_DISTRIBUTE, assembly = WW_VILLAGE,
            mesh = MeshBuilder().apply {
                addMesh(houseRoof(), Mat4.translation(V3(0.50f, 0.76f, 0.69f)))
                addMesh(houseRoof().transformed(Mat4.scale(V3(0.78f, 0.78f, 0.78f))), Mat4.translation(V3(1.08f, 0.65f, 0.60f)))
            }.build(),
            material = Materials.paintOrange,
        ),
    )
    parts.add(
        Part(
            id = "ww_windows", name = "Lit house windows", system = WW_DISTRIBUTE, assembly = WW_VILLAGE,
            mesh = MeshBuilder().apply {
                listOf(
                    V3(0.36f, 0.50f, 0.905f), V3(0.62f, 0.50f, 0.905f),
                    V3(1.03f, 0.46f, 0.772f), V3(1.18f, 0.46f, 0.772f),
                ).forEach { point ->
                    addMesh(Geo.box(0.075f, 0.095f, 0.012f), Mat4.translation(point))
                }
            }.build(),
            material = Materials.emitAmber,
        ),
    )

    // Control cabinet and its status lights.
    parts.add(
        Part(
            id = "ww_control", name = "Plant control cabinet", system = WW_CONTROL, assembly = WW_PLINTH,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.62f, 0.22f, 0.52f, cornerRadius = 0.035f, bevel = 0.014f), Mat4.translation(V3(-1.28f, 0.56f, 0.91f)))
                addMesh(Geo.box(0.52f, 0.16f, 0.020f), Mat4.translation(V3(-1.28f, 0.47f, 0.995f)))
            }.build(),
            material = Materials.bedPlate,
        ),
    )
    parts.add(
        Part(
            id = "ww_control_lights", name = "Plant status display", system = WW_CONTROL, assembly = WW_PLINTH,
            mesh = MeshBuilder().apply {
                for (i in 0 until 5) {
                    addMesh(Geo.box(0.050f, 0.020f, 0.010f), Mat4.translation(V3(-1.48f + i * 0.10f, 0.59f, 1.010f)))
                }
                addMesh(Geo.box(0.24f, 0.014f, 0.010f), Mat4.translation(V3(-1.28f, 0.49f, 1.010f)))
            }.build(),
            material = Materials.emitCyan,
        ),
    )

    val rawPath = listOf(
        V3(-1.38f, 0.36f, 0.48f), V3(-1.20f, 0.40f, 0.40f), V3(-0.96f, 0.56f, 0.39f),
        V3(-0.92f, 0.99f, 0.28f), V3(-0.70f, 1.15f, 0.04f), V3(-0.70f, 0.76f, 0.04f),
        V3(-0.48f, 0.54f, 0.04f), V3(0.02f, 0.62f, 0.06f),
    )
    val filterPath = listOf(
        V3(0.02f, 0.78f, 0.06f), V3(0.32f, 1.15f, 0.06f), V3(0.69f, 1.22f, 0.05f),
        V3(0.69f, 0.90f, 0.05f), V3(0.69f, 0.40f, 0.05f), V3(1.05f, 0.45f, 0.05f),
    )
    val distributionPath = listOf(
        V3(1.20f, 0.54f, -0.43f), V3(1.27f, 1.10f, -0.52f), V3(0.86f, 1.60f, -0.62f),
        V3(-0.32f, 1.76f, -0.62f), V3(-1.08f, 1.68f, -0.62f), V3(-1.08f, 1.50f, -0.62f),
        V3(-0.74f, 0.94f, -0.30f), V3(-0.30f, 0.62f, 0.30f), V3(0.44f, 0.44f, 0.66f),
    )

    val hotspots = listOf(
        Hotspot(
            "intake", "River intake",
            "A screened intake draws raw water from the river without pulling in debris. The low-lift pump lifts it into the treatment train, where every following stage works by gravity.",
            "ww_intake_pump", V3(0f, 0.08f, 0.14f),
            CameraPose(42f, 18f, 1f), readoutLabel = "RAW WATER",
        ),
        Hotspot(
            "coagulation", "Coagulation tank",
            "A coagulant is dosed into the water and a slow paddle mixes it. Fine suspended particles stick together into larger flocs that can settle out downstream.",
            "ww_coag_shell", V3(0f, 0.06f, 0.22f),
            CameraPose(32f, 14f, 1f), readoutLabel = "CHEMICAL MIX",
        ),
        Hotspot(
            "clarifier", "Clarifier bridge",
            "The clarifier gives flocs time to settle. A rotating bridge scrapes the sludge toward the centre cone while clear water spills over the outer weir.",
            "ww_clarifier_bridge", V3(0f, 0.02f, 0.40f),
            CameraPose(28f, 24f, 1f), readoutLabel = "SEDIMENTATION",
        ),
        Hotspot(
            "filter", "Sand filter",
            "Water passes down through a graded bed of sand and activated carbon. The tiny gaps trap the remaining particles and dissolved organics, polishing the water before disinfection.",
            "ww_filter_shell", V3(0f, 0.05f, 0.25f),
            CameraPose(38f, 16f, 1f), readoutLabel = "FILTRATION",
        ),
        Hotspot(
            "uv", "UV disinfection",
            "The clear-water tank is dosed with ultraviolet light. UV damages the DNA of any remaining microbes without adding a chemical taste to the water.",
            "ww_uv_lamp", V3(0f, 0f, 0.05f),
            CameraPose(48f, 18f, 1f), readoutLabel = "DISINFECTION",
        ),
        Hotspot(
            "tower", "Elevated storage",
            "The water tower keeps a pressurized reserve above the town. During peak demand, gravity keeps the taps flowing even before the high-lift pump catches up.",
            "ww_tower_shell", V3(0f, 0.10f, 0.34f),
            CameraPose(142f, 18f, 1f), readoutLabel = "STORAGE",
        ),
        Hotspot(
            "houses", "Distribution loop",
            "A loop of buried mains carries treated water to every house. Service valves let crews isolate one branch without shutting down the rest of the town.",
            "ww_houses", V3(0.05f, 0.06f, 0.20f),
            CameraPose(24f, 12f, 1f), readoutLabel = "LAST MILE",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Take it from the river",
            "The intake screen stops branches and debris. A low-lift pump moves raw water into the plant; everything after this point flows downhill by gravity.",
            WW_INTAKE, CameraPose(42f, 18f, 1.03f), 0.08f, 0.30f,
        ),
        GuideStep(
            "Make the particles collide",
            "Coagulant neutralizes the surface charge on tiny particles. A slow paddle gives them a chance to collide and grow into flocs that can be removed.",
            WW_TANKS, CameraPose(32f, 14f, 1.02f), 0.0f, 0.55f,
        ),
        GuideStep(
            "Let the sludge settle",
            "In the clarifier, the water slows down so the flocs can sink. The rotating bridge keeps the sludge moving toward the central cone.",
            WW_TANKS, CameraPose(28f, 24f, 1.02f), 0.06f, 0.62f,
        ),
        GuideStep(
            "Polish through the bed",
            "Sand removes the remaining particles, while activated carbon adsorbs dissolved organic compounds and odours. Clean water collects below the media.",
            WW_FILTER, CameraPose(38f, 16f, 1.02f), 0.10f, 0.48f,
        ),
        GuideStep(
            "Lift it above the town",
            "The high-lift pump fills the elevated tank. The tower's height provides pressure without a pump at every house, and its reserve covers the morning peak.",
            WW_TOWER, CameraPose(142f, 18f, 1.03f), 0.06f, 0.52f,
        ),
        GuideStep(
            "The assembly",
            "Explode the process train to see the intake, coagulation tank, clarifier, filter, clear-water tank and elevated storage as separate stages.",
            null, CameraPose(32f, 18f, 0.96f), 1f, 0.45f,
        ),
    )

    return ObjectSpec(
        id = "water_works",
        index = 21,
        title = "AQUA CIVIC W-4",
        designation = "AW-4 / WATER TREATMENT LOOP",
        category = "Infrastructure",
        categoryShort = "CIV",
        tagline = "From muddy river to the kitchen tap.",
        summary = "A miniature municipal water works: screened river intake, coagulation, a rotating clarifier bridge, a layered sand filter, UV disinfection, a high-lift pump and an elevated tower feeding a small town.",
        details = listOf(
            "AQUA CIVIC W-4 is a working section through a drinking-water plant. Raw water enters through a screen and is dosed with coagulant. Flocs form in the paddle tank, settle in the clarifier, and are trapped in a graded sand-and-carbon bed.",
            "After UV disinfection the clear-water tank feeds a high-lift pump. The pump charges the elevated tower, and gravity distributes the water to the houses through a valved loop.",
        ),
        specs = listOf(
            SpecRow("Capacity", "140 L/s"),
            SpecRow("Process", "Coagulation + filtration"),
            SpecRow("Filters", "Sand / carbon"),
            SpecRow("Storage", "450 m³"),
            SpecRow("Service", "24 h pressure"),
        ),
        tags = listOf("water", "treatment", "city", "infrastructure", "filter", "pump", "tower", "sustainable"),
        accent = 0xFF68D8E8.toInt(),
        control = ControlSpec(
            label = "DEMAND",
            lowLabel = "NIGHT",
            highLabel = "PEAK",
            defaultValue = 0.48f,
            hint = "Pump speed, process flow, tower level and village demand",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(33f, 16f, 1.02f),
        thumbnailPose = CameraPose(34f, 18f, 1.04f),
        thumbnailTime = 2.7f,
        thumbnailControl = 0.55f,
        focusOffset = V3(0f, 0.04f, 0f),
        readouts = { ctx ->
            val flow = 78f + 62f * ctx.control
            val turbidity = 0.72f - 0.54f * ctx.control
            val storage = 64f + 26f * ctx.control
            listOf(
                Readout("FLOW", String.format("%.0f", flow), "L/s", accent = true),
                Readout("TURBIDITY", String.format("%.2f", turbidity), "NTU"),
                Readout("STORAGE", String.format("%.0f", storage), "%"),
            )
        },
        effects = { ctx ->
            val p = 0.4f + 0.6f * ctx.control
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.72f, 0xFF68D8E8.toInt(), 0f, 0.14f),
                SceneEffect.Flow(rawPath, 0xFFB5B88F.toInt(), count = 16, speed = 0.28f, dotRadius = 0.030f),
                SceneEffect.Flow(filterPath, 0xFF80F5F1.toInt(), count = 12, speed = 0.30f, phase = 0.2f, dotRadius = 0.028f),
                SceneEffect.Flow(distributionPath, 0xFF6EE8F2.toInt(), count = 20, speed = 0.25f, phase = 0.45f, dotRadius = 0.030f),
                SceneEffect.Glow(V3(-1.08f, 1.70f, -0.62f), 0.38f, 0xFF8CF5FF.toInt(), 0.12f + 0.14f * p),
                SceneEffect.Glow(V3(1.20f, 0.68f, 0.05f), 0.30f, 0xFF63E6F4.toInt(), 0.18f),
                SceneEffect.Glow(V3(0.78f, 0.45f, 0.72f), 0.22f, 0xFFFFC06A.toInt(), 0.20f + 0.16f * p),
                SceneEffect.Spark(V3(1.20f, 0.40f, -0.43f), 0xFF75F4F1.toInt(), 0.025f, 0.25f + 0.35f * p),
            )
        },
        descriptionTitle = "The civic water loop",
    )
}
