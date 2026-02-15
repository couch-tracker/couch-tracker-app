package io.github.couchtracker.ui.screens.show

import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper

data class WatchedEpisodeSessionInfo(
    val watchedEpisodeSession: WatchedEpisodeSessionWrapper,
    val watchedEpisodes: List<WatchedItemWrapper.Episode>,
) {
    private val watchedEpisodesLocalWatchAts = watchedEpisodes.mapNotNull { it.watchAt }

    val firstWatchedEpisodeAt = watchedEpisodesLocalWatchAts.minWithOrNull(PartialDateTime.LOCAL_LOW_PRECISION_FIRST_COMPARATOR)
    val lastWatchedEpisodeAt = watchedEpisodesLocalWatchAts.maxWithOrNull(PartialDateTime.LOCAL_HIGH_PRECISION_FIRST_COMPARATOR)
}

fun FullProfileData.getWatchedEpisodeSessionInfo(watchedEpisodeSession: WatchedEpisodeSessionWrapper) = WatchedEpisodeSessionInfo(
    watchedEpisodeSession = watchedEpisodeSession,
    watchedEpisodes = watchedEpisodesBySession[watchedEpisodeSession].orEmpty(),
)

fun List<WatchedEpisodeSessionInfo>.sorted(): List<WatchedEpisodeSessionInfo> {
    return PartialDateTime.sort(
        items = this,
        getPartialDateTime = { lastWatchedEpisodeAt },
        additionalComparator = compareBy<WatchedEpisodeSessionInfo> { info -> info.watchedEpisodes.maxOfOrNull { it.addedAt } }
            .thenBy { it.watchedEpisodeSession.id },
    )
}
