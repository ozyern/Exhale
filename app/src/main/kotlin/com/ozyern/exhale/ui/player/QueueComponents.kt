@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.AquamorphicDampingRatio
import com.ozyern.exhale.constants.AquamorphicStiffness
import com.ozyern.exhale.extensions.metadata
import com.ozyern.exhale.db.entities.FormatEntity
import com.ozyern.exhale.playback.PlayerConnection
import com.ozyern.exhale.models.MediaMetadata
import com.ozyern.exhale.ui.component.ActionPromptDialog
import com.ozyern.exhale.ui.component.liquid.LiquidSlider
import com.ozyern.exhale.ui.component.BottomSheetPageState
import com.ozyern.exhale.ui.component.BottomSheetState
import com.ozyern.exhale.ui.component.MenuState
import com.ozyern.exhale.ui.component.bottomSheetDraggable
import com.ozyern.exhale.ui.menu.PlayerMenu
import com.ozyern.exhale.ui.utils.ShowMediaInfo
import com.ozyern.exhale.utils.makeTimeString
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt


/**
 * Current Song Header shown at the top of the queue
 * Displays album art, song info, and control buttons
 */
@Composable
fun CurrentSongHeader(
    sheetState: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    locked: Boolean,
    songCount: Int,
    queueDuration: Int,
    infiniteQueueEnabled: Boolean,
    backgroundColor: Color,
    onBackgroundColor: Color,
    onToggleLike: () -> Unit,
    onMenuClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLockClick: () -> Unit,
    onInfiniteQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .bottomSheetDraggable(sheetState)
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(onBackgroundColor.copy(alpha = 0.4f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(onBackgroundColor.copy(alpha = 0.06f))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = mediaMetadata?.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor
                )
                Text(
                    text = mediaMetadata?.artists?.joinToString(", ") { it.name } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (mediaMetadata?.liked == true)
                        MaterialTheme.colorScheme.primary
                    else onBackgroundColor
                )
            ) {
                Icon(
                    painter = painterResource(
                        if (mediaMetadata?.liked == true) R.drawable.favorite
                        else R.drawable.favorite_border
                    ),
                    contentDescription = stringResource(R.string.action_like),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(onBackgroundColor.copy(alpha = 0.06f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onLockClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = onBackgroundColor.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = onBackgroundColor.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = pluralStringResource(R.plurals.n_song, songCount, songCount)
                        + "  •  " + makeTimeString(queueDuration * 1000L),
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor.copy(alpha = 0.55f),
                modifier = Modifier.padding(end = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val uncheckedColors = ToggleButtonDefaults.toggleButtonColors(
                containerColor = onBackgroundColor.copy(alpha = 0.12f),
                contentColor = onBackgroundColor,
            )
            val checkedColors = ToggleButtonDefaults.toggleButtonColors(
                checkedContainerColor = onBackgroundColor.copy(alpha = 0.22f),
                checkedContentColor = onBackgroundColor,
            )
            val infiniteCheckedColors = ToggleButtonDefaults.toggleButtonColors(
                checkedContainerColor = MaterialTheme.colorScheme.primary,
                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = onBackgroundColor.copy(alpha = 0.12f),
                contentColor = onBackgroundColor.copy(alpha = 0.5f),
            )

            ToggleButton(
                checked = shuffleModeEnabled,
                onCheckedChange = { onShuffleClick() },
                modifier = Modifier
                    .weight(1f)
                    .size(48.dp),
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                colors = if (shuffleModeEnabled) checkedColors else uncheckedColors,
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.action_shuffle_on),
                    modifier = Modifier.size(22.dp)
                )
            }

            ToggleButton(
                checked = repeatMode != Player.REPEAT_MODE_OFF,
                onCheckedChange = { onRepeatClick() },
                modifier = Modifier
                    .weight(1f)
                    .size(48.dp),
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                colors = if (repeatMode != Player.REPEAT_MODE_OFF) checkedColors else uncheckedColors,
            ) {
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }

            ToggleButton(
                checked = infiniteQueueEnabled,
                onCheckedChange = { onInfiniteQueueClick() },
                modifier = Modifier
                    .weight(1f)
                    .size(48.dp),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                colors = infiniteCheckedColors,
            ) {
                Icon(
                    painter = painterResource(R.drawable.all_inclusive),
                    contentDescription = stringResource(R.string.similar_content),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.queue_continue_playing),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = onBackgroundColor
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.queue_autoplaying_similar),
            style = MaterialTheme.typography.bodySmall,
            color = onBackgroundColor.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(
            color = onBackgroundColor.copy(alpha = 0.08f),
            thickness = 1.dp
        )
    }
}

/**
 * Milliseconds until the sleep timer pauses playback.
 *
 * A duration timer answers this directly. A song counter has to be estimated: the rest of the
 * track playing now, plus the length of each track that will follow it. That estimate is what the
 * collapsed player bars count down, and it is worth making — "3 songs" is the setting people
 * choose, but "11:42" is the thing they want to know when they glance at it half asleep.
 *
 * It walks the timeline with the player's own repeat mode, so repeat-one gives the current track
 * back N times, which is exactly what will happen.
 */
internal fun sleepTimerMillisLeft(playerConnection: PlayerConnection): Long {
    val timer = playerConnection.service.sleepTimer

    if (timer.triggerTime != -1L) {
        return timer.triggerTime - System.currentTimeMillis()
    }

    return upcomingSongsMillis(playerConnection, timer.songsRemaining)
}

/**
 * Milliseconds the next [songs] tracks will take, counting the one playing now.
 *
 * Walks the timeline with the player's own repeat mode, so repeat-one gives the current track back
 * N times, which is exactly what will happen, and stops at the end of the queue rather than
 * pretending there is more music than there is.
 */
internal fun upcomingSongsMillis(playerConnection: PlayerConnection, songs: Int): Long {
    if (songs <= 0) return 0L

    val player = playerConnection.player
    // `duration` is TIME_UNSET until the track is prepared, and TIME_UNSET is a very large
    // negative number rather than an error.
    var total = (player.duration - player.currentPosition).coerceAtLeast(0L)
    if (songs == 1) return total

    val timeline = player.currentTimeline
    val window = Timeline.Window()
    var index = player.currentMediaItemIndex

    repeat(songs - 1) {
        index = timeline.getNextWindowIndex(index, player.repeatMode, player.shuffleModeEnabled)
        if (index == C.INDEX_UNSET) return total
        total += (timeline.getWindow(index, window).mediaItem.metadata?.duration ?: 0) * 1000L
    }

    return total
}

/** Wall-clock time, [millis] from now, in the reader's own short format. */
private fun clockTimeAfter(millis: Long): String =
    LocalTime.now()
        .plusSeconds((millis / 1000L).coerceAtLeast(0L))
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/**
 * The sleep timer.
 *
 * Two shapes, because "stop in twenty minutes" and "stop after three songs" are different
 * intentions and only one of them is a clock. Songs is the mode this gained and the one most people
 * actually want: nobody falls asleep on a schedule, and a clock timer does the one thing a music
 * player should never do, which is cut off mid-track.
 *
 * Top to bottom: what kind of timer, how much of it, when that lands, then the two ways to change
 * it — presets for the answer that is nearly always right, a fine control for when it is not. The
 * ordering is the argument: the mode switch changes what the number *means*, so it has to be
 * settled before the number is worth reading.
 *
 * A timer already running takes over the bottom of the sheet with its own state and a way out,
 * rather than being something you discover by setting a second one.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onConfirmMinutes: (Int) -> Unit,
    onConfirmSongs: (Int) -> Unit,
    onCancelTimer: () -> Unit = {},
    initialMinutes: Int = 30,
    initialSongs: Int = 3,
) {
    val playerConnection = LocalPlayerConnection.current
    val timer = playerConnection?.service?.sleepTimer

    var songsMode by rememberSaveable { mutableStateOf(false) }
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    var songs by rememberSaveable { mutableIntStateOf(initialSongs) }

    // Both fields are read so the running banner follows either mode.
    val runningSongs = timer?.songsRemaining ?: 0
    val runningTrigger = timer?.triggerTime ?: -1L
    val isRunning = runningTrigger != -1L || runningSongs > 0

    // Re-read every second, so a running timer's countdown is a countdown rather than a snapshot
    // from whenever the sheet happened to open. Also what keeps "Stops at" honest for a song
    // timer, whose estimate moves as the current track plays out.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    val plannedMillis = remember(songsMode, minutes, songs, playerConnection, now) {
        if (songsMode) {
            playerConnection?.let { upcomingSongsMillis(it, songs) } ?: 0L
        } else {
            minutes * 60_000L
        }
    }

    ActionPromptDialog(
        titleBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.headlineMediumEmphasized
                )
            }
        },
        onDismiss = onDismiss,
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                SleepTimerModeSwitch(
                    songsMode = songsMode,
                    onModeChange = { songsMode = it },
                )

                Spacer(Modifier.height(22.dp))

                // The number, at the size of the decision it is.
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (songsMode) songs.toString() else minutes.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (songsMode) {
                            unitOf(pluralStringResource(R.plurals.n_song, songs, songs), songs)
                        } else {
                            unitOf(pluralStringResource(R.plurals.minute, minutes, minutes), minutes)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                // The promise, in a form you can check against a clock.
                //
                // This line is the reason the dialog was rebuilt. A timer is a claim about the
                // future and "45" is not a claim anyone can check; "Stops at 11:42 PM" is. It is
                // shown for both modes, which is also what makes the two comparable — you can see
                // that three songs is about eleven minutes without doing the arithmetic that made
                // you reach for minutes in the first place.
                Text(
                    text = stringResource(R.string.sleep_timer_stops_at, clockTimeAfter(plannedMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                SleepTimerPresets(
                    options = if (songsMode) SleepTimerSongPresets else SleepTimerMinutePresets,
                    selected = if (songsMode) songs else minutes,
                    onSelect = { if (songsMode) songs = it else minutes = it },
                    label = { value ->
                        if (songsMode && value == 1) {
                            stringResource(R.string.sleep_timer_this_song)
                        } else {
                            value.toString()
                        }
                    },
                )

                Spacer(Modifier.height(16.dp))

                if (songsMode) {
                    // A stepper, not a slider. The useful range is one to a dozen, and a slider
                    // across twelve values spends most of its width being hard to land on.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        SleepTimerStepButton(
                            icon = R.drawable.remove,
                            enabled = songs > 1,
                            onClick = { songs = (songs - 1).coerceAtLeast(1) },
                        )
                        SleepTimerStepButton(
                            icon = R.drawable.add,
                            enabled = songs < SleepTimerMaxSongs,
                            onClick = { songs = (songs + 1).coerceAtMost(SleepTimerMaxSongs) },
                        )
                    }
                } else {
                    LiquidSlider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = ((it / 5f).roundToInt() * 5).coerceIn(5, 120) },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // A timer already running owns the bottom of the sheet. Finding out you had one
                // only by accidentally setting a second is the failure this prevents.
                if (isRunning && playerConnection != null) {
                    Spacer(Modifier.height(18.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 6.dp,
                                top = 6.dp,
                                bottom = 6.dp,
                            ),
                        ) {
                            val leftMillis = sleepTimerMillisLeft(playerConnection)

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (runningSongs > 0) {
                                        pluralStringResource(
                                            R.plurals.n_songs_left,
                                            runningSongs,
                                            runningSongs,
                                        )
                                    } else {
                                        makeTimeString(leftMillis)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.sleep_timer_stops_at,
                                        clockTimeAfter(leftMillis),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }

                            TextButton(onClick = onCancelTimer) {
                                Text(
                                    text = stringResource(R.string.sleep_timer_stop),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val cancelText = stringResource(android.R.string.cancel)
            val startText = stringResource(R.string.sleep_timer_start)

            ButtonGroup(
                overflowIndicator = {
                    ButtonGroupDefaults.OverflowIndicator(it)
                }
            ) {
                clickableItem(
                    onClick = onDismiss,
                    label = cancelText
                )

                clickableItem(
                    onClick = {
                        if (songsMode) onConfirmSongs(songs) else onConfirmMinutes(minutes)
                    },
                    label = startText
                )
            }
        }
    )
}

/**
 * The unit half of a formatted quantity — "minutes" out of "45 minutes".
 *
 * The plurals carry the number because that is how every other caller wants them; here the number
 * is already set three times larger beside it, so repeating it would read as "45 45 minutes".
 * Falls back to the whole string if the number is not where it was expected, which is the case in
 * languages that put it elsewhere.
 */
private fun unitOf(formatted: String, value: Int): String =
    formatted.removePrefix("$value").trim().ifEmpty { formatted }

private val SleepTimerMinutePresets = listOf(15, 30, 45, 60, 90)
private val SleepTimerSongPresets = listOf(1, 2, 3, 5, 10)
private const val SleepTimerMaxSongs = 50

/**
 * Duration or Songs.
 *
 * Hand-rolled rather than a pair of chips. Two chips are two independent toggles that happen to sit
 * next to each other; this is one switch with two positions, and the pill sliding between them is
 * what says that choosing one is unchoosing the other.
 */
@Composable
private fun SleepTimerModeSwitch(
    songsMode: Boolean,
    onModeChange: (Boolean) -> Unit,
) {
    val bias by animateFloatAsState(
        targetValue = if (songsMode) 1f else -1f,
        animationSpec = spring(
            dampingRatio = AquamorphicDampingRatio,
            stiffness = AquamorphicStiffness,
        ),
        label = "sleepTimerMode",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .align(BiasAlignment(bias, 0f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )

        Row(Modifier.fillMaxSize()) {
            SleepTimerModeLabel(
                text = stringResource(R.string.sleep_timer_duration),
                selected = !songsMode,
                onClick = { onModeChange(false) },
                modifier = Modifier.weight(1f),
            )
            SleepTimerModeLabel(
                text = stringResource(R.string.sleep_timer_songs),
                selected = songsMode,
                onClick = { onModeChange(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SleepTimerModeLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "sleepTimerModeLabel",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * The one-tap row above the fine control.
 *
 * Equal-width cells rather than chips that size to their own text, so the five options form a
 * single ruler across the dialog — "15 30 45 60 90" reads as a scale, five differently-sized
 * capsules read as five unrelated buttons.
 */
@Composable
private fun SleepTimerPresets(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    label: @Composable (Int) -> String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { value ->
            val chosen = value == selected
            val container by animateColorAsState(
                if (chosen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "sleepPresetBg",
            )
            val content by animateColorAsState(
                if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                label = "sleepPresetFg",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(container)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(value),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SleepTimerStepButton(
    @DrawableRes icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Codec information row displayed when showCodecOnPlayer is enabled.
 */
@Composable
fun CodecInfoRow(
    codec: String,
    bitrate: String,
    fileSize: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 30.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Text(
            text = buildString {
                append(codec)
                if (bitrate != "Unknown") {
                    append(" • ")
                    append(bitrate)
                }
                if (fileSize.isNotEmpty()) {
                    append(" • ")
                    append(fileSize)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * V2 Design Style collapsed queue content.
 */
@Composable
fun QueueCollapsedContentV2(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    repeatMode: Int,
    mediaMetadata: MediaMetadata?,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    onRepeatModeClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec =
                currentFormat.codecs
                    .takeIf { it.isNotBlank() }
                    ?: currentFormat.mimeType.substringAfter("/", missingDelimiterValue = currentFormat.mimeType).uppercase()

            val container =
                currentFormat.mimeType.substringAfter("/", missingDelimiterValue = currentFormat.mimeType).uppercase()

            val codecLabel =
                if (container.isNotBlank() && !codec.equals(container, ignoreCase = true)) {
                    "$codec ($container)"
                } else {
                    codec
                }

            val bitrate =
                if (currentFormat.bitrate > 0) {
                    "${currentFormat.bitrate / 1000} kbps"
                } else {
                    "Unknown"
                }

            val sampleRateText =
                currentFormat.sampleRate?.takeIf { it > 0 }?.let { sampleRate ->
                    val khz = (sampleRate / 100.0).roundToInt() / 10.0
                    "$khz kHz"
                }

            val fileSizeText =
                if (currentFormat.contentLength > 0) {
                    "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
                } else {
                    ""
                }

            val extraText =
                listOfNotNull(sampleRateText, fileSizeText.takeIf { it.isNotBlank() })
                    .joinToString(separator = " • ")

            CodecInfoRow(
                codec = codecLabel,
                bitrate = bitrate,
                fileSize = extraText,
                textColor = textBackgroundColor.copy(alpha = 0.7f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 10.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            val buttonSize = 42.dp
            val iconSize = 24.dp
            val borderColor = textBackgroundColor.copy(alpha = 0.35f)

            // Queue button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
                    .border(
                        1.dp,
                        borderColor,
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
                    .clickable { onExpandQueue() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.queue_music),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = textBackgroundColor
                )
            }

            // Sleep timer button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onSleepTimerClick() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled,
                ) { enabled ->
                    if (enabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            color = textBackgroundColor,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = textBackgroundColor
                        )
                    }
                }
            }

            // Lyrics button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onShowLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.lyrics),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = textBackgroundColor
                )
            }

            // Repeat mode button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(
                        RoundedCornerShape(
                            topStart = 10.dp,
                            bottomStart = 10.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp
                        )
                    )
                    .border(
                        1.dp,
                        borderColor,
                        RoundedCornerShape(
                            topStart = 10.dp,
                            bottomStart = 10.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp
                        )
                    )
                    .clickable { onRepeatModeClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                    tint = textBackgroundColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Menu button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(textButtonColor)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = iconButtonColor
                )
            }
        }
    }
}

/**
 * V3 Design Style collapsed queue content.
 */
@Composable
fun QueueCollapsedContentV3(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"

            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = "",
                textColor = textBackgroundColor.copy(alpha = 0.5f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            // Queue button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpandQueue() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = textBackgroundColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(id = R.string.queue),
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            // Sleep timer button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSleepTimerClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled,
                ) { enabled ->
                    if (enabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            color = textBackgroundColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = textBackgroundColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Lyrics button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onShowLyrics() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = textBackgroundColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(id = R.string.lyrics),
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            // Menu button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = textBackgroundColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * V1 Design Style collapsed queue content (text buttons).
 */
@Composable
fun QueueCollapsedContentV1(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"
            val fileSize = if (currentFormat.contentLength > 0) {
                "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
            } else ""

            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = fileSize,
                textColor = textBackgroundColor.copy(alpha = 0.7f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 12.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
        ) {
            TextButton(
                onClick = onExpandQueue,
                modifier = Modifier.weight(1f),
                shapes = ButtonDefaults.shapes(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.queue),
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            TextButton(
                onClick = onSleepTimerClick,
                modifier = Modifier.weight(1.2f),
                shapes = ButtonDefaults.shapes(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        label = "sleepTimer",
                        targetState = sleepTimerEnabled,
                    ) { enabled ->
                        if (enabled) {
                            Text(
                                text = makeTimeString(sleepTimerTimeLeft),
                                color = textBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.sleep_timer),
                                color = textBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onShowLyrics,
                modifier = Modifier.weight(1f),
                shapes = ButtonDefaults.shapes(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.lyrics),
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

/**
 * V4 Design Style collapsed queue content (pill buttons).
 */
@Composable
fun QueueCollapsedContentV4(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    mediaMetadata: MediaMetadata?,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"
            val fileSize = if (currentFormat.contentLength > 0) {
                "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
            } else ""

            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = fileSize,
                textColor = textBackgroundColor.copy(alpha = 0.6f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            val buttonSize = 48.dp
            val iconSize = 22.dp

            // Queue button (pill)
            Box(
                modifier = Modifier
                    .height(buttonSize)
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(textBackgroundColor.copy(alpha = 0.1f))
                    .clickable { onExpandQueue() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.queue),
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sleep timer button (circle)
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(
                        if (sleepTimerEnabled) textBackgroundColor.copy(alpha = 0.2f)
                        else textBackgroundColor.copy(alpha = 0.1f)
                    )
                    .clickable { onSleepTimerClick() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled,
                ) { enabled ->
                    if (enabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            color = textBackgroundColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = textBackgroundColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Lyrics button (pill)
            Box(
                modifier = Modifier
                    .height(buttonSize)
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(textBackgroundColor.copy(alpha = 0.1f))
                    .clickable { onShowLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.lyrics),
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun QueueCollapsedContentV7(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    onExpandQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onDeviceClick: () -> Unit,
    deviceName: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"
            val fileSize = if (currentFormat.contentLength > 0) {
                "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
            } else ""

            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = fileSize,
                textColor = textBackgroundColor.copy(alpha = 0.6f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            val iconSize = 22.dp

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onExpandQueue,
                    shape = CircleShape,
                    color = textBackgroundColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.queue_music),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = textBackgroundColor
                        )
                    }
                }

                Surface(
                    onClick = onShowLyrics,
                    shape = CircleShape,
                    color = textBackgroundColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.lyrics),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = textBackgroundColor
                        )
                    }
                }
            }

            Surface(
                onClick = onDeviceClick,
                shape = RoundedCornerShape(20.dp),
                color = textBackgroundColor.copy(alpha = 0.08f),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bluetooth),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.labelMedium,
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}