/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozyern.exhale.constants.ContentLanguageKey
import com.ozyern.exhale.constants.PreferredAudioLanguagesKey
import com.ozyern.exhale.innertube.YouTube
import com.ozyern.exhale.innertube.pages.ChartsPage
import com.ozyern.exhale.utils.ContentLanguageFilter
import com.ozyern.exhale.utils.dataStore
import com.ozyern.exhale.utils.getAsync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
    val chartsPage = _chartsPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * STRICT language enforcement (same policy as Home): charts are the single worst
     * geo-leak surface — "Trending"/"Top songs" come back region-localized regardless of
     * `hl`, so every shelf is passed through [ContentLanguageFilter] before it renders.
     */
    private suspend fun preferredLanguages(): Set<String> =
        ContentLanguageFilter.resolveLanguages(
            preferredCsv = context.dataStore.getAsync(PreferredAudioLanguagesKey, ""),
            contentLanguage = context.dataStore.getAsync(ContentLanguageKey, "system"),
        )

    fun loadCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val languages = preferredLanguages()
            YouTube.getChartsPage()
                .onSuccess { page ->
                    _chartsPage.value = page.copy(
                        sections = ContentLanguageFilter.filterChartSections(page.sections, languages),
                    )
                }
                .onFailure { e ->
                    _error.value = "Failed to load charts: ${e.message}"
                }

            _isLoading.value = false
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            _chartsPage.value?.continuation?.let { continuation ->
                _isLoading.value = true
                val languages = preferredLanguages()
                YouTube.getChartsPage(continuation)
                    .onSuccess { newPage ->
                        _chartsPage.value = _chartsPage.value?.copy(
                            sections = _chartsPage.value?.sections.orEmpty() +
                                    ContentLanguageFilter.filterChartSections(newPage.sections, languages),
                            continuation = newPage.continuation
                        )
                    }
                    .onFailure { e ->
                        _error.value = "Failed to load more: ${e.message}"
                    }
                _isLoading.value = false
            }
        }
    }
}
