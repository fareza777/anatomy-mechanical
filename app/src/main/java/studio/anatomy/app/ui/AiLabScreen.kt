package studio.anatomy.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import studio.anatomy.app.data.AnatomyPrefs
import studio.anatomy.app.data.PlateExporter
import studio.anatomy.app.data.PlateResult
import studio.anatomy.app.ui.components.CornerTicks
import studio.anatomy.app.ui.components.FilterPill
import studio.anatomy.app.ui.components.GlassIconButton
import studio.anatomy.app.ui.components.GlassPanel
import studio.anatomy.app.ui.components.Hairline
import studio.anatomy.app.ui.components.LabelWithDot
import studio.anatomy.app.ui.components.MonoLabel
import studio.anatomy.app.ui.components.PrimaryButton
import studio.anatomy.app.ui.components.TechnicalBackdrop
import studio.anatomy.app.ui.theme.AnatomyType
import studio.anatomy.app.ui.theme.JetBrainsMono
import studio.anatomy.app.ui.theme.LocalAnatomyColors
import studio.anatomy.app.ui.theme.Manrope
import studio.anatomy.app.ui.theme.SpaceGrotesk
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private enum class AiStyle(val label: String, val suffix: String) {
    PLATE(
        "PLATE",
        "Orthographic technical illustration on a dark graphite background, precise cutaway drafting, machined metal and brass details, soft rim light, no text, no labels, no watermark."
    ),
    BLUEPRINT(
        "BLUEPRINT",
        "Cyanotype engineering blueprint, white linework on deep blue, orthographic projection, exploded assembly, technical hatching, no text, no labels, no watermark."
    ),
    CUTAWAY(
        "CUTAWAY",
        "Isometric cutaway technical illustration, internal mechanisms visible, museum lighting, muted industrial palette, dark studio background, no text, no labels, no watermark."
    ),
    CINEMATIC(
        "CINEMATIC",
        "Premium cinematic product render of an industrial machine, dramatic rim light, dark studio, volumetric haze, photoreal metal, no text, no labels, no watermark."
    ),
}

private data class AiImage(val bitmap: Bitmap, val prompt: String, val style: AiStyle)

@Composable
fun AiLabScreen(onBack: () -> Unit, onOpenSystem: () -> Unit) {
    val colors = LocalAnatomyColors.current
    val context = LocalContext.current
    val prefs = remember { AnatomyPrefs(context) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var endpoint by remember { mutableStateOf(prefs.aiEndpoint) }
    var model by remember { mutableStateOf(prefs.aiModel) }
    var keyInput by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(prefs.aiKey.isNotBlank()) }
    var showSetup by remember { mutableStateOf(prefs.aiKey.isBlank()) }

    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(AiStyle.PLATE) }
    var square by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<AiImage?>(null) }
    var saved by remember { mutableStateOf<PlateResult?>(null) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(status) {
        if (status != null) {
            delay(2600)
            if (status != null) status = null
        }
    }
    BackHandler { onBack() }

    fun saveCredentials() {
        val cleanEndpoint = endpoint.trim()
        val cleanModel = model.trim()
        if (!cleanEndpoint.startsWith("https://", ignoreCase = true)) {
            error = "The endpoint must be an HTTPS image-generation URL."
            return
        }
        if (cleanModel.isBlank()) {
            error = "Add the model identifier required by your provider."
            return
        }
        val key = keyInput.trim().ifBlank { prefs.aiKey }
        if (key.isBlank()) {
            error = "Paste a provider API key to continue."
            return
        }
        prefs.aiEndpoint = cleanEndpoint
        prefs.aiModel = cleanModel
        prefs.aiKey = key
        keyInput = ""
        hasKey = true
        showSetup = false
        error = null
        status = "PROVIDER CONNECTED / KEY STORED LOCALLY"
        keyboard?.hide()
    }

    fun generate() {
        if (!hasKey) {
            showSetup = true
            return
        }
        if (prompt.trim().length < 4) {
            error = "Describe the machine you want to study."
            return
        }
        keyboard?.hide()
        error = null
        loading = true
        result = null
        saved = null
        val styleAtRequest = style
        val aspectAtRequest = square
        val fullPrompt = buildPrompt(prompt.trim(), styleAtRequest)
        scope.launch {
            try {
                val bitmap = requestImage(
                    endpoint = endpoint.trim(),
                    model = model.trim(),
                    key = if (keyInput.isNotBlank()) keyInput.trim() else prefs.aiKey,
                    prompt = fullPrompt,
                    square = aspectAtRequest,
                )
                result = AiImage(bitmap, prompt.trim(), styleAtRequest)
            } catch (failure: Exception) {
                error = failure.message?.take(190) ?: "The provider did not return an illustration."
            } finally {
                loading = false
            }
        }
    }

    fun saveResult(shareAfter: Boolean) {
        val generated = result ?: return
        saving = true
        scope.launch {
            try {
                val savedPlate = withContext(Dispatchers.IO) {
                    PlateExporter.saveGeneratedImage(context.applicationContext, generated.bitmap, "ai_${generated.style.name.lowercase()}")
                }
                saved = savedPlate
                if (shareAfter) {
                    runCatching { PlateExporter.share(context, savedPlate, "AI illustration") }
                        .onFailure { status = "NO APP AVAILABLE TO SHARE" }
                } else {
                    status = if (savedPlate.inGallery) "SAVED TO GALLERY / ANATOMY STUDIO" else "PLATE READY TO SHARE"
                }
            } catch (failure: Exception) {
                error = "Could not write the illustration to this device."
            } finally {
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.ink)) {
        TechnicalBackdrop(Modifier.fillMaxSize(), accent = colors.amber, glowCenterX = 0.62f)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    icon = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = 40.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    MonoLabel("AI LAB / OPTIONAL MODULE", color = colors.amber, fontSize = 8, letterSpacing = 1.3f)
                    MonoLabel("NETWORK REQUIRED / YOUR KEY, YOUR MODEL", color = colors.textTertiary, fontSize = 7)
                }
                GlassIconButton(
                    icon = Icons.Outlined.Tune,
                    contentDescription = "Provider settings",
                    onClick = { showSetup = !showSetup },
                    size = 38.dp,
                    iconSize = 16.dp,
                    active = showSetup,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(colors.amber, CircleShape))
                Spacer(Modifier.width(7.dp))
                MonoLabel("GENERATIVE INSTRUMENT", color = colors.amber, fontSize = 8, letterSpacing = 1.4f)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "Describe the machine.\nSee it drawn.",
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 34.sp,
                lineHeight = 37.sp,
                letterSpacing = (-1.0).sp,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "A prompt-driven sketchbook for concepts that do not exist yet. Your chosen image model receives the prompt directly. Anatomy Studio never proxies the request, and your key is sent only to that endpoint.",
                color = colors.textSecondary,
                style = AnatomyType.body,
            )
            Spacer(Modifier.height(20.dp))

            if (showSetup || !hasKey) {
                ProviderSetup(
                    endpoint = endpoint,
                    model = model,
                    keyInput = keyInput,
                    hasKey = hasKey,
                    error = error,
                    onEndpoint = { endpoint = it },
                    onModel = { model = it },
                    onKey = { keyInput = it },
                    onSave = { saveCredentials() },
                    onDisconnect = {
                        prefs.clearAiKey()
                        hasKey = false
                        showSetup = true
                        status = "PROVIDER KEY REMOVED FROM THIS DEVICE"
                    },
                    onDefault = {
                        endpoint = AnatomyPrefs.DEFAULT_AI_ENDPOINT
                        model = AnatomyPrefs.DEFAULT_AI_MODEL
                    },
                )
                Spacer(Modifier.height(16.dp))
            }

            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LabelWithDot("PROMPT / SUBJECT", color = colors.accent)
                        Spacer(Modifier.weight(1f))
                        MonoLabel("${prompt.length}/500", color = colors.textTertiary, fontSize = 7)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { if (it.length <= 500) prompt = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp),
                        placeholder = {
                            Text(
                                "A six-axis robotic arm in a cutaway view, brass harmonic drives, graphite castings, service cables…",
                                color = colors.textTertiary,
                                style = AnatomyType.bodySmall,
                            )
                        },
                        textStyle = AnatomyType.body.copy(color = colors.text),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 4,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent.copy(alpha = 0.55f),
                            unfocusedBorderColor = colors.line,
                            cursorColor = colors.accent,
                            focusedContainerColor = colors.inkDeep.copy(alpha = 0.45f),
                            unfocusedContainerColor = colors.inkDeep.copy(alpha = 0.35f),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    MonoLabel("VISUAL LANGUAGE", color = colors.textTertiary, fontSize = 8)
                    Spacer(Modifier.height(7.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AiStyle.entries.forEach { entry ->
                            FilterPill(
                                text = entry.label,
                                selected = style == entry,
                                onClick = { style = entry },
                            )
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel("FRAME", color = colors.textTertiary, fontSize = 8)
                        Spacer(Modifier.width(10.dp))
                        FilterPill(text = "LANDSCAPE", selected = !square, onClick = { square = false })
                        Spacer(Modifier.width(6.dp))
                        FilterPill(text = "SQUARE", selected = square, onClick = { square = true })
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton(
                            text = if (loading) "Drawing" else "Generate illustration",
                            onClick = { generate() },
                            icon = Icons.Outlined.AutoAwesome,
                            enabled = !loading && hasKey,
                            modifier = Modifier.weight(1f),
                        )
                        if (loading) {
                            Spacer(Modifier.width(14.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = colors.amber,
                                strokeWidth = 1.6.dp,
                            )
                        }
                    }
                    if (!hasKey) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Connect a provider above to enable generation.",
                            color = colors.textTertiary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            if (error != null && !showSetup) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.danger.copy(alpha = 0.08f))
                        .border(1.dp, colors.danger.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(4.dp).background(colors.danger, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        error.orEmpty(),
                        color = colors.danger,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            if (loading) {
                GeneratingFrame()
            } else {
                val generated = result
                if (generated != null) {
                    ResultPlate(
                        image = generated,
                        saving = saving,
                        saved = saved,
                        onSave = { saveResult(false) },
                        onShare = {
                            if (saved != null) {
                                runCatching { PlateExporter.share(context, saved!!, "AI illustration") }
                                    .onFailure { status = "NO APP AVAILABLE TO SHARE" }
                            } else {
                                saveResult(true)
                            }
                        },
                        onNew = {
                            result = null
                            saved = null
                            error = null
                        },
                    )
                } else {
                    EmptyCanvas()
                }
            }

            Spacer(Modifier.height(18.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Text(
                "AI output is generated by the provider you connect. Review it before publishing. The 3D atlas itself remains fully offline.",
                color = colors.textTertiary,
                fontFamily = Manrope,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, colors.line, RoundedCornerShape(50))
                    .clickable(onClick = onOpenSystem)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Key, null, tint = colors.textTertiary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                MonoLabel("PRIVACY / SYSTEM SETTINGS", color = colors.textTertiary, fontSize = 7)
            }
        }

        if (status != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp)
            ) {
                GlassPanel(shape = RoundedCornerShape(50)) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(4.dp).background(colors.amber, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            status.orEmpty(),
                            color = colors.text,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            letterSpacing = 0.7.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSetup(
    endpoint: String,
    model: String,
    keyInput: String,
    hasKey: Boolean,
    error: String?,
    onEndpoint: (String) -> Unit,
    onModel: (String) -> Unit,
    onKey: (String) -> Unit,
    onSave: () -> Unit,
    onDisconnect: () -> Unit,
    onDefault: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelWithDot("PROVIDER / IMAGE MODEL", color = colors.amber)
                Spacer(Modifier.weight(1f))
                if (hasKey) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).background(colors.accent, CircleShape))
                        Spacer(Modifier.width(5.dp))
                        MonoLabel("CONNECTED", color = colors.accent, fontSize = 7)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Anatomy Studio ships without a key. Connect any OpenAI-compatible image-generation endpoint. The key is stored in this app's private storage and sent only to that endpoint.",
                color = colors.textTertiary,
                fontFamily = Manrope,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(12.dp))
            TechField("ENDPOINT / HTTPS", endpoint, onEndpoint, placeholder = "https://api.openai.com/v1/images/generations")
            Spacer(Modifier.height(9.dp))
            TechField("MODEL", model, onModel, placeholder = "gpt-image-1")
            Spacer(Modifier.height(9.dp))
            TechField(
                "API KEY",
                keyInput,
                onKey,
                placeholder = if (hasKey) "••••••••  key already stored" else "Paste your provider key",
                mask = true,
            )
            if (error != null) {
                Spacer(Modifier.height(9.dp))
                Text(error, color = colors.danger, fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 12.sp)
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(
                    text = "Connect provider",
                    onClick = onSave,
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
                if (hasKey) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "DISCONNECT",
                        color = colors.textTertiary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, colors.line, RoundedCornerShape(50))
                            .clickable(onClick = onDisconnect)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "OPENAI-COMPATIBLE DEFAULTS",
                color = colors.accent.copy(alpha = 0.75f),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                modifier = Modifier.clickable(onClick = onDefault).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TechField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    mask: Boolean = false,
) {
    val colors = LocalAnatomyColors.current
    Column {
        MonoLabel(label, color = colors.textTertiary, fontSize = 10)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = AnatomyType.bodySmall.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (mask) KeyboardType.Password else KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inkDeep.copy(alpha = 0.5f))
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 11.dp, vertical = 11.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = colors.textTertiary, style = AnatomyType.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun GeneratingFrame() {
    val colors = LocalAnatomyColors.current
    val transition = rememberInfiniteTransition(label = "ai-scan")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 34.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(colors.lineSoft, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(colors.lineSoft, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            val lineY = size.height * scan
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, colors.amber.copy(alpha = 0.75f), Color.Transparent)),
                start = Offset(0f, lineY),
                end = Offset(size.width, lineY),
                strokeWidth = 1.4f,
            )
        }
        CornerTicks(Modifier.fillMaxSize(), color = colors.amber.copy(alpha = 0.35f))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = colors.amber, strokeWidth = 1.4.dp, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(12.dp))
            MonoLabel("DRAWING / PROVIDER RENDERING", color = colors.amber, fontSize = 8)
            Spacer(Modifier.height(3.dp))
            MonoLabel("THIS USES YOUR PROVIDER QUOTA", color = colors.textTertiary, fontSize = 7)
        }
    }
}

@Composable
private fun ResultPlate(
    image: AiImage,
    saving: Boolean,
    saved: PlateResult?,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onNew: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
        ) {
            Image(
                bitmap = image.bitmap.asImageBitmap(),
                contentDescription = image.prompt,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Brush.verticalGradient(listOf(colors.ink.copy(alpha = 0.6f), Color.Transparent)))
            )
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp).background(colors.amber, CircleShape))
                Spacer(Modifier.width(6.dp))
                MonoLabel("AI GENERATED / ${image.style.label}", color = colors.paper, fontSize = 7)
            }
            CornerTicks(Modifier.fillMaxSize(), color = colors.amber.copy(alpha = 0.5f), inset = 11.dp, length = 13.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "“${image.prompt}”",
            color = colors.textSecondary,
            fontFamily = Manrope,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                text = if (saving) "Saving" else if (saved != null) "Saved" else "Save",
                onClick = onSave,
                icon = Icons.Outlined.Download,
                enabled = !saving,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SecondaryAction("SHARE", Icons.Outlined.Share, onShare, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            SecondaryAction("NEW", Icons.Outlined.Close, onNew, Modifier.weight(0.8f))
        }
        if (saved != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Check, null, tint = colors.accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    saved.fileName,
                    color = colors.textTertiary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SecondaryAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 0.7.sp)
    }
}

@Composable
private fun EmptyCanvas() {
    val colors = LocalAnatomyColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface.copy(alpha = 0.5f))
            .border(1.dp, colors.lineSoft, RoundedCornerShape(22.dp))
    ) {
        CornerTicks(Modifier.fillMaxSize(), color = colors.line, inset = 13.dp)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = colors.textTertiary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            MonoLabel("NO ILLUSTRATION IN THIS FRAME", color = colors.textTertiary, fontSize = 10)
            Spacer(Modifier.height(3.dp))
            MonoLabel("WRITE A PROMPT AND GENERATE", color = colors.textTertiary.copy(alpha = 0.6f), fontSize = 7)
        }
    }
}

private fun buildPrompt(subject: String, style: AiStyle): String =
    "A precise technical illustration of: $subject. ${style.suffix}"

private suspend fun requestImage(
    endpoint: String,
    model: String,
    key: String,
    prompt: String,
    square: Boolean,
): Bitmap = withContext(Dispatchers.IO) {
    val url = URL(endpoint)
    if (!url.protocol.equals("https", ignoreCase = true)) {
        throw IOException("A secure HTTPS endpoint is required.")
    }
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 180_000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $key")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
    }
    try {
        val payload = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("n", 1)
            put("size", if (square) "1024x1024" else "1536x1024")
        }.toString()
        connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching {
                JSONObject(body).optJSONObject("error")?.optString("message")
            }.getOrNull()
            throw IOException(message?.take(190)?.ifBlank { null } ?: "Provider returned HTTP $code.")
        }
        val root = JSONObject(body)
        val item = root.optJSONArray("data")?.optJSONObject(0)
            ?: throw IOException("Provider returned no image data.")
        val base64 = item.optString("b64_json")
        if (base64.isNotBlank()) {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IOException("Provider image could not be decoded.")
        }
        val imageUrl = item.optString("url")
        if (imageUrl.isBlank()) throw IOException("Provider returned neither image data nor a URL.")
        return@withContext fetchImage(imageUrl)
    } finally {
        connection.disconnect()
    }
}

private fun fetchImage(imageUrl: String): Bitmap {
    val url = URL(imageUrl)
    if (!url.protocol.equals("https", ignoreCase = true)) {
        throw IOException("Provider returned an insecure image URL.")
    }
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 120_000
        instanceFollowRedirects = true
    }
    try {
        val code = connection.responseCode
        if (code !in 200..299) throw IOException("Image download failed with HTTP $code.")
        val bytes = connection.inputStream.use { it.readBytes() }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Provider image could not be decoded.")
    } finally {
        connection.disconnect()
    }
}
