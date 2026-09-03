/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import com.ozyern.exhale.ui.component.LiquidGlassIconButton
import com.ozyern.exhale.ui.component.TagsFilterChips
import com.ozyern.exhale.ui.component.rememberArtworkAmbientColors
import com.ozyern.exhale.utils.rememberEnumPreference
import com.ozyern.exhale.utils.rememberPreference

/**
 * The Library tab: one of six views, chosen by [ChipSortTypeKey].
 *
 * The chip row that used to sit at the top of every one of them is gone. It was doing two
 * incompatible jobs — naming where you were, and offering the five places you could go — and it
 * did the second one badly, because a selected chip and an unselected chip differ by a fill
 * colour. [LibraryMixScreen] now lists the destinations as rows, and each destination gets a real
 * sub-page header instead: a back button and its own title, which is what every other pushed
 * screen in the app looks like.
 */
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

    val backToLibrary = { filterType = LibraryFilter.LIBRARY }

    // A sub-view is a pushed page, so the system back gesture should pop it rather than leaving
    // the tab. Previously the only way back was to find and re-tap the lit chip.
    BackHandler(enabled = filterType != LibraryFilter.LIBRARY) { backToLibrary() }

    val subPageTitle = when (filterType) {
        LibraryFilter.PLAYLISTS -> R.string.filter_playlists
        LibraryFilter.SPOTIFY -> R.string.spotify
        else -> R.string.filter_library
    }

    // Passed to the sub-screens that take a header slot (Playlists, Spotify). Songs, Albums and
    // Artists render their own.
    val filterContent = @Composable {
        Column {
            LibrarySubPageHeader(
                title = stringResource(subPageTitle),
                onBack = backToLibrary,
            )

            if (showTagsInLibrary && filterType == LibraryFilter.PLAYLISTS) {
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
                navController = navController,
                onTabSelected = { filterType = it },
            )
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController, backToLibrary)
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController, backToLibrary)
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController, backToLibrary)
            LibraryFilter.SPOTIFY -> LibrarySpotifyPlaylistsScreen(
                navController = navController,
                filterContent = filterContent,
            )
        }
    }
}

/**
 * The header a library sub-view opens with: a round glass back button, then the view's name.
 *
 * The title is set one step below the Library page's own — a pushed page is subordinate to the
 * page that pushed it, and matching them at display size makes the stack read flat.
 */
@Composable
private fun LibrarySubPageHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
    ) {
        LiquidGlassIconButton(
            onClick = onBack,
            icon = R.drawable.arrow_back,
            contentDescription = stringResource(R.string.back),
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = (-0.025).em),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
