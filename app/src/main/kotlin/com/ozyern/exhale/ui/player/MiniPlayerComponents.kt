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
            }
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
                style = MaterialTheme.typography.titleSmall,
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    .basicMarquee()
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // A rounded square, matching every other piece of artwork in the app. The disc-in-a-ring
    // this replaced spent 8dp of a 44dp box on a progress indicator, so the actual album art
    // was 36dp and cropped to a circle — the least legible way to show a square image.
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
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
                modifier = Modifier.size(24.dp)
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
 * Rebuilt. What was here before crammed five separate controls into a 64dp capsule — a circular
 * thumbnail wearing a circular progress ring, title, artist, a person button, a like button and a
 * stock filled play button — so nothing in it had any weight and the whole row read as a toolbar
 * rather than as the thing that is playing. Three changes carry the redesign:
 *
 *  * **Artwork is a rounded square again.** Every other piece of album art in the app is a rounded
 *    rectangle; only this one was a disc, and it was wrapped in a progress ring that fought the
 *    thumbnail for the same 44dp. Progress moved to a hairline that runs along the bottom of the
 *    pill, where it is both more legible and out of the way.
 *  * **The artist line navigates.** The separate person button existed only to open the artist;
 *    tapping the artist's name does that now, which is the affordance people already try, and the
 *    row gets a whole 40dp slot back.
 *  * **Play/pause is the loudest thing in the pill**, which is the one control the pill exists for.
 *    See [MiniPlayerPlayPauseButton].
 */
@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState
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

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 9.dp, end = 9.dp),
        ) {
            MiniPlayerArtwork(
                mediaMetadata = mediaMetadata,
                isLoading = isLoading,
            )

            Spacer(modifier = Modifier.width(12.dp))

            mediaMetadata?.let {
                MiniPlayerInfo(
                    mediaMetadata = it,
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

            Spacer(modifier = Modifier.width(4.dp))

            // Like. A quiet 38dp glyph beside a loud 46dp play button, so the hierarchy in the
            // pill is unambiguous instead of two same-sized circles competing.
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
                    .size(38.dp)
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
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                        },
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            MiniPlayerPlayPauseButton(
                isPlaying = isPlaying,
                isLoading = isLoading,
                playerConnection = playerConnection
            )
        }

        // Progress hairline hugging the bottom of the capsule. Inset from the rounded ends so it
        // never pokes out of the pill, and drawn — not laid out — so ticking it forward every
        // second costs one draw and no recomposition of the row above it.
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .padding(horizontal = 26.dp),
        ) {
            val fraction =
                if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            val stroke = size.height
            val y = size.height / 2f
            drawLine(
                color = progressTrackColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (fraction > 0f) {
                drawLine(
                    color = progressColor,
                    start = Offset(0f, y),
                    end = Offset(size.width * fraction, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * The pill's play/pause control.
 *
 * The old one was a stock `FilledIconButton` in flat `primary`, with a `buttonScale` spring whose
 * target was a constant `1f` — an animation that could never run, driving a `graphicsLayer` that
 * could never change. It is now a hand-built disc: a vertical gradient off the accent so it catches
 * light like the rest of the chrome, a soft rim, and a real press response through
 * [pressScaleContainer] (which fires on ACTION_DOWN, unlike a ripple, so the button moves the
 * instant the thumb lands).
 */
@Composable
private fun MiniPlayerPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playerConnection: PlayerConnection,
) {
    val (enableHaptic) = rememberPreference(EnableHapticFeedbackKey, true)
    val haptic = rememberHaptic(enabled = enableHaptic)

    val accent = MaterialTheme.colorScheme.primary
    val fill = remember(accent) {
        Brush.verticalGradient(
            listOf(
                accent.copy(alpha = 1f),
                accent.copy(alpha = 0.82f),
            ),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .pressScaleContainer()
            .clip(CircleShape)
            .background(fill)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape = CircleShape,
            )
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
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
