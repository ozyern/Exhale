/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.ozyern.exhale.ui.player

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.EnableHapticFeedbackKey
import com.ozyern.exhale.constants.MiniPlayerHeight
import com.ozyern.exhale.constants.MiniPlayerPillHorizontalInset
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.ui.component.pressScaleContainer
import com.ozyern.exhale.ui.component.rememberPinnedArtworkRequest
import com.ozyern.exhale.ui.utils.safeHorizontalChromeInset
import com.ozyern.exhale.models.MediaMetadata
import com.ozyern.exhale.playback.PlayerConnection
import com.ozyern.exhale.together.TogetherSessionState
import com.ozyern.exhale.ui.component.BottomSheetState
import com.ozyern.exhale.utils.rememberHaptic
import com.ozyern.exhale.utils.rememberPreference
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.BoxScope


@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    // The pill draws shorter than its slot; the slot itself keeps [MiniPlayerHeight] because the
    // sheet's collapsed bound is captured once and cannot change per route. See
    // [com.ozyern.exhale.constants.CompactMiniPlayerHeight].
    compact: Boolean = false,
    content: @Composable (Float) -> Unit
) {
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    // Symmetric, and unioned with the display cutout — see `safeHorizontalChromeInset`. This
    // replaced a `windowInsetsPadding(systemBars.only(Horizontal))`, which was wrong twice over:
    // it ignored the cutout entirely (so the pill slid under a landscape "Fluid Cloud"), and it
    // padded each edge independently while the morph that has to land on this pill uses ONE
    // symmetric inset.
    val safeChromeInset = safeHorizontalChromeInset()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier
                        .padding(horizontal = safeChromeInset)
                        .background(
                            if (pureBlack) Color.Black
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                } else {
                    // Must match BottomSheet's Dynamic-Island morph inset, or the full player
                    // shrinks into a rectangle that doesn't line up with this pill. The host
                    // composes the morph target from exactly these two terms.
                    baseModifier.padding(
                        horizontal = safeChromeInset + MiniPlayerPillHorizontalInset,
                    )
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                        kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                                velocity > velocityThreshold
                                        ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                        if (com.ozyern.exhale.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                            try { com.ozyern.exhale.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                        }
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                        if (com.ozyern.exhale.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                            try { com.ozyern.exhale.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                        }
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            },
        // Centres the pill in its slot, which only matters when it is shorter than the slot. The
        // swipe indicators below place themselves with an explicit `align`, so nothing else in
        // here changes.
        contentAlignment = Alignment.Center,
    ) {
        content(offsetXAnimatable.value)

        // Visual indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata,
    // Non-null when the current track has an artist worth opening. The artist line becomes the
    // navigation affordance, replacing the separate person button that used to sit in the row.
    onArtistClick: (() -> Unit)? = null,
    // Drops one type step so two lines still fit the slim pill without the artist clipping.
    compact: Boolean = false,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.bodyMedium
                else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        Spacer(Modifier.height(1.dp))

        AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = if (compact) MaterialTheme.typography.labelSmall
                else MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Ellipsis, not marquee. Two independently scrolling lines 1dp apart is two
                // things moving in the corner of the eye at all times, and the artist is the
                // line you least need to read to the end — the screenshot that prompted this was
                // caught mid-loop, showing the tail of one pass, a gap, and the head of the next,
                // which reads as a layout fault rather than as a long name.
                modifier = Modifier
                    .then(
                        if (onArtistClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onArtistClick,
                            )
                        } else {
                            Modifier
                        },
                    )
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    // A disc.
    //
    // This was a rounded square, on the argument that cropping a square cover to a circle throws
    // away its corners. True, and it stops mattering at 48dp: at that size the corners of an
    // album cover carry almost nothing, and what the shape is really doing is saying which of the
    // objects at the bottom of the screen is the record. The pill now only appears on Home —
    // everywhere else the player is the island at the top — so it sits directly above a dock full
    // of rounded-square glyphs, and being the one round thing in that stack is worth more than
    // four corners of a thumbnail.
    val shape = CircleShape
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            // A real shadow under the art. The pill is a pane of glass and the row on it was
            // uniformly flat — every element sitting at exactly the same depth is what made the
            // whole thing read as a printed strip rather than as objects on a surface. The art
            // is the one thing in the row that is an image, so it is the one that should have
            // physical thickness.
            .shadow(
                elevation = 5.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // A light rim, not an `outline` stroke. The pill is a pane of glass and everything
            // on it is lit from above; a flat grey border drawn at full strength on all four
            // sides is the one element that read as a Material list thumbnail pasted onto it.
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f)),
                ),
                shape = shape,
            )
    ) {
        val thumbnailUrl = mediaMetadata?.thumbnailUrl
        if (thumbnailUrl != null) {
            AsyncImage(
                // Pinned to the memory cache under the URL so the artwork does not blink
                // when the player morphs between this pill and the nav bar capsule.
                model = rememberPinnedArtworkRequest(thumbnailUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.exhale),
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f)
            )
        }

        // Buffering veil over the art rather than a ring around it: the state belongs to the
        // track, so it reads better sitting on the track than orbiting it.
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f)),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

/**
 * The State-A mini player: the wide pill that floats above the dock.
 *
 * Three decisions carry it, all of them about hierarchy:
 *
 *  * **Artwork is a rounded square**, like every other piece of album art in the app. It used to
 *    be a disc wearing a circular progress ring, so the actual thumbnail got 36dp of a 44dp box
 *    and was cropped to a circle - the least legible way to show a square image.
 *  * **The artist line navigates**, which is the affordance people already try, so the separate
 *    person button that existed only to do that is gone and the row got a 40dp slot back.
 *  * **Play/pause is the loudest thing in the pill**, because it is the one control the pill
 *    exists for. See [MiniPlayerPlayPauseButton].
 *
 * Progress is a hairline along the bottom rather than a ring around the artwork: more legible,
 * out of the way of the thing it describes, and drawn rather than laid out, so ticking it forward
 * costs one draw pass and no recomposition of the row above it.
 *
 * @param compact the slim layout for surfaces whose bottom row is the search bar. Everything
 *   scales down one step and the like button drops out - at that width it was the least-used
 *   control competing with the most-used one for the same edge of the pill.
 */
@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    compact: Boolean = false,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isLiked = currentSong?.song?.liked == true

    val isLoading = playbackState == Player.STATE_BUFFERING

    val progressColor = MaterialTheme.colorScheme.primary
    val progressTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    val artworkSize = if (compact) 38.dp else 48.dp
    // 34dp, not 40. In a 56dp pill a 40dp button is over two thirds of the height, filled with a
    // solid accent — it stopped being the loudest thing in the row and became the only thing in
    // it. The wide pill keeps 48dp because it has 64dp to spend and a like button to out-rank.
    val buttonSize = if (compact) 34.dp else 48.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Progress first, so it is *under* the row rather than painted across it.
        //
        // It used to be the last child, inset 22dp from each end and lifted 7dp off the bottom.
        // On a 64dp pill that puts the line at y=54..57 — squarely inside the 48dp artwork's own
        // band, which starts at y=8 — and being drawn last put it on top. The visible result was
        // a short accent-coloured stub apparently glued to the bottom-left corner of the album
        // art, which is what a progress bar looks like when it is a few percent through a song
        // and has been laid over the one opaque object in the row. Nothing about it read as
        // progress.
        //
        // Now it sits in the 8dp margin *below* the artwork and behind everything. The end inset
        // is what the capsule's own curve allows at that height: 3dp up from the bottom of a 32dp
        // radius leaves ±13.5dp of chord about each end centre, so anything less than ~19dp of
        // inset gets clipped to a stub by the pill itself.
        MiniPlayerProgress(
            position = position,
            duration = duration,
            compact = compact,
            trackColor = progressTrackColor,
            progressColor = progressColor,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            MiniPlayerArtwork(
                mediaMetadata = mediaMetadata,
                isLoading = isLoading,
                size = artworkSize,
            )

            Spacer(modifier = Modifier.width(12.dp))

            mediaMetadata?.let {
                MiniPlayerInfo(
                    mediaMetadata = it,
                    compact = compact,
                    onArtistClick = {
                        val artistId = it.artists.firstOrNull()?.id
                        if (!artistId.isNullOrBlank()) {
                            navController.navigate("artist/$artistId")
                            state.collapseSoft()
                        }
                    },
                )
            } ?: Spacer(Modifier.weight(1f))

            if (togetherSessionState !is TogetherSessionState.Idle) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.all_inclusive),
                        contentDescription = stringResource(R.string.music_together),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // Like. A quiet glyph beside a loud filled button, so the hierarchy in the pill is
            // unambiguous instead of two same-sized circles competing. Dropped entirely in the
            // compact layout, where there is not enough width for a second control to be quiet.
            if (!compact) {

                val heartScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "heartScale",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { playerConnection.toggleLike() },
                        ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            },
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            MiniPlayerPlayPauseButton(
                isPlaying = isPlaying,
                isLoading = isLoading,
                playerConnection = playerConnection,
                size = buttonSize,
            )
        }

    }
}

/**
 * The hairline along the bottom of the pill.
 *
 * Drawn, not laid out: ticking it forward every second costs one draw pass and never recomposes
 * or re-measures the row above it. See the call site for why it is inset the way it is.
 */
@Composable
private fun BoxScope.MiniPlayerProgress(
    position: Long,
    duration: Long,
    compact: Boolean,
    trackColor: Color,
    progressColor: Color,
) {
    Canvas(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            // Inset far enough in from both ends that the capsule's own curve never clips the
            // line into a stub, and lifted far enough off the bottom that the rounded cap does
            // not ride the rim. At 2dp up on a 28dp radius the chord is only ~25dp wide of the
            // 39dp the old inset assumed, so both ends were being shaved by the pill itself and
            // the fill looked like a stray mark rather than a progress bar.
            .padding(
                start = if (compact) 22.dp else 26.dp,
                end = if (compact) 22.dp else 26.dp,
                bottom = if (compact) 4.dp else 5.dp,
            )
            .height(3.dp),
    ) {
        val fraction =
            if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        val stroke = size.height
        val y = size.height / 2f
        drawLine(
            color = trackColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (fraction > 0f) {
            // Fades in from the left rather than starting at full strength, so a track that has
            // barely begun does not put a hard accent dot under the artwork.
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(progressColor.copy(alpha = 0.45f), progressColor),
                    startX = 0f,
                    endX = (size.width * fraction).coerceAtLeast(1f),
                ),
                start = Offset(0f, y),
                end = Offset(size.width * fraction, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The pill's play/pause control.
 *
 * The state change is carried by the **shape**, not just by swapping a glyph. Paused it is a
 * circle; playing it pulls in to a squircle, and the corner radius springs between the two with a
 * little overshoot — so the button visibly reacts to the thing it just did, and you can read
 * playback state from the corner of your eye without resolving the icon at all. That is the part
 * a stock `FilledIconButton` cannot do: its shape is fixed, so the only thing that ever moved was
 * the glyph crossfade, and the button sat there inert underneath.
 *
 * (The version before that was worse still — it drove a `graphicsLayer` from an
 * `animateFloatAsState` whose target was a constant `1f`, an animation that could never run.)
 *
 * The fill is a vertical gradient off the accent rather than a flat tint, with a light rim, so it
 * catches light like every other surface in the app instead of reading as a Material chip dropped
 * onto glass. Press response is [pressScaleContainer], which fires on ACTION_DOWN, so the button
 * moves the instant the thumb lands rather than after a ripple has decided.
 */
@Composable
private fun MiniPlayerPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playerConnection: PlayerConnection,
    size: Dp = 48.dp,
) {
    val (enableHaptic) = rememberPreference(EnableHapticFeedbackKey, true)
    val haptic = rememberHaptic(enabled = enableHaptic)

    // Half the box is exactly a circle; ~a third of it is the squircle. Expressed as fractions of
    // [size] so the compact button morphs through the same *shape*, not the same absolute radius.
    // The spring is deliberately under-damped - the overshoot is what makes the press feel
    // answered.
    val corner by animateDpAsState(
        targetValue = if (isPlaying) size * 0.32f else size / 2f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playPauseCorner",
    )
    val shape = RoundedCornerShape(corner)

    val accent = MaterialTheme.colorScheme.primary
    val fill = remember(accent) {
        Brush.verticalGradient(
            listOf(accent, accent.copy(alpha = 0.80f)),
        )
    }
    // A soft accent glow under the button instead of a white rim around it. On glass a hard
    // 1dp highlight reads as a plastic chip laid on the pane; a coloured shadow reads as the
    // button sitting *in* it, and it is the only element in the pill that casts one, which is
    // exactly the hierarchy this control should have.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .pressScaleContainer()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false,
                ambientColor = accent,
                spotColor = accent,
            )
            .clip(shape)
            .background(fill)
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.click()
                    playerConnection.player.togglePlayPause()
                },
            ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(120)) +
                        scaleIn(
                            initialScale = 0.55f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )).togetherWith(
                        fadeOut(tween(90)) + scaleOut(targetScale = 1.35f, animationSpec = tween(90)),
                    )
                },
                label = "playPauseIcon",
            ) { playing ->
                Icon(
                    painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                    contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.46f),
                )
            }
        }
    }
}
