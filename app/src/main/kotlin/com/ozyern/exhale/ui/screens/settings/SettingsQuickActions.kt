/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ozyern.exhale.R

/**
 * Primary settings destinations (Appearance, Player, Storage, Privacy) rendered as a true
 * Apple Music-style grouped inset list: full-width rows stacked vertically inside a single
 * block clipped to 16dp over a frosted surface. Thin 1dp-scale dividers separate the rows,
 * NEVER at the very top or bottom of the group. The old side-by-side square-card grid is gone.
 *
 * The [columns] parameter is retained for source compatibility with the adaptive layouts but
 * is intentionally ignored — the list is always a single full-width column.
 */
@Composable
fun SettingsQuickActionsSection(
    actions: List<SettingsQuickAction>,
    @Suppress("UNUSED_PARAMETER") columns: Int = SettingsDimensions.CompactColumns,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor()),
    ) {
        actions.forEachIndexed { index, action ->
            QuickActionRow(
                action = action,
                showDivider = index < actions.size - 1,
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    action: SettingsQuickAction,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "quickActionRowScale",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.06f else 0f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "quickActionRowBg",
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(action.accentColor.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = action.onClick,
                )
                .padding(
                    horizontal = SettingsDimensions.RowHorizontalPadding,
                    vertical = SettingsDimensions.RowVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.RowIconSize)
                    .clip(RoundedCornerShape(SettingsDimensions.RowIconCornerRadius))
                    .background(action.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = action.icon,
                    contentDescription = action.label,
                    tint = action.accentColor,
                    modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = SettingsDimensions.DividerStartIndent),
                thickness = SettingsDimensions.DividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}
