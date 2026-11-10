package studio.anatomy.app.ui.studio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.ui.components.FilterPill
import studio.anatomy.app.ui.components.GlassPanel
import studio.anatomy.app.ui.components.Hairline
import studio.anatomy.app.ui.components.LabelWithDot
import studio.anatomy.app.ui.components.MonoLabel
import studio.anatomy.app.ui.components.PrimaryButton
import studio.anatomy.app.ui.components.SecondaryButton
import studio.anatomy.app.ui.theme.JetBrainsMono
import studio.anatomy.app.ui.theme.LocalAnatomyColors
import studio.anatomy.app.ui.theme.SpaceGrotesk
import kotlin.math.roundToInt

enum class StudioMode(val label: String) {
    EXPLORE("EXPLORE"),
    CUTAWAY("CUTAWAY"),
    EXPLODE("EXPLODE"),
    SIMULATE("SIMULATE"),
    LEARN("LEARN"),
}

@Composable
fun StudioConsole(
    state: StudioState,
    anim: AnimContext,
    onHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAnatomyColors.current
    val readouts = state.spec.readouts(anim)
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChangedCompat(onHeight)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        borderColor = colors.line.copy(alpha = 0.9f),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            if (!compact) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 5.dp, bottom = 4.dp)
                        .size(width = 32.dp, height = 3.dp)
                        .background(colors.line, CircleShape)
                )
            }
            ModeTabs(state.mode, compact) { state.enterMode(it) }
            Hairline(color = colors.lineSoft)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (compact) 3.dp else 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                readouts.take(3).forEachIndexed { index, readout ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(horizontal = 10.dp)
                                .width(1.dp)
                                .height(26.dp)
                                .background(colors.lineSoft)
                        )
                    }
                    ConsoleReadout(
                        label = readout.label,
                        value = readout.value,
                        unit = readout.unit,
                        accent = readout.accent,
                        warn = readout.warn,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Hairline(color = colors.lineSoft)
            AnimatedContent(
                targetState = state.mode,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 16 }) togetherWith
                        (fadeOut() + slideOutHorizontally { -it / 16 })
                },
                label = "studio-console-mode",
            ) { mode ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compact) 60.dp else 68.dp)
                        .padding(horizontal = 16.dp)
                        .padding(top = if (compact) 4.dp else 6.dp, bottom = if (compact) 4.dp else 6.dp)
                ) {
                    when (mode) {
                        StudioMode.EXPLORE -> ExplorePanel(state, compact)
                        StudioMode.CUTAWAY -> CutawayPanel(state)
                        StudioMode.EXPLODE -> ExplodePanel(state)
                        StudioMode.SIMULATE -> SimulatePanel(state)
                        StudioMode.LEARN -> LearnPanel(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleReadout(
    label: String,
    value: String,
    unit: String,
    accent: Boolean,
    warn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnatomyColors.current
    Column(modifier) {
        MonoLabel(label, color = colors.textTertiary, fontSize = 9, letterSpacing = 0.9f)
        Spacer(Modifier.height(2.dp))
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
                fontSize = 24.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    color = colors.textTertiary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun ModeTabs(selected: StudioMode, compact: Boolean, onSelect: (StudioMode) -> Unit) {
    val colors = LocalAnatomyColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 6.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 2.dp),
    ) {
        StudioMode.entries.forEach { mode ->
            val active = mode == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(mode) }
                    .padding(vertical = if (compact) 3.dp else 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    mode.label,
                    color = if (active) colors.accent else colors.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = if (compact) 9.sp else 11.sp,
                    letterSpacing = if (compact) 0.1.sp else 0.4.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(width = if (active) 14.dp else 4.dp, height = 2.dp)
                        .background(if (active) colors.accent else Color.Transparent, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ExplorePanel(state: StudioState, compact: Boolean = false) {
    val colors = LocalAnatomyColors.current
    val spec = state.spec
    Column {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterPill(
                text = "ALL",
                selected = state.selectedSystem == null,
                onClick = { state.selectedSystem = null },
            )
            spec.systemNames.forEach { system ->
                FilterPill(
                    text = system,
                    selected = state.selectedSystem == system,
                    onClick = { state.toggleSystem(system) },
                )
            }
        }
        Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(colors.accent, CircleShape))
            Spacer(Modifier.width(7.dp))
            MonoLabel(
                "${spec.hotspots.size.toString().padStart(2, '0')} EXPLAIN POINTS",
                color = colors.textTertiary,
                fontSize = 9,
                letterSpacing = 0.7f,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PrimaryButton(
                text = "Study",
                onClick = { state.beginStudy() },
                icon = Icons.Outlined.School,
                compact = true,
            )
        }
    }
}

@Composable
private fun CutawayPanel(state: StudioState) {
    val colors = LocalAnatomyColors.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MonoLabel("SHELL TRANSPARENCY", color = colors.textSecondary, fontSize = 8)
                Text(
                    if (state.xray) "Outer casings are see-through. Select a system to isolate it."
                    else "Switch on to reveal the internal architecture.",
                    color = colors.textTertiary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    fontFamily = studio.anatomy.app.ui.theme.Manrope,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = state.xray,
                onCheckedChange = { state.xray = it },
                modifier = Modifier.size(width = 40.dp, height = 30.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.ink,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textTertiary,
                    uncheckedTrackColor = colors.surfaceHigh,
                    uncheckedBorderColor = colors.line,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.spec.systemNames.forEach { system ->
                val count = state.spec.parts.count { it.system == system }
                FilterPill(
                    text = "$system · $count",
                    selected = state.selectedSystem == system,
                    onClick = { state.toggleSystem(system) },
                )
            }
        }
    }
}

@Composable
private fun ExplodePanel(state: StudioState) {
    val colors = LocalAnatomyColors.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonoLabel(
                if (state.explodedAssembly == null) "ALL ASSEMBLIES" else state.explodedAssembly!!,
                color = colors.accent,
                fontSize = 8,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(state.explodeTarget * 100).roundToInt()}%",
                color = colors.textSecondary,
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
            )
        }
        Slider(
            value = state.explodeTarget,
            onValueChange = { state.explodeTarget = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().height(30.dp),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.line,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterPill(
                text = "ALL",
                selected = state.explodedAssembly == null,
                onClick = { state.explodedAssembly = null },
            )
            state.spec.assemblyNames.forEach { assembly ->
                FilterPill(
                    text = assembly,
                    selected = state.explodedAssembly == assembly,
                    onClick = {
                        state.explodedAssembly = if (state.explodedAssembly == assembly) null else assembly
                        if (state.explodeTarget < 0.15f) state.explodeTarget = 0.72f
                    },
                )
            }
        }
    }
}

@Composable
private fun SimulatePanel(state: StudioState) {
    val colors = LocalAnatomyColors.current
    val control = state.spec.control
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LabelWithDot(control.label, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                control.hint,
                color = colors.textTertiary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Slider(
            value = state.control,
            onValueChange = { state.control = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().height(30.dp),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.line,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel(control.lowLabel, color = colors.textTertiary, fontSize = 7)
            Spacer(Modifier.weight(1f))
            SecondaryButton(
                text = if (state.playing) "Pause" else "Run",
                onClick = { state.playing = !state.playing },
                icon = if (state.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            )
            Spacer(Modifier.width(6.dp))
            SecondaryButton("Reset", onClick = {
                state.control = control.defaultValue
                state.playbackSpeed = 1f
                state.resetClockRequest++
            })
            Spacer(Modifier.weight(1f))
            MonoLabel(control.highLabel, color = colors.textTertiary, fontSize = 7)
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LabelWithDot("MOTION SPEED", color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    state.playbackSpeed < 0.97f -> String.format("%.2f×  SLOW MOTION", state.playbackSpeed)
                    state.playbackSpeed > 1.03f -> String.format("%.2f×  FAST", state.playbackSpeed)
                    else -> "1.00×  NATURAL"
                },
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp,
                maxLines = 1,
            )
        }
        Slider(
            value = state.playbackSpeed,
            onValueChange = { state.playbackSpeed = it },
            valueRange = 0.25f..2f,
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
            MonoLabel("0.25× SLOW", color = colors.textTertiary, fontSize = 7)
            Spacer(Modifier.weight(1f))
            MonoLabel("1× NATURAL", color = colors.textTertiary, fontSize = 7)
            Spacer(Modifier.weight(1f))
            MonoLabel("2× FAST", color = colors.textTertiary, fontSize = 7)
        }
    }
}

@Composable
private fun LearnPanel(state: StudioState) {
    val colors = LocalAnatomyColors.current
    val steps = state.spec.steps
    if (state.activeStep < 0 || steps.isEmpty()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    MonoLabel("GUIDED STUDY / ${steps.size.toString().padStart(2, '0')} STEPS", color = colors.textSecondary, fontSize = 8)
                    Text(
                        "Follow the system from intake to output.",
                        color = colors.textTertiary,
                        fontFamily = studio.anatomy.app.ui.theme.Manrope,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                }
                PrimaryButton(
                    text = "Begin",
                    onClick = { state.beginStudy() },
                    icon = Icons.Outlined.ArrowForward,
                    compact = true,
                    enabled = steps.isNotEmpty(),
                )
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(colors.line, CircleShape)
                    )
                }
            }
        }
    } else {
        val step = steps[state.activeStep.coerceIn(0, steps.lastIndex)]
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(
                    "STEP ${(state.activeStep + 1).toString().padStart(2, '0')} / ${steps.size.toString().padStart(2, '0')}",
                    color = colors.accent,
                    fontSize = 8,
                )
                Spacer(Modifier.weight(1f))
                MonoLabel(step.system ?: state.spec.category, color = colors.textTertiary, fontSize = 7)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                step.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 18.5.sp,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                step.body,
                color = colors.textSecondary,
                fontFamily = studio.anatomy.app.ui.theme.Manrope,
                fontSize = 12.5.sp,
                lineHeight = 16.5.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (index <= state.activeStep) colors.accent else colors.line,
                                    CircleShape
                                )
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                ConsoleStepButton(Icons.Outlined.ArrowBack, "Previous") { state.moveStep(-1) }
                Spacer(Modifier.width(5.dp))
                ConsoleStepButton(Icons.Outlined.ArrowForward, "Next") { state.moveStep(1) }
                Spacer(Modifier.width(8.dp))
                Text(
                    "EXIT",
                    color = colors.textTertiary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.clickable { state.activeStep = -1 },
                )
            }
        }
    }
}

@Composable
private fun ConsoleStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colors.surfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = colors.textSecondary, modifier = Modifier.size(13.dp))
    }
}

private fun Modifier.onSizeChangedCompat(onHeight: (Int) -> Unit): Modifier =
    this.then(
        Modifier.onSizeChanged { onHeight(it.height) }
    )
