/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.ozyern.exhale.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.AquamorphicDampingRatio
import com.ozyern.exhale.constants.AquamorphicStiffness
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.ui.component.liquid.LocalAppBackdrop
import com.ozyern.exhale.ui.screens.Screens

/**
 * The app's floating "liquid glass" bottom bar. Presentation-only: it reads live playback
 * state from [LocalPlayerConnection] and drives navigation via callbacks, but never touches
 * the draggable full-screen player sheet directly — tapping the mini pill just asks the host
 * to open it via [onMiniPlayerClick].
 *
 * Two scroll-driven states, morphing into each other with spring physics:
 *  - **State A** (`collapsed == false`, at top): a wide frosted pill holding all main tabs
 *    with a sliding accent indicator, plus a separate frosted Search circle to its right.
 *  - **State B** (`collapsed == true`, scrolled down): a frosted Home circle on the far left,
 *    a center frosted pill (the mini-player `[art | title | play/pause]` when a song is
 *    playing, otherwise the active-tab compact pill), and a frosted Search circle on the right.
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<Screens>,
    pureBlack: Boolean,
    collapsed: Boolean,
    hasNowPlaying: Boolean,
    onMiniPlayerClick: () -> Unit,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tabs shown in the nav pill (Search stays its own circle, never a tab).
    val tabs = remember(items) { items.filter { it.route != Screens.Search.route } }
    val searchScreen = remember(items) { items.find { it.route == Screens.Search.route } }
    val activeTab = tabs.find { isSelected(it) } ?: tabs.firstOrNull()
    val homeTab = tabs.find { it.route == Screens.Home.route } ?: tabs.firstOrNull()

    // Subtle premium haptic tick on every nav interaction. LocalHapticFeedback is the app-wide
    // custom provider that already respects the user's "haptic feedback" preference, so calling
    // it here is a no-op when the user has haptics off.
    val haptic = LocalHapticFeedback.current
    val onItemClickHaptic: (Screens, Boolean) -> Unit = { screen, selected ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onItemClick(screen, selected)
    }
    val onMiniPlayerClickHaptic: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onMiniPlayerClick()
    }

    Row(
        // Fixed height (all children are 64dp) instead of IntrinsicSize.Min — this drops the
        // intrinsic-measurement pass the morph used to trigger on every animation frame.
        modifier = modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = {
                // Smooth spring-based morph: fade + slide + scale for fluid transition — all of
                // which run on the GPU via graphicsLayer (alpha/scale) and placement (slide),
                // NOT by remeasuring layout.
                val spring = spring<Float>(
                    dampingRatio = AquamorphicDampingRatio,
                    stiffness = AquamorphicStiffness
                )
                val intSpring = spring<IntOffset>(
                    dampingRatio = AquamorphicDampingRatio,
                    stiffness = AquamorphicStiffness
                )

                // PERF: `using SizeTransform { snap() }` makes the container jump to the target
                // size instantly instead of animating its width every frame. Animating the size
                // forced a full layout pass (and re-measured the expensive frosted-glass
                // backdrops) ~60×/s during the A/B morph — the heavy frame drops. With clip=false
                // the cross-fading/scaling children mask the instant size change, so the morph
                // still reads as fluid but is now purely GPU-composited.
                (fadeIn(spring) +
                    slideInHorizontally(intSpring) { if (targetState) it / 4 else -it / 4 } +
                    scaleIn(spring, initialScale = 0.92f)) togetherWith
                    (fadeOut(spring) +
                        slideOutHorizontally(intSpring) { if (targetState) -it / 4 else it / 4 } +
                        scaleOut(spring, targetScale = 0.92f))
            },
            label = "bottomBarState",
            modifier = Modifier.weight(1f),
        ) { isCollapsed ->
            if (!isCollapsed) {
                // ---- STATE A: wide tab pill + trailing search circle ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FrostedPill(modifier = Modifier.weight(1f, fill = false).widthIn(max = 420.dp)) {
                        TabRow(
                            tabs = tabs,
                            pureBlack = pureBlack,
                            isSelected = isSelected,
                            onItemClick = onItemClickHaptic,
                        )
                    }
                    if (searchScreen != null) {
                        val searchActive = isSelected(searchScreen)
                        FrostedCircle(
                            onClick = { onItemClickHaptic(searchScreen, searchActive) },
                        ) {
                            NavGlyph(
                                iconRes = if (searchActive) searchScreen.iconIdActive else searchScreen.iconIdInactive,
                                contentDescription = stringResource(searchScreen.titleId),
                                tint = if (searchActive) MaterialTheme.colorScheme.primary
                                else itemContentColor(pureBlack),
                            )
                        }
                    }
                }
            } else {
                // ---- STATE B: home circle | center pill | search circle ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (homeTab != null) {
                        FrostedCircle(
                            onClick = { onItemClickHaptic(homeTab, isSelected(homeTab)) },
                        ) {
                            NavGlyph(
                                iconRes = if (isSelected(homeTab)) homeTab.iconIdActive else homeTab.iconIdInactive,
                                contentDescription = stringResource(homeTab.titleId),
                                tint = if (isSelected(homeTab)) MaterialTheme.colorScheme.primary
                                else itemContentColor(pureBlack),
                            )
                        }
                    }

                    FrostedPill(modifier = Modifier.weight(1f)) {
                        if (hasNowPlaying) {
                            MiniPlayerPill(pureBlack = pureBlack, onExpand = onMiniPlayerClickHaptic)
                        } else if (activeTab != null) {
                            // Nothing playing: center shows the active-tab compact pill.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                TabButton(
                                    screen = activeTab,
                                    selected = true,
                                    pureBlack = pureBlack,
                                    onClick = { onItemClickHaptic(activeTab, true) },
                                )
                            }
                        }
                    }

                    if (searchScreen != null) {
                        val searchActive = isSelected(searchScreen)
                        FrostedCircle(
                            onClick = { onItemClickHaptic(searchScreen, searchActive) },
                        ) {
                            NavGlyph(
                                iconRes = if (searchActive) searchScreen.iconIdActive else searchScreen.iconIdInactive,
                                contentDescription = stringResource(searchScreen.titleId),
                                tint = if (searchActive) MaterialTheme.colorScheme.primary
                                else itemContentColor(pureBlack),
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Frosted glass containers (share the app-wide 56dp / transparent / 0.20 tint look) */
/* ----------------------------------------------------------------------- */

@Composable
private fun frostedGlassModifier(shape: androidx.compose.ui.graphics.Shape): Modifier {
    // GENUINE Kyant AndroidLiquidGlass backdrop: the nav bar lives in the Scaffold's
    // bottomBar slot (a sibling of the NavHost content layer, drawn OVER it), so consuming
    // LocalAppBackdrop here is safe — it is NOT a re-entrant draw of the layer it lives in.
    // This gives true lens refraction of the content scrolling beneath the bar, not a flat
    // Haze blur. A translucent film is drawn on top so the glass stays legible even on the
    // first frame before the backdrop layer is captured.
    val backdrop = LocalAppBackdrop.current
    val isDark = isSystemInDarkTheme()
    // Apple Music's bar reads as a milky, heavily-frosted capsule — push the film opacity up a
    // touch and the blur/lens radii wider than the old subtle setting to match that reference.
    val film = if (isDark) Color.Black.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.30f)
    // PERF: memoised on everything it actually depends on. The A/B morph recomposes this bar's
    // subtree, and rebuilding the chain each time hands Compose a structurally *new* modifier —
    // which tears down and re-creates the backdrop node, re-capturing the (expensive) blur/lens
    // layer mid-animation. None of these inputs change during a morph, so the node is now
    // created once and simply re-drawn, and the glass keeps refracting live content for free.
    return remember(backdrop, shape, film) {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(14f.dp.toPx(), 26f.dp.toPx())
                },
                onDrawSurface = { drawRect(film) },
            )
            // Clip the bar's own children (tabs/icons/mini-player) to the glass shape.
            .clip(shape)
    }
}

@Composable
private fun FrostedPill(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .height(height)
            .then(frostedGlassModifier(shape)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun FrostedCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "circlePress",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale)
            .then(frostedGlassModifier(CircleShape))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NavGlyph(iconRes: Int, contentDescription: String?, tint: Color) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(24.dp),
    )
}

/* ----------------------------------------------------------------------- */
/* State A tab row with sliding accent indicator                            */
/* ----------------------------------------------------------------------- */

@Composable
private fun TabRow(
    tabs: List<Screens>,
    pureBlack: Boolean,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    // Apple-Music indicates the active tab purely by a filled icon + accent tint (handled in
    // [TabButton]) — NO background pill. Keep the row a simple evenly-spaced set of tabs so the
    // labels sit centered under their icons with breathing room and never collide.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEach { screen ->
            TabButton(
                screen = screen,
                selected = isSelected(screen),
                pureBlack = pureBlack,
                onClick = { onItemClick(screen, isSelected(screen)) },
            )
        }
    }
}

@Composable
private fun TabButton(
    screen: Screens,
    selected: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(targetState = selected, label = "tab_${screen.route}")
    val contentColor by transition.animateColor(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "tabColor",
    ) { sel ->
        if (sel) MaterialTheme.colorScheme.primary else itemContentColor(pureBlack)
    }
    val iconScale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.8f, stiffness = 300f) },
        label = "tabIconScale",
    ) { sel -> if (sel) 1.12f else 1f }

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "tabPress",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(percent = 50))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            // Tight vertical padding + generous horizontal so the four tabs breathe across the
            // pill without a background chip; labels stay centered under their icons.
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
            modifier = Modifier.size(24.dp).scale(iconScale),
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = stringResource(screen.titleId),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ----------------------------------------------------------------------- */
/* State B center mini-player pill                                          */
/* ----------------------------------------------------------------------- */

@Composable
private fun MiniPlayerPill(
    pureBlack: Boolean,
    onExpand: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art (clean circle — no wavy/floral shapes).
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val thumb = mediaMetadata?.thumbnailUrl
            if (thumb != null) {
                AsyncImage(
                    // Same pinned request the standalone mini-player pill uses, so the two share
                    // one memory-cache entry and the art survives the A/B morph without a reload.
                    model = rememberPinnedArtworkRequest(thumb),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.exhale),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mediaMetadata?.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            val artistText = mediaMetadata?.artists?.joinToString { it.name }.orEmpty()
            if (artistText.isNotEmpty()) {
                Text(
                    text = artistText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }
        }

        // Play / pause — instant press feedback, clean circle.
        var pressed by remember { mutableStateOf(false) }
        val pressScale by animateFloatAsState(
            targetValue = if (pressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "playPress",
        )
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(pressScale)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        pressed = true
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        pressed = false
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerConnection.player.togglePlayPause()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Search screen unified bottom bar                                         */
/* ----------------------------------------------------------------------- */

/**
 * The Search tab's dedicated bottom chrome — replaces the morphing A/B nav bar entirely
 * while on the Search route (the dynamic scroll-collapse logic is disabled there by the
 * host).
 *
 * Layout: TWO separate frosted pieces side by side — a standalone circular "Home" button
 * on the left in its own round frosted pill, and the search input field in its own
 * capsule beside it. The mini-player floats directly ABOVE this row (handled by the
 * host's sheet stack) and never merges into it.
 */
@Composable
fun SearchBottomBar(
    pureBlack: Boolean,
    placeholder: String,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Standalone circular Home pill — its OWN frosted glass surface. Sized to match
        // the slim Apple-Music search capsule beside it, NOT the fat 64dp nav circles.
        FrostedCircle(onClick = onHomeClick, size = 48.dp) {
            Icon(
                painter = painterResource(R.drawable.home_outlined),
                contentDescription = stringResource(R.string.home),
                tint = itemContentColor(pureBlack),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // Search input capsule — a SEPARATE frosted pill. Tapping anywhere on it expands
        // the real type-in field (host-owned overlay). Apple Music's field is a slim
        // ~48dp capsule, noticeably thinner than the 64dp nav-bar pills.
        FrostedPill(modifier = Modifier.weight(1f), height = 48.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onSearchClick),
            ) {
                Spacer(Modifier.width(16.dp))
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = itemContentColor(pureBlack),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = itemContentColor(pureBlack),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    tint = itemContentColor(pureBlack),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(16.dp))
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Colors (mirror FloatingNavigationToolbar's palette)                      */
/* ----------------------------------------------------------------------- */

@Composable
private fun itemContentColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant

// A low-alpha wash of the PRIMARY brand colour, matching the floating toolbar — see the tint note
// there. A solid `secondaryContainer` chip both drifted off-accent under dynamic colour and read as
// opaque against the frosted bar it sits on.
@Composable
private fun selectedItemContainerColor(pureBlack: Boolean): Color =
    if (pureBlack) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    }
