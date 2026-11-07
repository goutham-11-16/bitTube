package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.ChevronRight
import com.music.bitchord.data.model.ChannelOwnerInfo
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.YtMusicRepository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.video_quality
import com.music.bitchord.sharedui.resources.video_quality_auto
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.theme.HideSystemBars
import kotlinx.coroutines.delay

/** How long the controls stay over a playing video untouched. */
private const val VIDEO_CONTROLS_IDLE_MS = 3_500L

/**
 * A music video on a phone held sideways: the picture is the whole screen,
 * letterboxed in black, with the system bars away. A tap brings the controls
 * up over it — the title, the scrubber and the transport — and they stand
 * down again a few seconds after the last touch while it plays, as in every
 * video player. [onExitFullscreen] goes back to the ordinary landscape player.
 */
@Composable
internal fun FullscreenVideoPlayer(
    song: Song,
    isPlaying: Boolean,
    artRequest: ImageRequest,
    onRenderedChanged: (Boolean) -> Unit,
    rendered: Boolean,
    onExitFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    scrubber: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    onPlayPause: () -> Unit = {},
    onSeekBack: (() -> Unit)? = null,
    onSeekForward: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    HideSystemBars(true)
    var controlsShown by remember { mutableStateOf(true) }
    var videoFit by remember { mutableStateOf(true) }
    var captionsEnabled by remember { mutableStateOf(false) }
    var showPlaybackSpeed by remember { mutableStateOf(false) }
    val playbackSpeed by PlayerPlatform.host.playbackSpeed.collectAsStateWithLifecycle()
    // Bumped by every touch on the controls, so the idle timer starts over.
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(controlsShown, isPlaying, touches) {
        if (controlsShown && isPlaying) {
            delay(VIDEO_CONTROLS_IDLE_MS)
            controlsShown = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsShown = !controlsShown },
    ) {
        // Black with a spinner until the picture's first frame lands.
        if (!rendered) VideoLoadingBackdrop(Modifier.fillMaxSize())
        MusicVideoSurface(
            videoId = song.videoId,
            fit = videoFit,
            onRenderedChanged = onRenderedChanged,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.62f),
                            0.30f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    )
                    // Any touch on the controls starts the idle timer over.
                    // Watched, not taken: a tap on bare picture still reaches
                    // the box below and puts them away.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                touches++
                            }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    VideoCornerControls(
                        fullscreen = true,
                        onToggleFullscreen = onExitFullscreen,
                        onOpenQuality = onOpenQuality,
                        onOpenSpeed = { showPlaybackSpeed = true },
                        speedText = if (playbackSpeed == 1f) "1x" else "${playbackSpeed}x",
                        onToggleFit = { videoFit = !videoFit },
                        isFit = videoFit,
                        onToggleCaptions = { captionsEnabled = !captionsEnabled },
                        captionsEnabled = captionsEnabled,
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Rewind 10s
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                controlsShown = true
                                touches++
                                onSeekBack?.invoke()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    // Play / Pause
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .clickable {
                                controlsShown = true
                                touches++
                                onPlayPause()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    // Forward 10s
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                controlsShown = true
                                touches++
                                onSeekForward?.invoke()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    scrubber()
                }
            }
        }

        if (showPlaybackSpeed) {
            PlaybackSpeedSheet(
                currentSpeed = playbackSpeed,
                hazeState = remember { HazeState() },
                onSelectSpeed = { PlayerPlatform.host.setPlaybackSpeed(it) },
                onDismiss = { showPlaybackSpeed = false },
            )
        }
    }
}

/**
 * What stands in for a music video until its first frame lands: black, with a
 * spinner, rather than the still artwork. Shared by the portrait card and the
 * fullscreen landscape player.
 */
@Composable
internal fun VideoLoadingBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}

/**
 * The two glass boxes in a music video's top-right corner: the quality box —
 * the web player's VideoQuality, which reads the rendition on screen and opens
 * the quality drawer — and the full-screen button beside it.
 */
@Composable
internal fun VideoCornerControls(
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenSpeed: (() -> Unit)? = null,
    speedText: String = "1x",
    onToggleFit: (() -> Unit)? = null,
    isFit: Boolean = true,
    onToggleCaptions: (() -> Unit)? = null,
    captionsEnabled: Boolean = false,
    onEnterPip: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpenSettings != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (onToggleCaptions != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = if (captionsEnabled) 0.65f else 0.38f))
                    .clickable(onClick = onToggleCaptions),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (captionsEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
                    contentDescription = "Subtitles",
                    tint = if (captionsEnabled) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (onToggleFit != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onToggleFit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFit) Icons.Rounded.FitScreen else Icons.Rounded.Crop,
                    contentDescription = if (isFit) "Fit" else "Zoom",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (onOpenSpeed != null) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .widthIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onOpenSpeed)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = speedText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        VideoQualityBox(onClick = onOpenQuality)
        if (onEnterPip != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onEnterPip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.Pip,
                    contentDescription = "Picture in Picture",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onToggleFullscreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@Composable
internal fun PlaybackSpeedSheet(
    currentSpeed: Float,
    hazeState: HazeState,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerDrawer(
        hazeState = hazeState,
        title = "Playback Speed",
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PLAYBACK_SPEEDS.forEach { speed ->
                val label = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x"
                val active = abs(currentSpeed - speed) < 0.01f
                QualityRow(
                    name = label,
                    detail = null,
                    active = active,
                ) {
                    onSelectSpeed(speed)
                    onDismiss()
                }
            }
        }
    }
}

/** What the quality box and drawer say is playing: the held height, else the one on screen. */
private fun MusicVideoQualityUi.selected(): Int? =
    if (held == null || held !in heights) activeHeight else held

/**
 * The playing video's quality: 360p, 480p, HD, FullHD, 2K, 4K — what is on
 * screen, which is not always what was picked (4K held, then a video shot in
 * 1080p). Nothing until the renditions are known and there is a choice to make.
 */
@Composable
private fun VideoQualityBox(onClick: () -> Unit) {
    val quality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    if (quality.heights.size < 2) return
    val label = quality.selected()?.let(::videoQualityLabel) ?: "Auto"
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "videoQualityLabel",
        ) { text ->
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * The quality list, in the same drawer as the audio output: Auto, then every
 * height this video offers, the one playing marked. Picking one holds it from
 * now on — for the next video too, where that video has it.
 */
@Composable
internal fun VideoQualitySheet(hazeState: HazeState, onDismiss: () -> Unit) {
    val quality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val selected = quality.selected()
    PlayerDrawer(
        hazeState = hazeState,
        title = stringResource(Res.string.video_quality),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QualityRow(
                name = stringResource(Res.string.video_quality_auto),
                detail = if (quality.held == null) selected?.let(::videoQualityLabel) else null,
                active = quality.held == null,
            ) {
                PlayerPlatform.host.setMusicVideoQuality(null)
                onDismiss()
            }
            quality.heights.forEach { height ->
                QualityRow(
                    name = videoQualityLabel(height),
                    detail = "${height}p",
                    active = quality.held != null && selected == height,
                ) {
                    PlayerPlatform.host.setMusicVideoQuality(height)
                    onDismiss()
                }
            }
        }
    }
}

/** One row of [VideoQualitySheet], drawn as the audio output drawer draws an output. */
@Composable
private fun QualityRow(name: String, detail: String?, active: Boolean, onSelect: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(Color.White.copy(alpha = if (active) 0.10f else 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !active,
            ) {
                haptics.play(Haptic.Select)
                onSelect()
            }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = Color.White.copy(alpha = if (active) 1f else 0.85f),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (active) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * YouTube-style portrait video layout:
 * - 16:9 Video surface pinned at top with overlay controls
 * - Hairline scrubber directly below video
 * - Video title, channel row with avatar and Subscribe button
 * - Action pills row (Like, Dislike, Share, Download, Comments)
 * - Transport controls (Play/Pause, Next, Previous)
 * - Scrollable Up Next / related videos queue below
 */
@Composable
internal fun YouTubePortraitVideoLayout(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekBack: (() -> Unit)? = null,
    onSeekForward: (() -> Unit)? = null,
    rendered: Boolean,
    onRenderedChanged: (Boolean) -> Unit,
    likeStatus: LikeStatus,
    onToggleLike: () -> Unit,
    queue: List<Song>,
    queueIndex: Int,
    onSelectQueueItem: (Song, Int) -> Unit,
    onOpenQuality: () -> Unit,
    onOpenSpeed: () -> Unit,
    playbackSpeed: Float,
    captionsEnabled: Boolean,
    onToggleCaptions: () -> Unit,
    videoFit: Boolean,
    onToggleFit: () -> Unit,
    onFullscreen: () -> Unit,
    onDismiss: () -> Unit,
    onEnterPip: (() -> Unit)? = null,
    onPlayRecommended: ((Song) -> Unit)? = null,
    onOpenChannel: ((String, String) -> Unit)? = null,
    onDownload: ((Song) -> Unit)? = null,
    scrubber: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    hazeState: HazeState,
    inPip: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (inPip) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            MusicVideoSurface(
                videoId = song.videoId,
                fit = false,
                onRenderedChanged = onRenderedChanged,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSubscribed by remember(song.videoId) { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showVideoSettings by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }

    var channelOwner by remember(song.videoId) {
        mutableStateOf(
            ChannelOwnerInfo(
                channelId = song.artistId ?: song.channelId,
                channelTitle = song.artist,
                channelThumbnailUrl = song.channelThumbnailUrl,
            )
        )
    }
    LaunchedEffect(song.videoId) {
        try {
            val owner = withContext(Dispatchers.IO) {
                YtMusicRepository.videoOwner(song.videoId).getOrNull()
            }
            if (owner != null) {
                channelOwner = owner
            }
        } catch (_: Exception) {}
    }

    val videoAspect by PlayerPlatform.host.musicVideoAspect.collectAsStateWithLifecycle()
    val isVerticalShort = videoAspect in 0.3f..0.85f
    // Keep 16:9 header surface so vertical videos don't blow up the screen and push details/recommendations offscreen
    val surfaceAspect = 16f / 9f

    var controlsVisible by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(controlsVisible, isPlaying, touches) {
        if (controlsVisible && isPlaying) {
            delay(3000L)
            controlsVisible = false
        }
    }

    var is2xSpeedActive by remember { mutableStateOf(false) }
    var originalSpeed by remember { mutableFloatStateOf(1f) }
    var rewindRipple by remember { mutableStateOf(false) }
    var forwardRipple by remember { mutableStateOf(false) }

    LaunchedEffect(rewindRipple) {
        if (rewindRipple) {
            delay(650L)
            rewindRipple = false
        }
    }

    LaunchedEffect(forwardRipple) {
        if (forwardRipple) {
            delay(650L)
            forwardRipple = false
        }
    }

    var recommendedVideos by remember(song.videoId) { mutableStateOf<List<Song>>(emptyList()) }
    LaunchedEffect(song.videoId) {
        try {
            val related = withContext(Dispatchers.IO) {
                YtMusicRepository.radio(song.videoId, preferVideo = true).getOrNull()
            }
            if (!related.isNullOrEmpty()) {
                recommendedVideos = related.filter { it.videoId != song.videoId }
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Pinned Top Video Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .aspectRatio(surfaceAspect)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom > 1.25f && videoFit) {
                            onToggleFit()
                        } else if (zoom < 0.8f && !videoFit) {
                            onToggleFit()
                        }
                    }
                }
                .pointerInput(playbackSpeed) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) touches++
                        },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2f) {
                                onSeekBack?.invoke()
                                rewindRipple = true
                            } else {
                                onSeekForward?.invoke()
                                forwardRipple = true
                            }
                        },
                        onLongPress = {
                            originalSpeed = playbackSpeed
                            PlayerPlatform.host.setPlaybackSpeed(2.0f)
                            is2xSpeedActive = true
                        },
                        onPress = {
                            try {
                                tryAwaitRelease()
                            } finally {
                                if (is2xSpeedActive) {
                                    PlayerPlatform.host.setPlaybackSpeed(originalSpeed)
                                    is2xSpeedActive = false
                                }
                            }
                        },
                    )
                },
        ) {
            if (!rendered) VideoLoadingBackdrop(Modifier.fillMaxSize())
            MusicVideoSurface(
                videoId = song.videoId,
                fit = if (isVerticalShort) true else videoFit,
                onRenderedChanged = onRenderedChanged,
                modifier = Modifier.fillMaxSize(),
            )

            // Double tap rewind overlay (left half)
            androidx.compose.animation.AnimatedVisibility(
                visible = rewindRipple,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.45f)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("10 seconds", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Double tap forward overlay (right half)
            androidx.compose.animation.AnimatedVisibility(
                visible = forwardRipple,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.45f)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("10 seconds", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2X Speed preview indicator (top center)
            if (is2xSpeedActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("2X", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    // Minimize / dismiss button top-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    // Top-right corner controls
                    VideoCornerControls(
                        fullscreen = false,
                        onToggleFullscreen = onFullscreen,
                        onOpenQuality = onOpenQuality,
                        onOpenSpeed = onOpenSpeed,
                        speedText = if (playbackSpeed == 1f) "1x" else "${playbackSpeed}x",
                        onToggleFit = onToggleFit,
                        isFit = videoFit,
                        onToggleCaptions = onToggleCaptions,
                        captionsEnabled = captionsEnabled,
                        onEnterPip = onEnterPip,
                        onOpenSettings = { showVideoSettings = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )

                    // Center transport controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Rewind 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable {
                                    controlsVisible = true
                                    touches++
                                    onSeekBack?.invoke() ?: onPrevious()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        // Play / Pause
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .clickable {
                                    controlsVisible = true
                                    touches++
                                    onPlayPause()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.Black,
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }

                        // Forward 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable {
                                    controlsVisible = true
                                    touches++
                                    onSeekForward?.invoke() ?: onNext()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }

        // Inline scrubber directly beneath video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            scrubber()
        }

        // Scrollable content beneath video
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "yt:header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${song.artist} • Ad-free video",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )

                    Spacer(Modifier.height(12.dp))

                    val targetChannelId = channelOwner.channelId ?: song.artistId ?: song.channelId
                    val targetChannelName = channelOwner.channelTitle ?: song.artist

                    // Channel Avatar + Name + Subscribe Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    onOpenChannel?.invoke(targetChannelId.orEmpty(), targetChannelName)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!channelOwner.channelThumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = channelOwner.channelThumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    text = targetChannelName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onOpenChannel?.invoke(targetChannelId.orEmpty(), targetChannelName)
                                },
                        ) {
                            Text(
                                text = targetChannelName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!channelOwner.subscriberCount.isNullOrBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = channelOwner.subscriberCount!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                )
                            }
                        }

                        // Subscribe button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSubscribed) Color.White.copy(alpha = 0.18f) else Color.White)
                                .clickable {
                                    isSubscribed = !isSubscribed
                                    scope.launch {
                                        targetChannelId?.let { id ->
                                            if (id.isNotBlank()) YtMusicRepository.setSubscribed(id, isSubscribed)
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                color = if (isSubscribed) Color.White else Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Action Pills Row
            item(key = "yt:actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Like / Dislike pill
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable(onClick = onToggleLike)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ThumbUp,
                                contentDescription = "Like",
                                tint = if (likeStatus == LikeStatus.LIKE) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (likeStatus == LikeStatus.LIKE) "Liked" else "Like",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Share pill
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "https://youtube.com/watch?v=${song.videoId}")
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, song.title))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Download pill
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { showDownloadDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Download",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Comments pill
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { showCommentsSheet = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Comments",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Up Next Header
            item(key = "yt:upnext:title") {
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            // Up Next queue list & recommendations
            val upcoming = queue.filterIndexed { idx, _ -> idx > queueIndex }
            val displayItems = if (upcoming.isNotEmpty()) upcoming else recommendedVideos
            itemsIndexed(displayItems, key = { index, item -> "upnext_${item.videoId}_$index" }) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (upcoming.isNotEmpty()) {
                                onSelectQueueItem(item, queueIndex + 1 + index)
                            } else {
                                onPlayRecommended?.invoke(item) ?: onSelectQueueItem(item, 0)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        item.durationText?.let { duration ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = duration,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        }

        if (showDownloadDialog) {
            PlayerDrawer(
                hazeState = hazeState,
                title = "Download Quality",
                onDismiss = { showDownloadDialog = false },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    val qualities = listOf(
                        "1080p (Full HD)",
                        "720p (HD)",
                        "480p (Standard)",
                        "360p (Low)",
                        "Audio Only (High Quality)",
                    )
                    qualities.forEach { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDownloadDialog = false
                                    onDownload?.invoke(song)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (showCommentsSheet) {
            VideoCommentsSheet(
                videoId = song.videoId,
                hazeState = hazeState,
                onDismiss = { showCommentsSheet = false },
            )
        }

        if (showVideoSettings) {
            VideoSettingsSheet(
                qualityText = "Auto",
                speedText = if (playbackSpeed == 1f) "Normal" else "${playbackSpeed}x",
                captionsEnabled = captionsEnabled,
                isFit = videoFit,
                isLooping = isLooping,
                onToggleLoop = { isLooping = !isLooping },
                onOpenQuality = onOpenQuality,
                onOpenSpeed = { onOpenSpeed?.invoke() },
                onToggleCaptions = { onToggleCaptions?.invoke() },
                onToggleFit = { onToggleFit?.invoke() },
                hazeState = hazeState,
                onDismiss = { showVideoSettings = false },
            )
        }
    }
}

@Composable
internal fun VideoSettingsSheet(
    qualityText: String,
    speedText: String,
    captionsEnabled: Boolean,
    isFit: Boolean,
    isLooping: Boolean,
    onToggleLoop: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenSpeed: () -> Unit,
    onToggleCaptions: () -> Unit,
    onToggleFit: () -> Unit,
    hazeState: HazeState,
    onDismiss: () -> Unit,
) {
    PlayerDrawer(
        hazeState = hazeState,
        title = "Video Settings",
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            VideoSettingItem(
                icon = Icons.Rounded.HighQuality,
                title = "Quality",
                value = qualityText,
                onClick = {
                    onDismiss()
                    onOpenQuality()
                },
            )
            VideoSettingItem(
                icon = Icons.Rounded.Speed,
                title = "Playback speed",
                value = speedText,
                onClick = {
                    onDismiss()
                    onOpenSpeed()
                },
            )
            VideoSettingItem(
                icon = if (captionsEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
                title = "Captions",
                value = if (captionsEnabled) "On" else "Off",
                onClick = onToggleCaptions,
            )
            VideoSettingItem(
                icon = if (isFit) Icons.Rounded.FitScreen else Icons.Rounded.Crop,
                title = "Screen aspect",
                value = if (isFit) "Fit (16:9)" else "Fill (Zoomed)",
                onClick = onToggleFit,
            )
            VideoSettingItem(
                icon = Icons.Rounded.Repeat,
                title = "Loop video",
                value = if (isLooping) "On" else "Off",
                onClick = onToggleLoop,
            )
            VideoSettingItem(
                icon = Icons.Rounded.Headphones,
                title = "Audio track",
                value = "Original (High)",
                onClick = {
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun VideoSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Comments bottom sheet powered by NewPipeExtractor.
 */
@Composable
internal fun VideoCommentsSheet(
    videoId: String,
    hazeState: HazeState,
    onDismiss: () -> Unit,
) {
    var comments by remember(videoId) { mutableStateOf<List<CommentUi>>(emptyList()) }
    var loading by remember(videoId) { mutableStateOf(true) }
    var error by remember(videoId) { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        loading = true
        error = null
        try {
            comments = PlayerPlatform.host.fetchComments(videoId)
        } catch (e: Exception) {
            error = e.message ?: "Could not load comments"
        } finally {
            loading = false
        }
    }

    PlayerDrawer(
        hazeState = hazeState,
        title = "Comments",
        onDismiss = onDismiss,
    ) {
        when {
            loading -> {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            error != null -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Comments unavailable for this video", color = Color.White.copy(alpha = 0.7f))
                }
            }
            comments.isEmpty() -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No comments found", color = Color.White.copy(alpha = 0.7f))
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(comments) { comment ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!comment.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = comment.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else if (comment.author.isNotBlank()) {
                                    Text(
                                        text = comment.author.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.author,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (comment.timeAgo.isNotBlank()) {
                                        Text(
                                            text = " • ${comment.timeAgo}",
                                            color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = comment.text,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
