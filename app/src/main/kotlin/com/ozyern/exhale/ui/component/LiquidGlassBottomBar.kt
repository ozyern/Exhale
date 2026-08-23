/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.ozyern.exhale.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import coil3.compose.AsyncImage
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.AquamorphicDampingRatio
import com.ozyern.exhale.constants.AquamorphicStiffness
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.ui.component.liquid.LocalAppBackdrop
import com.ozyern.exhale.ui.screens.Screens
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.ozyern.exhale.ui.component.liquid.DampedDragAnimation
import kotlin.math.sign
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.TransformOrigin

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

    // The morph's physics, hoisted so the two states cannot drift apart: whatever leaves has to
    // leave the way the thing replacing it arrives, or the bar looks like it changes its mind
    // halfway through. `morphFast` is only for opacity — a piece that is still fading while it has
    // already finished travelling looks like it is stuck to the glass.
    val morphSpring = remember {
        spring<Float>(dampingRatio = AquamorphicDampingRatio, stiffness = AquamorphicStiffness)
    }
    val morphFast = remember { spring<Float>(dampingRatio = 1f, stiffness = 900f) }
    val morphOffset = remember {
        spring<IntOffset>(dampingRatio = AquamorphicDampingRatio, stiffness = AquamorphicStiffness)
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
                // Nothing on the container. Every piece of the dock animates itself.
                //
                // The old spec faded, slid and scaled the *whole row* in both directions, which
                // costs two things. The obvious one is that it reads as a slide show: two complete
                // docks crossing over, each of them a ghost for the length of the transition. The
                // subtle one is that the search circle is at the same place and the same size in
                // both states — and cross-fading it against itself made the one element that
                // should have been nailed down flicker through half opacity every single time the
                // bar collapsed.
                //
                // With `None` here, a child that does not animate is simply opaque for the whole
                // transition, so that circle is now a shared element for free. The pieces that
                // really do change carry their own motion instead, and they can each move the way
                // their own geometry implies: the tab strip folds down into its left edge, which
                // is where the home circle is about to be, and the home circle and the pill arrive
                // from the outside of the bar.
                //
                // `SizeTransform` still snaps — animating the container's width would remeasure
                // the frosted backdrops every frame, which is what the morph used to cost — and
                // `clip = false` keeps the springs' overshoot from being sheared off at the bounds.
                (EnterTransition.None togetherWith ExitTransition.None)
                    .using(SizeTransform(clip = false) { _, _ -> snap() })
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
                    LiquidTabBar(
                        tabs = tabs,
                        pureBlack = pureBlack,
                        isSelected = isSelected,
                        onItemClick = onItemClickHaptic,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 420.dp)
                            // Grows out of, and collapses back into, its left edge — which is
                            // exactly where the home circle sits in the collapsed state, so the
                            // strip reads as folding down into that circle rather than as a
                            // second widget fading over the top of it.
                            .animateEnterExit(
                                enter = fadeIn(morphSpring) +
                                    scaleIn(morphSpring, 0.90f, TransformOrigin(0f, 0.5f)),
                                exit = fadeOut(morphFast) +
                                    scaleOut(morphSpring, 0.90f, TransformOrigin(0f, 0.5f)),
                            ),
                    )
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
                            // In from the outside edge and up from nothing, because that is where
                            // it comes from: it is the piece the tab strip just folded into.
                            modifier = Modifier.animateEnterExit(
                                enter = fadeIn(morphFast) +
                                    slideInHorizontally(morphOffset) { -it } +
                                    scaleIn(morphSpring, 0.55f),
                                exit = fadeOut(morphFast) +
                                    slideOutHorizontally(morphOffset) { -it } +
                                    scaleOut(morphSpring, 0.55f),
                            ),
                        ) {
                            NavGlyph(
                                iconRes = if (isSelected(homeTab)) homeTab.iconIdActive else homeTab.iconIdInactive,
                                contentDescription = stringResource(homeTab.titleId),
                                tint = if (isSelected(homeTab)) MaterialTheme.colorScheme.primary
                                else itemContentColor(pureBlack),
                            )
                        }
                    }

                    FrostedPill(
                        modifier = Modifier
                            .weight(1f)
                            // Swells rather than slides. It is the widest thing in the collapsed
                            // bar and the only one that has no edge to come from, so it takes the
                            // space it is given from the middle out.
                            .animateEnterExit(
                                enter = fadeIn(morphSpring) + scaleIn(morphSpring, 0.86f),
                                exit = fadeOut(morphFast) + scaleOut(morphSpring, 0.86f),
                            ),
                    ) {
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

/**
 * How much opacity the dock adds over the base chrome tint, and how far it blurs.
 *
 * These are the dock's material, and they are deliberately *one* pair of numbers rather than one
 * per state. The bar's two states are supposed to be the same pane of glass caught mid-morph, so
 * the moment State A and State B disagree about their tint the morph stops being a shape change
 * and becomes a cross-fade between two different materials.
 *
 * They went up because the dock is the one surface in the app that is never over a background of
 * its own choosing: at the top of Home it sits over album art, over a grid of thumbnails, over
 * whatever the row underneath happens to be, and at the old strength a 10sp tab label over a busy
 * cover was genuinely hard to read. The extra blur is doing most of that work — it is what
 * destroys the *detail* behind the pane, and detail is what competes with small type — with the
 * tint only there to stop the result going transparent again over a light photo.
 *
 * It stays a pane and not a slab because the dock is 64dp tall. The same numbers on the 48dp
 * search row read as solid, which is why that row keeps its own, lighter pair.
 */
private const val DockGlassExtraTint = 0.07f
private val DockGlassBlurRadius = 68.dp

@Composable
private fun frostedGlassModifier(
    shape: androidx.compose.ui.graphics.Shape,
    /**
     * Extra opacity for chrome that has to stay readable over *anything*.
     *
     * Every piece of dock chrome takes the defaults — see [DockGlassExtraTint]. The parameters
     * exist for the search row, which is half the dock's height and therefore cannot carry the
     * dock's opacity without reading as a solid slab. A difference of degree, not a second
     * material.
     */
    extraTint: Float = DockGlassExtraTint,
    blurRadius: Dp = DockGlassBlurRadius,
): Modifier {
    // Real backdrop blur of the app content scrolling underneath. The bar lives in the
    // Scaffold's bottomBar slot — a sibling of the NavHost, drawn over it — so reading the
    // NavHost's haze source here is safe and not a re-entrant layer draw.
    //
    // This used to reach for Kyant's `drawBackdrop(LocalAppBackdrop.current, …)`. That local
    // is `rememberDefaultBackdrop()`, an EMPTY passthrough canvas: blurring and refracting it
    // yields no pixels at all, so the only thing the bar ever painted was its own 0.34-alpha
    // film. That is the "dock is fully transparent" bug — the glass was never glass.
    val isDark = isSystemInDarkTheme()
    return rememberChromeGlassModifier(
        shape = shape,
        dark = isDark,
        // Apple Music's dock is milky enough to read white-on-glass labels at any scroll
        // position. Under a real blur this is a tint, not a substitute for one.
        tintAlpha = (if (isDark) 0.30f else 0.26f) + extraTint,
        blurRadius = blurRadius,
        // The dock sits over a scrolling list, so its blur is recomputed every frame the
        // user scrolls. Half-resolution is invisible at this radius and halves that cost.
        quality = 0.5f,
    )
}

@Composable
private fun FrostedPill(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    extraTint: Float = DockGlassExtraTint,
    blurRadius: Dp = DockGlassBlurRadius,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .height(height)
            .then(frostedGlassModifier(shape, extraTint, blurRadius)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun FrostedCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    extraTint: Float = DockGlassExtraTint,
    blurRadius: Dp = DockGlassBlurRadius,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        // Underdamped and stiff. At 0.8/300 the circle sank under the finger and eased back like
        // a button on a lift; the capsule beside it is a droplet that overshoots and wobbles, and
        // two press physics on one bar is one too many.
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "circlePress",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale)
            .then(frostedGlassModifier(CircleShape, extraTint, blurRadius))
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

/**
 * The dock's tab strip: a permanent glass lens riding on the selected tab, dragged like a
 * physical object.
 *
 * ### Why the selected tab is not simply painted in the accent colour
 *
 * It is — on a *hidden* copy of the row. The strip is composed in four pieces:
 *
 *  1. a bare glass plate, no content on it, recorded into `glassBackdrop`;
 *  2. the visible row of tabs in the resting colour, drawn over that plate;
 *  3. an invisible twin (`alpha = 0`) of the same row in the **accent** colour with filled icons,
 *     recorded into `tabsBackdrop` — no glass of its own, just glyphs on nothing;
 *  4. the capsule, which draws `plate + twin` through a lens.
 *
 * So the blue icon and label of the selected tab are not a tint applied to a widget: they are the
 * hidden layer *seen through the glass*. Everything the lens does to the pixels underneath — the
 * magnification, the edge refraction, the chromatic fringe as it accelerates — happens to the
 * icon and the label too, because as far as the shader is concerned they are just more backdrop.
 * A capsule drawn over an already-blue tab can only ever look like a sticker on top of it; this
 * looks like the tab is *inside* the glass, which is the entire effect being copied.
 *
 * ### Why it is split that way, and not the obvious way
 *
 * The obvious build is two full copies of the dock — glass and all — with the capsule sampling
 * the app content plus the accent copy. That is what this was, and it cost three backdrop passes
 * per frame: the visible dock blurring and refracting the whole NavHost recording, the twin doing
 * the identical work again a pixel underneath, and the capsule compositing the app layer a third
 * time. At 120Hz over a 64dp strip that is what the dragging felt like.
 *
 * Splitting the plate away from the content means the expensive pass — vibrancy, a 13dp blur and
 * a lens over live app pixels — happens exactly **once**, and the other two layers are recordings
 * of already-drawn content, which cost a `RenderNode` draw each. The plate has to be contentless
 * for this to work: if the recording contained the resting icons, the capsule would show them
 * *and* the accent ones stacked as a double image.
 *
 * ### The drag drives a float, not a spring
 *
 * While a finger is down the capsule's position is a plain `mutableFloatStateOf` written straight
 * from the pointer callback. It used to call `updateValue` per frame, and `updateValue` cancels a
 * job and launches a coroutine that takes `Animatable`'s mutex — 240 of those a second at 120Hz,
 * to compute a spring toward a target that the finger had already moved past. The spring only
 * earns its keep once the finger is gone, so that is the only time it runs: [settleFrom] snaps to
 * where the gesture ended and springs to the tab it landed on, in one coroutine, once per
 * gesture. Same for velocity — the stretch reads an exponentially smoothed float rather than a
 * `VelocityTracker`, which fits a polynomial over its sample history every time it is asked.
 *
 * Press swells it (78dp of travel space for a 56dp pill); drag and it follows the finger
 * continuously rather than hopping tab to tab, stretching along its direction of travel and
 * squashing vertically in proportion to its own velocity, exactly as a droplet under acceleration
 * would. Release and it rounds to the nearest tab. Pushing past either end rubber-bands the whole
 * panel a few pixels and lets it spring back. The commit only happens on release, so a drag that
 * changes its mind costs nothing.
 *
 * Falls back to a flat accent wash on devices with no `RuntimeShader`, where there is no lens to
 * see the hidden layer through and the selected tab therefore has to colour itself.
 */
@Composable
private fun LiquidTabBar(
    tabs: List<Screens>,
    pureBlack: Boolean,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
) {
    if (tabs.isEmpty()) return

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shape = remember { RoundedCornerShape(percent = 50) }
    val accent = MaterialTheme.colorScheme.primary
    val restColor = itemContentColor(pureBlack)
    val glassy = remember { isRuntimeShaderSupported() }
    val isDark = isSystemInDarkTheme()

    val lastIndex = (tabs.size - 1).coerceAtLeast(0)
    val selectedIndex = tabs.indexOfFirst { isSelected(it) }.coerceIn(0, lastIndex)

    // Inset of the capsule inside the pill, and therefore what the tab pitch is measured from.
    // 64dp of bar minus 4dp top and bottom is the capsule's 56dp.
    val inset = 4.dp
    val insetPx = with(density) { inset.toPx() }
    val capsuleHeight = height - inset * 2

    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var tabWidthPx by remember { mutableFloatStateOf(0f) }

    // ---- Live gesture state. All plain float state: written from the pointer callback, read
    // ---- only from draw lambdas, so a drag frame costs a layer invalidation and nothing else.
    val dragging = remember { mutableStateOf(false) }
    val dragValue = remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    val dragVelocity = remember { mutableFloatStateOf(0f) }
    val overscrollPx = remember { mutableFloatStateOf(0f) }

    val rubberBandPx = with(density) { 6.dp.toPx() }
    val panelOffset: () -> Float = {
        val raw = overscrollPx.floatValue
        if (totalWidthPx == 0f || raw == 0f) {
            0f
        } else {
            val fraction = (raw / totalWidthPx).fastCoerceIn(-1f, 1f)
            rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }

    // The drag callbacks outlive the composition that built them, so everything they need from
    // the current frame is read through a State rather than captured by value.
    val onItemClickState by rememberUpdatedState(onItemClick)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val tabsState by rememberUpdatedState(tabs)

    // `moved` separates "the user dragged the capsule somewhere" from "the user tapped the
    // capsule", which are the same gesture as far as the drag inspector is concerned but mean
    // different things to the host.
    val moved = remember { mutableFloatStateOf(0f) }
    val tapSlopPx = with(density) { 12.dp.toPx() }

    val dampedDragAnimation = remember(scope, tabs.size, isLtr) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..lastIndex.toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            // 78dp of swell on a 56dp capsule. Big enough that the pill visibly bulges past the
            // top and bottom edges of the dock while held, which is what sells it as a blob of
            // liquid sitting on the bar rather than a rectangle cut into it.
            pressedScale = 78f / 56f,
            onDragStarted = {
                moved.floatValue = 0f
                dragVelocity.floatValue = 0f
                dragValue.floatValue = value
                dragging.value = true
            },
            onDragStopped = {
                val ended = dragValue.floatValue
                val landed = ended.fastRoundToInt().fastCoerceIn(0, lastIndex)

                // One coroutine for the whole settle: snap to where the finger left it, hand
                // drawing back to the animation, then spring onto the tab.
                settleFrom(ended, landed.toFloat()) { dragging.value = false }

                // Two more, once per gesture, to unwind the two decorative floats. Both are
                // cheap `Animatable`s created here and thrown away; the point is that neither of
                // them existed during the drag itself.
                val velocityFrom = dragVelocity.floatValue
                if (velocityFrom != 0f) {
                    scope.launch {
                        Animatable(velocityFrom).animateTo(0f, spring(0.5f, 300f)) {
                            dragVelocity.floatValue = value
                        }
                    }
                }
                val overscrollFrom = overscrollPx.floatValue
                if (overscrollFrom != 0f) {
                    scope.launch {
                        Animatable(overscrollFrom).animateTo(0f, spring(1f, 300f, 0.5f)) {
                            overscrollPx.floatValue = value
                        }
                    }
                }

                val screen = tabsState.getOrNull(landed)
                if (screen != null) {
                    if (landed != selectedIndexState) {
                        onItemClickState(screen, false)
                    } else if (moved.floatValue < tapSlopPx) {
                        // Never travelled: this was a tap that happened to land on the capsule,
                        // which by definition sits on the active tab. Forwarded as a re-tap so
                        // the host can do what it does for those (scroll the page back to the
                        // top), because the capsule covers that tab's own click target.
                        onItemClickState(screen, true)
                    }
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f) {
                    moved.floatValue += abs(dragAmount.x)
                    val delta = dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f
                    val next = dragValue.floatValue + delta
                    val clamped = next.fastCoerceIn(0f, lastIndex.toFloat())

                    // Haptic on crossing, not on landing: the tick has to arrive while the finger
                    // is still travelling or it reads as lag on the commit rather than as
                    // feedback on the movement.
                    if (clamped.fastRoundToInt() != dragValue.floatValue.fastRoundToInt()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    dragValue.floatValue = clamped

                    // Exponential smoothing rather than a VelocityTracker: only the capsule's
                    // squish reads this, and it is clamped to ±0.2 before it reaches a scale, so
                    // a least-squares fit over a sample history would be paying for precision
                    // that is thrown away two lines later.
                    dragVelocity.floatValue = dragVelocity.floatValue * 0.72f + delta * 2.4f

                    // Only what the capsule could not absorb becomes overscroll; anything within
                    // range pulls the panel back towards centre instead of accumulating.
                    overscrollPx.floatValue =
                        if (next < 0f || next > lastIndex) overscrollPx.floatValue + dragAmount.x
                        else overscrollPx.floatValue * 0.5f
                }
            },
        )
    }

    /** Where the capsule is, in tab units. The finger wins while it is down. */
    val capsuleAt: () -> Float = {
        if (dragging.value) dragValue.floatValue else dampedDragAnimation.value
    }

    // Route changes that did not come from this bar — a deep link, the back stack, the search
    // circle — still have to move the capsule, and must do it without the pressed swell.
    LaunchedEffect(dampedDragAnimation, selectedIndex) {
        if (!dragging.value) dampedDragAnimation.settleToValue(selectedIndex.toFloat())
    }

    val glassBackdrop = rememberLayerBackdrop()
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(glassBackdrop, tabsBackdrop)
    val containerGlass = frostedGlassModifier(shape)

    Box(
        modifier = modifier.height(height),
        contentAlignment = Alignment.CenterStart,
    ) {
        // ---- 1 + 2. the plate, and the row you can see on it ------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val width = coords.size.width.toFloat()
                    if (totalWidthPx != width) {
                        totalWidthPx = width
                        tabWidthPx = ((width - insetPx * 2f) / tabs.size).coerceAtLeast(0f)
                    }
                }
                .graphicsLayer {
                    translationX = panelOffset()
                    // The whole bar breathes a little under the finger, so the capsule is not the
                    // only thing acknowledging the touch.
                    val swell = lerp(1f, 1.012f, dampedDragAnimation.pressProgress)
                    scaleX = swell
                    scaleY = swell
                },
        ) {
            // Contentless on purpose — see the KDoc. `layerBackdrop` records everything drawn
            // *after* it in the chain, so it has to sit before the glass modifier to capture the
            // pane at all.
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (glassy) Modifier.layerBackdrop(glassBackdrop) else Modifier)
                    .then(containerGlass)
            )

            Row(
                modifier = Modifier.fillMaxSize().padding(inset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, screen ->
                    LiquidTabItem(
                        screen = screen,
                        // Without a lens there is no hidden layer to reveal, so the selection has
                        // to be painted here instead.
                        tint = if (!glassy && index == selectedIndex) accent else restColor,
                        filled = !glassy && index == selectedIndex,
                        scaleProvider = { 1f },
                        onClick = { onItemClick(screen, index == selectedIndex) },
                    )
                }
            }
        }

        // ---- 3. the twin the lens reads --------------------------------------------
        if (glassy) {
            Row(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .fillMaxWidth()
                    .height(capsuleHeight)
                    .graphicsLayer { translationX = panelOffset() }
                    .padding(horizontal = inset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { screen ->
                    LiquidTabItem(
                        screen = screen,
                        tint = accent,
                        filled = true,
                        // Magnified with the press, so squeezing the capsule appears to draw the
                        // icon towards the surface of the glass.
                        scaleProvider = { lerp(1f, 1.16f, dampedDragAnimation.pressProgress) },
                        onClick = null,
                    )
                }
            }
        }

        // ---- 4. the capsule ---------------------------------------------------------
        if (tabWidthPx > 0f) {
            val tabWidth = with(density) { tabWidthPx.toDp() }
            val capsuleModifier = Modifier
                .padding(horizontal = inset)
                .graphicsLayer {
                    val travel = capsuleAt() * tabWidthPx
                    translationX = (if (isLtr) travel else -travel) + panelOffset()
                }
                .then(dampedDragAnimation.modifier)

            if (glassy) {
                Box(
                    capsuleModifier
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { shape },
                            effects = {
                                // Never fully off. A capsule with no refraction at rest is a
                                // coloured rectangle, and the resting state is the one the user
                                // spends all their time looking at — in the reference the pill is
                                // visibly bending the label underneath it before anyone touches
                                // it. The press deepens the bend rather than switching it on.
                                val progress = dampedDragAnimation.pressProgress
                                val depth = 0.4f + 0.6f * progress
                                lens(
                                    9f.dp.toPx() * depth,
                                    14f.dp.toPx() * depth,
                                    true,
                                    // The extra sample cost of the colour fringe only buys
                                    // anything while the thing is moving.
                                    progress > 0.02f,
                                )
                            },
                            highlight = {
                                Highlight.Ambient.copy(
                                    alpha = 0.4f + 0.6f * dampedDragAnimation.pressProgress,
                                )
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 3f.dp + 5f.dp * progress,
                                    color = Color.Black.copy(alpha = 0.15f),
                                    alpha = 0.35f + 0.65f * progress,
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                // Conservation of volume, roughly: the faster it travels the
                                // longer and flatter it gets, and it recovers as it settles.
                                val velocity = dragVelocity.floatValue
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                // A wash at rest so the capsule is still legible as a selected
                                // slot on a busy backdrop, fading out as the lens takes over.
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    color = if (isDark) Color.White.copy(alpha = 0.13f)
                                    else Color.Black.copy(alpha = 0.09f),
                                    alpha = 1f - progress * 0.7f,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                        .height(capsuleHeight)
                        .width(tabWidth),
                )
            } else {
                Box(
                    capsuleModifier
                        .graphicsLayer {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                        }
                        .clip(shape)
                        .background(selectedItemContainerColor(pureBlack), shape)
                        .height(capsuleHeight)
                        .width(tabWidth),
                )
            }
        }
    }
}

/**
 * One tab. Equal width by construction — [RowScope.weight] rather than each tab measuring to its
 * own label — because the capsule is a single fixed-pitch slot sliding across the row, and a row
 * whose slots are "Home"-wide and "Mood & Genres"-wide cannot be indexed by multiplication.
 *
 * [onClick] is null for the hidden twin: it is a rendering of the row, not a copy of its
 * behaviour, and a second set of invisible click targets stacked over the real ones would
 * swallow every tap.
 */
@Composable
private fun RowScope.LiquidTabItem(
    screen: Screens,
    tint: Color,
    filled: Boolean,
    scaleProvider: () -> Float,
    onClick: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scale = scaleProvider()
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(if (filled) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(screen.titleId),
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
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
        // The song, as one thing that changes as one thing.
        //
        // Art, title and artist used to swap their contents in place: the cover blinked to the new
        // URL the instant it arrived, the two lines of text re-laid out underneath it on whatever
        // frame their own state landed on, and a track change came out as three small unrelated
        // glitches. Now the whole block travels — the outgoing song lifts out through the top of
        // the pill as the incoming one rises into it — which also makes it the one piece of motion
        // in the dock that means *something changed by itself* rather than *you touched something*.
        //
        // Keyed on the metadata rather than on the id, so a re-emission of the same song (a like,
        // a download finishing) compares equal and does not re-run the transition.
        AnimatedContent(
            targetState = mediaMetadata,
            transitionSpec = {
                (fadeIn(tween(190)) + slideInVertically { it / 2 }) togetherWith
                    (fadeOut(tween(130)) + slideOutVertically { -it / 2 }) using
                    SizeTransform(clip = false) { _, _ -> snap() }
            },
            label = "miniPillTrack",
            modifier = Modifier.weight(1f),
        ) { metadata ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    val thumb = metadata?.thumbnailUrl
                    if (thumb != null) {
                        AsyncImage(
                            // Same pinned request the standalone mini-player pill uses, so the two
                            // share one memory-cache entry and the art survives the A/B morph
                            // without a reload.
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
                        text = metadata?.title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                    )
                    val artistText = metadata?.artists?.joinToString { it.name }.orEmpty()
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
            }
        }

        // Play / pause — instant press feedback, clean circle.
        var pressed by remember { mutableStateOf(false) }
        val pressScale by animateFloatAsState(
            targetValue = if (pressed) 0.86f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
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
            // The glyph swap is the confirmation.
            //
            // The press scale fires on ACTION_DOWN, before anything has happened — it acknowledges
            // the touch, not the result. The bar and the triangle used to replace each other on a
            // single frame some time later, so the only feedback that the command actually *took*
            // was instantaneous and therefore easy to miss on a slow connection. Popping the new
            // glyph in from small gives that moment a shape, and it lasts about as long as a
            // transport control should be allowed to.
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    val glyph = spring<Float>(dampingRatio = 0.6f, stiffness = 1400f)
                    (scaleIn(glyph, 0.55f) + fadeIn(tween(90))) togetherWith
                        (scaleOut(glyph, 0.55f) + fadeOut(tween(90))) using
                        SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "miniPillPlayPause",
            ) { playing ->
                Icon(
                    painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Search screen unified bottom bar                                         */
/* ----------------------------------------------------------------------- */

/**
 * Slim on purpose, and not the dock's height.
 *
 * It was briefly 56dp, on the theory that a row which is *nearly* the dock reads as a mistake.
 * It does not — it reads as a search field that has been inflated. The field is one line of text
 * and a glyph; at 56dp the type sits in the middle of a lot of nothing and the capsule looks
 * padded rather than considered. The gap that change was really aimed at was never the row's
 * height anyway: it was the row hanging at the bottom of a taller reservation, which is fixed at
 * the call site by filling that band and centring in it.
 */
private val SearchRowHeight = 48.dp

/**
 * See [frostedGlassModifier]'s `extraTint` for why the search row is not dock-strength glass.
 *
 * Both numbers were higher — 0.14 and 72dp — and the result was a capsule that read as *thicker*
 * even though its height had not moved. A milky, heavily blurred pane at 48dp has no interior to
 * speak of, so the eye stops seeing a thin sheet of glass with content behind it and starts seeing
 * a solid slab, and a solid slab at that size looks bloated. Legibility over a busy result grid
 * was the goal and it costs far less than that: a few points of tint over the base chrome, and a
 * blur a little under the dock's own — which the row can afford to sit below precisely because it
 * is short enough that its interior would otherwise disappear.
 */
private const val SearchGlassExtraTint = 0.05f
private val SearchGlassBlurRadius = 60.dp

/**
 * The bottom chrome for **both** search surfaces — the Search tab and the results page for a
 * committed query. It replaces the morphing A/B nav bar entirely on those routes (the host
 * disables the scroll-collapse logic there).
 *
 * The search field never leaves the bottom of the screen. The results page used to put the field
 * back in the Scaffold's `topBar`, so committing a query threw the thing you were typing into to
 * the opposite end of the display and left your thumb pointing at nothing. Now only the leading
 * button and the text change: a Home circle and the placeholder while browsing, a back circle and
 * the live query once results are on screen.
 *
 * Layout: TWO separate frosted pieces side by side — a standalone circular button on the left in
 * its own round frosted pill, and the search input field in its own capsule beside it. The
 * mini-player floats directly ABOVE this row (handled by the host's sheet stack) and never merges
 * into it.
 */
@Composable
fun SearchBottomBar(
    pureBlack: Boolean,
    placeholder: String,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Non-null once a query has been committed: the pill shows it in the accent colour
    // instead of the grey placeholder, so the bar doubles as the results page's title.
    committedQuery: String? = null,
    // Swaps the leading circle from "go home" to "go back", which is what the button
    // actually does once you are a level deep in results.
    leadingIsBack: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Standalone circular leading pill — its OWN frosted glass surface. Sized to match the
        // slim search capsule beside it, NOT the fat 64dp nav circles.
        FrostedCircle(
            onClick = onHomeClick,
            size = SearchRowHeight,
            extraTint = SearchGlassExtraTint,
            blurRadius = SearchGlassBlurRadius,
        ) {
            Icon(
                painter = painterResource(
                    if (leadingIsBack) R.drawable.arrow_back else R.drawable.home_outlined,
                ),
                contentDescription = stringResource(
                    if (leadingIsBack) R.string.back else R.string.home,
                ),
                tint = itemContentColor(pureBlack),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // Search input capsule — a SEPARATE frosted pill. Tapping anywhere on it expands
        // the real type-in field (host-owned overlay). Apple Music's field is a slim
        // ~48dp capsule, noticeably thinner than the 64dp nav-bar pills.
        FrostedPill(
            modifier = Modifier.weight(1f),
            height = SearchRowHeight,
            extraTint = SearchGlassExtraTint,
            blurRadius = SearchGlassBlurRadius,
        ) {
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
                    tint = if (committedQuery != null) MaterialTheme.colorScheme.primary
                    else itemContentColor(pureBlack),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = committedQuery ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (committedQuery != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (committedQuery != null) MaterialTheme.colorScheme.primary
                    else itemContentColor(pureBlack),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(
                        if (committedQuery != null) R.drawable.close else R.drawable.mic,
                    ),
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
