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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.ozyern.exhale.BuildConfig
import com.ozyern.exhale.LocalPlayerAwareWindowInsets
import com.ozyern.exhale.R
import com.ozyern.exhale.constants.EnableUpdateNotificationKey
import com.ozyern.exhale.constants.AquamorphicDampingRatio
import com.ozyern.exhale.constants.AquamorphicStiffness
import com.ozyern.exhale.constants.UpdateChannel
import com.ozyern.exhale.constants.UpdateChannelKey
import com.ozyern.exhale.ui.component.EnumListPreference
import com.ozyern.exhale.ui.component.IconButton
import com.ozyern.exhale.ui.component.LiquidGlassSheet
import com.ozyern.exhale.ui.component.PreferenceGroupTitle
import com.ozyern.exhale.ui.component.SettingsDividerThickness
import com.ozyern.exhale.ui.component.SettingsGroupCornerRadius
import com.ozyern.exhale.ui.component.SwitchPreference
import com.ozyern.exhale.ui.component.liquid.LiquidButton
import com.ozyern.exhale.ui.component.settingsDividerColor
import com.ozyern.exhale.ui.utils.backToMain
import com.ozyern.exhale.utils.DownloadProgress
import com.ozyern.exhale.utils.GitCommit
import com.ozyern.exhale.utils.UpdateInfo
import com.ozyern.exhale.utils.UpdateNotificationManager
import com.ozyern.exhale.utils.Updater
import com.ozyern.exhale.utils.rememberEnumPreference
import com.ozyern.exhale.utils.rememberPreference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ─── Estado de comprobación de actualización ──────────────────────────────────

private sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Loading : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

// ─── Pantalla ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var nightlyInstallUrl by remember {
        mutableStateOf("https://pub-2218e6bbd5b948e1b5d882cf4d92086d.r2.dev/app-universal-release.apk")
    }

    val (enableUpdateNotification, onEnableUpdateNotificationChange) = rememberPreference(
        EnableUpdateNotificationKey, defaultValue = false
    )
    val (updateChannel, onUpdateChannelChange) = rememberEnumPreference(
        UpdateChannelKey, defaultValue = UpdateChannel.STABLE
    )

    var commits by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var isLoadingCommits by remember { mutableStateOf(true) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(true) }

    var updateCheckState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var showNightlyConfirmDialog by remember { mutableStateOf(false) }
    var showNotifConfirmDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            onEnableUpdateNotificationChange(true)
            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
        }
    }

    // ── Función de comprobación ───────────────────────────────────────────────
    fun checkForUpdate() {
        if (updateCheckState == UpdateCheckState.Loading) return
        coroutineScope.launch {
            updateCheckState = UpdateCheckState.Loading
            Updater.checkForUpdate(BuildConfig.VERSION_NAME)
                .onSuccess { info ->
                    if (info == null) {
                        updateCheckState = UpdateCheckState.UpToDate
                    } else {
                        pendingUpdateInfo = info
                        updateCheckState = UpdateCheckState.UpdateAvailable(info)
                        showUpdateDialog = true
                    }
                }
                .onFailure { err ->
                    updateCheckState = UpdateCheckState.Error(err.message ?: "Error desconocido")
                }
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────
    if (showUpdateDialog) {
        pendingUpdateInfo?.let { info ->
            UpdateAvailableSheet(
                info = info,
                onViewRelease = { showUpdateDialog = false; uriHandler.openUri(info.releasePageUrl) },
                onDismiss     = { showUpdateDialog = false }
            )
        }
    }

    if (showNotifConfirmDialog) {
        BuildChannelInfoDialog(
            title = stringResource(R.string.enable_update_notification),
            onConfirm = {
                showNotifConfirmDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else {
                    onEnableUpdateNotificationChange(true)
                    UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
                }
            },
            onDismiss = { showNotifConfirmDialog = false }
        )
    }

    if (showNightlyConfirmDialog) {
        BuildChannelInfoDialog(
            title = stringResource(R.string.channel_nightly),
            onConfirm = { showNightlyConfirmDialog = false; onUpdateChannelChange(UpdateChannel.NIGHTLY) },
            onDismiss = { showNightlyConfirmDialog = false }
        )
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Updater.getLatestVersionName().onSuccess { latestVersion = it }
            Updater.getCommitHistory(30).onSuccess { commits = it }.onFailure { commits = emptyList() }
            isLoadingCommits = false
        }
    }

    LaunchedEffect(updateChannel) {
        if (updateChannel == UpdateChannel.NIGHTLY) {
            coroutineScope.launch {
                Updater.getLatestReleaseInfo().onSuccess { info ->
                    nightlyInstallUrl = info.htmlUrl
                }
            }
        } else {
            nightlyInstallUrl = "https://pub-2218e6bbd5b948e1b5d882cf4d92086d.r2.dev/app-universal-release.apk"
        }
    }

    val rotationAngle by animateFloatAsState(if (isExpanded) 180f else 0f, label = "rotation")

    // ── Layout ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.updates)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) { Icon(painterResource(R.drawable.arrow_back), null) }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 16.dp)
        ) {

            // ── Tarjeta versión + botón comprobar ─────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.update),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.current_version),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BuildConfig.VERSION_NAME,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                latestVersion?.let { latest ->
                                    if (Updater.hasUpdate(latest, BuildConfig.VERSION_NAME)) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.latest_version_format, latest),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Botón con estado animado
                        UpdateCheckButton(
                            state = updateCheckState,
                            onClick = ::checkForUpdate
                        )

                        // Feedback textual bajo el botón
                        AnimatedVisibility(
                            visible = updateCheckState == UpdateCheckState.UpToDate ||
                                    updateCheckState is UpdateCheckState.Error
                        ) {
                            val (msg, color) = when (val s = updateCheckState) {
                                UpdateCheckState.UpToDate  ->
                                    "You already have the latest version." to MaterialTheme.colorScheme.tertiary
                                is UpdateCheckState.Error  ->
                                    "Error: ${s.message}" to MaterialTheme.colorScheme.error
                                else -> "" to MaterialTheme.colorScheme.onSurface
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Preferencias ──────────────────────────────────────────────────
            item { PreferenceGroupTitle(title = stringResource(R.string.notification_settings)) }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_update_notification)) },
                    description = stringResource(R.string.enable_update_notification_desc),
                    icon = { Icon(painterResource(R.drawable.new_release), null) },
                    checked = enableUpdateNotification,
                    onCheckedChange = { enabled ->
                        if (enabled) showNotifConfirmDialog = true
                        else {
                            onEnableUpdateNotificationChange(false)
                            UpdateNotificationManager.cancelPeriodicUpdateCheck(context)
                        }
                    }
                )
            }

            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.update_channel)) },
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    selectedValue = updateChannel,
                    valueText = { ch ->
                        when (ch) {
                            UpdateChannel.STABLE  -> stringResource(R.string.channel_stable)
                            UpdateChannel.NIGHTLY -> stringResource(R.string.channel_nightly)
                        }
                    },
                    onValueSelected = { ch ->
                        if (ch == UpdateChannel.NIGHTLY && updateChannel != UpdateChannel.NIGHTLY)
                            showNightlyConfirmDialog = true
                        else
                            onUpdateChannelChange(ch)
                    }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("settings/changelog") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.update), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.view_changelog))
                }
            }

            // ── Card nightly ──────────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = updateChannel == UpdateChannel.NIGHTLY) {
                    val latestHash = commits.firstOrNull()?.sha ?: "—"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Nightly Builds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Latest features and fixes from the development branch. May contain experimental features and occasional bugs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(latestHash, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(14.dp))
                            // Routed through the same sheet as a stable release, so the nightly
                            // channel gets in-app download and install too rather than dropping
                            // the user into a browser.
                            Button(
                                onClick = {
                                    pendingUpdateInfo = UpdateInfo(
                                        tagName = "nightly",
                                        versionName = latestHash,
                                        downloadUrl = nightlyInstallUrl,
                                        releasePageUrl = nightlyInstallUrl,
                                        releaseNotes = null,
                                        publishedAt = "",
                                    )
                                    showUpdateDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Install")
                            }
                        }
                    }
                }
            }

            // ── Historial de commits ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                PreferenceGroupTitle(title = stringResource(R.string.commit_history))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.history), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.recent_commits), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            }
                            Icon(painterResource(R.drawable.expand_more), null, modifier = Modifier.rotate(rotationAngle))
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                if (isLoadingCommits) {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isExpanded && !isLoadingCommits) {
                items(commits) { commit ->
                    CommitItem(commit = commit, onClick = { uriHandler.openUri(commit.url) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── UpdateCheckButton ────────────────────────────────────────────────────────

@Composable
private fun UpdateCheckButton(
    state: UpdateCheckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = state != UpdateCheckState.Loading,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (state) {
                UpdateCheckState.UpToDate           -> MaterialTheme.colorScheme.tertiary
                is UpdateCheckState.Error           -> MaterialTheme.colorScheme.error
                else                                -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        AnimatedContent(targetState = state, label = "btnContent") { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                when (s) {
                    UpdateCheckState.Loading -> {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Comprobando…")
                    }
                    UpdateCheckState.UpToDate -> {
                        Icon(painterResource(R.drawable.done), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Up to date")
                    }
                    is UpdateCheckState.UpdateAvailable -> {
                        Icon(painterResource(R.drawable.update), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View update (${s.info.versionName})")
                    }
                    is UpdateCheckState.Error -> {
                        Text("Reintentar")
                    }
                    else -> {
                        Icon(painterResource(R.drawable.update), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.updates))
                    }
                }
            }
        }
    }
}

// ─── UpdateAvailableSheet ─────────────────────────────────────────────────────

/** What the sheet's primary action is currently doing. */
private sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: DownloadProgress) : DownloadState
    data object Installing : DownloadState
    data object NeedsPermission : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * The "Software Update" sheet — Apple's presentation of an available update, in our glass.
 *
 * Replaces an `AlertDialog` whose only action kicked the user out to a browser to download the APK
 * by hand. The download now happens in place, which is the whole point of the ported backend: the
 * sheet owns the transfer, shows real byte progress, and hands the finished file straight to the
 * system installer.
 */
@Composable
private fun UpdateAvailableSheet(
    info: UpdateInfo,
    onViewRelease: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    val isBusy = state is DownloadState.Downloading || state is DownloadState.Installing

    fun startDownload() {
        coroutineScope.launch {
            // Fixed filename in the cache dir: it is covered by the <cache-path> entry in
            // provider_paths.xml (so FileProvider can hand it to the installer), and reusing one
            // name means a previous abandoned download cannot accumulate copies of a ~100 MB APK.
            val destination = File(context.cacheDir, "update.apk")
            state = DownloadState.Downloading(DownloadProgress(0L, -1L))
            runCatching {
                Updater.downloadApk(info.downloadUrl, destination).collect { progress ->
                    state = DownloadState.Downloading(progress)
                }
            }.onSuccess {
                state = DownloadState.Installing
                state = if (Updater.installApk(context, destination)) {
                    DownloadState.Installing
                } else {
                    DownloadState.NeedsPermission
                }
            }.onFailure { error ->
                state = DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    LiquidGlassSheet(
        onDismiss = onDismiss,
        // A half-written APK is worse than no APK, so the sheet refuses every dismissal route
        // while bytes are moving — scrim tap, back press and the grab handle all go away.
        dismissible = !isBusy,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            UpdateSheetHeader(versionName = info.versionName)

            Spacer(Modifier.height(22.dp))

            // Inset grouped table, the same language as Settings: two rows, one hairline between
            // them, never at the card's outer edges.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsGroupCornerRadius))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            ) {
                UpdateVersionRow(
                    label = stringResource(R.string.update_installed_version),
                    value = BuildConfig.VERSION_NAME,
                    highlight = false,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = SettingsDividerThickness,
                    color = settingsDividerColor(),
                )
                UpdateVersionRow(
                    label = stringResource(R.string.update_new_version),
                    value = info.versionName,
                    highlight = true,
                )
            }

            if (!info.releaseNotes.isNullOrBlank()) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.update_whats_new),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SettingsGroupCornerRadius))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(
                        text = info.releaseNotes.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            UpdateSheetActions(
                state = state,
                onDownload = ::startDownload,
                onGrantPermission = {
                    Updater.requestInstallPermission(context)
                    state = DownloadState.Idle
                },
                onViewRelease = onViewRelease,
                onDismiss = onDismiss,
            )
        }
    }
}

/** Hero block: a large circular glyph over bold identity text, centred — the iOS sheet opening. */
@Composable
private fun UpdateSheetHeader(versionName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.update),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.update_available_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.update_version_label, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpdateVersionRow(
    label: String,
    value: String,
    highlight: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun UpdateSheetActions(
    state: DownloadState,
    onDownload: () -> Unit,
    onGrantPermission: () -> Unit,
    onViewRelease: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "updateAction",
        ) { s ->
            when (s) {
                is DownloadState.Downloading -> UpdateProgressBlock(s.progress)

                DownloadState.Installing -> Text(
                    text = stringResource(R.string.update_opening_installer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                DownloadState.NeedsPermission -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.update_install_permission_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    LiquidButton(
                        onClick = onGrantPermission,
                        modifier = Modifier.fillMaxWidth(),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.update_open_settings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                is DownloadState.Failed -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    LiquidButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.update_retry),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                DownloadState.Idle -> LiquidButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_download_and_install),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Secondary actions stay hidden while the transfer is in flight — there is nothing safe
        // to do at that point except wait.
        AnimatedVisibility(
            visible = state !is DownloadState.Downloading && state != DownloadState.Installing,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onViewRelease) {
                    Text(stringResource(R.string.update_view_release))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_not_now))
                }
            }
        }
    }
}

/**
 * Apple-style transfer readout: a hairline capsule track with a spring-eased fill, the byte count
 * underneath, and the percentage on the right.
 *
 * The fill is animated rather than snapped to each emission — progress arrives every 512 KB, so a
 * raw binding would visibly step. Falls back to a full-width shimmer-free indeterminate bar when
 * the server withheld a Content-Length.
 */
@Composable
private fun UpdateProgressBlock(progress: DownloadProgress) {
    val fraction = progress.fraction
    val animatedFraction by animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = spring(
            dampingRatio = AquamorphicDampingRatio,
            stiffness = AquamorphicStiffness,
        ),
        label = "downloadFill",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
        ) {
            if (fraction == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (progress.totalBytes > 0L) {
                    stringResource(
                        R.string.update_downloaded_of,
                        formatBytes(progress.downloadedBytes),
                        formatBytes(progress.totalBytes),
                    )
                } else {
                    formatBytes(progress.downloadedBytes)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (fraction != null) {
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

// ─── BuildChannelInfoDialog ───────────────────────────────────────────────────

@Composable
private fun BuildChannelInfoDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Exhale provides two download channels for builds:", style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("• Stable builds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Distributed via official GitHub Releases.", style = MaterialTheme.typography.bodySmall)
                    Text("These versions are tested and recommended for most users.", style = MaterialTheme.typography.bodySmall)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("• Nightly builds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Automatically generated development builds hosted via nightly.link.", style = MaterialTheme.typography.bodySmall)
                    Text("Nightly builds may include experimental features, unfinished changes, or temporary regressions.", style = MaterialTheme.typography.bodySmall)
                }
                Text("By continuing, you acknowledge that nightly builds may be unstable and use them at your own risk.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

// ─── CommitItem ───────────────────────────────────────────────────────────────

@Composable
private fun CommitItem(commit: GitCommit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        // The grouped-list card colour, not `surface` — on a settings page `surface` IS the
        // page, so a card painted with it would be invisible.
        colors = CardDefaults.cardColors(containerColor = SettingsDimensions.groupSurfaceColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(commit.message, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(commit.sha, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(commit.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (commit.date.isNotEmpty()) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCommitDate(commit.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(painterResource(R.drawable.arrow_forward), null, Modifier.padding(start = 8.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

private fun formatCommitDate(isoDate: String): String = try {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    SimpleDateFormat("MMM d", Locale.getDefault()).format(inputFormat.parse(isoDate)!!)
} catch (e: Exception) {
    isoDate.take(10)
}
