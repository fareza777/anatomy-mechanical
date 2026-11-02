package studio.anatomy.app.ui.components

import android.view.View
import android.view.Window
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import studio.anatomy.app.ui.theme.AnatomyType
import studio.anatomy.app.ui.theme.LocalAnatomyColors
import studio.anatomy.app.ui.theme.SpaceGrotesk

/** Keeps system bars legible while a Compose modal sheet owns the top window. */
@Composable
fun SheetSystemBars(isDark: Boolean) {
    val view = LocalView.current
    SideEffect {
        val dialogWindow = findDialogWindow(view)
        val controller = if (dialogWindow != null) {
            WindowCompat.getInsetsController(dialogWindow, view)
        } else {
            ViewCompat.getWindowInsetsController(view)
        }
        controller?.apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}

private fun findDialogWindow(view: View): Window? {
    var current: View? = view
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    return null
}

/** Deep technical field with registration lines and a soft instrument glow. */
@Composable
fun TechnicalBackdrop(
    modifier: Modifier = Modifier,
    accent: Color = LocalAnatomyColors.current.accent,
    glowCenterX: Float = 0.72f,
    showGrid: Boolean = true,
) {
    val colors = LocalAnatomyColors.current
    Canvas(modifier) {
        drawRect(
            Brush.verticalGradient(
                listOf(colors.ink, colors.ink, colors.inkDeep),
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.075f), accent.copy(alpha = 0.016f), Color.Transparent),
                center = Offset(size.width * glowCenterX, size.height * 0.18f),
                radius = size.width * 0.92f,
            ),
            radius = size.width * 0.92f,
            center = Offset(size.width * glowCenterX, size.height * 0.18f),
        )
        if (!showGrid) return@Canvas
        val step = 56.dp.toPx()
        var x = 0f
        var index = 0
        while (x <= size.width) {
            drawLine(
                color = if (index % 5 == 0) colors.line.copy(alpha = 0.55f) else colors.lineSoft.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.7f,
            )
            x += step
            index++
        }
        var y = 0f
        index = 0
        while (y <= size.height) {
            drawLine(
                color = if (index % 5 == 0) colors.line.copy(alpha = 0.55f) else colors.lineSoft.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.7f,
            )
            y += step
            index++
        }
        val inset = 18.dp.toPx()
        val tick = 12.dp.toPx()
        val tickColor = accent.copy(alpha = 0.26f)
        listOf(
            Offset(inset, inset) to Offset(inset + tick, inset),
            Offset(inset, inset) to Offset(inset, inset + tick),
            Offset(size.width - inset, inset) to Offset(size.width - inset - tick, inset),
            Offset(size.width - inset, inset) to Offset(size.width - inset, inset + tick),
            Offset(inset, size.height - inset) to Offset(inset + tick, size.height - inset),
            Offset(inset, size.height - inset) to Offset(inset, size.height - inset - tick),
            Offset(size.width - inset, size.height - inset) to Offset(size.width - inset - tick, size.height - inset),
            Offset(size.width - inset, size.height - inset) to Offset(size.width - inset, size.height - inset - tick),
        ).forEach { (start, end) -> drawLine(tickColor, start, end, 1.2f) }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderColor: Color = LocalAnatomyColors.current.line.copy(alpha = 0.85f),
    content: @Composable () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(colors.surfaceElevated.copy(alpha = 0.97f), colors.surface.copy(alpha = 0.98f)),
                )
            )
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

@Composable
fun CornerTicks(
    modifier: Modifier = Modifier,
    color: Color = LocalAnatomyColors.current.accent.copy(alpha = 0.5f),
    length: Dp = 16.dp,
    inset: Dp = 14.dp,
) {
    Canvas(modifier) {
        val l = length.toPx()
        val i = inset.toPx()
        val sw = 1.1f
        val points = listOf(
            Offset(i, i) to Offset(i + l, i),
            Offset(i, i) to Offset(i, i + l),
            Offset(size.width - i, i) to Offset(size.width - i - l, i),
            Offset(size.width - i, i) to Offset(size.width - i, i + l),
            Offset(i, size.height - i) to Offset(i + l, size.height - i),
            Offset(i, size.height - i) to Offset(i, size.height - i - l),
            Offset(size.width - i, size.height - i) to Offset(size.width - i - l, size.height - i),
            Offset(size.width - i, size.height - i) to Offset(size.width - i, size.height - i - l),
        )
        points.forEach { (start, end) -> drawLine(color, start, end, sw) }
    }
}

@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalAnatomyColors.current.textTertiary,
    fontSize: Int = 12,
    letterSpacing: Float = 1.2f,
    weight: FontWeight = FontWeight.Normal,
) {
    val resolvedSize = if (fontSize < 11) 11 else fontSize
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = studio.anatomy.app.ui.theme.JetBrainsMono,
        fontWeight = weight,
        fontSize = resolvedSize.sp,
        lineHeight = (resolvedSize + 4).sp,
        letterSpacing = letterSpacing.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun AccentDot(
    color: Color = LocalAnatomyColors.current.accent,
    size: Dp = 5.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size)
            .background(color, CircleShape)
    )
}

@Composable
fun LabelWithDot(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalAnatomyColors.current.accent,
    dotColor: Color = color,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AccentDot(dotColor, 4.dp)
        Spacer(Modifier.width(8.dp))
        MonoLabel(text, color = color, fontSize = 11, letterSpacing = 1.2f)
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 18.dp,
    active: Boolean = false,
    tint: Color? = null,
    borderColor: Color? = null,
    background: Color? = null,
) {
    val colors = LocalAnatomyColors.current
    val resolvedTint = tint ?: if (active) colors.onAccent else colors.text
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background ?: if (active) colors.accent else colors.surface.copy(alpha = 0.82f))
            .border(
                1.dp,
                borderColor ?: if (active) colors.accent else colors.line.copy(alpha = 0.9f),
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = resolvedTint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val colors = LocalAnatomyColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) colors.accent else colors.surfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = if (compact) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text.uppercase(),
            color = if (enabled) colors.onAccent else colors.textTertiary,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
        )
        if (icon != null) {
            Spacer(Modifier.width(9.dp))
            Icon(icon, null, tint = if (enabled) colors.onAccent else colors.textTertiary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
) {
    val colors = LocalAnatomyColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, if (active) colors.accent else colors.line, RoundedCornerShape(50))
            .background(if (active) colors.accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (active) colors.accent else colors.textSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text.uppercase(),
            color = if (active) colors.accent else colors.textSecondary,
            fontFamily = studio.anatomy.app.ui.theme.JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 1.0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalAnatomyColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.accent else colors.surface.copy(alpha = 0.6f))
            .border(1.dp, if (selected) colors.accent else colors.line.copy(alpha = 0.8f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) colors.onAccent else colors.textSecondary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text.uppercase(),
            color = if (selected) colors.onAccent else colors.textSecondary,
            fontFamily = studio.anatomy.app.ui.theme.JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun ReadoutCell(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    warn: Boolean = false,
) {
    val colors = LocalAnatomyColors.current
    Column(modifier) {
        MonoLabel(label, color = colors.textTertiary, fontSize = 9, letterSpacing = 1.1f)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                color = when {
                    warn -> colors.amber
                    accent -> colors.accent
                    else -> colors.text
                },
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 25.sp,
                lineHeight = 27.sp,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    color = colors.textTertiary,
                    style = AnatomyType.monoMicro,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

enum class DockTab { ATLAS, AI, SYSTEM }

@Composable
fun AnatomyDock(
    selected: DockTab,
    onSelect: (DockTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    GlassPanel(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DockItem("ATLAS", Icons.Outlined.ViewInAr, selected == DockTab.ATLAS) { onSelect(DockTab.ATLAS) }
            Box(Modifier.width(1.dp).height(22.dp).background(colors.line))
            DockItem("AI LAB", Icons.Outlined.AutoAwesome, selected == DockTab.AI) { onSelect(DockTab.AI) }
            Box(Modifier.width(1.dp).height(22.dp).background(colors.line))
            DockItem("SYSTEM", Icons.Outlined.Tune, selected == DockTab.SYSTEM) { onSelect(DockTab.SYSTEM) }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .clip(RoundedCornerShape(40.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            null,
            tint = if (selected) colors.accent else colors.textTertiary,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (selected) colors.accent else colors.textTertiary,
            fontFamily = studio.anatomy.app.ui.theme.JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 1.0.sp,
        )
    }
}

@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = LocalAnatomyColors.current.line.copy(alpha = 0.8f),
) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun MetaRow(
    items: List<String>,
    modifier: Modifier = Modifier,
    color: Color = LocalAnatomyColors.current.textTertiary,
    separatorColor: Color = LocalAnatomyColors.current.line,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(Modifier.padding(horizontal = 7.dp).size(2.dp).background(separatorColor, CircleShape))
            }
            Text(
                item.uppercase(),
                color = color,
                fontFamily = studio.anatomy.app.ui.theme.JetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
        }
    }
}
