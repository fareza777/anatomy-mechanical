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
import kotlin.math.sin

private const val WG_TOWER = "TOWER"
private const val WG_ROTOR = "ROTOR"
private const val WG_DRIVE = "DRIVE"
private const val WG_GENERATOR = "GENERATOR"
private const val WG_GRID = "GRID"
private const val WG_HOUSE = "HOUSES"

private const val WG_LATTICE = "LATTICE"
private const val WG_HEAD = "WIND HEAD"
private const val WG_SHAFT = "VERTICAL SHAFT"
private const val WG_BASE = "BASE DRIVE"
private const val WG_POWER = "POWERHOUSE"
private const val WG_CABLE = "DISTRIBUTION"
private const val WG_HOMES = "TWO HOMES"

private const val MILL_X = -1.06f
private const val MILL_Z = -0.18f
private const val HUB_Y = 2.31f
private const val HUB_Z = 0.42f

private fun rotorAngle(ctx: AnimContext): Float =
    ctx.machineTime * (42f + 138f * ctx.control)

private fun shaftAngle(ctx: AnimContext): Float =
    rotorAngle(ctx) * 1.7f

private fun generatorAngle(ctx: AnimContext): Float =
    rotorAngle(ctx) * 7.2f

private fun windOutput(ctx: AnimContext): Float =
    0.25f + 4.75f * ctx.control

private fun bladeMesh(): Mesh = MeshBuilder().apply {
    // Twelve shallow-pitched sheet blades make a farm windmill read instantly as a wheel.
    for (i in 0 until 12) {
        val angle = i * 30f
        val blade = Geo.slab(0.86f, 0.125f, 0.026f, 0.012f, 0.006f)
            .transformed(Mat4.translation(V3(0.58f, 0f, 0f)).multiply(Mat4.rotateX(18f)))
        addMesh(blade, Mat4.rotateZ(angle))
        addMesh(
            Geo.slab(0.16f, 0.105f, 0.032f, 0.012f, 0.005f),
            Mat4.rotateZ(angle).multiply(Mat4.translation(V3(0.94f, 0f, 0f))),
        )
    }
    // Outer rim segments tie the wheel together without closing the air path.
    for (i in 0 until 12) {
        addMesh(
            Geo.cylinderBetween(
                V3(0.985f * kotlin.math.cos(Math.toRadians(i * 30.0)).toFloat(), 0.985f * kotlin.math.sin(Math.toRadians(i * 30.0)).toFloat(), 0f),
                V3(0.985f * kotlin.math.cos(Math.toRadians((i + 1) * 30.0)).toFloat(), 0.985f * kotlin.math.sin(Math.toRadians((i + 1) * 30.0)).toFloat(), 0f),
                0.012f, 6,
            ),
        )
    }
}.build()

private fun hubMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.155f, 0.23f, 28, bevel = 0.025f).transformed(Mat4.rotateX(90f)))
    addMesh(Geo.taperedCylinder(0.095f, 0.025f, 0.18f, 20).transformed(Mat4.rotateX(-90f)), Mat4.translation(V3(0f, 0f, 0.18f)))
    addMesh(Geo.torus(0.158f, 0.014f, 26, 5).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, 0.10f)))
}.build()

private fun towerMesh(): Mesh = MeshBuilder().apply {
    val base = 0.40f
    val top = 0.14f
    listOf(-1f, 1f).forEach { sx ->
        listOf(-1f, 1f).forEach { sz ->
            addMesh(
                Geo.cylinderBetween(
                    V3(MILL_X + sx * base, 0.31f, MILL_Z + sz * 0.33f),
                    V3(MILL_X + sx * top, 2.28f, MILL_Z + sz * 0.12f),
                    0.032f, 8,
                ),
            )
            addMesh(
                Geo.cylinderBetween(
                    V3(MILL_X + sx * base, 0.31f, MILL_Z + sz * 0.33f),
                    V3(MILL_X + sx * 0.18f, 0.95f, MILL_Z + sz * 0.18f),
                    0.055f, 10,
                ),
            )
        }
    }
    // Cross bracing: two directions per bay.
    listOf(0.58f, 0.92f, 1.28f, 1.66f, 2.04f).forEach { y ->
        val t = (y - 0.31f) / 1.97f
        val half = base + (top - base) * t
        listOf(-1f, 1f).forEach { side ->
            addMesh(
                Geo.cylinderBetween(
                    V3(MILL_X - half, y, MILL_Z + side * 0.20f),
                    V3(MILL_X + half, y, MILL_Z + side * 0.20f),
                    0.018f, 6,
                ),
            )
            addMesh(
                Geo.cylinderBetween(
                    V3(MILL_X + side * half, y, MILL_Z - 0.20f),
                    V3(MILL_X + side * half, y, MILL_Z + 0.20f),
                    0.018f, 6,
                ),
            )
        }
    }
    for (i in 0 until 4) {
        val y0 = 0.40f + i * 0.48f
        val y1 = y0 + 0.48f
        val t0 = (y0 - 0.31f) / 1.97f
        val t1 = (y1 - 0.31f) / 1.97f
        val h0 = base + (top - base) * t0
        val h1 = base + (top - base) * t1
        addMesh(Geo.cylinderBetween(V3(MILL_X - h0, y0, MILL_Z - 0.21f), V3(MILL_X + h1, y1, MILL_Z - 0.21f), 0.014f, 5))
        addMesh(Geo.cylinderBetween(V3(MILL_X + h0, y0, MILL_Z + 0.21f), V3(MILL_X - h1, y1, MILL_Z + 0.21f), 0.014f, 5))
    }
    addMesh(Geo.slab(0.94f, 0.74f, 0.06f, 0.05f, 0.02f), Mat4.translation(V3(MILL_X, 2.28f, MILL_Z)))
    addMesh(Geo.box(0.50f, 0.06f, 0.36f), Mat4.translation(V3(MILL_X, 2.34f, MILL_Z - 0.02f)))
}.build()

private fun millHeadMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.34f, 0.48f, 0.24f, 0.045f, 0.02f), Mat4.translation(V3(0f, 0f, -0.10f)))
    addMesh(Geo.cylinder(0.085f, 0.30f, 18).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, 0.20f)))
    addMesh(Geo.torus(0.10f, 0.014f, 20, 5).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, 0.37f)))
}.build()

private fun tailVaneMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinderBetween(V3(0f, 0f, 0.02f), V3(0f, 0f, -0.52f), 0.030f, 8))
    addMesh(Geo.slab(0.035f, 0.50f, 0.34f, 0.02f, 0.008f), Mat4.translation(V3(0f, 0.13f, -0.58f)))
    addMesh(Geo.slab(0.045f, 0.40f, 0.06f, 0.015f, 0.005f), Mat4.translation(V3(0f, 0.28f, -0.58f)))
}.build()

private fun verticalShaftMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.035f, 1.68f, 14, bevel = 0.006f), Mat4.translation(V3(0f, 0.84f, 0f)))
    listOf(0.10f, 0.50f, 0.92f, 1.34f, 1.66f).forEach { y ->
        addMesh(Geo.torus(0.044f, 0.009f, 16, 4), Mat4.translation(V3(0f, y, 0f)))
    }
    addMesh(Geo.box(0.020f, 1.32f, 0.014f), Mat4.translation(V3(0f, 0.90f, 0.040f)))
    addMesh(Geo.cylinder(0.060f, 0.09f, 14), Mat4.translation(V3(0f, 1.66f, 0f)))
    addMesh(Geo.cylinder(0.060f, 0.09f, 14), Mat4.translation(V3(0f, 0.10f, 0f)))
}.build()

private fun pulleyMesh(radius: Float): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(radius, 0.10f, 28, bevel = 0.012f).transformed(Mat4.rotateZ(90f)))
    addMesh(Geo.torus(radius * 0.82f, 0.014f, 24, 5).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(0.058f, 0f, 0f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.075f, radius * 1.46f, 0.035f),
            Mat4.rotateX(72f * i),
        )
    }
    addMesh(Geo.cylinder(radius * 0.18f, 0.16f, 14).transformed(Mat4.rotateZ(90f)))
}.build()

private fun generatorShellMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.185f, 0.46f, 28).transformed(Mat4.rotateZ(90f)))
    listOf(-0.23f, 0.23f).forEach { x ->
        addMesh(Geo.torus(0.188f, 0.014f, 26, 5).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(x, 0f, 0f)))
    }
    for (i in 0 until 6) {
        addMesh(
            Geo.box(0.035f, 0.29f, 0.035f),
            Mat4.rotateX(30f + i * 60f).multiply(Mat4.translation(V3(0f, 0.168f, 0f))),
        )
    }
    addMesh(Geo.cylinder(0.11f, 0.07f, 20).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(0.27f, 0f, 0f)))
}.build()

private fun generatorRotorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.105f, 0.34f, 22).transformed(Mat4.rotateZ(90f)))
    for (i in 0 until 8) {
        addMesh(
            Geo.box(0.30f, 0.032f, 0.025f),
            Mat4.rotateX(45f * i).multiply(Mat4.translation(V3(0f, 0.092f, 0f))),
        )
    }
    addMesh(Geo.cylinder(0.045f, 0.66f, 14).transformed(Mat4.rotateZ(90f)))
}.build()

private fun generatorFanMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.035f, 0.035f, 14).transformed(Mat4.rotateZ(90f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.014f, 0.15f, 0.07f),
            Mat4.rotateX(72f * i).multiply(Mat4.translation(V3(0f, 0f, 0.082f))),
        )
    }
}.build()

private fun transformerMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.145f, 0.54f, 26, bevel = 0.025f), Mat4.translation(V3(0f, 0.27f, 0f)))
    for (i in 0 until 5) {
        addMesh(Geo.torus(0.153f, 0.012f, 24, 4), Mat4.translation(V3(0f, 0.10f + i * 0.085f, 0f)))
    }
    for (i in 0 until 3) {
        val a = Math.toRadians(210.0 + i * 60.0)
        addMesh(
            Geo.cylinder(0.034f, 0.15f, 12),
            Mat4.translation(V3(0f, 0.66f, 0f))
                .multiply(Mat4.translation(V3((0.10f * kotlin.math.cos(a)).toFloat(), 0f, (0.10f * kotlin.math.sin(a)).toFloat()))),
        )
    }
    addMesh(Geo.slab(0.46f, 0.12f, 0.05f, 0.015f, 0.006f), Mat4.translation(V3(0f, 0.03f, 0f)))
}.build()

private fun houseMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.48f, 0.42f, 0.60f, 0.025f, 0.012f, 2), Mat4.translation(V3(0f, 0.30f, 0f)))
    addMesh(Geo.box(0.32f, 0.025f, 0.46f), Mat4.translation(V3(-0.12f, 0.63f, 0f)).multiply(Mat4.rotateZ(28f)))
    addMesh(Geo.box(0.32f, 0.025f, 0.46f), Mat4.translation(V3(0.12f, 0.63f, 0f)).multiply(Mat4.rotateZ(-28f)))
    addMesh(Geo.box(0.075f, 0.22f, 0.075f), Mat4.translation(V3(0.14f, 0.70f, -0.08f)))
    // Door and window frames.
    addMesh(Geo.box(0.13f, 0.25f, 0.014f), Mat4.translation(V3(0f, 0.14f, 0.215f)))
    addMesh(Geo.box(0.16f, 0.03f, 0.032f), Mat4.translation(V3(0f, 0.275f, 0.225f)))
    addMesh(Geo.box(0.12f, 0.12f, 0.012f), Mat4.translation(V3(0.15f, 0.32f, 0.216f)))
}.build()

private fun houseWindowMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.10f, 0.10f, 0.008f), Mat4.translation(V3(0.15f, 0.32f, 0.223f)))
    addMesh(Geo.box(0.08f, 0.06f, 0.010f), Mat4.translation(V3(0f, 0.32f, 0.236f)))
}.build()

fun windGridSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "grid",
            width = 4.05f,
            depth = 2.7f,
            top = 0.30f,
            system = WG_GRID,
            assembly = WG_CABLE,
            accentMaterial = Materials.emitAmber,
        ),
    )

    parts.add(
        Part(
            id = "wg_tower",
            name = "Lattice windmill tower",
            system = WG_TOWER, assembly = WG_LATTICE,
            mesh = towerMesh(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "wg_tower_feet",
            name = "Tower foundation",
            system = WG_TOWER, assembly = WG_LATTICE,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { sx ->
                    listOf(-1f, 1f).forEach { sz ->
                        addMesh(
                            Geo.slab(0.24f, 0.24f, 0.09f, 0.03f, 0.012f),
                            Mat4.translation(V3(MILL_X + sx * 0.40f, 0.35f, MILL_Z + sz * 0.33f)),
                        )
                        addMesh(
                            Geo.hexBolt(0.028f, 0.030f),
                            Mat4.translation(V3(MILL_X + sx * 0.40f, 0.41f, MILL_Z + sz * 0.33f)),
                        )
                    }
                }
            }.build(),
            material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "wg_mill_head",
            name = "Wind head",
            system = WG_TOWER, assembly = WG_HEAD,
            mesh = millHeadMesh(),
            material = Materials.paintSlate,
            origin = V3(MILL_X, HUB_Y, HUB_Z - 0.23f),
        ),
    )
    parts.add(
        Part(
            id = "wg_rotor",
            name = "Twelve-blade rotor",
            system = WG_ROTOR, assembly = WG_HEAD,
            mesh = bladeMesh(),
            material = Materials.paintCream,
            origin = V3(MILL_X, HUB_Y, HUB_Z),
            animate = { ctx -> Mat4.rotateZ(rotorAngle(ctx)) },
            doubleSided = true,
            explode = V3(0f, 0f, 0.8f),
        ),
    )
    parts.add(
        Part(
            id = "wg_rotor_tips",
            name = "Rotor tip markers",
            system = WG_ROTOR, assembly = WG_HEAD,
            mesh = MeshBuilder().apply {
                for (i in 0 until 12) {
                    addMesh(
                        Geo.slab(0.16f, 0.105f, 0.032f, 0.012f, 0.005f),
                        Mat4.rotateZ(i * 30f).multiply(Mat4.translation(V3(0.94f, 0f, 0.01f))),
                    )
                }
            }.build(),
            material = Materials.paintOrange,
            origin = V3(MILL_X, HUB_Y, HUB_Z + 0.02f),
            animate = { ctx -> Mat4.rotateZ(rotorAngle(ctx)) },
            explode = V3(0f, 0f, 0.8f),
        ),
    )
    parts.add(
        Part(
            id = "wg_hub",
            name = "Hub and pitch bearing",
            system = WG_ROTOR, assembly = WG_HEAD,
            mesh = hubMesh(),
            material = Materials.paintOrange,
            origin = V3(MILL_X, HUB_Y, HUB_Z),
            animate = { ctx -> Mat4.rotateZ(rotorAngle(ctx)) },
            explode = V3(0f, 0f, 1.0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_tail",
            name = "Tail vane",
            system = WG_TOWER, assembly = WG_HEAD,
            mesh = tailVaneMesh(),
            material = Materials.paintOrange,
            origin = V3(MILL_X, HUB_Y + 0.02f, HUB_Z - 0.27f),
            animate = { ctx -> Mat4.rotateY(7f * sin(ctx.machineTime * 0.21f + 0.4f)) },
            explode = V3(0f, 0f, -0.9f),
        ),
    )

    parts.add(
        Part(
            id = "wg_vertical_shaft",
            name = "Vertical drive shaft",
            system = WG_DRIVE, assembly = WG_SHAFT,
            mesh = verticalShaftMesh(),
            material = Materials.nickel,
            origin = V3(MILL_X, 0.50f, MILL_Z - 0.10f),
            animate = { ctx -> Mat4.rotateY(shaftAngle(ctx)) },
            explode = V3(0f, 0.8f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_top_gear",
            name = "Top bevel gear",
            system = WG_DRIVE, assembly = WG_SHAFT,
            mesh = MeshBuilder().apply {
                addMesh(Geo.gear(0.165f, 18, 0.070f, 0.020f, 3).transformed(Mat4.rotateX(90f)))
                addMesh(Geo.cylinder(0.050f, 0.09f, 14).transformed(Mat4.rotateZ(90f)))
            }.build(),
            material = Materials.brass,
            origin = V3(MILL_X, 2.21f, MILL_Z - 0.03f),
            animate = { ctx -> Mat4.rotateX(rotorAngle(ctx)) },
        ),
    )
    parts.add(
        Part(
            id = "wg_bottom_gear",
            name = "Base bevel gears",
            system = WG_DRIVE, assembly = WG_BASE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.gear(0.19f, 20, 0.075f, 0.020f, 3), Mat4.translation(V3(0f, 0f, 0f)))
                addMesh(Geo.gear(0.16f, 17, 0.070f, 0.020f, 3).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0.11f, 0f, 0f)))
                addMesh(Geo.slab(0.44f, 0.36f, 0.20f, 0.04f, 0.015f), Mat4.translation(V3(0.05f, -0.16f, 0f)))
            }.build(),
            material = Materials.brass,
            origin = V3(MILL_X, 0.56f, MILL_Z - 0.10f),
            animate = { ctx -> Mat4.rotateY(shaftAngle(ctx)) },
            explode = V3(0f, -0.2f, 0f),
        ),
    )

    parts.add(
        Part(
            id = "wg_base_frame",
            name = "Generator bedplate",
            system = WG_DRIVE, assembly = WG_BASE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(1.55f, 0.48f, 0.12f, 0.035f, 0.015f), Mat4.translation(V3(-0.24f, 0.36f, -0.18f)))
                listOf(-0.92f, -0.42f, 0.18f, 0.52f).forEach { x ->
                    addMesh(Geo.box(0.10f, 0.16f, 0.10f), Mat4.translation(V3(x, 0.46f, -0.18f)))
                }
            }.build(),
            material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "wg_flywheel",
            name = "Drive flywheel",
            system = WG_DRIVE, assembly = WG_BASE,
            mesh = pulleyMesh(0.205f),
            material = Materials.brushedSteel,
            origin = V3(-0.80f, 0.62f, -0.18f),
            animate = { ctx -> Mat4.rotateX(shaftAngle(ctx)) },
        ),
    )
    parts.add(
        Part(
            id = "wg_belt",
            name = "Generator belt",
            system = WG_DRIVE, assembly = WG_BASE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.tube(listOf(V3(-0.80f, 0.82f, -0.18f), V3(-0.08f, 0.75f, -0.18f)), 0.020f, 6, subdivide = 2))
                addMesh(Geo.tube(listOf(V3(-0.80f, 0.42f, -0.18f), V3(-0.08f, 0.49f, -0.18f)), 0.020f, 6, subdivide = 2))
            }.build(),
            material = Materials.rubber,
        ),
    )
    parts.add(
        Part(
            id = "wg_generator_pulley",
            name = "Generator pulley",
            system = WG_DRIVE, assembly = WG_BASE,
            mesh = pulleyMesh(0.135f),
            material = Materials.nickel,
            origin = V3(-0.06f, 0.62f, -0.18f),
            animate = { ctx -> Mat4.rotateX(generatorAngle(ctx)) },
        ),
    )

    parts.add(
        Part(
            id = "wg_powerhouse",
            name = "Powerhouse shell",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(1.10f, 0.86f, 0.06f, 0.035f, 0.014f), Mat4.translation(V3(0.20f, 0.33f, -0.22f)))
                addMesh(Geo.slab(1.06f, 0.06f, 0.80f, 0.035f, 0.014f), Mat4.translation(V3(0.20f, 0.73f, -0.62f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.slab(0.06f, 0.78f, 0.80f, 0.035f, 0.014f), Mat4.translation(V3(0.20f + side * 0.50f, 0.73f, -0.22f)))
                }
                addMesh(Geo.slab(1.14f, 0.90f, 0.06f, 0.035f, 0.014f), Mat4.translation(V3(0.20f, 1.14f, -0.22f)))
                addMesh(Geo.box(1.06f, 0.05f, 0.06f), Mat4.translation(V3(0.20f, 1.18f, 0.21f)))
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.cylinderBetween(V3(0.20f + side * 0.48f, 0.34f, 0.16f), V3(0.20f + side * 0.48f, 1.10f, 0.16f), 0.028f, 7))
                }
            }.build(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "wg_powerhouse_trim",
            name = "Powerhouse openings",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.box(0.53f, 0.42f, 0.018f), Mat4.translation(V3(0.20f, 0.74f, 0.215f)))
                addMesh(Geo.box(0.35f, 0.06f, 0.028f), Mat4.translation(V3(0.20f, 1.02f, 0.228f)))
                addMesh(Geo.box(1.05f, 0.035f, 0.06f), Mat4.translation(V3(0.20f, 1.06f, 0.22f)))
            }.build(),
            material = Materials.graphite,
        ),
    )
    parts.add(
        Part(
            id = "wg_generator_shell",
            name = "Generator shell",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = generatorShellMesh(),
            material = Materials.glass,
            origin = V3(0.20f, 0.66f, -0.27f),
            doubleSided = true,
            explode = V3(0.8f, 0.4f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_generator_coils",
            name = "Generator stator coils",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = MeshBuilder().apply {
                for (i in 0 until 5) {
                    addMesh(Geo.torus(0.165f, 0.012f, 24, 4).transformed(Mat4.rotateZ(90f)), Mat4.translation(V3(-0.16f + i * 0.08f, 0f, 0f)))
                }
            }.build(),
            material = Materials.copperBright,
            origin = V3(0.20f, 0.66f, -0.27f),
            explode = V3(0.8f, 0.4f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_generator_rotor",
            name = "Generator rotor",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = generatorRotorMesh(),
            material = Materials.brass,
            origin = V3(0.20f, 0.66f, -0.27f),
            animate = { ctx -> Mat4.rotateX(generatorAngle(ctx)) },
            explode = V3(0.8f, 0.4f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_generator_fan",
            name = "Generator cooling fan",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = generatorFanMesh(),
            material = Materials.nickel,
            origin = V3(0.51f, 0.66f, -0.27f),
            animate = { ctx -> Mat4.rotateX(generatorAngle(ctx)) },
            explode = V3(0.9f, 0.4f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_generator_terminal",
            name = "Generator terminals",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.slab(0.24f, 0.18f, 0.14f, 0.025f, 0.010f), Mat4.translation(V3(0f, 0f, 0f)))
                listOf(-0.06f, 0f, 0.06f).forEach { z ->
                    addMesh(Geo.cylinder(0.018f, 0.055f, 10), Mat4.translation(V3(0f, 0.10f, z)))
                }
            }.build(),
            material = Materials.paintSlate,
            origin = V3(0.20f, 0.96f, -0.27f),
            explode = V3(0.8f, 0.4f, 0f),
        ),
    )
    parts.add(
        Part(
            id = "wg_output_meter",
            name = "Output meter",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.075f, 0.018f, 20).transformed(Mat4.rotateX(90f)))
                addMesh(Geo.torus(0.073f, 0.008f, 20, 4).transformed(Mat4.rotateX(90f)))
            }.build(),
            material = Materials.paintCream,
            origin = V3(-0.22f, 0.95f, 0.235f),
        ),
    )
    parts.add(
        Part(
            id = "wg_meter_needle",
            name = "Output needle",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = Geo.box(0.008f, 0.055f, 0.006f).transformed(Mat4.translation(V3(0f, 0.025f, 0f))),
            material = Materials.emitAmber,
            origin = V3(-0.22f, 0.95f, 0.248f),
            animate = { ctx -> Mat4.rotateZ(-118f + 236f * ctx.control) },
        ),
    )
    parts.add(
        Part(
            id = "wg_powerhouse_lamp",
            name = "Powerhouse lamp",
            system = WG_GENERATOR, assembly = WG_POWER,
            mesh = Geo.box(0.18f, 0.025f, 0.06f),
            material = Materials.emitAmber,
            origin = V3(0.20f, 1.05f, 0.05f),
            animate = { ctx ->
                val pulse = 0.9f + 0.1f * sin(ctx.machineTime * 2.4f)
                Mat4.scale(V3(1f, pulse, 1f))
            },
        ),
    )

    parts.add(
        Part(
            id = "wg_transformer",
            name = "Step-up transformer",
            system = WG_GRID, assembly = WG_CABLE,
            mesh = transformerMesh(),
            material = Materials.paintSlate,
            origin = V3(0.82f, 0.30f, -0.67f),
            explode = V3(0.5f, 0.3f, -0.4f),
        ),
    )
    parts.add(
        Part(
            id = "wg_transformer_coils",
            name = "Transformer coils",
            system = WG_GRID, assembly = WG_CABLE,
            mesh = MeshBuilder().apply {
                for (i in 0 until 6) {
                    addMesh(Geo.torus(0.150f, 0.013f, 24, 4), Mat4.translation(V3(0f, 0.12f + i * 0.062f, 0f)))
                }
                listOf(-1f, 1f).forEach { side ->
                    addMesh(Geo.box(0.035f, 0.40f, 0.16f), Mat4.translation(V3(side * 0.155f, 0.32f, 0f)))
                }
            }.build(),
            material = Materials.copperBright,
            origin = V3(0.82f, 0.30f, -0.67f),
            explode = V3(0.5f, 0.3f, -0.4f),
        ),
    )
    parts.add(
        Part(
            id = "wg_transformer_light",
            name = "Transformer indicator",
            system = WG_GRID, assembly = WG_CABLE,
            mesh = Geo.torus(0.162f, 0.010f, 26, 4),
            material = Materials.emitCyan,
            origin = V3(0.82f, 0.61f, -0.67f),
            animate = { ctx ->
                val pulse = 0.88f + 0.12f * sin(ctx.machineTime * 3.2f)
                Mat4.scale(V3(pulse, 1f, pulse))
            },
        ),
    )

    parts.add(
        Part(
            id = "wg_pole",
            name = "Distribution pole",
            system = WG_GRID, assembly = WG_CABLE,
            mesh = MeshBuilder().apply {
                addMesh(Geo.taperedCylinder(0.055f, 0.038f, 1.28f, 12), Mat4.translation(V3(1.18f, 0.98f, -0.12f)))
                addMesh(Geo.box(0.52f, 0.045f, 0.045f), Mat4.translation(V3(1.18f, 1.53f, -0.12f)))
                listOf(-0.18f, 0f, 0.18f).forEach { dx ->
                    addMesh(Geo.cylinder(0.026f, 0.055f, 10), Mat4.translation(V3(1.18f + dx, 1.57f, -0.12f)))
                    addMesh(Geo.torus(0.030f, 0.006f, 12, 4), Mat4.translation(V3(1.18f + dx, 1.60f, -0.12f)))
                }
                addMesh(Geo.slab(0.18f, 0.18f, 0.045f, 0.02f, 0.008f), Mat4.translation(V3(1.18f, 0.33f, -0.12f)))
            }.build(),
            material = Materials.paintCream,
        ),
    )
    parts.add(
        Part(
            id = "wg_wires",
            name = "Service wires",
            system = WG_GRID, assembly = WG_CABLE,
            mesh = MeshBuilder().apply {
                listOf(-0.16f, 0f, 0.16f).forEachIndexed { index, dx ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(0.82f + dx * 0.35f, 0.97f, -0.60f),
                                V3(0.96f, 1.18f, -0.36f),
                                V3(1.18f + dx * 0.40f, 1.55f, -0.12f),
                                V3(1.38f + dx * 0.20f, 1.11f, 0.16f),
                            ),
                            0.012f, 5, subdivide = 3,
                        ),
                    )
                }
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(1.18f + side * 0.18f, 1.54f, -0.12f),
                                V3(1.28f, 1.25f, 0.18f),
                                V3(1.03f + (if (side > 0) 0.54f else 0f), 0.98f, 0.42f),
                            ),
                            0.010f, 5, subdivide = 3,
                        ),
                    )
                }
            }.build(),
            material = Materials.rubber,
        ),
    )

    val houseXs = listOf(0.96f, 1.54f)
    houseXs.forEachIndexed { index, x ->
        val z = 0.56f
        val suffix = if (index == 0) "a" else "b"
        parts.add(
            Part(
                id = "wg_house_$suffix",
                name = "House ${index + 1}",
                system = WG_HOUSE, assembly = WG_HOMES,
                mesh = houseMesh(),
                material = if (index == 0) Materials.paintCream else Materials.paintSlate,
                origin = V3(x, 0f, z),
                explode = V3(0.35f * (index + 1), 0.25f, 0.45f),
            ),
        )
        parts.add(
            Part(
                id = "wg_house_${suffix}_light",
                name = "House ${index + 1} window",
                system = WG_HOUSE, assembly = WG_HOMES,
                mesh = houseWindowMesh(),
                material = Materials.emitAmber,
                origin = V3(x, 0f, z),
                animate = { ctx ->
                    val pulse = 0.82f + 0.18f * sin(ctx.machineTime * 2.0f + index * 1.7f)
                    Mat4.scale(V3(1f, pulse, 1f))
                },
                explode = V3(0.35f * (index + 1), 0.25f, 0.45f),
            ),
        )
    }

    val hotspots = listOf(
        Hotspot(
            "rotor", "Twelve-blade rotor",
            "The wheel is a low-speed, high-torque rotor. Twelve shallow sails catch a wide annulus of wind, which is why a farm windmill starts in a gentle breeze.",
            "wg_rotor", V3(-0.52f, -0.52f, 0f),
            CameraPose(24f, 12f, 1.28f), readoutLabel = "ROTOR",
        ),
        Hotspot(
            "tail", "Tail vane",
            "The vane keeps the wheel facing the wind. When the wind shifts, the vane swings the whole head around the tower top until the wheel is square to the flow again.",
            "wg_tail", V3(0f, 0.16f, -0.45f),
            CameraPose(66f, 18f, 1.24f), readoutLabel = "YAW",
        ),
        Hotspot(
            "shaft", "Vertical drive shaft",
            "A bevel gear at the top turns the rotor's horizontal shaft into a vertical one. The shaft carries torque down the tower without a chain or a belt in the weather.",
            "wg_vertical_shaft", V3(0f, 0.75f, 0.05f),
            CameraPose(48f, 20f, 1.12f), readoutLabel = "DRIVE",
        ),
        Hotspot(
            "gears", "Base bevel gears",
            "At the bottom, another bevel pair turns the drive through ninety degrees again. The large gear trades speed for torque before the belt reaches the generator.",
            "wg_bottom_gear", V3(0.03f, 0f, 0.08f),
            CameraPose(34f, 26f, 1.26f), readoutLabel = "GEARING",
        ),
        Hotspot(
            "generator", "Permanent-magnet generator",
            "The glass generator shows the rotor and stator. The belt raises the shaft speed, and the magnets induce a current in the copper windings without brushes.",
            "wg_generator_shell", V3(0.05f, 0f, 0.18f),
            CameraPose(28f, 14f, 1.34f), readoutLabel = "GENERATOR",
        ),
        Hotspot(
            "transformer", "Step-up transformer",
            "The transformer raises the voltage for the short distribution run. Higher voltage means less current, so the wires waste less energy as heat.",
            "wg_transformer", V3(0.02f, 0.30f, 0.15f),
            CameraPose(44f, 18f, 1.28f), readoutLabel = "GRID",
        ),
        Hotspot(
            "houses", "Two houses",
            "The houses are the load. Their lights are deliberately warm: the generator is making useful power, not just spinning a pretty wheel.",
            "wg_house_b", V3(0.02f, 0.26f, 0.22f),
            CameraPose(30f, 10f, 1.34f), readoutLabel = "LOAD",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Catch the wind",
            "The twelve sails turn the hub. The tail vane keeps the wheel facing the wind, and the lattice tower holds it above the turbulence of the ground.",
            WG_ROTOR, CameraPose(30f, 16f, 1.06f), 0f, 0.40f,
        ),
        GuideStep(
            "Turn the corner",
            "A bevel gear at the top sends the torque down a vertical shaft. The shaft is supported at several heights so it does not whip under load.",
            WG_DRIVE, CameraPose(44f, 20f, 1.10f), 0.20f, 0.58f,
        ),
        GuideStep(
            "Raise the speed",
            "At the base, another bevel pair and a belt drive raise the speed for the generator. The flywheel smooths the pulses from the twelve sails.",
            WG_DRIVE, CameraPose(30f, 24f, 1.16f), 0.35f, 0.72f,
        ),
        GuideStep(
            "Make electricity",
            "The permanent-magnet rotor turns inside copper stator coils. The glass housing is a teaching cutaway; the real machine is sealed and air-cooled.",
            WG_GENERATOR, CameraPose(26f, 12f, 1.26f), 0.20f, 0.86f,
        ),
        GuideStep(
            "Step up the voltage",
            "The transformer raises the voltage and lowers the current. That is why the thin service wires can carry the whole output without glowing red.",
            WG_GRID, CameraPose(46f, 18f, 1.14f), 0.15f, 0.68f,
        ),
        GuideStep(
            "Light the houses",
            "The pole and service drops split the power to both homes. When the wind drops, the lights dim; when it rises, the meter needle climbs.",
            WG_HOUSE, CameraPose(34f, 12f, 1.20f), 0f, 1f,
        ),
        GuideStep(
            "The assembly",
            "Explode the microgrid to separate the rotor, tower, shaft, power train, generator, transformer and houses. Every cable is a service boundary.",
            null, CameraPose(38f, 24f, 0.98f), 1f, 0.55f,
        ),
    )

    return ObjectSpec(
        id = "wind_grid_system",
        index = 19,
        title = "WINDSPIRE W-1",
        designation = "WG-1 / WIND-TO-GRID",
        category = "Energy",
        categoryShort = "WIND",
        tagline = "A farm windmill that lights two homes.",
        summary = "A cutaway wind-to-grid microgrid: a twelve-blade farm windmill, vertical shaft, bevel gearing, belt-driven generator, transformer, pole and two lit houses.",
        details = listOf(
            "WINDSPIRE W-1 is a small generating system rather than a single machine. The rotor turns at wind speed, the shaft carries torque down the tower, and the base gearing and belt raise that speed for a compact generator.",
            "The generator feeds a step-up transformer, a single distribution pole and two houses. The moving chain is deliberately visible from the sails to the lights, so every conversion stage can be followed.",
        ),
        specs = listOf(
            SpecRow("Rotor", "2.4 m / 12 sails"),
            SpecRow("Tower", "3.0 m lattice"),
            SpecRow("Generator", "5 kW PMG"),
            SpecRow("Wind range", "4 – 18 m/s"),
            SpecRow("Homes", "2 connected"),
        ),
        tags = listOf("wind", "windmill", "generator", "microgrid", "energy", "house", "transformer", "renewable"),
        accent = 0xFFE4B45C.toInt(),
        control = ControlSpec(
            label = "WIND",
            lowLabel = "BREEZE",
            highLabel = "GUST",
            defaultValue = 0.55f,
            hint = "Rotor speed, generator output and house lights",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(34f, 16f, 1.24f),
        thumbnailPose = CameraPose(36f, 18f, 1.26f),
        thumbnailTime = 2.5f,
        thumbnailControl = 0.62f,
        readouts = { ctx ->
            val output = windOutput(ctx)
            val wind = 4f + 14f * ctx.control
            val rpm = 32f + 48f * ctx.control
            listOf(
                Readout("OUTPUT", String.format("%.1f", output), "kW", accent = true),
                Readout("WIND", String.format("%.0f", wind), "m/s", warn = wind > 16f),
                Readout("ROTOR", String.format("%.0f", rpm), "rpm"),
            )
        },
        effects = { ctx ->
            val flow = 0.30f + 0.4f * ctx.control
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.90f, 0xFFE4B45C.toInt(), 0f, 0.20f),
                SceneEffect.Flow(
                    points = listOf(
                        V3(-3.2f, 2.75f, -1.0f),
                        V3(-2.35f, 2.65f, -0.55f),
                        V3(-1.55f, 2.45f, -0.10f),
                        V3(-1.10f, 2.32f, 0.22f),
                    ),
                    color = 0xFFB8F6EC.toInt(),
                    count = 12,
                    speed = flow,
                    phase = 0.12f,
                    dotRadius = 0.024f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(0.54f, 0.66f, -0.27f),
                        V3(0.74f, 0.60f, -0.42f),
                        V3(0.82f, 0.64f, -0.62f),
                    ),
                    color = 0xFFFFC46B.toInt(),
                    count = 7,
                    speed = flow * 1.4f,
                    phase = 0.5f,
                    dotRadius = 0.018f,
                ),
                SceneEffect.Flow(
                    points = listOf(
                        V3(0.82f, 0.96f, -0.60f),
                        V3(1.18f, 1.53f, -0.12f),
                        V3(1.40f, 1.18f, 0.16f),
                        V3(1.15f, 1.02f, 0.42f),
                    ),
                    color = 0xFFFFD37A.toInt(),
                    count = 10,
                    speed = flow,
                    phase = 0.25f,
                    dotRadius = 0.018f,
                ),
                SceneEffect.Glow(V3(0.20f, 0.68f, -0.20f), 0.48f, 0xFFFFC46B.toInt(), 0.24f + 0.22f * ctx.control),
                SceneEffect.Glow(V3(0.82f, 0.60f, -0.64f), 0.26f, 0xFF6EF2DF.toInt(), 0.18f),
                SceneEffect.Glow(V3(1.25f, 0.44f, 0.62f), 0.30f, 0xFFFFB562.toInt(), 0.18f + 0.18f * ctx.control),
            )
        },
    )
}
