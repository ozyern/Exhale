/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component.liquid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * The backdrop that glass **inside** a page may legally refract.
 *
 * ### The problem this exists to solve
 *
 * Kyant's glass is not a blur — it is a lens. `drawBackdrop` samples real pixels from a
 * [LayerBackdrop] (an off-screen `GraphicsLayer` recording) and bends them at the rim. With
 * nothing recorded there is nothing to bend, and the surface collapses into a tinted rectangle
 * with a bright edge: convincing enough on artwork, and unmistakably *not glass* everywhere else.
 * That is exactly the complaint the old in-content recipe kept drawing.
 *
 * The obvious fix — sample [LocalAppBackdrop], the recording of the NavHost — is illegal for
 * anything inside the NavHost. The layer would have to draw itself, and Compose throws on the
 * first frame. (Twice, historically: "Settings crashes on click", then "the Updates page crashes
 * on open".) So `rememberInContentBackdrop` handed back an empty canvas and in-content glass
 * simply went without refraction.
 *
 * ### The way around it
 *
 * Re-entrancy is a question of *ancestry*, not of depth. A layer may not sample itself, but any
 * content may sample a layer that was recorded by a **sibling drawn beneath it**. So instead of
 * pointing in-content glass at the recording of the whole app, each page records only what is
 * painted *behind* its content — the ambient wash, the drifting colour field, the page ground —
 * and publishes that here. The content is not inside that recording, so sampling it is ordinary,
 * and the refraction is of exactly the pixels a real pane of glass on that page would bend.
 *
 * Null means no page has published one, and glass should fall back to its tonal recipe rather
 * than refract an empty layer. It is a plain `compositionLocalOf` (not `static`) precisely
 * because nested pages are expected to override it: the root publishes the app's ambient
 * background, and a page that paints its own opaque ground over that — Settings, say — publishes
 * its own so its glass refracts the wash that is actually behind it rather than an aurora that
 * is not visible anywhere on screen.
 */
val LocalPageBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Paints [background] into a fresh [LayerBackdrop] and draws [content] over it as a sibling,
 * with that backdrop published as [LocalPageBackdrop].
 *
 * The ordering is the whole point and is not an implementation detail: [background] is a separate
 * child laid out *first*, so by the time [content] draws, the recording it wants to sample has
 * already been made this frame.
 */
@Composable
fun PageBackdropHost(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(backdrop),
            content = background,
        )
        CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}
