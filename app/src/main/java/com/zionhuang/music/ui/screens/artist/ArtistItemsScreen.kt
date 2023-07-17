package com.zionhuang.music.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.innertube.models.*
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.constants.GridThumbnailHeight
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.playback.queues.YouTubeQueue
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.YouTubeGridItem
import com.zionhuang.music.ui.component.YouTubeListItem
import com.zionhuang.music.ui.component.shimmer.ListItemPlaceHolder
import com.zionhuang.music.ui.component.shimmer.ShimmerHost
import com.zionhuang.music.ui.menu.YouTubeAlbumMenu
import com.zionhuang.music.ui.menu.YouTubeArtistMenu
import com.zionhuang.music.ui.menu.YouTubeSongMenu
import com.zionhuang.music.viewmodels.ArtistItemsViewModel
import com.zionhuang.music.viewmodels.MainViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistItemsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistItemsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playWhenReady by playerConnection.playWhenReady.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val librarySongIds by mainViewModel.librarySongIds.collectAsState()
    val likedSongIds by mainViewModel.likedSongIds.collectAsState()
    val libraryAlbumIds by mainViewModel.libraryAlbumIds.collectAsState()
    val libraryPlaylistIds by mainViewModel.libraryPlaylistIds.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val title by viewModel.title.collectAsState()
    val itemsPage by viewModel.itemsPage.collectAsState()

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    if (itemsPage == null) {
        ShimmerHost(
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
        ) {
            repeat(8) {
                ListItemPlaceHolder()
            }
        }
    }

    if (itemsPage?.items?.firstOrNull() is SongItem) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id }
            ) { item ->
                YouTubeListItem(
                    item = item,
                    badges = {
                        if (item is SongItem && item.id in librarySongIds ||
                            item is AlbumItem && item.id in libraryAlbumIds ||
                            item is PlaylistItem && item.id in libraryPlaylistIds
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_library_add_check),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                        if (item is SongItem && item.id in likedSongIds) {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                        if (item.explicit) {
                            Icon(
                                painter = painterResource(R.drawable.ic_explicit),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                    },
                    isPlaying = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    },
                    playWhenReady = playWhenReady,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            album = item,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            playerConnection = playerConnection,
                                            onDismiss = menuState::dismiss
                                        )
                                        is PlaylistItem -> {}
                                    }
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
                        .clickable {
                            when (item) {
                                is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            }
                        }
                )
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id }
            ) { item ->
                YouTubeGridItem(
                    item = item,
                    badges = {
                        if (item is SongItem && item.id in librarySongIds ||
                            item is AlbumItem && item.id in libraryAlbumIds ||
                            item is PlaylistItem && item.id in libraryPlaylistIds
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_library_add_check),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                        if (item is SongItem && item.id in likedSongIds) {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                        if (item.explicit) {
                            Icon(
                                painter = painterResource(R.drawable.ic_explicit),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 2.dp)
                            )
                        }
                    },
                    isPlaying = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    },
                    playWhenReady = playWhenReady,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            },
                            onLongClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            album = item,
                                            navController = navController,
                                            playerConnection = playerConnection,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            playerConnection = playerConnection,
                                            onDismiss = menuState::dismiss
                                        )
                                        is PlaylistItem -> {}
                                    }
                                }
                            }
                        )
                )
            }
        }
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
