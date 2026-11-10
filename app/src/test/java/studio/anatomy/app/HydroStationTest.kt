package studio.anatomy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.buildScene

class HydroStationTest {
    private val spec = AnatomyCatalog.byId("hydro_station") ?: error("hydro_station is not in the catalog")

    @Test
    fun `station is a dense multi-system study`() {
        val scene = buildScene(spec)
        val animated = spec.parts.count { it.animate != null }
        val faces = spec.parts.sumOf { it.mesh.faces.size }

        assertTrue("parts=${spec.parts.size}", spec.parts.size >= 70)
        assertTrue("animated=$animated", animated >= 28)
        assertTrue("systems=${spec.systemNames.size}", spec.systemNames.size >= 7)
        assertTrue("assemblies=${spec.assemblyNames.size}", spec.assemblyNames.size >= 9)
        assertTrue("hotspots=${spec.hotspots.size}", spec.hotspots.size >= 8)
        assertTrue("steps=${spec.steps.size}", spec.steps.size >= 7)
        // Dense enough to reward inspection, light enough to stay smooth on a phone.
        assertTrue("faces=$faces", faces in 5000..14000)
        assertTrue("radius=${scene.restSceneBounds.radius}", scene.restSceneBounds.radius in 1.5f..8f)
        println("hydro_station parts=${spec.parts.size} animated=$animated faces=$faces radius=${scene.restSceneBounds.radius}")
    }

    @Test
    fun `part ids are unique and every reference resolves`() {
        val ids = spec.parts.map { it.id }
        assertEquals("duplicate part ids", ids.size, ids.toSet().size)
        spec.parts.forEach { part ->
            part.parent?.let { parent -> assertTrue("${part.id} parent $parent", parent in ids) }
        }
        spec.hotspots.forEach { hotspot ->
            assertTrue("hotspot ${hotspot.id} -> ${hotspot.partId}", hotspot.partId in ids)
        }
        spec.steps.forEach { step ->
            step.system?.let { system -> assertTrue("step system $system", system in spec.systemNames) }
        }
    }

    @Test
    fun `every assembly separates when exploded`() {
        val scene = buildScene(spec)
        val rest = scene.bounds(emptyMap()).radius
        val exploded = scene.bounds(scene.assemblies.associateWith { 1f }).radius
        assertTrue("exploded $exploded should exceed rest $rest", exploded > rest * 1.15f)
        val moving = spec.assemblyNames.count { assembly ->
            spec.parts.any { it.assembly == assembly && it.explode.lengthSq() > 1e-4f }
        }
        assertTrue("assemblies that move: $moving", moving >= spec.assemblyNames.size - 2)
    }

    @Test
    fun `wicket gates open with the load control`() {
        val gate = spec.parts.firstOrNull { it.id == "hy_gate_0" }
        assertNotNull("hy_gate_0", gate)
        val animate = requireNotNull(gate?.animate)
        val closed = animate(AnimContext(1f, 1f, 0f)).m
        val open = animate(AnimContext(1f, 1f, 1f)).m
        assertTrue("gate angle should change with control", kotlin.math.abs(closed[0] - open[0]) > 0.05f)
    }

    @Test
    fun `readouts and effects follow the control`() {
        val low = AnimContext(2f, 2f, 0.1f)
        val high = AnimContext(2f, 2f, 0.9f)
        assertTrue(spec.readouts(low).size >= 3)
        assertTrue(spec.effects(high).size >= 6)
        val lowPower = spec.readouts(low).first().value.replace(',', '.').toFloat()
        val highPower = spec.readouts(high).first().value.replace(',', '.').toFloat()
        assertTrue("power $lowPower -> $highPower", highPower > lowPower)
    }
}
