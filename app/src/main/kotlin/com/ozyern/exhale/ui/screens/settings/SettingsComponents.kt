/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.screens.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozyern.exhale.BuildConfig
import com.ozyern.exhale.R
import com.ozyern.exhale.ui.component.settingsIconPuck

/**
 * Wraps a settings destination in the Apple-Music grouped-table look.
 *
 * Two things happen here, both once for every settings screen instead of once per screen file:
 *
 *  1. The page is painted with the solid, distinct grouped background — pure black on dark, a
 *     near-white grey on light — so the inset group cards read as plates floating on a ground
 *     rather than as tinted patches of the same sheet.
 *  2. `colorScheme.surface` is re-pointed at that same colour for the subtree. Every settings
 *     sub-screen draws a plain M3 `TopAppBar`, whose container defaults to `surface`; without
 *     this the bar would sit a visible step off the page it heads. Re-pointing the token also
 *     kills the tonal-elevation tint M3 paints on scrolled bars — the flat, single-colour chrome
 *     is the whole point. Only `surface` moves; `onSurface`, the accent and every container
 *     token are untouched, so contrast and the brand tint are unaffected.
 */
@Composable
fun SettingsPage(content: @Composable BoxScope.() -> Unit) {
    val pageBackground = SettingsDimensions.screenBackgroundColor()
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(surface = pageBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground),
            content = content,
        )
    }
}

/**
 * The header plate at the top of Settings.
 *
 * Modelled on the account row iOS puts at the top of its Settings app: an oversized rounded-square
 * glyph, the name at headline weight, a quiet second line, and a chevron - because the row *goes*
 * somewhere. It used to be a dead card, which meant the most prominent element on the screen was
 * also the only one that did nothing when you tapped it.
 */
/**
 * The search field that sits under the large title.
 *
 * iOS puts a real, visible search field at the top of Settings; we had a magnifier in the app bar
 * that swapped the entire screen for a search overlay. A tap target you have to already know about
 * is not a search affordance — this one states that the page is searchable before you look for it.
 *
 * It is a button rather than a text field: focus, the keyboard and the result list all belong to
 * the existing search overlay, and duplicating a second editable field here would mean two places
 * holding the same query.
 */
@Composable
fun SettingsSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.14f else 0.08f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "searchPillBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.settings_search_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SettingsProfileHeader(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.09f else 0f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "heroBgAlpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsDimensions.HeroCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor())
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        },
                    )
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        // 64dp, and with a live accent behind it. At 56dp on a flat tint this card was a settings
        // row with a slightly larger glyph — the most prominent thing on the page was carrying no
        // more visual weight than the five rows under it. The breathing halo is the same idea as
        // About's spectrum band: on a page of static plates, the identity is the one element that
        // should look like the app is running.
        val haloTransition = rememberInfiniteTransition(label = "heroHalo")
        val halo by haloTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heroHaloPhase",
        )
        val accent = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.26f),
                            accent.copy(alpha = 0.10f),
                        ),
                    )
                )
                // Draw phase, so a permanent animation on the settings root costs one draw
                // invalidation per frame and never recomposes the header or relays it out.
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.10f + 0.22f * halo),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.minDimension * (0.42f + 0.16f * halo),
                        ),
                        radius = size.minDimension * (0.42f + 0.16f * halo),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.exhale),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(34.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.settings_hero_subtitle,
                    BuildConfig.VERSION_NAME,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }
    }
}

@Composable
fun SettingsPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor())
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.BannerIconSize)
                    .settingsIconPuck(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.security),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.permissions_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.permissions_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.allow),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
    }
}

@Composable
fun SettingsUpdateBanner(
    latestVersion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.PressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "updateScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.BannerIconSize)
                    .settingsIconPuck(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_version_available),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "v$latestVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
    }
}

@Composable
fun SettingsSearchEmpty(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.search_try_different),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

/**
 * The header that sits above every grouped block.
 *
 * One definition, used by the categories, the quick actions and the integrations alike. Before
 * this, two of the three groups on the Settings screen had *no* header at all — they were bare
 * cards floating with nothing naming them, which is the one thing an iOS grouped table never does.
 *
 * It is a **label**, not a title. At `titleLarge`/Bold/`onSurface` — what it used to be — the header
 * was set larger and heavier than the row titles it introduced, so scrolling the page read as a
 * stack of competing headlines with the actual content in between. A grouped list wants its
 * headers quiet: small, tracked, one tone down, doing nothing but naming the plate underneath.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            start = SettingsDimensions.SectionHeaderHorizontalPadding + 8.dp,
            end = SettingsDimensions.SectionHeaderHorizontalPadding,
            top = 18.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
fun SettingsGroupCard(
    group: SettingsGroup,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(group.title)

        // Apple Music-style grouped inset list: a Column clipped to 16dp over a frosted
        // translucent surface — NO Material Card. Dividers appear only BETWEEN rows
        // (never top/bottom).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius))
                .background(SettingsDimensions.groupSurfaceColor()),
        ) {
            group.items.forEachIndexed { index, item ->
                SettingsRow(
                    item = item,
                    showDivider = index < group.items.size - 1,
                )
            }
        }
    }
}

/**
 * Reusable Apple Music-style grouped inset section: a large bold header above a Column
 * clipped to 16dp over a distinct surface. Dividers are drawn only BETWEEN the supplied
 * rows — never at the very top or bottom of the group.
 */
@Composable
fun InsetGroup(
    title: String,
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>,
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius))
                .background(SettingsDimensions.groupSurfaceColor()),
        ) {
            rows.forEachIndexed { index, row ->
                row()
                if (index < rows.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = SettingsDimensions.DividerStartIndent),
                        thickness = SettingsDimensions.DividerThickness,
                        color = SettingsDimensions.dividerColor(),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    item: SettingsItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val effectiveAccent = if (item.accentColor.isSpecified) {
        item.accentColor
    } else {
        MaterialTheme.colorScheme.primary
    }

    // Subtle premium haptic tick on row taps — routed through the app-wide custom
    // LocalHapticFeedback provider, which already respects the user's haptics preference.
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // A pressed row in iOS fills edge to edge with a neutral grey and does not move. The previous
    // behaviour shrank the row to 98% and tinted it with the brand colour, which reads as a
    // *button* — and a grouped table is a list of destinations, not a panel of buttons. Scaling
    // also visibly detached each row from the hairlines above and below it.
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.09f else 0f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "rowBgAlpha",
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        item.onClick()
                    },
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
                    .settingsIconPuck(
                        effectiveAccent,
                        RoundedCornerShape(SettingsDimensions.RowIconCornerRadius),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showUpdateIndicator) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(8.dp),
                            )
                        },
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = effectiveAccent,
                            modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.showUpdateIndicator) {
                            effectiveAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item.badge?.let { badge ->
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

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
                color = SettingsDimensions.dividerColor(),
            )
        }
    }
}
