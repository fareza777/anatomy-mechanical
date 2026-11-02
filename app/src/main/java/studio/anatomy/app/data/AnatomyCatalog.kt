package studio.anatomy.app.data

import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.model.buildScene
import studio.anatomy.app.objects.arachnid
import studio.anatomy.app.objects.cameraLens
import studio.anatomy.app.objects.deltaRobot
import studio.anatomy.app.objects.gearbox
import studio.anatomy.app.objects.hydraulicArm
import studio.anatomy.app.objects.ionThruster
import studio.anatomy.app.objects.quadcopter
import studio.anatomy.app.objects.robotArm
import studio.anatomy.app.objects.roboticHand
import studio.anatomy.app.objects.rocketEngine
import studio.anatomy.app.objects.spindle
import studio.anatomy.app.objects.teslaTurbine
import studio.anatomy.app.objects.turbofan
import studio.anatomy.app.objects.v8Engine
import studio.anatomy.app.objects.watchMovement
import studio.anatomy.app.objects.windTurbine
import java.util.concurrent.ConcurrentHashMap

/**
 * Local atlas. Every object is compiled procedurally on device; nothing is downloaded
 * and the whole collection works in airplane mode.
 */
object AnatomyCatalog {
    val specs: List<ObjectSpec> by lazy {
        listOf(
            turbofan(),
            robotArm(),
            windTurbine(),
            gearbox(),
            deltaRobot(),
            rocketEngine(),
            v8Engine(),
            cameraLens(),
            quadcopter(),
            watchMovement(),
            roboticHand(),
            arachnid(),
            ionThruster(),
            hydraulicArm(),
            spindle(),
            teslaTurbine(),
        )
    }

    val categories: List<String>
        get() = listOf("ALL") + specs.map { it.category }.distinct()

    fun byId(id: String?): ObjectSpec? = specs.firstOrNull { it.id == id }

    fun search(query: String, favoritesOnly: Boolean, favorites: Set<String>): List<ObjectSpec> {
        val needle = query.trim().lowercase()
        return specs.filter { spec ->
            val favoriteMatch = !favoritesOnly || spec.id in favorites
            val queryMatch = needle.isEmpty() ||
                spec.title.lowercase().contains(needle) ||
                spec.designation.lowercase().contains(needle) ||
                spec.category.lowercase().contains(needle) ||
                spec.tagline.lowercase().contains(needle) ||
                spec.tags.any { it.lowercase().contains(needle) }
            favoriteMatch && queryMatch
        }
    }

    private val compiled = ConcurrentHashMap<String, CompiledScene>()

    fun scene(spec: ObjectSpec): CompiledScene = compiled.computeIfAbsent(spec.id) { buildScene(spec) }
}
