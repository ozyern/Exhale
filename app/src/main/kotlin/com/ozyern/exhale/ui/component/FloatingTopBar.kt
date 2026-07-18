/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.ozyern.exhale.ui.component.liquid.LocalAppBackdrop

/**
 * A detached, floating "liquid glass" backdrop for the main top header — a perfect,
 * borderless capsule that uses the genuine Kyant AndroidLiquidGlass backdrop engine to
 * REFRACT the app content scrolling underneath it (true lens distortion, not a flat blur).
 *
 * The header is a sibling of the content layer (it lives in the Scaffold's `topBar` slot,
 * the NavHost lives in the `content` slot), so consuming [LocalAppBackdrop] here is safe —
 * it is never a re-entrant draw of the layer it lives inside.
 *
 * Strict styling: ZERO border, ZERO outline, ZERO highlight stroke. Separation from the
 * content comes only from a soft, wide, diffuse drop shadow (the "floating island" feel).
 *
 * Place this BEHIND a transparent [androidx.compose.material3.TopAppBar]; the bar's own
 * logo/account content renders on top.
 */
@Composable
fun FloatingTopBarSurface(
    height: Dp,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    horizontalMargin: Dp = 14.dp,
    topMargin: Dp = 8.dp,
    @Suppress("UNUSED_PARAMETER") cornerRadius: Dp = 28.dp,
) {
    val backdrop = LocalAppBackdrop.current
    val isDark = isSystemInDarkTheme() || pureBlack
    // Perfect capsule — a true floating pill regardless of height.
    val pill = RoundedCornerShape(percent = 50)

    // Soft, wide, diffuse shadow instead of any border/outline.
    val shadowColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.16f)
    // A whisper-thin translucent film so text stays legible over bright content;
    // NOT a border — it fills the whole surface.
    val filmColor = if (isDark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalMargin, end = horizontalMargin, top = topMargin)
            .height(height)
            .shadow(
                elevation = 20.dp,
                shape = pill,
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { pill },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                onDrawSurface = {
                    // Borderless translucent film — fills the surface, draws no stroke.
                    drawRect(filmColor)
                },
            ),
    )
}
