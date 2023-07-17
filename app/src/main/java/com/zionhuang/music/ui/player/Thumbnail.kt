package com.zionhuang.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.constants.ShowLyricsKey
import com.zionhuang.music.constants.ThumbnailCornerRadius
import com.zionhuang.music.extensions.metadata
import com.zionhuang.music.ui.component.Lyrics
import com.zionhuang.music.ui.utils.HorizontalPager
import com.zionhuang.music.ui.utils.SnapLayoutInfoProvider
import com.zionhuang.music.utils.rememberPreference
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val windows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val error by playerConnection.error.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, false)

    val pagerState = rememberPagerState(
        initialPage = currentWindowIndex.takeIf { it != -1 } ?: 0
    )

    val snapLayoutInfoProvider = remember(pagerState) {
        SnapLayoutInfoProvider(
            pagerState = pagerState,
            positionInLayout = { _, _ -> 0f }
        )
    }

    LaunchedEffect(pagerState, currentWindowIndex) {
        if (windows.isNotEmpty()) {
            try {
                pagerState.animateScrollToPage(currentWindowIndex)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { index ->
            if (!pagerState.isScrollInProgress && index != currentWindowIndex && windows.isNotEmpty()) {
                playerConnection.player.seekToDefaultPosition(windows[index].firstPeriodIndex)
            }
        }
    }

    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                items = windows,
                key = { it.uid.hashCode() },
                beyondBoundsPageCount = 2
            ) { window ->
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = window.mediaItem.metadata?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .align(Alignment.Center)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        if (offset.x < size.width / 2) {
                                            playerConnection.player.seekBack()
                                        } else {
                                            playerConnection.player.seekForward()
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Lyrics(sliderPositionProvider = sliderPositionProvider)
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center)
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare
                )
            }
        }
    }
}
