/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ozyern.exhale.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/**
 * The cold-start boot animation, drawn as the TOP-MOST layer of the root composition.
 *
 * The beats, in order:
 *
 *  1. **Bloom.** A warm amber glow swells out of pure black behind the mark. It starts on
 *     frame one — before the logo is even decoded — so the screen is never a dead black slab.
 *  2. **Impact.** The mark springs in from 0.70x with a live overshoot and unwinds a 10° tilt,
 *     while two shockwave rings expand out from underneath it and dissolve.
 *  3. **Sheen.** A specular highlight sweeps diagonally across the glass mark — masked to the
 *     artwork's own alpha (`BlendMode.SrcAtop`), so it glints off the logo rather than smearing
 *     a band across the canvas.
 *  4. **Iris.** The exit is not a crossfade. A circular hole opens out of the centre of the mark
 *     (`BlendMode.Clear` into an offscreen layer) while the logo itself scales past the camera
 *     and fades — the app is revealed *through* the logo, like an aperture opening.
 *
 * ### Why it used to be slow
 *
 * Two separate causes, both fixed here:
 *
 *  - **~1.8s of mandatory animation.** The old timeline waited for the *slowest* of three intro
 *    tweens (720ms) to fully settle, then held 480ms, then crossfaded 440ms + 40ms of slack.
 *    That is the whole of it spent staring at a static mark. The budget below is ~1.16s, and
 *    every phase is doing something.
 *  - **A main-thread image decode at the worst possible moment.** `splash_logo.png` is a
 *    1024x1024 / 1.4MB PNG; `painterResource` decodes it *synchronously, on the main thread,
 *    during composition* — 4MB of ARGB_8888 allocated on the exact frame the whole app is also
 *    composing its first screen. That was the stutter. It is now decoded on [Dispatchers.IO] and
 *    the timeline simply starts when it lands (single-digit-to-low-tens of milliseconds later),
 *    with the bloom already on screen covering the gap.
 *
 * Everything animated here is read inside `graphicsLayer` / draw lambdas, so the whole sequence
 * runs in the draw phase — it never triggers a recomposition or a relayout while the app behind
 * it is doing its expensive first composition.
 *
 * The layer is removed from composition entirely once finished, costing nothing afterwards.
 * `rememberSaveable` in the host keeps it a cold-start-only moment; rotations never replay it.
 */

// ---- Timeline (ms) -------------------------------------------------------------------------
/** When the sheen starts its sweep, measured from the logo's entrance. */
private const val SHEEN_START_MS = 260L
private const val SHEEN_MS = 540
/** How long the mark is on screen before the aperture opens. Short: it is a flourish, not a wait. */
private const val ENTRANCE_MS = 760L
/** The aperture opening. Also the crossfade, the zoom, and the hand-off — all one motion. */
private const val IRIS_MS = 380

/** Fraction of the shorter viewport edge the square splash artwork occupies. */
private const val SPLASH_ARTWORK_FRACTION = 0.56f

// Brand palette, sampled from the artwork itself rather than guessed: the mean colour of every
// opaque pixel in splash_logo.png is #BE851A, a rich amber, and the mark ranges from a #622B00
// shadow to a #FFF234 highlight. It is a GOLD logo.
//
// The bloom used to be crimson-magenta (#B01E45 over #3A0A1E) — a palette belonging to no part of
// this artwork. A gold mark floating in a pink glow is the mismatch; these are its own colours.
private val SplashBase = Color.Black
private val BloomDeep = Color(0xFFE0A020)   // warm amber core, the mark's mid-gold pushed brighter
private val BloomDark = Color(0xFF33200A)   // deep brown-amber mid-tone, the mark's own shadow

@Composable
fun BootSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sized off the SHORTER viewport edge so the square artwork is generous on a tablet, fully
    // un-cropped on a small phone, and safe in landscape — with margin left over for the spring's
    // overshoot and the exit zoom.
    val configuration = LocalConfiguration.current
    val artworkSize = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        (minOf(configuration.screenWidthDp, configuration.screenHeightDp) * SPLASH_ARTWORK_FRACTION).dp
    }

    val context = LocalContext.current
    var logo by remember { mutableStateOf<ImageBitmap?>(null) }
    var decodeFailed by remember { mutableStateOf(false) }

    val bloom = remember { Animatable(0f) }
    val ringA = remember { Animatable(0f) }
    val ringB = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.70f) }
    val logoRotation = remember { Animatable(-10f) }
    val logoAlpha = remember { Animatable(0f) }
    val sheen = remember { Animatable(0f) }
    val iris = remember { Animatable(0f) }

    // Frame one: the bloom is already breathing in while the artwork is still being decoded on a
    // background thread. The user never sees an empty black hold.
    LaunchedEffect(Unit) {
        launch {
            bloom.animateTo(1f, tween(durationMillis = 380, easing = LinearOutSlowInEasing))
        }
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.splash_logo)
                    ?.asImageBitmap()
            }.getOrNull()
        }
        if (decoded != null) logo = decoded else decodeFailed = true
    }

    val ready = logo != null || decodeFailed

    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect

        // --- Entrance: one impact, four channels ---
        launch {
            // Low damping, high stiffness: a real overshoot with a crisp settle. This is the beat
            // the whole animation is built around, so it is allowed to be lively.
            logoScale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 340f))
        }
        launch { logoRotation.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 340f)) }
        launch { logoAlpha.animateTo(1f, tween(durationMillis = 220, easing = LinearOutSlowInEasing)) }
        launch {
            delay(60)
            ringA.animateTo(1f, tween(durationMillis = 760, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(180)
            ringB.animateTo(1f, tween(durationMillis = 760, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(SHEEN_START_MS)
            sheen.animateTo(1f, tween(durationMillis = SHEEN_MS, easing = FastOutSlowInEasing))
        }

        delay(ENTRANCE_MS)

        // --- Exit: the aperture opens and the mark flies past the camera ---
        launch {
            logoScale.animateTo(1.45f, tween(durationMillis = IRIS_MS, easing = FastOutSlowInEasing))
        }
        launch { logoAlpha.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing)) }
        iris.animateTo(1f, tween(durationMillis = IRIS_MS, easing = FastOutSlowInEasing))

        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Offscreen compositing is what makes the aperture possible: BlendMode.Clear can only
            // punch a true hole through pixels that live in their own layer. Without this, Clear
            // would blend against the window and paint black instead of revealing the app.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val progress = iris.value
                if (progress > 0f) {
                    // Grows from the mark's own radius out past the far corners, so the last frame
                    // of the splash is genuinely empty and the hand-off has nothing left to hide.
                    val start = artworkSize.toPx() * 0.30f
                    val end = hypot(size.width, size.height) * 0.52f
                    drawCircle(
                        color = Color.Black,
                        radius = lerp(start, end, progress),
                        center = center,
                        blendMode = BlendMode.Clear,
                    )
                }
            }
            .background(SplashBase),
        contentAlignment = Alignment.Center,
    ) {
        // ---- Gradient bloom (deep crimson-magenta -> pure black) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bloom.value }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BloomDark.copy(alpha = 0.55f), SplashBase),
                    ),
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BloomDeep.copy(alpha = 0.60f),
                            BloomDark.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        center = Offset.Unspecified,
                        radius = Float.POSITIVE_INFINITY,
                    ),
                ),
        )

        // ---- Shockwave rings ----
        // Two stroked circles expanding out from under the mark and thinning as they dissolve.
        // Reading the animatables inside the draw lambda keeps this a draw-phase-only animation.
        Canvas(Modifier.fillMaxSize()) {
            val from = artworkSize.toPx() * 0.42f
            val to = size.maxDimension * 0.62f
            listOf(ringA.value, ringB.value).forEach { progress ->
                if (progress <= 0f || progress >= 1f) return@forEach
                val fade = (1f - progress)
                drawCircle(
                    color = BloomDeep.copy(alpha = 0.40f * fade * fade),
                    radius = lerp(from, to, progress),
                    center = center,
                    style = Stroke(width = lerp(5f.dp.toPx(), 0.8f.dp.toPx(), progress)),
                )
            }
        }

        // ---- The mark ----
        // Its own offscreen layer so the sheen can be masked to the artwork's alpha.
        //
        // The entrance transform lives on THIS layer, not on the Image inside it: an offscreen
        // layer clips its content to its bounds, so a rotated/1.45x-scaled child would have its
        // corners sliced off. Transforming the layer itself happens after rasterisation, so the
        // mark scales and tilts freely — and the sheen tilts with it, as a real highlight would.
        Box(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    rotationZ = logoRotation.value
                    alpha = logoAlpha.value
                }
                .drawWithContent {
                    drawContent()
                    val progress = sheen.value
                    if (progress <= 0f || progress >= 1f) return@drawWithContent
                    val band = size.width * 0.55f
                    val travel = lerp(-band, size.width + band, progress)
                    // SrcAtop, not SrcIn: SrcIn would erase every pixel the gradient does not
                    // cover, i.e. the entire logo except the band. SrcATop keeps the mark and
                    // lays the highlight on top of it only where the mark already is.
                    drawRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to Color.White.copy(alpha = 0.55f),
                                1f to Color.Transparent,
                            ),
                            start = Offset(travel, 0f),
                            end = Offset(travel + band, size.height),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val markModifier = Modifier.fillMaxSize()

            val bitmap = logo
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = markModifier,
                )
            } else if (decodeFailed) {
                // Belt and braces: if the background decode ever fails, fall back to the
                // synchronous resource path rather than booting into a logo-less splash.
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.splash_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = markModifier,
                )
            }
        }
    }
}
