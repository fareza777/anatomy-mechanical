package studio.anatomy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.anatomy.app.data.AnatomyCatalog
import studio.anatomy.app.data.AnatomyPrefs
import studio.anatomy.app.data.LocalStudioSettings
import studio.anatomy.app.data.StudioSettings
import studio.anatomy.app.model.ObjectSpec
import studio.anatomy.app.render.RenderQuality
import studio.anatomy.app.ui.components.AnatomyDock
import studio.anatomy.app.ui.components.CornerTicks
import studio.anatomy.app.ui.components.DockTab
import studio.anatomy.app.ui.components.FilterPill
import studio.anatomy.app.ui.components.GlassIconButton
import studio.anatomy.app.ui.components.Hairline
import studio.anatomy.app.ui.components.LabelWithDot
import studio.anatomy.app.ui.components.MetaRow
import studio.anatomy.app.ui.components.ModelPlate
import studio.anatomy.app.ui.components.MonoLabel
import studio.anatomy.app.ui.components.PrimaryButton
import studio.anatomy.app.ui.components.SecondaryButton
import studio.anatomy.app.ui.components.SheetSystemBars
import studio.anatomy.app.ui.components.TechnicalBackdrop
import studio.anatomy.app.ui.theme.AnatomyColors
import studio.anatomy.app.ui.theme.AnatomyType
import studio.anatomy.app.ui.theme.JetBrainsMono
import studio.anatomy.app.ui.theme.LocalAnatomyColors
import studio.anatomy.app.ui.theme.SpaceGrotesk

@Composable
fun CatalogScreen(
    specs: List<ObjectSpec>,
    favorites: Set<String>,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAI: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticsOn = remember { AnatomyPrefs(context).haptics }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    val filtered = remember(specs, selectedCategory, favoritesOnly, favorites) {
        specs.filter { spec ->
            val categoryMatch = selectedCategory == "ALL" || spec.category == selectedCategory
            val favoriteMatch = !favoritesOnly || spec.id in favorites
            categoryMatch && favoriteMatch
        }
    }
    val defaultView = selectedCategory == "ALL" && !favoritesOnly
    val hero = if (defaultView) specs.firstOrNull() else filtered.firstOrNull()
    val cards = if (defaultView) filtered.filter { it.id != hero?.id } else filtered

    BackHandler(enabled = showSearch) { showSearch = false }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.ink)) {
        val landscape = maxWidth > maxHeight
        TechnicalBackdrop(Modifier.fillMaxSize(), accent = colors.accent, glowCenterX = 0.74f)

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1.04f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 96.dp)
                ) {
                    CatalogHeader(
                        objectCount = specs.size,
                        onSearch = { showSearch = true },
                        compact = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (hero != null) {
                        FeatureStrip(
                            spec = hero,
                            onOpen = {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpen(hero.id)
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line.copy(alpha = 0.7f)))
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .statusBarsPadding()
                        .padding(end = 40.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 96.dp)
                ) {
                    CollectionHeader(
                        shown = if (defaultView) specs.size else filtered.size,
                        total = specs.size,
                        filtered = !defaultView,
                        compact = true,
                    )
                    FilterRow(
                        categories = AnatomyCatalog.categories,
                        selectedCategory = selectedCategory,
                        favoritesOnly = favoritesOnly,
                        onCategory = {
                            selectedCategory = it
                            if (it != "ALL") favoritesOnly = false
                        },
                        onFavorites = { favoritesOnly = !favoritesOnly },
                    )
                    Spacer(Modifier.height(4.dp))
                    val listed = if (defaultView) specs else filtered
                    listed.forEach { spec ->
                        AtlasCard(
                            spec = spec,
                            isFavorite = spec.id in favorites,
                            onOpen = {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpen(spec.id)
                            },
                            onFavorite = {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleFavorite(spec.id)
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp),
        ) {
            item {
                CatalogHeader(
                    objectCount = specs.size,
                    onSearch = { showSearch = true },
                )
            }
            if (hero != null) {
                item {
                    Column {
                        FeaturedCard(
                            spec = hero,
                            isFavorite = hero.id in favorites,
                            onOpen = {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpen(hero.id)
                            },
                            onFavorite = {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleFavorite(hero.id)
                            },
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
            item {
                CollectionHeader(
                    shown = if (defaultView) specs.size else filtered.size,
                    total = specs.size,
                    filtered = !defaultView,
                )
            }
            item {
                FilterRow(
                    categories = AnatomyCatalog.categories,
                    selectedCategory = selectedCategory,
                    favoritesOnly = favoritesOnly,
                    onCategory = {
                        selectedCategory = it
                        if (it != "ALL") favoritesOnly = false
                    },
                    onFavorites = { favoritesOnly = !favoritesOnly },
                )
            }
            if (cards.isEmpty()) {
                item { EmptyCollection(favoritesOnly) }
            } else {
                items(cards, key = { it.id }) { spec ->
                    AtlasCard(
                        spec = spec,
                        isFavorite = spec.id in favorites,
                        onOpen = {
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpen(spec.id)
                        },
                        onFavorite = {
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFavorite(spec.id)
                        },
                    )
                }
            }
            item { CatalogFooter() }
        }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.ink)
                .navigationBarsPadding()
                .padding(horizontal = 42.dp)
                .padding(vertical = 9.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line.copy(alpha = 0.7f))
            )
            Spacer(Modifier.height(8.dp))
            AnatomyDock(
                selected = DockTab.ATLAS,
                onSelect = { tab ->
                    when (tab) {
                        DockTab.ATLAS -> Unit
                        DockTab.AI -> onOpenAI()
                        DockTab.SYSTEM -> onOpenSystem()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.ink, colors.ink.copy(alpha = 0.92f), Color.Transparent)
                    )
                )
        )

        if (landscape) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.ink.copy(alpha = 0.82f), colors.ink)
                        )
                    )
            )

            AnatomyDock(
                selected = DockTab.ATLAS,
                onSelect = { tab ->
                    when (tab) {
                        DockTab.ATLAS -> Unit
                        DockTab.AI -> onOpenAI()
                        DockTab.SYSTEM -> onOpenSystem()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 42.dp)
                    .padding(bottom = 10.dp),
            )
        }

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 8 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { -it / 10 },
        ) {
            SearchOverlay(
                specs = specs,
                favorites = favorites,
                onClose = { showSearch = false },
                onOpen = { id ->
                    showSearch = false
                    onOpen(id)
                },
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun CatalogHeader(objectCount: Int, onSearch: () -> Unit, compact: Boolean = false) {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .statusBarsPadding()
            .padding(horizontal = if (compact) 18.dp else 22.dp)
            .padding(top = if (compact) 4.dp else 10.dp)
    ) {
        if (!compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("VOL. 01 / MECHANICAL ATLAS", color = colors.textTertiary, fontSize = 8, letterSpacing = 1.4f)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.height(1.dp).weight(1f).background(colors.line.copy(alpha = 0.9f)))
                Spacer(Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(4.dp).background(colors.accent, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    MonoLabel("OFFLINE", color = colors.accent.copy(alpha = 0.8f), fontSize = 8, letterSpacing = 1.0f)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(if (compact) 34.dp else 44.dp)
                    .background(
                        Brush.verticalGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.05f)))
                    )
            )
            Spacer(Modifier.width(if (compact) 10.dp else 12.dp))
            Column {
                Text(
                    "ANATOMY",
                    color = colors.text,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (compact) 24.sp else 29.sp,
                    lineHeight = if (compact) 25.sp else 30.sp,
                    letterSpacing = if (compact) 3.8.sp else 4.4.sp,
                )
                Text(
                    "INTERACTIVE 3D STUDIO",
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = if (compact) 7.5.sp else 8.5.sp,
                    letterSpacing = if (compact) 1.6.sp else 1.9.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            if (compact) {
                MonoLabel("OFFLINE", color = colors.accent.copy(alpha = 0.8f), fontSize = 7, letterSpacing = 1f)
                Spacer(Modifier.width(8.dp))
            }
            GlassIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = "Search the atlas",
                onClick = onSearch,
                size = if (compact) 38.dp else 42.dp,
                iconSize = if (compact) 16.dp else 18.dp,
            )
        }
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        if (compact) {
            Text(
                "Cut open the machines that move the world.",
                color = colors.textSecondary,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                letterSpacing = (-0.1).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
        } else {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "Cut open the machines\nthat move the world.",
                color = colors.textSecondary,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    objectCount.toString().padStart(2, '0'),
                    color = colors.accent.copy(alpha = 0.3f),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Light,
                    fontSize = 44.sp,
                    lineHeight = 38.sp,
                )
                MonoLabel("OBJECTS", color = colors.textTertiary, fontSize = 7, letterSpacing = 1.2f)
            }
        }
        Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun FeaturedCard(
    spec: ObjectSpec,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val accent = specAccent(spec, colors)
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onOpen)
    ) {            Box(
                Modifier
                    .fillMaxWidth()
                    .height(122.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            ) {
            ModelPlate(
                spec = spec,
                widthPx = 900,
                heightPx = 560,
                quality = RenderQuality.FULL,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.stageInk.copy(alpha = 0.55f), colors.surface)
                        )
                    )
            )
            Text(
                spec.index.toString().padStart(2, '0'),
                color = colors.stageText.copy(alpha = 0.08f),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 92.sp,
                letterSpacing = (-4).sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 18.dp),
            )
            Row(
                Modifier.padding(14.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeaturedBadge("${spec.index.toString().padStart(2, '0')} / FEATURED")
                Spacer(Modifier.width(7.dp))
                FeaturedBadge("LIVE 3D", accent = true)
                Spacer(Modifier.weight(1f))
                GlassIconButton(
                    icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    onClick = onFavorite,
                    size = 38.dp,
                    iconSize = 17.dp,
                    tint = if (isFavorite) colors.amber else colors.stageText,
                    background = colors.stageInk.copy(alpha = 0.62f),
                    borderColor = colors.stageLine,
                )
            }
            CornerTicks(
                Modifier.fillMaxSize(),
                color = colors.stageAccent.copy(alpha = 0.35f),
                length = 14.dp,
                inset = 12.dp,
            )
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 13.dp)) {
            LabelWithDot(spec.category, color = accent, dotColor = accent)
            Spacer(Modifier.height(4.dp))
            Text(
                spec.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.8).sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                spec.tagline,
                color = colors.textSecondary,
                style = AnatomyType.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaRow(
                    listOf(
                        "${spec.partCount} PARTS",
                        "${spec.systemNames.size} SYSTEMS",
                        spec.designation.substringBefore(" /"),
                    ),
                    color = colors.textTertiary,
                )
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = "Open 3D study",
                onClick = onOpen,
                icon = Icons.Outlined.ArrowOutward,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeatureStrip(
    spec: ObjectSpec,
    onOpen: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val accent = specAccent(spec, colors)
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onOpen)
    ) {
        Box(Modifier.width(158.dp).fillMaxHeight()) {
            ModelPlate(
                spec = spec,
                widthPx = 620,
                heightPx = 620,
                quality = RenderQuality.FULL,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.55f to Color.Transparent,
                            1f to colors.surface.copy(alpha = 0.9f),
                        )
                    )
            )
            Row(Modifier.padding(9.dp)) {
                FeaturedBadge("${spec.index.toString().padStart(2, '0')} / FEATURED")
            }
            CornerTicks(
                Modifier.fillMaxSize(),
                color = colors.stageAccent.copy(alpha = 0.35f),
                length = 12.dp,
                inset = 9.dp,
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(13.dp)) {
            LabelWithDot(spec.category, color = accent, dotColor = accent)
            Spacer(Modifier.height(4.dp))
            Text(
                spec.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 23.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.7).sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                spec.tagline,
                color = colors.textSecondary,
                style = AnatomyType.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaRow(
                    listOf("${spec.partCount} PARTS", "${spec.systemNames.size} SYSTEMS"),
                    color = colors.textTertiary,
                )
                Spacer(Modifier.weight(1f))
                MonoLabel("OPEN", color = accent, fontSize = 7, letterSpacing = 1f)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.ArrowOutward, null, tint = accent, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun FeaturedBadge(text: String, accent: Boolean = false) {
    val colors = LocalAnatomyColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (accent) colors.stageAccent.copy(alpha = 0.16f) else colors.stageInk.copy(alpha = 0.62f))
            .border(1.dp, if (accent) colors.stageAccent.copy(alpha = 0.42f) else colors.stageLine, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent) {
            Box(
                Modifier
                    .size(4.dp)
                    .background(colors.stageAccent, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text.uppercase(),
            color = if (accent) colors.stageAccent else colors.stageText,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 1.1.sp,
        )
    }
}

@Composable
private fun CollectionHeader(shown: Int, total: Int, filtered: Boolean, compact: Boolean = false) {
    val colors = LocalAnatomyColors.current
    Row(
        Modifier
            .padding(horizontal = if (compact) 18.dp else 22.dp)
            .padding(top = if (compact) 14.dp else 26.dp, bottom = if (compact) 9.dp else 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            LabelWithDot("THE COLLECTION", color = colors.accent)
            Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
            Text(
                "All systems",
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = if (compact) 21.sp else 27.sp,
                letterSpacing = (-0.7).sp,
            )
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                shown.toString().padStart(2, '0'),
                color = colors.textSecondary,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 26.sp,
                lineHeight = 26.sp,
            )
            MonoLabel(
                if (filtered) "OF ${total.toString().padStart(2, '0')} PLATES" else "PLATES / LIVE",
                color = colors.textTertiary,
                fontSize = 7.5f.toInt(),
                letterSpacing = 1f,
            )
        }
    }
}

@Composable
private fun FilterRow(
    categories: List<String>,
    selectedCategory: String,
    favoritesOnly: Boolean,
    onCategory: (String) -> Unit,
    onFavorites: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterPill(
                text = category,
                selected = !favoritesOnly && selectedCategory == category,
                onClick = { onCategory(category) },
            )
        }
        FilterPill(
            text = "FAVORITES",
            selected = favoritesOnly,
            onClick = onFavorites,
            icon = if (favoritesOnly) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
        )
    }
}

@Composable
private fun AtlasCard(
    spec: ObjectSpec,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val accent = specAccent(spec, colors)
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .padding(horizontal = 18.dp, vertical = 7.dp)
            .fillMaxWidth()
            .height(152.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(accent, CircleShape))
                Spacer(Modifier.width(7.dp))
                MonoLabel(
                    "${spec.index.toString().padStart(2, '0')} / ${spec.category}",
                    color = accent,
                    fontSize = 7.5f.toInt(),
                    letterSpacing = 1.1f,
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                spec.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                spec.tagline,
                color = colors.textSecondary,
                style = AnatomyType.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaRow(
                    listOf("${spec.partCount} PARTS", "${spec.systemNames.size} SYSTEMS"),
                    color = colors.textTertiary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Outlined.ArrowOutward,
                    null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Box(
            Modifier
                .width(128.dp)
                .fillMaxHeight()
                .background(colors.stageInk)
        ) {
            ModelPlate(
                spec = spec,
                widthPx = 420,
                heightPx = 500,
                quality = RenderQuality.INTERACTIVE,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(26.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(colors.surface, colors.surface.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
            GlassIconButton(
                icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                onClick = onFavorite,
                size = 32.dp,
                iconSize = 14.dp,
                tint = if (isFavorite) colors.amber else colors.stageTextSecondary,
                background = colors.stageInk.copy(alpha = 0.62f),
                borderColor = colors.stageLine,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.stageAccent)
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ArrowOutward,
                    null,
                    tint = colors.stageInk,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyCollection(favoritesOnly: Boolean) {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "00",
            color = colors.line,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Light,
            fontSize = 76.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (favoritesOnly) "No favorites yet." else "No plates in this sector.",
            color = colors.textSecondary,
            style = AnatomyType.body,
        )
        Spacer(Modifier.height(4.dp))
        MonoLabel("MARK A PLATE WITH THE HEART TO KEEP IT HERE", color = colors.textTertiary, fontSize = 8)
    }
}

@Composable
private fun CatalogFooter() {
    val colors = LocalAnatomyColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 30.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Hairline(Modifier.width(42.dp), colors.line)
        Spacer(Modifier.height(16.dp))
        MonoLabel(
            "NATIVE 3D / KOTLIN + COMPOSE / OFFLINE FIRST",
            color = colors.textTertiary,
            fontSize = 7,
            letterSpacing = 1.3f,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Concept language inspired by the open-source anatomy project (MIT).",
            color = colors.textTertiary.copy(alpha = 0.7f),
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun SearchOverlay(
    specs: List<ObjectSpec>,
    favorites: Set<String>,
    onClose: () -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val colors = LocalAnatomyColors.current
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val results = remember(query, specs) { AnatomyCatalog.search(query, false, favorites) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.ink.copy(alpha = 0.985f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = "Close search",
                onClick = {
                    keyboard?.hide()
                    onClose()
                },
                size = 40.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                MonoLabel("SEARCH / ATLAS", color = colors.accent, fontSize = 8, letterSpacing = 1.4f)
                Text(
                    "Find a system.",
                    color = colors.text,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp,
                    letterSpacing = (-0.4).sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = colors.textTertiary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                textStyle = AnatomyType.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Turbo machinery, hydraulics, kinetics…",
                                color = colors.textTertiary,
                                style = AnatomyType.body,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Outlined.Close,
                    "Clear",
                    tint = colors.textTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { query = "" },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        if (query.isEmpty()) {
            MonoLabel("SUGGESTED QUERIES", color = colors.textTertiary, fontSize = 8)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TURBOFAN", "HYDRAULICS", "KINEMATICS", "ENERGY", "BIONICS").forEach { suggestion ->
                    FilterPill(
                        text = suggestion,
                        selected = false,
                        onClick = { query = suggestion.lowercase() },
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }
        MonoLabel(
            if (query.isEmpty()) "ALL PLATES" else "RESULTS / ${results.size.toString().padStart(2, '0')}",
            color = colors.textTertiary,
            fontSize = 8,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            if (results.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Ø", color = colors.line, fontFamily = SpaceGrotesk, fontSize = 54.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No plates match that query.", color = colors.textSecondary, style = AnatomyType.body)
                    }
                }
            } else {
                items(results, key = { it.id }) { spec ->
                    SearchResultRow(
                        spec = spec,
                        isFavorite = spec.id in favorites,
                        onOpen = { onOpen(spec.id) },
                        onFavorite = { onToggleFavorite(spec.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    spec: ObjectSpec,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 104.dp, height = 68.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.line, RoundedCornerShape(12.dp))
        ) {
            ModelPlate(
                spec = spec,
                widthPx = 300,
                heightPx = 200,
                quality = RenderQuality.INTERACTIVE,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            MonoLabel("${spec.index.toString().padStart(2, '0')} / ${spec.category}", color = spec.accent.toComposeColor(), fontSize = 8)
            Spacer(Modifier.height(4.dp))
            Text(
                spec.title,
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                maxLines = 1,
            )
            Text(
                spec.tagline,
                color = colors.textTertiary,
                style = AnatomyType.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlassIconButton(
            icon = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
            onClick = onFavorite,
            size = 34.dp,
            iconSize = 15.dp,
            tint = if (isFavorite) colors.amber else colors.textTertiary,
            background = Color.Transparent,
            borderColor = Color.Transparent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SystemSheet(
    appearance: String,
    textScale: Float,
    favoritesCount: Int,
    onAppearance: (String) -> Unit,
    onTextScale: (Float) -> Unit,
    onClearFavorites: () -> Unit,
    onDismiss: () -> Unit,
    adFreeUntil: Long = 0L,
    onRewardAd: () -> Unit = {},
    removeAdsOwned: Boolean = false,
    removeAdsPrice: String = "\$4.99",
    onRemoveAds: () -> Unit = {},
    onOpenAI: () -> Unit,
) {
    val colors = LocalAnatomyColors.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val prefs = remember { AnatomyPrefs(context) }
    val studio = LocalStudioSettings.current ?: remember { StudioSettings(prefs) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmClear by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).size(width = 34.dp, height = 3.dp).background(colors.line, CircleShape)) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SheetSystemBars(colors.isDark)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabelWithDot("SYSTEM / STUDIO CONFIG", color = colors.accent)
                Spacer(Modifier.weight(1f))
                GlassIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Close settings",
                    onClick = onDismiss,
                    size = 36.dp,
                    iconSize = 16.dp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tune the instrument.",
                color = colors.text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Light,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.9).sp,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Light or dark, readable or compact, crisp or fast. Everything here applies immediately and is stored on this device.",
                color = colors.textSecondary,
                style = AnatomyType.body,
            )
            Spacer(Modifier.height(18.dp))
            Hairline()

            SettingsLabel("APPEARANCE", "Light is the default. Auto follows the phone.")
            SettingsPills {
                FilterPill("LIGHT", appearance == AnatomyPrefs.APPEARANCE_LIGHT, onClick = { onAppearance(AnatomyPrefs.APPEARANCE_LIGHT) })
                FilterPill("DARK", appearance == AnatomyPrefs.APPEARANCE_DARK, onClick = { onAppearance(AnatomyPrefs.APPEARANCE_DARK) })
                FilterPill("AUTO", appearance == AnatomyPrefs.APPEARANCE_SYSTEM, onClick = { onAppearance(AnatomyPrefs.APPEARANCE_SYSTEM) })
            }

            SettingsLabel("TEXT SIZE", "Every label, hint, and readout in the atlas.")
            SettingsPills {
                FilterPill("COMPACT", textScale < 1.12f, onClick = { onTextScale(1.08f) })
                FilterPill("STANDARD", textScale >= 1.12f && textScale < 1.2f, onClick = { onTextScale(1.16f) })
                FilterPill("LARGE", textScale >= 1.2f && textScale < 1.28f, onClick = { onTextScale(AnatomyPrefs.DEFAULT_TEXT_SCALE) })
                FilterPill("XL", textScale >= 1.28f, onClick = { onTextScale(1.34f) })
            }
            SettingsHint("LARGE is selected by default for comfortable reading on a phone.")

            SettingsLabel("RENDER DETAIL", "How much detail the stage keeps while it moves.")
            SettingsPills {
                FilterPill("LITE", studio.renderQuality == AnatomyPrefs.QUALITY_LITE, onClick = { studio.updateRenderQuality(AnatomyPrefs.QUALITY_LITE) })
                FilterPill("BALANCED", studio.renderQuality == AnatomyPrefs.QUALITY_BALANCED, onClick = { studio.updateRenderQuality(AnatomyPrefs.QUALITY_BALANCED) })
                FilterPill("SHARP", studio.renderQuality == AnatomyPrefs.QUALITY_SHARP, onClick = { studio.updateRenderQuality(AnatomyPrefs.QUALITY_SHARP) })
            }
            SettingsHint(
                when (studio.renderQuality) {
                    AnatomyPrefs.QUALITY_LITE -> "Lowest load and the coolest phone. Still frames stay fully sharp."
                    AnatomyPrefs.QUALITY_SHARP -> "Highest moving resolution and every outline. Best on a recent phone or while charging."
                    else -> "Still frames render fully sharp; moving frames keep full colours with a mild resolution trade for 30 fps. Right for most phones."
                }
            )

            SettingsLabel("FRAME RATE", "Animation and camera smoothness.")
            SettingsPills {
                FilterPill("30 FPS", studio.frameRate == 30, onClick = { studio.updateFrameRate(30) })
                FilterPill("45 FPS", studio.frameRate == 45, onClick = { studio.updateFrameRate(45) })
                FilterPill("60 FPS", studio.frameRate == 60, onClick = { studio.updateFrameRate(60) })
            }
            SettingsHint("30 FPS is the default and the safest choice for mid-range hardware. Raise it if the stage stays smooth.")

            SettingsLabel("STAGE LAYERS", "What appears when a 3D study opens.")
            SettingSwitch(
                title = "FLOOR GRID",
                detail = "Registration grid and turntable ring under the model.",
                checked = studio.gridDefault,
                onChange = { studio.updateGridDefault(it) },
            )
            SettingSwitch(
                title = "HOTSPOT PINS",
                detail = "Numbered explanation targets on the model.",
                checked = studio.pinsDefault,
                onChange = { studio.updatePinsDefault(it) },
            )

            SettingsLabel("MOTION", "Mechanism animation budget.")
            SettingsPills {
                FilterPill("FULL", studio.motion == "FULL", onClick = { studio.updateMotion("FULL") })
                FilterPill("REDUCED", studio.motion == "REDUCED", onClick = { studio.updateMotion("REDUCED") })
                FilterPill("OFF", studio.motion == "OFF", onClick = { studio.updateMotion("OFF") })
            }
            SettingsHint("OFF opens every model paused; the transport controls still start it manually.")
            SettingSwitch(
                title = "AUTO-ANIMATE",
                detail = "Start mechanisms moving when a study opens. Off keeps the opening frame sharp and light.",
                checked = studio.autoplay,
                onChange = { studio.updateAutoplay(it) },
            )

            SettingsLabel("TOUCH & SCREEN", "Feedback and study comfort.")
            SettingSwitch(
                title = "HAPTIC FEEDBACK",
                detail = "A short pulse when a pin, mode, or plate is selected.",
                checked = studio.haptics,
                onChange = { studio.updateHaptics(it) },
            )
            SettingSwitch(
                title = "KEEP SCREEN ON",
                detail = "Stops the display sleeping while a 3D study is open.",
                checked = studio.keepAwake,
                onChange = { studio.updateKeepAwake(it) },
            )

            SettingsLabel(
                "AD-FREE PASS",
                "Watch one short ad and the atlas runs without banners or full-screen ads for 12 hours.",
            )
            val adFreeActive = adFreeUntil > System.currentTimeMillis()
            if (adFreeActive) {
                val untilText = remember(adFreeUntil) {
                    android.text.format.DateFormat.getTimeFormat(context)
                        .format(java.util.Date(adFreeUntil))
                }
                SettingsHint("Active until $untilText. Banners and interstitials are off.")
            } else {
                SecondaryButton(
                    text = "Watch ad / 12 h ad-free",
                    onClick = onRewardAd,
                    icon = Icons.Outlined.PlayCircleOutline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsLabel(
                "REMOVE ADS",
                "One-time purchase. Removes banners and interstitials from the atlas for good.",
            )
            if (removeAdsOwned) {
                SettingsHint("Ads removed on this account. Thank you.")
            } else {
                SecondaryButton(
                    text = "Remove ads — $removeAdsPrice",
                    onClick = onRemoveAds,
                    icon = Icons.Outlined.Block,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(14.dp))
            Hairline()
            SettingsLabel("LIBRARY", "${favoritesCount.toString().padStart(2, '0')} FAVORITES / ${AnatomyCatalog.specs.size.toString().padStart(2, '0')} OBJECTS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = if (confirmClear) "Tap again" else "Clear favorites",
                    onClick = {
                        if (favoritesCount == 0) return@SecondaryButton
                        if (confirmClear) {
                            onClearFavorites()
                            confirmClear = false
                        } else {
                            confirmClear = true
                        }
                    },
                    icon = Icons.Outlined.FavoriteBorder,
                    active = confirmClear,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Reset settings",
                    onClick = {
                        studio.reset()
                        onAppearance(prefs.appearance)
                        onTextScale(prefs.textScale)
                        confirmClear = false
                    },
                    icon = Icons.Outlined.Tune,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(16.dp))
            SystemRow("VERSION", "1.0.0 / ATLAS 01")
            SystemRow("RENDERER", "NATIVE 3D / PROCEDURAL")
            SystemRow("COLLECTION", "${AnatomyCatalog.specs.size.toString().padStart(2, '0')} OBJECTS")
            SystemRow("NETWORK", "AI LAB ONLY / OPTIONAL")
            SystemRow("LICENSE", "MIT CONCEPT / ORIGINAL CODE")
            Spacer(Modifier.height(16.dp))
            Text(
                "Concept and craft language inspired by the open-source anatomy project by Ryan, used under the MIT License. All 3D meshes, the renderer, and the interface in this app are original implementations.",
                color = colors.textTertiary,
                style = AnatomyType.bodySmall,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, colors.line, RoundedCornerShape(50))
                    .clickable {
                        uriHandler.openUri("https://github.com/wheresryan22/anatomy")
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoLabel("VIEW REFERENCE / MIT", color = colors.textSecondary, fontSize = 9)
                Spacer(Modifier.width(7.dp))
                Icon(Icons.Outlined.ArrowOutward, null, tint = colors.accent, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = "Open AI Lab",
                onClick = onOpenAI,
                icon = Icons.Outlined.AutoAwesome,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPills(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = LocalAnatomyColors.current
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            MonoLabel(title, color = colors.text, fontSize = 10, letterSpacing = 1.0f)
            Spacer(Modifier.height(3.dp))
            Text(
                detail,
                color = colors.textTertiary,
                style = AnatomyType.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textTertiary,
                uncheckedTrackColor = colors.surfaceHigh,
                uncheckedBorderColor = colors.line,
            ),
        )
    }
}

@Composable
private fun SettingsLabel(title: String, hint: String) {
    val colors = LocalAnatomyColors.current
    Spacer(Modifier.height(20.dp))
    MonoLabel(title, color = colors.text, fontSize = 11, letterSpacing = 1.1f)
    Spacer(Modifier.height(3.dp))
    Text(
        hint,
        color = colors.textTertiary,
        fontFamily = studio.anatomy.app.ui.theme.Manrope,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsHint(text: String) {
    val colors = LocalAnatomyColors.current
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        color = colors.textTertiary,
        fontFamily = studio.anatomy.app.ui.theme.Manrope,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    )
}

@Composable
private fun SystemRow(key: String, value: String) {
    val colors = LocalAnatomyColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        MonoLabel(key, color = colors.textTertiary, fontSize = 10)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = colors.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun SecondarySystemButton(text: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = color, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 1.1.sp)
    }
}

private fun Int.toComposeColor(): Color = Color(this)

private fun specAccent(spec: ObjectSpec, colors: AnatomyColors): Color {
    val raw = Color(spec.accent)
    return if (colors.isDark) raw else lerp(raw, colors.ink, 0.42f)
}
