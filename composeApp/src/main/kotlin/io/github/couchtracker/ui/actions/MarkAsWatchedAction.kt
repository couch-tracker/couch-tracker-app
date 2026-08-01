package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.ModalWatchedEpisodeSessionSelectorBottomSheet
import io.github.couchtracker.db.profile.model.watchedItem.rememberModalWatchedEpisodeSessionSelectorBottomSheetState
import io.github.couchtracker.ui.LocalWatchedItemSheetScaffoldState
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.utils.str

@Composable
fun markMovieAsWatchedAction(movieId: ExternalMovieId, watchedItemSheetModel: () -> WatchedItemSheetMode.New.Movie): Action {
    val state = LocalWatchedItemSheetScaffoldState.current
    val fullProfileData = LocalFullProfileDataContext.current
    val isWatched = movieId in fullProfileData.watchedItemsByMovie
    return Action(
        name = R.string.mark_movie_as_watched.str(),
        icon = Icons.Filled.Check,
        active = isWatched,
        onClick = {
            state.open(watchedItemSheetModel())
        },
    )
}

@Composable
fun markEpisodeAsWatchedAction(
    showId: ExternalShowId,
    episodeId: ExternalEpisodeId,
    watchedItemSheetModel: (WatchedItemSheetMode.New.Episode.WatchedSession) -> WatchedItemSheetMode.New.Episode,
): Action {
    val fullProfileData = LocalFullProfileDataContext.current
    val sessionSelectorSheetState = rememberModalWatchedEpisodeSessionSelectorBottomSheetState()
    val watchedItemSheetState = LocalWatchedItemSheetScaffoldState.current
    val isWatched = fullProfileData.watchedItemsByEpisode[episodeId].orEmpty().any { it.session.isActive }

    return Action(
        name = R.string.mark_episode_as_watched.str(),
        icon = Icons.Filled.Check,
        active = isWatched,
        onClick = {
            val activeSessions = fullProfileData.watchedEpisodeSessions[showId].orEmpty().filter { it.isActive }
            if (activeSessions.size > 1) {
                sessionSelectorSheetState.open(
                    sessions = activeSessions,
                    onSelected = { watchSession ->
                        watchedItemSheetState.open(
                            watchedItemSheetModel(WatchedItemSheetMode.New.Episode.WatchedSession.Existing(watchSession, showLabel = true)),
                        )
                    },
                )
            } else {
                val sessionProvider = if (activeSessions.isEmpty()) {
                    WatchedItemSheetMode.New.Episode.WatchedSession.New { db ->
                        db.watchedEpisodeSessionQueries.insert(
                            showId = showId,
                            name = null,
                            description = null,
                            isActive = true,
                            defaultDimensionSelections = db.watchedItemDimensionSelectionsQueries.insert().executeAsOne(),
                        ).executeAsOne()
                    }
                } else {
                    val session = activeSessions.single()
                    WatchedItemSheetMode.New.Episode.WatchedSession.Existing(
                        session = session,
                        showLabel = session.name != null,
                    )
                }
                watchedItemSheetState.open(watchedItemSheetModel(sessionProvider))
            }
        },
        companionComposable = {
            ModalWatchedEpisodeSessionSelectorBottomSheet(sessionSelectorSheetState)
        },
    )
}
