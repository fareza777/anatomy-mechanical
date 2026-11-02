package studio.anatomy.app.ui.studio

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.data.PlateExporter
import studio.anatomy.app.data.PlateResult
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.ui.components.FilterPill
import studio.anatomy.app.ui.components.LabelWithDot
import studio.anatomy.app.ui.components.MonoLabel
import studio.anatomy.app.ui.components.PrimaryButton
import studio.anatomy.app.ui.components.SheetSystemBars
import studio.anatomy.app.ui.theme.JetBrainsMono
import studio.anatomy.app.ui.theme.LocalAnatomyColors
import studio.anatomy.app.ui.theme.SpaceGrotesk

private enum class ExportFormat(val label: String) { PNG("PNG"), SVG("SVG VECTOR") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioExportSheet(
    spec: ObjectSpec,
    state: StudioState,
    anim: AnimContext,
    explodeMap: Map<String, Float>,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit,
) {
    val colors = LocalAnatomyColors.current
    SheetSystemBars(colors.isDark)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scene = remember(spec.id) { AnatomyCatalog.scene(spec) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var format by remember { mutableStateOf(ExportFormat.PNG) }
    var longEdge by remember { mutableIntStateOf(2560) }
    var exporting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PlateResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!exporting) onDismiss() },
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
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LabelWithDot("EXPORT / PLATE", color = colors.accent)
            Spacer(Modifier.height(8.dp))
            Text(
                "Take the study with you.",
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 28.sp,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "A titled, print-ready plate rendered from the live scene.",
                color = colors.textSecondary,
                fontFamily = studio.anatomy.app.ui.theme.Manrope,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(18.dp))
            Hairline2()
            Spacer(Modifier.height(14.dp))
            MonoLabel("FORMAT", color = colors.textTertiary, fontSize = 8)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { entry ->
                    FilterPill(
                        text = entry.label,
                        selected = format == entry,
                        onClick = { if (!exporting) { format = entry; result = null; error = null } },
                    )
                }
            }
            if (format == ExportFormat.PNG) {
                Spacer(Modifier.height(14.dp))
                MonoLabel("RESOLUTION", color = colors.textTertiary, fontSize = 8)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResolutionCard("2K", "2560 × 1600", selected = longEdge == 2560, enabled = !exporting) { longEdge = 2560 }
                    ResolutionCard("4K", "3840 × 2400", selected = longEdge == 3840, enabled = !exporting) { longEdge = 3840 }
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Infinite-scale vector geometry: every visible face, the floor grid, and the typography are written as editable SVG paths.",
                    color = colors.textTertiary,
                    fontFamily = studio.anatomy.app.ui.theme.Manrope,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(18.dp))
            if (error != null) {
                Text(
                    error.orEmpty(),
                    color = colors.danger,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(10.dp))
            }
            if (result == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(
                        text = if (exporting) "Rendering" else "Render plate",
                        onClick = {
                            if (exporting) return@PrimaryButton
                            exporting = true
                            error = null
                            scope.launch {
                                try {
                                    val exported = if (format == ExportFormat.PNG) {
                                        PlateExporter.exportPng(
                                            context = context.applicationContext,
                                            spec = spec,
                                            scene = scene,
                                            pose = state.pose,
                                            anim = anim,
                                            explode = explodeMap,
                                            selectedSystem = state.selectedSystem,
                                            xray = state.xray,
                                            longEdge = longEdge,
                                        )
                                    } else {
                                        PlateExporter.exportSvg(
                                            context = context.applicationContext,
                                            spec = spec,
                                            scene = scene,
                                            pose = state.pose,
                                            anim = anim,
                                            explode = explodeMap,
                                            selectedSystem = state.selectedSystem,
                                            xray = state.xray,
                                        )
                                    }
                                    result = exported
                                } catch (failure: Exception) {
                                    error = "COULD NOT RENDER PLATE / ${failure.javaClass.simpleName.uppercase()}"
                                    android.util.Log.e("AnatomyExport", "Export failed", failure)
                                } finally {
                                    exporting = false
                                }
                            }
                        },
                        enabled = !exporting,
                        modifier = Modifier.weight(1f),
                    )
                    if (exporting) {
                        Spacer(Modifier.width(14.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colors.accent,
                            strokeWidth = 1.6.dp,
                        )
                    }
                }
            } else {
                ExportSuccess(
                    result = result!!,
                    onShare = {
                        runCatching { PlateExporter.share(context, result!!, spec.title) }
                            .onFailure { onToast("NO APP AVAILABLE TO SHARE") }
                    },
                    onClose = onDismiss,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (Build.VERSION.SDK_INT >= 29) {
                    "PNG → Pictures/Anatomy Studio · SVG → Downloads/Anatomy Studio"
                } else {
                    "Plates are rendered into the app cache and shared securely. Gallery saving requires Android 10 or newer."
                },
                color = colors.textTertiary.copy(alpha = 0.8f),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
private fun ResolutionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.surfaceHigh.copy(alpha = 0.5f))
            .border(1.dp, if (selected) colors.accent else colors.line, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(
            title,
            color = if (selected) colors.accent else colors.text,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
        Text(
            subtitle,
            color = colors.textTertiary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun ExportSuccess(result: PlateResult, onShare: () -> Unit, onClose: () -> Unit) {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accent.copy(alpha = 0.07f))
            .border(1.dp, colors.accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, null, tint = colors.ink, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Plate rendered",
                    color = colors.text,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                )
                Text(
                    if (result.inGallery) "SAVED TO GALLERY / READY TO SHARE" else "READY TO SHARE",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            result.fileName,
            color = colors.textTertiary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                text = "Share plate",
                onClick = onShare,
                icon = Icons.Outlined.Share,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "CLOSE",
                color = colors.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, colors.line, RoundedCornerShape(50))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun Hairline2() {
    val colors = LocalAnatomyColors.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lineSoft))
}

@Composable
private fun IconPlaceholder() {
    Icon(Icons.Outlined.Check, null, tint = Color.Transparent, modifier = Modifier.size(0.dp))
}
