/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * An [ImageRequest] for now-playing artwork that is pinned to the memory cache under a stable key.
 *
 * The mini player exists in two places that swap during the Dynamic-Island morph: the standalone
 * pill above the nav bar (State A) and the capsule inside the nav bar (State B). Those are
 * different composables, so the artwork leaves the composition and re-enters it mid-animation.
 * With Coil's defaults that means a fresh request, a fresh (possibly async) decode and a 100ms
 * crossfade from nothing — the artwork visibly blinks at exactly the moment the morph is drawing
 * the eye to it.
 *
 * Two things prevent that:
 *
 *  * **A stable key.** `memoryCacheKey` + `placeholderMemoryCacheKey` are both set to the URL, so
 *    every site that shows this song's art reads and writes the same entry. The second site gets
 *    a synchronous cache hit and, failing that, still paints the cached bitmap as its placeholder
 *    instead of empty space. (Coil's default key includes the resolved size, so two views of
 *    different sizes would otherwise miss each other entirely.)
 *  * **No crossfade.** A crossfade is only ever correct when something is actually arriving; on a
 *    cache hit it just fades in a bitmap that was already available. Off, a re-entering image is
 *    painted whole on its first frame.
 *
 * The app-wide loader already reserves a quarter of the heap for the memory cache (see
 * `App.newImageLoader`), so the entry survives comfortably for the length of any transition.
 */
@Composable
fun rememberPinnedArtworkRequest(url: String): ImageRequest {
    val context = LocalContext.current
    return remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .memoryCacheKey(url)
            .placeholderMemoryCacheKey(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
}
