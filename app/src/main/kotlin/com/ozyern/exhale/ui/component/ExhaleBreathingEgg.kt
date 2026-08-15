/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ozyern.exhale.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Four seconds in, six seconds out — the ratio that actually slows a heart rate. */
private const val InhaleMillis = 4_000f
private const val ExhaleMillis = 6_000f
private const val CycleMillis = InhaleMillis + ExhaleMillis

private const val MoteCount = 150
private const val RippleLifeMillis = 1_600f
private const val DismissDragPx = 140f

/**
 * The hidden thing behind seven taps on the About hero.
 *
 * The app is called Exhale, so the egg is a breathing pacer — but a pacer you can only watch is a
 * progress bar with better manners, so this one is a field you are inside of. A hundred and fifty
 * motes orbit a core; the inhale draws them in and tightens their orbit, the exhale throws them
 * back out. Three counter-rotating arcs cross the field at different rates, a phase ring closes
 * once per half-breath, and touching anywhere pushes a ripple out from your finger.
 *
 * **Everything is one Canvas and one clock.** A single frame loop writes elapsed milliseconds into
 * a float state that is read *inside* the draw lambda, so a frame costs one draw pass — no
 * recomposition, no layout, no per-mote composable. That is what keeps a hundred and fifty
 * particles plus five gradients at full frame rate instead of a slideshow with good taste.
 *
 * Tap to ripple, swipe down or press back to leave.
 */
@Composable
fun ExhaleBreathingEgg(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val haptics = LocalHapticFeedback.current
        val elapsed = remember { mutableFloatStateOf(0f) }
        val ripples = remember { mutableStateListOf<Ripple>() }
        var inhaling by remember { mutableStateOf(true) }
        var breaths by remember { mutableStateOf(0) }

        // Motes are laid out once. Their orbits are deliberately uneven: an evenly spaced ring
        // reads as a machine part, and the point is breath, not clockwork.
        val motes = remember {
            val random = Random(0x3AB1E)
            List(MoteCount) {
                Mote(
                    angle = random.nextFloat() * TwoPi,
                    orbit = random.nextFloat().let { it * it },
                    radius = 1.1f + random.nextFloat() * 2.6f,
                    speed = (0.10f + random.nextFloat() * 0.32f) *
                        if (random.nextBoolean()) 1f else -1f,
                    wobble = random.nextFloat() * TwoPi,
                )
            }
        }

        LaunchedEffect(Unit) {
            val origin = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    val millis = (now - origin) / 1_000_000f
                    elapsed.floatValue = millis

                    val intoCycle = millis % CycleMillis
                    inhaling = intoCycle < InhaleMillis
                    breaths = (millis / CycleMillis).toInt()

                    if (ripples.isNotEmpty()) {
                        ripples.removeAll { millis - it.bornMillis > RippleLifeMillis }
                    }
                }
            }
        }

        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AuroraBackdrop()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            ripples += Ripple(offset, elapsed.floatValue)
                        }
                    }
                    .pointerInput(Unit) {
                        var travelled = 0f
                        detectVerticalDragGestures(
                            onDragStart = { travelled = 0f },
                            onDragEnd = { if (travelled > DismissDragPx) onDismiss() },
                        ) { _, delta -> travelled += delta }
                    },
            ) {
                val millis = elapsed.floatValue
                val intoCycle = millis % CycleMillis
                val drawingIn = intoCycle < InhaleMillis
                val breath = if (drawingIn) {
                    eased(intoCycle / InhaleMillis)
                } else {
                    1f - eased((intoCycle - InhaleMillis) / ExhaleMillis)
                }
                val phase = if (drawingIn) {
                    intoCycle / InhaleMillis
                } else {
                    (intoCycle - InhaleMillis) / ExhaleMillis
                }

                val middle = center
                val core = size.minDimension * 0.17f * (0.60f + 0.40f * breath)
                val seconds = millis / 1_000f

                drawBloom(middle, core, breath, primary, tertiary)
                drawMotes(motes, middle, core, breath, seconds, primary, secondary, tertiary)
                drawArcs(middle, core, breath, seconds, primary, tertiary)
                drawPhaseRing(middle, core, phase, drawingIn, primary, secondary)
                drawCore(middle, core, breath, primary)
                drawRipples(ripples, millis, primary)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
            ) {
                Crossfade(targetState = inhaling, label = "breathPrompt") { drawingIn ->
                    Text(
                        text = stringResource(
                            if (drawingIn) R.string.egg_breathe_in else R.string.egg_breathe_out
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (breaths == 0) {
                        stringResource(R.string.egg_hint)
                    } else {
                        stringResource(R.string.egg_breath_count, breaths.toString())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.egg_close_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─── Field ────────────────────────────────────────────────────────────────────

private const val TwoPi = (2.0 * PI).toFloat()

/**
 * One orbiting speck. [orbit] is squared at construction so most motes sit close to the core and
 * a few stray far out, which is what makes the field look like dust rather than a dial.
 */
private class Mote(
    val angle: Float,
    val orbit: Float,
    val radius: Float,
    val speed: Float,
    val wobble: Float,
)

private class Ripple(val origin: Offset, val bornMillis: Float)

/** Cosine ease. Breath has no straight lines in it. */
private fun eased(t: Float): Float = (1f - cos(t.coerceIn(0f, 1f) * PI.toFloat())) / 2f

private fun DrawScope.drawBloom(
    middle: Offset,
    core: Float,
    breath: Float,
    primary: Color,
    tertiary: Color,
) {
    val reach = core * (3.4f + 1.1f * breath)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.34f + 0.20f * breath),
                tertiary.copy(alpha = 0.14f + 0.10f * breath),
                Color.Transparent,
            ),
            center = middle,
            radius = reach,
        ),
        radius = reach,
        center = middle,
    )
}

/**
 * The orbit collapses on the inhale and flings open on the exhale, so the field itself breathes
 * rather than merely surrounding something that does.
 */
private fun DrawScope.drawMotes(
    motes: List<Mote>,
    middle: Offset,
    core: Float,
    breath: Float,
    seconds: Float,
    primary: Color,
    secondary: Color,
    tertiary: Color,
) {
    val palette = listOf(primary, secondary, tertiary)
    val spread = core * (1.5f + 3.2f * (1f - breath))

    motes.forEachIndexed { index, mote ->
        val angle = mote.angle + seconds * mote.speed
        val breathing = sin(seconds * 0.9f + mote.wobble) * 0.08f
        val distance = core * 1.15f + spread * (mote.orbit + breathing)

        val position = Offset(
            x = middle.x + distance * cos(angle),
            y = middle.y + distance * sin(angle),
        )
        val twinkle = 0.45f + 0.55f * ((sin(seconds * 2.1f + mote.wobble) + 1f) / 2f)

        drawCircle(
            color = palette[index % palette.size].copy(
                alpha = (0.16f + 0.42f * breath) * twinkle,
            ),
            radius = mote.radius * (0.75f + 0.45f * breath),
            center = position,
        )
    }
}

/** Three counter-rotating sweeps. Their stroke thickens with the inhale. */
private fun DrawScope.drawArcs(
    middle: Offset,
    core: Float,
    breath: Float,
    seconds: Float,
    primary: Color,
    tertiary: Color,
) {
    val bands = listOf(
        Triple(1.75f, 11f, primary),
        Triple(2.25f, -7f, tertiary),
        Triple(2.85f, 4.5f, primary),
    )

    bands.forEachIndexed { index, (scale, degreesPerSecond, tone) ->
        val radius = core * scale
        val sweep = Brush.sweepGradient(
            colors = listOf(
                Color.Transparent,
                tone.copy(alpha = 0.05f + 0.30f * breath),
                Color.Transparent,
                tone.copy(alpha = 0.02f + 0.12f * breath),
                Color.Transparent,
            ),
            center = middle,
        )

        rotate(degrees = seconds * degreesPerSecond + index * 40f, pivot = middle) {
            drawCircle(
                brush = sweep,
                radius = radius,
                center = middle,
                style = Stroke(width = (1.2f + 2.6f * breath) * (3f - index) * density * 0.5f),
            )
        }
    }
}

/**
 * A ring that closes exactly once per half-breath — the only element that says how far through the
 * current phase you are, so the pacer is followable without a number on screen.
 */
private fun DrawScope.drawPhaseRing(
    middle: Offset,
    core: Float,
    phase: Float,
    drawingIn: Boolean,
    primary: Color,
    secondary: Color,
) {
    val radius = core * 3.25f
    val stroke = Stroke(width = 2.5f * density)
    val tone = if (drawingIn) primary else secondary

    drawArc(
        color = tone.copy(alpha = 0.10f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(middle.x - radius, middle.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = stroke,
    )
    drawArc(
        color = tone.copy(alpha = 0.55f),
        startAngle = -90f,
        sweepAngle = 360f * phase.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(middle.x - radius, middle.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = stroke,
    )
}

private fun DrawScope.drawCore(middle: Offset, core: Float, breath: Float, primary: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.60f + 0.30f * breath),
                primary.copy(alpha = 0.72f),
                primary.copy(alpha = 0.18f),
            ),
            center = middle,
            radius = core,
        ),
        radius = core,
        center = middle,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.12f + 0.16f * breath),
        radius = core,
        center = middle,
        style = Stroke(width = 1.4f * density),
    )
}

/** Touch ripples: a ring that widens and thins out, plus a soft wash that fades with it. */
private fun DrawScope.drawRipples(ripples: List<Ripple>, millis: Float, primary: Color) {
    ripples.forEach { ripple ->
        val life = ((millis - ripple.bornMillis) / RippleLifeMillis).coerceIn(0f, 1f)
        val radius = size.minDimension * 0.55f * eased(life)
        val fade = (1f - life)

        drawCircle(
            color = primary.copy(alpha = 0.28f * fade),
            radius = radius,
            center = ripple.origin,
            style = Stroke(width = (5f * fade + 1f) * density),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, primary.copy(alpha = 0.10f * fade)),
                center = ripple.origin,
                radius = radius.coerceAtLeast(1f),
            ),
            radius = radius,
            center = ripple.origin,
        )
    }
}
