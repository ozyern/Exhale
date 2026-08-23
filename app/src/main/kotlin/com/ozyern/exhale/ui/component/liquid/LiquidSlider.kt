package com.ozyern.exhale.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs

private val ThumbWidth = 40.dp
private val ThumbHeight = 24.dp
private val TrackHeight = 6.dp

/**
 * Liquid glass slider with lens refraction and smooth damped drag animation.
 *
 * Drop-in for Material's `Slider` at the call sites this app uses, hence [steps],
 * [onValueChangeFinished] and [accentColor] — without those three, swapping it in would have
 * silently turned discrete sliders continuous, dropped commit-on-release, and repainted the
 * theme creator's per-channel R/G/B tracks a single blue.
 *
 * ### The thumb follows the finger, not the host
 *
 * A drag positions the thumb *absolutely*, from where the pointer is on the track, and the
 * position it draws lives here for as long as the gesture does. The host's [value] is an output
 * during a drag, never an input.
 *
 * That is not a stylistic preference, it is the fix for a slider that could not be dragged at all.
 * The old gesture accumulated `dragAmount` onto the value the *host* had last sent back, so every
 * frame's delta had to survive a round trip through the caller. The theme creator's R/G/B rows
 * take an `Int`, so a slow drag — a couple of pixels per frame, well under one of 256 steps —
 * was truncated back to the value it started from on every single frame, forever. The thumb sat
 * still while the finger travelled the width of the screen, and a fast flick moved it in random
 * jumps because only those frames survived rounding. Reading the pointer's absolute position
 * removes the loop entirely: quantisation can round what is *published*, but it can no longer
 * feed back into where the next frame thinks the thumb is.
 *
 * The whole row is the grab area, too. The gesture used to live on the 40×24dp thumb, so the
 * control could only be moved by catching that exact rectangle.
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

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f

    // Quantise to the nearest stop before anything leaves this component, so the hosting state
    // and the thumb position can never disagree about where the slider actually is.
    val snap: (Float) -> Float = { raw ->
        if (steps <= 0) raw.coerceIn(valueRange)
        else {
            val stepSize = span / (steps + 1)
            (valueRange.start + ((raw - valueRange.start) / stepSize).fastRoundToInt() * stepSize)
                .coerceIn(valueRange)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth().height(ThumbHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value,
                valueRange = valueRange,
                visibilityThreshold = span * 0.001f,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {},
                // The gesture below drives the thumb directly; this callback is only here because
                // the shared class requires one, and LiquidToggle still routes through it.
                onDrag = { _, _ -> }
            )
        }

        // Live gesture position, in 0..1 along the track. Plain float state written straight from
        // the pointer callback: no `Animatable`, therefore no coroutine and no spring between the
        // finger and the pixels, which is what a direct-manipulation control should feel like.
        // Read only from draw/layout lambdas, so writing it costs a frame, not a recomposition.
        var dragging by remember { mutableStateOf(false) }
        val dragFraction = remember { mutableFloatStateOf(0f) }
        val dragVelocity = remember { mutableFloatStateOf(0f) }

        val progressOf: () -> Float = {
            if (dragging) dragFraction.floatValue
            else dampedDragAnimation.progress.fastCoerceIn(0f, 1f)
        }

        // Travel is the track minus the thumb, so the knob stays fully on the track at both ends
        // instead of hanging a quarter of itself off the edge, and so the fill, the knob and the
        // pointer maths all read the same number.
        val thumbPx = with(LocalDensity.current) { ThumbWidth.toPx() }
        val travelPx = (trackWidth - thumbPx).coerceAtLeast(1f)

        val fractionAt: (Float) -> Float = { x ->
            val raw = ((x - thumbPx / 2f) / travelPx).fastCoerceIn(0f, 1f)
            if (isLtr) raw else 1f - raw
        }
        val publish: (Float) -> Unit = { fraction ->
            val snapped = snap(valueRange.start + fraction * span)
            // Redraw at the *snapped* position, so a stepped slider's knob sits on the stop the
            // caller was told about rather than under the finger.
            dragFraction.floatValue = ((snapped - valueRange.start) / span).fastCoerceIn(0f, 1f)
            onValueChange(snapped)
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value }
                .collectLatest { value ->
                    // Ignored mid-gesture. The host is echoing back what this very drag just
                    // published — often rounded — and letting that steer the thumb is the
                    // feedback loop described in the KDoc.
                    if (!dragging && dampedDragAnimation.targetValue != value) {
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
                    .height(TrackHeight)
                    .fillMaxWidth()
            )

            // Active track. Stops under the thumb's centre — the same expression the thumb's own
            // translation uses — so the fill and the knob can never drift apart.
            Box(
                Modifier
                    .clip(RoundedCornerShape(50)) // Capsule
                    .background(activeColor)
                    .height(TrackHeight)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (thumbPx / 2f + travelPx * progressOf()).fastRoundToInt()
                        layout(width.coerceIn(0, constraints.maxWidth), placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // Thumb
        Box(
            Modifier
                .graphicsLayer {
                    translationX = travelPx * progressOf() * if (isLtr) 1f else -1f
                }
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
                        val velocity =
                            (if (dragging) dragVelocity.floatValue else dampedDragAnimation.velocity) / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // Never fully clear. At `1 - progress` the pressed thumb was pure glass:
                        // against a dark track it vanished under the finger, so the one moment the
                        // knob most needs to be locatable was the one moment it could not be seen.
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress * 0.55f))
                    }
                )
                .size(ThumbWidth, ThumbHeight)
        )

        // ---- Gestures ----
        //
        // ONE handler, deciding for itself whether the gesture is a tap, a horizontal drag or a
        // vertical scroll that merely started on top of a slider.
        //
        // It used to be two `pointerInput` nodes on this same Box — `detectTapGestures` plus
        // `detectHorizontalDragGestures` — and the tap half worked while the drag half never
        // fired. Two detectors on one node is a race over the same pointer stream: the tap
        // detector consumes the down as soon as it sees it, and each detector's own cancellation
        // rules then decide the outcome of every subsequent move. Which one wins depends on pass
        // ordering and on internals of two library gesture loops, neither of which this file
        // controls, and the answer here was consistently "neither drags".
        //
        // Written out, the arbitration is four lines and there is nothing left to race:
        //
        //  * movement is accumulated but nothing is claimed and nothing is consumed until the
        //    pointer has travelled past touch slop, so a hesitant finger still counts as a tap;
        //  * whichever axis crosses slop first wins the gesture outright — vertical first and
        //    this bails out having consumed nothing, which is what lets the theme creator's three
        //    stacked sliders be scrolled *past* rather than acting as three dead bands;
        //  * horizontal first and every later change is consumed, so the scrolling column cannot
        //    steal the drag back halfway through;
        //  * an up before either slop is a tap.
        //
        // Both paths position the thumb from the pointer's absolute x — see the KDoc for why
        // nothing here may accumulate onto a value the host has handed back.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(trackWidth, isLtr, steps, valueRange) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var claimed = false
                        var abandoned = false
                        var last = down.position

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == down.id }
                            if (change == null) {
                                abandoned = true
                                break
                            }
                            // Something upstream took the gesture (a parent scroll winning its
                            // own slop, the sheet being dragged). Only meaningful before we
                            // claim: afterwards we are the ones consuming.
                            if (!claimed && change.isConsumed) {
                                abandoned = true
                                break
                            }
                            last = change.position
                            if (!change.pressed) break

                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y

                            if (!claimed) {
                                if (abs(totalY) > slop && abs(totalY) > abs(totalX)) {
                                    abandoned = true
                                    break
                                }
                                if (abs(totalX) > slop) {
                                    claimed = true
                                    dragging = true
                                    dragVelocity.floatValue = 0f
                                    dampedDragAnimation.press()
                                }
                            }

                            if (claimed) {
                                // Cheap exponential smoothing of pointer speed, in track-fractions
                                // per frame. Only the thumb's squish reads it, so a VelocityTracker
                                // with its own history buffer would be paying for precision
                                // nothing looks at.
                                dragVelocity.floatValue =
                                    dragVelocity.floatValue * 0.7f + (delta.x / travelPx) * 3f
                                publish(fractionAt(change.position.x))
                                change.consume()
                            }
                        }

                        when {
                            claimed -> {
                                val target = snap(valueRange.start + dragFraction.floatValue * span)
                                dragVelocity.floatValue = 0f
                                // `dragging` is cleared from inside the snap, not here — see
                                // snapToValue.
                                dampedDragAnimation.snapToValue(target) { dragging = false }
                                dampedDragAnimation.release()
                                onValueChangeFinished?.invoke()
                            }

                            !abandoned -> {
                                val target = snap(valueRange.start + fractionAt(last.x) * span)
                                dampedDragAnimation.animateToValue(target)
                                onValueChange(target)
                                onValueChangeFinished?.invoke()
                            }
                        }
                    }
                }
        )
    }
}