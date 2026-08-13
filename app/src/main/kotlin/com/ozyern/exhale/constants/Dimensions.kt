/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.constants

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val CONTENT_TYPE_HEADER = 0
const val CONTENT_TYPE_LIST = 1
const val CONTENT_TYPE_SONG = 2
const val CONTENT_TYPE_ARTIST = 3
const val CONTENT_TYPE_ALBUM = 4
const val CONTENT_TYPE_PLAYLIST = 5

val FloatingToolbarHeight = 72.dp
val SlimFloatingToolbarHeight = 64.dp
val FloatingToolbarHorizontalPadding = 16.dp
val FloatingToolbarBottomPadding = 12.dp
val NavigationBarHeight = FloatingToolbarHeight
val SlimNavBarHeight = SlimFloatingToolbarHeight
val MiniPlayerHeight = 64.dp
val MiniPlayerBottomSpacing = 8.dp // Space between MiniPlayer and NavigationBar

/**
 * Geometry of the collapsed mini-player *pill*. These three values are the morph target for the
 * Dynamic-Island transition in `BottomSheet`, so they must stay in lockstep with how
 * `SwipeableMiniPlayerBox` / `NewMiniPlayer` actually lay the pill out — if they drift, the full
 * player shrinks into the wrong rectangle and the hand-off to the pill visibly jumps.
 */
val MiniPlayerPillHorizontalInset = 12.dp
val MiniPlayerPillCornerRadius = 32.dp

/**
 * Geometry of the floating nav bar's own centre pill — the **State B** morph target.
 *
 * When the user minimises the player while scrolled down, the full player must not land on the
 * standalone mini-player pill (which is hidden in that state); it has to shrink straight into the
 * frosted nav container. These values describe that container as laid out by
 * `LiquidGlassBottomBar`: a [NavBarPillHeight]-tall capsule flanked by a home circle and a search
 * circle, so the centre pill starts [NavBarPillSideSlot] in from the row's own horizontal padding.
 *
 * They must stay in lockstep with `LiquidGlassBottomBar` — if they drift, the player shrinks into
 * a rectangle that is not the bar and the hand-off visibly jumps.
 */
val NavBarPillHeight = 64.dp
private val NavBarPillGap = 10.dp
val NavBarPillSideSlot = FloatingToolbarHorizontalPadding + NavBarPillHeight + NavBarPillGap
val NavBarPillCornerRadius = NavBarPillHeight / 2
val QueuePeekHeight = 64.dp
val AppBarHeight = 64.dp

val ListItemHeight = 64.dp
val SuggestionItemHeight = 56.dp
val SearchFilterHeight = 48.dp
val ListThumbnailSize = 48.dp
val SmallGridThumbnailHeight = 104.dp
val GridThumbnailHeight = 128.dp
val AlbumThumbnailSize = 144.dp

val ThumbnailCornerRadius = 10.dp
val GridThumbnailCornerRadius = 8.dp

val PlayerHorizontalPadding = 32.dp

val NavigationBarAnimationSpec = spring<Dp>(
	dampingRatio = Spring.DampingRatioNoBouncy,
	stiffness = Spring.StiffnessLow
)

/**
 * The one spring every bottom sheet settles on, and therefore the curve the Dynamic-Island morph
 * is driven by — the morph reads `state.progress`, so this spec *is* the animation.
 *
 * Deliberately under-damped. A critically damped spring glides to a stop, which makes the player
 * look like it is being lowered; 0.7 damping lets it land on the pill with a small physical
 * overshoot, which is what sells the morph as a single object settling into place. 400 stiffness
 * keeps it quick enough not to feel loose. Identical to the spec the floating nav bar uses for its
 * own A/B morph and sliding tab indicator, so the whole bottom of the screen moves as one system.
 *
 * Expansion cannot visibly overshoot: `Animatable` is bounded by `expandedBound`, so the bounce
 * only ever appears on the way *down*, which is the direction the transition is about.
 */
val BottomSheetAnimationSpec = spring<Dp>(
	dampingRatio = 0.7f,
	stiffness = 400f
)

/**
 * Used for programmatic (tapped, not flung) transitions — `expandSoft` / `collapseSoft`. Now the
 * same curve as [BottomSheetAnimationSpec] on purpose: minimising by tapping the chevron and
 * minimising by flinging the sheet are the same gesture as far as the user is concerned, and they
 * used to settle on visibly different springs.
 */
val BottomSheetSoftAnimationSpec = spring<Dp>(
	dampingRatio = 0.7f,
	stiffness = 400f
)
