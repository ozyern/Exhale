/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.ozyern.exhale.LocalDatabase
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.ChipSortTypeKey
import com.ozyern.exhale.constants.DisableBlurKey
import com.ozyern.exhale.constants.LibraryFilter
import com.ozyern.exhale.constants.PlaylistTagsFilterKey
import com.ozyern.exhale.constants.ShowTagsInLibraryKey
import com.ozyern.exhale.ui.component.ChipsRow
import com.ozyern.exhale.ui.component.TagsFilterChips
import com.ozyern.exhale.utils.rememberEnumPreference
import com.ozyern.exhale.utils.rememberPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val filterContent = @Composable {
        Column {
            // The page opened straight onto a row of filter chips with nothing above it, so
            // there was no moment where the screen said what it was. A display-weight title
            // gives the carousels below something to hang off and matches the heading scale
            // the rest of the app moved to.
            Text(
                text = stringResource(R.string.filter_library),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 14.dp),
            )
            Row {
                ChipsRow(
                    chips =
                    listOf(
                        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                        LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                        LibraryFilter.SPOTIFY to stringResource(R.string.spotify)
                    ),
                    currentValue = filterType,
                    onValueUpdate = {
                        filterType =
                            if (filterType == it) {
                                LibraryFilter.LIBRARY
                            } else {
                                it
                            }
                    },
                    icons = mapOf(
                        LibraryFilter.PLAYLISTS to R.drawable.queue_music,
                        LibraryFilter.SONGS to R.drawable.music_note,
                        LibraryFilter.ALBUMS to R.drawable.album,
                        LibraryFilter.ARTISTS to R.drawable.person,
                        LibraryFilter.SPOTIFY to R.drawable.spotify_icon
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            if (showTagsInLibrary) {
                TagsFilterChips(
                    database = database,
                    selectedTags = selectedTagIds,
                    onTagToggle = { tag ->
                        val newTags = if (tag.id in selectedTagIds) {
                            selectedTagIds - tag.id
                        } else {
                            selectedTagIds + tag.id
                        }
                        onSelectedTagsFilterChange(newTags.joinToString(","))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // Captured outside `drawBehind` so the draw lambda reads no composition state.
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // M3E Mesh gradient background layer at the top
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f) // Cover top 70% of screen
                    .align(Alignment.TopCenter)
                    .zIndex(-1f) // Place behind all content
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Three blobs, not five. Each `drawRect` here covers the top 70% of the
                    // display with a full-size radial gradient, and every one of them is a
                    // separate full-surface fill — on a 1080p panel that was five overlapping
                    // screen-sized paints plus a sixth for the fade, on every invalidation of
                    // this layer. Three carries the same mesh read (warm left, cool right, a
                    // lift through the middle) at just over half the fill cost, and the fade
                    // to surface is folded into the same pass.
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color1.copy(alpha = 0.38f),
                                color1.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            center = Offset(width * 0.15f, height * 0.10f),
                            radius = width * 0.72f,
                        ),
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color2.copy(alpha = 0.34f),
                                color2.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                            center = Offset(width * 0.88f, height * 0.20f),
                            radius = width * 0.78f,
                        ),
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color3.copy(alpha = 0.26f),
                                color3.copy(alpha = 0.11f),
                                Color.Transparent,
                            ),
                            center = Offset(width * 0.42f, height * 0.52f),
                            radius = width * 0.85f,
                        ),
                    )

                    // Fade the whole thing into the page so the mesh has no visible edge.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.30f),
                                surfaceColor.copy(alpha = 0.72f),
                                surfaceColor,
                            ),
                            startY = height * 0.42f,
                            endY = height,
                        ),
                    )
                }
        ) {}
        }

        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(
                navController,
                filterContent,
                onTabSelected = { filterType = it }
            )
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY }
            )
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY }
            )
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY }
            )

            LibraryFilter.SPOTIFY -> {
                LibrarySpotifyPlaylistsScreen(
                    navController = navController,
                    filterContent = filterContent
                )
            }
        }
    }
}
