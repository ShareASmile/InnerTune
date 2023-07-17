package com.zionhuang.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.playback.queues.YouTubeQueue
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.SongListItem
import com.zionhuang.music.ui.menu.SongMenu
import com.zionhuang.music.viewmodels.DateAgo
import com.zionhuang.music.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playWhenReady by playerConnection.playWhenReady.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val events by viewModel.events.collectAsState()

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
    ) {
        events.forEach { (dateAgo, events) ->
            stickyHeader {
                Text(
                    text = when (dateAgo) {
                        DateAgo.Today -> stringResource(R.string.today)
                        DateAgo.Yesterday -> stringResource(R.string.yesterday)
                        DateAgo.ThisWeek -> stringResource(R.string.this_week)
                        DateAgo.LastWeek -> stringResource(R.string.last_week)
                        is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            items(
                items = events,
                key = { it.event.id }
            ) { event ->
                SongListItem(
                    song = event.song,
                    isPlaying = event.song.id == mediaMetadata?.id,
                    playWhenReady = playWhenReady,
                    badges = {
                        if (event.song.song.inLibrary != null) {
                            Icon(
                                painter = painterResource(R.drawable.ic_library_add_check),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                        if (event.song.song.liked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = event.song,
                                        event = event.event,
                                        navController = navController,
                                        playerConnection = playerConnection,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    endpoint = WatchEndpoint(videoId = event.song.id),
                                    preloadItem = event.song.toMediaMetadata()
                                )
                            )
                        }
                        .animateItemPlacement()
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.history)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
