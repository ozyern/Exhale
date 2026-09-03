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
     * When the running duration timer was set, or -1 if none is running.
     *
     * Kept only so the UI can draw how far through the timer is. Remaining time alone cannot say
     * that: eleven minutes left is nearly over on a fifteen-minute timer and barely started on a
     * two-hour one, and a ring that cannot tell those apart is decoration.
     */
    var startTime by mutableLongStateOf(-1L)
        private set

    /** How many songs the running song counter was set to, so the same ring works in that mode. */
    var totalSongs by mutableIntStateOf(0)
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
        startAt(System.currentTimeMillis(), minute.minutes.inWholeMilliseconds)
    }

    /**
     * Push the finish line back by [minutes], keeping the timer running.
     *
     * The one action a running sleep timer actually needs. Everything else you would want from it
     * — how long is left, stopping it — you can already see; "I am still awake" is the one thing
     * that otherwise costs you a trip back through the menu to set the whole thing again.
     *
     * Extending stretches the ring rather than restarting it, so the progress you have already
     * made stays honest.
     */
    fun extend(minutes: Int) {
        val now = System.currentTimeMillis()
        val left = if (triggerTime != -1L) (triggerTime - now).coerceAtLeast(0L) else 0L
        val began = if (startTime != -1L) startTime else now
        startAt(began, (now - began) + left + minutes.minutes.inWholeMilliseconds)
    }

    private fun startAt(began: Long, total: Long) {
        cancelJob()
        songsRemaining = 0
        totalSongs = 0
        startTime = began
        triggerTime = began + total
        val wait = (triggerTime - System.currentTimeMillis()).coerceAtLeast(0L)
        sleepTimerJob = scope.launch {
            delay(wait)
            player.pause()
            triggerTime = -1L
            startTime = -1L
        }
    }

    /** Play [count] more songs — the current one included — then pause. */
    fun startAfterSongs(count: Int) {
        cancelJob()
        triggerTime = -1L
        startTime = -1L
        songsRemaining = count.coerceAtLeast(1)
        totalSongs = songsRemaining
    }

    fun clear() {
        cancelJob()
        songsRemaining = 0
        totalSongs = 0
        triggerTime = -1L
        startTime = -1L
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
            totalSongs = 0
            player.pause()
        }
    }
}
