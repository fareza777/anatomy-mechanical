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

private const val PL_FRAME = "BODY FRAME"
private const val PL_CIRC = "CIRCULATION"
private const val PL_RESP = "RESPIRATION"
private const val PL_CTRL = "CONTROL"

private const val PL_PLINTH = "PLINTH"
private const val PL_ARCH = "ARCH"
private const val PL_HEART = "HEART"
private const val PL_LUNG = "LUNGS"
private const val PL_AIR = "AIRWAY"
private const val PL_VESSEL = "VESSELS"
private const val PL_CAP = "CAPILLARY BED"

private val pulseArtery = Material(
    "pulseArtery", "Oxygen-rich blood", 0xFFC85B59.toInt(), specular = 0.36f, shininess = 22f,
)
private val pulseVein = Material(
    "pulseVein", "Oxygen-poor blood", 0xFF50749E.toInt(), specular = 0.32f, shininess = 20f,
)
private val pulseLung = Material(
    "pulseLung", "Lung tissue", 0xFFDCAEB2.toInt(), specular = 0.18f, shininess = 14f,
)
private val pulseLungDeep = Material(
    "pulseLungDeep", "Lung hilum", 0xFFA96D75.toInt(), specular = 0.22f, shininess = 16f,
)
private val pulseBloodGlow = 0xFFFF9B8A.toInt()
private val pulseBlueGlow = 0xFF8CB6FF.toInt()

private fun ellipsoidPulse(rx: Float, ry: Float, rz: Float, segments: Int = 22, rings: Int = 11): Mesh =
    Geo.sphere(1f, segments, rings).transformed(Mat4.scale(V3(rx, ry, rz)))

private fun cardiacBeat(ctx: AnimContext): Float {
    val rate = 0.82f + 1.25f * ctx.control
    val phase = ctx.machineTime * 6.4f * rate
    val thump = ((sin(phase) + 1f) * 0.5f)
    val echo = ((sin(phase - 0.75f) + 1f) * 0.5f)
    return (thump * thump * thump * 0.82f + echo * echo * 0.18f).coerceIn(0f, 1f)
}

private fun breath(ctx: AnimContext): Float {
    val cycle = sin(ctx.machineTime * 1.15f + 0.8f)
    return ((cycle + 1f) * 0.5f).coerceIn(0f, 1f)
}

private fun lungMesh(side: Float): Mesh = MeshBuilder().apply {
    // Two overlapping lobes give the lung a soft, asymmetric silhouette instead
    // of a single egg. The left side is slightly smaller to leave room for the heart.
    addMesh(
        ellipsoidPulse(if (side < 0f) 0.235f else 0.250f, 0.390f, 0.205f, 24, 12),
        Mat4.translation(V3(side * 0.025f, 0.02f, 0.015f)).multiply(Mat4.rotateZ(side * -3f)),
    )
    addMesh(
        ellipsoidPulse(0.190f, 0.285f, 0.180f, 22, 11),
        Mat4.translation(V3(side * 0.085f, 0.385f, -0.010f)).multiply(Mat4.rotateZ(side * 7f)),
    )
    addMesh(
        ellipsoidPulse(0.135f, 0.135f, 0.125f, 18, 9),
        Mat4.translation(V3(side * 0.035f, 0.555f, 0.000f)),
    )
    // The hilum is the darker root where airways and vessels enter.
    addMesh(
        ellipsoidPulse(0.095f, 0.120f, 0.085f, 18, 9),
        Mat4.translation(V3(side * -0.075f, 0.225f, -0.105f)),
    )
}.build()

private fun ventricleMesh(rx: Float, ry: Float, rz: Float): Mesh = MeshBuilder().apply {
    addMesh(ellipsoidPulse(rx, ry, rz, 28, 14))
    // Fine coronal bands read as muscle fibres and also break up the specular highlight.
    for (band in -2..2) {
        val y = band * 0.052f
        val taper = 1f - kotlin.math.abs(band) * 0.075f
        addMesh(
            Geo.torus(rx * taper, 0.006f, 22, 4).transformed(Mat4.scale(V3(1f, 1f, rz / rx))),
            Mat4.translation(V3(0f, y, 0f)),
        )
    }
    addMesh(
        Geo.taperedCylinder(0.012f, rx * 0.64f, ry * 0.30f, 20),
        Mat4.translation(V3(0f, -ry * 0.94f, 0f)),
    )
}.build()

fun pulseSystem(): ObjectSpec {
    val parts = ArrayList<Part>()
    parts.addAll(
        displayBase(
            prefix = "pulse",
            width = 2.42f,
            depth = 1.80f,
            top = 0.30f,
            system = PL_FRAME,
            assembly = PL_PLINTH,
            accentMaterial = Materials.emitCyan,
        ),
    )

    // A thin rear arch turns the organs into an exhibit rather than a floating pile.
    parts.add(
        Part(
            id = "pl_arch", name = "Thoracic display arch", system = PL_FRAME, assembly = PL_ARCH,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.torus(1.04f, 0.032f, 48, 6).transformed(Mat4.rotateX(90f)),
                    Mat4.translation(V3(0f, 1.34f, -0.36f)),
                )
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.cylinderBetween(
                            V3(side * 1.03f, 0.30f, -0.36f),
                            V3(side * 1.03f, 1.34f, -0.36f),
                            0.022f, 8,
                        ),
                    )
                    addMesh(Geo.hexBolt(0.045f, 0.035f), Mat4.translation(V3(side * 1.03f, 0.34f, -0.36f)))
                }
            }.build(),
            material = Materials.darkSteel,
        ),
    )
    parts.add(
        Part(
            id = "pl_arch_markers", name = "Arch field markers", system = PL_FRAME, assembly = PL_ARCH,
            mesh = MeshBuilder().apply {
                for (i in 0 until 3) {
                    addMesh(
                        Geo.box(0.070f, 0.024f, 0.032f),
                        Mat4.rotateZ(i * 120f).multiply(Mat4.translation(V3(1.04f, 0f, 0.05f))),
                    )
                }
            }.build(),
            material = Materials.emitCyan,
            origin = V3(0f, 1.34f, -0.31f),
            animate = { ctx -> Mat4.rotateZ(ctx.machineTime * 16f) },
        ),
    )

    val leftLung = lungMesh(-1f)
    val rightLung = lungMesh(1f)
    parts.add(
        Part(
            id = "pl_lung_left", name = "Left lung", system = PL_RESP, assembly = PL_LUNG,
            mesh = leftLung, material = pulseLung,
            origin = V3(-0.57f, 1.60f, 0.00f),
            animate = { ctx ->
                val b = breath(ctx)
                Mat4.scale(V3(1f + 0.028f * b, 1f + 0.055f * b, 1f + 0.028f * b))
            },
            explode = V3(-0.26f, 0.16f, 0.10f),
        ),
    )
    parts.add(
        Part(
            id = "pl_lung_right", name = "Right lung", system = PL_RESP, assembly = PL_LUNG,
            mesh = rightLung, material = pulseLung,
            origin = V3(0.57f, 1.60f, 0.00f),
            animate = { ctx ->
                val b = breath(ctx)
                Mat4.scale(V3(1f + 0.028f * b, 1f + 0.055f * b, 1f + 0.028f * b))
            },
            explode = V3(0.26f, 0.16f, 0.10f),
        ),
    )

    // Airway tree: trachea, two primary bronchi, and small branches on the lung surface.
    parts.add(
        Part(
            id = "pl_airways", name = "Trachea and bronchial tree", system = PL_RESP, assembly = PL_AIR,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(V3(0f, 2.34f, 0.04f), V3(0f, 2.16f, 0.02f), V3(0f, 1.98f, 0.01f)),
                        0.052f, 10, subdivide = 3,
                    ),
                )
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(0f, 1.98f, 0.01f),
                                V3(side * 0.20f, 1.90f, -0.01f),
                                V3(side * 0.44f, 1.80f, -0.02f),
                                V3(side * 0.56f, 1.67f, -0.01f),
                            ),
                            0.036f, 8, endRadius = 0.026f, subdivide = 4,
                        ),
                    )
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(side * 0.43f, 1.80f, -0.02f),
                                V3(side * 0.52f, 1.72f, 0.06f),
                                V3(side * 0.63f, 1.62f, 0.08f),
                            ),
                            0.018f, 6, endRadius = 0.011f, subdivide = 3,
                        ),
                    )
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(side * 0.46f, 1.78f, -0.01f),
                                V3(side * 0.50f, 1.57f, -0.01f),
                                V3(side * 0.56f, 1.43f, 0.02f),
                            ),
                            0.016f, 6, endRadius = 0.010f, subdivide = 3,
                        ),
                    )
                }
            }.build(),
            material = Materials.nickel,
            explode = V3(0f, 0.30f, -0.16f),
        ),
    )
    parts.add(
        Part(
            id = "pl_airway_rings", name = "Tracheal cartilage rings", system = PL_RESP, assembly = PL_AIR,
            mesh = MeshBuilder().apply {
                for (i in 0 until 5) {
                    addMesh(
                        Geo.torus(0.058f, 0.009f, 18, 4),
                        Mat4.translation(V3(0f, 2.28f - i * 0.075f, 0.03f)),
                    )
                }
            }.build(),
            material = Materials.paintCream,
            explode = V3(0f, 0.30f, -0.16f),
        ),
    )

    // Heart: two ventricles, two atria, a septum and four valve rings.
    val heartbeatLeft = { ctx: AnimContext ->
        val b = cardiacBeat(ctx)
        Mat4.scale(V3(1f + 0.045f * b, 1f + 0.035f * b, 1f + 0.045f * b))
    }
    val heartbeatRight = { ctx: AnimContext ->
        val b = cardiacBeat(ctx)
        Mat4.scale(V3(1f + 0.038f * b, 1f + 0.030f * b, 1f + 0.038f * b))
    }
    parts.add(
        Part(
            id = "pl_heart_left", name = "Left ventricle", system = PL_CIRC, assembly = PL_HEART,
            mesh = ventricleMesh(0.175f, 0.265f, 0.155f), material = pulseArtery,
            origin = V3(-0.095f, 1.15f, 0.105f), animate = heartbeatLeft,
            explode = V3(-0.16f, 0f, 0.38f),
        ),
    )
    parts.add(
        Part(
            id = "pl_heart_right", name = "Right ventricle", system = PL_CIRC, assembly = PL_HEART,
            mesh = ventricleMesh(0.155f, 0.235f, 0.140f), material = pulseVein,
            origin = V3(0.135f, 1.17f, 0.080f), animate = heartbeatRight,
            explode = V3(0.16f, 0f, 0.42f),
        ),
    )
    parts.add(
        Part(
            id = "pl_atrium_left", name = "Left atrium", system = PL_CIRC, assembly = PL_HEART,
            mesh = MeshBuilder().apply {
                addMesh(ellipsoidPulse(0.125f, 0.095f, 0.105f, 22, 11))
                addMesh(Geo.torus(0.125f, 0.008f, 20, 4), Mat4.translation(V3(0f, 0.07f, 0f)))
            }.build(), material = pulseArtery,
            origin = V3(-0.150f, 1.39f, 0.105f), animate = heartbeatLeft,
            explode = V3(-0.18f, 0.08f, 0.38f),
        ),
    )
    parts.add(
        Part(
            id = "pl_atrium_right", name = "Right atrium", system = PL_CIRC, assembly = PL_HEART,
            mesh = MeshBuilder().apply {
                addMesh(ellipsoidPulse(0.135f, 0.100f, 0.110f, 22, 11))
                addMesh(Geo.torus(0.135f, 0.007f, 20, 4), Mat4.translation(V3(0f, 0.07f, 0f)))
            }.build(), material = pulseVein,
            origin = V3(0.150f, 1.40f, 0.090f), animate = heartbeatRight,
            explode = V3(0.18f, 0.08f, 0.42f),
        ),
    )
    parts.add(
        Part(
            id = "pl_septum", name = "Interventricular septum", system = PL_CIRC, assembly = PL_HEART,
            mesh = Geo.slab(0.035f, 0.245f, 0.320f, cornerRadius = 0.012f, bevel = 0.006f),
            material = Materials.paintCream,
            origin = V3(0.015f, 1.16f, 0.075f),
            explode = V3(0f, 0.02f, 0.50f),
        ),
    )
    parts.add(
        Part(
            id = "pl_valves", name = "Heart valve rings", system = PL_CIRC, assembly = PL_HEART,
            mesh = MeshBuilder().apply {
                listOf(
                    V3(-0.155f, 1.355f, 0.105f),
                    V3(0.095f, 1.365f, 0.080f),
                    V3(0.165f, 1.430f, 0.070f),
                    V3(-0.205f, 1.430f, 0.100f),
                ).forEach { point ->
                    addMesh(Geo.torus(0.048f, 0.010f, 18, 5), Mat4.translation(point))
                }
            }.build(),
            material = Materials.nickel,
            explode = V3(0f, 0.24f, 0.34f),
        ),
    )

    // Great vessels.
    parts.add(
        Part(
            id = "pl_aorta", name = "Aortic arch", system = PL_CIRC, assembly = PL_VESSEL,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(-0.11f, 1.28f, 0.12f),
                            V3(-0.10f, 1.58f, 0.10f),
                            V3(-0.01f, 1.82f, 0.02f),
                            V3(0.17f, 1.96f, -0.07f),
                            V3(0.34f, 1.89f, -0.10f),
                            V3(0.43f, 1.66f, -0.08f),
                            V3(0.42f, 1.32f, -0.04f),
                        ),
                        0.072f, 12, endRadius = 0.060f, subdivide = 4,
                    ),
                )
                listOf(0.15f, 0.30f, 0.44f).forEach { y ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(0.08f, y + 0.37f, 0.02f),
                                V3(0.02f, y + 0.48f, 0.06f),
                                V3(-0.02f, y + 0.56f, 0.08f),
                            ),
                            0.022f, 6, endRadius = 0.016f, subdivide = 2,
                        ),
                    )
                }
            }.build(),
            material = pulseArtery,
            explode = V3(0.24f, 0.10f, -0.12f),
        ),
    )
    parts.add(
        Part(
            id = "pl_pulmonary_artery", name = "Pulmonary artery", system = PL_CIRC, assembly = PL_VESSEL,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.14f, 1.24f, 0.10f),
                            V3(0.15f, 1.44f, 0.05f),
                            V3(0.10f, 1.60f, 0.00f),
                        ),
                        0.058f, 10, endRadius = 0.046f, subdivide = 3,
                    ),
                )
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(0.06f, 1.58f, 0.00f),
                                V3(side * 0.22f, 1.67f, -0.02f),
                                V3(side * 0.48f, 1.68f, -0.04f),
                                V3(side * 0.62f, 1.60f, -0.02f),
                            ),
                            0.032f, 8, endRadius = 0.024f, subdivide = 3,
                        ),
                    )
                }
            }.build(),
            material = pulseVein,
            explode = V3(0f, 0.20f, -0.14f),
        ),
    )
    parts.add(
        Part(
            id = "pl_pulmonary_veins", name = "Pulmonary veins", system = PL_CIRC, assembly = PL_VESSEL,
            mesh = MeshBuilder().apply {
                listOf(-1f, 1f).forEach { side ->
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(side * 0.62f, 1.50f, 0.02f),
                                V3(side * 0.40f, 1.45f, 0.08f),
                                V3(side * 0.20f, 1.40f, 0.12f),
                                V3(side * 0.08f, 1.39f, 0.14f),
                            ),
                            0.026f, 7, endRadius = 0.020f, subdivide = 3,
                        ),
                    )
                }
            }.build(),
            material = pulseArtery,
            explode = V3(0f, 0.16f, -0.16f),
        ),
    )
    parts.add(
        Part(
            id = "pl_vena_cava", name = "Superior vena cava", system = PL_CIRC, assembly = PL_VESSEL,
            mesh = Geo.tube(
                listOf(
                    V3(0.18f, 1.56f, 0.05f),
                    V3(0.20f, 1.38f, 0.05f),
                    V3(0.19f, 1.20f, 0.02f),
                    V3(0.33f, 0.92f, -0.02f),
                    V3(0.58f, 0.78f, 0.12f),
                ),
                0.052f, 10, endRadius = 0.040f, subdivide = 4,
            ),
            material = pulseVein,
            explode = V3(0.26f, -0.06f, -0.10f),
        ),
    )

    // The sinoatrial node sits on the right atrium with a fine electrode lead.
    parts.add(
        Part(
            id = "pl_pacemaker", name = "Sinoatrial pacemaker", system = PL_CTRL, assembly = PL_CTRL,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.050f, 0.026f, 18, bevel = 0.008f).transformed(Mat4.rotateX(90f)))
                addMesh(Geo.torus(0.051f, 0.008f, 18, 4).transformed(Mat4.rotateX(90f)))
                addMesh(Geo.box(0.016f, 0.016f, 0.016f), Mat4.translation(V3(0f, 0f, 0.018f)))
            }.build(),
            material = Materials.emitCyan,
            origin = V3(0.25f, 1.47f, 0.18f),
            explode = V3(0.2f, 0.2f, 0.4f),
        ),
    )
    parts.add(
        Part(
            id = "pl_pacemaker_lead", name = "Pacemaker electrode lead", system = PL_CTRL, assembly = PL_CTRL,
            mesh = Geo.tube(
                listOf(
                    V3(0.25f, 1.45f, 0.19f),
                    V3(0.20f, 1.38f, 0.18f),
                    V3(0.09f, 1.30f, 0.17f),
                    V3(-0.02f, 1.23f, 0.18f),
                ),
                0.008f, 5, subdivide = 3,
            ),
            material = Materials.copperBright,
            explode = V3(0.2f, 0.2f, 0.5f),
        ),
    )

    // Diaphragm: a flexible ring with radial ribs that moves with the breath.
    parts.add(
        Part(
            id = "pl_diaphragm", name = "Diaphragm", system = PL_RESP, assembly = PL_FRAME,
            mesh = MeshBuilder().apply {
                addMesh(Geo.torus(0.79f, 0.028f, 40, 5), Mat4.translation(V3(0f, 0.78f, -0.02f)))
                for (i in 0 until 9) {
                    val angle = Math.toRadians(i * 40.0)
                    val x = (0.76f * kotlin.math.cos(angle)).toFloat()
                    val z = (0.76f * kotlin.math.sin(angle)).toFloat()
                    addMesh(
                        Geo.tube(
                            listOf(
                                V3(0f, 0.72f, 0f),
                                V3(x * 0.55f, 0.77f, z * 0.55f),
                                V3(x, 0.78f, z),
                            ),
                            0.010f, 5, subdivide = 2,
                        ),
                    )
                }
            }.build(),
            material = Materials.darkSteel,
            animate = { ctx ->
                val b = breath(ctx)
                Mat4.scale(V3(1f + 0.012f * b, 1f + 0.045f * b, 1f + 0.012f * b))
            },
            explode = V3(0f, -0.12f, 0.12f),
        ),
    )

    // A transparent peripheral capillary capsule keeps the systemic loop visible.
    val capillaryOrigin = V3(0.82f, 0.70f, 0.48f)
    parts.add(
        Part(
            id = "pl_capillary_red", name = "Systemic arterial capillary", system = PL_CIRC, assembly = PL_CAP,
            mesh = Geo.helix(0.105f, 0.018f, 0.34f, 3, 16),
            material = pulseArtery,
            origin = capillaryOrigin + V3(-0.028f, 0f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 28f) },
            explode = V3(0.2f, 0.12f, 0.2f),
        ),
    )
    parts.add(
        Part(
            id = "pl_capillary_blue", name = "Systemic venous capillary", system = PL_CIRC, assembly = PL_CAP,
            mesh = Geo.helix(0.105f, 0.015f, 0.34f, 3, 16),
            material = pulseVein,
            origin = capillaryOrigin + V3(0.030f, 0f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * -28f) },
            explode = V3(0.2f, 0.12f, 0.24f),
        ),
    )
    parts.add(
        Part(
            id = "pl_capillary_shell", name = "Capillary chamber", system = PL_CIRC, assembly = PL_CAP,
            mesh = MeshBuilder().apply {
                addMesh(Geo.cylinder(0.190f, 0.46f, 24, bevel = 0.014f))
                addMesh(Geo.torus(0.190f, 0.012f, 26, 4), Mat4.translation(V3(0f, 0.22f, 0f)))
                addMesh(Geo.torus(0.190f, 0.012f, 26, 4), Mat4.translation(V3(0f, -0.22f, 0f)))
            }.build(),
            material = Materials.glass,
            origin = capillaryOrigin,
            explode = V3(0.2f, 0.12f, 0.28f),
        ),
    )
    parts.add(
        Part(
            id = "pl_capillary_pipes", name = "Systemic supply and return", system = PL_CIRC, assembly = PL_CAP,
            mesh = MeshBuilder().apply {
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.42f, 1.32f, -0.04f),
                            V3(0.64f, 1.05f, 0.18f),
                            V3(0.76f, 0.92f, 0.44f),
                            V3(0.82f, 0.82f, 0.48f),
                        ),
                        0.034f, 7, endRadius = 0.024f, subdivide = 3,
                    ),
                )
                addMesh(
                    Geo.tube(
                        listOf(
                            V3(0.82f, 0.56f, 0.48f),
                            V3(0.68f, 0.50f, 0.34f),
                            V3(0.50f, 0.77f, 0.16f),
                            V3(0.34f, 0.92f, -0.02f),
                        ),
                        0.034f, 7, endRadius = 0.022f, subdivide = 3,
                    ),
                )
            }.build(),
            material = Materials.paintSlate,
            explode = V3(0.18f, 0.04f, 0.30f),
        ),
    )

    val hotspots = listOf(
        Hotspot(
            "ventricle", "Left ventricle",
            "The left ventricle is the pressure pump of the systemic loop. Its thick muscle wall drives oxygenated blood through the aortic valve into the aorta and then to every capillary bed in the body.",
            "pl_heart_left", V3(0f, 0.03f, 0.17f),
            CameraPose(24f, 12f, 1f), readoutLabel = "SYSTEMIC PUMP",
        ),
        Hotspot(
            "lungs", "Alveolar lung",
            "The lungs are a paired gas exchanger. Air travels down the trachea and bronchi into millions of alveoli, where oxygen crosses into the capillary blood and carbon dioxide crosses back out.",
            "pl_lung_left", V3(0.02f, 0.12f, 0.19f),
            CameraPose(30f, 8f, 1f), readoutLabel = "GAS EXCHANGE",
        ),
        Hotspot(
            "aorta", "Aortic arch",
            "The aortic arch receives the full stroke volume of the left ventricle. Its curved shape turns the flow downward toward the body and gives rise to the head and arm branches.",
            "pl_aorta", V3(0.20f, 1.88f, -0.04f),
            CameraPose(34f, 22f, 1f), readoutLabel = "SYSTEMIC OUTFLOW",
        ),
        Hotspot(
            "airway", "Trachea and bronchi",
            "C-shaped cartilage rings keep the trachea open while the neck bends. At the carina it splits into two bronchi, then keeps branching until the air reaches the alveolar surface.",
            "pl_airways", V3(0f, 2.18f, 0.03f),
            CameraPose(18f, 18f, 1f), readoutLabel = "AIRWAY",
        ),
        Hotspot(
            "pacemaker", "Sinoatrial node",
            "The sinoatrial node is the heart's natural clock. It fires an electrical wave that spreads through the atria, pauses at the AV node, then contracts the ventricles in a coordinated beat.",
            "pl_pacemaker", V3(0f, 0f, 0.06f),
            CameraPose(42f, 16f, 1f), readoutLabel = "PACEMAKER",
        ),
        Hotspot(
            "capillary", "Capillary exchange",
            "Arterioles feed a dense capillary mesh where oxygen and nutrients leave the blood and carbon dioxide enters it. The veins then return this oxygen-poor blood to the right side of the heart.",
            "pl_capillary_shell", V3(0f, 0.04f, 0.12f),
            CameraPose(54f, 20f, 1f), readoutLabel = "MICROCIRCULATION",
        ),
    )

    val steps = listOf(
        GuideStep(
            "Two pumps in series",
            "The right ventricle sends blood to the lungs; the left ventricle sends it to the body. Both ventricles contract together, but they serve two different loops.",
            PL_CIRC, CameraPose(24f, 12f, 1.05f), 0.18f, 0.25f,
        ),
        GuideStep(
            "Breathe in",
            "The diaphragm flattens and the rib cage expands. Air rushes down the trachea and the lungs fill, bringing oxygen to the alveolar membrane.",
            PL_RESP, CameraPose(18f, 16f, 1.02f), 0.10f, 0.55f,
        ),
        GuideStep(
            "Exchange",
            "In the alveoli, oxygen crosses into the blood while carbon dioxide leaves it. The pulmonary veins carry the freshly oxygenated blood back to the left atrium.",
            PL_VESSEL, CameraPose(32f, 20f, 1.0f), 0.22f, 0.68f,
        ),
        GuideStep(
            "Beat",
            "The SA node fires, the atria contract, and the ventricles follow. Valves snap shut to stop backflow, producing the two sounds of a heartbeat.",
            PL_CTRL, CameraPose(42f, 16f, 1.06f), 0.12f, 0.34f,
        ),
        GuideStep(
            "Deliver and return",
            "Aortic blood reaches the capillary bed, where exchange happens at the smallest scale. Veins collect the returning blood and deliver it to the right atrium.",
            PL_CAP, CameraPose(52f, 20f, 1.02f), 0.20f, 0.45f,
        ),
        GuideStep(
            "The assembly",
            "Explode the model to separate the heart, lungs, airway, great vessels and capillary chamber. The rear arch stays with the display plinth.",
            null, CameraPose(28f, 18f, 0.96f), 1f, 0.35f,
        ),
    )

    val arterialPath = listOf(
        V3(-0.11f, 1.30f, 0.12f), V3(-0.02f, 1.72f, 0.03f), V3(0.22f, 1.94f, -0.08f),
        V3(0.41f, 1.68f, -0.07f), V3(0.42f, 1.32f, -0.04f), V3(0.68f, 0.96f, 0.30f),
        V3(0.82f, 0.80f, 0.48f), V3(0.82f, 0.56f, 0.48f),
    )
    val venousPath = listOf(
        V3(0.82f, 0.54f, 0.48f), V3(0.66f, 0.50f, 0.34f), V3(0.50f, 0.75f, 0.16f),
        V3(0.34f, 0.92f, -0.02f), V3(0.20f, 1.20f, 0.03f), V3(0.19f, 1.40f, 0.08f),
    )

    return ObjectSpec(
        id = "pulse_system",
        index = 20,
        title = "PULSE C-7",
        designation = "PC-7 / CARDIOPULMONARY LOOP",
        category = "Biology",
        categoryShort = "BIO",
        tagline = "Two pumps, one breath, one continuous loop.",
        summary = "A working cardiopulmonary exhibit: a beating four-chamber heart, breathing lungs, a branching airway, an aortic arch and a transparent capillary chamber with animated blood flow.",
        details = listOf(
            "PULSE C-7 separates the two loops that keep a body running. The right side of the heart sends oxygen-poor blood to the lungs; the left side receives oxygen-rich blood and drives it through the aorta to the body.",
            "The lungs expand and relax while air moves through the trachea and bronchi. At the capillary chamber, red and blue flow lines make the exchange visible: oxygen leaves the arterial side, carbon dioxide returns on the venous side.",
        ),
        specs = listOf(
            SpecRow("Heart rate", "62–124 bpm"),
            SpecRow("Chambers", "4"),
            SpecRow("Lung lobes", "5"),
            SpecRow("Gas exchange", "Alveolar"),
            SpecRow("Capillary bed", "Transparent"),
        ),
        tags = listOf("heart", "lung", "circulation", "biology", "anatomy", "blood", "breathing", "medical"),
        accent = 0xFFFF8B7D.toInt(),
        control = ControlSpec(
            label = "EXERTION",
            lowLabel = "REST",
            highLabel = "SPRINT",
            defaultValue = 0.24f,
            hint = "Heart rate, stroke volume, breathing depth and blood flow",
        ),
        parts = parts,
        hotspots = hotspots,
        steps = steps,
        defaultPose = CameraPose(25f, 13f, 1.04f),
        thumbnailPose = CameraPose(27f, 15f, 1.06f),
        thumbnailTime = 2.4f,
        thumbnailControl = 0.36f,
        focusOffset = V3(0f, 0.04f, 0f),
        readouts = { ctx ->
            val bpm = 62f + 62f * ctx.control
            val flow = 4.8f + 8.2f * ctx.control
            val saturation = 98f - 1.2f * ctx.control
            listOf(
                Readout("PULSE", String.format("%.0f", bpm), "bpm", accent = true),
                Readout("O₂ SAT", String.format("%.1f", saturation), "%"),
                Readout("CARDIAC", String.format("%.1f", flow), "L/min"),
            )
        },
        effects = { ctx ->
            val beat = cardiacBeat(ctx)
            val b = breath(ctx)
            listOf(
                SceneEffect.Ring(V3(0f, 0.31f, 0f), 1.28f, 0xFFFF8B7D.toInt(), 0f, 0.16f),
                SceneEffect.Flow(arterialPath, pulseBloodGlow, count = 18, speed = 0.30f, dotRadius = 0.036f),
                SceneEffect.Flow(venousPath, pulseBlueGlow, count = 15, speed = 0.24f, phase = 0.35f, dotRadius = 0.032f),
                SceneEffect.Flow(
                    listOf(V3(0f, 2.32f, 0.04f), V3(0f, 2.02f, 0f), V3(-0.30f, 1.88f, -0.02f), V3(-0.57f, 1.73f, 0.03f)),
                    0xFF9DFFF0.toInt(), count = 8, speed = 0.22f, dotRadius = 0.024f,
                ),
                SceneEffect.Glow(V3(0f, 1.20f, 0.16f), 0.42f, 0xFFFF6F64.toInt(), 0.16f + 0.22f * beat),
                SceneEffect.Glow(V3(-0.57f, 1.64f, 0.04f), 0.32f, 0xFF9DFFF0.toInt(), 0.07f + 0.10f * b),
                SceneEffect.Glow(V3(0.82f, 0.72f, 0.48f), 0.20f, 0xFF7FD8FF.toInt(), 0.18f),
                SceneEffect.Spark(V3(0.25f, 1.49f, 0.22f), 0xFF9DFFF0.toInt(), 0.028f, 0.22f + 0.44f * beat),
            )
        },
        descriptionTitle = "The living loop",
    )
}
