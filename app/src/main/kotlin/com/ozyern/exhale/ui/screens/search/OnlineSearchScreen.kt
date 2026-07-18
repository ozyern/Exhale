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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ozyern.exhale.innertube.models.*
import com.ozyern.exhale.LocalDatabase
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.models.toMediaMetadata
import com.ozyern.exhale.playback.queues.YouTubeQueue
import com.ozyern.exhale.ui.component.LocalMenuState
import com.ozyern.exhale.ui.component.YouTubeListItem
import com.ozyern.exhale.ui.menu.*
import com.ozyern.exhale.R
import com.ozyern.exhale.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    // Apple-Music "Browse Categories" landing: shown while the field is empty and there are no
    // history/suggestion rows to surface. Tapping a category runs it as a search query.
    val showBrowse = query.isBlank() &&
        viewState.history.isEmpty() &&
        viewState.suggestions.isEmpty() &&
        viewState.items.isEmpty()

    if (showBrowse) {
        BrowseCategoriesGrid(
            pureBlack = pureBlack,
            onProfileClick = { navController.navigate("settings") },
            onCategoryClick = { category ->
                onSearch(category)
                onDismiss()
            },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.systemBars.only(WindowInsetsSides.Top)
                    .asPaddingValues().calculateTopPadding() + 4.dp,
                // Extra bottom room so the last cards clear the floating mini-player + bottom search pill.
                bottom = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                    .asPaddingValues().calculateBottomPadding() + 140.dp,
            ),
        )
        return
    }

    // iOS inset-grouped list geometry: history + suggestions render as ONE rounded card,
    // so every row needs its global position to pick its corner shape and divider.
    val historyCount = viewState.history.size
    val groupCount = historyCount + viewState.suggestions.size

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (historyCount > 0) {
            item(key = "recent_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 24.dp, top = 8.dp, bottom = 6.dp)
                        .animateItem(),
                ) {
                    Text(
                        text = stringResource(R.string.search_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.clear),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                database.query {
                                    clearSearchHistory()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        itemsIndexed(viewState.history, key = { _, it -> "history_${it.query}" }) { index, history ->
            SuggestionItem(
                query = history.query,
                online = false,
                position = groupPosition(index, groupCount),
                showDivider = index < groupCount - 1,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query {
                        delete(history)
                    }
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        itemsIndexed(viewState.suggestions, key = { _, it -> "suggestion_$it" }) { index, query ->
            SuggestionItem(
                query = query,
                online = true,
                position = groupPosition(historyCount + index, groupCount),
                showDivider = historyCount + index < groupCount - 1,
                onClick = {
                    onSearch(query)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(query, TextRange(query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.top_results),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(viewState.items.distinctBy { it.id }, key = { "item_${it.id}" }) { item ->
            YouTubeListItem(
                item = item,
                isActive = when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = scope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(item.toMediaMetadata())
                                        )
                                        onDismiss()
                                    }
                                }

                                is AlbumItem -> {
                                    navController.navigate("album/${item.id}")
                                    onDismiss()
                                }

                                is ArtistItem -> {
                                    navController.navigate("artist/${item.id}")
                                    onDismiss()
                                }

                                is PlaylistItem -> {
                                    navController.navigate("online_playlist/${item.id}")
                                    onDismiss()
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )

                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )

                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )

                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    )
                    .animateItem()
            )
        }
    }
}

/** Where a row sits inside its inset-grouped card — drives per-corner rounding. */
enum class GroupPosition { SINGLE, FIRST, MIDDLE, LAST }

fun groupPosition(index: Int, count: Int): GroupPosition = when {
    count <= 1 -> GroupPosition.SINGLE
    index == 0 -> GroupPosition.FIRST
    index == count - 1 -> GroupPosition.LAST
    else -> GroupPosition.MIDDLE
}

/**
 * One row of the iOS-style inset-grouped "Recent Searches" card: rows share a single
 * rounded translucent surface (corners only on the group's first/last row), separated by
 * hairline dividers inset past the leading icon — exactly Apple's Settings/Music list
 * grammar, replacing the old flat full-bleed text rows.
 */
@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    position: GroupPosition = GroupPosition.MIDDLE,
    showDivider: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean
) {
    val cardColor = if (pureBlack) {
        Color.White.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
    }

    val iconTint = if (pureBlack) {
        Color.White.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val corner = 22.dp
    val shape = when (position) {
        GroupPosition.SINGLE -> RoundedCornerShape(corner)
        GroupPosition.FIRST -> RoundedCornerShape(topStart = corner, topEnd = corner)
        GroupPosition.LAST -> RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
        GroupPosition.MIDDLE -> RoundedCornerShape(0.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(cardColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .focusable()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )

            Spacer(Modifier.width(14.dp))

            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                color = if (pureBlack) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (!online) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = if (pureBlack) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(onClick = onFillTextField, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.arrow_top_left),
                    contentDescription = null,
                    tint = if (pureBlack) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showDivider) {
            // Hairline divider inset past the icon column — the iOS grouped-list separator.
            HorizontalDivider(
                thickness = 0.5.dp,
                color = if (pureBlack) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 49.dp)
            )
        }
    }
}
