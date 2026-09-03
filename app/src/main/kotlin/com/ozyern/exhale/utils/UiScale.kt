/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.utils

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import com.ozyern.exhale.constants.UiScaleKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

/**
 * Interface scale: Exhale's own copy of Android's *Display Size*, for this app only.
 *
 * ### Why this is a Configuration override and not a `LocalDensity` override
 *
 * The obvious implementation is one line in the theme — provide a scaled [androidx.compose.ui.unit.Density]
 * and let every `dp` and `sp` follow. It is also broken, in two ways that only show up once you
 * actually use the app:
 *
 *  1. **`LocalConfiguration` does not follow.** `screenWidthDp` and `screenHeightDp` stay in
 *     *system* dp, so every `(screenWidthDp * f).dp` in the codebase — the player artwork, the
 *     always-on display, the artist hero, the bottom-sheet menus — multiplies a system-dp number
 *     by a scaled density and lands at `scale x` its intended size. At 130% a menu is a third
 *     taller than the screen it has to fit in.
 *  2. **Dialogs and popups do not follow.** Each one is a new window with its own
 *     `AndroidComposeView`, and that view re-provides `LocalDensity` (and `LocalConfiguration`)
 *     from its own resources. Everything painted in a dialog would silently snap back to system
 *     size.
 *
 * Overriding the Activity's `Configuration` fixes all of it at the source, because that is where
 * both values come from. `densityDpi` drives `DisplayMetrics.density`, which is what Compose reads
 * for `LocalDensity` in every window it opens; `screenWidthDp`/`screenHeightDp` come along
 * explicitly (see [withUiScale]); and resource qualifiers — `sw600dp`, `w480dp` — resolve against
 * the scaled values, exactly as they do when the system's own Display Size is changed.
 *
 * `fontScale` is deliberately untouched. Android separates the two dials on purpose: Display Size
 * scales density so dp and sp grow together and a layout keeps its proportions, while Font Size
 * scales sp alone. Scaling both here would compound with the user's accessibility font setting and
 * hand someone at 130% text a 169% app.
 *
 * ### The cost
 *
 * A Configuration override is read in `attachBaseContext`, so changing it means recreating the
 * Activity. That is why the picker commits on a button rather than live on the slider.
 */
const val UiScaleDefault = 1f

/**
 * The offered sizes, in 5% steps.
 *
 * Discrete, not a free slider: below 85% the 40dp minimum touch target stops surviving, above 130%
 * the two-column grids wrap, and the values between the steps are differences nobody can see.
 */
val UiScaleSteps: List<Float> = listOf(
    0.85f, 0.90f, 0.95f, 1.00f, 1.05f, 1.10f, 1.15f, 1.20f, 1.25f, 1.30f,
)

val UiScaleMin: Float get() = UiScaleSteps.first()
val UiScaleMax: Float get() = UiScaleSteps.last()

/**
 * The narrowest the app is willing to believe its own screen is.
 *
 * Scaling up does not make the phone bigger; it makes the app think the phone is smaller. At 130%
 * a 360dp handset reports 277dp, which is under every width this app's chrome was drawn against —
 * the dock stops fitting its tabs, the two-column grids stop being two columns, the player's
 * controls start touching. 300dp is the floor where all of that still holds.
 *
 * So the offered steps are filtered per device rather than fixed: a large phone gets the whole
 * range, a small one is not offered a size that would break the app it is offering it in.
 */
private const val MinUsableWidthDp = 300f

/**
 * The steps this particular screen can actually render, largest offer first excluded if it would
 * take the layout under [MinUsableWidthDp].
 *
 * @param systemWidthDp the screen's width in *unscaled* dp — what the device would report at 100%.
 */
fun usableUiScaleSteps(systemWidthDp: Float): List<Float> {
    if (systemWidthDp <= 0f) return UiScaleSteps
    // Never return an empty list, and never take away the system size itself: a phone too narrow
    // for 100% is not a phone this app runs on at all.
    return UiScaleSteps.filter { it <= UiScaleDefault || systemWidthDp / it >= MinUsableWidthDp }
}

/** The nearest offered step to [scale] — the stored value is never trusted to be one of them. */
fun snapUiScale(scale: Float): Float =
    UiScaleSteps.minBy { kotlin.math.abs(it - scale) }

/** Index of [scale] in [UiScaleSteps], for driving a stepper. */
fun uiScaleStepIndex(scale: Float): Int =
    UiScaleSteps.indexOf(snapUiScale(scale)).coerceAtLeast(0)

/**
 * A context whose resources report the scaled size.
 *
 * `densityDpi` alone is not enough. The framework recomputes `DisplayMetrics` from it but leaves
 * the configuration's dp screen size exactly as it found it, so an override that sets only the
 * density produces a Configuration that contradicts itself — a 1080px-wide screen claiming to be
 * both 411dp and 3.5x density. The dp dimensions are therefore divided by the same factor the
 * density was multiplied by, which is what the system does at the display level when Display Size
 * changes.
 */
fun Context.withUiScale(scale: Float): Context {
    if (scale == UiScaleDefault) return this

    val base = resources.configuration
    val scaled = Configuration(base).apply {
        densityDpi = (base.densityDpi * scale).roundToInt()
        // Undefined stays undefined: these are 0 before the window has been sized, and 0/scale is
        // a number the framework would then treat as a real 0dp screen.
        if (base.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
            screenWidthDp = (base.screenWidthDp / scale).roundToInt()
        }
        if (base.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
            screenHeightDp = (base.screenHeightDp / scale).roundToInt()
        }
        if (base.smallestScreenWidthDp != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
            smallestScreenWidthDp = (base.smallestScreenWidthDp / scale).roundToInt()
        }
    }

    return createConfigurationContext(scaled)
}

/**
 * The stored scale, read synchronously.
 *
 * `attachBaseContext` runs before anything else and cannot suspend, so this blocks — a single
 * DataStore read, which is already in memory for every call after the first. Any failure falls
 * back to the system size rather than propagating: an unreadable preference should cost the user
 * their scale setting, not their launch.
 */
fun readUiScaleBlocking(context: Context): Float =
    runCatching {
        runBlocking { context.dataStore.data.first()[UiScaleKey] }
    }.getOrNull()
        ?.let { snapUiScale(it) }
        ?: UiScaleDefault

/**
 * Persist [scale].
 *
 * Suspends until the write has landed so the caller can recreate the Activity immediately
 * afterwards — [readUiScaleBlocking] runs during that recreation, and a fire-and-forget write
 * races it.
 */
suspend fun writeUiScale(context: Context, scale: Float) {
    context.dataStore.edit { it[UiScaleKey] = snapUiScale(scale) }
}
