package studio.anatomy.app.ui.studio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint as NativePaint
import android.graphics.Rect
import android.graphics.RectF
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import studio.anatomy.app.core.AnimContextFactory
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.data.AnatomyPrefs
import studio.anatomy.app.data.LocalStudioSettings
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.Hotspot
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.render.HotspotProjector
import studio.anatomy.app.render.ProjectedHotspot
import studio.anatomy.app.render.RenderQuality
import studio.anatomy.app.render.RenderRequest
import studio.anatomy.app.render.SceneRenderer
import studio.anatomy.app.ui.components.CornerTicks
import studio.anatomy.app.ui.components.GlassIconButton
import studio.anatomy.app.ui.components.GlassPanel
import studio.anatomy.app.ui.components.Hairline
import studio.anatomy.app.ui.components.MonoLabel
import studio.anatomy.app.ui.components.SheetSystemBars
import studio.anatomy.app.ui.theme.AnatomyType
import studio.anatomy.app.ui.theme.JetBrainsMono
import studio.anatomy.app.ui.theme.LocalAnatomyColors
import studio.anatomy.app.ui.theme.SpaceGrotesk
import kotlin.math.hypot
import kotlin.math.roundToInt

class StudioState(
    val spec: ObjectSpec,
    private val scope: CoroutineScope,
    startPlaying: Boolean = true,
    showGridDefault: Boolean = true,
    showPinsDefault: Boolean = true,
    val hapticsAllowed: Boolean = true,
) {
    var pose by mutableStateOf(spec.defaultPose)
    var explodeTarget by mutableFloatStateOf(0f)
    var explodedAssembly by mutableStateOf<String?>(null)
    var selectedSystem by mutableStateOf<String?>(null)
    var xray by mutableStateOf(false)
    var mode by mutableStateOf(StudioMode.EXPLORE)
    var activeStep by mutableIntStateOf(-1)
    var control by mutableFloatStateOf(spec.control.defaultValue)
    var playbackSpeed by mutableFloatStateOf(1f)
    var playing by mutableStateOf(startPlaying)
    var showPins by mutableStateOf(showPinsDefault)
    var showGrid by mutableStateOf(showGridDefault)
    var selectedHotspot by mutableStateOf<Hotspot?>(null)
    var resetClockRequest by mutableIntStateOf(0)
    var toast by mutableStateOf<String?>(null)
    var showInfoSheet by mutableStateOf(false)

    private var cameraJob: Job? = null

    fun enterMode(next: StudioMode) {
        mode = next
        when (next) {
            StudioMode.EXPLORE -> {
                selectedHotspot = null
                xray = false
            }
            StudioMode.CUTAWAY -> {
                xray = true
                selectedHotspot = null
            }
            StudioMode.EXPLODE -> {
                selectedHotspot = null
                xray = false
                if (explodeTarget < 0.08f) explodeTarget = 0.66f
            }
            StudioMode.SIMULATE -> {
                selectedHotspot = null
                xray = false
                playing = true
            }
            StudioMode.LEARN -> {
                selectedHotspot = null
                xray = false
                playing = true
            }
        }
    }

    fun toggleSystem(system: String) {
        selectedSystem = if (selectedSystem == system) null else system
    }

    fun beginStudy() {
        if (spec.steps.isEmpty()) return
        mode = StudioMode.LEARN
        activeStep = 0
        selectedHotspot = null
        explodedAssembly = null
        xray = false
        playing = true
        applyStep(0)
    }

    fun moveStep(delta: Int) {
        if (spec.steps.isEmpty()) return
        if (activeStep < 0) {
            beginStudy()
            return
        }
        val next = activeStep + delta
        if (next < 0) return
        if (next > spec.steps.lastIndex) {
            toast = "STUDY COMPLETE / ${spec.systemNames.size} SYSTEMS UNDERSTOOD"
            return
        }
        activeStep = next
        applyStep(next)
    }

    private fun applyStep(index: Int) {
        val step = spec.steps.getOrNull(index) ?: return
        selectedSystem = step.system
        step.explode?.let { explodeTarget = it }
        step.control?.let { control = it }
        step.camera?.let { flyTo(it) }
    }

    fun flyTo(camera: CameraPose) {
        cameraJob?.cancel()
        val start = pose
        val target = camera
        cameraJob = scope.launch {
            val duration = 620f
            val started = System.nanoTime()
            while (isActive) {
                val t = ((System.nanoTime() - started) / 1_000_000f / duration).coerceIn(0f, 1f)
                val e = t * t * (3f - 2f * t)
                val yawDelta = shortestAngle(start.yaw, target.yaw)
                pose = CameraPose(
                    yaw = start.yaw + yawDelta * e,
                    pitch = start.pitch + (target.pitch - start.pitch) * e,
                    zoom = start.zoom + (target.zoom - start.zoom) * e,
                )
                if (t >= 1f) break
                delay(16)
            }
        }
    }

    fun stopCamera() {
        cameraJob?.cancel()
        cameraJob = null
    }

    fun orbit(yawDelta: Float, pitchDelta: Float, zoomFactor: Float) {
        stopCamera()
        pose = CameraPose(
            yaw = pose.yaw + yawDelta,
            pitch = (pose.pitch + pitchDelta).coerceIn(-5f, 82f),
            zoom = (pose.zoom * zoomFactor).coerceIn(0.55f, 2.35f),
        )
    }

    fun openHotspot(hotspot: Hotspot) {
        selectedHotspot = hotspot
        spec.parts.firstOrNull { it.id == hotspot.partId }?.let { selectedSystem = it.system }
        // A focused part is often behind an outer shell. Ghosting the rest of
        // the model keeps the selected hardware visible instead of centering a
        // camera on an opaque cover.
        xray = true
        if (hotspot.explode > 0.01f) explodeTarget = hotspot.explode
        // The stage now frames the selected part itself. Start at 1:1 so the
        // user can step through parts without inheriting a second zoom level.
        flyTo(hotspot.camera.copy(zoom = 1f))
    }

    fun stepHotspot(delta: Int) {
        val current = selectedHotspot ?: return
        val index = spec.hotspots.indexOf(current)
        val next = index + delta
        if (index < 0 || next !in spec.hotspots.indices) return
        openHotspot(spec.hotspots[next])
    }

    fun setPartZoom(value: Float) {
        stopCamera()
        pose = pose.copy(zoom = value.coerceIn(0.6f, 2.4f))
    }

    fun resetPartZoom() {
        selectedHotspot?.let { flyTo(it.camera.copy(zoom = 1f)) }
    }

    fun dismissHotspot() {
        selectedHotspot = null
        selectedSystem = null
        xray = false
        flyTo(spec.defaultPose)
    }

    fun resetView() {
        selectedHotspot = null
        selectedSystem = null
        xray = false
        flyTo(spec.defaultPose)
    }

    fun isolateHotspot(hotspot: Hotspot) {
        spec.parts.firstOrNull { it.id == hotspot.partId }?.let { selectedSystem = it.system }
        xray = true
        mode = StudioMode.CUTAWAY
        selectedHotspot = null
        toast = "ISOLATING / ${selectedSystem ?: spec.category}"
    }

    fun resetAll() {
        stopCamera()
        pose = spec.defaultPose
        explodeTarget = 0f
        explodedAssembly = null
        selectedSystem = null
        xray = false
        mode = StudioMode.EXPLORE
        activeStep = -1
        control = spec.control.defaultValue
        playing = true
        selectedHotspot = null
        resetClockRequest++
        toast = "STUDY RESET"
    }

    fun toggleXray() {
        xray = !xray
        if (xray) mode = StudioMode.CUTAWAY else if (mode == StudioMode.CUTAWAY) mode = StudioMode.EXPLORE
    }

    private fun shortestAngle(start: Float, end: Float): Float {
        var delta = (end - start) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    spec: ObjectSpec,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val context = LocalContext.current
    val prefs = remember { AnatomyPrefs(context) }
    val settings = LocalStudioSettings.current
    val qualitySetting = settings?.renderQuality ?: prefs.renderQuality
    val motionSetting = settings?.motion ?: prefs.motion
    val hapticsAllowed = settings?.haptics ?: prefs.haptics
    val frameRate = settings?.frameRate ?: prefs.frameRate
    val gridDefault = settings?.gridDefault ?: prefs.gridDefault
    val pinsDefault = settings?.pinsDefault ?: prefs.pinsDefault
    val keepAwake = settings?.keepAwake ?: prefs.keepAwake
    val autoplay = settings?.autoplay ?: prefs.autoplay
    val scope = rememberCoroutineScope()
    val state = remember(spec.id) {
        StudioState(
            spec = spec,
            scope = scope,
            startPlaying = autoplay && motionSetting != "OFF",
            showGridDefault = gridDefault,
            showPinsDefault = pinsDefault,
            hapticsAllowed = hapticsAllowed,
        )
    }
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity, keepAwake) {
        if (keepAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val frameStepMs = (1000L / frameRate.coerceIn(30, 60)) + if (motionSetting == "REDUCED") 6L else 0L
    val clock = remember(spec.id) {
        AnimContextFactory().apply {
            snapControl(state.control)
        }
    }
    val scene = remember(spec.id) { AnatomyCatalog.scene(spec) }
    var frame by remember { mutableLongStateOf(0L) }
    var consoleHeightPx by remember { mutableIntStateOf(0) }
    var fullscreenPanelHeightPx by remember { mutableIntStateOf(0) }
    var showExport by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(state.playing, frameStepMs) {
        if (!state.playing) return@LaunchedEffect
        var last = 0L
        val stepNanos = frameStepMs * 1_000_000L
        while (isActive) {
            withFrameNanos { now ->
                if (now - last > stepNanos) {
                    last = now
                    clock.update(now, playing = state.playing, rate = state.playbackSpeed)
                    frame++
                }
            }
        }
    }
    LaunchedEffect(state.control) { clock.targetControl = state.control }
    LaunchedEffect(state.resetClockRequest) {
        clock.resetClock()
        clock.targetControl = state.control
    }
    // Guided study is a colour walk-through: it keeps animating normally and
    // never inherits a cutaway ghost left over from a previous exploration.
    LaunchedEffect(state.mode) {
        if (state.mode == StudioMode.LEARN) state.xray = false
    }

    val anim = AnimContext(clock.ambient, clock.machine, clock.control)
    val exploded by animateFloatAsState(
        targetValue = state.explodeTarget,
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "explode",
    )
    val explodeMap = remember(exploded, state.explodedAssembly, spec.id) {
        when {
            exploded < 0.001f -> emptyMap()
            state.explodedAssembly == null -> spec.assemblyNames.associateWith { exploded }
            else -> mapOf(state.explodedAssembly!! to exploded)
        }
    }

    val toast = state.toast
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1900)
            if (state.toast == toast) state.toast = null
        }
    }

    BackHandler {
        when {
            state.selectedHotspot != null -> state.dismissHotspot()
            fullscreen -> fullscreen = false
            showExport -> showExport = false
            else -> onBack()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.ink)) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            val panelWidth = (maxWidth * 0.37f).coerceIn(300.dp, 380.dp)
            Row(Modifier.fillMaxSize().navigationBarsPadding()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    StudioStage(
                        spec = spec,
                        scene = scene,
                        state = state,
                        clock = clock,
                        frameProvider = { frame },
                        frameRate = frameRate,
                        explodeMap = explodeMap,
                        isLandscape = true,
                        consoleHeightPx = 0,
                        qualitySetting = qualitySetting,
                        modifier = Modifier.fillMaxSize(),
                    )
                    StudioToolRail(
                        state = state,
                        fullscreen = fullscreen,
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .zIndex(2f),
                    )
                }
                if (!fullscreen) {
                Column(
                    Modifier
                        .width(panelWidth)
                        .fillMaxHeight()
                        .background(colors.ink.copy(alpha = 0.96f))
                        .border(1.dp, colors.line.copy(alpha = 0.6f))
                        .verticalScroll(rememberScrollState())
                ) {
                    StudioTopBar(
                        spec = spec,
                        isFavorite = isFavorite,
                        onBack = onBack,
                        onFavorite = onToggleFavorite,
                        onExport = { showExport = true },
                        compact = true,
                    )
                    StudioTitle(spec = spec, compact = true)
                    Spacer(Modifier.height(9.dp))
                    Hairline(Modifier.padding(horizontal = 18.dp))
                    Spacer(Modifier.height(8.dp))
                    val focused = state.selectedHotspot
                    if (focused != null) {
                        PartFocusPanel(
                            spec = spec,
                            state = state,
                            hotspot = focused,
                            number = spec.hotspots.indexOf(focused) + 1,
                            compact = true,
                        )
                    } else {
                        StudioConsole(
                            state = state,
                            anim = anim,
                            onHeight = {},
                            modifier = Modifier.fillMaxWidth(),
                            compact = true,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (!fullscreen) {
                    StudioTopBar(
                        spec = spec,
                        isFavorite = isFavorite,
                        onBack = onBack,
                        onFavorite = onToggleFavorite,
                        onExport = { showExport = true },
                        compact = false,
                    )
                    StudioTitle(spec = spec, compact = false)
                    Spacer(Modifier.height(10.dp))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(
                            if (fullscreen) {
                                Modifier.statusBarsPadding().navigationBarsPadding()
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = if (fullscreen) 0.dp else 12.dp)
                        .clip(RoundedCornerShape(if (fullscreen) 0.dp else 26.dp))
                        .background(colors.stageInk)
                        .border(
                            if (fullscreen) 0.dp else 1.dp,
                            colors.stageLine,
                            RoundedCornerShape(if (fullscreen) 0.dp else 26.dp),
                        )
                ) {
                    StudioStage(
                        spec = spec,
                        scene = scene,
                        state = state,
                        clock = clock,
                        frameProvider = { frame },
                        frameRate = frameRate,
                        explodeMap = explodeMap,
                        isLandscape = false,
                        consoleHeightPx = 0,
                        qualitySetting = qualitySetting,
                        modifier = Modifier.fillMaxSize(),
                    )
                    StudioToolRail(
                        state = state,
                        fullscreen = fullscreen,
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .zIndex(2f),
                    )
                }
                if (!fullscreen) {
                    Spacer(Modifier.height(8.dp))
                    val focused = state.selectedHotspot
                    if (focused != null) {
                        PartFocusPanel(
                            spec = spec,
                            state = state,
                            hotspot = focused,
                            number = spec.hotspots.indexOf(focused) + 1,
                            onHeight = { consoleHeightPx = it },
                        )
                    } else {
                        StudioConsole(
                            state = state,
                            anim = anim,
                            onHeight = { consoleHeightPx = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        val fullscreenHotspot = if (fullscreen) state.selectedHotspot else null
        LaunchedEffect(fullscreen, fullscreenHotspot?.id) {
            if (fullscreenHotspot == null) fullscreenPanelHeightPx = 0
        }
        AnimatedVisibility(
            visible = fullscreenHotspot != null,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(if (landscape) 0.62f else 1f)
                .padding(horizontal = if (landscape) 12.dp else 0.dp),
        ) {
            fullscreenHotspot?.let { hotspot ->
                PartFocusPanel(
                    spec = spec,
                    state = state,
                    hotspot = hotspot,
                    number = spec.hotspots.indexOf(hotspot) + 1,
                    compact = true,
                    overlay = true,
                    onHeight = { fullscreenPanelHeightPx = it },
                )
            }
        }
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    bottom = with(LocalDensity.current) {
                        (if (fullscreen) fullscreenPanelHeightPx else consoleHeightPx).toDp() + 12.dp
                    },
                ),
        ) {
            GlassPanel(shape = RoundedCornerShape(50)) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(4.dp).background(colors.accent, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        toast.orEmpty(),
                        color = colors.text,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }
    }

    if (showExport) {
        StudioExportSheet(
            spec = spec,
            state = state,
            anim = anim,
            explodeMap = explodeMap,
            onDismiss = { showExport = false },
            onToast = { state.toast = it },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

@Composable
private fun StudioStage(
    spec: ObjectSpec,
    scene: CompiledScene,
    state: StudioState,
    clock: AnimContextFactory,
    frameProvider: () -> Long,
    frameRate: Int,
    explodeMap: Map<String, Float>,
    isLandscape: Boolean,
    consoleHeightPx: Int,
    qualitySetting: String = "BALANCED",
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    val density = LocalDensity.current
    // Reading the animation tick here keeps only the stage invalidating, not the whole chrome.
    frameProvider()
    val anim = AnimContext(clock.ambient, clock.machine, clock.control)
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val renderer = remember(spec.id) { SceneRenderer() }
    val perfFrame = remember { IntArray(1) }
    var stageWidth by remember { mutableIntStateOf(0) }
    var stageHeight by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragReset by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val safeLeft = with(density) { (if (isLandscape) 18.dp else 12.dp).toPx() }
    val safeRight = with(density) { (if (isLandscape) 18.dp else 48.dp).toPx() }
    val safeTop = with(density) { (if (isLandscape) 14.dp else 12.dp).toPx() }
    val safeBottom = with(density) { (if (isLandscape) 18.dp else 14.dp).toPx() }

    val still = !dragging
    val studyColor = state.mode == StudioMode.LEARN
    val sceneFaceCount = remember(scene) { scene.parts.sumOf { it.mesh.faces.size } }
    val request = RenderRequest(
        width = stageWidth,
        height = stageHeight,
        pose = state.pose,
        anim = anim,
        explode = explodeMap,
        selectedSystem = if (studyColor) null else state.selectedSystem,
        focusPartId = state.selectedHotspot?.partId,
        xray = if (studyColor) false else state.xray,
        showGrid = state.showGrid,
        // Full detail is reserved for a paused, stationary inspection frame.
        // Animated scenes use the cheaper paint path and a smaller live buffer.
        quality = if (still && !state.playing) RenderQuality.FULL else RenderQuality.INTERACTIVE,
        safeTop = safeTop,
        safeBottom = safeBottom,
        safeSide = safeLeft,
        safeRight = safeRight,
        density = density.density,
        // Surface hatching is a still-frame detail. Running animations keep
        // their geometry and lighting, but skip the clipped pattern pass.
        detail = still && !state.playing,
    )
    // The studio rasterises into its own bitmap. Drawing thousands of small paths straight
    // into a hardware canvas crashes older Adreno drivers inside HWUI's mesh path, and the
    // software buffer also lets us trade resolution for frame time while the camera moves.
    val buffer = remember { arrayOfNulls<Bitmap>(1) }
    // Release the stage buffer as soon as the study leaves the screen. Without this the
    // bitmap lingers until GC, which is what pushed low-RAM phones into OOM crashes.
    DisposableEffect(Unit) {
        onDispose {
            buffer[0]?.recycle()
            buffer[0] = null
        }
    }
    val bufferPaint = remember {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG or NativePaint.FILTER_BITMAP_FLAG).apply {
            isDither = true
        }
    }
    // Light models can afford a much sharper moving buffer than a 80-part turbofan, so the
    // resolution follows the scene's weight instead of punishing every object equally.
    val heavyScene = scene.parts.size >= 40 || sceneFaceCount >= 3600
    val liveScale = when {
        still && !state.playing -> 1f
        // Motion always gets the adaptive buffer. The quality selector still
        // governs the paused, full-detail frame and manual dragging; this cap
        // prevents a heavy mechanism from lagging just because SHARP is set.
        still && state.playing -> if (heavyScene) 0.55f else 0.64f
        qualitySetting == AnatomyPrefs.QUALITY_LITE ->
            if (dragging) 0.5f else if (heavyScene) 0.55f else 0.62f
        qualitySetting == AnatomyPrefs.QUALITY_SHARP ->
            if (dragging) 0.7f else 0.8f
        heavyScene -> if (dragging) 0.54f else 0.6f
        dragging -> 0.68f
        else -> 0.78f
    }
    val pins = if (state.showPins && stageWidth > 0) HotspotProjector.project(scene, request) else emptyList()
    val pinPlacements = if (pins.isEmpty()) emptyList() else layoutPins(
        pins = pins,
        renderScale = 1f,
        stageWidth = stageWidth,
        stageHeight = stageHeight,
        topLimit = with(density) { if (isLandscape) 14.dp.toPx() else 54.dp.toPx() },
        bottomLimit = stageHeight - with(density) { (if (isLandscape) 34.dp else 14.dp).toPx() },
        minDistance = with(density) { 39.dp.toPx() },
        rightLimit = stageWidth - with(density) { (if (isLandscape) 16.dp else 62.dp).toPx() },
    )

    Box(modifier.background(colors.inkDeep).onSizeChanged {
        stageWidth = it.width
        stageHeight = it.height
    }) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(state.mode) {
                    detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                        state.orbit(
                            yawDelta = -pan.x * 0.30f,
                            pitchDelta = pan.y * 0.24f,
                            zoomFactor = zoom,
                        )
                        dragging = true
                        dragReset?.cancel()
                        dragReset = scope.launch {
                            delay(220)
                            dragging = false
                        }
                    }
                }
        ) {
            if (stageWidth <= 0 || stageHeight <= 0) return@Canvas
            val bufferWidth = (stageWidth * liveScale).roundToInt().coerceIn(2, stageWidth)
            val bufferHeight = (stageHeight * liveScale).roundToInt().coerceIn(2, stageHeight)
            var target = buffer[0]
            if (target == null || target.width != bufferWidth || target.height != bufferHeight) {
                target?.recycle()
                // A large quality buffer can fail on low-RAM phones. Halving it is far better
                // than letting the allocation take the whole app down.
                target = try {
                    Bitmap.createBitmap(bufferWidth, bufferHeight, Bitmap.Config.ARGB_8888)
                } catch (oom: OutOfMemoryError) {
                    Bitmap.createBitmap(
                        (bufferWidth / 2).coerceAtLeast(2),
                        (bufferHeight / 2).coerceAtLeast(2),
                        Bitmap.Config.ARGB_8888,
                    )
                }
                buffer[0] = target
            }
            val effectiveScale = target.width.toFloat() / stageWidth
            val scaled = if (target.width == stageWidth && target.height == stageHeight) request else request.copy(
                width = target.width,
                height = target.height,
                safeTop = safeTop * effectiveScale,
                safeBottom = safeBottom * effectiveScale,
                safeSide = safeLeft * effectiveScale,
                safeRight = safeRight * effectiveScale,
                density = density.density * effectiveScale,
            )
            val report = renderer.draw(NativeCanvas(target), scene, scaled)
            drawContext.canvas.nativeCanvas.drawBitmap(
                target,
                null,
                RectF(0f, 0f, stageWidth.toFloat(), stageHeight.toFloat()),
                bufferPaint,
            )
            if (++perfFrame[0] % 10 == 0) {
                Log.d(
                    "AnatomyPerf",
                    "faces=${report.faceCount} batches=${report.batches} " +
                        "patterns=${report.patterns} " +
                        "chrome=${report.chromeMillis} build=${report.buildMillis} sort=${report.sortMillis} " +
                        "paint=${report.paintMillis} cpu=${report.renderMillis}ms " +
                        "${bufferWidth}x${bufferHeight}@${liveScale}",
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            pinPlacements.forEach { placement ->
                val selected = state.selectedHotspot?.id == placement.pin.hotspot.id
                val d = kotlin.math.hypot(
                    (placement.x - placement.anchorX).toDouble(),
                    (placement.y - placement.anchorY).toDouble(),
                )
                if (d > 5.0) {
                    drawLine(
                        color = if (selected) colors.accent else colors.accent.copy(alpha = 0.34f),
                        start = Offset(placement.anchorX, placement.anchorY),
                        end = Offset(placement.x, placement.y),
                        strokeWidth = if (selected) 1.4f else 1f,
                    )
                    drawCircle(
                        color = if (selected) colors.accent else colors.accent.copy(alpha = 0.55f),
                        radius = if (selected) 2.6f else 1.9f,
                        center = Offset(placement.anchorX, placement.anchorY),
                    )
                }
            }
        }

        pinPlacements.forEach { placement ->
            HotspotPin(
                pin = placement.pin,
                renderScale = 1f,
                selected = state.selectedHotspot?.id == placement.pin.hotspot.id,
                onClick = {
                    if (state.hapticsAllowed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.openHotspot(placement.pin.hotspot)
                },
                modifier = Modifier.offset {
                    IntOffset(
                        (placement.x - 14.dp.toPx()).roundToInt(),
                        (placement.y - 14.dp.toPx()).roundToInt(),
                    )
                },
            )
        }

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(if (state.playing) colors.ink.copy(alpha = 0.72f) else colors.accent)
                .border(
                    1.dp,
                    if (state.playing) colors.stageLine else colors.accent,
                    RoundedCornerShape(50),
                )
                .clickable {
                    if (state.hapticsAllowed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.playing = !state.playing
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (state.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (state.playing) "Pause mechanism" else "Run mechanism",
                tint = if (state.playing) colors.stageText else colors.ink,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (state.playing) "PAUSE" else "RUN",
                color = if (state.playing) colors.stageText else colors.ink,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
            )
        }

        if (isLandscape) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                MonoHint("DRAG / ORBIT   PINCH / SCALE")
            }
        }
    }
}

@Composable
private fun MonoHint(text: String) {
    val colors = LocalAnatomyColors.current
    Text(
        text,
        color = colors.textTertiary.copy(alpha = 0.78f),
        fontFamily = JetBrainsMono,
        fontSize = 12.sp,
        letterSpacing = 0.9.sp,
    )
}

@Composable
private fun HotspotPin(
    pin: ProjectedHotspot,
    renderScale: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.ink.copy(alpha = 0.82f))
            .border(1.1.dp, if (selected) colors.accent else colors.accent.copy(alpha = 0.62f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            pin.number.toString().padStart(2, '0'),
            color = if (selected) colors.ink else colors.text,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
        )
        if (!selected) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .border(1.dp, colors.accent.copy(alpha = 0.12f), CircleShape)
            )
        }
    }
}

@Composable
private fun StudioTopBar(
    spec: ObjectSpec,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onExport: () -> Unit,
    compact: Boolean,
) {
    val colors = LocalAnatomyColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = if (compact) 14.dp else 16.dp)
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = Icons.Outlined.ArrowBack,
            contentDescription = "Back to atlas",
            onClick = onBack,
            size = 40.dp,
            iconSize = 18.dp,
        )
        Spacer(Modifier.width(12.dp))
        MonoLabel(
            "OBJECT ${spec.index.toString().padStart(2, '0')} / ${spec.designation.substringBefore(" /")}",
            color = colors.accent.copy(alpha = 0.8f),
            fontSize = 9,
            letterSpacing = 1.1f,
            modifier = Modifier.weight(1f),
        )
        GlassIconButton(
            icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
            onClick = onFavorite,
            size = 38.dp,
            iconSize = 17.dp,
            tint = if (isFavorite) colors.amber else colors.text,
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = Icons.Outlined.Share,
            contentDescription = "Export plate",
            onClick = onExport,
            size = 38.dp,
            iconSize = 16.dp,
        )
    }
}

@Composable
private fun StudioTitle(spec: ObjectSpec, compact: Boolean) {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = if (compact) 18.dp else 20.dp, end = if (compact) 18.dp else 20.dp)
            .padding(top = if (compact) 12.dp else 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(colors.accent, CircleShape))
            Spacer(Modifier.width(7.dp))
            MonoLabel(spec.category, color = colors.accent, fontSize = 8, letterSpacing = 1.4f)
            Spacer(Modifier.width(9.dp))
            Box(Modifier.height(1.dp).width(22.dp).background(colors.line))
            Spacer(Modifier.width(9.dp))
            MonoLabel(spec.designation.substringBefore(" /"), color = colors.textTertiary, fontSize = 7)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            spec.title,
            color = colors.text,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = if (compact) 26.sp else 29.sp,
            lineHeight = if (compact) 28.sp else 31.sp,
            letterSpacing = (-0.9).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel(
                "${spec.parts.size.toString().padStart(2, '0')} PARTS",
                color = colors.textTertiary,
                fontSize = 7,
                letterSpacing = 0.9f,
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(2.dp).background(colors.textTertiary.copy(alpha = 0.5f), CircleShape))
            Spacer(Modifier.width(8.dp))
            MonoLabel(
                "${spec.systemNames.size.toString().padStart(2, '0')} SYSTEMS",
                color = colors.textTertiary,
                fontSize = 7,
                letterSpacing = 0.9f,
            )
        }
    }
}

@Composable
private fun StudioToolRail(
    state: StudioState,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.width(1.dp).height(14.dp).background(colors.line))
        GlassIconButton(
            icon = Icons.Outlined.CenterFocusStrong,
            contentDescription = "Reset view",
            onClick = { state.resetView() },
            size = 34.dp,
            iconSize = 15.dp,
        )
        GlassIconButton(
            icon = Icons.Outlined.GridOn,
            contentDescription = "Toggle floor grid",
            onClick = { state.showGrid = !state.showGrid },
            size = 34.dp,
            iconSize = 15.dp,
            active = state.showGrid,
        )
        GlassIconButton(
            icon = Icons.Outlined.PushPin,
            contentDescription = "Toggle hotspots",
            onClick = { state.showPins = !state.showPins },
            size = 34.dp,
            iconSize = 15.dp,
            active = state.showPins,
        )
        GlassIconButton(
            icon = Icons.Outlined.Visibility,
            contentDescription = "Toggle x-ray",
            onClick = { state.toggleXray() },
            size = 34.dp,
            iconSize = 15.dp,
            active = state.xray,
        )
        GlassIconButton(
            icon = if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
            contentDescription = if (fullscreen) "Exit full screen" else "Full screen",
            onClick = onToggleFullscreen,
            size = 34.dp,
            iconSize = 15.dp,
        )
        Box(Modifier.width(1.dp).height(14.dp).background(colors.line))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotspotSheet(
    spec: ObjectSpec,
    hotspot: Hotspot,
    number: Int,
    zoom: Float,
    onDismiss: () -> Unit,
    onIsolate: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onZoom: (Float) -> Unit,
    onResetZoom: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    SheetSystemBars(colors.isDark)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val part = spec.parts.firstOrNull { it.id == hotspot.partId }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(Modifier.padding(vertical = 12.dp).size(width = 34.dp, height = 3.dp).background(colors.line, CircleShape))
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    number.toString().padStart(2, '0'),
                    color = colors.accent.copy(alpha = 0.28f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Light,
                    fontSize = 46.sp,
                    lineHeight = 46.sp,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    MonoLabel("PART FOCUS", color = colors.accent, fontSize = 8)
                    MonoLabel(
                        "${number.toString().padStart(2, '0')} OF ${spec.hotspots.size.toString().padStart(2, '0')}",
                        color = colors.textTertiary,
                        fontSize = 8,
                    )
                    MonoLabel(part?.system ?: spec.category, color = colors.textTertiary, fontSize = 8)
                }
                Spacer(Modifier.weight(1f))
                PartStepButton(
                    icon = Icons.Outlined.ArrowBack,
                    contentDescription = "Previous part",
                    enabled = number > 1,
                    onClick = onPrevious,
                )
                Spacer(Modifier.width(6.dp))
                PartStepButton(
                    icon = Icons.Outlined.ArrowForward,
                    contentDescription = "Next part",
                    enabled = number < spec.hotspots.size,
                    onClick = onNext,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                hotspot.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                hotspot.body,
                color = colors.textSecondary,
                style = AnatomyType.body,
            )
            Spacer(Modifier.height(18.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            HotspotRow("COMPONENT", part?.name ?: hotspot.partId)
            HotspotRow("ASSEMBLY", part?.assembly ?: "—")
            HotspotRow("PART ID", hotspot.partId.uppercase())
            if (hotspot.readoutLabel != null) HotspotRow("FUNCTION", hotspot.readoutLabel)
            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("PART ZOOM", color = colors.textSecondary, fontSize = 8, letterSpacing = 0.8f)
                Spacer(Modifier.weight(1f))
                Text(
                    "${(zoom * 100).roundToInt()}%",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.width(9.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, colors.line, RoundedCornerShape(50))
                        .clickable(onClick = onResetZoom)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("1:1", color = colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 8.sp)
                }
            }
            Slider(
                value = zoom.coerceIn(0.6f, 2.4f),
                onValueChange = onZoom,
                valueRange = 0.6f..2.4f,
                modifier = Modifier.fillMaxWidth().height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.line,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("WIDE", color = colors.textTertiary, fontSize = 7)
                Spacer(Modifier.weight(1f))
                MonoLabel("DETAIL", color = colors.textTertiary, fontSize = 7)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryInline("ISOLATE SYSTEM", onIsolate, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                SecondaryInline("CLOSE", onDismiss, Modifier.weight(0.7f))
            }
        }
    }
}

@Composable
private fun PartStepButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Box(
        Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.32f)
            .clip(CircleShape)
            .border(1.dp, colors.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PartFocusPanel(
    spec: ObjectSpec,
    state: StudioState,
    hotspot: Hotspot,
    number: Int,
    compact: Boolean = false,
    overlay: Boolean = false,
    onHeight: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    val part = spec.parts.firstOrNull { it.id == hotspot.partId }
    val total = spec.hotspots.size
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeight(it.height) }
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        borderColor = colors.line.copy(alpha = 0.9f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(top = if (compact) 9.dp else 10.dp, bottom = if (overlay) 8.dp else 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    number.toString().padStart(2, '0'),
                    color = colors.accent.copy(alpha = 0.34f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Light,
                    fontSize = 40.sp,
                    lineHeight = 40.sp,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    MonoLabel("PART FOCUS", color = colors.accent, fontSize = 8, letterSpacing = 0.8f)
                    MonoLabel(
                        "${number.toString().padStart(2, '0')} OF ${total.toString().padStart(2, '0')}  ·  ${part?.system ?: spec.category}",
                        color = colors.textTertiary,
                        fontSize = 8,
                        letterSpacing = 0.4f,
                    )
                }
                Spacer(Modifier.weight(1f))
                PartStepButton(
                    icon = Icons.Outlined.ArrowBack,
                    contentDescription = "Previous part",
                    enabled = number > 1,
                    onClick = { state.stepHotspot(-1) },
                )
                Spacer(Modifier.width(6.dp))
                PartStepButton(
                    icon = Icons.Outlined.ArrowForward,
                    contentDescription = "Next part",
                    enabled = number < total,
                    onClick = { state.stepHotspot(1) },
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                hotspot.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 21.sp else 24.sp,
                lineHeight = if (compact) 23.sp else 27.sp,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                hotspot.body,
                color = colors.textSecondary,
                fontSize = if (compact) 11.5.sp else 12.5.sp,
                lineHeight = if (compact) 15.sp else 17.sp,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(part?.name ?: hotspot.partId, color = colors.textSecondary, fontSize = 9)
                Spacer(Modifier.weight(1f))
                MonoLabel(part?.assembly ?: "—", color = colors.textTertiary, fontSize = 7)
            }
            Spacer(Modifier.height(8.dp))
            Hairline()
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("PART ZOOM", color = colors.textTertiary, fontSize = 8, letterSpacing = 0.8f)
                Spacer(Modifier.weight(1f))
                Text(
                    "${(state.pose.zoom * 100).roundToInt()}%",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, colors.line, RoundedCornerShape(50))
                        .clickable { state.resetPartZoom() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("1:1", color = colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 8.sp)
                }
            }
            Slider(
                value = state.pose.zoom.coerceIn(0.6f, 2.4f),
                onValueChange = { state.setPartZoom(it) },
                valueRange = 0.6f..2.4f,
                modifier = Modifier.fillMaxWidth().height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.line,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PrimaryInline("ISOLATE SYSTEM", { state.isolateHotspot(hotspot) }, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                SecondaryInline("CLOSE", { state.dismissHotspot() }, Modifier.weight(0.58f))
            }
        }
    }
}

@Composable
private fun HotspotRow(key: String, value: String) {
    val colors = LocalAnatomyColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        MonoLabel(key, color = colors.textTertiary, fontSize = 8)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = colors.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.End,
        )
    }
}

private data class PinPlacement(
    val pin: ProjectedHotspot,
    val x: Float,
    val y: Float,
    val anchorX: Float,
    val anchorY: Float,
)

private fun layoutPins(
    pins: List<ProjectedHotspot>,
    renderScale: Float,
    stageWidth: Int,
    stageHeight: Int,
    topLimit: Float,
    bottomLimit: Float,
    minDistance: Float,
    rightLimit: Float,
): List<PinPlacement> {
    if (pins.isEmpty() || stageWidth <= 0 || stageHeight <= 0) return emptyList()
    val occupied = ArrayList<Pair<Float, Float>>(pins.size)
    val safeTop = topLimit.coerceAtMost(bottomLimit - 2f)
    val safeBottom = bottomLimit.coerceAtLeast(safeTop + 2f)
    val edge = 24f
    val rightEdge = rightLimit.coerceAtLeast(edge + 1f)

    fun isFree(x: Float, y: Float): Boolean = occupied.none { (px, py) ->
        hypot((px - x).toDouble(), (py - y).toDouble()) < minDistance
    }

    return pins.map { pin ->
        val anchorX = pin.x / renderScale
        val anchorY = pin.y / renderScale
        var x = anchorX.coerceIn(edge, rightEdge)
        var y = anchorY.coerceIn(safeTop, safeBottom)
        if (!isFree(x, y)) {
            var found = false
            for (step in 1..4) {
                val candidates = listOf(
                    y + minDistance * step to x,
                    y - minDistance * step to x,
                )
                for ((cy, cx) in candidates) {
                    val clampedY = cy.coerceIn(safeTop, safeBottom)
                    if (isFree(cx, clampedY)) {
                        y = clampedY
                        found = true
                        break
                    }
                }
                if (found) break
            }
            if (!found) {
                for (step in 1..3) {
                    val cx = (x + minDistance * step).coerceAtMost(rightEdge)
                    if (isFree(cx, y)) {
                        x = cx
                        break
                    }
                }
            }
        }
        occupied.add(x to y)
        PinPlacement(pin, x, y, anchorX, anchorY)
    }
}

@Composable
private fun PrimaryInline(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAnatomyColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color(0xFF031C1A), fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun SecondaryInline(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAnatomyColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 0.9.sp)
    }
}
