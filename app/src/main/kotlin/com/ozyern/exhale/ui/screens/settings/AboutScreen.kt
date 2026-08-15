/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.screens.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ozyern.exhale.BuildConfig
import com.ozyern.exhale.LocalPlayerAwareWindowInsets
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.AquamorphicDampingRatio
import com.ozyern.exhale.constants.AquamorphicStiffness
import com.ozyern.exhale.ui.component.AuroraBackdrop
import com.ozyern.exhale.ui.component.IconButton
import com.ozyern.exhale.ui.component.liquidGlassSurface
import com.ozyern.exhale.ui.utils.backToMain

// ─── People and links ─────────────────────────────────────────────────────────

private const val LeadDeveloperName = "Aditya Jha"
private const val LeadDeveloperHandle = "ozyern"

/**
 * GitHub serves every user's avatar at `github.com/<handle>.png`, so the maintainer's picture
 * follows whatever they set on their profile instead of being pinned to a numeric asset id that
 * silently rots the day they change it.
 */
private const val LeadDeveloperAvatar = "https://github.com/$LeadDeveloperHandle.png"
private const val LeadDeveloperUrl = "https://github.com/$LeadDeveloperHandle"

private const val LicenseUrl = "https://github.com/ozyern/Exhale/blob/master/LICENSE"

private data class SocialLink(
    val iconRes: Int,
    val label: String,
    val handle: String,
    val url: String,
)

private val SocialLinks = listOf(
    SocialLink(R.drawable.github, "GitHub", "@ozyern", "https://github.com/ozyern"),
    SocialLink(R.drawable.telegram, "Telegram", "@ozyern", "https://t.me/ozyern"),
    SocialLink(
        R.drawable.instagram,
        "Instagram",
        "@imozyern",
        "https://www.instagram.com/imozyern/",
    ),
)

// ─── Screen ───────────────────────────────────────────────────────────────────

/**
 * About.
 *
 * Rebuilt as an inset grouped table on the same ground as the rest of Settings. What was here
 * before was a stack of `ElevatedCard`s at 20–32dp radii with shadows, shimmer sweeps, fake hover
 * states and a hero that rotated two degrees when tapped — a different design language on every
 * card. Now there is one: a hero plate, then labelled groups of rows, the same rows Settings uses.
 *
 * The hero keeps exactly one flourish, and it is hidden. See [ExhaleBreathingEgg].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    var showEasterEgg by remember { mutableStateOf(false) }

    if (showEasterEgg) {
        ExhaleBreathingEgg(onDismiss = { showEasterEgg = false })
    }

    val pageBackground = SettingsDimensions.screenBackgroundColor()
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                // Flat and identical in both states, like every other settings destination.
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = pageBackground,
                    scrolledContainerColor = pageBackground,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            contentPadding = PaddingValues(start = pad, end = pad, top = 4.dp, bottom = 40.dp),
        ) {
            item(key = "hero") {
                AboutHero(
                    onSecretUnlocked = { showEasterEgg = true },
                    modifier = Modifier.padding(bottom = spacing),
                )
            }

            item(key = "maintainer") {
                Column(modifier = Modifier.padding(bottom = spacing)) {
                    SettingsSectionHeader(stringResource(R.string.about_maintainer))
                    AboutGroup {
                        AboutPersonRow(
                            avatarUrl = LeadDeveloperAvatar,
                            name = LeadDeveloperName,
                            role = stringResource(R.string.about_lead_developer),
                            onClick = { uriHandler.openUri(LeadDeveloperUrl) },
                        )
                    }
                }
            }

            item(key = "social") {
                Column(modifier = Modifier.padding(bottom = spacing)) {
                    SettingsSectionHeader(stringResource(R.string.about_connect))
                    AboutGroup {
                        SocialLinks.forEachIndexed { index, link ->
                            if (index > 0) AboutDivider()
                            AboutRow(
                                icon = link.iconRes,
                                title = link.label,
                                value = link.handle,
                                onClick = { uriHandler.openUri(link.url) },
                            )
                        }
                    }
                }
            }

            item(key = "info") {
                Column(modifier = Modifier.padding(bottom = spacing)) {
                    SettingsSectionHeader(stringResource(R.string.about_information))
                    AboutGroup {
                        AboutValueRow(
                            icon = R.drawable.info,
                            title = stringResource(R.string.update_installed_version),
                            value = BuildConfig.VERSION_NAME,
                        )
                        AboutDivider()
                        AboutValueRow(
                            icon = R.drawable.token,
                            title = stringResource(R.string.about_build),
                            value = BuildConfig.VERSION_CODE.toString(),
                        )
                        AboutDivider()
                        AboutValueRow(
                            icon = R.drawable.exhale,
                            title = stringResource(R.string.about_package),
                            value = BuildConfig.APPLICATION_ID,
                        )
                    }
                }
            }

            item(key = "license") {
                Column {
                    SettingsSectionHeader(stringResource(R.string.about_legal))
                    AboutGroup {
                        AboutRow(
                            icon = R.drawable.policy,
                            title = "GNU General Public License v3.0",
                            value = null,
                            onClick = { uriHandler.openUri(LicenseUrl) },
                        )
                    }
                }
            }
        }
    }
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

/** Taps on the hero needed to open the easter egg. Android's version-tap egg wants seven too. */
private const val SecretTapCount = 7

/**
 * The identity plate: app mark, name, version.
 *
 * It counts taps. Nothing visible acknowledges them until the fourth, at which point the mark
 * starts leaning into each press a little harder than a normal tap would — enough that someone
 * poking at it realises something is happening, and invisible to someone who is not. On the
 * seventh, [ExhaleBreathingEgg] opens.
 */
@Composable
private fun AboutHero(
    onSecretUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var taps by remember { mutableIntStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // The tell. Below the halfway mark this is an ordinary, almost imperceptible press response;
    // past it the mark visibly winds up, which is the only hint the egg exists.
    val warmth = (taps.toFloat() / SecretTapCount).coerceIn(0f, 1f)
    val markScale by animateFloatAsState(
        targetValue = if (isPressed) 1f - 0.04f - 0.10f * warmth else 1f + 0.06f * warmth,
        animationSpec = spring(
            dampingRatio = AquamorphicDampingRatio,
            stiffness = AquamorphicStiffness,
        ),
        label = "markScale",
    )
    val markRotation by animateFloatAsState(
        targetValue = if (isPressed) -8f * warmth else 0f,
        animationSpec = spring(
            dampingRatio = AquamorphicDampingRatio,
            stiffness = AquamorphicStiffness,
        ),
        label = "markRotation",
    )

    // Taps have to be consecutive-ish; wandering off and coming back later starts over.
    LaunchedEffect(taps) {
        if (taps in 1 until SecretTapCount) {
            kotlinx.coroutines.delay(2_500)
            taps = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsDimensions.HeroCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                val next = taps + 1
                if (next >= SecretTapCount) {
                    taps = 0
                    onSecretUnlocked()
                } else {
                    taps = next
                }
            }
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f + 0.25f * warmth),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = markScale
                        scaleY = markScale
                        rotationZ = markRotation
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            ),
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(26.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.exhale),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.about_version_build,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toString(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Easter egg ───────────────────────────────────────────────────────────────

private const val InhaleMillis = 4_000
private const val ExhaleMillis = 6_000

/**
 * The hidden thing behind seven taps on the About hero.
 *
 * The app is called Exhale, so the egg is a breathing pacer: an orb that swells over four seconds
 * and empties over six — the 4-6 rhythm that actually slows a heart rate rather than a decorative
 * loop that merely looks calm. It runs over the app's own aurora, and it is the one screen in the
 * app with no controls on it at all: tap anywhere to leave.
 *
 * The orb is driven by a single `Animatable` read inside `graphicsLayer`, so the whole thing is
 * two scaling layers on the render thread and nothing else.
 */
@Composable
private fun ExhaleBreathingEgg(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val breath = remember { Animatable(0f) }
        var inhaling by remember { mutableStateOf(true) }
        var breaths by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                inhaling = true
                breath.animateTo(1f, tween(InhaleMillis, easing = FastOutSlowInEasing))
                inhaling = false
                breath.animateTo(0f, tween(ExhaleMillis, easing = FastOutSlowInEasing))
                breaths++
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AuroraBackdrop()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Outer bloom, lagging slightly behind the core so the orb has depth rather
                    // than reading as one flat disc changing size.
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .graphicsLayer {
                                val scale = 0.55f + 0.45f * breath.value
                                scaleX = scale
                                scaleY = scale
                                alpha = 0.30f + 0.35f * breath.value
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        Color.Transparent,
                                    ),
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer {
                                val scale = 0.62f + 0.38f * breath.value
                                scaleX = scale
                                scaleY = scale
                            }
                            .liquidGlassSurface(
                                shape = CircleShape,
                                tint = MaterialTheme.colorScheme.primary,
                            ),
                    )
                }

                Spacer(Modifier.height(36.dp))

                Text(
                    text = stringResource(
                        if (inhaling) R.string.egg_breathe_in else R.string.egg_breathe_out
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (breaths == 0) {
                        stringResource(R.string.egg_hint)
                    } else {
                        stringResource(R.string.egg_breath_count, breaths.toString())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─── Grouped rows ─────────────────────────────────────────────────────────────

@Composable
private fun AboutGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius))
            .background(SettingsDimensions.groupSurfaceColor()),
    ) {
        content()
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = SettingsDimensions.DividerStartIndent),
        thickness = SettingsDimensions.DividerThickness,
        color = SettingsDimensions.dividerColor(),
    )
}

/** A row fronted by a circular photograph rather than a glyph tile: a person, not a setting. */
@Composable
private fun AboutPersonRow(
    avatarUrl: String,
    name: String,
    role: String,
    onClick: () -> Unit,
) {
    AboutRowScaffold(onClick = onClick) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(SettingsDimensions.RowIconSize)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    shape = CircleShape,
                ),
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AboutChevron()
    }
}

@Composable
private fun AboutRow(
    icon: Int,
    title: String,
    value: String?,
    onClick: () -> Unit,
) {
    AboutRowScaffold(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(SettingsDimensions.RowIconSize)
                .clip(RoundedCornerShape(SettingsDimensions.RowIconCornerRadius))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
        }

        AboutChevron()
    }
}

/** A row that states a fact. No chevron, no press state — nothing here is tappable. */
@Composable
private fun AboutValueRow(
    icon: Int,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AboutChevron() {
    Icon(
        painter = painterResource(R.drawable.navigate_next),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.size(SettingsDimensions.ChevronSize),
    )
}

/** Shared row geometry and the iOS press fill, so every About row behaves like a Settings row. */
@Composable
private fun AboutRowScaffold(
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.09f else 0f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "aboutRowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = SettingsDimensions.RowHorizontalPadding,
                vertical = SettingsDimensions.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
