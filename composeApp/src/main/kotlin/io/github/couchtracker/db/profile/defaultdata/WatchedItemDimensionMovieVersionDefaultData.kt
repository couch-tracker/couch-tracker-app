package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType

object WatchedItemDimensionMovieVersionDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.MOVIE_VERSION.toDbText(),
    appliesTo = setOf(WatchedItemType.MOVIE),
    choices = listOf(
        DefaultChoice(DbDefaultText.MOVIE_VERSION_THEATRICAL, DbDefaultIcon.MOVIE_TICKET),
        DefaultChoice(DbDefaultText.MOVIE_VERSION_DIRECTORS_CUT, DbDefaultIcon.MOVIE_REEL),
        DefaultChoice(DbDefaultText.MOVIE_VERSION_EXTENDED_CUT, DbDefaultIcon.MORE_TIME),
    ),
    isImportant = true,
)
