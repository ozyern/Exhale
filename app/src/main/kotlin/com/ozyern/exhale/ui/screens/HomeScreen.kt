/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ozyern.exhale.innertube.utils.parseCookieString
import com.ozyern.exhale.LocalPlayerAwareWindowInsets
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.db.entities.Album
import com.ozyern.exhale.db.entities.Artist
import com.ozyern.exhale.db.entities.LocalItem
import com.ozyern.exhale.db.entities.Playlist
import com.ozyern.exhale.db.entities.Song
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.innertube.models.WatchEndpoint
import com.ozyern.exhale.models.toMediaMetadata
import com.ozyern.exhale.playback.queues.YouTubeQueue
import com.ozyern.exhale.constants.InnerTubeCookieKey
import com.ozyern.exhale.constants.ShowHomeCategoryChipsKey
import com.ozyern.exhale.ui.component.AmbientArtworkGlow
import com.ozyern.exhale.ui.component.ChipsRow
import com.ozyern.exhale.ui.component.LocalBottomSheetPageState
import com.ozyern.exhale.ui.component.LocalMenuState
import com.ozyern.exhale.ui.component.rememberArtworkAmbientColors
import com.ozyern.exhale.ui.component.NavigationTitle
import com.ozyern.exhale.utils.rememberPreference
import com.ozyern.exhale.viewmodels.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val allItemsMetadata by viewModel.allItemsMetadata.collectAsState()

    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    // Apple-Music declutter: the mood-chip carousel is off by default (still opt-in via
    // Settings → Appearance) — the reference Home has no chip row.
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, false)
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) {
            viewModel.toggleChip(selectedChip)
        }
    }

    // One handler for the shortcut grid. Songs play in place; everything else is a destination.
    val onShortcutClick: (LocalItem) -> Unit = { item ->
        when (item) {
            is Song ->
                if (item.id == mediaMetadata?.id) {
                    playerConnection.player.togglePlayPause()
                } else {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            endpoint = WatchEndpoint(item.id),
                            preloadItem = item.toMediaMetadata(),
                        )
                    )
                }

            is Album -> navController.navigate("album/${item.id}")
            is Artist -> navController.navigate("artist/${item.id}")
            is Playlist -> navController.navigate("local_playlist/${item.id}")
        }
    }

    // Ambient colour from the current cover, washing down from the top of the surface.
    //
    // This is NOT the old 5-blob mesh backdrop that used to live here — that one covered the whole
    // screen, animated across all of it, and cost a full-screen overdraw every frame for a texture
    // you read as noise. This is confined to the top, drawn in the draw phase only, and gone by
    // mid-viewport, so the list below it sits on the plain surface exactly as before.
    //
    // Keyed to the current song when there is one and to the top of the feed when there is not.
    // Keying it to playback alone meant the wash did not exist on a cold start — you opened the
    // app, nothing was playing, and Home was the flat page this was added to fix, right up until
    // you tapped something. The first artwork on the page is a perfectly good stand-in: it is the
    // picture the user is looking at anyway, so the room is lit by what is on screen.
    val ambientSource: Pair<String, String?>? = mediaMetadata?.let { it.id to it.thumbnailUrl }
        ?: quickPicks?.firstOrNull { it.thumbnailUrl != null }?.let { it.id to it.thumbnailUrl }
        ?: keepListening?.firstOrNull { it.thumbnailUrl != null }?.let { it.id to it.thumbnailUrl }

    val ambientColors by rememberArtworkAmbientColors(
        songId = ambientSource?.first,
        thumbnailUrl = ambientSource?.second,
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AmbientArtworkGlow(
            colors = ambientColors,
            modifier = Modifier.fillMaxSize(),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
        ) {
            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                // Apple Music's large title: the weekday as a quiet eyebrow over a heavy
                // display-size "Home". It lives *inside* the scroll content rather than in the app
                // bar, which is the whole point of the pattern — it scrolls away under the bar as
                // you move down, leaving the compact chrome behind.
                item(key = "home_large_title", contentType = "large_title") {
                    HomeLargeTitle()
                }

                keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                    item(key = "home_shortcuts", contentType = "shortcuts") {
                        HomeShortcutsGrid(
                            items = items.take(6),
                            onItemClick = onShortcutClick,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (showHomeCategoryChips) {
                    item(key = "chips_row", contentType = "chips_row") {
                        ChipsRow(
                            chips = homePage?.chips.orEmpty().map { it to it.title },
                            currentValue = selectedChip,
                            onValueUpdate = {
                                viewModel.toggleChip(it)
                            }
                        )
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
            /*
                item {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier.animateItem()
                    )
                }
            */

                item(key = "quick_picks", contentType = "quick_picks") {
                    QuickPicksSection(
                        quickPicks = picks,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        metadataMap = allItemsMetadata
                    )
                }
            }

            // Keep Listening used to be a horizontal shelf of grid cards down here, behind
            // Quick Picks. It is the shelf people actually came for, and a sideways-scrolling
            // row shows four items at a time — so it moved to the top of the page and became a
            // Spotify shortcut grid instead, which shows six at once and needs no aiming.

            AccountPlaylistsContainer(
                viewModel = viewModel,
                accountName = accountName,
                accountImageUrl = url,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )

            SimilarRecommendationsContainer(
                viewModel = viewModel,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )

            // Stable per-section keys keep the LazyColumn from recomposing/rebinding every
            // section (and its whole nested LazyRow of thumbnails) when the list grows via
            // pagination. Fall back to the title and index if a section has no endpoint identity.
            //
            // The browse id alone is NOT unique, which is what crashed the app with
            // `Key "home_section_title_FEmusic_new_releases_albums" was already used`: YouTube
            // Music happily returns the same endpoint twice in one feed — "New releases" and
            // "New albums & singles" are both `FEmusic_new_releases_albums` — and a continuation
            // can append a section the first page already had. LazyColumn treats a repeated key
            // as a programming error and throws during measure, so the whole screen went down the
            // moment such a feed arrived.
            //
            // Repeats get an occurrence suffix rather than everything getting the index appended:
            // the first section under a given id keeps the key it always had, so the common case
            // (an append-only feed with no duplicates) is byte-for-byte the old behaviour and
            // pagination still does not rebind a single existing row.
            val sectionKeys = buildList {
                val seen = HashMap<String, Int>()
                homePage?.sections?.forEachIndexed { index, section ->
                    val base = section.endpoint?.browseId ?: "title_${section.title}_$index"
                    val occurrence = (seen[base] ?: 0) + 1
                    seen[base] = occurrence
                    add(if (occurrence == 1) base else "${base}__$occurrence")
                }
            }

            homePage?.sections?.forEachIndexed { index, section ->
                val sectionKey = sectionKeys.getOrElse(index) { "section_$index" }

                item(key = "home_section_title_$sectionKey", contentType = "section_title") {
                    HomePageSectionTitle(
                        section = section,
                        navController = navController,
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "home_section_content_$sectionKey", contentType = "home_section") {
                    HomePageSectionContent(
                        section = section,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        scope = scope,
                        metadataMap = allItemsMetadata
                    )
                }
            }

            if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                item(key = "home_loading_shimmer", contentType = "shimmer") {
                    HomeLoadingShimmer(modifier = Modifier.animateItem())
                }
            }
            }

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    }
}

/**
 * The Apple Music large title.
 *
 * Two lines: the weekday, quiet and small, over the section name at display weight. Apple leads
 * Home with the day rather than a greeting because it is the one piece of context that is always
 * true and never has to guess at the hour or the mood.
 *
 * Horizontal insets are applied the same way [com.ozyern.exhale.ui.component.NavigationTitle] does
 * them, so the title's left edge lands exactly on the section headers below it.
 */
@Composable
private fun HomeLargeTitle(modifier: Modifier = Modifier) {
    val weekday = remember {
        DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(LocalDate.now())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
    ) {
        // The eyebrow steps back: semibold at label size, not bold at title size. It was
        // competing with the line under it, and an eyebrow that competes with its own headline is
        // just a two-line heading.
        Text(
            text = weekday,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home),
            // Tracked in hard. At 36sp the default fitting leaves visible gaps between letters —
            // the difference between a word that has been *set* and one that has been typed. This
            // is the largest type in the app, so it is where loose tracking shows most.
            style = MaterialTheme.typography.displaySmall.copy(letterSpacing = (-0.03).em),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
