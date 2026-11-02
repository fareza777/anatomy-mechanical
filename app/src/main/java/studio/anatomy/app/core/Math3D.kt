package studio.anatomy.app.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class V2(val x: Float, val y: Float) {
    operator fun plus(other: V2) = V2(x + other.x, y + other.y)
    operator fun minus(other: V2) = V2(x - other.x, y - other.y)
    operator fun times(value: Float) = V2(x * value, y * value)
    fun length() = sqrt(x * x + y * y)
}

data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: V3) = V3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: V3) = V3(x - other.x, y - other.y, z - other.z)
    operator fun times(value: Float) = V3(x * value, y * value, z * value)
    operator fun unaryMinus() = V3(-x, -y, -z)

    fun dot(other: V3) = x * other.x + y * other.y + z * other.z
    fun cross(other: V3) = V3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun length() = sqrt(x * x + y * y + z * z)
    fun lengthSq() = x * x + y * y + z * z
    fun normalized(): V3 {
        val length = length()
        return if (length < 1e-6f) ZERO else this * (1f / length)
    }

    fun lerp(other: V3, t: Float) = V3(
        x + (other.x - x) * t,
        y + (other.y - y) * t,
        z + (other.z - z) * t,
    )

    fun abs() = V3(kotlin.math.abs(x), kotlin.math.abs(y), kotlin.math.abs(z))

    companion object {
        val ZERO = V3(0f, 0f, 0f)
        val ONE = V3(1f, 1f, 1f)
        val X = V3(1f, 0f, 0f)
        val Y = V3(0f, 1f, 0f)
        val Z = V3(0f, 0f, 1f)
    }
}

data class Aabb(val min: V3, val max: V3) {
    val center: V3 get() = (min + max) * 0.5f
    val size: V3 get() = max - min
    val radius: Float get() = size.length() * 0.5f

    fun expand(point: V3) = Aabb(
        V3(min.x.coerceAtMost(point.x), min.y.coerceAtMost(point.y), min.z.coerceAtMost(point.z)),
        V3(max.x.coerceAtLeast(point.x), max.y.coerceAtLeast(point.y), max.z.coerceAtLeast(point.z)),
    )

    fun union(other: Aabb) = Aabb(
        V3(min.x.coerceAtMost(other.min.x), min.y.coerceAtMost(other.min.y), min.z.coerceAtMost(other.min.z)),
        V3(max.x.coerceAtLeast(other.max.x), max.y.coerceAtLeast(other.max.y), max.z.coerceAtLeast(other.max.z)),
    )

    fun translated(offset: V3) = Aabb(min + offset, max + offset)

    fun corners(): List<V3> = listOf(
        V3(min.x, min.y, min.z), V3(max.x, min.y, min.z),
        V3(min.x, max.y, min.z), V3(max.x, max.y, min.z),
        V3(min.x, min.y, max.z), V3(max.x, min.y, max.z),
        V3(min.x, max.y, max.z), V3(max.x, max.y, max.z),
    )

    companion object {
        fun of(points: List<V3>): Aabb {
            if (points.isEmpty()) return Aabb(V3.ZERO, V3.ZERO)
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE
            points.forEach {
                minX = min(minX, it.x); minY = min(minY, it.y); minZ = min(minZ, it.z)
                maxX = max(maxX, it.x); maxY = max(maxY, it.y); maxZ = max(maxZ, it.z)
            }
            return Aabb(V3(minX, minY, minZ), V3(maxX, maxY, maxZ))
        }

        fun union(a: List<Aabb>): Aabb = a.reduce { acc, next -> acc.union(next) }
    }
}

class Mat4(val m: FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)) {
    fun multiply(other: Mat4): Mat4 {
        val out = FloatArray(16)
        val a = m
        val b = other.m
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                var value = 0f
                for (k in 0 until 4) value += a[row * 4 + k] * b[k * 4 + col]
                out[row * 4 + col] = value
            }
        }
        return Mat4(out)
    }

    fun transform(point: V3) = V3(
        m[0] * point.x + m[1] * point.y + m[2] * point.z + m[3],
        m[4] * point.x + m[5] * point.y + m[6] * point.z + m[7],
        m[8] * point.x + m[9] * point.y + m[10] * point.z + m[11],
    )

    fun transformDirection(direction: V3) = V3(
        m[0] * direction.x + m[1] * direction.y + m[2] * direction.z,
        m[4] * direction.x + m[5] * direction.y + m[6] * direction.z,
        m[8] * direction.x + m[9] * direction.y + m[10] * direction.z,
    )

    fun translation(): V3 = V3(m[3], m[7], m[11])

    fun copy() = Mat4(m.copyOf())

    companion object {
        fun identity() = Mat4()

        fun translation(value: V3) = Mat4(floatArrayOf(
            1f, 0f, 0f, value.x,
            0f, 1f, 0f, value.y,
            0f, 0f, 1f, value.z,
            0f, 0f, 0f, 1f,
        ))

        fun translation(x: Float, y: Float, z: Float) = translation(V3(x, y, z))

        fun scale(value: V3) = Mat4(floatArrayOf(
            value.x, 0f, 0f, 0f,
            0f, value.y, 0f, 0f,
            0f, 0f, value.z, 0f,
            0f, 0f, 0f, 1f,
        ))

        fun scale(value: Float) = scale(V3(value, value, value))

        fun rotateX(degrees: Float): Mat4 {
            val r = Math.toRadians(degrees.toDouble())
            val c = cos(r).toFloat()
            val s = sin(r).toFloat()
            return Mat4(floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, c, -s, 0f,
                0f, s, c, 0f,
                0f, 0f, 0f, 1f,
            ))
        }

        fun rotateY(degrees: Float): Mat4 {
            val r = Math.toRadians(degrees.toDouble())
            val c = cos(r).toFloat()
            val s = sin(r).toFloat()
            return Mat4(floatArrayOf(
                c, 0f, s, 0f,
                0f, 1f, 0f, 0f,
                -s, 0f, c, 0f,
                0f, 0f, 0f, 1f,
            ))
        }

        fun rotateZ(degrees: Float): Mat4 {
            val r = Math.toRadians(degrees.toDouble())
            val c = cos(r).toFloat()
            val s = sin(r).toFloat()
            return Mat4(floatArrayOf(
                c, -s, 0f, 0f,
                s, c, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ))
        }

        fun rotate(axis: V3, degrees: Float): Mat4 {
            val a = axis.normalized()
            if (a.lengthSq() < 0.5f) return identity()
            val r = Math.toRadians(degrees.toDouble())
            val c = cos(r).toFloat()
            val s = sin(r).toFloat()
            val t = 1f - c
            val x = a.x
            val y = a.y
            val z = a.z
            return Mat4(floatArrayOf(
                t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0f,
                t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0f,
                t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0f,
                0f, 0f, 0f, 1f,
            ))
        }

        fun compose(vararg matrices: Mat4): Mat4 {
            var result = identity()
            matrices.forEach { result = result.multiply(it) }
            return result
        }
    }
}

fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
    if (edge0 == edge1) return if (value < edge0) 0f else 1f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

fun approx(a: Float, b: Float, epsilon: Float = 1e-4f) = abs(a - b) <= epsilon

fun wrapDegrees(value: Float): Float {
    var result = value % 360f
    if (result < 0f) result += 360f
    return result
}

fun shortestAngle(from: Float, to: Float): Float {
    var delta = (to - from) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}
