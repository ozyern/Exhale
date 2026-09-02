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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ozyern.exhale.LocalDatabase
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.ChipSortTypeKey
import com.ozyern.exhale.constants.DisableBlurKey
import com.ozyern.exhale.constants.LibraryFilter
import com.ozyern.exhale.constants.PlaylistTagsFilterKey
import com.ozyern.exhale.constants.ShowTagsInLibraryKey
import com.ozyern.exhale.models.MediaMetadata
import com.ozyern.exhale.ui.component.AmbientArtworkGlow
import com.ozyern.exhale.ui.component.ChipsRow
import com.ozyern.exhale.ui.component.TagsFilterChips
import com.ozyern.exhale.ui.component.liquidGlassSurface
import com.ozyern.exhale.ui.component.rememberArtworkAmbientColors
import com.ozyern.exhale.utils.rememberEnumPreference
import com.ozyern.exhale.utils.rememberPreference
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf

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
            // The filter row sits on a glass plate rather than loose on the page.
            //
            // Chips on a bare surface are five separate objects with nothing holding them
            // together, and against the colour wash behind them they had no ground of their own to
            // sit on. One plate makes the row a single control — which is what it is — and gives
            // the wash something to be seen *through*, which is the entire point of the material.
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .liquidGlassSurface(RoundedCornerShape(22.dp))
                    .padding(vertical = 6.dp),
            ) {
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

    // Library takes the same ambient wash as Home and the player: the colour of whatever is
    // playing, bleeding down from the top.
    //
    // This replaces a three-blob mesh built from the *theme's* primary/secondary/tertiary. That
    // version was fixed — the same wash under every song, on a page where nothing else moved
    // either — and it was the only surface in the app painting its own background instead of
    // reacting to the record. Sharing the component means Library cannot drift from Home again,
    // and the glass on top of it finally has something worth revealing.
    // Not `?: return` as Home does: Library is reachable before the playback service has bound,
    // and a blank tab is a worse failure than a wash that has not arrived yet.
    val mediaMetadata by LocalPlayerConnection.current?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf<MediaMetadata?>(null) }
    val ambientColors by rememberArtworkAmbientColors(
        songId = mediaMetadata?.id,
        thumbnailUrl = mediaMetadata?.thumbnailUrl,
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!disableBlur) {
            AmbientArtworkGlow(
                colors = ambientColors,
                modifier = Modifier.fillMaxSize(),
            )
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
