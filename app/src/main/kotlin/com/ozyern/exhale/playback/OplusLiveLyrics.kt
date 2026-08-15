/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
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
 * own players use. Two honest caveats follow from that. First, stock ColorOS historically gates
 * the lyric pipeline behind a package whitelist, so on an untouched ROM a third-party player may
 * publish a perfectly valid payload and still be ignored; users running the Bridge get it
 * unconditionally. Second, being a private SystemUI surface, the key can change between OPlus
 * releases. Neither is a reason not to publish — the payload is a few hundred bytes attached to
 * metadata we already build, and it costs nothing on devices that ignore it.
 *
 * See `docs/PLAYER_INTEGRATION.md` of Andrea-lyz/ColorOS-Live-Lyrics-Bridge.
 */
object OplusLiveLyrics {

    /** The media-metadata key OPlus SystemUI reads the lyric document from. */
    const val METADATA_KEY = "lyricInfo"

    /**
     * Manifest opt-in that lets a non-whitelisted package into the OPlus media-history stack, so
     * the ColorOS media card survives the app being fully stopped. Declared in AndroidManifest.
     */
    const val MANIFEST_META_MEDIA_HISTORY =
        "io.github.andrealtb.lockscreenlyrics.OPLUS_MEDIA_HISTORY"

    /**
     * A single LRC timestamp: `[m:ss]`, `[mm:ss.xx]`, `[mm:ss:xxx]`.
     *
     * The protocol requires at least one of these. Plain unsynced lyrics — which several of our
     * providers legitimately return — must NOT be published: the lock screen cannot scroll them,
     * and a payload it cannot use is worse than no payload, because it displaces the "no lyrics"
     * state the ROM would otherwise fall back to.
     */
    private val TimedLineRegex = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""")

    fun isTimed(lyrics: String?): Boolean =
        !lyrics.isNullOrBlank() && TimedLineRegex.containsMatchIn(lyrics)

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
        if (!isTimed(lyrics)) return null
        return JSONObject()
            .put("songName", songName)
            .put("artist", artist)
            .put("songId", songId)
            .put("lyric", lyrics)
            .toString()
    }

    /** The payload currently attached to this item, if any. */
    fun MediaItem.lyricInfo(): String? = mediaMetadata.extras?.getString(METADATA_KEY)

    /**
     * Returns a copy of this item carrying [payload].
     *
     * `buildUpon` is what makes this safe: it preserves the local configuration, and with it the
     * `tag` that the whole app reads its own [com.ozyern.exhale.models.MediaMetadata] out of via
     * `Player.currentMetadata`. Rebuilding the item from scratch here would strip that tag and
     * quietly break every screen that asks the player what is playing.
     */
    fun MediaItem.withLyricInfo(payload: String): MediaItem {
        val extras = mediaMetadata.extras?.let { Bundle(it) } ?: Bundle()
        extras.putString(METADATA_KEY, payload)
        return buildUpon()
            .setMediaMetadata(
                mediaMetadata.buildUpon()
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}
