package studio.anatomy.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.render.RenderQuality
import studio.anatomy.app.render.RenderRequest
import studio.anatomy.app.render.SceneRenderer
import studio.anatomy.app.ui.theme.AnatomyType
import studio.anatomy.app.ui.theme.LocalAnatomyColors

/**
 * Renders a real 3D plate on a background thread and caches it. Thumbnails are not
 * screenshots of the UI; they are independent, full-quality renders of the object.
 */
object RenderPlateCache {
    private val cache = LruCache<String, Bitmap>(12)

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun key(spec: ObjectSpec, width: Int, height: Int, quality: RenderQuality): String =
        "${spec.id}:${width}x${height}:${quality.name}"
}

suspend fun renderPlate(
    spec: ObjectSpec,
    width: Int,
    height: Int,
    quality: RenderQuality,
): Bitmap = withContext(Dispatchers.Default) {
    val scene = AnatomyCatalog.scene(spec)
    val surface = Bitmap.createBitmap(width.coerceAtLeast(2), height.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
    val request = RenderRequest(
        width = surface.width,
        height = surface.height,
        pose = spec.thumbnailPose,
        anim = AnimContext(
            ambientTime = spec.thumbnailTime,
            machineTime = spec.thumbnailTime * 2.2f,
            control = spec.thumbnailControl,
        ),
        quality = quality,
        showGrid = true,
        safeTop = surface.height * 0.07f,
        safeBottom = surface.height * 0.07f,
        safeSide = surface.width * 0.035f,
        density = 1.35f,
        cacheLayers = false,
    )
    SceneRenderer().draw(Canvas(surface), scene, request)
    surface
}

@Composable
fun ModelPlate(
    spec: ObjectSpec,
    widthPx: Int,
    heightPx: Int,
    quality: RenderQuality,
    modifier: Modifier = Modifier,
    showGridLines: Boolean = true,
) {
    val colors = LocalAnatomyColors.current
    val cacheKey = RenderPlateCache.key(spec, widthPx, heightPx, quality)
    var bitmap by remember(cacheKey) { mutableStateOf(RenderPlateCache.get(cacheKey)) }
    var image by remember(bitmap) { mutableStateOf(bitmap?.asImageBitmap()) }

    LaunchedEffect(cacheKey) {
        if (bitmap != null) return@LaunchedEffect
        val rendered = renderPlate(spec, widthPx, heightPx, quality)
        RenderPlateCache.put(cacheKey, rendered)
        withContext(Dispatchers.Main) {
            bitmap = rendered
            image = rendered.asImageBitmap()
        }
    }

    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))) {
        val loaded = image
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = "${spec.title} 3D plate",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.surface, colors.inkDeep)
                        )
                    )
            ) {
                ComposeCanvas(Modifier.fillMaxSize()) {
                    val step = 34.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(colors.lineSoft, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(colors.lineSoft, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
                        y += step
                    }
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.35f),
                        radius = size.minDimension * 0.11f,
                        center = center,
                        style = Stroke(width = 1.4f),
                    )
                    drawCircle(colors.accent.copy(alpha = 0.8f), 3.2f, center)
                }
                Text(
                    "RENDERING PLATE",
                    color = colors.textTertiary,
                    style = AnatomyType.monoMicro,
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                )
            }
        }
    }
}
