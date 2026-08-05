/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ozyern.exhale.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium Apple-style boot animation, drawn as the TOP-MOST layer of the root composition on
 * cold start:
 *
 *  1. A subtle, premium GRADIENT canvas that complements the brand: a deep crimson-magenta
 *     bloom radiating from behind the mark, feathering down into pure black at the edges —
 *     the same palette as the glass Exhale logo, never a flat slab.
 *  2. The glass Exhale orb springs in with a buttery ease-out scale (0.86 → 1.0) while fading
 *     up, the bloom blooming in behind it at the same time.
 *  3. Holds for a beat so the mark registers.
 *  4. The whole layer crossfades away while the logo drifts a hair larger (1.0 → 1.06) — it
 *     reads as the splash MORPHING/zooming into the already-composed Home screen beneath,
 *     never a hard cut.
 *
 * The layer sits above ALL app chrome (nav bar, mini-player, dialogs) and is removed from
 * composition entirely once finished, costing nothing afterwards. rememberSaveable in the host
 * keeps it a cold-start-only moment — rotations/recreations never replay it.
 */

// Launch-speed tuning: long enough to feel like an intentional flourish, short enough that the
// mark never reads as "waiting". Scale-in ≈ 620ms (spring settle), then a held beat, then the
// morph-out crossfade.
private const val LOGO_HOLD_MS = 480L
private const val CROSSFADE_OUT_MS = 440

/** Fraction of the shorter viewport edge the square splash artwork occupies. */
private const val SPLASH_ARTWORK_FRACTION = 0.56f

// Brand palette — the glass "E))" is a neon crimson-magenta on near-black; the bloom mirrors it.
private val SplashBase = Color.Black
private val BloomDeep = Color(0xFFB01E45)   // vivid crimson-magenta core
private val BloomDark = Color(0xFF3A0A1E)   // deep dark magenta mid-tone

@Composable
fun BootSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sized off the SHORTER viewport edge so the square artwork is generous on a tablet, fully
    // un-cropped on a small phone, and safe in landscape — with margin left over for the spring's
    // overshoot and the 1.06x exit drift.
    val configuration = LocalConfiguration.current
    val artworkSize = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        (minOf(configuration.screenWidthDp, configuration.screenHeightDp) * SPLASH_ARTWORK_FRACTION).dp
    }

    var visible by remember { mutableStateOf(true) }
    val logoScale = remember { Animatable(0.86f) }
    val logoAlpha = remember { Animatable(0f) }
    // Drives the gradient bloom fading up behind the mark — kept as its own channel so the
    // background "breathes in" independently of the logo's spring.
    val bloomAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Bloom + logo rise together as one fluid "breathe in" motion.
        launchIntro(logoScale, logoAlpha, bloomAlpha)
        delay(LOGO_HOLD_MS)
        visible = false
        // Morph-out: as the layer crossfades, the mark drifts slightly larger so it feels like
        // it is zooming into Home rather than simply disappearing.
        launch {
            logoScale.animateTo(
                targetValue = 1.06f,
                animationSpec = tween(durationMillis = CROSSFADE_OUT_MS, easing = FastOutSlowInEasing),
            )
        }
        // Let the exit crossfade play out before the host removes this layer.
        delay(CROSSFADE_OUT_MS.toLong() + 40L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = CROSSFADE_OUT_MS, easing = FastOutSlowInEasing)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SplashBase),
            contentAlignment = Alignment.Center,
        ) {
            // ---- Premium gradient bloom (deep crimson-magenta → pure black) ----
            // Radial glow centred behind the mark, feathering to black; a faint vertical wash
            // adds depth from the top. Both ride `bloomAlpha` so they fade up smoothly and never
            // pop against the black system-splash frame that precedes this layer.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bloomAlpha.value }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BloomDark.copy(alpha = 0.55f),
                                SplashBase,
                            ),
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

            // Source: assets/splash.png, mirrored into res/drawable-nodpi/splash_logo.png (and
            // referenced by @drawable/splash_logo_inset for the Android 12+ system splash, so the
            // OS frame and this Compose frame show the SAME artwork — one seamless boot screen).
            //
            // Sized as a fraction of the container instead of a fixed dp: the artwork is a square
            // 1024x1024, so 56% of the shorter edge keeps it generous on a tablet and un-cropped on
            // a small phone, while the spring's overshoot and the 1.06x exit drift both stay inside
            // the remaining margin. ContentScale.Fit guarantees the mark is never cropped.
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(artworkSize)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    },
            )
        }
    }
}

private suspend fun launchIntro(
    scale: Animatable<Float, *>,
    alpha: Animatable<Float, *>,
    bloom: Animatable<Float, *>,
) {
    // Buttery, unhurried entrance: alpha and the bloom ramp on smooth ease-out curves while a
    // low-stiffness, well-damped spring settles the scale with only the faintest overshoot — the
    // three together read as a single fluid "breathe in".
    coroutineScope {
        launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 560, easing = LinearOutSlowInEasing))
        }
        launch {
            bloom.animateTo(1f, animationSpec = tween(durationMillis = 720, easing = LinearOutSlowInEasing))
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 180f,
                ),
            )
        }
    }
}
