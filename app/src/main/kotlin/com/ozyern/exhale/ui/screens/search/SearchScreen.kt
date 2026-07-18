/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ozyern.exhale.LocalPlayerAwareWindowInsets
import com.ozyern.exhale.constants.PureBlackKey
import com.ozyern.exhale.utils.rememberPreference
import java.net.URLEncoder

/**
 * The Search TAB — a first-class, peer-level NavHost destination (route "search"), sibling to
 * Home/Library, NOT a dialog/overlay.
 *
 * BOTTOM CHROME: the Search route uses a COMPLETELY different navigation layout from Home.
 * MainActivity swaps the morphing A/B [LiquidGlassBottomBar] for the fixed
 * [com.ozyern.exhale.ui.component.SearchBottomBar] (a standalone circular Home pill + a
 * separate search-input capsule) while this screen is on top, and disables the
 * scroll-collapse morph entirely. The mini-player floats directly above that bar. This
 * screen therefore renders ONLY the category grid — no docked pill of its own.
 */
@Composable
fun SearchScreen(
    navController: NavController,
) {
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)
    val layoutDirection = LocalLayoutDirection.current
    // Player-aware insets already include the top app bar, nav bar and mini-player heights,
    // so the grid never hides behind the floating chrome (incl. the unified search bar).
    val basePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        BrowseCategoriesGrid(
            pureBlack = pureBlack,
            // The main top bar already carries the account entry on tab destinations, so the
            // in-grid avatar is hidden and the header is a pure, huge "Search" title.
            showProfile = false,
            onProfileClick = { navController.navigate("settings") },
            onCategoryClick = { category ->
                navController.navigate("search/${URLEncoder.encode(category, "UTF-8")}")
            },
            contentPadding = PaddingValues(
                start = basePadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = basePadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = basePadding.calculateTopPadding() + 4.dp,
                bottom = basePadding.calculateBottomPadding() + 16.dp,
            ),
        )
    }
}
