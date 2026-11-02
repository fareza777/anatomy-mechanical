package studio.anatomy.app.render

import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V2
import studio.anatomy.app.core.V3
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Mesh(
    val vertices: List<V3>,
    val faces: List<Face>,
    val bounds: Aabb,
) {
    class Face(
        val indices: IntArray,
        val normal: V3,
        val materialHint: String? = null,
    )

    fun transformed(matrix: Mat4): Mesh {
        val builder = MeshBuilder()
        builder.addMesh(this, matrix)
        return builder.build()
    }

    companion object {
        val EMPTY = Mesh(emptyList(), emptyList(), Aabb(V3.ZERO, V3.ZERO))
    }
}

class MeshBuilder {
    private val vertices = ArrayList<V3>()
    private val faces = ArrayList<Mesh.Face>()

    fun addVertex(point: V3): Int {
        vertices.add(point)
        return vertices.size - 1
    }

    fun addFace(points: List<V3>, normal: V3? = null) {
        if (points.size < 3) return
        val indices = IntArray(points.size)
        points.forEachIndexed { index, point -> indices[index] = addVertex(point) }
        addIndexedFace(indices, normal ?: normalOf(points))
    }

    fun addIndexedFace(indices: IntArray, normal: V3) {
        if (indices.size < 3) return
        val normalized = normal.normalized()
        if (normalized.lengthSq() < 0.5f) return
        faces.add(Mesh.Face(indices, normalized))
    }

    fun addQuad(a: V3, b: V3, c: V3, d: V3, normal: V3? = null) {
        addFace(listOf(a, b, c, d), normal)
    }

    fun addMesh(mesh: Mesh, matrix: Mat4 = Mat4.identity()) {
        if (matrix.m.contentEquals(Mat4.identity().m)) {
            val offset = vertices.size
            vertices.addAll(mesh.vertices)
            mesh.faces.forEach { face ->
                faces.add(Mesh.Face(face.indices.map { it + offset }.toIntArray(), face.normal))
            }
        } else {
            val offset = vertices.size
            mesh.vertices.forEach { vertices.add(matrix.transform(it)) }
            mesh.faces.forEach { face ->
                val normal = matrix.transformDirection(face.normal).normalized()
                faces.add(Mesh.Face(face.indices.map { it + offset }.toIntArray(), normal))
            }
        }
    }

    fun build(): Mesh {
        if (vertices.isEmpty()) return Mesh.EMPTY
        return Mesh(vertices.toList(), faces.toList(), Aabb.of(vertices))
    }

    private fun normalOf(points: List<V3>): V3 {
        var nx = 0f
        var ny = 0f
        var nz = 0f
        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            nx += (current.y - next.y) * (current.z + next.z)
            ny += (current.z - next.z) * (current.x + next.x)
            nz += (current.x - next.x) * (current.y + next.y)
        }
        val result = V3(nx, ny, nz)
        return if (result.lengthSq() < 1e-10f) V3.ZERO else result.normalized()
    }
}

object Geo {

    fun empty() = Mesh.EMPTY

    fun roundedRectRing(width: Float, depth: Float, radius: Float, cornerSegments: Int = 3): List<V2> {
        val hw = width * 0.5f
        val hd = depth * 0.5f
        val r = radius.coerceIn(0f, min(hw, hd))
        if (r <= 0.0005f) {
            return listOf(V2(hw, hd), V2(-hw, hd), V2(-hw, -hd), V2(hw, -hd))
        }
        val points = ArrayList<V2>(4 * (cornerSegments + 1))
        val corners = listOf(
            Triple(V2(hw - r, hd - r), 0f, 90f),
            Triple(V2(-hw + r, hd - r), 90f, 180f),
            Triple(V2(-hw + r, -hd + r), 180f, 270f),
            Triple(V2(hw - r, -hd + r), 270f, 360f),
        )
        corners.forEach { (center, start, end) ->
            for (step in 0..cornerSegments) {
                val angle = Math.toRadians((start + (end - start) * step / cornerSegments).toDouble())
                points.add(V2(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat()))
            }
        }
        return points
    }

    fun slab(
        width: Float,
        depth: Float,
        height: Float,
        cornerRadius: Float = min(width, depth) * 0.08f,
        bevel: Float = min(height * 0.22f, 0.035f),
        segments: Int = 1,
    ): Mesh {
        val builder = MeshBuilder()
        val outerBottom = roundedRectRing(width, depth, cornerRadius, segments)
        val outerTop = outerBottom
        val safeBevel = bevel.coerceIn(0f, min(min(width, depth) * 0.15f, height * 0.45f))
        val innerBottom = roundedRectRing(
            width - safeBevel * 2f,
            depth - safeBevel * 2f,
            (cornerRadius - safeBevel).coerceAtLeast(0.001f),
            segments,
        )
        val innerTop = innerBottom
        val yBottom = -height * 0.5f
        val yTop = height * 0.5f
        val byBottom = yBottom + safeBevel
        val byTop = yTop - safeBevel
        val center = V3.ZERO

        fun sideRing(a: List<V2>, ya: Float, b: List<V2>, yb: Float, bevelTop: Boolean, bevelBottom: Boolean) {
            for (i in a.indices) {
                val j = (i + 1) % a.size
                val p0 = V3(a[i].x, ya, a[i].y)
                val p1 = V3(a[j].x, ya, a[j].y)
                val p2 = V3(b[j].x, yb, b[j].y)
                val p3 = V3(b[i].x, yb, b[i].y)
                val mid = V3((a[i].x + a[j].x) * 0.5f, (ya + yb) * 0.5f, (a[i].y + a[j].y) * 0.5f)
                val radial = V3(mid.x - center.x, 0f, mid.z - center.z).normalized()
                val normal = when {
                    bevelTop -> (radial * 0.55f + V3.Y * 0.85f).normalized()
                    bevelBottom -> (radial * 0.55f - V3.Y * 0.85f).normalized()
                    else -> radial
                }
                builder.addQuad(p0, p1, p2, p3, normal)
            }
        }

        if (safeBevel > 0.0005f) {
            sideRing(outerBottom, byBottom, outerTop, byBottom, false, false)
            sideRing(innerBottom, yBottom, outerBottom, byBottom, false, true)
            sideRing(outerTop, byTop, innerTop, yTop, true, false)
        } else {
            sideRing(outerBottom, yBottom, outerTop, yTop, false, false)
        }

        val topRing = if (safeBevel > 0.0005f) innerTop else outerTop
        val bottomRing = if (safeBevel > 0.0005f) innerBottom else outerBottom
        builder.addFace(topRing.map { V3(it.x, yTop, it.y) }, V3.Y)
        builder.addFace(bottomRing.map { V3(it.x, yBottom, it.y) }, -V3.Y)
        return builder.build()
    }

    fun box(width: Float, height: Float, depth: Float) = slab(width, depth, height, 0.001f, 0f, 1)

    fun cylinder(
        radius: Float,
        height: Float,
        segments: Int = 24,
        capTop: Boolean = true,
        capBottom: Boolean = true,
        bevel: Float = 0f,
    ): Mesh {
        val h = height * 0.5f
        val profile = ArrayList<Pair<Float, Float>>()
        val safeBevel = bevel.coerceIn(0f, min(radius * 0.45f, h * 0.45f))
        if (safeBevel > 0.0001f) {
            profile.add(radius - safeBevel to -h)
            profile.add(radius to -h + safeBevel)
            profile.add(radius to h - safeBevel)
            profile.add(radius - safeBevel to h)
        } else {
            profile.add(radius to -h)
            profile.add(radius to h)
        }
        return lathe(profile, segments, capBottom, capTop)
    }

    fun taperedCylinder(
        radiusBottom: Float,
        radiusTop: Float,
        height: Float,
        segments: Int = 32,
        capTop: Boolean = true,
        capBottom: Boolean = true,
    ): Mesh {
        val h = height * 0.5f
        return lathe(listOf(radiusBottom to -h, radiusTop to h), segments, capBottom, capTop)
    }

    fun lathe(
        profile: List<Pair<Float, Float>>,
        segments: Int = 28,
        capBottom: Boolean = false,
        capTop: Boolean = false,
    ): Mesh {
        if (profile.size < 2) return Mesh.EMPTY
        val builder = MeshBuilder()
        val count = segments.coerceAtLeast(3)
        val rings = profile.map { (radius, y) ->
            val points = ArrayList<V3>(count)
            for (i in 0 until count) {
                val angle = 2.0 * PI * i / count
                points.add(V3((radius * cos(angle)).toFloat(), y, (radius * sin(angle)).toFloat()))
            }
            points
        }
        for (p in 0 until profile.size - 1) {
            val r0 = profile[p].first
            val r1 = profile[p + 1].first
            val y0 = profile[p].second
            val y1 = profile[p + 1].second
            val dr = r1 - r0
            val dy = y1 - y0
            val length = sqrt(dr * dr + dy * dy)
            if (length < 1e-5f) continue
            val nr = dy / length
            val ny = -dr / length
            for (i in 0 until count) {
                val j = (i + 1) % count
                val a0 = rings[p][i]
                val a1 = rings[p][j]
                val b0 = rings[p + 1][i]
                val b1 = rings[p + 1][j]
                val angle = 2.0 * PI * (i + 0.5) / count
                val normal = V3(
                    (cos(angle) * nr).toFloat(),
                    ny,
                    (sin(angle) * nr).toFloat(),
                ).normalized()
                val degenerateBottom = r0 < 1e-4f
                val degenerateTop = r1 < 1e-4f
                when {
                    degenerateBottom -> builder.addFace(listOf(a0, b0, b1), normal)
                    degenerateTop -> builder.addFace(listOf(a0, a1, b0), normal)
                    else -> builder.addQuad(a0, a1, b1, b0, normal)
                }
            }
        }
        if (capBottom && profile.first().first > 1e-4f) {
            builder.addFace(rings.first(), -V3.Y)
        }
        if (capTop && profile.last().first > 1e-4f) {
            builder.addFace(rings.last(), V3.Y)
        }
        return builder.build()
    }

    fun hollowCylinder(
        outerRadius: Float,
        innerRadius: Float,
        height: Float,
        segments: Int = 24,
    ): Mesh {
        val builder = MeshBuilder()
        val count = segments.coerceAtLeast(3)
        val h = height * 0.5f
        val inner = innerRadius.coerceIn(0.001f, outerRadius * 0.98f)
        val outerBottom = ArrayList<V3>(count)
        val outerTop = ArrayList<V3>(count)
        val innerBottom = ArrayList<V3>(count)
        val innerTop = ArrayList<V3>(count)
        for (i in 0 until count) {
            val a = 2.0 * PI * i / count
            val x = cos(a).toFloat()
            val z = sin(a).toFloat()
            outerBottom.add(V3(x * outerRadius, -h, z * outerRadius))
            outerTop.add(V3(x * outerRadius, h, z * outerRadius))
            innerBottom.add(V3(x * inner, -h, z * inner))
            innerTop.add(V3(x * inner, h, z * inner))
        }
        for (i in 0 until count) {
            val j = (i + 1) % count
            val angle = 2.0 * PI * (i + 0.5) / count
            val outward = V3(cos(angle).toFloat(), 0f, sin(angle).toFloat())
            builder.addQuad(outerBottom[i], outerBottom[j], outerTop[j], outerTop[i], outward)
            builder.addQuad(innerBottom[i], innerTop[i], innerTop[j], innerBottom[j], -outward)
            builder.addQuad(outerTop[i], outerTop[j], innerTop[j], innerTop[i], V3.Y)
            builder.addQuad(outerBottom[i], innerBottom[i], innerBottom[j], outerBottom[j], -V3.Y)
        }
        return builder.build()
    }

    fun annulus(outerRadius: Float, innerRadius: Float, thickness: Float, segments: Int = 40): Mesh {
        return hollowCylinder(outerRadius, innerRadius, thickness, segments)
    }

    fun arcShell(
        outerRadius: Float,
        innerRadius: Float,
        height: Float,
        segments: Int = 20,
        startAngle: Float = 0f,
        sweepAngle: Float = 250f,
        capEnds: Boolean = true,
    ): Mesh {
        val builder = MeshBuilder()
        val count = segments.coerceAtLeast(2)
        val h = height * 0.5f
        val inner = innerRadius.coerceIn(0.001f, outerRadius * 0.98f)
        val step = sweepAngle / count
        val outerBottom = ArrayList<V3>(count + 1)
        val outerTop = ArrayList<V3>(count + 1)
        val innerBottom = ArrayList<V3>(count + 1)
        val innerTop = ArrayList<V3>(count + 1)
        for (i in 0..count) {
            val angle = Math.toRadians((startAngle + step * i).toDouble())
            val x = cos(angle).toFloat()
            val z = sin(angle).toFloat()
            outerBottom.add(V3(x * outerRadius, -h, z * outerRadius))
            outerTop.add(V3(x * outerRadius, h, z * outerRadius))
            innerBottom.add(V3(x * inner, -h, z * inner))
            innerTop.add(V3(x * inner, h, z * inner))
        }
        for (i in 0 until count) {
            val angle = Math.toRadians((startAngle + step * (i + 0.5)).toDouble())
            val outward = V3(cos(angle).toFloat(), 0f, sin(angle).toFloat())
            builder.addQuad(outerBottom[i], outerBottom[i + 1], outerTop[i + 1], outerTop[i], outward)
            builder.addQuad(innerBottom[i], innerTop[i], innerTop[i + 1], innerBottom[i + 1], -outward)
            builder.addQuad(outerTop[i], outerTop[i + 1], innerTop[i + 1], innerTop[i], V3.Y)
            builder.addQuad(outerBottom[i], innerBottom[i], innerBottom[i + 1], outerBottom[i + 1], -V3.Y)
        }
        if (capEnds) {
            builder.addQuad(outerBottom[0], innerBottom[0], innerTop[0], outerTop[0])
            builder.addQuad(outerBottom[count], outerTop[count], innerTop[count], innerBottom[count])
        }
        return builder.build()
    }

    fun torus(
        majorRadius: Float,
        minorRadius: Float,
        majorSegments: Int = 28,
        minorSegments: Int = 6,
    ): Mesh {
        val builder = MeshBuilder()
        val major = majorSegments.coerceAtLeast(3)
        val minor = minorSegments.coerceAtLeast(3)
        val grid = Array(major) { i ->
            Array(minor) { j ->
                val theta = 2.0 * PI * i / major
                val phi = 2.0 * PI * j / minor
                val r = majorRadius + minorRadius * cos(phi)
                V3((r * cos(theta)).toFloat(), (minorRadius * sin(phi)).toFloat(), (r * sin(theta)).toFloat())
            }
        }
        for (i in 0 until major) {
            for (j in 0 until minor) {
                val ni = (i + 1) % major
                val nj = (j + 1) % minor
                val theta = 2.0 * PI * (i + 0.5) / major
                val phi = 2.0 * PI * (j + 0.5) / minor
                val normal = V3(
                    (cos(phi) * cos(theta)).toFloat(),
                    sin(phi).toFloat(),
                    (cos(phi) * sin(theta)).toFloat(),
                ).normalized()
                builder.addQuad(grid[i][j], grid[ni][j], grid[ni][nj], grid[i][nj], normal)
            }
        }
        return builder.build()
    }

    fun sphere(radius: Float, segments: Int = 18, rings: Int = 9): Mesh {
        val builder = MeshBuilder()
        val seg = segments.coerceAtLeast(3)
        val rng = rings.coerceAtLeast(2)
        val grid = Array(rng + 1) { r ->
            val phi = PI * r / rng
            Array(seg) { s ->
                val theta = 2.0 * PI * s / seg
                V3(
                    (radius * sin(phi) * cos(theta)).toFloat(),
                    (radius * cos(phi)).toFloat(),
                    (radius * sin(phi) * sin(theta)).toFloat(),
                )
            }
        }
        for (r in 0 until rng) {
            for (s in 0 until seg) {
                val ns = (s + 1) % seg
                val a = grid[r][s]
                val b = grid[r][ns]
                val c = grid[r + 1][ns]
                val d = grid[r + 1][s]
                val mid = ((a + b + c + d) * 0.25f).normalized()
                if (r == 0) {
                    builder.addFace(listOf(a, c, d), mid)
                } else if (r == rng - 1) {
                    builder.addFace(listOf(a, b, c), mid)
                } else {
                    builder.addQuad(a, b, c, d, mid)
                }
            }
        }
        return builder.build()
    }

    fun prism(points: List<V2>, height: Float, bevel: Float = 0f): Mesh {
        if (points.size < 3) return Mesh.EMPTY
        val builder = MeshBuilder()
        val h = height * 0.5f
        val safeBevel = bevel.coerceIn(0f, min(height * 0.4f, 0.12f))
        val centroid = V2(points.sumOf { it.x.toDouble() }.toFloat() / points.size, points.sumOf { it.y.toDouble() }.toFloat() / points.size)
        fun inset(point: V2, amount: Float): V2 {
            val direction = point - centroid
            val length = direction.length()
            if (length < 1e-5f) return point
            val scale = max(0f, (length - amount) / length)
            return centroid + direction * scale
        }
        val bottom = points.map { V3(it.x, -h, it.y) }
        val top = points.map { V3(it.x, h, it.y) }
        if (safeBevel > 0.0001f) {
            val outerTop = points.map { V3(it.x, h - safeBevel, it.y) }
            val innerTop = points.map { inset(it, safeBevel) }.map { V3(it.x, h, it.y) }
            val outerBottom = points.map { V3(it.x, -h + safeBevel, it.y) }
            val innerBottom = points.map { inset(it, safeBevel) }.map { V3(it.x, -h, it.y) }
            for (i in points.indices) {
                val j = (i + 1) % points.size
                val mid = (points[i] + points[j]) * 0.5f
                val outward = (mid - centroid).let { V3(it.x, 0f, it.y).normalized() }
                builder.addQuad(bottom[i], bottom[j], outerTop[j], outerTop[i], outward)
                builder.addQuad(outerTop[i], outerTop[j], innerTop[j], innerTop[i], (outward * 0.6f + V3.Y * 0.8f).normalized())
                builder.addQuad(outerBottom[i], innerBottom[i], innerBottom[j], outerBottom[j], (outward * 0.6f - V3.Y * 0.8f).normalized())
            }
            builder.addFace(innerTop, V3.Y)
            builder.addFace(innerBottom, -V3.Y)
        } else {
            for (i in points.indices) {
                val j = (i + 1) % points.size
                val mid = (points[i] + points[j]) * 0.5f
                val outward = (mid - centroid).let { V3(it.x, 0f, it.y).normalized() }
                builder.addQuad(bottom[i], bottom[j], top[j], top[i], outward)
            }
            builder.addFace(top, V3.Y)
            builder.addFace(bottom, -V3.Y)
        }
        return builder.build()
    }

    fun gear(
        radius: Float,
        teeth: Int,
        thickness: Float,
        toothDepth: Float,
        segmentsPerTooth: Int = 4,
    ): Mesh {
        val count = teeth.coerceAtLeast(6)
        val points = ArrayList<V2>()
        for (tooth in 0 until count) {
            val base = 2.0 * PI * tooth / count
            val step = 2.0 * PI / count / segmentsPerTooth
            for (s in 0 until segmentsPerTooth) {
                val angle = base + step * s
                val isTooth = s == 0 || s == segmentsPerTooth - 1
                val r = if (isTooth) radius + toothDepth else radius
                points.add(V2((r * cos(angle)).toFloat(), (r * sin(angle)).toFloat()))
            }
        }
        return prism(points, thickness, min(0.02f, thickness * 0.15f))
    }

    fun roundedBlade(
        span: Float,
        rootChord: Float,
        tipChord: Float,
        rootThickness: Float,
        tipThickness: Float,
        twistDegrees: Float,
        sweep: Float,
        sections: Int = 5,
        perimeter: Int = 7,
    ): Mesh {
        val builder = MeshBuilder()
        val rings = ArrayList<List<V3>>()
        val count = sections.coerceAtLeast(2)
        for (s in 0 until count) {
            val t = s.toFloat() / (count - 1)
            val y = span * t
            val chord = rootChord + (tipChord - rootChord) * t
            val thickness = rootThickness + (tipThickness - rootThickness) * t
            val twist = Math.toRadians((twistDegrees * t).toDouble())
            val zShift = sweep * t * t
            val ring = ArrayList<V3>(perimeter)
            for (p in 0 until perimeter) {
                val u = p.toFloat() / perimeter
                val shape = airfoil(u)
                val localX = shape.first * thickness
                val localZ = shape.second * chord
                val rotatedX = (localX * cos(twist) - localZ * sin(twist)).toFloat()
                val rotatedZ = (localX * sin(twist) + localZ * cos(twist)).toFloat()
                ring.add(V3(rotatedX, y, rotatedZ + zShift))
            }
            rings.add(ring)
        }
        for (s in 0 until count - 1) {
            for (p in 0 until perimeter) {
                val np = (p + 1) % perimeter
                val a = rings[s][p]
                val b = rings[s][np]
                val c = rings[s + 1][np]
                val d = rings[s + 1][p]
                val outward = (c - a).cross(d - b)
                builder.addQuad(a, b, c, d, if (outward.lengthSq() > 1e-8f) outward else V3.Y)
            }
        }
        builder.addFace(rings.first(), -V3.Y)
        builder.addFace(rings.last(), V3.Y)
        return builder.build()
    }

    private fun airfoil(u: Float): Pair<Float, Float> {
        val angle = u * 2.0 * PI
        val chord = cos(angle).toFloat()
        val thickness = sin(angle).toFloat() * (0.55f + 0.45f * (1f - kotlin.math.abs(chord)))
        return thickness to chord * 0.5f
    }

    fun tube(
        controlPoints: List<V3>,
        radius: Float,
        radialSegments: Int = 8,
        endRadius: Float = radius,
        subdivide: Int = 4,
        caps: Boolean = true,
    ): Mesh {
        if (controlPoints.size < 2) return Mesh.EMPTY
        val path = smoothPath(controlPoints, subdivide)
        if (path.size < 2) return Mesh.EMPTY
        val builder = MeshBuilder()
        val count = radialSegments.coerceAtLeast(3)
        val frames = ArrayList<Pair<V3, V3>>(path.size)
        var previousNormal = V3.ZERO
        for (i in path.indices) {
            val tangent = when (i) {
                0 -> path[1] - path[0]
                path.lastIndex -> path[i] - path[i - 1]
                else -> path[i + 1] - path[i - 1]
            }.normalized()
            val reference = if (previousNormal.lengthSq() < 0.1f) {
                if (kotlin.math.abs(tangent.y) < 0.9f) V3.Y else V3.X
            } else {
                previousNormal
            }
            val normal = (reference - tangent * reference.dot(tangent)).normalized()
            previousNormal = normal
            frames.add(tangent to normal)
        }
        val rings = ArrayList<List<V3>>(path.size)
        for (i in path.indices) {
            val (tangent, normal) = frames[i]
            val binormal = tangent.cross(normal).normalized()
            val t = i.toFloat() / (path.size - 1)
            val r = radius + (endRadius - radius) * t
            val ring = ArrayList<V3>(count)
            for (j in 0 until count) {
                val angle = 2.0 * PI * j / count
                val offset = normal * (cos(angle) * r).toFloat() + binormal * (sin(angle) * r).toFloat()
                ring.add(path[i] + offset)
            }
            rings.add(ring)
        }
        for (i in 0 until rings.size - 1) {
            val (tangent, normal) = frames[i]
            val binormal = tangent.cross(normal).normalized()
            for (j in 0 until count) {
                val nj = (j + 1) % count
                val angle = 2.0 * PI * (j + 0.5) / count
                val n = normal * cos(angle).toFloat() + binormal * sin(angle).toFloat()
                builder.addQuad(rings[i][j], rings[i][nj], rings[i + 1][nj], rings[i + 1][j], n)
            }
        }
        if (caps) {
            builder.addFace(rings.first(), -frames.first().first)
            builder.addFace(rings.last(), frames.last().first)
        }
        return builder.build()
    }

    fun helix(
        radius: Float,
        wireRadius: Float,
        height: Float,
        turns: Int,
        samplesPerTurn: Int = 14,
    ): Mesh {
        val points = ArrayList<V3>()
        val total = turns * samplesPerTurn
        for (i in 0..total) {
            val t = i.toFloat() / total
            val angle = 2.0 * PI * turns * t
            points.add(V3((radius * cos(angle)).toFloat(), -height * 0.5f + height * t, (radius * sin(angle)).toFloat()))
        }
        return tube(points, wireRadius, 6, subdivide = 1, caps = false)
    }

    fun hexBolt(radius: Float, height: Float): Mesh {
        val builder = MeshBuilder()
        builder.addMesh(cylinder(radius, height * 0.55f, 6, bevel = radius * 0.12f))
        return builder.build()
    }

    fun smoothPath(points: List<V3>, subdivisions: Int): List<V3> {
        if (points.size < 2) return points
        if (subdivisions <= 1 || points.size < 3) return points
        val result = ArrayList<V3>((points.size - 1) * subdivisions + 1)
        for (i in 0 until points.size - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
            val steps = if (i == points.size - 2) subdivisions else subdivisions - 1
            for (step in 0 until steps) {
                val t = step.toFloat() / (subdivisions - 1).coerceAtLeast(1)
                val t2 = t * t
                val t3 = t2 * t
                val point = (p1 * 2f + (p2 - p0) * t + (p0 * 2f - p1 * 5f + p2 * 4f - p3) * t2 + (p1 * 3f - p0 - p2 * 3f + p3) * t3) * 0.5f
                result.add(point)
            }
        }
        result.add(points.last())
        return result
    }

    fun transformBetweenY(start: V3, end: V3): Mat4 {
        val direction = end - start
        val length = direction.length()
        if (length < 1e-5f) return Mat4.translation(start)
        val dir = direction * (1f / length)
        val dot = V3.Y.dot(dir).coerceIn(-1f, 1f)
        val axis = V3.Y.cross(dir)
        val rotation = if (axis.lengthSq() < 1e-8f) {
            if (dot > 0f) Mat4.identity() else Mat4.rotateX(180f)
        } else {
            Mat4.rotate(axis, Math.toDegrees(acos(dot.toDouble())).toFloat())
        }
        return Mat4.translation(start + direction * 0.5f).multiply(rotation)
    }

    fun cylinderBetween(start: V3, end: V3, radius: Float, segments: Int = 16): Mesh {
        val length = (end - start).length()
        if (length < 1e-5f) return Mesh.EMPTY
        return cylinder(radius, length, segments).transformed(transformBetweenY(start, end))
    }

    fun boltSet(points: List<V3>, radius: Float, height: Float, axis: V3 = V3.Y): Mesh {
        val builder = MeshBuilder()
        val base = hexBolt(radius, height)
        points.forEach { point ->
            val rotation = when {
                axis.dot(V3.Y) > 0.99f -> Mat4.identity()
                axis.dot(V3.Y) < -0.99f -> Mat4.rotateX(180f)
                else -> Mat4.rotate(V3.Y.cross(axis), Math.toDegrees(acos(V3.Y.dot(axis.normalized()).toDouble())).toFloat())
            }
            builder.addMesh(base, Mat4.translation(point).multiply(rotation))
        }
        return builder.build()
    }

    fun merge(vararg meshes: Mesh): Mesh {
        val builder = MeshBuilder()
        meshes.forEach { builder.addMesh(it) }
        return builder.build()
    }
}
