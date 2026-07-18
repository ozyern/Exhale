/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ozyern.exhale.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium Apple-style boot animation, drawn as the TOP-MOST layer of the root
 * composition on cold start:
 *
 *  1. Pure-black canvas — no chrome, no gradients, nothing to distract from the mark.
 *  2. The glass Exhale orb (its baked glow feathered to transparent, so it melts into
 *     the black) springs in with a soft ease-out scale (0.72 → 1.0) while fading up.
 *  3. Holds for a brief beat.
 *  4. The whole layer crossfades away, revealing the already-composed Home screen
 *     beneath it — a seamless transition, never a hard cut.
 *
 * The layer sits above ALL app chrome (nav bar, mini-player, dialogs) and is removed
 * from composition entirely once finished, costing nothing afterwards.
 */
// Launch-speed tuning: the hold is just long enough to register the mark before the
// crossfade reveals Home — the splash reads as a flourish, never as waiting.
private const val LOGO_HOLD_MS = 350L
private const val CROSSFADE_OUT_MS = 350

/** Pure black — matches the window background so system splash → Compose splash is one frame. */
private val SplashCanvasColor = Color.Black

@Composable
fun BootSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(true) }
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Scale-in with a soft spring settle; alpha ramps alongside.
        launchLogoIn(logoScale, logoAlpha)
        delay(LOGO_HOLD_MS)
        visible = false
        // Let the exit crossfade play out before the host removes this layer.
        delay(CROSSFADE_OUT_MS.toLong() + 50L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = CROSSFADE_OUT_MS)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SplashCanvasColor),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    // Down from 220dp: a fixed 172dp orb matches the inset system splash
                    // icon so the OS→Compose handoff is size-continuous, and even with the
                    // spring's slight scale overshoot the artwork stays fully on-screen on
                    // every device — no more cropped edges.
                    .size(172.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    },
            )
        }
    }
}

private suspend fun launchLogoIn(
    scale: Animatable<Float, *>,
    alpha: Animatable<Float, *>,
) {
    // Run alpha quickly to full while the spring settles the scale — the two together
    // read as one fluid "breathe in" motion. dampingRatio 0.8 gives the faintest
    // overshoot; stiffness 300 keeps it unhurried but never sluggish.
    coroutineScope {
        launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 260))
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f,
                ),
            )
        }
    }
}
