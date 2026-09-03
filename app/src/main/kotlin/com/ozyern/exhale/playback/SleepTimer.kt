/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * Stops playback after a while — where "a while" is either a length of time or a number of songs.
 *
 * Two modes, never both at once: [triggerTime] counts down a clock, [songsRemaining] counts down
 * tracks. Starting either cancels the other, because "stop in 20 minutes, and also after 3 songs"
 * has no useful meaning — one of them would fire and the other would be a lie left on screen.
 *
 * The song counter is the more honest one for falling asleep to music. Twenty minutes cuts off
 * mid-track; three songs does not.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    val player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null

    /** When the duration timer fires, or -1 if no duration timer is running. */
    var triggerTime by mutableLongStateOf(-1L)
        private set

    /**
     * How many more songs will finish before playback pauses. 0 when the song counter is off.
     *
     * The song playing right now counts as one, so 1 means "stop at the end of this song".
     */
    var songsRemaining by mutableIntStateOf(0)
        private set

    val isActive: Boolean
        get() = triggerTime != -1L || songsRemaining > 0

    /** Pause in [minute] minutes, wherever in a song that lands. */
    fun start(minute: Int) {
        cancelJob()
        songsRemaining = 0
        triggerTime = System.currentTimeMillis() + minute.minutes.inWholeMilliseconds
        sleepTimerJob = scope.launch {
            delay(minute.minutes)
            player.pause()
            triggerTime = -1L
        }
    }

    /** Play [count] more songs — the current one included — then pause. */
    fun startAfterSongs(count: Int) {
        cancelJob()
        triggerTime = -1L
        songsRemaining = count.coerceAtLeast(1)
    }

    fun clear() {
        cancelJob()
        songsRemaining = 0
        triggerTime = -1L
    }

    private fun cancelJob() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // Only a song that ran out counts. A transition can also mean the user pressed next, or
        // the queue was rebuilt underneath us, and neither is a song listened to — counting those
        // would let someone skip their way through a sleep timer in five seconds.
        val songFinished = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
        if (songFinished) {
            consumeSong()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        // The queue ran out. Nothing will transition again, so the last song has to be counted
        // here or a timer set on the final track never fires.
        if (playbackState == Player.STATE_ENDED) {
            consumeSong()
        }
    }

    private fun consumeSong() {
        if (songsRemaining <= 0) return

        songsRemaining -= 1
        if (songsRemaining == 0) {
            player.pause()
        }
    }
}
