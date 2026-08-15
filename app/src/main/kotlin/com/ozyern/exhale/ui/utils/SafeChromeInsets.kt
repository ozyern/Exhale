/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/**
 * The horizontal inset every piece of *floating* chrome must clear — the mini-player pill, the
 * frosted nav bar, the search bar, and the Dynamic-Island morph target they share.
 *
 * ### Why `systemBars ∪ displayCutout`
 *
 * `systemBars` alone is not the unsafe region on the skins this app targets. OnePlus and Oppo
 * (OxygenOS 16 / ColorOS 16) render a live "Fluid Cloud" capsule around the camera cutout, and
 * `displayCutout` is the only inset that reports a *side* cutout at all — which is what a device
 * held in landscape actually has. Chrome that respects only the system bars will happily slide
 * underneath it.
 *
 * ### Why one symmetric value instead of a left/right pair
 *
 * All of this chrome is centred, and — decisively — the morph target handed to `BottomSheet` is a
 * *single symmetric* `pillHorizontalInset`. The morph clips the collapsing player to
 * `[inset, width - inset]`. Feed that a value derived from one edge while the pill it is becoming
 * is positioned from the other, and on any device with an asymmetric inset (landscape, nav bar on
 * one side) the player shrinks into a rectangle offset from the pill and the hand-off visibly
 * jumps. Taking the wider edge and applying it to both keeps the two provably identical, at the
 * cost of a few dp of padding on the safe side — invisible next to a morph that lands wrong.
 *
 * Callers must apply this as plain symmetric `padding`, **not** `windowInsetsPadding`, which would
 * reintroduce the per-edge asymmetry this exists to avoid.
 */
@Composable
fun safeHorizontalChromeInset(): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    return with(density) {
        maxOf(
            insets.getLeft(density, layoutDirection),
            insets.getRight(density, layoutDirection),
        ).toDp()
    }
}
