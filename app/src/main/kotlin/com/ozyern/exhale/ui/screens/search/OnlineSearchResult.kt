/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package com.ozyern.exhale.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ozyern.exhale.innertube.YouTube.SearchFilter
import com.ozyern.exhale.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.ozyern.exhale.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.ozyern.exhale.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.ozyern.exhale.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.ozyern.exhale.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.ozyern.exhale.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.ozyern.exhale.innertube.models.AlbumItem
import com.ozyern.exhale.innertube.models.ArtistItem
import com.ozyern.exhale.innertube.models.PlaylistItem
import com.ozyern.exhale.innertube.models.SongItem
import com.ozyern.exhale.innertube.models.WatchEndpoint
import com.ozyern.exhale.innertube.models.YTItem
import com.ozyern.exhale.LocalPlayerAwareWindowInsets
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.SearchFilterHeight
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.models.toMediaMetadata
import com.ozyern.exhale.playback.queues.YouTubeQueue
import com.ozyern.exhale.ui.component.ChipsRow
import com.ozyern.exhale.ui.component.EmptyPlaceholder
import com.ozyern.exhale.ui.component.LocalMenuState
import com.ozyern.exhale.ui.component.YouTubeListItem
import com.ozyern.exhale.ui.component.shimmer.ListItemPlaceHolder
import com.ozyern.exhale.ui.component.shimmer.ShimmerHost
import com.ozyern.exhale.ui.menu.YouTubeAlbumMenu
import com.ozyern.exhale.ui.menu.YouTubeArtistMenu
import com.ozyern.exhale.ui.menu.YouTubePlaylistMenu
import com.ozyern.exhale.ui.menu.YouTubeSongMenu
import com.ozyern.exhale.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch
import kotlin.text.get

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }
    // Sections in "All" mode, each carrying the filter that shows the rest of it. The top
    // summary ("Top result") has no filter of its own, so its header gets no See-all affordance.
    val allModeSections =
        buildList<Triple<String, List<YTItem>, SearchFilter?>> {
            searchSummary?.summaries?.firstOrNull()?.takeIf { it.items.isNotEmpty() }?.let {
                add(Triple(it.title, it.items, null))
            }

            listOf(
                FILTER_SONG to stringResource(R.string.filter_songs),
                FILTER_VIDEO to stringResource(R.string.filter_videos),
                FILTER_ALBUM to stringResource(R.string.filter_albums),
                FILTER_ARTIST to stringResource(R.string.filter_artists),
                FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
            ).forEach { (sectionFilter, sectionTitle) ->
                viewModel.viewStateMap[sectionFilter.value]
                    ?.items
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { items ->
                        add(Triple(sectionTitle, items, sectionFilter))
                    }
            }
        }

    val isAllModeLoaded =
        searchSummary != null ||
                listOf(
                    FILTER_SONG,
                    FILTER_VIDEO,
                    FILTER_ALBUM,
                    FILTER_ARTIST,
                    FILTER_COMMUNITY_PLAYLIST,
                    FILTER_FEATURED_PLAYLIST,
                ).all { viewModel.viewStateMap.containsKey(it.value) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is ArtistItem ->
                        YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss,
                        )

                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive =
                when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(
                    onClick = longClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
                Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata()
                                            )
                                        )
                                    }
                                }

                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            }
                        },
                        onLongClick = longClick,
                    )
                    .animateItem(),
        )
    }

    val topInset = WindowInsets.systemBars.only(WindowInsetsSides.Top).asPaddingValues()
        .calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                // The filter row is no longer an opaque slab pinned under a top search bar —
                // it floats over the list — so the list only has to clear the status bar plus
                // the row's own height. `AppBarHeight` used to be reserved on top of that for
                // a search field that is now docked at the bottom of the screen instead.
                top = topInset + SearchFilterHeight + 12.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                    .calculateBottomPadding(),
            ),
        ) {
            if (searchFilter == null) {
                allModeSections.forEachIndexed { index, (title, sectionItems, sectionFilter) ->
                    item(key = "section_header_${title}_$index") {
                        SearchSectionHeader(
                            title = title,
                            // "See all" swaps the chip row onto this section's filter, which is
                            // the same thing tapping the chip does — so the header is a shortcut
                            // to the full list rather than a dead label.
                            onSeeAll = sectionFilter?.let {
                                {
                                    if (viewModel.filter.value != it) viewModel.filter.value = it
                                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    itemsIndexed(
                        items = sectionItems,
                        key = { itemIndex, item -> "$title/${item.id}/$itemIndex" },
                    ) { _, item ->
                        ytItemContent(item)
                    }

                    item(key = "section_spacer_${title}_$index") {
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (allModeSections.isEmpty() && isAllModeLoaded) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            } else {
                items(
                    items = itemsPage?.items.orEmpty().distinctBy { it.id },
                    key = { "filtered_${it.id}" },
                    itemContent = ytItemContent,
                )

                if (itemsPage?.continuation != null) {
                    item(key = "loading") {
                        ShimmerHost {
                            repeat(3) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }

                if (itemsPage?.items?.isEmpty() == true) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }

            if (searchFilter == null && allModeSections.isEmpty() && !isAllModeLoaded ||
                searchFilter != null && itemsPage == null
            ) {
                item {
                    ShimmerHost {
                        repeat(8) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }

        // Floating filter row. It used to be a `Surface` with a shadow — an opaque grey plank
        // welded across the top of the results, which is the single most dated thing on this
        // page. Now the chips ride on a scrim that fades to nothing, so results scroll up and
        // dissolve under them instead of hitting a hard edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surface,
                        0.72f to MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        1f to Color.Transparent,
                    ),
                )
                .padding(top = topInset, bottom = 10.dp),
        ) {
            ChipsRow(
                chips =
                    listOf(
                        null to stringResource(R.string.filter_all),
                        FILTER_SONG to stringResource(R.string.filter_songs),
                        FILTER_VIDEO to stringResource(R.string.filter_videos),
                        FILTER_ALBUM to stringResource(R.string.filter_albums),
                        FILTER_ARTIST to stringResource(R.string.filter_artists),
                        FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                        FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
                    ),
                currentValue = searchFilter,
                onValueUpdate = {
                    if (viewModel.filter.value != it) {
                        viewModel.filter.value = it
                    }
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                icons = mapOf(
                    null to R.drawable.search,
                    FILTER_SONG to R.drawable.music_note,
                    FILTER_VIDEO to R.drawable.slow_motion_video,
                    FILTER_ALBUM to R.drawable.album,
                    FILTER_ARTIST to R.drawable.person,
                    FILTER_COMMUNITY_PLAYLIST to R.drawable.queue_music,
                    FILTER_FEATURED_PLAYLIST to R.drawable.playlist_play,
                ),
            )
        }
    }
}

/**
 * Section heading for "All" results.
 *
 * The old one was a 3dp accent tick beside 14sp semibold text — a Material-2 era list caption.
 * This is a real heading: the title at `titleLarge` weight so sections separate at a glance
 * while scrolling fast, and a "See all" chip that jumps the chip row to that category instead
 * of leaving the user to find the matching filter themselves.
 */
@Composable
private fun SearchSectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onSeeAll != null) {
            Text(
                text = stringResource(R.string.view_all),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
