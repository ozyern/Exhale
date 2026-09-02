/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.ozyern.exhale.BuildConfig
import com.ozyern.exhale.R
import com.ozyern.exhale.ui.component.LiquidBackButton
import com.ozyern.exhale.ui.component.LiquidGlassIconButton
import com.ozyern.exhale.ui.component.IconButton
import com.ozyern.exhale.ui.component.TopSearch
import com.ozyern.exhale.ui.utils.backToMain
import com.ozyern.exhale.utils.Updater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val listState = rememberLazyListState()

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isStorageGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            notificationPermission == null ||
                ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isStorageGranted = result[storagePermission] == true || isStorageGranted
        if (notificationPermission != null) {
            isNotificationGranted = result[notificationPermission] == true || isNotificationGranted
        }
    }

    val shouldShowPermissionHint = !isStorageGranted || !isNotificationGranted
    val hasUpdate = Updater.hasUpdate(latestVersionName, BuildConfig.VERSION_NAME)

    val resetSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    val quickActions = buildQuickActions(navController, resetSearch)
    val integrationActions = buildIntegrationActions(navController, resetSearch)
    val settingsGroups = buildSettingsGroups(navController, isAndroid12OrLater, hasUpdate, context, resetSearch)
    val internalItems = buildInternalItems(navController, resetSearch)

    val queryText = query.text.trim()
    val showSearchBar = isSearching || queryText.isNotBlank()

    val filteredQuickActions = filterQuickActions(quickActions, queryText)
    val filteredIntegrations = filterIntegrations(integrationActions, queryText)
    val filteredGroups = filterSettingsGroups(settingsGroups, queryText)
    val filteredInternalItems = filterInternalItems(internalItems, queryText)

    val hasSearchResults by remember(
        filteredQuickActions,
        filteredGroups,
        filteredIntegrations,
        filteredInternalItems,
    ) {
        derivedStateOf {
            filteredQuickActions.isNotEmpty() ||
                filteredGroups.isNotEmpty() ||
                filteredIntegrations.isNotEmpty() ||
                filteredInternalItems.isNotEmpty()
        }
    }

    val internalGroup = if (filteredInternalItems.isNotEmpty()) {
        SettingsGroup(
            title = stringResource(R.string.internal_subcategory_settings),
            items = filteredInternalItems,
        )
    } else null

    val contentState = SettingsContentState(
        quickActions = if (queryText.isBlank()) quickActions else filteredQuickActions,
        integrations = if (queryText.isBlank()) integrationActions else filteredIntegrations,
        groups = if (queryText.isBlank()) settingsGroups else filteredGroups,
        internalGroup = if (queryText.isNotBlank()) internalGroup else null,
        showPermissionBanner = shouldShowPermissionHint,
        showUpdateBanner = hasUpdate,
        latestVersion = latestVersionName,
        isSearchActive = queryText.isNotBlank(),
        hasSearchResults = hasSearchResults,
        onRequestPermission = {
            val toRequest = buildList {
                if (!isStorageGranted) add(storagePermission)
                if (!isNotificationGranted && notificationPermission != null) {
                    add(notificationPermission)
                }
            }
            if (toRequest.isNotEmpty()) {
                permissionLauncher.launch(toRequest.toTypedArray())
            }
        },
        onUpdateClick = { navController.navigate("settings/update") },
        onAboutClick = { navController.navigate("settings/about") },
        onSearchClick = { isSearching = true },
    )

    // Solid, distinct page colour — the ground the inset groups float on. Hoisted so the app bar
    // can sit on exactly the same colour and the header reads as part of the page, not as a
    // separate elevated Material surface.
    val pageBackground = SettingsDimensions.screenBackgroundColor()

    Scaffold(
        topBar = {
            if (!showSearchBar) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings),
                            // iOS large-title weight. The type scale supplies the geometric face
                            // and tight tracking; the header just asks for the heavy cut.
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        LiquidBackButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain,
                            icon = R.drawable.arrow_back,
                        )
                    },
                    actions = {
                        // Same disc of glass as the back arrow opposite it. A bare Material icon
                        // here was the tell that the glass was decoration rather than the app's
                        // control surface: one side of the bar an object, the other a glyph.
                        LiquidGlassIconButton(
                            onClick = { isSearching = true },
                            icon = R.drawable.search,
                            contentDescription = stringResource(R.string.search),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    // Opaque and identical to the page in both states: no tonal-elevation shift
                    // on scroll, which is the single most "Android" tell a settings screen has.
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = pageBackground,
                        scrolledContainerColor = pageBackground,
                    ),
                )
            }
        },
        // Transparent, so the album-art wash `SettingsPage` lays down is what you see behind
        // the groups. The app bar above stays opaque on purpose: the large title has rows
        // sliding under it as the list scrolls, and a translucent bar there would show them.
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showSearchBar) {
                AdaptiveSettingsLayout(
                    state = contentState,
                    listState = listState,
                    topPadding = innerPadding.calculateTopPadding(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn(tween(durationMillis = 220)),
                exit = fadeOut(tween(durationMillis = 160)),
            ) {
                TopSearch(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { focusManager.clearFocus() },
                    active = showSearchBar,
                    onActiveChange = { active ->
                        if (active) {
                            isSearching = true
                        } else {
                            resetSearch()
                        }
                    },
                    placeholder = { Text(text = stringResource(R.string.search)) },
                    leadingIcon = {
                        LiquidBackButton(
                            onClick = { resetSearch() },
                            onLongClick = {
                                if (queryText.isBlank()) {
                                    navController.backToMain()
                                }
                            },
                            icon = R.drawable.arrow_back,
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (query.text.isNotBlank()) {
                                LiquidGlassIconButton(
                                    onClick = { query = TextFieldValue() },
                                    icon = R.drawable.close,
                                    diameter = 32.dp,
                                    iconSize = 16.dp,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    focusRequester = focusRequester,
                ) {
                    val searchState = contentState.copy(
                        isSearchActive = true,
                    )
                    AdaptiveSettingsLayout(
                        state = searchState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

