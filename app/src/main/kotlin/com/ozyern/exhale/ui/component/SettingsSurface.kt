/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

/**
 * Corner radius of every inset group card.
 *
 * 22dp, not 16. At 16 the cards read as *rectangles with the corners taken off*; the softness has
 * to be a visible property of the shape before a stack of plates on a grey ground stops looking
 * like a table with borders. This is the single cheapest change that moves the whole of Settings.
 */
val SettingsGroupCornerRadius = 22.dp

/** Hairline between rows inside a group. Never drawn at a card's top or bottom edge. */
val SettingsDividerThickness = 1.dp

/**
 * Left indent so a divider starts under the row's text, not under its leading icon.
 *
 * This is not a taste value — it is the sum of the row geometry above it: 16dp of card padding,
 * a 36dp glyph tile, and the 14dp gap after it. At 60dp (what it used to be) every hairline in
 * Settings stopped 6dp short of the text it was supposed to align with, which is exactly the kind
 * of near-miss that makes a grouped list read as an imitation of iOS rather than as iOS.
 */
val SettingsDividerStartIndent = 66.dp

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

/**
 * The rounded-square plate every settings glyph sits on.
 *
 * One definition for what used to be seven copies of `background(accent.copy(alpha = 0.12f))`
 * scattered across the settings screens — which is how a "design system" quietly becomes seven
 * things that happen to look similar today.
 *
 * A flat 12% wash is what made the rows look generic: a real puck catches light. This is a
 * vertical gradient from a brighter top to a dimmer bottom with a hairline rim, so the glyph
 * plate reads as a small physical object rather than a tinted rectangle. The values are low
 * enough that a row of four different accents still reads as one family.
 */
fun Modifier.settingsIconPuck(
    accent: Color,
    shape: Shape = RoundedCornerShape(12.dp),
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.11f))
        )
    )
    .border(
        width = 0.7.dp,
        brush = Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.38f), accent.copy(alpha = 0.06f))
        ),
        shape = shape,
    )
