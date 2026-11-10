package studio.anatomy.app.objects

import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V2
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
import studio.anatomy.app.render.SurfacePattern
import kotlin.math.cos
import kotlin.math.sin

// Systems: what each part does.
private const val HY_CIVIL = "CIVIL WORKS"
private const val HY_WATERWAY = "WATERWAY"
private const val HY_TURBINE = "TURBINE"
private const val HY_GENERATOR = "GENERATOR"
private const val HY_GOVERNOR = "GOVERNOR"
private const val HY_GRID = "TRANSMISSION"
private const val HY_SERVICE = "SERVICE"
private const val HY_CONTROL = "CONTROL"

// Assemblies: what moves together when the station is exploded.
private const val HY_A_PLINTH = "PLINTH"
private const val HY_A_DAM = "DAM"
private const val HY_A_INTAKE = "INTAKE"
private const val HY_A_PENSTOCK = "PENSTOCK"
private const val HY_A_SCROLL = "SCROLL CASE"
private const val HY_A_DISTRIBUTOR = "DISTRIBUTOR"
private const val HY_A_RUNNER = "RUNNER"
private const val HY_A_DRAFT = "DRAFT TUBE"
private const val HY_A_STATOR = "STATOR"
private const val HY_A_ROTOR = "ROTOR"
private const val HY_A_EXCITER = "EXCITER"
private const val HY_A_HALL = "POWERHOUSE"
private const val HY_A_CRANE = "BRIDGE CRANE"
private const val HY_A_GOVERNOR = "GOVERNOR"
private const val HY_A_TRANSFORMER = "TRANSFORMER"
private const val HY_A_YARD = "SWITCHYARD"

/** Turbine axis in plan, and the main elevations of the unit. */
private const val HY_X = 0.25f
private const val HY_Z = 0.25f
private const val HY_RUN_Y = 1.02f
private const val HY_FLOOR_Y = 1.58f
private const val HY_GEN_Y = 1.98f
private const val HY_TOP = 0.30f

private const val HY_GATES = 16
private const val HY_SPIN = 150f

private const val HY_WATER_COLOR = 0xFF7FD8FF.toInt()
private const val HY_POWER_COLOR = 0xFFFFC56B.toInt()

private val hyConcrete = Material(
    "hydroConcrete", "Mass concrete", 0xFF9EA39C.toInt(), specular = 0.10f, shininess = 8f,
    fillLight = 0.10f, rimLight = 0.12f, patina = 0.050f,
    pattern = SurfacePattern.SPECKLE, patternStrength = 0.30f,
)
private val hyConcreteLight = Material(
    "hydroConcreteLight", "Formed concrete", 0xFFC2C6BD.toInt(), specular = 0.10f, shininess = 8f,
    fillLight = 0.10f, rimLight = 0.12f, patina = 0.040f,
    pattern = SurfacePattern.PANEL, patternStrength = 0.34f,
)
private val hyWater = Material(
    "hydroWater", "Water", 0xFF4FB6E8.toInt(), specular = 0.70f, shininess = 48f,
    alpha = 64, specTint = 0xFFE4FBFF.toInt(),
)
private val hyWaterDeep = Material(
    "hydroWaterDeep", "Reservoir", 0xFF2E86C4.toInt(), specular = 0.66f, shininess = 44f,
    alpha = 58, specTint = 0xFFD8F4FF.toInt(),
)
private val hyCasing = Material(
    "hydroCasing", "Casing enamel", 0xFF2F7C8F.toInt(), specular = 0.44f, shininess = 26f,
    fillLight = 0.10f, rimLight = 0.18f, patina = 0.030f, edgeWear = 0.06f,
)
private val hyGenerator = Material(
    "hydroGenerator", "Generator enamel", 0xFF3567A8.toInt(), specular = 0.42f, shininess = 24f,
    fillLight = 0.10f, rimLight = 0.18f, patina = 0.028f, edgeWear = 0.05f,
    pattern = SurfacePattern.PANEL, patternStrength = 0.30f,
)
private val hyBronze = Material(
    "hydroBronze", "Runner bronze", 0xFFC79A4E.toInt(), specular = 0.66f, shininess = 40f,
    fillLight = 0.10f, rimLight = 0.20f, patina = 0.060f, edgeWear = 0.07f,
)
private val hyCore = Material(
    "hydroCore", "Stator laminations", 0xFF39464C.toInt(), specular = 0.36f, shininess = 20f,
    fillLight = 0.08f, rimLight = 0.14f, patina = 0.035f,
    pattern = SurfacePattern.BRUSHED, patternStrength = 0.36f,
)
private val hyPorcelain = Material(
    "hydroPorcelain", "Glazed porcelain", 0xFFA8573C.toInt(), specular = 0.52f, shininess = 36f,
    fillLight = 0.09f, rimLight = 0.18f, patina = 0.030f,
)
private val hyScreen = Material(
    "hydroScreen", "Console display", 0xFF8CF6E8.toInt(), emissive = true, specular = 0f,
)
private val hyCutEdge = Material(
    "hydroCutEdge", "Section edge", 0xFFFFB562.toInt(), emissive = true, specular = 0f,
)
private val hyLamp = Material(
    "hydroLamp", "Indicator lamp", 0xFFFFD98A.toInt(), emissive = true, specular = 0f,
)

private fun hyDeg(degrees: Float): Double = Math.toRadians(degrees.toDouble())

/** A point on a circle around the turbine axis, at height [y]. */
private fun hyAround(radius: Float, degrees: Float, y: Float): V3 = V3(
    HY_X + radius * cos(hyDeg(degrees)).toFloat(),
    y,
    HY_Z + radius * sin(hyDeg(degrees)).toFloat(),
)

/** A profile drawn in the X/Y plane and extruded along Z, centred on [z]. */
private fun hyExtrudeZ(profile: List<V2>, depth: Float, z: Float): Mesh =
    Geo.prism(profile, depth).transformed(Mat4.translation(V3(0f, 0f, z)).multiply(Mat4.rotateX(-90f)))

private fun hyRingAlong(center: V3, direction: V3, major: Float, minor: Float, segments: Int = 18): Mesh {
    val d = direction.normalized() * 0.001f
    val orient = Geo.transformBetweenY(center - d, center + d)
    return Geo.torus(major, minor, segments, 4).transformed(orient)
}

// ---------------------------------------------------------------- civil works

private val hyDamProfile = listOf(
    V2(-1.75f, HY_TOP), V2(-1.75f, 2.50f), V2(-1.42f, 2.50f), V2(-1.42f, 2.26f), V2(-0.95f, HY_TOP),
)

private fun hyDamMesh(): Mesh = MeshBuilder().apply {
    addMesh(hyExtrudeZ(hyDamProfile, 1.45f, -0.675f))
    // Inspection gallery portals on the sectioned face.
    for (y in listOf(0.78f, 1.40f)) {
        addMesh(Geo.box(0.16f, 0.20f, 0.012f), Mat4.translation(V3(-1.50f, y, 0.056f)))
    }
}.build()

private fun hyCrestMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.42f, 0.035f, 1.47f), Mat4.translation(V3(-1.585f, 2.52f, -0.675f)))
    // Parapets and lamp standards along the crest road.
    for (x in listOf(-1.775f, -1.395f)) {
        addMesh(Geo.box(0.022f, 0.075f, 1.47f), Mat4.translation(V3(x, 2.575f, -0.675f)))
    }
    for (i in 0 until 6) {
        val z = -1.30f + i * 0.25f
        addMesh(Geo.cylinder(0.008f, 0.20f, 6), Mat4.translation(V3(-1.775f, 2.70f, z)))
    }
    // Crane rails.
    for (x in listOf(-1.68f, -1.49f)) {
        addMesh(Geo.box(0.016f, 0.014f, 1.40f), Mat4.translation(V3(x, 2.545f, -0.675f)))
    }
}.build()

private fun hyCrestLampMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 6) {
        val z = -1.30f + i * 0.25f
        addMesh(Geo.sphere(0.018f, 6, 3), Mat4.translation(V3(-1.775f, 2.81f, z)))
    }
}.build()

private fun hySpillwayMesh(): Mesh = MeshBuilder().apply {
    val chute = listOf(V2(-1.42f, 2.30f), V2(-1.385f, 2.31f), V2(-0.80f, 0.42f), V2(-0.62f, 0.40f), V2(-0.62f, HY_TOP), V2(-0.95f, HY_TOP))
    addMesh(hyExtrudeZ(chute, 0.46f, -1.02f))
    for (z in listOf(-1.27f, -0.77f)) {
        val wall = listOf(V2(-1.42f, 2.42f), V2(-1.33f, 2.42f), V2(-0.62f, 0.56f), V2(-0.62f, HY_TOP), V2(-0.95f, HY_TOP))
        addMesh(hyExtrudeZ(wall, 0.04f, z))
    }
    // Flip bucket teeth that break the jet before it reaches the river bed.
    for (i in 0 until 4) {
        addMesh(Geo.box(0.07f, 0.06f, 0.06f), Mat4.translation(V3(-0.66f, 0.45f, -1.17f + i * 0.10f)))
    }
}.build()

private fun hySpillWaterMesh(): Mesh {
    val sheet = listOf(V2(-1.40f, 2.335f), V2(-1.375f, 2.345f), V2(-0.80f, 0.465f), V2(-0.82f, 0.445f))
    return hyExtrudeZ(sheet, 0.40f, -1.02f)
}

private fun hyReservoirMesh(): Mesh = Geo.box(0.64f, 2.02f, 1.90f)
    .transformed(Mat4.translation(V3(-2.07f, HY_TOP + 1.01f, -0.45f)))

private fun hyReservoirBedMesh(): Mesh = MeshBuilder().apply {
    val bed = listOf(V2(-2.40f, HY_TOP), V2(-2.40f, 0.52f), V2(-1.75f, 0.36f), V2(-1.75f, HY_TOP))
    addMesh(hyExtrudeZ(bed, 1.90f, -0.45f))
}.build()

private fun hyIntakeTowerMesh(): Mesh = MeshBuilder().apply {
    for (z in listOf(0.03f, 0.47f)) {
        addMesh(Geo.box(0.07f, 2.42f, 0.06f), Mat4.translation(V3(-1.83f, HY_TOP + 1.21f, z)))
    }
    addMesh(Geo.box(0.16f, 0.07f, 0.56f), Mat4.translation(V3(-1.83f, 2.755f, 0.25f)))
    addMesh(Geo.box(0.22f, 0.02f, 0.62f), Mat4.translation(V3(-1.83f, 2.80f, 0.25f)))
    // Bellmouth where the penstock leaves the reservoir.
    addMesh(
        machineRingX(-1.765f, 0.24f, 0.165f, 0.05f, 20)
            .transformed(Mat4.translation(V3(0f, 1.95f, HY_Z))),
    )
}.build()

private fun hyTrashRackMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 9) {
        addMesh(Geo.box(0.010f, 0.56f, 0.010f), Mat4.translation(V3(-1.93f, 1.95f, 0.05f + i * 0.05f)))
    }
    for (y in listOf(1.70f, 1.95f, 2.20f)) {
        addMesh(Geo.box(0.014f, 0.014f, 0.44f), Mat4.translation(V3(-1.93f, y, 0.25f)))
    }
    // Struts back to the gate slot.
    for (y in listOf(1.70f, 2.20f)) {
        for (z in listOf(0.05f, 0.45f)) {
            addMesh(Geo.box(0.10f, 0.012f, 0.012f), Mat4.translation(V3(-1.88f, y, z)))
        }
    }
}.build()

private fun hyIntakeGateMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.022f, 0.46f, 0.36f))
    for (i in 0 until 4) {
        addMesh(Geo.box(0.032f, 0.022f, 0.36f), Mat4.translation(V3(0f, -0.18f + i * 0.12f, 0f)))
    }
    // Hoist stems rising to the tower deck.
    for (z in listOf(-0.12f, 0.12f)) {
        addMesh(Geo.cylinder(0.008f, 0.60f, 6), Mat4.translation(V3(0f, 0.53f, z)))
    }
}.build()

private fun hyHoistMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.045f, 0.30f, 14).transformed(Mat4.rotateX(90f)))
    for (z in listOf(-0.17f, 0.17f)) {
        addMesh(Geo.cylinder(0.062f, 0.012f, 14).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, z)))
    }
    for (i in 0 until 4) {
        addMesh(
            Geo.box(0.012f, 0.10f, 0.012f),
            Mat4.translation(V3(0f, 0f, 0.185f)).multiply(Mat4.rotateZ(i * 45f)),
        )
    }
}.build()

private fun hyCrestGantryMesh(): Mesh = MeshBuilder().apply {
    for (x in listOf(-0.095f, 0.095f)) {
        for (z in listOf(-0.09f, 0.09f)) {
            addMesh(Geo.box(0.018f, 0.34f, 0.018f), Mat4.translation(V3(x, 0.17f, z)))
        }
        addMesh(Geo.box(0.026f, 0.026f, 0.24f), Mat4.translation(V3(x, 0.012f, 0f)))
    }
    addMesh(Geo.box(0.23f, 0.05f, 0.21f), Mat4.translation(V3(0f, 0.36f, 0f)))
    addMesh(Geo.box(0.10f, 0.06f, 0.10f), Mat4.translation(V3(0.02f, 0.415f, 0f)))
    addMesh(Geo.cylinder(0.006f, 0.20f, 6), Mat4.translation(V3(0f, 0.235f, 0f)))
    addMesh(Geo.box(0.05f, 0.04f, 0.05f), Mat4.translation(V3(0f, 0.125f, 0f)))
}.build()

// ------------------------------------------------------------------- waterway

private val hyPenstockPath = listOf(
    V3(-1.74f, 1.95f, 0.25f),
    V3(-1.42f, 1.95f, 0.25f),
    V3(-1.12f, 1.63f, 0.36f),
    V3(-0.84f, 1.22f, 0.60f),
    V3(-0.56f, 1.04f, 0.75f),
    V3(-0.16f, HY_RUN_Y, 0.775f),
)

private fun hyPenstockMesh(): Mesh = Geo.tube(hyPenstockPath, 0.165f, 12, subdivide = 4, caps = false)

private fun hyPenstockHardwareMesh(): Mesh = MeshBuilder().apply {
    val smooth = Geo.smoothPath(hyPenstockPath, 4)
    // Stiffener rings every few stations along the pipe.
    var index = 2
    while (index < smooth.size - 2) {
        val here = smooth[index]
        val direction = smooth[index + 1] - smooth[index - 1]
        addMesh(hyRingAlong(here, direction, 0.172f, 0.014f, 16))
        index += 3
    }
    // Saddle supports under the inclined run.
    for ((x, y, z) in listOf(Triple(-1.10f, 1.63f, 0.36f), Triple(-0.72f, 1.13f, 0.67f))) {
        val height = y - 0.20f - HY_TOP
        addMesh(Geo.box(0.16f, height, 0.26f), Mat4.translation(V3(x, HY_TOP + height * 0.5f, z)))
        addMesh(Geo.box(0.20f, 0.04f, 0.32f), Mat4.translation(V3(x, HY_TOP + 0.02f, z)))
    }
}.build()

private fun hyInletValveBodyMesh(): Mesh = MeshBuilder().apply {
    val x = -0.36f
    addMesh(machineRingX(x, 0.215f, 0.165f, 0.11f, 20).transformed(Mat4.translation(V3(0f, HY_RUN_Y, 0.765f))))
    for (dx in listOf(-0.075f, 0.075f)) {
        addMesh(machineRingX(x + dx, 0.245f, 0.165f, 0.022f, 20).transformed(Mat4.translation(V3(0f, HY_RUN_Y, 0.765f))))
        addMesh(boltCircleX(6, 0.228f, x + dx, 0.016f, 0.03f).transformed(Mat4.translation(V3(0f, HY_RUN_Y, 0.765f))))
    }
    // Actuator on top of the valve trunnion.
    addMesh(Geo.cylinder(0.022f, 0.20f, 8), Mat4.translation(V3(x, HY_RUN_Y + 0.30f, 0.765f)))
    addMesh(Geo.box(0.12f, 0.09f, 0.12f), Mat4.translation(V3(x, HY_RUN_Y + 0.44f, 0.765f)))
}.build()

private fun hyInletDiscMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.158f, 0.020f, 20, bevel = 0.004f).transformed(Mat4.rotateZ(-90f)))
    addMesh(Geo.cylinder(0.022f, 0.34f, 8))
    addMesh(Geo.box(0.030f, 0.26f, 0.05f))
}.build()

private fun hyScrollRadius(step: Int, steps: Int): Float = 0.165f - 0.112f * step / steps

private fun hyScrollPath(): List<V3> {
    val steps = 13
    return (0..steps).map { step ->
        // The case wraps clockwise from the cut at 25 degrees; its section shrinks as it
        // feeds the distributor, which keeps the water speed even all the way round.
        hyAround(0.365f + hyScrollRadius(step, steps), 25f - step * 20f, HY_RUN_Y)
    }
}

private fun hyScrollMesh(): Mesh = Geo.tube(hyScrollPath(), 0.165f, 12, endRadius = 0.053f, subdivide = 2, caps = false)

private fun hyScrollRibsMesh(): Mesh = MeshBuilder().apply {
    val path = hyScrollPath()
    for (step in 1 until path.size - 1 step 2) {
        val radius = hyScrollRadius(step, path.size - 1)
        addMesh(hyRingAlong(path[step], path[step + 1] - path[step - 1], radius + 0.006f, 0.011f, 14))
    }
    // Anchor pedestals under the casing.
    for (degrees in listOf(-40f, -130f, -210f)) {
        val p = hyAround(0.46f, degrees, 0f)
        addMesh(Geo.box(0.12f, 0.50f, 0.12f), Mat4.translation(V3(p.x, HY_TOP + 0.25f, p.z)))
    }
}.build()

private fun hyCutRingsMesh(): Mesh = MeshBuilder().apply {
    val path = hyScrollPath()
    addMesh(hyRingAlong(path[0], path[0] - path[1], 0.168f, 0.012f, 20))
    addMesh(hyRingAlong(hyPenstockPath.last(), V3.X, 0.168f, 0.012f, 20))
}.build()

private fun hyStayRingMesh(): Mesh = MeshBuilder().apply {
    for (dy in listOf(-0.092f, 0.092f)) {
        addMesh(Geo.torus(0.375f, 0.016f, 28, 4), Mat4.translation(V3(HY_X, HY_RUN_Y + dy, HY_Z)))
    }
    // Fixed stay vanes carry the weight of the generator floor across the water passage.
    for (i in 0 until 8) {
        val degrees = i * 45f + 11f
        val p = hyAround(0.385f, degrees, HY_RUN_Y)
        addMesh(
            Geo.box(0.060f, 0.185f, 0.018f),
            Mat4.translation(p).multiply(Mat4.rotateY(-degrees + 38f)),
        )
    }
    addMesh(Geo.hollowCylinder(0.275f, 0.262f, 0.05f, 30), Mat4.translation(V3(HY_X, HY_RUN_Y - 0.115f, HY_Z)))
}.build()

private fun hyGateMesh(): Mesh = MeshBuilder().apply {
    // A streamlined vane pivoting on a vertical stem; the lever on top meets the gate ring.
    addMesh(Geo.box(0.016f, 0.168f, 0.105f), Mat4.translation(V3(0f, 0f, 0.012f)))
    addMesh(Geo.cylinder(0.011f, 0.34f, 8), Mat4.translation(V3(0f, 0.07f, 0f)))
    addMesh(Geo.box(0.014f, 0.012f, 0.085f), Mat4.translation(V3(0f, 0.235f, 0.036f)))
    addMesh(Geo.cylinder(0.009f, 0.022f, 6), Mat4.translation(V3(0f, 0.245f, 0.072f)))
}.build()

private fun hyGateRingMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.hollowCylinder(0.455f, 0.405f, 0.022f, 36))
    for (i in 0 until HY_GATES) {
        val degrees = i * 360f / HY_GATES
        val a = hyDeg(degrees)
        addMesh(
            Geo.box(0.07f, 0.010f, 0.012f),
            Mat4.translation(V3(0.39f * cos(a).toFloat(), -0.004f, 0.39f * sin(a).toFloat()))
                .multiply(Mat4.rotateY(-degrees)),
        )
    }
    // Lugs where the two servomotors push the ring round.
    for (degrees in listOf(170f, 290f)) {
        val a = hyDeg(degrees)
        addMesh(
            Geo.box(0.06f, 0.03f, 0.05f),
            Mat4.translation(V3(0.47f * cos(a).toFloat(), 0f, 0.47f * sin(a).toFloat())).multiply(Mat4.rotateY(-degrees)),
        )
    }
}.build()

private fun hyHeadCoverMesh(): Mesh = Geo.hollowCylinder(0.405f, 0.075f, 0.010f, 32)
    .transformed(Mat4.translation(V3(HY_X, HY_RUN_Y + 0.105f, HY_Z)))

private fun hyRunnerMesh(): Mesh = MeshBuilder().apply {
    // Crown, band and nose cone of a Francis runner.
    addMesh(Geo.lathe(listOf(0.060f to 0.090f, 0.215f to 0.082f, 0.250f to 0.060f, 0.250f to 0.040f), 28, capTop = false))
    addMesh(Geo.cylinder(0.215f, 0.012f, 28), Mat4.translation(V3(0f, 0.086f, 0f)))
    addMesh(Geo.hollowCylinder(0.258f, 0.240f, 0.046f, 28), Mat4.translation(V3(0f, -0.085f, 0f)))
    addMesh(Geo.lathe(listOf(0.004f to -0.185f, 0.050f to -0.110f, 0.085f to -0.020f, 0.095f to 0.050f), 18, capBottom = true))
    for (i in 0 until 13) {
        val degrees = i * 360f / 13f
        addMesh(
            Geo.roundedBlade(0.150f, 0.150f, 0.105f, 0.020f, 0.010f, 34f, 0.035f, sections = 4, perimeter = 6),
            Mat4.rotateY(degrees)
                .multiply(Mat4.translation(V3(0.165f, 0.050f, 0f)))
                .multiply(Mat4.rotateY(52f))
                .multiply(Mat4.rotateX(180f)),
        )
    }
    for (i in 0 until 5) {
        val a = hyDeg(i * 72f)
        addMesh(Geo.hexBolt(0.016f, 0.020f), Mat4.translation(V3(0.125f * cos(a).toFloat(), 0.100f, 0.125f * sin(a).toFloat())))
    }
}.build()

private fun hyShaftMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.052f, 1.02f, 16), Mat4.translation(V3(0f, 0.60f, 0f)))
    // Bolted coupling between the turbine shaft and the generator shaft.
    for (dy in listOf(0.30f, 0.345f)) {
        addMesh(Geo.cylinder(0.118f, 0.036f, 20, bevel = 0.006f), Mat4.translation(V3(0f, dy, 0f)))
    }
    for (i in 0 until 6) {
        val a = hyDeg(i * 60f)
        addMesh(Geo.hexBolt(0.014f, 0.030f), Mat4.translation(V3(0.094f * cos(a).toFloat(), 0.372f, 0.094f * sin(a).toFloat())))
    }
    // A keyway strip makes the rotation readable on an otherwise plain shaft.
    addMesh(Geo.box(0.014f, 0.40f, 0.014f), Mat4.translation(V3(0.054f, 0.74f, 0f)))
}.build()

private val hyDraftPath = listOf(
    V3(HY_X, HY_RUN_Y - 0.13f, HY_Z),
    V3(HY_X + 0.01f, 0.70f, HY_Z),
    V3(HY_X + 0.30f, 0.585f, HY_Z),
    V3(HY_X + 1.02f, 0.585f, HY_Z),
)

private fun hyDraftMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.tube(hyDraftPath, 0.150f, 12, endRadius = 0.245f, subdivide = 4, caps = false))
    addMesh(hyRingAlong(hyDraftPath.last(), V3.X, 0.250f, 0.016f, 20))
    addMesh(hyRingAlong(hyDraftPath.first(), V3.Y, 0.156f, 0.014f, 18))
}.build()

private fun hyTailraceMesh(): Mesh = MeshBuilder().apply {
    for (z in listOf(-0.36f, 0.98f)) {
        addMesh(Geo.box(1.14f, 0.44f, 0.05f), Mat4.translation(V3(1.84f, HY_TOP + 0.22f, z)))
    }
    addMesh(Geo.box(0.05f, 0.20f, 1.34f), Mat4.translation(V3(2.39f, HY_TOP + 0.10f, 0.31f)))
    // Draft-tube gate frame.
    for (z in listOf(-0.06f, 0.56f)) {
        addMesh(Geo.box(0.05f, 0.86f, 0.05f), Mat4.translation(V3(1.31f, HY_TOP + 0.43f, z)))
    }
    addMesh(Geo.box(0.07f, 0.06f, 0.70f), Mat4.translation(V3(1.31f, HY_TOP + 0.89f, 0.25f)))
}.build()

private fun hyTailWaterMesh(): Mesh = Geo.box(1.06f, 0.30f, 1.28f)
    .transformed(Mat4.translation(V3(1.84f, HY_TOP + 0.15f, 0.31f)))

private fun hyTailGateMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.022f, 0.40f, 0.56f))
    for (i in 0 until 3) {
        addMesh(Geo.box(0.032f, 0.020f, 0.56f), Mat4.translation(V3(0f, -0.13f + i * 0.13f, 0f)))
    }
}.build()

// ------------------------------------------------------------------ generator

private fun hyStatorFrameMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.arcShell(0.715f, 0.600f, 0.46f, 26, 110f, 230f), Mat4.translation(V3(HY_X, HY_GEN_Y, HY_Z)))
    for (dy in listOf(-0.245f, 0.245f)) {
        addMesh(Geo.arcShell(0.750f, 0.590f, 0.035f, 26, 110f, 230f), Mat4.translation(V3(HY_X, HY_GEN_Y + dy, HY_Z)))
    }
    // Air coolers bolted round the frame.
    for (i in 0 until 6) {
        val degrees = 128f + i * 39f
        val p = hyAround(0.775f, degrees, HY_GEN_Y)
        addMesh(Geo.box(0.07f, 0.34f, 0.24f), Mat4.translation(p).multiply(Mat4.rotateY(-degrees)))
        for (k in 0 until 3) {
            val fin = hyAround(0.815f, degrees, HY_GEN_Y - 0.11f + k * 0.11f)
            addMesh(Geo.box(0.018f, 0.012f, 0.25f), Mat4.translation(fin).multiply(Mat4.rotateY(-degrees)))
        }
    }
}.build()

private fun hyStatorCoreMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.arcShell(0.600f, 0.515f, 0.36f, 30, 110f, 230f), Mat4.translation(V3(HY_X, HY_GEN_Y, HY_Z)))
}.build()

private fun hyWindingMesh(): Mesh = MeshBuilder().apply {
    for (dy in listOf(-0.205f, 0.205f)) {
        addMesh(Geo.arcShell(0.590f, 0.520f, 0.050f, 30, 110f, 230f), Mat4.translation(V3(HY_X, HY_GEN_Y + dy, HY_Z)))
    }
    // Individual bar ends give the winding its comb-like rim.
    for (i in 0 until 15) {
        val degrees = 114f + i * 15.8f
        for (dy in listOf(-0.245f, 0.245f)) {
            val p = hyAround(0.555f, degrees, HY_GEN_Y + dy)
            addMesh(Geo.box(0.050f, 0.030f, 0.018f), Mat4.translation(p).multiply(Mat4.rotateY(-degrees)))
        }
    }
}.build()

private fun hyRotorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.hollowCylinder(0.440f, 0.345f, 0.30f, 32))
    addMesh(Geo.cylinder(0.130f, 0.34f, 18, bevel = 0.015f))
    for (i in 0 until 8) {
        val degrees = i * 45f
        addMesh(
            Geo.box(0.225f, 0.24f, 0.030f),
            Mat4.rotateY(-degrees).multiply(Mat4.translation(V3(0.238f, 0f, 0f))),
        )
    }
    // Brake ring under the rim.
    addMesh(Geo.hollowCylinder(0.430f, 0.330f, 0.020f, 32), Mat4.translation(V3(0f, -0.165f, 0f)))
}.build()

private fun hyPoleMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 18) {
        val degrees = i * 20f
        addMesh(
            Geo.box(0.050f, 0.275f, 0.100f),
            Mat4.rotateY(-degrees).multiply(Mat4.translation(V3(0.468f, 0f, 0f))),
        )
    }
}.build()

private fun hyPoleShoeMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 18) {
        val degrees = i * 20f
        addMesh(
            Geo.box(0.014f, 0.290f, 0.124f),
            Mat4.rotateY(-degrees).multiply(Mat4.translation(V3(0.500f, 0f, 0f))),
        )
    }
}.build()

private fun hyBracketMesh(): Mesh = MeshBuilder().apply {
    // Upper bracket: carries the guide bearing and the exciter above the rotor.
    addMesh(Geo.cylinder(0.185f, 0.11f, 20, bevel = 0.015f), Mat4.translation(V3(HY_X, HY_GEN_Y + 0.335f, HY_Z)))
    for (degrees in listOf(135f, 195f, 255f, 315f)) {
        val p = hyAround(0.45f, degrees, HY_GEN_Y + 0.325f)
        addMesh(Geo.box(0.56f, 0.075f, 0.055f), Mat4.translation(p).multiply(Mat4.rotateY(-degrees)))
    }
    // Lower bracket and thrust bearing under the rotor, braced to the pit wall.
    addMesh(Geo.cylinder(0.165f, 0.15f, 20, bevel = 0.015f), Mat4.translation(V3(HY_X, 1.47f, HY_Z)))
    for (degrees in listOf(135f, 195f, 255f, 315f)) {
        val p = hyAround(0.43f, degrees, 1.47f)
        addMesh(Geo.box(0.56f, 0.085f, 0.050f), Mat4.translation(p).multiply(Mat4.rotateY(-degrees)))
    }
    for (i in 0 until 4) {
        val p = hyAround(0.140f, 45f + i * 90f, 1.552f)
        addMesh(Geo.hexBolt(0.015f, 0.022f), Mat4.translation(p))
    }
}.build()

private fun hyExciterHousingMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.arcShell(0.150f, 0.118f, 0.20f, 14, 110f, 230f), Mat4.translation(V3(HY_X, HY_GEN_Y + 0.49f, HY_Z)))
    addMesh(Geo.arcShell(0.170f, 0.110f, 0.022f, 14, 110f, 230f), Mat4.translation(V3(HY_X, HY_GEN_Y + 0.60f, HY_Z)))
}.build()

private fun hySlipRingMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.040f, 0.26f, 12), Mat4.translation(V3(0f, 0.10f, 0f)))
    for (i in 0 until 3) {
        addMesh(Geo.cylinder(0.082f, 0.024f, 18, bevel = 0.004f), Mat4.translation(V3(0f, 0.03f + i * 0.055f, 0f)))
    }
    addMesh(Geo.box(0.18f, 0.014f, 0.022f), Mat4.translation(V3(0f, 0.215f, 0f)))
    addMesh(Geo.cylinder(0.055f, 0.030f, 12), Mat4.translation(V3(0f, 0.240f, 0f)))
}.build()

// ----------------------------------------------------------------- powerhouse

private fun hyPitMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.arcShell(0.830f, 0.735f, 1.24f, 26, 110f, 230f), Mat4.translation(V3(HY_X, HY_TOP + 0.62f, HY_Z)))
    addMesh(Geo.box(2.30f, 1.24f, 0.10f), Mat4.translation(V3(0.20f, HY_TOP + 0.62f, -0.86f)))
}.build()

private fun hyFloorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.arcShell(0.960f, 0.735f, 0.075f, 26, 110f, 230f), Mat4.translation(V3(HY_X, HY_FLOOR_Y - 0.0375f, HY_Z)))
    addMesh(Geo.box(2.30f, 0.070f, 0.30f), Mat4.translation(V3(0.20f, HY_FLOOR_Y - 0.040f, -0.81f)))
}.build()

private fun hyHandrailMesh(): Mesh = MeshBuilder().apply {
    for (i in 0..10) {
        val degrees = 112f + i * 22.6f
        val p = hyAround(0.94f, degrees, HY_FLOOR_Y + 0.085f)
        addMesh(Geo.cylinder(0.007f, 0.17f, 6), Mat4.translation(p))
    }
    addMesh(Geo.arcShell(0.946f, 0.934f, 0.012f, 26, 112f, 226f, capEnds = false), Mat4.translation(V3(HY_X, HY_FLOOR_Y + 0.17f, HY_Z)))
    addMesh(Geo.arcShell(0.946f, 0.934f, 0.008f, 26, 112f, 226f, capEnds = false), Mat4.translation(V3(HY_X, HY_FLOOR_Y + 0.09f, HY_Z)))
}.build()

private const val HY_HALL_X0 = -0.90f
private const val HY_HALL_X1 = 1.30f
private const val HY_HALL_ZB = -0.93f
private const val HY_HALL_ZF = 1.24f
private const val HY_RAIL_Y = 2.90f

private fun hyHallFrameMesh(): Mesh = MeshBuilder().apply {
    for (x in listOf(HY_HALL_X0, HY_HALL_X1)) {
        for (z in listOf(HY_HALL_ZB, HY_HALL_ZF)) {
            val height = HY_RAIL_Y - HY_TOP
            addMesh(Geo.box(0.060f, height, 0.060f), Mat4.translation(V3(x, HY_TOP + height * 0.5f, z)))
            addMesh(Geo.box(0.13f, 0.03f, 0.13f), Mat4.translation(V3(x, HY_TOP + 0.015f, z)))
            // Corbel under the crane rail.
            addMesh(Geo.box(0.10f, 0.10f, 0.10f), Mat4.translation(V3(x, HY_RAIL_Y - 0.09f, z)))
        }
    }
    for (z in listOf(HY_HALL_ZB, HY_HALL_ZF)) {
        addMesh(Geo.box(2.32f, 0.075f, 0.050f), Mat4.translation(V3(0.20f, HY_RAIL_Y, z)))
        addMesh(Geo.box(2.32f, 0.016f, 0.085f), Mat4.translation(V3(0.20f, HY_RAIL_Y + 0.045f, z)))
    }
    // Tie beams across the two gable ends.
    for (x in listOf(HY_HALL_X0, HY_HALL_X1)) {
        addMesh(Geo.box(0.045f, 0.055f, 2.17f), Mat4.translation(V3(x, HY_RAIL_Y + 0.11f, 0.155f)))
    }
}.build()

private fun hyGlazingFrameMesh(): Mesh = MeshBuilder().apply {
    for (i in 0..6) {
        val x = HY_HALL_X0 + 0.06f + i * 0.347f
        addMesh(Geo.box(0.018f, 1.22f, 0.018f), Mat4.translation(V3(x, 2.22f, HY_HALL_ZB)))
    }
    for (y in listOf(1.63f, 2.03f, 2.43f, 2.83f)) {
        addMesh(Geo.box(2.10f, 0.018f, 0.018f), Mat4.translation(V3(0.20f, y, HY_HALL_ZB)))
    }
}.build()

private fun hyGlazingMesh(): Mesh = Geo.box(2.08f, 1.18f, 0.006f)
    .transformed(Mat4.translation(V3(0.20f, 2.23f, HY_HALL_ZB - 0.012f)))

private fun hyBridgeMesh(): Mesh = MeshBuilder().apply {
    val span = HY_HALL_ZF - HY_HALL_ZB
    for (x in listOf(-0.075f, 0.075f)) {
        addMesh(Geo.box(0.040f, 0.11f, span), Mat4.translation(V3(x, 0.10f, 0f)))
        addMesh(Geo.box(0.060f, 0.014f, span), Mat4.translation(V3(x, 0.162f, 0f)))
    }
    for (z in listOf(-span * 0.5f, span * 0.5f)) {
        addMesh(Geo.box(0.30f, 0.07f, 0.075f), Mat4.translation(V3(0f, 0.075f, z)))
        for (x in listOf(-0.11f, 0.11f)) {
            addMesh(Geo.cylinder(0.030f, 0.030f, 10).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(x, 0.055f, z)))
        }
    }
    // Walkway and maintenance cab.
    addMesh(Geo.box(0.030f, 0.012f, span * 0.92f), Mat4.translation(V3(0.125f, 0.05f, 0f)))
    addMesh(Geo.box(0.11f, 0.12f, 0.16f), Mat4.translation(V3(0.02f, -0.02f, -span * 0.5f + 0.20f)))
}.build()

private fun hyTrolleyMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.22f, 0.050f, 0.20f), Mat4.translation(V3(0f, 0.195f, 0f)))
    addMesh(Geo.cylinder(0.045f, 0.16f, 12).transformed(Mat4.rotateZ(-90f)), Mat4.translation(V3(0f, 0.245f, 0f)))
    addMesh(Geo.box(0.07f, 0.07f, 0.09f), Mat4.translation(V3(0.07f, 0.245f, 0.07f)))
    for (x in listOf(-0.025f, 0.025f)) {
        addMesh(Geo.cylinder(0.004f, 0.36f, 5), Mat4.translation(V3(x, -0.01f, 0f)))
    }
}.build()

private fun hyHookMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(0.085f, 0.075f, 0.055f))
    addMesh(Geo.cylinder(0.032f, 0.060f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0.01f, 0f)))
    addMesh(Geo.cylinder(0.010f, 0.050f, 6), Mat4.translation(V3(0f, -0.06f, 0f)))
    addMesh(Geo.arcShell(0.034f, 0.020f, 0.016f, 10, 180f, 230f).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, -0.115f, 0f)))
}.build()

// ------------------------------------------------------- governor and control

private const val HY_GOV_X = 1.02f
private const val HY_GOV_Z = -0.80f

private fun hyGovernorMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.34f, 0.20f, 0.40f, cornerRadius = 0.02f, bevel = 0.012f), Mat4.translation(V3(HY_GOV_X, HY_FLOOR_Y + 0.20f, HY_GOV_Z)))
    // Sump tank and pump set beside the cabinet.
    addMesh(Geo.slab(0.22f, 0.18f, 0.16f, cornerRadius = 0.02f, bevel = 0.010f), Mat4.translation(V3(HY_GOV_X - 0.32f, HY_FLOOR_Y + 0.08f, HY_GOV_Z)))
    addMesh(
        Geo.cylinder(0.040f, 0.14f, 12).transformed(Mat4.rotateZ(-90f)),
        Mat4.translation(V3(HY_GOV_X - 0.32f, HY_FLOOR_Y + 0.205f, HY_GOV_Z)),
    )
    for (i in 0 until 3) {
        addMesh(Geo.box(0.26f, 0.008f, 0.006f), Mat4.translation(V3(HY_GOV_X, HY_FLOOR_Y + 0.10f + i * 0.045f, HY_GOV_Z + 0.103f)))
    }
}.build()

private fun hyAccumulatorMesh(): Mesh = MeshBuilder().apply {
    for (dx in listOf(-0.075f, 0.075f)) {
        val x = HY_GOV_X + 0.30f + dx
        addMesh(Geo.cylinder(0.052f, 0.40f, 14), Mat4.translation(V3(x, HY_FLOOR_Y + 0.23f, HY_GOV_Z)))
        addMesh(Geo.sphere(0.052f, 12, 4), Mat4.translation(V3(x, HY_FLOOR_Y + 0.43f, HY_GOV_Z)))
        addMesh(Geo.cylinder(0.062f, 0.03f, 14), Mat4.translation(V3(x, HY_FLOOR_Y + 0.015f, HY_GOV_Z)))
        addMesh(Geo.torus(0.053f, 0.006f, 14, 4), Mat4.translation(V3(x, HY_FLOOR_Y + 0.26f, HY_GOV_Z)))
    }
}.build()

private fun hyFlyballMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.007f, 0.16f, 6), Mat4.translation(V3(0f, 0.08f, 0f)))
    addMesh(Geo.cylinder(0.018f, 0.014f, 10), Mat4.translation(V3(0f, 0.155f, 0f)))
    for (side in listOf(-1f, 1f)) {
        addMesh(
            Geo.box(0.070f, 0.007f, 0.007f),
            Mat4.translation(V3(side * 0.030f, 0.125f, 0f)).multiply(Mat4.rotateZ(side * 38f)),
        )
        addMesh(Geo.sphere(0.021f, 10, 5), Mat4.translation(V3(side * 0.062f, 0.098f, 0f)))
        addMesh(
            Geo.box(0.060f, 0.006f, 0.006f),
            Mat4.translation(V3(side * 0.034f, 0.070f, 0f)).multiply(Mat4.rotateZ(side * -40f)),
        )
    }
    addMesh(Geo.cylinder(0.016f, 0.020f, 10), Mat4.translation(V3(0f, 0.045f, 0f)))
}.build()

private fun hyOilLinesMesh(): Mesh = MeshBuilder().apply {
    val servoA = hyAround(0.62f, 290f, HY_RUN_Y + 0.185f)
    val servoB = hyAround(0.62f, 170f, HY_RUN_Y + 0.185f)
    for ((offset, target) in listOf(0.03f to servoA, -0.03f to servoB)) {
        addMesh(
            Geo.tube(
                listOf(
                    V3(HY_GOV_X + offset, HY_FLOOR_Y + 0.02f, HY_GOV_Z + 0.06f),
                    V3(HY_GOV_X + offset, HY_FLOOR_Y - 0.14f, HY_GOV_Z + 0.16f),
                    V3((HY_GOV_X + target.x) * 0.5f, HY_RUN_Y + 0.34f, (HY_GOV_Z + target.z) * 0.5f - 0.10f),
                    V3(target.x, target.y + 0.03f, target.z),
                ),
                0.012f, 6, subdivide = 3,
            ),
        )
    }
}.build()

private fun hyServoBodyMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.034f, 0.17f, 12).transformed(Mat4.rotateX(90f)))
    for (z in listOf(-0.085f, 0.085f)) {
        addMesh(Geo.cylinder(0.042f, 0.014f, 12).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, z)))
    }
    addMesh(Geo.box(0.05f, 0.11f, 0.06f), Mat4.translation(V3(0f, -0.085f, 0f)))
}.build()

private fun hyServoRodMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.013f, 0.20f, 8).transformed(Mat4.rotateX(90f)), Mat4.translation(V3(0f, 0f, 0.11f)))
    addMesh(Geo.box(0.030f, 0.030f, 0.034f), Mat4.translation(V3(0f, 0f, 0.215f)))
}.build()

private const val HY_DESK_X = -0.52f
private const val HY_DESK_Z = -0.80f

private fun hyConsoleMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.56f, 0.18f, 0.20f, cornerRadius = 0.02f, bevel = 0.010f), Mat4.translation(V3(HY_DESK_X, HY_FLOOR_Y + 0.10f, HY_DESK_Z)))
    addMesh(
        Geo.box(0.56f, 0.014f, 0.15f),
        Mat4.translation(V3(HY_DESK_X, HY_FLOOR_Y + 0.225f, HY_DESK_Z + 0.03f)).multiply(Mat4.rotateX(18f)),
    )
    // Mimic panel behind the desk.
    addMesh(Geo.box(0.62f, 0.34f, 0.022f), Mat4.translation(V3(HY_DESK_X, HY_FLOOR_Y + 0.46f, HY_DESK_Z - 0.085f)))
}.build()

private fun hyConsoleScreenMesh(): Mesh = MeshBuilder().apply {
    for (i in 0 until 3) {
        addMesh(
            Geo.box(0.15f, 0.006f, 0.085f),
            Mat4.translation(V3(HY_DESK_X - 0.18f + i * 0.18f, HY_FLOOR_Y + 0.236f, HY_DESK_Z + 0.03f)).multiply(Mat4.rotateX(18f)),
        )
    }
    // One-line diagram of the unit on the mimic panel.
    addMesh(Geo.box(0.50f, 0.008f, 0.006f), Mat4.translation(V3(HY_DESK_X, HY_FLOOR_Y + 0.52f, HY_DESK_Z - 0.071f)))
    for (i in 0 until 4) {
        addMesh(Geo.box(0.008f, 0.12f, 0.006f), Mat4.translation(V3(HY_DESK_X - 0.21f + i * 0.14f, HY_FLOOR_Y + 0.46f, HY_DESK_Z - 0.071f)))
        addMesh(Geo.box(0.050f, 0.050f, 0.006f), Mat4.translation(V3(HY_DESK_X - 0.21f + i * 0.14f, HY_FLOOR_Y + 0.38f, HY_DESK_Z - 0.071f)))
    }
}.build()

// --------------------------------------------------------------- transmission

private const val HY_TX_X = 1.70f
private const val HY_TX_Z = -0.95f
private const val HY_PAD_Y = 0.74f

private fun hyPadMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.box(1.02f, HY_PAD_Y - HY_TOP, 0.96f), Mat4.translation(V3(1.90f, (HY_PAD_Y + HY_TOP) * 0.5f, -0.95f)))
    // Oil bund round the transformer.
    for (z in listOf(-1.25f, -0.65f)) {
        addMesh(Geo.box(0.62f, 0.035f, 0.025f), Mat4.translation(V3(HY_TX_X, HY_PAD_Y + 0.017f, z)))
    }
    for (x in listOf(HY_TX_X - 0.31f, HY_TX_X + 0.31f)) {
        addMesh(Geo.box(0.025f, 0.035f, 0.62f), Mat4.translation(V3(x, HY_PAD_Y + 0.017f, -0.95f)))
    }
}.build()

private fun hyTransformerMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.slab(0.44f, 0.30f, 0.40f, cornerRadius = 0.03f, bevel = 0.015f), Mat4.translation(V3(HY_TX_X, HY_PAD_Y + 0.23f, HY_TX_Z)))
    addMesh(Geo.box(0.48f, 0.022f, 0.34f), Mat4.translation(V3(HY_TX_X, HY_PAD_Y + 0.44f, HY_TX_Z)))
    for (x in listOf(-0.16f, 0.16f)) {
        addMesh(Geo.box(0.05f, 0.03f, 0.36f), Mat4.translation(V3(HY_TX_X + x, HY_PAD_Y + 0.015f, HY_TX_Z)))
    }
    // Conservator on its two brackets, with the breather pipe.
    addMesh(
        Geo.cylinder(0.060f, 0.40f, 14, bevel = 0.012f).transformed(Mat4.rotateZ(-90f)),
        Mat4.translation(V3(HY_TX_X, HY_PAD_Y + 0.60f, HY_TX_Z - 0.13f)),
    )
    for (x in listOf(-0.14f, 0.14f)) {
        addMesh(Geo.box(0.018f, 0.12f, 0.018f), Mat4.translation(V3(HY_TX_X + x, HY_PAD_Y + 0.50f, HY_TX_Z - 0.13f)))
    }
    // Tap changer on the low-voltage side.
    addMesh(Geo.box(0.07f, 0.24f, 0.16f), Mat4.translation(V3(HY_TX_X - 0.25f, HY_PAD_Y + 0.26f, HY_TX_Z)))
}.build()

private fun hyRadiatorMesh(): Mesh = MeshBuilder().apply {
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until 8) {
            addMesh(
                Geo.box(0.022f, 0.30f, 0.10f),
                Mat4.translation(V3(HY_TX_X - 0.175f + i * 0.05f, HY_PAD_Y + 0.23f, HY_TX_Z + side * 0.215f)),
            )
        }
        addMesh(Geo.box(0.40f, 0.022f, 0.022f), Mat4.translation(V3(HY_TX_X, HY_PAD_Y + 0.39f, HY_TX_Z + side * 0.175f)))
        addMesh(Geo.box(0.40f, 0.022f, 0.022f), Mat4.translation(V3(HY_TX_X, HY_PAD_Y + 0.07f, HY_TX_Z + side * 0.175f)))
    }
}.build()

private fun hyFanMesh(): Mesh = MeshBuilder().apply {
    addMesh(Geo.cylinder(0.018f, 0.020f, 10).transformed(Mat4.rotateX(90f)))
    for (i in 0 until 5) {
        addMesh(
            Geo.box(0.060f, 0.020f, 0.006f),
            Mat4.rotateZ(i * 72f).multiply(Mat4.translation(V3(0.040f, 0f, 0f))).multiply(Mat4.rotateX(24f)),
        )
    }
}.build()

private fun hyFanGuardMesh(): Mesh = MeshBuilder().apply {
    for (dx in listOf(-0.10f, 0.10f)) {
        addMesh(
            Geo.torus(0.078f, 0.007f, 18, 4).transformed(Mat4.rotateX(90f)),
            Mat4.translation(V3(HY_TX_X + dx, HY_PAD_Y + 0.23f, HY_TX_Z + 0.275f)),
        )
    }
}.build()

private val hyBushingX = listOf(HY_TX_X - 0.13f, HY_TX_X, HY_TX_X + 0.13f)
private const val HY_BUSHING_Z = HY_TX_Z + 0.06f
private const val HY_BUSHING_BASE = HY_PAD_Y + 0.45f
private const val HY_BUSHING_TOP = HY_BUSHING_BASE + 0.31f

private fun hyBushingMesh(): Mesh = MeshBuilder().apply {
    val profile = ArrayList<Pair<Float, Float>>()
    profile.add(0.030f to 0f)
    for (i in 0 until 6) {
        val y = 0.02f + i * 0.044f
        val shed = 0.046f - i * 0.003f
        profile.add(shed to y)
        profile.add(0.020f to y + 0.020f)
    }
    profile.add(0.014f to 0.30f)
    val bushing = Geo.lathe(profile, 9, capBottom = true, capTop = true)
    for (x in hyBushingX) {
        addMesh(bushing, Mat4.translation(V3(x, HY_BUSHING_BASE, HY_BUSHING_Z)))
    }
}.build()

private fun hyTerminalMesh(): Mesh = MeshBuilder().apply {
    for (x in hyBushingX) {
        addMesh(Geo.sphere(0.020f, 10, 5), Mat4.translation(V3(x, HY_BUSHING_TOP, HY_BUSHING_Z)))
    }
}.build()

private fun hyBusPaths(): List<List<V3>> = listOf(-0.05f, 0f, 0.05f).map { offset ->
    listOf(
        V3(HY_X + 0.40f, HY_GEN_Y - 0.10f + offset, HY_Z - 0.62f),
        V3(HY_X + 0.74f, HY_GEN_Y - 0.12f + offset, HY_Z - 0.86f),
        V3(1.26f, 1.66f + offset, HY_TX_Z + 0.02f + offset),
        V3(HY_TX_X - 0.30f, HY_PAD_Y + 0.34f + offset, HY_TX_Z + offset),
    )
}

private fun hyBusMesh(): Mesh = MeshBuilder().apply {
    hyBusPaths().forEach { path -> addMesh(Geo.tube(path, 0.015f, 6, subdivide = 3)) }
}.build()

private const val HY_TOWER_X = 2.16f
private const val HY_TOWER_Z = -0.98f
private const val HY_TOWER_TOP = 2.86f

private val hyArmLevels = listOf(2.28f, 2.54f, 2.80f)
private val hyArmSides = listOf(1f, -1f, 1f)

private fun hyInsulatorTip(phase: Int): V3 =
    V3(HY_TOWER_X, hyArmLevels[phase] - 0.15f, HY_TOWER_Z + hyArmSides[phase] * 0.33f)

private fun hyTowerMesh(): Mesh = MeshBuilder().apply {
    val height = HY_TOWER_TOP - HY_PAD_Y
    fun corner(level: Float, sx: Float, sz: Float): V3 {
        val t = (level - HY_PAD_Y) / height
        val half = 0.150f - 0.118f * t
        return V3(HY_TOWER_X + sx * half, level, HY_TOWER_Z + sz * half)
    }
    val signs = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
    signs.forEach { (sx, sz) ->
        addMesh(Geo.cylinderBetween(corner(HY_PAD_Y, sx, sz), corner(HY_TOWER_TOP, sx, sz), 0.011f, 5))
        addMesh(Geo.box(0.05f, 0.02f, 0.05f), Mat4.translation(corner(HY_PAD_Y + 0.01f, sx, sz)))
    }
    // Lattice bracing: one diagonal and one girt per panel, on all four faces.
    val panels = 7
    for (panel in 0 until panels) {
        val low = HY_PAD_Y + height * panel / panels
        val high = HY_PAD_Y + height * (panel + 1) / panels
        for (face in 0 until 4) {
            val (ax, az) = signs[face]
            val (bx, bz) = signs[(face + 1) % 4]
            val flip = (panel + face) % 2 == 0
            val from = if (flip) corner(low, ax, az) else corner(low, bx, bz)
            val to = if (flip) corner(high, bx, bz) else corner(high, ax, az)
            addMesh(Geo.cylinderBetween(from, to, 0.006f, 4))
            addMesh(Geo.cylinderBetween(corner(high, ax, az), corner(high, bx, bz), 0.006f, 4))
        }
    }
    // Cross-arms, one per phase, alternating sides.
    for (phase in 0 until 3) {
        val y = hyArmLevels[phase]
        val side = hyArmSides[phase]
        val tip = V3(HY_TOWER_X, y, HY_TOWER_Z + side * 0.33f)
        addMesh(Geo.cylinderBetween(V3(HY_TOWER_X, y, HY_TOWER_Z), tip, 0.009f, 5))
        addMesh(Geo.cylinderBetween(V3(HY_TOWER_X, y + 0.10f, HY_TOWER_Z), tip, 0.006f, 4))
    }
    addMesh(Geo.cylinder(0.006f, 0.12f, 5), Mat4.translation(V3(HY_TOWER_X, HY_TOWER_TOP + 0.06f, HY_TOWER_Z)))
}.build()

private fun hyInsulatorMesh(): Mesh = MeshBuilder().apply {
    for (phase in 0 until 3) {
        val tip = hyInsulatorTip(phase)
        for (i in 0 until 4) {
            addMesh(Geo.cylinder(0.024f, 0.012f, 8), Mat4.translation(V3(tip.x, tip.y + 0.024f + i * 0.033f, tip.z)))
        }
        addMesh(Geo.cylinder(0.005f, 0.15f, 5), Mat4.translation(V3(tip.x, tip.y + 0.075f, tip.z)))
    }
}.build()

private fun hyLinePaths(): List<List<V3>> = (0 until 3).map { phase ->
    val terminal = V3(hyBushingX[phase], HY_BUSHING_TOP, HY_BUSHING_Z)
    val tip = hyInsulatorTip(phase)
    val middle = V3((terminal.x + tip.x) * 0.5f, (terminal.y + tip.y) * 0.5f - 0.14f, (terminal.z + tip.z) * 0.5f)
    listOf(terminal, middle, tip, V3(tip.x + 0.16f, tip.y - 0.035f, tip.z), V3(2.47f, tip.y - 0.02f, tip.z))
}

private fun hyLineMesh(): Mesh = MeshBuilder().apply {
    hyLinePaths().forEach { path -> addMesh(Geo.tube(path, 0.007f, 5, subdivide = 4)) }
}.build()

private fun hyBreakerMesh(): Mesh = MeshBuilder().apply {
    // A three-pole disconnector between the transformer and the tower.
    val x = 2.16f
    val z = -0.62f
    addMesh(Geo.box(0.30f, 0.03f, 0.05f), Mat4.translation(V3(x, HY_PAD_Y + 0.30f, z)))
    for (dx in listOf(-0.12f, 0.12f)) {
        addMesh(Geo.box(0.022f, 0.30f, 0.022f), Mat4.translation(V3(x + dx, HY_PAD_Y + 0.15f, z)))
    }
    for (dx in listOf(-0.10f, 0f, 0.10f)) {
        addMesh(Geo.cylinder(0.014f, 0.09f, 8), Mat4.translation(V3(x + dx, HY_PAD_Y + 0.36f, z)))
    }
}.build()

private fun hyBladeMesh(): Mesh = MeshBuilder().apply {
    for (dx in listOf(-0.10f, 0f, 0.10f)) {
        addMesh(Geo.box(0.008f, 0.012f, 0.13f), Mat4.translation(V3(dx, 0f, 0.065f)))
    }
    addMesh(Geo.box(0.23f, 0.010f, 0.010f), Mat4.translation(V3(0f, 0f, 0.10f)))
}.build()

// -------------------------------------------------------------------- station

private val hyExplode: Map<String, V3> = mapOf(
    HY_A_PLINTH to V3.ZERO,
    HY_A_DAM to V3(-1.25f, 0f, -0.35f),
    HY_A_INTAKE to V3(-1.55f, 0.55f, 0.75f),
    HY_A_PENSTOCK to V3(-0.75f, 0.45f, 1.05f),
    HY_A_SCROLL to V3(0.10f, 0f, 1.30f),
    HY_A_DISTRIBUTOR to V3(0f, 0.34f, 0f),
    HY_A_RUNNER to V3(0f, 0f, 0f),
    HY_A_DRAFT to V3(0.95f, 0f, 0.95f),
    HY_A_STATOR to V3(0f, 1.55f, 0f),
    HY_A_ROTOR to V3(0f, 0.92f, 0f),
    HY_A_EXCITER to V3(0f, 2.15f, 0f),
    HY_A_HALL to V3(0f, 0f, -1.10f),
    HY_A_CRANE to V3(0f, 0.85f, -1.10f),
    HY_A_GOVERNOR to V3(0.55f, 0.55f, -1.45f),
    HY_A_TRANSFORMER to V3(1.25f, 0f, -0.70f),
    HY_A_YARD to V3(1.85f, 0.35f, -0.95f),
)

private fun hySpin(ctx: AnimContext): Mat4 = Mat4.rotateY(ctx.machineTime * HY_SPIN)

/** Wicket gate angle away from the closed, tangential position. */
private fun hyGateAngle(control: Float): Float = 6f + 46f * control.coerceIn(0f, 1f)

fun hydroStation(): ObjectSpec {
    val parts = ArrayList<Part>()

    fun add(
        id: String,
        name: String,
        system: String,
        assembly: String,
        mesh: Mesh,
        material: Material,
        origin: V3 = V3.ZERO,
        rotation: Mat4 = Mat4.identity(),
        parent: String? = null,
        doubleSided: Boolean = false,
        animate: ((AnimContext) -> Mat4)? = null,
    ) {
        parts.add(
            Part(
                id = id, name = name, system = system, assembly = assembly, mesh = mesh, material = material,
                origin = origin, rotation = rotation, parent = parent,
                explode = hyExplode[assembly] ?: V3.ZERO,
                animate = animate, doubleSided = doubleSided,
            ),
        )
    }

    parts.addAll(
        displayBase(
            prefix = "hydro",
            width = 5.10f,
            depth = 3.10f,
            top = HY_TOP,
            system = HY_CIVIL,
            assembly = HY_A_PLINTH,
            accentMaterial = Materials.emitCyan,
        ),
    )

    // Dam, reservoir and spillway.
    add("hy_dam", "Gravity dam section", HY_CIVIL, HY_A_DAM, hyDamMesh(), hyConcrete)
    add("hy_crest", "Crest road and parapets", HY_CIVIL, HY_A_DAM, hyCrestMesh(), hyConcreteLight)
    add("hy_crest_lamps", "Crest lamps", HY_CIVIL, HY_A_DAM, hyCrestLampMesh(), hyLamp)
    add("hy_spillway", "Spillway chute", HY_CIVIL, HY_A_DAM, hySpillwayMesh(), hyConcreteLight)
    add("hy_spill_water", "Spillway flow", HY_WATERWAY, HY_A_DAM, hySpillWaterMesh(), hyWater, doubleSided = true)
    add("hy_reservoir_bed", "Reservoir bed", HY_CIVIL, HY_A_DAM, hyReservoirBedMesh(), hyConcrete)
    add("hy_reservoir", "Reservoir", HY_WATERWAY, HY_A_DAM, hyReservoirMesh(), hyWaterDeep, doubleSided = true)
    add(
        "hy_crest_gantry", "Crest gantry crane", HY_SERVICE, HY_A_DAM, hyCrestGantryMesh(), Materials.paintOrange,
        origin = V3(-1.585f, 2.552f, -0.62f),
        animate = { ctx -> Mat4.translation(V3(0f, 0f, 0.52f * sin(ctx.machineTime * 0.22f))) },
    )

    // Intake works.
    add("hy_intake_tower", "Intake tower and bellmouth", HY_CIVIL, HY_A_INTAKE, hyIntakeTowerMesh(), hyConcreteLight)
    add("hy_trash_rack", "Trash rack", HY_WATERWAY, HY_A_INTAKE, hyTrashRackMesh(), Materials.brushedSteel)
    add(
        "hy_intake_gate", "Intake gate", HY_WATERWAY, HY_A_INTAKE, hyIntakeGateMesh(), Materials.paintSlate,
        origin = V3(-1.795f, 1.95f, 0.25f),
        animate = { ctx -> Mat4.translation(V3(0f, 0.10f + 0.40f * ctx.control.coerceIn(0f, 1f), 0f)) },
    )
    add(
        "hy_intake_hoist", "Gate hoist drum", HY_SERVICE, HY_A_INTAKE, hyHoistMesh(), Materials.darkSteel,
        origin = V3(-1.83f, 2.875f, 0.25f),
        animate = { ctx -> Mat4.rotateZ(ctx.control.coerceIn(0f, 1f) * 540f) },
    )

    // Penstock and main inlet valve.
    add("hy_penstock", "Penstock", HY_WATERWAY, HY_A_PENSTOCK, hyPenstockMesh(), hyCasing, doubleSided = true)
    add("hy_penstock_rings", "Stiffeners and saddles", HY_WATERWAY, HY_A_PENSTOCK, hyPenstockHardwareMesh(), Materials.darkSteel)
    add("hy_inlet_valve", "Main inlet valve body", HY_WATERWAY, HY_A_PENSTOCK, hyInletValveBodyMesh(), Materials.brushedSteel)
    add(
        "hy_inlet_disc", "Butterfly disc", HY_WATERWAY, HY_A_PENSTOCK, hyInletDiscMesh(), Materials.nickel,
        origin = V3(-0.36f, HY_RUN_Y, 0.765f),
        animate = { ctx -> Mat4.rotateY(90f * ctx.control.coerceIn(0f, 1f) - 90f) },
    )

    // Spiral case: sectioned where it would hide the runner.
    add("hy_scroll", "Spiral case", HY_TURBINE, HY_A_SCROLL, hyScrollMesh(), hyCasing, doubleSided = true)
    add("hy_scroll_ribs", "Case ribs and pedestals", HY_TURBINE, HY_A_SCROLL, hyScrollRibsMesh(), Materials.darkSteel)
    add("hy_cut_rings", "Section edges", HY_TURBINE, HY_A_SCROLL, hyCutRingsMesh(), hyCutEdge)

    // Distributor: stay ring, sixteen wicket gates, gate ring and servomotors.
    add("hy_stay_ring", "Stay ring and vanes", HY_TURBINE, HY_A_DISTRIBUTOR, hyStayRingMesh(), Materials.brushedSteel)
    add("hy_head_cover", "Head cover sight glass", HY_TURBINE, HY_A_DISTRIBUTOR, hyHeadCoverMesh(), Materials.glass, doubleSided = true)
    val gateMesh = hyGateMesh()
    for (i in 0 until HY_GATES) {
        val degrees = i * 360f / HY_GATES
        add(
            "hy_gate_$i", "Wicket gate ${i + 1}", HY_GOVERNOR, HY_A_DISTRIBUTOR, gateMesh, Materials.nickel,
            origin = hyAround(0.318f, degrees, HY_RUN_Y),
            rotation = Mat4.rotateY(-degrees),
            animate = { ctx -> Mat4.rotateY(-hyGateAngle(ctx.control)) },
        )
    }
    add(
        "hy_gate_ring", "Gate operating ring", HY_GOVERNOR, HY_A_DISTRIBUTOR, hyGateRingMesh(), Materials.paintOrange,
        origin = V3(HY_X, HY_RUN_Y + 0.255f, HY_Z),
        animate = { ctx -> Mat4.rotateY(-(hyGateAngle(ctx.control) - 29f) * 0.30f) },
    )
    for ((index, degrees) in listOf(290f, 170f).withIndex()) {
        val anchor = hyAround(0.62f, degrees, HY_RUN_Y + 0.255f)
        val facing = Mat4.rotateY(-degrees - 180f + 90f)
        add(
            "hy_servo_$index", "Servomotor ${index + 1}", HY_GOVERNOR, HY_A_DISTRIBUTOR, hyServoBodyMesh(), Materials.darkSteel,
            origin = anchor, rotation = facing,
        )
        add(
            "hy_servo_rod_$index", "Servomotor rod ${index + 1}", HY_GOVERNOR, HY_A_DISTRIBUTOR, hyServoRodMesh(), Materials.nickel,
            origin = anchor, rotation = facing,
            animate = { ctx -> Mat4.translation(V3(0f, 0f, 0.055f * (ctx.control.coerceIn(0f, 1f) - 0.5f))) },
        )
    }

    // Rotating line: runner, shaft, rotor, slip rings.
    add(
        "hy_runner", "Francis runner", HY_TURBINE, HY_A_RUNNER, hyRunnerMesh(), hyBronze,
        origin = V3(HY_X, HY_RUN_Y, HY_Z), animate = ::hySpin,
    )
    add(
        "hy_shaft", "Main shaft and coupling", HY_TURBINE, HY_A_RUNNER, hyShaftMesh(), Materials.nickel,
        origin = V3(HY_X, HY_RUN_Y, HY_Z), animate = ::hySpin,
    )
    add("hy_draft_tube", "Draft tube", HY_WATERWAY, HY_A_DRAFT, hyDraftMesh(), Materials.paintSlate, doubleSided = true)
    add("hy_tailrace", "Tailrace walls and gate frame", HY_CIVIL, HY_A_DRAFT, hyTailraceMesh(), hyConcreteLight)
    add("hy_tail_water", "Tailwater", HY_WATERWAY, HY_A_DRAFT, hyTailWaterMesh(), hyWater, doubleSided = true)
    add(
        "hy_tail_gate", "Draft tube gate", HY_WATERWAY, HY_A_DRAFT, hyTailGateMesh(), Materials.paintSlate,
        origin = V3(1.345f, HY_TOP + 0.62f, 0.25f),
        animate = { ctx -> Mat4.translation(V3(0f, 0.04f * sin(ctx.machineTime * 0.35f), 0f)) },
    )

    add("hy_stator_frame", "Stator frame and coolers", HY_GENERATOR, HY_A_STATOR, hyStatorFrameMesh(), hyGenerator)
    add("hy_stator_core", "Stator core", HY_GENERATOR, HY_A_STATOR, hyStatorCoreMesh(), hyCore)
    add("hy_windings", "Stator windings", HY_GENERATOR, HY_A_STATOR, hyWindingMesh(), Materials.copperBright)
    add(
        "hy_rotor", "Rotor spider and rim", HY_GENERATOR, HY_A_ROTOR, hyRotorMesh(), Materials.darkSteel,
        origin = V3(HY_X, HY_GEN_Y, HY_Z), animate = ::hySpin,
    )
    add(
        "hy_poles", "Field poles", HY_GENERATOR, HY_A_ROTOR, hyPoleMesh(), Materials.copper,
        origin = V3(HY_X, HY_GEN_Y, HY_Z), animate = ::hySpin,
    )
    add(
        "hy_pole_shoes", "Pole shoes", HY_GENERATOR, HY_A_ROTOR, hyPoleShoeMesh(), Materials.brushedSteel,
        origin = V3(HY_X, HY_GEN_Y, HY_Z), animate = ::hySpin,
    )
    add("hy_brackets", "Bearing brackets", HY_GENERATOR, HY_A_ROTOR, hyBracketMesh(), hyGenerator)
    add("hy_exciter", "Exciter housing", HY_GENERATOR, HY_A_EXCITER, hyExciterHousingMesh(), hyGenerator)
    add(
        "hy_slip_rings", "Slip rings", HY_GENERATOR, HY_A_EXCITER, hySlipRingMesh(), Materials.copperBright,
        origin = V3(HY_X, HY_GEN_Y + 0.40f, HY_Z), animate = ::hySpin,
    )
    add(
        "hy_beacon", "Unit running lamp", HY_CONTROL, HY_A_EXCITER,
        Geo.sphere(0.026f, 10, 5).transformed(Mat4.translation(V3(HY_X, HY_GEN_Y + 0.685f, HY_Z))), hyLamp,
    )

    // Powerhouse structure and bridge crane.
    add("hy_pit", "Turbine pit wall", HY_CIVIL, HY_A_HALL, hyPitMesh(), hyConcrete)
    add("hy_floor", "Generator floor", HY_CIVIL, HY_A_HALL, hyFloorMesh(), hyConcreteLight)
    add("hy_handrail", "Floor handrail", HY_CIVIL, HY_A_HALL, hyHandrailMesh(), Materials.paintOrange)
    add("hy_hall_frame", "Hall columns and crane rails", HY_CIVIL, HY_A_HALL, hyHallFrameMesh(), Materials.paintSlate)
    add("hy_glazing_frame", "Hall glazing bars", HY_CIVIL, HY_A_HALL, hyGlazingFrameMesh(), Materials.darkSteel)
    add("hy_glazing", "Hall glazing", HY_CIVIL, HY_A_HALL, hyGlazingMesh(), Materials.glassDark, doubleSided = true)
    add(
        "hy_bridge", "Bridge crane girders", HY_SERVICE, HY_A_CRANE, hyBridgeMesh(), Materials.paintOrange,
        origin = V3(0.20f, HY_RAIL_Y + 0.02f, (HY_HALL_ZB + HY_HALL_ZF) * 0.5f),
        animate = { ctx -> Mat4.translation(V3(0.62f * sin(ctx.machineTime * 0.26f), 0f, 0f)) },
    )
    add(
        "hy_trolley", "Crane trolley and ropes", HY_SERVICE, HY_A_CRANE, hyTrolleyMesh(), Materials.darkSteel,
        parent = "hy_bridge",
        animate = { ctx -> Mat4.translation(V3(0f, 0f, 0.48f * sin(ctx.machineTime * 0.41f + 0.8f))) },
    )
    add(
        "hy_hook", "Hook block", HY_SERVICE, HY_A_CRANE, hyHookMesh(), Materials.paintOrange,
        parent = "hy_trolley", origin = V3(0f, -0.20f, 0f),
        animate = { ctx -> Mat4.translation(V3(0f, 0.07f * sin(ctx.machineTime * 0.63f), 0f)) },
    )

    // Governor and control desk.
    add("hy_governor", "Governor cabinet and pump set", HY_GOVERNOR, HY_A_GOVERNOR, hyGovernorMesh(), Materials.graphite)
    add("hy_accumulators", "Pressure accumulators", HY_GOVERNOR, HY_A_GOVERNOR, hyAccumulatorMesh(), Materials.accentPaint)
    add(
        "hy_flyball", "Speed-sensing flyballs", HY_GOVERNOR, HY_A_GOVERNOR, hyFlyballMesh(), Materials.brass,
        origin = V3(HY_GOV_X, HY_FLOOR_Y + 0.40f, HY_GOV_Z),
        animate = { ctx -> Mat4.rotateY(ctx.machineTime * 310f) },
    )
    add("hy_oil_lines", "Governor oil lines", HY_GOVERNOR, HY_A_GOVERNOR, hyOilLinesMesh(), Materials.copper)
    add("hy_console", "Control desk and mimic panel", HY_CONTROL, HY_A_GOVERNOR, hyConsoleMesh(), Materials.graphite)
    add("hy_console_screens", "Desk displays", HY_CONTROL, HY_A_GOVERNOR, hyConsoleScreenMesh(), hyScreen)
    add(
        "hy_console_scan", "Synchroscope sweep", HY_CONTROL, HY_A_GOVERNOR,
        Geo.box(0.010f, 0.14f, 0.006f).transformed(Mat4.translation(V3(HY_DESK_X, HY_FLOOR_Y + 0.46f, HY_DESK_Z - 0.068f))), hyLamp,
        animate = { ctx -> Mat4.translation(V3(0.25f * sin(ctx.machineTime * 1.7f), 0f, 0f)) },
    )

    // Step-up transformer.
    add("hy_pad", "Transformer pad and bund", HY_CIVIL, HY_A_TRANSFORMER, hyPadMesh(), hyConcreteLight)
    add("hy_transformer", "Step-up transformer tank", HY_GRID, HY_A_TRANSFORMER, hyTransformerMesh(), Materials.paintSlate)
    add("hy_radiators", "Oil radiators", HY_GRID, HY_A_TRANSFORMER, hyRadiatorMesh(), Materials.brushedSteel)
    add("hy_fan_guards", "Fan guards", HY_GRID, HY_A_TRANSFORMER, hyFanGuardMesh(), Materials.darkSteel)
    for ((index, dx) in listOf(-0.10f, 0.10f).withIndex()) {
        add(
            "hy_fan_$index", "Cooling fan ${index + 1}", HY_GRID, HY_A_TRANSFORMER, hyFanMesh(), Materials.nickel,
            origin = V3(HY_TX_X + dx, HY_PAD_Y + 0.23f, HY_TX_Z + 0.275f),
            animate = { ctx -> Mat4.rotateZ(ctx.machineTime * (420f + 380f * ctx.control.coerceIn(0f, 1f))) },
        )
    }
    add("hy_bushings", "High-voltage bushings", HY_GRID, HY_A_TRANSFORMER, hyBushingMesh(), hyPorcelain)
    add("hy_terminals", "Bushing terminals", HY_GRID, HY_A_TRANSFORMER, hyTerminalMesh(), Materials.nickel)
    add("hy_busbars", "Generator busbars", HY_GRID, HY_A_TRANSFORMER, hyBusMesh(), Materials.copperBright)

    // Switchyard.
    add("hy_tower", "Lattice tower", HY_GRID, HY_A_YARD, hyTowerMesh(), Materials.brushedSteel)
    add("hy_insulators", "Insulator strings", HY_GRID, HY_A_YARD, hyInsulatorMesh(), hyPorcelain)
    add("hy_lines", "Phase conductors", HY_GRID, HY_A_YARD, hyLineMesh(), Materials.titanium)
    add("hy_breaker", "Disconnector frame", HY_GRID, HY_A_YARD, hyBreakerMesh(), Materials.paintSlate)
    add(
        "hy_breaker_blades", "Disconnector blades", HY_GRID, HY_A_YARD, hyBladeMesh(), Materials.copperBright,
        origin = V3(2.16f, HY_PAD_Y + 0.41f, -0.62f),
        animate = { ctx -> Mat4.rotateX(-62f * (1f - ctx.control.coerceIn(0f, 1f)) * (0.5f + 0.5f * sin(ctx.machineTime * 0.3f))) },
    )
    add(
        "hy_tower_lamp", "Aircraft warning lamp", HY_GRID, HY_A_YARD,
        Geo.sphere(0.022f, 8, 4).transformed(Mat4.translation(V3(HY_TOWER_X, HY_TOWER_TOP + 0.13f, HY_TOWER_Z))),
        Materials.emitRed,
    )

    val hotspots = listOf(
        Hotspot(
            "intake", "Intake and trash rack",
            "Water leaves the reservoir well below the surface, where the pressure is high and the level never drops out of reach. The rack keeps logs and debris out, and the gate can shut the whole unit off from the lake.",
            "hy_trash_rack", V3(-1.93f, 1.95f, 0.25f),
            CameraPose(118f, 16f, 1.20f), readoutLabel = "INTAKE",
        ),
        Hotspot(
            "penstock", "Penstock",
            "A steel pipe carries the water down the dam. Every metre it falls adds pressure, so by the bottom the pipe holds the full head of the reservoir. Stiffener rings stop it buckling and the inlet valve can isolate the turbine.",
            "hy_penstock", V3(-1.00f, 1.45f, 0.46f),
            CameraPose(64f, 14f, 1.24f), readoutLabel = "NET HEAD",
        ),
        Hotspot(
            "scroll", "Spiral case",
            "The case wraps the turbine like a snail shell and narrows as it goes. Because water keeps leaving it into the runner, the shrinking section holds the speed constant, so every part of the runner receives the same push.",
            "hy_scroll", V3(HY_X + 0.52f, HY_RUN_Y, HY_Z - 0.10f),
            CameraPose(42f, 22f, 1.34f), readoutLabel = "SPIRAL CASE",
        ),
        Hotspot(
            "gates", "Wicket gates",
            "Sixteen vanes turn together on their stems. Nearly closed they throttle the unit; opened they admit more water and steer it into the runner at the right angle. One ring, pushed by two servomotors, moves them all at once.",
            "hy_gate_ring", V3(0.42f, 0f, 0.12f),
            CameraPose(34f, 34f, 1.50f), readoutLabel = "GATE OPENING",
        ),
        Hotspot(
            "runner", "Francis runner",
            "Water enters the runner from the outside edge, turns through the curved blades and leaves downward through the centre. Changing its direction is what pushes the blades round; the pressure and the speed are both spent here.",
            "hy_runner", V3(0.20f, 0.02f, 0.14f),
            CameraPose(30f, 20f, 1.62f), readoutLabel = "RUNNER",
        ),
        Hotspot(
            "draft", "Draft tube",
            "The widening tube slows the water gently after the runner. Slowing it recovers pressure that would otherwise be thrown away as speed, which in effect pulls on the runner from below and adds several percent to the output.",
            "hy_draft_tube", V3(HY_X + 0.72f, 0.64f, HY_Z + 0.20f),
            CameraPose(28f, 12f, 1.36f), readoutLabel = "DRAFT HEAD",
        ),
        Hotspot(
            "generator", "Generator",
            "The shaft turns a ring of electromagnets inside a stationary ring of copper bars. Each pole sweeping past a bar pushes current through it; eighteen poles at this speed give exactly fifty cycles a second.",
            "hy_stator_core", V3(HY_X - 0.50f, HY_GEN_Y, HY_Z + 0.22f),
            CameraPose(44f, 20f, 1.34f), readoutLabel = "GENERATOR",
        ),
        Hotspot(
            "governor", "Governor",
            "When the grid asks for more power the unit starts to slow. The governor senses that drop in speed and uses oil pressure to open the wicket gates until speed is back, holding the frequency steady without anyone touching it.",
            "hy_governor", V3(HY_GOV_X, HY_FLOOR_Y + 0.30f, HY_GOV_Z + 0.10f),
            CameraPose(22f, 16f, 1.50f), readoutLabel = "GOVERNOR",
        ),
        Hotspot(
            "transformer", "Step-up transformer",
            "The generator makes heavy current at modest voltage. The transformer trades one for the other: raising the voltage many times cuts the current by the same factor, and with it the heat lost in hundreds of kilometres of line.",
            "hy_transformer", V3(HY_TX_X, HY_PAD_Y + 0.30f, HY_TX_Z + 0.16f),
            CameraPose(18f, 16f, 1.46f), readoutLabel = "STEP-UP",
        ),
        Hotspot(
            "crane", "Bridge crane",
            "Everything in the hall is built to be lifted out. The crane runs the length of the building on rails and can raise the whole rotor through the floor, which is why the roof stands so far above the machine.",
            "hy_bridge", V3(0f, 0.12f, 0.50f),
            CameraPose(46f, 10f, 1.24f), readoutLabel = "SERVICE CRANE",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Store the height",
            "The dam does not make energy; it stores it. Holding the river back raises the water, and that height above the turbine is the head the station can spend.",
            HY_CIVIL, CameraPose(96f, 18f, 1.00f), 0f, 0.55f,
        ),
        GuideStep(
            "Take it in",
            "Water enters through the trash rack and past the intake gate. From here on it is inside a closed pipe, so the pressure of the whole lake stands behind it.",
            HY_WATERWAY, CameraPose(112f, 16f, 1.18f), 0f, 0.60f,
        ),
        GuideStep(
            "Fall through the penstock",
            "Down the penstock the water gains pressure as it loses height. Follow the particles: they speed up only where the pipe narrows, at the spiral case.",
            HY_WATERWAY, CameraPose(62f, 14f, 1.20f), 0f, 0.70f,
        ),
        GuideStep(
            "Wrap and aim",
            "The spiral case feeds the water evenly round the turbine and the wicket gates aim it. Move the load control and watch all sixteen gates swing together.",
            HY_GOVERNOR, CameraPose(36f, 32f, 1.48f), 0f, 0.85f,
        ),
        GuideStep(
            "Turn the runner",
            "Inside the runner the water is turned from inward to downward. The blades take that change of direction as torque and pass it up the shaft.",
            HY_TURBINE, CameraPose(30f, 20f, 1.58f), 0f, 0.80f,
        ),
        GuideStep(
            "Make electricity",
            "The same shaft spins the rotor poles past the stator bars. No part touches another across the air gap; the energy crosses as a moving magnetic field.",
            HY_GENERATOR, CameraPose(44f, 20f, 1.32f), 0f, 0.80f,
        ),
        GuideStep(
            "Hold the speed",
            "Load on the grid changes every second. The governor answers by moving the gates, so the unit keeps turning at one fixed speed and the frequency stays put.",
            HY_GOVERNOR, CameraPose(22f, 16f, 1.44f), 0f, 0.45f,
        ),
        GuideStep(
            "Send it away",
            "Busbars carry the output to the transformer, which raises the voltage for the line. The water, its work done, slows in the draft tube and rejoins the river.",
            HY_GRID, CameraPose(16f, 16f, 1.30f), 0f, 0.90f,
        ),
        GuideStep(
            "The whole station",
            "Explode the station to see the order it is built in: dam and intake, penstock, spiral case, distributor, runner, rotor, stator and exciter, with the hall and yard around them.",
            null, CameraPose(40f, 20f, 0.88f), 1f, 0.60f,
        ),
    )

    return ObjectSpec(
        id = "hydro_station",
        index = 29,
        title = "TORRENT H-12",
        designation = "TH-12 / FRANCIS HYDRO STATION",
        category = "Energy",
        categoryShort = "ENG",
        tagline = "A falling river, turned into current.",
        summary = "A sectioned hydroelectric station: gravity dam and spillway, intake tower, penstock with inlet valve, spiral case, sixteen governed wicket gates, a Francis runner on a vertical shaft, an eighteen-pole generator, bridge crane, governor, step-up transformer and switchyard.",
        details = listOf(
            "TORRENT H-12 follows one litre of water from the reservoir to the river. The dam stores it high; the penstock turns that height into pressure; the spiral case and wicket gates aim it; the runner takes its energy as rotation; and the draft tube recovers what speed is left.",
            "Above the turbine the same shaft carries the generator rotor. The governor watches the speed of that shaft and trims the gates with oil pressure, so the station follows the load on the grid without changing frequency. The transformer and switchyard then raise the voltage for the journey out.",
        ),
        specs = listOf(
            SpecRow("Rated output", "12 MW"),
            SpecRow("Net head", "86 m"),
            SpecRow("Rated flow", "16 m³/s"),
            SpecRow("Speed", "333 rpm, 18 poles"),
            SpecRow("Turbine", "Vertical Francis, 13 blades"),
            SpecRow("Line voltage", "11 kV / 150 kV"),
        ),
        tags = listOf(
            "energy", "hydro", "hydroelectric", "dam", "turbine", "francis", "generator", "penstock",
            "transformer", "grid", "renewable", "water", "power station", "plta",
        ),
        accent = 0xFF7FD8FF.toInt(),
        control = ControlSpec(
            label = "LOAD",
            lowLabel = "SPEED NO LOAD",
            highLabel = "FULL GATE",
            defaultValue = 0.62f,
            hint = "Wicket gates, inlet valve, flow, output and transformer cooling",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(46f, 22f, 1.16f),
        thumbnailPose = CameraPose(46f, 22f, 1.14f),
        thumbnailTime = 2.6f,
        thumbnailControl = 0.75f,
        focusOffset = V3(0f, 0.04f, 0f),
        readouts = { ctx ->
            val load = ctx.control.coerceIn(0f, 1f)
            val power = 0.4f + 11.6f * load
            val flow = 2.4f + 13.6f * load
            val gate = 100f * (hyGateAngle(load) - 6f) / 46f
            listOf(
                Readout("OUTPUT", String.format("%.1f", power), "MW", accent = true),
                Readout("FLOW", String.format("%.1f", flow), "m³/s"),
                Readout("GATES", String.format("%.0f", gate), "%"),
            )
        },
        effects = { ctx ->
            val load = ctx.control.coerceIn(0f, 1f)
            val flowSpeed = 0.07f + 0.22f * load
            val scroll = hyScrollPath()
            val headrace = listOf(V3(-2.30f, 2.05f, 0.25f), V3(-1.95f, 1.96f, 0.25f)) + hyPenstockPath +
                listOf(hyAround(0.53f, 90f, HY_RUN_Y), hyAround(0.53f, 58f, HY_RUN_Y)) + scroll.take(4)
            val wrap = scroll.drop(3)
            val swirl = (0..18).map { index ->
                val t = index / 18f
                hyAround(0.34f - 0.20f * t, 40f - 330f * t, HY_RUN_Y - 0.02f - 0.06f * t)
            }
            val tail = hyDraftPath + listOf(V3(1.55f, 0.50f, 0.25f), V3(2.30f, 0.52f, 0.25f))
            val spill = listOf(V3(-1.40f, 2.36f, -1.02f), V3(-1.10f, 1.42f, -1.02f), V3(-0.80f, 0.50f, -1.02f), V3(-0.52f, 0.52f, -1.02f))
            val pulse = 0.5f + 0.5f * sin(ctx.machineTime * 3.1f)
            val effects = ArrayList<SceneEffect>()
            effects.add(SceneEffect.Ring(V3(0f, HY_TOP + 0.01f, 0f), 2.55f, HY_WATER_COLOR, 0f, 0.10f))
            effects.add(SceneEffect.Flow(headrace, HY_WATER_COLOR, count = 30, speed = flowSpeed, phase = 0.04f, dotRadius = 0.024f))
            effects.add(SceneEffect.Flow(wrap, HY_WATER_COLOR, count = 16, speed = flowSpeed * 1.5f, phase = 0.31f, dotRadius = 0.018f))
            effects.add(SceneEffect.Flow(swirl, 0xFFB8ECFF.toInt(), count = 14, speed = flowSpeed * 2.2f, phase = 0.52f, dotRadius = 0.014f))
            effects.add(SceneEffect.Flow(tail, HY_WATER_COLOR, count = 16, speed = flowSpeed * 0.9f, phase = 0.18f, dotRadius = 0.026f))
            effects.add(SceneEffect.Flow(spill, 0xFFCFF2FF.toInt(), count = 9, speed = 0.20f, phase = 0.67f, dotRadius = 0.018f))
            hyBusPaths().forEachIndexed { index, path ->
                effects.add(
                    SceneEffect.Flow(path, HY_POWER_COLOR, count = 6, speed = 0.30f + 0.50f * load, phase = index * 0.33f, dotRadius = 0.013f),
                )
            }
            hyLinePaths().forEachIndexed { index, path ->
                effects.add(
                    SceneEffect.Flow(path, HY_POWER_COLOR, count = 7, speed = 0.34f + 0.56f * load, phase = 0.11f + index * 0.29f, dotRadius = 0.011f),
                )
                effects.add(SceneEffect.Spark(path.first(), HY_POWER_COLOR, 0.016f, (0.25f + 0.55f * load) * (0.6f + 0.4f * pulse)))
            }
            effects.add(SceneEffect.Glow(V3(HY_X, HY_RUN_Y, HY_Z), 0.46f, HY_WATER_COLOR, 0.10f + 0.20f * load))
            effects.add(SceneEffect.Glow(V3(HY_X, HY_GEN_Y, HY_Z), 0.62f, HY_POWER_COLOR, 0.05f + 0.17f * load))
            effects.add(SceneEffect.Glow(V3(HY_TX_X, HY_BUSHING_TOP, HY_BUSHING_Z), 0.24f, HY_POWER_COLOR, 0.06f + 0.14f * load * pulse))
            effects
        },
        descriptionTitle = "From head to current",
    )
}
