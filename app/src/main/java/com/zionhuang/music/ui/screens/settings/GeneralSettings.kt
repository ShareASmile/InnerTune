package com.zionhuang.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.R
import com.zionhuang.music.constants.AutoAddToLibraryKey
import com.zionhuang.music.constants.AutoDownloadKey
import com.zionhuang.music.constants.ExpandOnPlayKey
import com.zionhuang.music.constants.NotificationMoreActionKey
import com.zionhuang.music.ui.component.SwitchPreference
import com.zionhuang.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (autoAddToLibrary, onAutoAddToLibraryChange) = rememberPreference(key = AutoAddToLibraryKey, defaultValue = true)
    val (autoDownload, onAutoDownloadChange) = rememberPreference(key = AutoDownloadKey, defaultValue = false)
    val (expandOnPlay, onExpandOnPlayChange) = rememberPreference(key = ExpandOnPlayKey, defaultValue = false)
    val (notificationMoreAction, onNotificationMoreActionChange) = rememberPreference(key = NotificationMoreActionKey, defaultValue = true)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        SwitchPreference(
            title = stringResource(R.string.auto_add_song),
            description = stringResource(R.string.auto_add_song_desc),
            icon = R.drawable.ic_library_add,
            checked = autoAddToLibrary,
            onCheckedChange = onAutoAddToLibraryChange
        )
        SwitchPreference(
            title = stringResource(R.string.auto_download),
            description = stringResource(R.string.auto_download_desc),
            icon = R.drawable.ic_save_alt,
            checked = autoDownload,
            onCheckedChange = onAutoDownloadChange
        )
        SwitchPreference(
            title = stringResource(R.string.expand_on_play),
            icon = R.drawable.ic_open_in_full,
            checked = expandOnPlay,
            onCheckedChange = onExpandOnPlayChange
        )
        SwitchPreference(
            title = stringResource(R.string.notification_more_action),
            description = stringResource(R.string.notification_more_action_desc),
            icon = R.drawable.ic_notifications,
            checked = notificationMoreAction,
            onCheckedChange = onNotificationMoreActionChange
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.general)) },
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
