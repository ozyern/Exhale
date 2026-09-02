/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ozyern.exhale.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where the blobs sit, as a fraction of height. Their own falloff is what ends the effect — past
 * roughly half the viewport a tint stops reading as light from above and starts reading as a
 * coloured screen.
 */
private const val GlowBandFraction = 0.42f

/** Palettes are keyed by song id and shared process-wide, so returning to Home is free. */
private const val AmbientCacheLimit = 24
private val ambientColorCache = object : LinkedHashMap<String, List<Color>>(
    AmbientCacheLimit,
    0.75f,
    true,
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Color>>) =
        size > AmbientCacheLimit
}

/**
 * The palette of the currently playing artwork, for use as ambient colour.
 *
 * Shares [PlayerColorExtractor] with the full player so Home and the player agree about what a
 * song looks like — a cover that reads teal behind the controls reads teal at the top of Home too.
 * Extraction is a bitmap decode, so results are cached by song id, and nothing runs at all when
 * nothing is playing.
 */
@Composable
fun rememberArtworkAmbientColors(
    songId: String?,
    thumbnailUrl: String?,
): State<List<Color>> {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val colors = remember { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(songId, thumbnailUrl) {
        if (songId == null || thumbnailUrl == null) {
            colors.value = emptyList()
            return@LaunchedEffect
        }
        ambientColorCache[songId]?.let {
            colors.value = it
            return@LaunchedEffect
        }
        val extracted = runCatching {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                // Palette has to read pixels back, which a hardware bitmap will not allow.
                .allowHardware(false)
                .build()
            val bitmap = withContext(Dispatchers.IO) {
                context.imageLoader.execute(request).image?.toBitmap()
            } ?: return@runCatching emptyList()
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bitmap)
                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                    .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                    .generate()
            }
            PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallback)
        }.getOrNull().orEmpty()

        if (extracted.isNotEmpty()) ambientColorCache[songId] = extracted
        colors.value = extracted
    }

    return colors
}

/**
 * A soft wash of the current artwork's colour bleeding down from the top of a scrolling surface.
 *
 * Home was a flat sheet: correct, and completely inert. This puts the record back in the room —
 * the surface picks up whatever is playing, the way light takes the colour of what it falls on,
 * and the screen stops looking like a list on a void.
 *
 * Built to be cheap enough to leave running:
 *
 *  * **Three radial blobs, no blur.** A radial gradient's falloff is already softer than any blur
 *    kernel, so a RenderEffect here would cost a full-screen pass every frame and change nothing
 *    you can see. The player's backdrop makes the same trade for the same reason.
 *  * **Static.** The blobs deliberately do not drift. A slow wander looks lovely in isolation, but
 *    this sits on Home and Library — the screens the app opens on and returns to — and any
 *    animation here means repainting three full-screen gradients every frame for as long as the
 *    tab is visible, forever, to move something nobody is looking at. The colour changing from
 *    song to song is the part that reads as alive, and that costs one crossfade. The player's
 *    backdrop can afford to drift because you are looking at it and it is on screen for seconds
 *    at a time; this is on screen for the life of the app.
 *  * **Drawn in [drawBehind], reading the crossfade inside the draw lambda.** The one animation
 *    that does run — the fade between songs — invalidates the draw phase only; composition never
 *    runs, so the list scrolling above it is untouched.
 *  * **Nothing playing, nothing drawn.** With no colours the layer returns before allocating a
 *    brush.
 *
 * Each blob is painted across the *whole* surface rather than into a band. Clipping to a band is
 * the obvious way to keep the wash at the top, and it is wrong: wherever a blob's falloff has not
 * finished by the band's edge it gets cut by a straight line, which is precisely the seam the
 * effect exists to avoid. The gradients reach transparent on their own, so position and radius are
 * what confine them — nothing has to be clipped at all.
 */
@Composable
fun AmbientArtworkGlow(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    /** Scales the whole effect. Lower it on surfaces carrying a lot of small text. */
    intensity: Float = 1f,
) {
    // Dark surfaces take colour readily. On a light surface the same alpha turns the top of the
    // screen into a stain, so it is pulled well back.
    val peakAlpha = (if (isSystemInDarkTheme()) 0.42f else 0.18f) * intensity

    // Crossfade between songs — swapping palettes instantly is a visible pop at this size.
    val presence by animateFloatAsState(
        targetValue = if (colors.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "ambient_presence",
    )

    val blobs = remember(colors) { colors.take(3) }

    Box(
        modifier = modifier.drawBehind {
            if (presence <= 0.01f || blobs.isEmpty()) return@drawBehind

            val band = size.height * GlowBandFraction
            val radius = size.width * 0.85f

            blobs.forEachIndexed { index, color ->
                val cx = size.width * (0.24f + 0.26f * index)
                val cy = band * (0.18f + 0.14f * index)

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = peakAlpha * presence),
                            color.copy(alpha = peakAlpha * 0.30f * presence),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                )
            }
        },
    )
}
