package studio.anatomy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.buildScene

class NewObjectsTest {
    private val newIds = listOf("halo_habitat", "cyclotron", "mars_rover")

    @Test
    fun newObjectsCompileAndAnimate() {
        newIds.forEach { id ->
            val spec = AnatomyCatalog.byId(id) ?: error("missing spec $id")
            val scene = buildScene(spec)
            val animated = spec.parts.count { it.animate != null }

            assertTrue("$id should have at least 20 parts", spec.parts.size >= 20)
            assertTrue("$id should animate at least 12 parts", animated >= 12)
            assertTrue("$id should expose 5 systems", spec.systemNames.size >= 5)
            assertTrue("$id should expose 5 assemblies", spec.assemblyNames.size >= 5)
            assertEquals("$id assembly list", spec.assemblyNames.size, scene.assemblies.size)
            assertTrue("$id should have 5 hotspots", spec.hotspots.size >= 5)
            assertTrue("$id should have 5 guide steps", spec.steps.size >= 5)

            spec.hotspots.forEach { hotspot ->
                assertTrue(
                    "$id hotspot ${hotspot.id} points at ${hotspot.partId}",
                    scene.partIndex.containsKey(hotspot.partId),
                )
            }

            val box = scene.restSceneBounds
            assertTrue("$id bounds radius ${box.radius}", box.radius in 0.5f..8f)
            assertTrue("$id readouts", spec.readouts(AnimContext(1f, 1f, 0.5f)).isNotEmpty())

            println(
                "$id parts=${spec.parts.size} animated=$animated systems=${spec.systemNames.size} " +
                    "hotspots=${spec.hotspots.size} radius=${box.radius}",
            )
        }
    }
}
