package studio.anatomy.app.objects

import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.model.ControlSpec
import studio.anatomy.app.model.GuideStep
import studio.anatomy.app.model.Hotspot
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.model.Part
import studio.anatomy.app.model.Readout
import studio.anatomy.app.model.SceneEffect
import studio.anatomy.app.model.SpecRow
import studio.anatomy.app.render.Geo
import studio.anatomy.app.render.MeshBuilder
import studio.anatomy.app.render.Materials as Mat
import kotlin.math.cos
import kotlin.math.sin

fun testObject(): ObjectSpec {
    val parts = ArrayList<Part>()
    val baseMesh = Geo.slab(12f, 8f, 0.34f, 1.2f, 0.06f)
    parts.add(
        Part(
            id = "base", name = "Datum plate", system = "STRUCTURE", assembly = "BASE",
            mesh = baseMesh, material = Mat.darkSteel, origin = V3(0f, -2.1f, 0f)
        )
    )
    parts.add(
        Part(
            id = "ring", name = "Turntable", system = "STRUCTURE", assembly = "BASE",
            mesh = Geo.hollowCylinder(2.9f, 2.55f, 0.12f, 64), material = Mat.copper,
            origin = V3(0f, -1.88f, 0f)
        )
    )
    val boltPoints = ArrayList<V3>()
    for (i in 0 until 8) {
        val a = Math.PI * 2 * i / 8
        boltPoints.add(V3((cos(a) * 3.5).toFloat(), 0f, (sin(a) * 3.5).toFloat()))
    }
    parts.add(
        Part(
            id = "bolts", name = "Fasteners", system = "STRUCTURE", assembly = "BASE",
            mesh = Geo.boltSet(boltPoints, 0.08f, 0.18f), material = Mat.titanium,
            origin = V3(0f, -1.86f, 0f)
        )
    )
    parts.add(
        Part(
            id = "shaft", name = "Drive shaft", system = "DRIVE", assembly = "ROTOR",
            mesh = Geo.cylinder(0.16f, 3.9f, 24), material = Mat.titanium,
            origin = V3(0f, 0.1f, 0f)
        )
    )
    parts.add(
        Part(
            id = "hub", name = "Rotor hub", system = "ROTOR", assembly = "ROTOR",
            mesh = Geo.cylinder(0.62f, 0.55f, 32, bevel = 0.08f), material = Mat.brushedSteel,
            origin = V3(0f, 0.9f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 52f) }
        )
    )
    val rotorBuilder = MeshBuilder()
    for (i in 0 until 12) {
        val angle = 360f * i / 12f
        val matrix = Mat4.translation(V3(0f, 0.9f, 0f))
            .multiply(Mat4.rotateY(angle))
            .multiply(Mat4.translation(V3(0f, 0f, 0.58f)))
            .multiply(Mat4.rotateX(90f))
        rotorBuilder.addMesh(
            Geo.roundedBlade(2.2f, 0.8f, 0.38f, 0.075f, 0.02f, 34f, 0.38f, 5, 7),
            matrix,
        )
    }
    parts.add(
        Part(
            id = "rotor", name = "Impeller", system = "ROTOR", assembly = "ROTOR",
            mesh = rotorBuilder.build(), material = Mat.titanium,
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 52f) }
        )
    )
    parts.add(
        Part(
            id = "coil", name = "Induction coil", system = "COIL", assembly = "COIL",
            mesh = Geo.helix(1.3f, 0.055f, 2.4f, 10, 16), material = Mat.copper,
            origin = V3(0f, 0.0f, 0f)
        )
    )
    parts.add(
        Part(
            id = "dome", name = "Containment dome", system = "SHIELD", assembly = "SHIELD",
            mesh = Geo.sphere(2.7f, 48, 24), material = Mat.glass,
            origin = V3(0f, 0.8f, 0f), doubleSided = true
        )
    )
    val pipe = Geo.tube(
        listOf(
            V3(-4.2f, -1.7f, -2.6f),
            V3(-4.2f, -0.4f, -2.6f),
            V3(-3.0f, -0.4f, -2.6f),
            V3(-3.0f, 0.4f, -2.0f),
        ),
        0.09f, 8, subdivide = 6,
    )
    parts.add(
        Part(
            id = "pipe", name = "Coolant line", system = "COOLING", assembly = "COOLING",
            mesh = pipe, material = Mat.copper
        )
    )
    parts.add(
        Part(
            id = "emitter", name = "Field emitter", system = "FIELD", assembly = "ROTOR",
            mesh = Geo.torus(1.68f, 0.045f, 64, 8), material = Mat.emitCyan,
            origin = V3(0f, 0.45f, 0f),
            animate = { ctx -> Mat4.rotateY(ctx.machineTime * 18f) }
        )
    )
    val hot = listOf(
        Hotspot("hub", "Rotor hub", "Concentric hub carrying the twelve blades.", "hub", V3(0f, 0.2f, 0.5f)),
        Hotspot("coil", "Induction coil", "Ten turns of copper develop the drive field.", "coil", V3(1.3f, 0.2f, 0f)),
        Hotspot("dome", "Containment dome", "Optically clear shielding keeps the rotor isolated.", "dome", V3(0f, 2.1f, 0f)),
    )
    return ObjectSpec(
        id = "test",
        index = 0,
        title = "MERIDIAN TEST RIG",
        designation = "TR-00",
        category = "Laboratory",
        categoryShort = "LAB",
        tagline = "Calibration bench for the field rotor.",
        summary = "A temporary assembly used to validate the rendering pipeline.",
        details = listOf("A dense arrangement of turned and extruded parts."),
        specs = listOf(SpecRow("Rotor", "12 blades"), SpecRow("Coil", "10 turns")),
        tags = listOf("test"),
        accent = 0xFF6EF2DF.toInt(),
        control = ControlSpec("DRIVE", "IDLE", "FULL", 0.7f, "Rotor speed"),
        parts = parts,
        hotspots = hot,
        steps = listOf(GuideStep("Rotor", "The impeller turns inside the dome.", "ROTOR", CameraPose(34f, 24f, 1f))),
        defaultPose = CameraPose(38f, 27f, 1f),
        thumbnailPose = CameraPose(40f, 28f, 1f),
        readouts = { ctx ->
            listOf(
                Readout("FIELD", "${(ctx.control * 4820).toInt()}", "rpm", accent = true),
                Readout("TORQUE", "${(ctx.control * 18.4f).toInt()}", "N·m"),
            )
        },
        effects = { ctx ->
            listOf(
                SceneEffect.Glow(V3(0f, 0.45f, 0f), 1.2f, 0xFF6EF2DF.toInt(), 0.45f + ctx.control * 0.3f),
                SceneEffect.Flow(
                    points = listOf(V3(-4.2f, -1.7f, -2.6f), V3(-3f, 0.4f, -2f)),
                    color = 0xFFF2B25C.toInt(), count = 6, speed = 0.15f, dotRadius = 0.05f,
                )
            )
        },
    )
}
