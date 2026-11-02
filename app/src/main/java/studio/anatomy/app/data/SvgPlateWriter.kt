package studio.anatomy.app.data

import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.core.cameraBasis
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.model.Part
import studio.anatomy.app.render.FaceMode
import studio.anatomy.app.render.Shading
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A genuine vector export: every visible polygon is written into the SVG in
 * painter's order. Gradients are used only for the atmospheric backdrop.
 */
object SvgPlateWriter {

    private const val WIDTH = 2400
    private const val HEIGHT = 1500
    private const val CX = WIDTH * 0.5f
    private const val CY = HEIGHT * 0.43f

    private data class SvgFace(
        val depth: Float,
        val points: FloatArray,
        val fill: Int,
        val stroke: Int,
        val area: Float,
    )

    fun write(
        spec: ObjectSpec,
        scene: CompiledScene,
        pose: CameraPose,
        anim: AnimContext,
        explode: Map<String, Float>,
        selectedSystem: String?,
        xray: Boolean,
    ): String {
        val basis = cameraBasis(pose)
        val bounds = scene.bounds(explode)
        val visual = expand(bounds, 1.14f)
        val target = visual.center + spec.focusOffset
        val scale = fitScale(visual, target, basis.right, basis.up, pose.zoom)

        val worlds = worldMatrices(scene, anim, explode)
        val faces = ArrayList<SvgFace>(6000)
        scene.parts.forEachIndexed { index, part ->
            val world = worlds[index] ?: return@forEachIndexed
            val mode = modeFor(part, selectedSystem, xray)
            val includeBackfaces = part.doubleSided || xray
            part.mesh.faces.forEach { face ->
                val nx = world.m[0] * face.normal.x + world.m[1] * face.normal.y + world.m[2] * face.normal.z
                val ny = world.m[4] * face.normal.x + world.m[5] * face.normal.y + world.m[6] * face.normal.z
                val nz = world.m[8] * face.normal.x + world.m[9] * face.normal.y + world.m[10] * face.normal.z
                val dot = nx * basis.eye.x + ny * basis.eye.y + nz * basis.eye.z
                val backface = dot <= 0f
                if (backface && !includeBackfaces) return@forEach
                val screen = FloatArray(face.indices.size * 2)
                var minX = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                var depth = 0f
                for (i in face.indices.indices) {
                    val vertexIndex = face.indices[i]
                    val vertex = part.mesh.vertices[vertexIndex]
                    val m = world.m
                    val wx = m[0] * vertex.x + m[1] * vertex.y + m[2] * vertex.z + m[3]
                    val wy = m[4] * vertex.x + m[5] * vertex.y + m[6] * vertex.z + m[7]
                    val wz = m[8] * vertex.x + m[9] * vertex.y + m[10] * vertex.z + m[11]
                    val rx = wx - target.x
                    val ry = wy - target.y
                    val rz = wz - target.z
                    val sx = CX + (rx * basis.right.x + ry * basis.right.y + rz * basis.right.z) * scale
                    val sy = CY - (rx * basis.up.x + ry * basis.up.y + rz * basis.up.z) * scale
                    val d = rx * basis.eye.x + ry * basis.eye.y + rz * basis.eye.z
                    depth += d
                    screen[i * 2] = sx
                    screen[i * 2 + 1] = sy
                    if (sx < minX) minX = sx
                    if (sx > maxX) maxX = sx
                    if (sy < minY) minY = sy
                    if (sy > maxY) maxY = sy
                }
                if (screen.size < 6) return@forEach
                if (maxX < 0f || minX > WIDTH || maxY < 0f || minY > HEIGHT) return@forEach
                val area = (maxX - minX) * (maxY - minY)
                if (area < 0.7f) return@forEach
                val shadeNormal = if (backface) V3(-nx, -ny, -nz) else V3(nx, ny, nz)
                val fill = Shading.shade(part.material, shadeNormal.x, shadeNormal.y, shadeNormal.z, mode, anim.ambientTime)
                val stroke = part.material.edgeColor(mode)
                faces.add(SvgFace(depth / face.indices.size, screen, fill, stroke, area))
            }
        }
        faces.sortBy { it.depth }

        val paint = Shading.ACCENT
        val sb = StringBuilder(1_200_000)
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="$WIDTH" height="$HEIGHT" viewBox="0 0 $WIDTH $HEIGHT">""").append('\n')
        sb.append("<title>${escape(spec.title)} — Anatomy Studio</title>\n")
        sb.append("<desc>${escape(spec.tagline)}. Vector plate generated on device by Anatomy Studio.</desc>\n")
        sb.append("<defs>\n")
        sb.append("""<linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0A1319"/><stop offset="0.55" stop-color="#070C11"/><stop offset="1" stop-color="#04070A"/></linearGradient>""").append('\n')
        sb.append("""<radialGradient id="glow" cx="50%" cy="35%" r="62%"><stop offset="0" stop-color="#${hex(paint)}" stop-opacity="0.10"/><stop offset="0.6" stop-color="#${hex(paint)}" stop-opacity="0.02"/><stop offset="1" stop-color="#${hex(paint)}" stop-opacity="0"/></radialGradient>""").append('\n')
        sb.append("""<linearGradient id="fade" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#050A0E" stop-opacity="0"/><stop offset="0.55" stop-color="#050A0E" stop-opacity="0.88"/><stop offset="1" stop-color="#050A0E" stop-opacity="1"/></linearGradient>""").append('\n')
        sb.append("""<filter id="soft" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="26"/></filter>""").append('\n')
        sb.append("</defs>\n")
        sb.append("""<rect width="$WIDTH" height="$HEIGHT" fill="url(#bg)"/>""").append('\n')
        sb.append("""<rect width="$WIDTH" height="$HEIGHT" fill="url(#glow)"/>""").append('\n')

        drawFloor(sb, scene, bounds, target, basis, scale, paint)
        drawContactShadow(sb, bounds, target, basis, scale)

        faces.forEach { face ->
            sb.append("""<path d="M""")
            for (i in 0 until face.points.size / 2) {
                val x = face.points[i * 2]
                val y = face.points[i * 2 + 1]
                if (i > 0) sb.append('L')
                sb.append(format(x)).append(' ').append(format(y)).append(' ')
            }
            sb.append("Z\" fill=\"#").append(hex(face.fill)).append('"')
            val fillAlpha = (face.fill ushr 24) and 0xFF
            if (fillAlpha < 250) sb.append(" fill-opacity=\"").append(format(fillAlpha / 255f, 2)).append('"')
            if (face.area > 8f) {
                sb.append(" stroke=\"#").append(hex(face.stroke)).append('"')
                val strokeAlpha = (face.stroke ushr 24) and 0xFF
                if (strokeAlpha < 250) sb.append(" stroke-opacity=\"").append(format(strokeAlpha / 255f, 2)).append('"')
                sb.append(" stroke-width=\"0.9\" stroke-linejoin=\"round\"")
            }
            sb.append("/>\n")
        }

        val border = 0x3320313A
        sb.append("""<rect x="${WIDTH * 0.035f}" y="${HEIGHT * 0.04f}" width="${WIDTH * 0.93f}" height="${HEIGHT * 0.92f}" fill="none" stroke="#${hex(border)}" stroke-width="2"/>""").append('\n')
        sb.append("""<rect y="${HEIGHT * 0.62f}" width="$WIDTH" height="${HEIGHT * 0.38f}" fill="url(#fade)"/>""").append('\n')

        val accentHex = hex(spec.accent)
        text(sb, "ANATOMY / 3D STUDIO", WIDTH * 0.055f, HEIGHT * 0.082f, WIDTH * 0.0105f, accentHex, mono = true, letterSpacing = 0.12f)
        text(sb, "OBJECT ${spec.index.toString().padStart(2, '0')} / ${spec.category}", WIDTH * 0.945f, HEIGHT * 0.082f, WIDTH * 0.0092f, "#93A7A9", mono = true, anchor = "end")
        text(sb, spec.designation, WIDTH * 0.055f, HEIGHT * 0.108f, WIDTH * 0.0084f, "#93A7A9", mono = true, letterSpacing = 0.08f)
        text(sb, "NATIVE VECTOR PLATE", WIDTH * 0.945f, HEIGHT * 0.108f, WIDTH * 0.0084f, "#93A7A9", mono = true, anchor = "end")

        text(sb, spec.title, WIDTH * 0.055f, HEIGHT * 0.842f, WIDTH * 0.054f, "#EDF2ED", weight = 500, letterSpacing = -0.02f)
        sb.append("""<rect x="${WIDTH * 0.055f}" y="${HEIGHT * 0.866f}" width="${WIDTH * 0.04f}" height="4" fill="#$accentHex"/>""").append('\n')
        text(sb, spec.category, WIDTH * 0.055f, HEIGHT * 0.893f, WIDTH * 0.0092f, accentHex, mono = true, letterSpacing = 0.14f)
        text(sb, spec.tagline, WIDTH * 0.055f, HEIGHT * 0.918f, WIDTH * 0.0126f, "#93A7A9")
        text(
            sb,
            "${spec.partCount} PARTS  /  ${spec.systemNames.size} SYSTEMS  /  ${spec.hotspots.size} HOTSPOTS",
            WIDTH * 0.055f,
            HEIGHT * 0.949f,
            WIDTH * 0.0084f,
            "#93A7A9",
            mono = true,
        )
        text(sb, "GENERATED ON DEVICE", WIDTH * 0.945f, HEIGHT * 0.949f, WIDTH * 0.0084f, "#93A7A9", mono = true, anchor = "end")
        sb.append("""<line x1="${WIDTH * 0.055f}" y1="${HEIGHT * 0.963f}" x2="${WIDTH * 0.945f}" y2="${HEIGHT * 0.963f}" stroke="#3320313A" stroke-width="1"/>""").append('\n')
        sb.append("</svg>\n")
        return sb.toString()
    }

    private fun drawFloor(
        sb: StringBuilder,
        scene: CompiledScene,
        bounds: Aabb,
        target: V3,
        basis: studio.anatomy.app.core.CameraBasis,
        scale: Float,
        accent: Int,
    ) {
        val floorY = bounds.min.y - max(0.12f, bounds.size.y * 0.075f)
        val size = max(bounds.size.x, bounds.size.z)
        val center = V3(bounds.center.x, floorY, bounds.center.z)
        val extent = max(bounds.size.x, bounds.size.z) * 1.5f + 1.5f
        val steps = 10
        sb.append("<g stroke=\"#${hex(accent)}\" stroke-opacity=\"0.09\" stroke-width=\"1\">\n")
        for (i in -steps..steps) {
            val offset = extent * i / steps
            line(sb, center + V3(offset, 0f, -extent), center + V3(offset, 0f, extent), target, basis, scale)
            line(sb, center + V3(-extent, 0f, offset), center + V3(extent, 0f, offset), target, basis, scale)
        }
        sb.append("</g>\n")
        val radius = size * 0.78f + 0.4f
        sb.append("<polyline points=\"")
        for (i in 0..72) {
            val angle = 2.0 * Math.PI * i / 72
            val p = center + V3((radius * cos(angle)).toFloat(), 0f, (radius * sin(angle)).toFloat())
            val projected = project(p, target, basis, scale)
            sb.append(format(projected.first)).append(',').append(format(projected.second)).append(' ')
        }
        sb.append("\" fill=\"none\" stroke=\"#${hex(accent)}\" stroke-opacity=\"0.28\" stroke-width=\"1.4\"/>\n")
        sb.append("<g stroke=\"#${hex(accent)}\" stroke-opacity=\"0.22\" stroke-width=\"1.1\">\n")
        for (i in 0 until 36) {
            val angle = 2.0 * Math.PI * i / 36
            val inner = radius - if (i % 3 == 0) 0.16f else 0.08f
            val p0 = center + V3((inner * cos(angle)).toFloat(), 0f, (inner * sin(angle)).toFloat())
            val p1 = center + V3((radius * cos(angle)).toFloat(), 0f, (radius * sin(angle)).toFloat())
            line(sb, p0, p1, target, basis, scale)
        }
        sb.append("</g>\n")
        sb.append("<g stroke=\"#${hex(accent)}\" stroke-opacity=\"0.5\" stroke-width=\"2\">\n")
        for (arc in 0 until 2) {
            sb.append("<polyline points=\"")
            val start = (if (arc == 0) 16 else 196) * Math.PI / 180.0
            val sweep = 24 * Math.PI / 180.0
            for (i in 0..22) {
                val angle = start + sweep * i / 22
                val p = center + V3((radius * 1.025f * cos(angle)).toFloat(), 0f, (radius * 1.025f * sin(angle)).toFloat())
                val projected = project(p, target, basis, scale)
                sb.append(format(projected.first)).append(',').append(format(projected.second)).append(' ')
            }
            sb.append("\" fill=\"none\"/>\n")
        }
        sb.append("</g>\n")
    }

    private fun drawContactShadow(
        sb: StringBuilder,
        bounds: Aabb,
        target: V3,
        basis: studio.anatomy.app.core.CameraBasis,
        scale: Float,
    ) {
        val center = V3(bounds.center.x, bounds.min.y - 0.01f, bounds.center.z)
        val projected = project(center, target, basis, scale)
        val radius = max(20f, max(bounds.size.x, bounds.size.z) * 0.56f * scale)
        sb.append("""<ellipse cx="${format(projected.first)}" cy="${format(projected.second + radius * 0.12f)}" rx="${format(radius * 1.9f)}" ry="${format(radius * 0.62f)}" fill="#000000" fill-opacity="0.48" filter="url(#soft)"/>""").append('\n')
    }

    private fun line(
        sb: StringBuilder,
        start: V3,
        end: V3,
        target: V3,
        basis: studio.anatomy.app.core.CameraBasis,
        scale: Float,
    ) {
        val a = project(start, target, basis, scale)
        val b = project(end, target, basis, scale)
        sb.append("<line x1=\"").append(format(a.first)).append("\" y1=\"").append(format(a.second))
            .append("\" x2=\"").append(format(b.first)).append("\" y2=\"").append(format(b.second)).append("\"/>\n")
    }

    private fun project(
        point: V3,
        target: V3,
        basis: studio.anatomy.app.core.CameraBasis,
        scale: Float,
    ): Pair<Float, Float> {
        val rx = point.x - target.x
        val ry = point.y - target.y
        val rz = point.z - target.z
        return Pair(
            CX + (rx * basis.right.x + ry * basis.right.y + rz * basis.right.z) * scale,
            CY - (rx * basis.up.x + ry * basis.up.y + rz * basis.up.z) * scale,
        )
    }

    private fun worldMatrices(scene: CompiledScene, anim: AnimContext, explode: Map<String, Float>): Array<Mat4?> {
        val worlds = arrayOfNulls<Mat4>(scene.parts.size)
        scene.order.forEach { index ->
            val part = scene.parts[index]
            var local = Mat4.translation(part.origin).multiply(part.rotation)
            part.animate?.let { local = local.multiply(it(anim)) }
            local = local.multiply(Mat4.scale(part.scale))
            val parent = scene.parentIndex[index]
            val base = if (parent >= 0) (worlds[parent] ?: Mat4.identity()).multiply(local) else local
            val amount = explode[part.assembly] ?: 0f
            worlds[index] = if (amount > 0.0001f) {
                Mat4.translation(part.explode * amount).multiply(base)
            } else {
                base
            }
        }
        return worlds
    }

    private fun modeFor(part: Part, selectedSystem: String?, xray: Boolean): FaceMode = when {
        selectedSystem == null && xray -> FaceMode.GHOST
        selectedSystem == null -> FaceMode.NORMAL
        part.system == selectedSystem -> FaceMode.SELECTED
        xray -> FaceMode.GHOST
        else -> FaceMode.DIMMED
    }

    private fun fitScale(visual: Aabb, target: V3, right: V3, up: V3, zoom: Float): Float {
        var maxX = 0.001f
        var maxY = 0.001f
        visual.corners().forEach { corner ->
            val rx = corner.x - target.x
            val ry = corner.y - target.y
            val rz = corner.z - target.z
            maxX = max(maxX, abs(rx * right.x + ry * right.y + rz * right.z))
            maxY = max(maxY, abs(rx * up.x + ry * up.y + rz * up.z))
        }
        val availableWidth = WIDTH * 0.90f
        val availableHeight = HEIGHT * 0.72f
        return min(availableWidth / (maxX * 2f), availableHeight / (maxY * 2f)) * 0.94f * zoom
    }

    private fun expand(bounds: Aabb, factor: Float): Aabb {
        val center = bounds.center
        val half = bounds.size * (factor * 0.5f)
        return Aabb(center - half, center + half)
    }

    private fun text(
        sb: StringBuilder,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: String,
        mono: Boolean = false,
        anchor: String = "start",
        letterSpacing: Float = 0f,
        weight: Int = 400,
    ) {
        sb.append("<text x=\"").append(format(x)).append("\" y=\"").append(format(y))
            .append("\" font-family=\"")
            .append(if (mono) "JetBrains Mono, DejaVu Sans Mono, monospace" else "Space Grotesk, Helvetica Neue, Arial, sans-serif")
            .append("\" font-size=\"").append(format(size))
            .append("\" font-weight=\"").append(weight)
            .append("\" fill=\"#").append(color).append('"')
        if (anchor != "start") sb.append(" text-anchor=\"").append(anchor).append('"')
        if (letterSpacing != 0f) sb.append(" letter-spacing=\"").append(format(letterSpacing * size)).append('"')
        sb.append('>').append(escape(value)).append("</text>\n")
    }

    private fun format(value: Float): String = String.format(java.util.Locale.US, "%.1f", value)

    private fun format(value: Float, decimals: Int): String =
        String.format(java.util.Locale.US, "%.${decimals}f", value)

    private fun hex(color: Int): String {
        val value = color and 0xFFFFFF
        return value.toString(16).padStart(6, '0')
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
