/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ozyern.exhale.constants.AquamorphicDampingRatio
import com.ozyern.exhale.constants.AquamorphicStiffness

/** Top corner radius of every frosted sheet. Deep enough to read as iOS rather than Material. */
private val SheetCornerRadius = 36.dp

/**
 * The app's signature frosted-glass bottom sheet.
 *
 * This is the chrome only — the rising spring entrance, the layered translucent gradient, the
 * bright hairline along the top rim, the grab handle, and the scrim tap-to-dismiss. Callers supply
 * the contents.
 *
 * It exists because the account sheet and the update sheet were otherwise going to carry two
 * independent copies of the same forty lines of gradient and border, which is exactly how two
 * surfaces that are supposed to be the *same material* drift apart. Anything presented as a sheet
 * should come through here.
 *
 * Deliberately not a Material `ModalBottomSheet`: that paints an opaque `surfaceContainer` with a
 * tonal elevation overlay, which defeats the translucency the whole look depends on, and its
 * entrance is a fixed Material easing rather than the app's shared spring.
 *
 * @param onDismiss invoked on scrim tap, back press and handle-area dismissal.
 * @param dismissible when false the sheet ignores every dismissal route. Used while an operation
 *   the user must not interrupt is in flight (an in-progress download); the grab handle is hidden
 *   too, so the sheet also *looks* modal rather than merely refusing to close.
 * @param maxHeightFraction cap on the sheet's height as a fraction of the available window.
 */
@Composable
fun LiquidGlassSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    maxHeightFraction: Float = 0.88f,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = dismissible,
            dismissOnBackPress = dismissible,
        ),
    ) {
        val scheme = MaterialTheme.colorScheme
        val sheetShape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)

        // Rise from the bottom edge on the app's shared spring, rather than popping in fully
        // formed. Driven off a one-shot flag so it plays on entry only.
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val offsetFraction by animateFloatAsState(
            targetValue = if (shown) 0f else 1f,
            animationSpec = spring(
                dampingRatio = AquamorphicDampingRatio,
                stiffness = AquamorphicStiffness,
            ),
            label = "liquidSheetSlide",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { if (dismissible) onDismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            BoxWithConstraints {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * maxHeightFraction)
                        // Translate in the draw phase — animating an offset or padding here would
                        // relayout the whole sheet on every frame of the entrance.
                        .graphicsLayer { translationY = offsetFraction * size.height }
                        .clip(sheetShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    scheme.surfaceContainerHigh.copy(alpha = 0.96f),
                                    scheme.surfaceContainer.copy(alpha = 0.92f),
                                ),
                            ),
                        )
                        // The hairline is what sells it as glass: a bright rim catching light at
                        // the top edge, fading to nothing by the bottom.
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.24f),
                                    Color.White.copy(alpha = 0.03f),
                                ),
                            ),
                            shape = sheetShape,
                        )
                        // Swallow taps so anything inside the sheet never reaches the scrim.
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {},
                ) {
                    if (dismissible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(scheme.onSurfaceVariant.copy(alpha = 0.35f)),
                            )
                        }
                    }

                    content()
                }
            }
        }
    }
}
