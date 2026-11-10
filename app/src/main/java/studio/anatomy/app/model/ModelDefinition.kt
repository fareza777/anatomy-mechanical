package studio.anatomy.app.model

import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.PoseConfig
import studio.anatomy.app.core.V3
import studio.anatomy.app.render.Material
import studio.anatomy.app.render.Mesh
import studio.anatomy.app.render.MeshBuilder

data class AnimContext(
    val ambientTime: Float,
    val machineTime: Float,
    val control: Float,
    val selectedSystem: String? = null,
) {
    fun sine(speed: Float, phase: Float = 0f) = kotlin.math.sin(machineTime * speed + phase)
}

class Part(
    val id: String,
    val name: String,
    val system: String,
    val assembly: String,
    val mesh: Mesh,
    val material: Material,
    val origin: V3 = V3.ZERO,
    val rotation: Mat4 = Mat4.identity(),
    val scale: V3 = V3.ONE,
    val parent: String? = null,
    val explode: V3 = V3.ZERO,
    val animate: ((AnimContext) -> Mat4)? = null,
    val doubleSided: Boolean = false,
    val anchor: V3? = null,
)

data class Hotspot(
    val id: String,
    val title: String,
    val body: String,
    val partId: String,
    val anchor: V3,
    val camera: CameraPose = CameraPose(36f, 26f, 1f),
    val explode: Float = 0f,
    val readoutLabel: String? = null,
)

data class GuideStep(
    val title: String,
    val body: String,
    val system: String? = null,
    val camera: CameraPose? = null,
    val explode: Float? = null,
    val control: Float? = null,
)

data class SpecRow(val label: String, val value: String)

data class Readout(
    val label: String,
    val value: String,
    val unit: String = "",
    val accent: Boolean = false,
    val warn: Boolean = false,
)

data class ControlSpec(
    val label: String,
    val lowLabel: String,
    val highLabel: String,
    val defaultValue: Float,
    val hint: String,
)

enum class PlumePalette { PLASMA, AMBER, STEAM, DIESEL }

sealed interface SceneEffect {
    data class Glow(
        val position: V3,
        val radius: Float,
        val color: Int,
        val intensity: Float,
    ) : SceneEffect

    data class Plume(
        val origin: V3,
        val direction: V3,
        val length: Float,
        val radius: Float,
        val intensity: Float,
        val palette: PlumePalette,
    ) : SceneEffect

    data class Flow(
        val points: List<V3>,
        val color: Int,
        val count: Int = 14,
        val speed: Float = 0.25f,
        val phase: Float = 0f,
        val dotRadius: Float = 0.03f,
    ) : SceneEffect

    data class Ring(
        val center: V3,
        val radius: Float,
        val color: Int,
        val phase: Float,
        val intensity: Float = 0.7f,
    ) : SceneEffect

    data class Beam(
        val start: V3,
        val end: V3,
        val color: Int,
        val intensity: Float = 0.7f,
    ) : SceneEffect

    data class Spark(
        val position: V3,
        val color: Int,
        val size: Float,
        val intensity: Float,
    ) : SceneEffect
}

data class ObjectSpec(
    val id: String,
    val index: Int,
    val title: String,
    val designation: String,
    val category: String,
    val categoryShort: String,
    val tagline: String,
    val summary: String,
    val details: List<String>,
    val specs: List<SpecRow>,
    val tags: List<String>,
    val accent: Int,
    val control: ControlSpec,
    val parts: List<Part>,
    val hotspots: List<Hotspot>,
    val steps: List<GuideStep>,
    val defaultPose: CameraPose,
    val thumbnailPose: CameraPose,
    val thumbnailTime: Float = 3f,
    val thumbnailControl: Float = 0.75f,
    val focusOffset: V3 = V3.ZERO,
    val readouts: (AnimContext) -> List<Readout>,
    val effects: (AnimContext) -> List<SceneEffect> = { emptyList() },
    val descriptionTitle: String = "The mechanism",
) {
    val partCount: Int get() = parts.size
    val systemNames: List<String> get() = parts.map { it.system }.distinct()
    val assemblyNames: List<String> get() = parts.map { it.assembly }.distinct()
}

class ModelBuilder {
    private val parts = ArrayList<Part>()

    fun add(
        id: String,
        name: String,
        system: String,
        assembly: String,
        mesh: Mesh,
        material: Material,
        origin: V3 = V3.ZERO,
        rotation: Mat4 = Mat4.identity(),
        scale: V3 = V3.ONE,
        parent: String? = null,
        explode: V3 = V3.ZERO,
        animate: ((AnimContext) -> Mat4)? = null,
        doubleSided: Boolean = false,
        anchor: V3? = null,
    ) {
        if (mesh.vertices.isEmpty()) return
        parts.add(
            Part(
                id = id,
                name = name,
                system = system,
                assembly = assembly,
                mesh = mesh,
                material = material,
                origin = origin,
                rotation = rotation,
                scale = scale,
                parent = parent,
                explode = explode,
                animate = animate,
                doubleSided = doubleSided,
                anchor = anchor,
            )
        )
    }

    fun addPart(part: Part) {
        parts.add(part)
    }

    fun buildParts(): List<Part> = parts.toList()

    fun merge(
        targetId: String,
        name: String,
        system: String,
        assembly: String,
        material: Material,
        origin: V3 = V3.ZERO,
        rotation: Mat4 = Mat4.identity(),
        animate: ((AnimContext) -> Mat4)? = null,
        explode: V3 = V3.ZERO,
        doubleSided: Boolean = false,
        build: MeshBuilder.() -> Unit,
    ) {
        // The lambda was never invoked before, so every merge produced an empty mesh
        // and silently added nothing. Run it against the builder as intended.
        val builder = MeshBuilder().apply(build)
        add(
            id = targetId,
            name = name,
            system = system,
            assembly = assembly,
            mesh = builder.build(),
            material = material,
            origin = origin,
            rotation = rotation,
            animate = animate,
            explode = explode,
            doubleSided = doubleSided,
        )
    }
}

fun part(
    id: String,
    name: String,
    system: String,
    assembly: String,
    mesh: Mesh,
    material: Material,
    origin: V3 = V3.ZERO,
    rotation: Mat4 = Mat4.identity(),
    parent: String? = null,
    explode: V3 = V3.ZERO,
    animate: ((AnimContext) -> Mat4)? = null,
    doubleSided: Boolean = false,
    anchor: V3? = null,
): Part = Part(
    id = id,
    name = name,
    system = system,
    assembly = assembly,
    mesh = mesh,
    material = material,
    origin = origin,
    rotation = rotation,
    parent = parent,
    explode = explode,
    animate = animate,
    doubleSided = doubleSided,
    anchor = anchor,
)

class CompiledScene(val spec: ObjectSpec) {
    val parts: List<Part> = spec.parts
    val partIndex: Map<String, Int> = parts.mapIndexed { index, part -> part.id to index }.toMap()
    val order: IntArray
    val parentIndex: IntArray
    val restWorld: Array<Mat4>
    val restBounds: Array<Aabb>
    val hotspotPartIndex: IntArray
    val systems: Map<String, List<Int>>
    val assemblies: List<String> = spec.assemblyNames
    val restSceneBounds: Aabb
    val assemblyCenters: Map<String, V3>

    init {
        parentIndex = IntArray(parts.size) { index ->
            parts[index].parent?.let { partIndex[it] } ?: -1
        }
        val visited = BooleanArray(parts.size)
        val visiting = BooleanArray(parts.size)
        val result = ArrayList<Int>(parts.size)
        fun visit(index: Int) {
            if (visited[index] || visiting[index]) return
            visiting[index] = true
            val parent = parentIndex[index]
            if (parent >= 0) visit(parent)
            visiting[index] = false
            visited[index] = true
            result.add(index)
        }
        for (index in parts.indices) visit(index)
        order = result.toIntArray()

        restWorld = Array(parts.size) { Mat4.identity() }
        restBounds = Array(parts.size) { Aabb(V3.ZERO, V3.ZERO) }
        val boxes = ArrayList<Aabb>()
        order.forEach { index ->
            val item = parts[index]
            val local = Mat4.translation(item.origin)
                .multiply(item.rotation)
                .multiply(Mat4.scale(item.scale))
            restWorld[index] = if (parentIndex[index] >= 0) {
                restWorld[parentIndex[index]].multiply(local)
            } else {
                local
            }
            val box = transformBounds(item.mesh.bounds, restWorld[index])
            restBounds[index] = box
            boxes.add(box)
        }
        restSceneBounds = if (boxes.isEmpty()) Aabb(V3.ZERO, V3.ZERO) else Aabb.union(boxes)
        hotspotPartIndex = IntArray(spec.hotspots.size) { index ->
            partIndex[spec.hotspots[index].partId] ?: -1
        }
        systems = parts.indices.groupBy { parts[it].system }
        assemblyCenters = assemblies.associateWith { assembly ->
            val boxes = parts.indices.filter { parts[it].assembly == assembly }.map { restBounds[it] }
            if (boxes.isEmpty()) V3.ZERO else Aabb.union(boxes).center
        }
    }

    fun systemCount(system: String): Int = systems[system]?.size ?: 0

    fun bounds(explode: Map<String, Float>): Aabb {
        var box: Aabb? = null
        parts.forEachIndexed { index, part ->
            val amount = explode[part.assembly] ?: 0f
            val partBox = if (amount == 0f) restBounds[index] else restBounds[index].translated(part.explode * amount)
            box = box?.union(partBox) ?: partBox
        }
        return box ?: Aabb(V3.ZERO, V3.ZERO)
    }

    fun annotationWorld(partId: String, anchor: V3, worlds: Array<Mat4>): V3 {
        val index = partIndex[partId] ?: return anchor
        return worlds[index].transform(anchor)
    }

    companion object {
        fun transformBounds(bounds: Aabb, matrix: Mat4): Aabb {
            return Aabb.of(bounds.corners().map { matrix.transform(it) })
        }
    }
}

data class CatalogEntry(
    val spec: ObjectSpec,
    val scene: CompiledScene,
)

fun buildScene(spec: ObjectSpec) = CompiledScene(spec)

fun Mat4.withScale(scale: V3) = multiply(Mat4.scale(scale))

fun PoseConfig.camera() = CameraPose(yaw, pitch, zoom)
