/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.playback

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.ozyern.exhale.lyrics.LyricsEntry
import com.ozyern.exhale.lyrics.LyricsUtils
import org.json.JSONObject

/**
 * Lock-screen lyrics for OxygenOS / ColorOS "Live Space".
 *
 * OPlus SystemUI reads timed lyrics off the active media session under a single string metadata
 * key, `lyricInfo`, holding a small JSON document. Publish it and the lock screen's music focus
 * mode — the one where the artwork fills the screen — shows our lyrics instead of "No lyrics",
 * and the Live Alert capsule can scroll the current line.
 *
 * This is a *published third-party protocol*, not an entry in the Android SDK: the schema below
 * comes from the ColorOS Live Lyrics Bridge integration spec, which documents the payload OPlus's
 * own players use. The reader hooks `MediaSession#setMetadata`, so what matters is not that the
 * payload exists on our [MediaItem] but that a `setMetadata` call carrying it actually reaches the
 * platform session — see [MusicService.publishLiveLyrics] for why that is the hard part under
 * Media3. Being a private SystemUI surface, the key can also change between OPlus releases.
 *
 * See `docs/PLAYER_INTEGRATION.md` of Andrea-lyz/ColorOS-Live-Lyrics-Bridge.
 */
object OplusLiveLyrics {

    /** The media-metadata key OPlus SystemUI reads the lyric document from. */
    const val METADATA_KEY = "lyricInfo"

    /**
     * Every key the whole-song document is published under, not just [METADATA_KEY].
     *
     * The lyric surfaces are separate SystemUI consumers of the same media session and they do
     * not have to agree on a key — which is exactly what the observed split looks like: the Live
     * Alert capsule scrolls our lines correctly while the lock screen still falls back to "no
     * lyrics". `lyricInfo` is the key documented against ColorOS 15; the class doc already warned
     * a private SystemUI key can move between OPlus releases, and a lock screen reading a renamed
     * key fails in precisely this way.
     *
     * Publishing under all of them is close to free — one `Bundle` with a few more short strings —
     * and it cannot regress the surface that already works, because that surface keeps reading the
     * key it always read. Every other ROM ignores all of them.
     */
    val METADATA_KEY_ALIASES = listOf(
        METADATA_KEY,
        "oplus.lyricInfo",
        "com.oplus.media.metadata.LYRIC",
        "android.media.metadata.LYRIC",
    )

    /**
     * Keys carrying the **single line playing right now**, refreshed as playback advances.
     *
     * This is a different shape of protocol from [METADATA_KEY_ALIASES], not another guess at a
     * name for the same one, and it closes a real gap rather than widening a net. The document
     * keys hand the consumer a whole LRC and leave it to do its own timing against the session's
     * playback position; a consumer built the other way round — one that expects to be *fed* the
     * current line and simply renders whatever it was last given — receives nothing at all from
     * us today. A capsule that scrolls a document while a lock screen shows nothing is consistent
     * with the lock screen being that second kind of consumer.
     *
     * These ride on the session extras rather than the track metadata: extras are forwarded
     * verbatim and immediately, so publishing per line there costs nothing and cannot trip the
     * per-track metadata debounce the document path has to be careful about.
     */
    val CURRENT_LINE_KEY_ALIASES = listOf(
        "currentLyric",
        "oplus.currentLyric",
        "com.oplus.media.metadata.CURRENT_LYRIC",
        "lyric",
    )

    /** Companion keys for the current line's start time, in milliseconds. */
    val CURRENT_LINE_TIME_KEY_ALIASES = listOf(
        "currentLyricTime",
        "oplus.currentLyricTime",
    )

    /**
     * Manifest opt-in that keeps the ColorOS media card alive after the app is fully stopped.
     * Declared in AndroidManifest. Not required for lyric delivery itself.
     *
     * Worth being clear about what this is and is not: the name is the community bridge's, and it
     * is the *bridge* that reads it. Stock ColorOS SystemUI gates its lock-screen lyric surface on
     * its own package whitelist, which a third-party player is never going to be on. So on a
     * device without the bridge installed, a working Live Alert capsule and an empty lock screen
     * is a possible *correct* outcome of everything here — the capsule is an open surface, the
     * lock screen may simply not be one. The multi-key and current-line publication exist to cover
     * the case where it IS readable and we were addressing it wrongly; they cannot open a door
     * that is bolted from the other side.
     */
    const val MANIFEST_META_MEDIA_HISTORY =
        "io.github.andrealtb.lockscreenlyrics.OPLUS_MEDIA_HISTORY"

    /** A line-level LRC timestamp: `[m:ss]`, `[mm:ss.xx]`, `[mm:ss:xxx]`. */
    private val LineTimeRegex = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""")

    /** A word-level timestamp inside an enhanced-LRC line: `<mm:ss.xx>`. */
    private val WordTimeRegex = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    /**
     * Normalises whatever a provider gave us into the line-level LRC the ROM can scroll, or null
     * when the lyrics cannot drive a lock screen at all.
     *
     * Two provider formats reach us. Plain LRC passes through. TTML — which the word-synced
     * providers return, and which is what a premium-looking lyrics screen is usually rendering —
     * is not LRC and would otherwise be silently dropped here, so it is flattened to one timed
     * line per cue.
     *
     * Unsynced plain text is deliberately rejected: the lock screen cannot scroll it, and a
     * payload it cannot use is worse than none, because it displaces the "no lyrics" state the
     * ROM would otherwise fall back to.
     */
    fun toLrc(lyrics: String?): String? {
        if (lyrics.isNullOrBlank()) return null
        if (LyricsUtils.isTtml(lyrics)) return ttmlToLrc(lyrics) { entry -> entry.text }
        return lyrics.takeIf { LineTimeRegex.containsMatchIn(it) }
    }

    /**
     * The word-timed form, for ROMs that highlight per syllable, or null when we have no word
     * timings to offer. Enhanced LRC passes through; TTML is re-emitted with its word timings.
     */
    private fun toEnhancedLrc(lyrics: String): String? {
        if (LyricsUtils.isTtml(lyrics)) {
            return ttmlToLrc(lyrics) { entry ->
                val words = entry.words?.takeIf { it.isNotEmpty() } ?: return@ttmlToLrc null
                words.joinToString("") { word -> stamp(word.startTime, "<", ">") + word.text }
            }
        }
        return lyrics.takeIf { WordTimeRegex.containsMatchIn(it) }
    }

    /**
     * Builds the `lyricInfo` document, or null when [lyrics] cannot drive a lock screen.
     *
     * @param songId a *stable* id for the track. The ROM uses it to decide whether a metadata
     *   update is a new song or a refresh of the current one, so it must be our media id and
     *   nothing derived from the lyric text.
     */
    fun buildPayload(
        songId: String,
        songName: String,
        artist: String,
        lyrics: String?,
    ): String? {
        val lrc = toLrc(lyrics) ?: return null
        return buildPayloadFromLrc(songId, songName, artist, lrc, lyrics)
    }

    /**
     * The document as [buildPayload] would emit it, from an already-normalised [lrc].
     *
     * Split out so the caller can normalise once and keep the LRC for the line ticker instead of
     * running the TTML flattener a second time to get the same string back.
     */
    fun buildPayloadFromLrc(
        songId: String,
        songName: String,
        artist: String,
        lrc: String,
        rawLyrics: String?,
    ): String {
        return JSONObject()
            .put("songName", songName)
            .put("artist", artist)
            .put("songId", songId)
            .put("lyric", lrc)
            .apply { rawLyrics?.let { raw -> toEnhancedLrc(raw)?.let { put("rawLyric", it) } } }
            .toString()
    }

    /** The payload currently attached to this item, if any. */
    fun MediaItem.lyricInfo(): String? = mediaMetadata.extras?.getString(METADATA_KEY)

    /**
     * Returns a copy of this item carrying [payload], and optionally [signal].
     *
     * `buildUpon` is what makes this safe: it preserves the local configuration, and with it the
     * `tag` that the whole app reads its own [com.ozyern.exhale.models.MediaMetadata] out of via
     * `Player.currentMetadata`. Rebuilding the item from scratch here would strip that tag and
     * quietly break every screen that asks the player what is playing.
     */
    fun MediaItem.withLyricInfo(payload: String, signal: Uri? = null): MediaItem {
        val extras = mediaMetadata.extras?.let { Bundle(it) } ?: Bundle()
        METADATA_KEY_ALIASES.forEach { key -> extras.putString(key, payload) }
        return buildUpon()
            .setMediaMetadata(
                mediaMetadata.buildUpon()
                    .setExtras(extras)
                    .build()
            )
            .apply {
                if (signal != null) {
                    setRequestMetadata(
                        requestMetadata.buildUpon()
                            .setMediaUri(signal)
                            .build()
                    )
                }
            }
            .build()
    }

    /**
     * The value written to `requestMetadata.mediaUri` to make a lyrics-only change visible to
     * Media3's session layer. See [MusicService.publishLiveLyrics] for why this is needed;
     * [revision] must differ from the previous publication for the same track.
     */
    fun signalUri(songId: String, revision: Int): Uri =
        "exhale://live-lyrics/$songId/$revision".toUri()

    /**
     * Flattens TTML to LRC. [line] renders one cue's body and may return null to drop it, which
     * is how the enhanced form opts out of cues that carry no word timings.
     */
    private fun ttmlToLrc(ttml: String, line: (LyricsEntry) -> String?): String? {
        val entries = runCatching { LyricsUtils.parseTtml(ttml) }.getOrNull().orEmpty()
        if (entries.isEmpty()) return null
        val rendered = entries.mapNotNull { entry ->
            val body = line(entry) ?: return@mapNotNull null
            stamp(entry.time / 1000.0, "[", "]") + body
        }
        return rendered.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    /** `mm:ss.cc` wrapped in the given delimiters. [seconds] is clamped at zero. */
    private fun stamp(seconds: Double, open: String, close: String): String {
        val totalMs = (seconds * 1000.0).toLong().coerceAtLeast(0L)
        val minutes = totalMs / 60_000
        val secs = (totalMs % 60_000) / 1000
        val centis = (totalMs % 1000) / 10
        return "%s%02d:%02d.%02d%s".format(open, minutes, secs, centis, close)
    }
}
