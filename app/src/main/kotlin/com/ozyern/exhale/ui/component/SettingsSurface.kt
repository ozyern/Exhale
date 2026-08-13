/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The Apple-Music "inset grouped table" palette, in one place.
 *
 * Both the settings screens (`ui.screens.settings`) and the shared preference widgets
 * (`ui.component`) draw the same grouped lists, so the tokens live down here in `ui.component`
 * where either side can reach them without the components package having to depend on a screen
 * package. `SettingsDimensions` re-exports these under its own names for call sites that already
 * read from it.
 *
 * The defining property of the look is a *two-tone* pair: a solid page colour and a solid card
 * colour one step off it. Translucent cards were the previous approach and they fail on a pure
 * black page — 5% white ink over black is just a slightly lighter black, so the group stops
 * reading as a floating plate and starts reading as a smudge.
 */

/** Corner radius of every inset group card. */
val SettingsGroupCornerRadius = 16.dp

/** Hairline between rows inside a group. Never drawn at a card's top or bottom edge. */
val SettingsDividerThickness = 1.dp

/** Left indent so a divider starts under the row's text, not under its leading icon. */
val SettingsDividerStartIndent = 60.dp

/**
 * The ground the group cards float on. Pure black on dark (correct on OLED and the strongest
 * possible contrast against the cards), a near-white grey on light.
 */
@Composable
fun settingsPageBackgroundColor(): Color =
    if (isSystemInDarkTheme()) Color.Black else Color(0xFFF2F2F7)

/** The card colour — one step off [settingsPageBackgroundColor], solid, never elevated. */
@Composable
fun settingsGroupSurfaceColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color.White

/** Low-contrast row separator: enough to divide, not enough to draw a grid. */
@Composable
fun settingsDividerColor(): Color =
    if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
