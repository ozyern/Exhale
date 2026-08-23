/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozyern.exhale.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The three library totals shown in the account sheet.
 *
 * Its own view model rather than three more fields on `HomeViewModel`: the sheet is the only thing
 * that wants these, and hanging them off the home screen's model would keep three database
 * observers alive for the whole session to serve a panel that is open for four seconds.
 *
 * `WhileSubscribed` with a short grace period is the point — the queries run while the sheet is
 * on screen and stop shortly after it closes, and the grace period is what stops a configuration
 * change from tearing all three down and starting them again.
 */
@HiltViewModel
class AccountLibraryViewModel @Inject constructor(
    database: MusicDatabase,
) : ViewModel() {
    val songCount: StateFlow<Int> = database.librarySongsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val artistCount: StateFlow<Int> = database.libraryArtistsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val albumCount: StateFlow<Int> = database.libraryAlbumsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
