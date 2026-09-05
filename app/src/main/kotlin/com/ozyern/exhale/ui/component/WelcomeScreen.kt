/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ozyern.exhale.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/* ------------------------------------------------------------------------------------------- */
/* The beats                                                                                    */
/* ------------------------------------------------------------------------------------------- */

/**
 * How long the stars take to fade up out of black, scattered across the whole screen.
 *
 * Nothing moves during this. The scatter has to register as a state of its own before anything
 * travels, or the number reads as fading in rather than as being assembled.
 */
private const val WFADE = 0.75f

/** The pause at the end of the fade, for the same reason. */
private const val WHOLD = 1.30f

/** How long one star takes to travel from where it was waiting to where it belongs. */
private const val WASSEMBLE = 2.55f

/**
 * Extra seconds spread across the field by radius, so it settles middle-outward.
 *
 * A field that lands all at once is a cross-fade. One that arrives from the inside out is a thing
 * coming together, and it is the same trick the announcement page's hero uses.
 */
private const val WSPREAD = 0.55f

/** The moment the figure is complete, and the moment it comes apart. */
private const val WFORMED = WHOLD + WASSEMBLE + WSPREAD
private const val WBURST = WFORMED + 0.95f

/** How long the debris takes to leave. */
private const val WBURST_LEN = 1.55f

/** When the word starts being written. Inside the burst, so the two overlap rather than queue. */
private const val WWELCOME = WBURST + 0.42f

/** How long the hand takes to get through it. */
private const val WWRITE = 1.35f

/* ------------------------------------------------------------------------------------------- */
/* The field                                                                                    */
/* ------------------------------------------------------------------------------------------- */

/** How many stars the number is made of. Well under the website's: this is a phone. */
private const val WCOUNT = 1000

/** Large faint smears under it, on the same points, so the stars sit in haze and not in a row. */
private const val WNEBULA = 120

/** Points forced to maximum magnitude, spread along the spines. */
private const val WHOTSPOTS = 18

/** And a handful far beyond even those: the things the eye finds first. */
private const val WGIANTS = 5

/** Background dust, scattered over the frame rather than over the figure. */
private const val WDUST = 130

/** The mask the glyphs are rasterised into. Fixed, so the arrangement never depends on the screen. */
private const val WMASK_W = 1100
private const val WMASK_H = 430

/** Sprite size. One texture per tint, blitted a few thousand times a frame. */
private const val WSPRITE = 64

private fun wsmoother(x: Float): Float = x * x * x * (x * (x * 6f - 15f) + 10f)

private fun wclamp01(x: Float): Float = if (x < 0f) 0f else if (x > 1f) 1f else x

private fun wsmoothstep(x: Float): Float = wclamp01(x).let { it * it * (3f - 2f * it) }

private fun fpow(x: Float, e: Float): Float = Math.pow(x.toDouble(), e.toDouble()).toFloat()

/**
 * One star.
 *
 * A class per star rather than a packed FloatArray because there are a thousand of them, not a
 * million, and the draw loop does the same handful of arithmetic ops per field either way. The
 * readable version is the one that can still be corrected a year from now.
 */
private class Star(
    /** Home, normalised against the ink's own bounding box. */
    val x: Float,
    val y: Float,
    /** 0 at the very edge of a stroke, 1 down its middle. */
    val depth: Float,
    /** How bright this one is, on the cube law. Read by the bloom, which scales harder than size. */
    val mag: Float,
    /** Which way the stroke runs here — the bloom is stretched along it. */
    val tx: Float,
    val ty: Float,
    /** Where it waits before it is called in, in frame coordinates. */
    val sx: Float,
    val sy: Float,
    /** Its head start, by distance from the middle. */
    val delay: Float,
    val tint: Int,
    var size: Float,
    var glow: Float,
    val twRate: Float,
    val twDepth: Float,
    val phase: Float,
    /** Where it goes when the figure comes apart, and how fast. */
    val burstX: Float,
    val burstY: Float,
    val burstSpeed: Float,
    var giant: Boolean = false,
)

/** Everything the field needs that does not depend on the size of the window. */
private class StarNumber(
    val motes: List<Star>,
    val nebula: List<Star>,
    val dust: List<Star>,
    /** How tall the ink is per unit of its width, so the figure is fitted without guessing. */
    val ratio: Float,
    /** Characters on the line. It is what says how wide one stroke is relative to the figure. */
    val perLine: Int,
)

/**
 * The number, as points.
 *
 * A port of the announcement page's hero (`website/src/components/BreathField.jsx`) — the same
 * chamfer distance transform, the same ridge-weighted sampling, the same tangents — because the
 * number in the app and the number on the page should be recognisably the same object. What is not
 * ported is the breathing loop: this one has somewhere to be.
 *
 * The short version of why it is built this way: filling a letterform evenly with dots gives you
 * dots in the shape of a letter. A distance transform says how deep inside the stroke each pixel
 * is; concentrating the sampling on the ridge turns every stroke into a chain rather than a filled
 * region, and stretching each star's bloom along the local stroke direction lets neighbours run
 * together into a ribbon instead of staying a row of separate points.
 */
private fun buildStarNumber(text: String, random: Random): StarNumber {
    // ---- 1. rasterise ------------------------------------------------------------------
    val bitmap = Bitmap.createBitmap(WMASK_W, WMASK_H, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        // Tracked out, because three digits set solid become one cloud with two notches in it.
        letterSpacing = 0.08f
    }

    // Fitted rather than guessed: the number changes length between releases and this should not
    // need a new magic number when it does.
    paint.textSize = 340f
    val measured = paint.measureText(text).coerceAtLeast(1f)
    paint.textSize = 340f * min(1f, WMASK_W * 0.86f / measured)
    val metrics = paint.fontMetrics
    canvas.drawText(text, WMASK_W / 2f, WMASK_H / 2f - (metrics.ascent + metrics.descent) / 2f, paint)

    val pixels = IntArray(WMASK_W * WMASK_H)
    bitmap.getPixels(pixels, 0, WMASK_W, 0, 0, WMASK_W, WMASK_H)
    bitmap.recycle()

    // ---- 2. how deep inside the stroke every pixel is ----------------------------------
    //
    // A two-pass chamfer transform: background starts at zero, ink starts at infinity, and each
    // pass takes the cheapest route to a background pixel from the three neighbours behind it.
    // Weights of 3 orthogonally and 4 diagonally approximate a Euclidean distance closely enough
    // for something that is about to be scattered anyway, and it is two linear passes rather than
    // a search per pixel.
    val n = WMASK_W * WMASK_H
    val dist = IntArray(n)
    val big = 1 shl 28
    for (i in 0 until n) dist[i] = if ((pixels[i] ushr 24) > 140) big else 0

    for (y in 1 until WMASK_H) {
        val row = y * WMASK_W
        for (x in 1 until WMASK_W - 1) {
            val i = row + x
            if (dist[i] == 0) continue
            val up = i - WMASK_W
            var best = dist[i - 1] + 3
            if (dist[up] + 3 < best) best = dist[up] + 3
            if (dist[up - 1] + 4 < best) best = dist[up - 1] + 4
            if (dist[up + 1] + 4 < best) best = dist[up + 1] + 4
            if (best < dist[i]) dist[i] = best
        }
    }
    for (y in WMASK_H - 2 downTo 0) {
        val row = y * WMASK_W
        for (x in WMASK_W - 2 downTo 1) {
            val i = row + x
            if (dist[i] == 0) continue
            val down = i + WMASK_W
            var best = dist[i + 1] + 3
            if (dist[down] + 3 < best) best = dist[down] + 3
            if (dist[down + 1] + 4 < best) best = dist[down + 1] + 4
            if (dist[down - 1] + 4 < best) best = dist[down - 1] + 4
            if (best < dist[i]) dist[i] = best
        }
    }

    var deepest = 1
    for (i in 0 until n) if (dist[i] < big && dist[i] > deepest) deepest = dist[i]

    // ---- 3. sample ---------------------------------------------------------------------
    fun gauss(): Float =
        sqrt(-2f * ln(random.nextFloat().coerceAtLeast(1e-6f))) *
            cos(2f * Math.PI.toFloat() * random.nextFloat())

    val hx = ArrayList<Float>(4096)
    val hy = ArrayList<Float>(4096)
    val hd = ArrayList<Float>(4096)
    val htx = ArrayList<Float>(4096)
    val hty = ArrayList<Float>(4096)
    var minX = WMASK_W.toFloat()
    var maxX = 0f
    var minY = WMASK_H.toFloat()
    var maxY = 0f

    for (y in 1 until WMASK_H - 1) {
        for (x in 1 until WMASK_W - 1) {
            val i = y * WMASK_W + x
            if (dist[i] == 0) continue

            val depth = min(1f, dist[i].toFloat() / deepest)
            // Concentrated hard on the ridge. A near-fourth power leaves the rim almost empty and
            // piles nearly everything onto the middle of the stroke. The few percent that do
            // survive out at the edge are what keeps the outline from looking cut with a knife.
            if (random.nextFloat() > 0.025f + 0.975f * fpow(depth, 3.6f)) continue

            // The distance field climbs fastest straight across a stroke, so its gradient is the
            // normal and the perpendicular is the tangent.
            val gx = (dist[i + 1] - dist[i - 1]).toFloat()
            val gy = (dist[i + WMASK_W] - dist[i - WMASK_W]).toFloat()
            val len = hypot(gx, gy).coerceAtLeast(1f)

            hx.add(x + gauss() * 1.5f)
            hy.add(y + gauss() * 1.5f)
            hd.add(depth)
            htx.add(-gy / len)
            hty.add(gx / len)

            if (x < minX) minX = x.toFloat()
            if (x > maxX) maxX = x.toFloat()
            if (y < minY) minY = y.toFloat()
            if (y > maxY) maxY = y.toFloat()
        }
    }

    val bw = (maxX - minX).coerceAtLeast(1f)
    val bh = (maxY - minY).coerceAtLeast(1f)
    val midX = (minX + maxX) / 2f
    val midY = (minY + maxY) / 2f

    // Fisher-Yates, so thinning takes points from all over the number rather than lopping the
    // bottom off it.
    val order = MutableList(hx.size) { it }
    for (i in order.indices.reversed()) {
        val j = random.nextInt(i + 1)
        val t = order[i]; order[i] = order[j]; order[j] = t
    }

    fun mote(index: Int, forNebula: Boolean): Star {
        // Divided by the same number in both axes, so the number is never stretched.
        val nx = (hx[index] - midX) / bw
        val ny = (hy[index] - midY) / bw
        val depth = hd[index]

        // Magnitude as a cube law: a handful of bright things and hundreds you can barely see. An
        // even scatter of mid-size dots reads as confetti, which is the single thing that makes an
        // effect like this look like a placeholder.
        val mag = fpow(random.nextFloat(), 3.1f)
        val clump = 0.5f + 0.25f * sin(nx * 21.3f + ny * 13.7f) +
            0.25f * sin(nx * 41.1f - ny * 31.9f + 1.7f)

        val angle = atan2(ny, nx) + (random.nextFloat() - 0.5f) * 0.6f
        val reach = hypot(nx, ny)

        return Star(
            x = nx,
            y = ny,
            depth = depth,
            mag = mag,
            tx = htx[index],
            ty = hty[index],
            sx = random.nextFloat(),
            sy = random.nextFloat(),
            delay = min(1f, reach / 0.55f) * WSPREAD,
            tint = when {
                random.nextFloat() < 0.44f -> 0
                random.nextFloat() < 0.5f -> 1
                random.nextFloat() < 0.6f -> 2
                else -> 3
            },
            size = if (forNebula) {
                34f + fpow(random.nextFloat(), 2f) * 108f
            } else {
                (0.5f + mag * 10f) * (0.62f + clump * 0.95f)
            },
            glow = if (forNebula) {
                (0.024f + fpow(random.nextFloat(), 2f) * 0.05f) * (0.35f + depth * 0.9f)
            } else {
                (0.26f + mag * 1.05f) * (0.55f + clump * 0.68f) * (0.5f + depth * 0.72f)
            },
            twRate = 0.5f + random.nextFloat() * 2.1f,
            twDepth = 0.05f + fpow(random.nextFloat(), 2.4f) * 0.42f,
            phase = random.nextFloat() * 2f * Math.PI.toFloat(),
            // Outward from the middle, roughly. The jitter is what stops the debris looking like a
            // diagram of a starburst; the speed spread is what gives it a leading edge.
            burstX = cos(angle),
            burstY = sin(angle),
            burstSpeed = 0.55f + random.nextFloat() * 1.5f + reach * 0.7f,
        )
    }

    val motes = ArrayList<Star>(WCOUNT)
    for (k in 0 until min(WCOUNT, order.size)) motes.add(mote(order[k], false))

    // The bright ones. Spaced, so they are not all in the same stroke.
    val chosen = ArrayList<Star>(WHOTSPOTS)
    var tries = 0
    while (chosen.size < WHOTSPOTS && tries < 800 && motes.isNotEmpty()) {
        tries += 1
        val m = motes[random.nextInt(motes.size)]
        if (m.depth < 0.55f) continue
        if (chosen.any { hypot(it.x - m.x, it.y - m.y) < 0.075f }) continue
        m.size = 5.5f + random.nextFloat() * 3.5f
        m.glow = 0.95f
        chosen.add(m)
    }

    // And a few beyond even those. The eye finds the brightest thing in a frame first and
    // everything else second; without something to be found first, a field of near-equal stars is
    // a texture rather than a sky.
    var giants = 0
    tries = 0
    while (giants < WGIANTS && tries < 400 && chosen.isNotEmpty()) {
        tries += 1
        val m = chosen[random.nextInt(chosen.size)]
        if (m.giant) continue
        if (chosen.any { it.giant && hypot(it.x - m.x, it.y - m.y) < 0.2f }) continue
        m.giant = true
        m.size = 12f + random.nextFloat() * 6f
        m.glow = 1f
        giants += 1
    }

    val nebula = ArrayList<Star>(WNEBULA)
    var guard = 0
    while (nebula.size < WNEBULA && guard < WNEBULA * 20 && order.isNotEmpty()) {
        guard += 1
        val index = order[random.nextInt(order.size)]
        if (hd[index] < 0.5f) continue
        nebula.add(mote(index, true))
    }

    // The sky behind, in frame coordinates: it is not part of the number and must not move or
    // resize with it.
    val dust = List(WDUST) {
        val mag = fpow(random.nextFloat(), 3.4f)
        Star(
            x = random.nextFloat(), y = random.nextFloat(), depth = 1f, mag = mag,
            tx = 0f, ty = 0f, sx = 0f, sy = 0f, delay = 0f,
            tint = if (random.nextFloat() < 0.8f) 0 else random.nextInt(4),
            size = 0.55f + mag * 3.4f,
            glow = 0.05f + mag * 0.5f,
            twRate = 0.3f + random.nextFloat() * 0.8f,
            twDepth = 0.25f,
            phase = random.nextFloat() * 2f * Math.PI.toFloat(),
            burstX = 0f, burstY = 0f, burstSpeed = 0f,
        )
    }

    return StarNumber(motes, nebula, dust, bh / bw, text.length.coerceAtLeast(1))
}

/**
 * A star, pre-rendered once and blitted a few thousand times a frame.
 *
 * Two per tint: a sharp core and a wide faint halo. Building a radial gradient per star per frame
 * is the version of this that drops frames on a phone; drawing a texture is the version that does
 * not, and at this size nobody can tell the difference.
 */
private fun starSprite(color: Int, core: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(WSPRITE, WSPRITE, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val r = WSPRITE / 2f
    val a = { f: Float -> (color and 0x00FFFFFF) or (((255 * f).toInt().coerceIn(0, 255)) shl 24) }

    val shader = if (core) {
        RadialGradient(
            r, r, r,
            intArrayOf(android.graphics.Color.WHITE, a(1f), a(0.34f), a(0f)),
            floatArrayOf(0f, 0.16f, 0.34f, 1f),
            Shader.TileMode.CLAMP,
        )
    } else {
        RadialGradient(
            r, r, r,
            intArrayOf(a(0.55f), a(0.12f), a(0f)),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawCircle(r, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
    return bmp
}

/* ------------------------------------------------------------------------------------------- */
/* The screen                                                                                   */
/* ------------------------------------------------------------------------------------------- */

/**
 * The one-time screen an upgrade lands on.
 *
 * Modelled on the screen Apple shows after a major update — a soft field of colour, one word, one
 * button — with the opening replaced by the thing the release is actually about: the build number
 * assembles itself out of a scattered star field, exactly as it does at the top of the
 * announcement page, holds for a beat, and then comes apart. The colour left behind is what the
 * debris turns into, so the two halves are one event rather than a slide and then another slide.
 *
 * Shown once, ever, per upgrade — see the gate in MainActivity. A build that has never recorded a
 * version code and has never been updated is a fresh install, and a fresh install gets the
 * first-run setup instead: being welcomed *back* to an app you have never opened is worse than not
 * being welcomed at all.
 *
 * @param version the version name to name, e.g. "1.0.203".
 * @param figure what the stars spell. The build number, short enough to read as one shape.
 */
@Composable
fun WelcomeScreen(
    version: String,
    figure: String,
    onDismiss: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    // Built off the main thread: the transform is half a million pixels twice over, and this
    // screen appears on the frame after a cold start.
    var field by remember { mutableStateOf<StarNumber?>(null) }
    LaunchedEffect(figure) {
        field = withContext(Dispatchers.Default) { buildStarNumber(figure, Random(figure.hashCode())) }
    }

    val sprites = remember(accent) {
        val tints = intArrayOf(
            0xFFEEF3FF.toInt(),
            0xFFB6CDFF.toInt(),
            0xFFFFE3BB.toInt(),
            accent.toArgb(),
        )
        Array(tints.size) { i -> arrayOf(starSprite(tints[i], true), starSprite(tints[i], false)) }
    }

    // The word, and the tape measure that runs along it. Both built once: `getSegment` needs an
    // arc-length parameterisation of the whole path, and rebuilding that every frame is the
    // expensive way to draw the same curve.
    val wordPath = remember { welcomePath() }
    val wordMeasure = remember(wordPath) { PathMeasure().apply { setPath(wordPath, false) } }
    val wordScratch = remember { Path() }

    // One clock for the whole screen, in seconds. Written from the frame callback and read only
    // inside draw lambdas, so a frame of this costs a layer invalidation and neither a
    // recomposition nor a relayout.
    val clock = remember { mutableFloatStateOf(0f) }
    var arrived by remember { mutableStateOf(false) }

    LaunchedEffect(field) {
        if (field == null) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            // Clamped, so a dropped frame or a debugger pause advances the story by a frame rather
            // than teleporting past it.
            clock.floatValue += ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            // The button waits for the hand to finish. Offering someone a way out of a sentence
            // that is still being written is the fastest way to make sure nobody reads it.
            if (!arrived && clock.floatValue >= WWELCOME + WWRITE * 0.8f) arrived = true
        }
    }

    val wordIn by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "welcomeWord",
    )
    val buttonIn by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
        label = "welcomeButton",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                // A tap before the burst does not skip the screen — it skips to the part worth
                // seeing. Nobody who taps here wants to miss the explosion; they want the four
                // seconds of assembly they have already watched once.
                if (clock.floatValue < WBURST) clock.floatValue = WBURST
            }
            // Order matters. The colour is under the field, so the debris crosses it.
            .drawBehind { drawWelcomeAurora(clock.floatValue, accent) }
            .drawBehind { drawStarNumber(clock.floatValue, field, sprites) },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val spoken = stringResource(R.string.welcome_title)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    // The ratio of the *control-point* box, not of the ink: `Path.getBounds`
                    // returns the former on Android — the `exact` flag has been ignored for as
                    // long as the method has existed — and the fit inside `drawWelcomeWord` is
                    // measured against the same thing. Handing it the ink's 3.67 instead makes
                    // the fit height-bound and the word quietly small.
                    .aspectRatio(3.23f)
                    .semantics { contentDescription = spoken },
            ) {
                // Read straight off the clock rather than from an animation of its own: the pen
                // has to be somewhere specific at every instant, and a spring would put it there
                // approximately.
                drawWelcomeWord(
                    path = wordPath,
                    measure = wordMeasure,
                    scratch = wordScratch,
                    progress = wclamp01((clock.floatValue - WWELCOME) / WWRITE),
                    accent = accent,
                )
            }

            Text(
                text = stringResource(R.string.welcome_version, version),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .graphicsLayer {
                        alpha = wordIn
                        translationY = (1f - wordIn) * 12.dp.toPx()
                    },
            )
        }

        // The app's own glass, not a white rectangle at 17%.
        //
        // `liquidGlassSurface` is the recipe the shortcut tiles and the Sound Chem deck are made
        // of — a vertical fill, a diagonal sheen and a rim that fades down the same axis — and it
        // is self-contained, so unlike the dock's chrome glass it does not need a backdrop layer
        // to sample. Over the aurora, which is all gradient and no detail, there is nothing a real
        // blur would find that this does not already give.
        Text(
            text = stringResource(R.string.welcome_get_started),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .graphicsLayer {
                    alpha = buttonIn
                    translationY = (1f - buttonIn) * 34.dp.toPx()
                }
                .liquidGlassSurface(RoundedCornerShape(percent = 50))
                .clickable(enabled = arrived) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDismiss()
                }
                .padding(vertical = 18.dp),
        )
    }
}

/* ------------------------------------------------------------------------------------------- */
/* The word                                                                                     */
/* ------------------------------------------------------------------------------------------- */

/**
 * "welcome", as one unbroken cursive stroke.
 *
 * Drawn rather than set, and the reason is that there is no cursive to set it in. The app bundles
 * four faces and none of them is a script; `FontFamily.Cursive` resolves to whatever the ROM calls
 * "cursive", which is Dancing Script on AOSP and quietly nothing at all on a good number of OEM
 * builds — so half the people who see this screen would get the word in the same sans as the
 * version line under it, on the one screen where that is the entire point.
 *
 * A path has no such failure mode, and it buys the better moment as well: a stroke can be *drawn
 * on*, so the word is written in front of you instead of fading up. See [drawWelcomeWord].
 *
 * ### The coordinate system
 *
 * x runs right, y runs **down**, the baseline is y = 0, the x-height is y = -1 and the ascender is
 * y = -2. Every letter is authored to enter and leave on the connector line at y = -0.35, which is
 * what lets the whole word be one path with no special cases at the joins: a letter's exit *is*
 * the next letter's entry.
 *
 * Each row is `endX, endY, c1x, c1y, c2x, c2y` — one cubic, relative to the letter's own origin.
 * The shapes were authored against a renderer that draws exactly these curves, because cursive is
 * one of the things you cannot check by reading the numbers.
 */
private val WELCOME_ADVANCE = mapOf(
    'w' to 1.50f, 'e' to 0.74f, 'l' to 0.68f, 'c' to 0.66f, 'o' to 0.70f, 'm' to 1.52f,
)

private val WELCOME_STROKES = mapOf(
    // Pointed at the top *and* the bottom. That is the whole difference between a w and an m, and
    // getting it wrong is why the first pass of this read as "mvelcome".
    'w' to floatArrayOf(
        0.26f, -1.00f, 0.06f, -0.66f, 0.23f, -0.84f,
        0.46f, -0.02f, 0.29f, -0.84f, 0.44f, -0.32f,
        0.72f, -1.00f, 0.48f, -0.32f, 0.69f, -0.84f,
        0.92f, -0.02f, 0.75f, -0.84f, 0.90f, -0.32f,
        1.18f, -1.00f, 0.94f, -0.32f, 1.15f, -0.84f,
        1.50f, -0.35f, 1.23f, -0.84f, 1.43f, -0.60f,
    ),
    // The eyelet has to actually close: the upstroke runs to the top right, the return crosses it
    // coming back down the left, and the gap enclosed between the two is the letter.
    'e' to floatArrayOf(
        0.42f, -0.86f, 0.14f, -0.50f, 0.30f, -0.80f,
        0.10f, -0.60f, 0.46f, -1.02f, 0.16f, -0.98f,
        0.36f, -0.04f, 0.02f, -0.28f, 0.14f, -0.02f,
        0.74f, -0.35f, 0.56f, -0.06f, 0.68f, -0.16f,
    ),
    // A real ascender loop: up leaning right, over the top, back down the *left* of the upstroke
    // so the two cross, then down the stem to the baseline.
    'l' to floatArrayOf(
        0.30f, -1.82f, 0.10f, -0.76f, 0.22f, -1.44f,
        0.13f, -1.26f, 0.46f, -2.22f, 0.01f, -2.04f,
        0.31f, -0.05f, 0.21f, -0.86f, 0.18f, -0.28f,
        0.68f, -0.35f, 0.44f, 0.07f, 0.60f, -0.09f,
    ),
    'c' to floatArrayOf(
        0.44f, -0.84f, 0.15f, -0.56f, 0.32f, -0.90f,
        0.11f, -0.70f, 0.50f, -1.06f, 0.19f, -1.06f,
        0.40f, -0.05f, 0.01f, -0.40f, 0.15f, -0.03f,
        0.66f, -0.35f, 0.56f, -0.07f, 0.62f, -0.19f,
    ),
    // A closed oval, left by the top. The third curve returns to exactly where the first ended,
    // which is what shuts it; anything short of that reads as an a.
    'o' to floatArrayOf(
        0.34f, -0.90f, 0.10f, -0.60f, 0.18f, -0.90f,
        0.32f, -0.05f, 0.53f, -0.90f, 0.53f, -0.20f,
        0.34f, -0.90f, 0.08f, -0.01f, 0.03f, -0.86f,
        0.70f, -0.35f, 0.49f, -0.93f, 0.64f, -0.64f,
    ),
    // Round on top, pointed underneath — the mirror of the w.
    'm' to floatArrayOf(
        0.22f, -0.95f, 0.06f, -0.60f, 0.13f, -0.95f,
        0.43f, -0.04f, 0.33f, -0.95f, 0.40f, -0.44f,
        0.63f, -0.95f, 0.47f, -0.50f, 0.54f, -0.95f,
        0.84f, -0.04f, 0.74f, -0.95f, 0.81f, -0.44f,
        1.04f, -0.95f, 0.88f, -0.50f, 0.95f, -0.95f,
        1.25f, -0.04f, 1.15f, -0.95f, 1.22f, -0.44f,
        1.52f, -0.35f, 1.33f, 0.03f, 1.46f, -0.16f,
    ),
)

/** The whole word, in the units above. Built once. */
private fun welcomePath(word: String = "welcome"): Path {
    val path = Path()
    // The lead-in swash, up off the baseline and onto the connector line.
    path.moveTo(-0.34f, -0.02f)
    path.cubicTo(-0.22f, 0.10f, -0.12f, -0.10f, 0f, -0.35f)

    var x = 0f
    for (ch in word) {
        val strokes = WELCOME_STROKES[ch] ?: continue
        var i = 0
        while (i < strokes.size) {
            path.cubicTo(
                x + strokes[i + 2], strokes[i + 3],
                x + strokes[i + 4], strokes[i + 5],
                x + strokes[i], strokes[i + 1],
            )
            i += 6
        }
        x += WELCOME_ADVANCE[ch] ?: 0f
    }

    // And out: a shallow rising swash to finish on, so the word ends rather than stops.
    path.cubicTo(x + 0.24f, -0.30f, x + 0.44f, -0.52f, x + 0.62f, -0.52f)
    return path
}

/**
 * The word being written.
 *
 * `progress` is how much of the stroke has been laid down, by arc length — so the pen moves at a
 * roughly constant speed through the letters rather than racing the short curves and labouring
 * over the long ones, which is what an interpolation per segment would have done.
 *
 * Four passes: three wide and faint for the glow, one sharp on top. It is the cheapest bloom there
 * is, it costs four stroked paths, and it is what keeps the word from looking like a wire bent
 * into a shape.
 */
private fun DrawScope.drawWelcomeWord(
    path: Path,
    measure: PathMeasure,
    scratch: Path,
    progress: Float,
    accent: Color,
) {
    if (progress <= 0.001f) return

    val bounds = path.getBounds()
    if (bounds.width <= 0f || bounds.height <= 0f) return

    scratch.reset()
    if (progress >= 0.999f) {
        scratch.addPath(path)
    } else {
        measure.getSegment(0f, measure.length * progress, scratch, true)
    }

    // Fitted to the box the caller gave us, uniformly, so the hand does not get stretched.
    val scale = min(size.width / bounds.width, size.height / bounds.height)
    val ox = size.width / 2f - (bounds.left + bounds.width / 2f) * scale
    val oy = size.height / 2f - (bounds.top + bounds.height / 2f) * scale

    withTransform({
        translate(ox, oy)
        scale(scale, scale, Offset.Zero)
    }) {
        // Stroke widths are in the same unit space as the path, so they scale with it and the word
        // has the same weight on every screen.
        drawPath(scratch, accent.copy(alpha = 0.13f), style = wstroke(0.46f))
        drawPath(scratch, accent.copy(alpha = 0.18f), style = wstroke(0.30f))
        drawPath(scratch, Color(0xFFBFD4FF).copy(alpha = 0.34f), style = wstroke(0.185f))
        drawPath(scratch, Color.White, style = wstroke(0.088f))
    }
}

private fun wstroke(width: Float) =
    Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

/* ------------------------------------------------------------------------------------------- */
/* Drawing                                                                                      */
/* ------------------------------------------------------------------------------------------- */

/**
 * What the stars turn into.
 *
 * Five very wide radial gradients on a near-black ground, drifting slowly against each other. Wide
 * and soft enough that they need no blur pass — a full-screen blur every frame is the expensive
 * way to get what a large enough gradient gives away for free.
 *
 * It arrives *on* the burst rather than after it. The debris is still crossing the screen while
 * the colour comes up behind it, so the light the explosion throws is the light the page is left
 * lit by, and the two beats read as one thing happening.
 */
private fun DrawScope.drawWelcomeAurora(clock: Float, accent: Color) {
    val up = wsmoothstep((clock - WBURST) / 1.5f)
    if (up <= 0f) return

    val w = size.width
    val h = size.height
    val t = clock * 0.09f
    val reach = hypot(w, h)

    // The ground first, so the blobs are light on something rather than holes in black.
    drawRect(color = Color(0xFF070A14).copy(alpha = up))

    // Deep, cool, cool, warm, and one pale core. The accent is the smallest because it is the
    // loudest; a wash that is mostly brand colour reads as a splash screen for the brand.
    val blobs = listOf(
        Triple(
            Offset(w * (0.24f + 0.06f * sin(t)), h * (0.20f + 0.05f * cos(t * 0.8f))),
            reach * 0.85f, Color(0xFF1B3A8C),
        ),
        Triple(
            Offset(w * (0.82f + 0.05f * cos(t * 1.1f)), h * (0.32f + 0.06f * sin(t * 0.7f))),
            reach * 0.72f, Color(0xFF2E5BD6),
        ),
        Triple(
            Offset(w * (0.16f + 0.07f * sin(t * 0.6f + 2f)), h * (0.74f + 0.05f * cos(t))),
            reach * 0.66f, Color(0xFF4B2C8F),
        ),
        Triple(
            Offset(w * (0.78f + 0.06f * sin(t * 0.9f + 1f)), h * (0.86f + 0.04f * cos(t * 1.3f))),
            reach * 0.55f, accent,
        ),
        Triple(Offset(w * 0.5f, h * 0.5f), reach * 0.5f, Color(0xFF8FB4FF)),
    )

    blobs.forEachIndexed { i, blob ->
        val (centre, radius, color) = blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f)),
                center = centre,
                radius = radius,
            ),
            radius = radius,
            center = centre,
            alpha = up * (if (i == 4) 0.30f else 0.9f),
        )
    }
}

/**
 * The number, at whatever it is doing this second.
 *
 * Four passes, back to front: the haze the stars sit in, every star's bloom drawn twice along the
 * stroke it belongs to, the sharp cores, and the sky behind. Additive throughout, because light is
 * additive and a star drawn over another star is brighter than either.
 *
 * Everything reuses one Paint and one RectF, and nothing in the loop allocates. A thousand stars
 * times two blooms plus a core is three thousand blits a frame; a single allocation in there is
 * the difference between an animation and a stutter.
 */
private fun DrawScope.drawStarNumber(
    clock: Float,
    field: StarNumber?,
    sprites: Array<Array<Bitmap>>,
) {
    val data = field ?: return
    val burst = wclamp01((clock - WBURST) / WBURST_LEN)
    // Once the debris is gone there is nothing here but the aurora, and three thousand blits at
    // zero alpha still cost three thousand blits.
    if (burst >= 1f) return

    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    // The figure is fitted to whichever edge runs out first, and stars are sized against the
    // *stroke* rather than against the window: `span` over the number of characters is roughly one
    // stroke, and everything sitting on it has to scale by the same factor or the chains turn to
    // dust on a small screen. Sizing this off `min(width, height)` is the same expression on a
    // tablet and half of it on a phone, which is exactly wrong.
    val span = min(w * 0.86f, (h * 0.42f) / data.ratio.coerceAtLeast(0.2f))
    val unit = (span / data.perLine) * (3f / 727f)

    val lit = wclamp01(clock / WFADE)
    if (lit <= 0f) return

    // Debris decelerates hard: a cubic ease-out is a shockwave, a linear one is confetti.
    val fly = 1f - fpow(1f - burst, 3f)
    val debris = fpow(1f - burst, 1.7f)
    val formed = wclamp01((clock - WHOLD) / (WASSEMBLE + WSPREAD))

    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.PLUS }
    val src = Rect(0, 0, WSPRITE, WSPRITE)
    val dst = RectF()

    drawIntoCanvas { compose ->
        val canvas = compose.nativeCanvas

        fun blit(bmp: Bitmap, x: Float, y: Float, px: Float, a: Float) {
            if (a <= 0.004f || px <= 0.4f) return
            if (x < -px || x > w + px || y < -px || y > h + px) return
            paint.alpha = (a.coerceAtMost(1f) * 255f).toInt()
            dst.set(x - px / 2f, y - px / 2f, x + px / 2f, y + px / 2f)
            canvas.drawBitmap(bmp, src, dst, paint)
        }

        // 1. the haze. It has nothing to lie along until the stars have arrived, so it arrives
        // with them and late, on the square.
        val haze = formed * formed * (1f - burst)
        if (haze > 0f) {
            for (nb in data.nebula) {
                val home = wsmoother(wclamp01((clock - WHOLD - nb.delay) / WASSEMBLE))
                val nxp = nb.sx * w + (cx + nb.x * span - nb.sx * w) * home
                val nyp = nb.sy * h + (cy + nb.y * span - nb.sy * h) * home
                val travel = if (burst > 0f) nb.burstSpeed * fly * span * 1.9f else 0f
                blit(
                    sprites[nb.tint][1],
                    nxp + nb.burstX * travel,
                    nyp + nb.burstY * travel,
                    nb.size * unit,
                    nb.glow * haze * lit,
                )
            }
        }

        // 2 + 3. every star: bloom, then core.
        for (m in data.motes) {
            val home = wsmoother(wclamp01((clock - WHOLD - m.delay) / WASSEMBLE))
            var px = m.sx * w + (cx + m.x * span - m.sx * w) * home
            var py = m.sy * h + (cy + m.y * span - m.sy * h) * home
            if (burst > 0f) {
                val travel = m.burstSpeed * fly * span * 1.9f
                px += m.burstX * travel
                py += m.burstY * travel
            }

            // Scintillation, sharpened. `sin * |sin|` keeps the period but spends more of it near
            // the middle, so a star sits at its own brightness and occasionally jumps rather than
            // sliding up and down like something being dimmed.
            val tw = sin(clock * m.twRate + m.phase)
            val shine = 1f + m.twDepth * tw * abs(tw) * home

            // Small and faint out in the scatter, full size once home. A star already at its final
            // size while it is still travelling looks like a sprite being moved; one that grows
            // into place looks like it is getting closer.
            val grown = 0.5f + 0.5f * home
            val on = lit * (0.42f + 0.58f * home) * shine * (if (burst > 0f) debris else 1f)

            // Two draws a third of a radius apart down the tangent make an ellipse out of a circle
            // without a per-star transform. It is the whole reason the chains read as ribbons: a
            // round bloom keeps its neighbours separate, a stretched one runs into them. On the way
            // out the same trick, re-aimed along the direction of travel, is the streak.
            val bloom = m.size * (3.8f + m.mag * 9f) * unit * grown * (1f + burst * 2.4f)
            val lean = bloom * (0.3f + burst * 1.1f)
            val bx = if (burst > 0f) m.burstX else m.tx
            val by = if (burst > 0f) m.burstY else m.ty
            val bloomAlpha = m.glow * 0.062f * on
            blit(sprites[m.tint][1], px + bx * lean, py + by * lean, bloom, bloomAlpha)
            blit(sprites[m.tint][1], px - bx * lean, py - by * lean, bloom, bloomAlpha)

            blit(sprites[m.tint][0], px, py, m.size * 1.45f * unit * grown, m.glow * 0.8f * on)
        }

        // The flash: one moment of the number's own light thrown outward, gone in a third of a
        // second. It is what makes the debris read as having been pushed rather than released.
        if (burst > 0f && burst < 0.35f) {
            val f = 1f - burst / 0.35f
            blit(sprites[0][1], cx, cy, span * (0.9f + 2.6f * (1f - f)), f * 0.8f)
        }

        // 4. the sky behind. Off the frame rather than off the figure, and it does not explode:
        // the number came apart, not the room. It fades as the colour comes up, because a star
        // field over a lit sky is a sky with dirt on it.
        val skyOut = 1f - wsmoothstep((clock - WBURST) / 1.2f)
        if (skyOut > 0f) {
            val dustUnit = min(w, h) / 720f
            for (d in data.dust) {
                blit(
                    sprites[d.tint][0],
                    d.x * w,
                    d.y * h,
                    d.size * 2.2f * dustUnit,
                    d.glow * (0.62f + 0.38f * sin(clock * 0.45f + d.phase)) * lit * skyOut,
                )
            }
        }
    }
}
