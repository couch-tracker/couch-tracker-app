package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedEpisodeSession
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedItemDimensionSelections
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode

sealed class WatchedItemSelectionsState {
    abstract val sheetMode: WatchedItemSheetMode
    abstract val datetime: DateTimeSectionState
    abstract val dimensionsState: WatchedItemDimensionSelectionsState

    val dimensions get() = dimensionsState.dimensions

    abstract fun isValid(): Boolean

    fun update(selection: WatchedItemDimensionSelection<*>) {
        dimensionsState.update(selection)
    }

    fun save(db: ProfileData) {
        db.transaction {
            val dimensionSelections = dimensionsState.save(db)
            val watchAt = datetime.dateTime?.dateTime

            save(db, watchAt, dimensionSelections)
        }
    }

    protected abstract fun save(db: ProfileData, watchAt: PartialDateTime?, dimensionSelections: WatchedItemDimensionSelections)

    abstract class New : WatchedItemSelectionsState() {

        override fun save(
            db: ProfileData,
            watchAt: PartialDateTime?,
            dimensionSelections: WatchedItemDimensionSelections,
        ) {
            val watchedItem = db.watchedItemQueries.insert(
                watchAt = watchAt,
                dimensionSelections = dimensionSelections.id,
            ).executeAsOne()

            insert(db, watchedItem)
        }

        protected abstract fun insert(db: ProfileData, watchedItem: WatchedItem)

        class Movie(
            override val sheetMode: WatchedItemSheetMode.New.Movie,
            override val datetime: DateTimeSectionState,
            override val dimensionsState: WatchedItemDimensionSelectionsState,
        ) : New() {
            override fun isValid() = dimensionsState.isValid()

            override fun insert(db: ProfileData, watchedItem: WatchedItem) {
                db.watchedMovieQueries.insert(id = watchedItem.id, movieId = sheetMode.itemId)
            }
        }

        class Episode(
            override val sheetMode: WatchedItemSheetMode.New.Episode,
            override val datetime: DateTimeSectionState,
            override val dimensionsState: WatchedItemDimensionSelectionsState,
            initialSession: WatchedEpisodeSessionWrapper?,
        ) : New() {

            var session by mutableStateOf(initialSession)

            fun isSessionValid() = sheetMode.sessions.isEmpty() || session != null

            override fun isValid() = isSessionValid() && dimensionsState.isValid()

            override fun insert(db: ProfileData, watchedItem: WatchedItem) {
                db.watchedEpisodeQueries.insert(
                    id = watchedItem.id,
                    episodeId = sheetMode.itemId,
                    session = session?.id ?: insertNewSession(db).id,
                )
            }

            private fun insertNewSession(db: ProfileData): WatchedEpisodeSession {
                return db.watchedEpisodeSessionQueries.insert(
                    showId = sheetMode.showId,
                    name = null,
                    description = null,
                    isActive = true,
                    defaultDimensionSelections = db.watchedItemDimensionSelectionsQueries.insert().executeAsOne(),
                ).executeAsOne()
            }
        }
    }

    class Edit(
        override val sheetMode: WatchedItemSheetMode.Edit,
        override val datetime: DateTimeSectionState,
        override val dimensionsState: WatchedItemDimensionSelectionsState,
    ) : WatchedItemSelectionsState() {

        override fun isValid() = dimensionsState.isValid()

        override fun save(db: ProfileData, watchAt: PartialDateTime?, dimensionSelections: WatchedItemDimensionSelections) {
            db.watchedItemQueries.updateWatchAt(
                id = sheetMode.watchedItem.id,
                watchAt = watchAt,
            ).executeAsOne()
        }
    }
}

@Composable
fun rememberWatchedItemSelectionsState(watchedItemType: WatchedItemType, mode: WatchedItemSheetMode): WatchedItemSelectionsState {
    val profileData = LocalFullProfileDataContext.current
    val dimensionSelectionsState = rememberWatchedItemDimensionSelectionsState(
        watchedItemType = watchedItemType,
        mode = when (mode) {
            is WatchedItemSheetMode.New -> WatchedItemDimensionSelectionsMode.New
            is WatchedItemSheetMode.Edit -> WatchedItemDimensionSelectionsMode.Edit(mode.watchedItem.selections)
        },
    )

    // TODO make this savable
    return remember(profileData, watchedItemType, mode, dimensionSelectionsState) {
        when (mode) {
            is WatchedItemSheetMode.New.Movie -> WatchedItemSelectionsState.New.Movie(
                sheetMode = mode,
                datetime = DateTimeSectionState(),
                dimensionsState = dimensionSelectionsState,
            )
            is WatchedItemSheetMode.New.Episode -> WatchedItemSelectionsState.New.Episode(
                sheetMode = mode,
                datetime = DateTimeSectionState(),
                dimensionsState = dimensionSelectionsState,
                initialSession = mode.sessions.singleOrNull(),
            )

            is WatchedItemSheetMode.Edit -> WatchedItemSelectionsState.Edit(
                sheetMode = mode,
                datetime = DateTimeSectionState(mode.watchedItem.watchAt),
                dimensionsState = dimensionSelectionsState,
            )
        }
    }
}
