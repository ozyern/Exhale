/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The clip window for the "Dynamic Island" player morph.
 *
 * At [progress] `1f` this is the plain full-screen rectangle the expanded player has always
 * drawn into. As the sheet collapses toward `0f` the window contracts — top-anchored — down to
 * exactly the collapsed mini-player pill: [pillHeightPx] tall, [pillInsetPx] in from each edge,
 * with [pillRadiusPx] corners. Because the pill lives at the *top* of the sheet's collapsed
 * region (the sheet box itself is what slides down), anchoring the window's top edge at 0 and
 * only animating its bottom edge makes the window track the sheet's visible area for free.
 *
 * Interpolating the outline instead of cross-fading two composables is what sells the effect:
 * the launcher-style rectangle→pill corner sweep happens on the render thread as a single
 * hardware outline clip, so it stays glued to the drag with no recomposition and no layout pass.
 *
 * This is a `data class` on purpose. Compose invalidates a layer's outline by comparing the new
 * [Shape] to the old one, so a fresh *structurally equal* instance per frame is a no-op while a
 * mutated singleton would never invalidate at all. One small allocation per animated frame is
 * far cheaper than the outline rebuild it guards.
 */
@Immutable
data class DynamicIslandMorphShape(
    val progress: Float,
    val pillHeightPx: Float,
    val pillInsetPx: Float,
    val pillRadiusPx: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        val collapsing = 1f - p

        // Guard against the first frame, where the sheet has been composed but not yet measured.
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(size.toRect())
        }

        val inset = (pillInsetPx * collapsing).coerceAtMost(size.width / 2f)
        val bottom = (pillHeightPx + (size.height - pillHeightPx) * p).coerceIn(0f, size.height)
        val radius = (pillRadiusPx * collapsing).coerceAtMost(minOf(size.width / 2f - inset, bottom / 2f))

        return Outline.Rounded(
            RoundRect(
                left = inset,
                top = 0f,
                right = size.width - inset,
                bottom = bottom,
                cornerRadius = CornerRadius(radius),
            )
        )
    }
}

private fun Size.toRect() = androidx.compose.ui.geometry.Rect(0f, 0f, width, height)

/**
 * `smoothstep` — zero first derivative at both ends, so the shrink eases out of the fully
 * expanded state and settles into the pill instead of arriving at constant velocity. Applied to
 * the *scale* only; the clip window stays linear so it keeps tracking the finger during a drag.
 */
fun morphEase(p: Float): Float {
    val x = p.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
