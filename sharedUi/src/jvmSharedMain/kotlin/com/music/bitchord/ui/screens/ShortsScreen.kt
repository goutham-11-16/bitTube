package com.music.bitchord.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.player.MusicVideoSurface
import com.music.bitchord.ui.player.VideoCommentsSheet
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated, continuous vertical-scrolling YouTube Shorts player.
 * Supports swipe up/down, like, comments, share, channel avatar, and subscribe.
 */
@Composable
fun ShortsScreen(
    initialVideoId: String? = null,
    onClose: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onOpenChannel: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    androidx.activity.compose.BackHandler { onClose() }
    val shortsList = remember { mutableStateListOf<Song>() }
    var loading by remember { mutableStateOf(true) }
    var showCommentsForId by remember { mutableStateOf<String?>(null) }
    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val fetched = withContext(Dispatchers.IO) {
                // Fetch Shorts from search results and trending
                val searchShorts = YtMusicRepository.search("shorts #shorts", SearchFilter.SONGS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()
                    ?.map { it.song }
                    .orEmpty()
                if (searchShorts.isNotEmpty()) searchShorts
                else {
                    YtMusicRepository.search("trending shorts", SearchFilter.SONGS).getOrNull()
                        ?.filterIsInstance<SearchResult.Track>()
                        ?.map { it.song }
                        .orEmpty()
                }
            }
            if (fetched.isNotEmpty()) {
                shortsList.clear()
                if (initialVideoId != null) {
                    val target = fetched.firstOrNull { it.videoId == initialVideoId }
                    if (target != null) {
                        shortsList.add(target)
                        shortsList.addAll(fetched.filter { it.videoId != initialVideoId })
                    } else {
                        shortsList.addAll(fetched)
                    }
                } else {
                    shortsList.addAll(fetched)
                }
            }
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    if (loading && shortsList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(40.dp))
        }
        return
    }

    if (shortsList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No Shorts available right now", color = Color.White)
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shortsList.size })

    // When the settled page changes, play the audio of the active Short
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            shortsList.getOrNull(page)?.let { currentShort ->
                onPlaySong(currentShort)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val short = shortsList[page]
            val isCurrent = page == pagerState.currentPage
            var isSubscribed by remember(short.videoId) { mutableStateOf(false) }
            var isLiked by remember(short.videoId) { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                // 9:16 Fullscreen Video Player
                if (isCurrent) {
                    MusicVideoSurface(
                        videoId = short.videoId,
                        fit = false,
                        onRenderedChanged = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = short.thumbnailUrl ?: "https://i.ytimg.com/vi/${short.videoId}/hqdefault.jpg",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Bottom Gradient Scrim for readable text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            ),
                        ),
                )

                // Right Action Pills Column
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Like Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { isLiked = !isLiked },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isLiked) Color.Red else Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.ThumbUp, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(if (isLiked) "Liked" else "Like", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // Dislike Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.ThumbDown, contentDescription = "Dislike", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("Dislike", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // Comments Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showCommentsForId = short.videoId },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Comments", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("Comments", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // Share Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "https://youtube.com/shorts/${short.videoId}")
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, short.title))
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Bottom Left Channel & Video Details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 72.dp, bottom = 44.dp),
                ) {
                    // Channel Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                                .clickable {
                                    val cid = short.artistId ?: short.channelId
                                    if (cid != null) onOpenChannel?.invoke(cid, short.artist)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!short.channelThumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = short.channelThumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    text = short.artist.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = short.artist.ifBlank { "YouTube Creator" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                val cid = short.artistId ?: short.channelId
                                if (cid != null) onOpenChannel?.invoke(cid, short.artist)
                            },
                        )

                        Spacer(Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSubscribed) Color.White.copy(alpha = 0.2f) else Color.Red)
                                .clickable {
                                    isSubscribed = !isSubscribed
                                    scope.launch {
                                        short.artistId?.let { id ->
                                            YtMusicRepository.setSubscribed(id, isSubscribed)
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Video Title
                    Text(
                        text = short.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Top Navigation Bar (Close Button + Shorts Branding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Close Shorts",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Shorts",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }

        // Comments sheet overlay if requested
        if (showCommentsForId != null) {
            VideoCommentsSheet(
                videoId = showCommentsForId!!,
                hazeState = hazeState,
                onDismiss = { showCommentsForId = null },
            )
        }
    }
}
