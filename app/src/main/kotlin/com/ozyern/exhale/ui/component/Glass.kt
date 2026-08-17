/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ozyern.exhale.ui.theme.LocalGlass
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared [HazeState] for the app's live backdrop blur. Provided by the root scaffold and
 * read by glass surfaces via [GlassSurface] so panels can blur the content behind them
 * without prop-drilling. Null when live blur isn't wired for the current screen.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * True only where the platform supports hardware [android.graphics.RenderEffect] backdrop
 * blur (Android 12 / API 31+). Below this, glass surfaces fall back to a translucent scrim.
 */
val supportsLiveBlur: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The app's one real backdrop-blur modifier for **floating chrome** — the bottom dock, the
 * search capsule, the device sheet. Blurs whatever the [LocalHazeState] source is drawing
 * (the NavHost content) and lays a translucent film over it so text stays legible.
 *
 * Why this exists rather than Kyant's `drawBackdrop`: the app-wide `LocalAppBackdrop` is
 * `rememberDefaultBackdrop()`, an **empty** passthrough canvas that draws nothing. Blurring
 * and refracting it produces exactly nothing, which is why the dock read as fully
 * transparent — the only pixels it ever painted were its own tint film. Haze's source layer
 * is the real one, already registered on the NavHost via `hazeSource`.
 *
 * @param tintAlpha opacity of the film laid over the blur. Higher = milkier, more legible.
 * @param blurRadius blur strength. Apple's chrome sits around 40–60dp at this scale.
 * @param quality fraction of full resolution the blur is computed at. Below 1 the GPU
 *   samples a smaller layer and upscales — invisible under a heavy blur, and the single
 *   biggest lever on blur cost, which matters because this runs under a scrolling list.
 */
@OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun rememberChromeGlassModifier(
    shape: Shape,
    dark: Boolean,
    tintAlpha: Float = 0.32f,
    blurRadius: Dp = 48.dp,
    quality: Float = 0.5f,
): Modifier {
    val hazeState = LocalHazeState.current
    val tint = if (dark) Color.Black.copy(alpha = tintAlpha) else Color.White.copy(alpha = tintAlpha)
    // Memoised on everything it depends on: rebuilding the chain replaces the haze node,
    // which re-registers the blur area and re-reads the source layer. Doing that mid-morph
    // is what turns a cheap GPU blur into dropped frames.
    return remember(hazeState, shape, tint, blurRadius, quality) {
        Modifier
            .clip(shape)
            .then(
                if (hazeState != null && supportsLiveBlur) {
                    Modifier.hazeEffect(state = hazeState) {
                        this.blurRadius = blurRadius
                        backgroundColor = Color.Transparent
                        noiseFactor = 0.04f
                        inputScale = HazeInputScale.Fixed(quality)
                        tints = listOf(HazeTint(tint))
                    }
                } else {
                    // No hardware blur: a solid-enough scrim is the only honest fallback.
                    // A near-transparent film here is what "invisible dock" looks like.
                    Modifier.background(
                        if (dark) Color.Black.copy(alpha = 0.72f)
                        else Color.White.copy(alpha = 0.80f)
                    )
                }
            )
    }
}

/**
 * Decorative "frosted glass" surface treatment that works on *every* device: soft shadow,
 * translucent fill, and a hairline light-catching border. This does NOT do live backdrop
 * blur — use [GlassSurface] for that. Ideal for cards/grid items over the liquid background.
 */
fun Modifier.glassSurface(
    shape: Shape? = null,
    tint: Color? = null,
    border: Boolean = true,
): Modifier = composed {
    val glass = LocalGlass.current
    val s = shape ?: RoundedCornerShape(glass.cornerRadius)
    this
        .shadow(
            elevation = glass.shadowElevation,
            shape = s,
            clip = false,
            ambientColor = glass.shadowColor,
            spotColor = glass.shadowColor,
        )
        .clip(s)
        .background(tint ?: glass.fallbackScrim)
        .then(if (border) Modifier.border(glass.borderWidth, glass.borderBrush, s) else Modifier)
}

/**
 * A frosted glass panel with *live* backdrop blur of whatever is drawn behind the current
 * [LocalHazeState] source (Android 12+). On older devices, or when no source is available,
 * it degrades gracefully to a translucent tinted scrim — the layout and content are identical.
 *
 * All Haze-specific API is contained here, so a Haze version bump only touches this function.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalGlass.current.cornerRadius),
    hazeState: HazeState? = LocalHazeState.current,
    tint: Color? = null,
    border: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalGlass.current
    val useLiveBlur = hazeState != null && supportsLiveBlur

    val base = modifier
        .shadow(
            elevation = glass.shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = glass.shadowColor,
            spotColor = glass.shadowColor,
        )
        .clip(shape)

    val filled = if (useLiveBlur) {
        base.hazeEffect(state = hazeState!!) {
            blurRadius = glass.blurRadius
            backgroundColor = glass.blurBackground
            noiseFactor = glass.noiseFactor
        }.background(tint ?: glass.tint)
    } else {
        base.background(tint ?: glass.fallbackScrim)
    }

    Box(
        modifier = filled
            .then(if (border) Modifier.border(glass.borderWidth, glass.borderBrush, shape) else Modifier),
    ) {
        // Soft interior sheen — draw behind content so it never dims text.
        Box(Modifier.matchParentSize().background(glass.highlightBrush))
        content()
    }
}

/**
 * Slowly drifting, multi-layered organic gradient "blobs" that simulate a liquid look
 * behind frosted surfaces. Seed [colors] from the theme (which is already album-art
 * reactive) so the ambient background matches the current song.
 *
 * Cheap: a handful of radial gradients on a single [Canvas]; the soft radial falloff
 * reads as blur without an actual (expensive) full-screen blur pass.
 */
@Composable
fun LiquidBackground(
    colors: List<Color>,
    baseColor: Color,
    modifier: Modifier = Modifier,
    blobAlpha: Float = 0.45f,
) {
    val transition = rememberInfiniteTransition(label = "liquid")
    val twoPi = (2.0 * PI).toFloat()
    val p1 by transition.animateFloat(
        0f, twoPi, infiniteRepeatable(tween(19_000, easing = LinearEasing)), label = "p1",
    )
    val p2 by transition.animateFloat(
        0f, twoPi, infiniteRepeatable(tween(27_000, easing = LinearEasing)), label = "p2",
    )
    val p3 by transition.animateFloat(
        0f, twoPi, infiniteRepeatable(tween(33_000, easing = LinearEasing)), label = "p3",
    )

    val palette = remember(colors, blobAlpha) {
        val safe = if (colors.size >= 3) colors else (colors + colors + colors).take(3)
        safe.map { it.copy(alpha = blobAlpha) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(baseColor)
        val w = size.width
        val h = size.height
        val r = maxOf(w, h) * 0.85f

        fun blob(color: Color, cx: Float, cy: Float, radius: Float) {
            val center = Offset(cx, cy)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        blob(palette[0], w * (0.30f + 0.18f * sin(p1)), h * (0.26f + 0.14f * cos(p1 * 0.9f)), r)
        blob(palette[1], w * (0.74f + 0.16f * sin(p2)), h * (0.42f + 0.16f * cos(p2)), r * 0.9f)
        blob(palette[2], w * (0.48f + 0.22f * cos(p3)), h * (0.82f + 0.12f * sin(p3)), r * 1.1f)
    }
}
