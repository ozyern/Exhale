/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ozyern.exhale.ui.component.supportsLiveBlur
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * The expanded player's frosted control shelf.
 *
 * Until now the full-screen player had **no glass anywhere** — every control surface on it was a
 * flat alpha box laid over the cover, with a vertical scrim doing all the legibility work. That is
 * why the big player looked cheap next to the dock and the sheets: it was the one screen in the
 * app not built out of the app's own material.
 *
 * This is a genuine backdrop blur of the artwork behind it. The player's own [HazeState] is used
 * rather than the app-wide one, because the app source is the NavHost — a different layer entirely,
 * and the player sheet is drawn over it, so blurring it would sample the *home screen* through the
 * album art. The source is registered on the player's backdrop, which is a sibling drawn before
 * this shelf inside the same `Box`, so there is no re-entrant layer read.
 *
 * @param hazeState the state whose source is the player's artwork backdrop.
 * @param dark true when the shelf sits on dark artwork treatment and should tint toward black.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun PlayerGlassShelf(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    dark: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 34.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val tint = if (dark) Color.Black.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.30f)

    // Memoised: the shelf sits over an animating mesh gradient, and rebuilding the chain would
    // replace the haze node every recomposition, re-registering the blur area each time.
    val glass = remember(hazeState, shape, tint) {
        Modifier
            .clip(shape)
            .then(
                if (supportsLiveBlur) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 42.dp
                        backgroundColor = Color.Transparent
                        noiseFactor = 0.035f
                        // The artwork behind is static per track, but the mesh gradient drifts,
                        // so this blur is genuinely recomputed. Half resolution is free at this
                        // radius and the shelf is a large area.
                        inputScale = HazeInputScale.Fixed(0.5f)
                        tints = listOf(HazeTint(tint))
                    }
                } else {
                    Modifier.background(
                        if (dark) Color.Black.copy(alpha = 0.62f)
                        else Color.White.copy(alpha = 0.72f)
                    )
                }
            )
    }

    Box(modifier = modifier.fillMaxWidth().then(glass)) {
        // Hairline rim: a single light-catching edge is what stops a blurred panel from reading
        // as a smudge and makes it read as a pane with a physical boundary.
        Box(
            Modifier
                .matchParentSize()
                .border(
                    width = 0.7.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (dark) 0.22f else 0.55f),
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = if (dark) 0.09f else 0.20f),
                        )
                    ),
                    shape = shape,
                )
        )
        // Interior sheen, drawn behind the controls so it can never dim a label.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.07f else 0.16f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.06f),
                    )
                )
        )
        content()
    }
}
