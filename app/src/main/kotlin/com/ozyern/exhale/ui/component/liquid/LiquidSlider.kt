package com.ozyern.exhale.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest

/**
 * Liquid glass slider with lens refraction and smooth damped drag animation.
 *
 * Drop-in for Material's `Slider` at the call sites this app uses, hence [steps],
 * [onValueChangeFinished] and [accentColor] — without those three, swapping it in would have
 * silently turned discrete sliders continuous, dropped commit-on-release, and repainted the
 * theme creator's per-channel R/G/B tracks a single blue.
 *
 * @param steps number of discrete stops *between* the range ends, matching Material's meaning.
 *   0 (default) is a continuous slider.
 * @param onValueChangeFinished invoked once when the drag or tap settles — for callers that
 *   only want to persist the final value, not every frame of the drag.
 * @param accentColor active-track colour. Defaults to the app's system blue.
 */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = Color.Unspecified,
    // Kept for API compatibility only — see LiquidToggle. Never consumes the app layer.
    @Suppress("UNUSED_PARAMETER") backdrop: Backdrop = rememberInContentBackdrop()
) {
    val isLightTheme = !isSystemInDarkTheme()
    val activeColor = accentColor.takeOrElse {
        if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    }
    val trackColor = if (isLightTheme) Color(0xFF787878).copy(0.2f) else Color(0xFF787880).copy(0.36f)

    // Quantise to the nearest stop before anything leaves this component, so the hosting state
    // and the thumb position can never disagree about where the slider actually is.
    val snap: (Float) -> Float = { raw ->
        if (steps <= 0) raw.coerceIn(valueRange)
        else {
            val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
            (valueRange.start + ((raw - valueRange.start) / stepSize).fastRoundToInt() * stepSize)
                .coerceIn(valueRange)
        }
    }
    val emit: (Float) -> Unit = { raw -> onValueChange(snap(raw)) }

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value,
                valueRange = valueRange,
                visibilityThreshold = (valueRange.endInclusive - valueRange.start) * 0.001f,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        emit(targetValue)
                    }
                    onValueChangeFinished?.invoke()
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    emit(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange)
                    )
                }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value }
                .collectLatest { value ->
                    if (dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        // Track layer
        Box(Modifier.layerBackdrop(trackBackdrop)) {
            // Background track
            Box(
                Modifier
                    .clip(RoundedCornerShape(50)) // Capsule
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue = snap(
                                (if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            )
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                            onValueChangeFinished?.invoke()
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            // Active track
            Box(
                Modifier
                    .clip(RoundedCornerShape(50)) // Capsule
                    .background(activeColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // Thumb
        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    // Consume ONLY the local track layer, never the ancestor app content
                    // backdrop — see LiquidToggle for the full rationale (re-entrant layer
                    // draw crash). The `backdrop` parameter is ignored for in-content sliders.
                    backdrop = rememberBackdrop(trackBackdrop) { drawBackdrop ->
                        val progress = dampedDragAnimation.pressProgress
                        val scaleX = lerp(2f / 3f, 1f, progress)
                        val scaleY = lerp(0f, 1f, progress)
                        scale(scaleX, scaleY) {
                            drawBackdrop()
                        }
                    },
                    shape = { RoundedCornerShape(50) }, // Capsule
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
