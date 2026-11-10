package studio.anatomy.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import studio.anatomy.app.R
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.core.cameraBasis
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.model.Part
import studio.anatomy.app.render.BloomPass
import studio.anatomy.app.render.ColorMath
import studio.anatomy.app.render.FaceMode
import studio.anatomy.app.render.RenderQuality
import studio.anatomy.app.render.RenderRequest
import studio.anatomy.app.render.SceneRenderer
import studio.anatomy.app.render.Shading
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PlateResult(
    val fileUri: Uri,
    val galleryUri: Uri?,
    val fileName: String,
    val inGallery: Boolean,
)

object PlateExporter {

    suspend fun exportPng(
        context: Context,
        spec: ObjectSpec,
        scene: CompiledScene,
        pose: CameraPose,
        anim: AnimContext,
        explode: Map<String, Float>,
        selectedSystem: String?,
        xray: Boolean,
        longEdge: Int,
    ): PlateResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val width = longEdge
        val height = (longEdge * 0.625f).roundToInt()
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            // Two step-downs so a large export plate still succeeds on a 3 GB phone.
            try {
                Bitmap.createBitmap(2560, 1600, Bitmap.Config.ARGB_8888)
            } catch (second: OutOfMemoryError) {
                Bitmap.createBitmap(1920, 1200, Bitmap.Config.ARGB_8888)
            }
        }
        val renderer = SceneRenderer()
        val request = RenderRequest(
            width = bitmap.width,
            height = bitmap.height,
            pose = pose,
            anim = anim,
            explode = explode,
            selectedSystem = selectedSystem,
            xray = xray,
            showGrid = true,
            quality = RenderQuality.FULL,
            safeTop = bitmap.height * 0.075f,
            safeBottom = bitmap.height * 0.215f,
            safeSide = bitmap.width * 0.052f,
            density = 2.1f,
            cacheLayers = false,
        )
        renderer.draw(Canvas(bitmap), scene, request)
        BloomPass.apply(bitmap, strength = 0.42f)
        drawPlateCaption(context, bitmap, spec)
        val fileName = "anatomy_${spec.id}_${stamp()}.png"
        val file = writeCacheFile(context, fileName, "image/png") { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        val gallery = insertGallery(context, fileName, "image/png", "image/png") { out ->
            // Re-encode only if needed; use the cached file contents.
            file.inputStream().use { it.copyTo(out) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        PlateResult(uri, gallery, fileName, gallery != null)
    }

    suspend fun exportSvg(
        context: Context,
        spec: ObjectSpec,
        scene: CompiledScene,
        pose: CameraPose,
        anim: AnimContext,
        explode: Map<String, Float>,
        selectedSystem: String?,
        xray: Boolean,
    ): PlateResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val svg = SvgPlateWriter.write(spec, scene, pose, anim, explode, selectedSystem, xray)
        val fileName = "anatomy_${spec.id}_${stamp()}.svg"
        val file = writeCacheFile(context, fileName, "image/svg+xml") { out ->
            out.write(svg.toByteArray(Charsets.UTF_8))
        }
        val gallery = insertGallery(context, fileName, "image/svg+xml", "image/svg+xml") { out ->
            out.write(svg.toByteArray(Charsets.UTF_8))
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        PlateResult(uri, gallery, fileName, gallery != null)
    }

    fun saveGeneratedImage(context: Context, bitmap: Bitmap, prefix: String = "ai"): PlateResult {
        val fileName = "anatomy_${prefix}_${stamp()}.png"
        val file = writeCacheFile(context, fileName, "image/png") { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val gallery = insertGallery(context, fileName, "image/png", "image/png") { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return PlateResult(uri, gallery, fileName, gallery != null)
    }

    fun share(context: Context, result: PlateResult, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (result.fileName.endsWith(".svg")) "image/svg+xml" else "image/png"
            putExtra(Intent.EXTRA_STREAM, result.fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title — an Anatomy Studio plate.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share plate").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun writeCacheFile(
        context: Context,
        name: String,
        mime: String,
        write: (FileOutputStream) -> Unit,
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, name)
        FileOutputStream(file).use(write)
        file.setReadable(true, false)
        return file
    }

    private fun insertGallery(
        context: Context,
        name: String,
        mime: String,
        relativeType: String,
        write: (java.io.OutputStream) -> Unit,
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val collection = if (mime == "image/svg+xml") {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val path = if (mime == "image/svg+xml") {
                "${Environment.DIRECTORY_DOWNLOADS}/Anatomy Studio"
            } else {
                "${Environment.DIRECTORY_PICTURES}/Anatomy Studio"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use(write)
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } catch (error: Exception) {
            android.util.Log.w("AnatomyExport", "MediaStore insert failed: ${error.javaClass.simpleName}")
            null
        }
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun drawPlateCaption(context: Context, bitmap: Bitmap, spec: ObjectSpec) {
        val canvas = Canvas(bitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val accent = spec.accent
        val paper = 0xFFEDF2ED.toInt()
        val secondary = 0xFF93A7A9.toInt()
        val mono = ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
        val display = ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.SANS_SERIF

        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.62f, 0f, h,
                intArrayOf(AndroidColor.TRANSPARENT, 0xE6050A0E.toInt(), 0xFF050A0E.toInt()),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, h * 0.6f, w, h, footer)

        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, w * 0.0007f)
            color = 0x3320313A
        }
        canvas.drawRect(w * 0.035f, h * 0.04f, w * 0.965f, h * 0.96f, frame)

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(mono, Typeface.NORMAL)
            textSize = w * 0.0105f
            letterSpacing = 0.12f
            color = accent
        }
        canvas.drawText("ANATOMY / 3D STUDIO", w * 0.055f, h * 0.082f, label)
        label.color = secondary
        label.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "OBJECT ${spec.index.toString().padStart(2, '0')} / ${spec.category}",
            w * 0.945f,
            h * 0.082f,
            label,
        )
        label.textAlign = Paint.Align.LEFT
        label.textSize = w * 0.0084f
        label.color = (secondary and 0x00FFFFFF) or (190 shl 24)
        canvas.drawText(spec.designation, w * 0.055f, h * 0.108f, label)
        label.textAlign = Paint.Align.RIGHT
        canvas.drawText("NATIVE 3D PLATE", w * 0.945f, h * 0.108f, label)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(display, Typeface.NORMAL)
            textSize = w * 0.066f
            color = paper
            letterSpacing = -0.025f
        }
        canvas.drawText(spec.title, w * 0.055f, h * 0.842f, title)

        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRect(w * 0.055f, h * 0.866f, w * 0.095f, h * 0.869f, rule)

        val category = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(mono, Typeface.NORMAL)
            textSize = w * 0.0098f
            letterSpacing = 0.14f
            color = accent
        }
        canvas.drawText(spec.category, w * 0.055f, h * 0.893f, category)

        val tagline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(display, Typeface.NORMAL)
            textSize = w * 0.0126f
            color = secondary
        }
        canvas.drawText(spec.tagline, w * 0.055f, h * 0.918f, tagline)

        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(mono, Typeface.NORMAL)
            textSize = w * 0.0088f
            letterSpacing = 0.1f
            color = (secondary and 0x00FFFFFF) or (210 shl 24)
        }
        canvas.drawText(
            "${spec.partCount} PARTS  /  ${spec.systemNames.size} SYSTEMS  /  ${spec.specs.firstOrNull()?.label ?: "STUDY"}",
            w * 0.055f,
            h * 0.949f,
            meta,
        )
        meta.textAlign = Paint.Align.RIGHT
        canvas.drawText("GENERATED ON DEVICE", w * 0.945f, h * 0.949f, meta)

        val footRule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x3320313A.toInt() }
        canvas.drawRect(w * 0.055f, h * 0.963f, w * 0.945f, h * 0.9635f, footRule)
    }
}
