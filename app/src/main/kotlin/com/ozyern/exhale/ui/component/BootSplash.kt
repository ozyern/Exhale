/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
 *  2. **Arrival.** The mark fades up and settles from 0.90x on a near-critically-damped spring.
 *     One gesture, no bounce.
 *  3. **Breath.** Through the hold the mark expands by 3.5% on a dead-linear ramp — slow enough
 *     that you never catch it moving, fast enough that the frame is never frozen. The app is
 *     called Exhale; the launch mark should be alive rather than parked.
 *  4. **Iris.** The exit is not a crossfade. A circular hole opens out of the centre of the mark
 *     (`BlendMode.Clear` into an offscreen layer) while the logo itself scales past the camera
 *     and fades — the app is revealed *through* the logo, like an aperture opening. The bloom
 *     goes with it, so the last frames are clean rather than a coloured wash handing over to a
 *     fully drawn app.
 *
 * ### What was taken out
 *
 * Two expanding shockwave rings, a 10° entrance tilt, and a specular band sweeping diagonally
 * across the mark. Each was defensible in isolation; together they were four things competing for
 * attention inside one second, which is what a splash screen looks like when it is trying to
 * impress you. An Apple launch does exactly one thing — the app opens out of its own icon — and
 * the reason it reads as expensive is that nothing else happens at the same time. The aperture is
 * that one thing here, and everything left now serves it.
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
/** How long the mark is on screen before the aperture opens. Short: it is a flourish, not a wait. */
private const val ENTRANCE_MS = 720L
/**
 * The aperture opening. Also the fade, the zoom and the hand-off — one motion on one easing,
 * because two eases running at once is how a single gesture stops reading as single.
 *
 * Longer than the 380ms it was. The iris is the moment the whole animation exists for, and at 380
 * it was over before the eye had followed the edge outward — the extra 120ms is the difference
 * between a cut and an opening.
 */
private const val IRIS_MS = 500

/** Fraction of the shorter viewport edge the square splash artwork occupies. */
private const val SPLASH_ARTWORK_FRACTION = 0.56f

// Brand palette, sampled from the artwork itself rather than guessed. splash_logo.png is a black
// glyph wearing a gold rim light: 72% of the mark is near-black body, its rim averages #DEB41A,
// and it ranges from a #000100 shadow to a #FFF07F highlight. It is a GOLD logo with a dark core.
//
// That split is why the bloom matters more than it looks. On the black canvas the body is
// invisible on its own and only the rim reads — the warm glow behind the mark is what gives the
// body an edge to sit against, so the bloom is doing structural work, not decoration.
//
// (The bloom was once crimson-magenta, #B01E45 over #3A0A1E, a palette belonging to no part of
// this artwork. A gold mark floating in a pink glow is the mismatch; these are its own colours.)
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
    // 0.90, not 0.70. A mark arriving from two thirds of its size has visibly *travelled*, which
    // needs a bounce to land and then reads as a bounce. From 0.90 it simply settles.
    val logoScale = remember { Animatable(0.90f) }
    val logoAlpha = remember { Animatable(0f) }
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

        // --- Arrival, then the breath ---
        launch { logoAlpha.animateTo(1f, tween(durationMillis = 260, easing = LinearOutSlowInEasing)) }
        launch {
            // Damping 0.88: it settles rather than bounces. The old 0.62 gave a visible rebound,
            // which is a *toy* gesture — right for a game splash, wrong for the screen that opens
            // in front of a music library every morning.
            logoScale.animateTo(1f, spring(dampingRatio = 0.88f, stiffness = 300f))
            // Linear on purpose. Any easing has an acceleration you can perceive, and a breath you
            // can perceive is a zoom. You should only notice this one by comparing the first frame
            // of the hold against the last.
            logoScale.animateTo(
                1.035f,
                tween(durationMillis = ENTRANCE_MS.toInt(), easing = LinearEasing),
            )
        }

        delay(ENTRANCE_MS)

        // --- Exit: the aperture opens and the mark flies past the camera ---
        // The breath is still running here; `animateTo` on the same Animatable cancels it and
        // carries on from wherever it had reached, so the hand-off has no seam in it.
        launch {
            logoScale.animateTo(1.38f, tween(durationMillis = IRIS_MS, easing = FastOutSlowInEasing))
        }
        launch { logoAlpha.animateTo(0f, tween(durationMillis = 340, easing = FastOutSlowInEasing)) }
        // The glow leaves with the mark. Left up, it is a warm haze lying over the first frames of
        // a fully drawn app, which is the one thing that can make an otherwise clean hand-off look
        // like a rendering fault.
        launch { bloom.animateTo(0f, tween(durationMillis = 340, easing = FastOutSlowInEasing)) }
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

        // ---- The mark ----
        // The transform lives on this Box rather than on the Image inside it, so the whole thing
        // scales as one rasterised object. No offscreen layer: that was only ever needed to mask
        // the sheen sweep to the artwork's alpha, and the sweep is gone.
        Box(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
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
