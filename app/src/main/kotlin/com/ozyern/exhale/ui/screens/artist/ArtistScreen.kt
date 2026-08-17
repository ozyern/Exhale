/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.ozyern.exhale.LocalDatabase
import com.ozyern.exhale.LocalPlayerAwareWindowInsets
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.canvas.providers.AppleMusicArtistBackgroundProvider
import com.ozyern.exhale.constants.AppBarHeight
import com.ozyern.exhale.constants.DisableBlurKey
import com.ozyern.exhale.constants.HideExplicitKey
import com.ozyern.exhale.db.entities.ArtistEntity
import com.ozyern.exhale.extensions.togglePlayPause
import com.ozyern.exhale.extensions.toMediaItem
import com.ozyern.exhale.innertube.models.AlbumItem
import com.ozyern.exhale.innertube.models.ArtistItem
import com.ozyern.exhale.innertube.models.PlaylistItem
import com.ozyern.exhale.innertube.models.SongItem
import com.ozyern.exhale.innertube.models.WatchEndpoint
import com.ozyern.exhale.models.toMediaMetadata
import com.ozyern.exhale.playback.queues.ListQueue
import com.ozyern.exhale.playback.queues.YouTubeQueue
import com.ozyern.exhale.ui.component.AlbumGridItem
import com.ozyern.exhale.ui.component.HideOnScrollFAB
import com.ozyern.exhale.ui.component.IconButton
import com.ozyern.exhale.ui.component.LocalMenuState
import com.ozyern.exhale.ui.component.NavigationTitle
import com.ozyern.exhale.ui.component.SongListItem
import com.ozyern.exhale.ui.component.YouTubeGridItem
import com.ozyern.exhale.ui.component.YouTubeListItem
import com.ozyern.exhale.ui.component.shimmer.ButtonPlaceholder
import com.ozyern.exhale.ui.component.shimmer.ListItemPlaceHolder
import com.ozyern.exhale.ui.component.shimmer.ShimmerHost
import com.ozyern.exhale.ui.component.shimmer.TextPlaceholder
import com.ozyern.exhale.ui.menu.AlbumMenu
import com.ozyern.exhale.ui.menu.SongMenu
import com.ozyern.exhale.ui.menu.YouTubeAlbumMenu
import com.ozyern.exhale.ui.menu.YouTubeArtistMenu
import com.ozyern.exhale.ui.menu.YouTubePlaylistMenu
import com.ozyern.exhale.ui.menu.YouTubeSongMenu
import com.ozyern.exhale.ui.player.CanvasArtworkPlayer
import com.ozyern.exhale.ui.theme.PlayerColorExtractor
import com.ozyern.exhale.ui.utils.backToMain
import com.ozyern.exhale.ui.utils.resize
import com.ozyern.exhale.utils.rememberPreference
import com.ozyern.exhale.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer

// Sealed class for video background state
sealed class VideoBackgroundState {
    object Loading : VideoBackgroundState()
    object Empty : VideoBackgroundState()
    data class Success(val url: String) : VideoBackgroundState()
    data class Error(val message: String) : VideoBackgroundState()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // System bars padding
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    // Gradient colors for mesh background
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Get thumbnail URL
    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl

    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

    val storefront = remember {
        java.util.Locale.getDefault()
            .country
            .takeIf { it.length == 2 }
            ?.lowercase(java.util.Locale.ROOT)
            ?: "us"
    }

    // Enhanced video background state with error handling and loading state
    val videoBackgroundState by produceState<VideoBackgroundState>(
        initialValue = VideoBackgroundState.Loading,
        artistName, storefront
    ) {
        value = if (!artistName.isNullOrBlank()) {
            try {
                val url = withContext(Dispatchers.IO) {
                    AppleMusicArtistBackgroundProvider.getByArtistName(
                        artistName = artistName,
                        storefront = storefront,
                    )
                }
                if (url.isNullOrBlank()) {
                    VideoBackgroundState.Empty
                } else {
                    // Small delay to ensure smooth transition
                    delay(100)
                    VideoBackgroundState.Success(url)
                }
            } catch (e: Exception) {
                VideoBackgroundState.Error(e.message ?: "Failed to load background video")
            }
        } else {
            VideoBackgroundState.Empty
        }
    }

    // Extract gradient colors from artist image
    LaunchedEffect(thumbnail) {
        if (thumbnail != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnail)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request)
            }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColors = extractedColors
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Calculate gradient opacity based on scroll position
    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 800f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(libraryArtist) {
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        // Video background with state handling
        when (val state = videoBackgroundState) {
            is VideoBackgroundState.Success -> {
                CanvasArtworkPlayer(
                    primaryUrl = state.url,
                    fallbackUrl = null,
                    isPlaying = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(-2f),
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                )
            }

            is VideoBackgroundState.Loading -> {
                // Optional: Show subtle loading indicator or just transparent background
                // For better UX, we show nothing and let the gradient/blur handle it
            }

            is VideoBackgroundState.Error, is VideoBackgroundState.Empty -> {
                // No video, just use gradient background
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    surfaceColor.copy(alpha = 0.55f)
                )
        )

        if (!disableBlur && gradientColors.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.65f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height

                        // Draw-phase read: `gradientAlpha` tracks the scroll offset, so testing it
                        // up in composition invalidated this whole screen on every scrolled pixel.
                        // Bailing here instead keeps the check but pays for it in the draw pass.
                        if (gradientAlpha <= 0f) return@drawBehind

                        if (gradientColors.size >= 3) {
                            val c0 = gradientColors[0]
                            val c1 = gradientColors[1]
                            val c2 = gradientColors[2]
                            val c3 = gradientColors.getOrElse(3) { c0 }
                            val c4 = gradientColors.getOrElse(4) { c1 }
                            // Primary color blob - top center
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c0.copy(alpha = gradientAlpha * 0.72f),
                                        c0.copy(alpha = gradientAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.2f),
                                    radius = width * 0.7f
                                )
                            )

                            // Secondary color blob - top left
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c1.copy(alpha = gradientAlpha * 0.56f),
                                        c1.copy(alpha = gradientAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.15f, height * 0.35f),
                                    radius = width * 0.6f
                                )
                            )

                            // Third color blob - right side
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c2.copy(alpha = gradientAlpha * 0.52f),
                                        c2.copy(alpha = gradientAlpha * 0.26f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.85f, height * 0.45f),
                                    radius = width * 0.65f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c3.copy(alpha = gradientAlpha * 0.34f),
                                        c3.copy(alpha = gradientAlpha * 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.35f, height * 0.6f),
                                    radius = width * 0.8f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c4.copy(alpha = gradientAlpha * 0.28f),
                                        c4.copy(alpha = gradientAlpha * 0.14f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.55f, height * 0.85f),
                                    radius = width * 0.95f
                                )
                            )
                        } else if (gradientColors.isNotEmpty()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.6f),
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.3f),
                                    radius = width * 0.8f
                                )
                            )
                        }

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                                    surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                                    surfaceColor
                                ),
                                startY = height * 0.4f,
                                endY = height
                            )
                        )
                    }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                // Shimmer loading state
                item(key = "shimmer") {
                    ShimmerHost {
                        // Hero section placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = systemBarsTopPadding + AppBarHeight)
                        ) {
                            // Artist image placeholder - circular
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(210.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .shimmer()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Artist name placeholder
                            TextPlaceholder(
                                height = 32.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats placeholder
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(3) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        TextPlaceholder(
                                            height = 20.dp,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TextPlaceholder(
                                            height = 14.dp,
                                            modifier = Modifier.width(50.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Buttons placeholder
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                            ) {
                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                )
                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Songs list placeholder
                        repeat(5) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                // Hero Header
                item(key = "header") {
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = systemBarsTopPadding + AppBarHeight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Artist portrait. Sized down from 210dp and given a soft rim: at 210 it ate
                        // the whole first screen and, sitting directly on the colour wash with no
                        // edge of its own, it bled into the gradient behind it instead of reading
                        // as a portrait on top of one.
                        Box(
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val rimBrush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            )
                            if (thumbnail != null) {
                                AsyncImage(
                                    model = thumbnail.resize(600, 600),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(172.dp)
                                        .shadow(22.dp, CircleShape, clip = false)
                                        .clip(CircleShape)
                                        .border(1.5.dp, rimBrush, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(172.dp)
                                        .shadow(22.dp, CircleShape, clip = false)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.5.dp, rimBrush, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Artist Name
                        Text(
                            text = artistName ?: stringResource(R.string.unknown_artist),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        // Artist Description (expandable)
                        //
                        // Left-aligned, and truncated by LINE COUNT rather than by character count.
                        // Centring a paragraph is fine for one line and hostile past that — every
                        // line starts at a different x, so the eye has to hunt for the start of the
                        // next one, and this block routinely runs to eight or ten lines expanded.
                        // The old `description.take(100)` also cut mid-word and then appended its
                        // own ellipsis on top of an `overflow = Ellipsis` that was already doing the
                        // job, so a clipped bio could end in two ellipses.
                        val description = artistPage?.description
                        if (!description.isNullOrBlank()) {
                            var isExpanded by rememberSaveable { mutableStateOf(false) }
                            var isTruncated by remember(description) { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 14.dp)
                                    .combinedClickable(
                                        onClick = { isExpanded = !isExpanded },
                                        onLongClick = {}
                                    ),
                            ) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    // The only reliable way to know whether the text actually
                                    // overflowed at this width; guessing from `length` shows a
                                    // "More" link on bios that already fit.
                                    onTextLayout = { result ->
                                        if (!isExpanded) isTruncated = result.hasVisualOverflow
                                    },
                                )

                                if (isTruncated || isExpanded) {
                                    Text(
                                        text = stringResource(
                                            if (isExpanded) R.string.less else R.string.more
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }

                        // Counts, as one quiet caption line under the name.
                        //
                        // They used to be a `SpaceEvenly` row of `titleLarge` bold numerals with
                        // labels beneath — a stats panel of the kind a dashboard has. On an artist
                        // page the counts are context, not content: they belong at caption weight
                        // beside the name, and reclaiming that band of vertical space is what lets
                        // the actual music start above the fold.
                        run {
                            // Songs count - sum all SongItem instances across all sections
                            val songSections = artistPage?.sections?.filter { section ->
                                section.items.any { it is SongItem }
                            }
                            val songCount = if (showLocal) {
                                librarySongs.size
                            } else {
                                songSections
                                    ?.flatMap { it.items }
                                    ?.filterIsInstance<SongItem>()
                                    ?.distinctBy { it.id }
                                    ?.size ?: librarySongs.size
                            }
                            // Check if any song section has moreEndpoint (meaning there are more songs)
                            val hasMoreSongs = !showLocal && songSections?.any { it.moreEndpoint != null } == true

                            // Albums count - sum all AlbumItem instances across all sections
                            val albumSections = artistPage?.sections?.filter { section ->
                                section.items.any { it is AlbumItem }
                            }
                            val albumCount = if (showLocal) {
                                libraryAlbums.size
                            } else {
                                albumSections
                                    ?.flatMap { it.items }
                                    ?.filterIsInstance<AlbumItem>()
                                    ?.distinctBy { it.id }
                                    ?.size ?: libraryAlbums.size
                            }
                            // Check if any album section has moreEndpoint (meaning there are more albums)
                            val hasMoreAlbums = !showLocal && albumSections?.any { it.moreEndpoint != null } == true

                            val parts = buildList {
                                if (songCount > 0) {
                                    val n = if (hasMoreSongs) "$songCount+" else songCount.toString()
                                    add("$n ${stringResource(R.string.songs)}")
                                }
                                if (albumCount > 0) {
                                    val n = if (hasMoreAlbums) "$albumCount+" else albumCount.toString()
                                    add("$n ${stringResource(R.string.albums)}")
                                }
                            }

                            if (parts.isNotEmpty()) {
                                Text(
                                    text = parts.joinToString("  ·  "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }

                        // One action row instead of three buttons in two rows across three
                        // different Material styles (filled-tonal, filled, outlined). There is
                        // exactly one primary thing to do on an artist page — play them — so that
                        // is the only button carrying a label; subscribing and starting a radio are
                        // secondary, and secondary actions read better as glyphs beside the primary
                        // than as full-width buttons stacked under it.
                        val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                        val shuffleEnabled = if (showLocal) {
                            librarySongs.isNotEmpty()
                        } else {
                            artistPage?.artist?.shuffleEndpoint != null
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 20.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    if (!showLocal) {
                                        artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                                            playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                        }
                                    } else if (librarySongs.isNotEmpty()) {
                                        val shuffledSongs = librarySongs.shuffled()
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                items = shuffledSongs.map { it.toMediaItem() }
                                            )
                                        )
                                    }
                                },
                                enabled = shuffleEnabled,
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.shuffle),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            ArtistActionButton(
                                iconRes = if (isSubscribed) R.drawable.done else R.drawable.add,
                                contentDescription = stringResource(
                                    if (isSubscribed) R.string.subscribed else R.string.subscribe
                                ),
                                active = isSubscribed,
                                onClick = {
                                    database.transaction {
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            update(artist.toggleLike())
                                        } else {
                                            artistPage?.artist?.let {
                                                insert(
                                                    ArtistEntity(
                                                        id = it.id,
                                                        name = it.title,
                                                        channelId = it.channelId,
                                                        thumbnailUrl = it.thumbnail,
                                                    ).toggleLike()
                                                )
                                            }
                                        }
                                    }
                                },
                            )

                            if (!showLocal) {
                                artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                    ArtistActionButton(
                                        iconRes = R.drawable.radio,
                                        contentDescription = stringResource(R.string.radio),
                                        active = false,
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Content sections
                if (showLocal) {
                    // Local Songs Section
                    if (librarySongs.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        val filteredLibrarySongs = if (hideExplicit) {
                            librarySongs.filter { !it.song.explicit }
                        } else {
                            librarySongs
                        }

                        itemsIndexed(
                            items = filteredLibrarySongs.take(5),
                            key = { index, item -> "local_song_${item.id}_$index" }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                        onLongClick = {},
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = libraryArtist?.artist?.name
                                                            ?: "Unknown Artist",
                                                        items = librarySongs.map { it.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }

                        // Show "View All" if more songs available
                        if (filteredLibrarySongs.size > 5) {
                            item {
                                Surface(
                                    onClick = {
                                        navController.navigate("artist/${viewModel.artistId}/songs")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.view_all),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Local Albums Section
                    if (libraryAlbums.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.albums),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/albums")
                                }
                            )
                        }

                        item {
                            val filteredLibraryAlbums = if (hideExplicit) {
                                libraryAlbums.filter { !it.album.explicit }
                            } else {
                                libraryAlbums
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { album -> "local_album_${album.id}_${filteredLibraryAlbums.indexOf(album)}" }
                                ) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = album,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // YouTube/Remote content sections
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item {
                                NavigationTitle(
                                    title = section.title,
                                    onClick = section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}&params=${it.params}",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            // Song items with album info - display as list
                            items(
                                items = section.items.distinctBy { it.id },
                                key = { "youtube_song_${it.id}" },
                            ) { song ->
                                YouTubeListItem(
                                    item = song as SongItem,
                                    isActive = mediaMetadata?.id == song.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                            onLongClick = {},
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song.id),
                                                            song.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        } else {
                            // Grid items (albums, playlists, etc.)
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = {
                                            val type = when (it) {
                                                is SongItem -> "song"
                                                is AlbumItem -> "album"
                                                is ArtistItem -> "artist"
                                                is PlaylistItem -> "playlist"
                                                else -> "item"
                                            }
                                            "youtube_${type}_${it.id}"
                                        },
                                    ) { item ->
                                        YouTubeGridItem(
                                            item = item,
                                            isActive = when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    ),
                                                                )

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate(
                                                                "artist/${item.id}"
                                                            )

                                                            is PlaylistItem -> navController.navigate(
                                                                "online_playlist/${item.id}"
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
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
                                                    },
                                                )
                                                .animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // FAB for switching between local/remote view
        HideOnScrollFAB(
            visible = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }


    // Top App Bar
    TopAppBar(
        title = {
            val animatedAlpha by animateFloatAsState(
                targetValue = if (!transparentAppBar) 1f else 0f,
                animationSpec = tween(200),
                label = "titleAlpha"
            )
            Text(
                text = artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: "",
                modifier = Modifier.alpha(animatedAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            // Share/Copy link button
            IconButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {},
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }

            // Share button
            IconButton(
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            viewModel.artistPage?.artist?.shareLink
                                ?: "https://music.youtube.com/channel/${viewModel.artistId}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                },
                onLongClick = {},
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null
                )
            }
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}

/**
 * A secondary action beside the artist page's primary Shuffle pill: a 52dp glyph disc, sized to
 * match the pill's height so the row reads as one control cluster rather than a pill with two
 * smaller things next to it.
 *
 * [active] is the "already subscribed" state — a filled accent disc, versus the neutral translucent
 * plate an inactive action gets.
 */
@Composable
private fun ArtistActionButton(
    iconRes: Int,
    contentDescription: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(container)
            .border(
                width = 1.dp,
                color = if (active) Color.Transparent else Color.White.copy(alpha = 0.14f),
                shape = CircleShape,
            )
            .combinedClickable(onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
    }
}
