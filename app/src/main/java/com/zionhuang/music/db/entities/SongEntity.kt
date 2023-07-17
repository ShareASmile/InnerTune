package com.zionhuang.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "song")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Int = -1, // in seconds
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val liked: Boolean = false,
    val totalPlayTime: Long = 0, // in milliseconds
    val downloadState: Int = STATE_NOT_DOWNLOADED,
    val inLibrary: LocalDateTime? = null,
) {
    fun toggleLike() =
        if (!liked) {
            copy(
                liked = true,
                inLibrary = inLibrary ?: LocalDateTime.now()
            )
        } else copy(liked = false)

    fun toggleLibrary() =
        copy(inLibrary = if (inLibrary == null) LocalDateTime.now() else null)

    companion object {
        const val STATE_NOT_DOWNLOADED = 0
        const val STATE_PREPARING = 1
        const val STATE_DOWNLOADING = 2
        const val STATE_DOWNLOADED = 3
    }
}
