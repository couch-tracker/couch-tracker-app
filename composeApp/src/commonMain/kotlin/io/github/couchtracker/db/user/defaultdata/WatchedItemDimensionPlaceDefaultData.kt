package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.user.model.icon.DbDefaultIcon
import io.github.couchtracker.db.user.model.text.DbDefaultText

object WatchedItemDimensionPlaceDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.PLACE.toDbText(),
    choices = listOf(
        DefaultChoice(DbDefaultText.PLACE_HOME, DbDefaultIcon.HOUSE),
        DefaultChoice(DbDefaultText.PLACE_FRIENDS_HOUSE, DbDefaultIcon.FRIENDS),
        DefaultChoice(DbDefaultText.PLACE_CINEMA, DbDefaultIcon.CINEMA),
        DefaultChoice(DbDefaultText.PLACE_VACATION, DbDefaultIcon.VACATION),
        DefaultChoice(DbDefaultText.PLACE_CAR, DbDefaultIcon.CAR),
        DefaultChoice(DbDefaultText.PLACE_PLANE, DbDefaultIcon.PLANE),
        DefaultChoice(DbDefaultText.PLACE_TRAIN, DbDefaultIcon.TRAIN),
        DefaultChoice(DbDefaultText.PLACE_BUS, DbDefaultIcon.BUS),
        DefaultChoice(DbDefaultText.PLACE_WORK, DbDefaultIcon.OFFICE_BUILDING),
        DefaultChoice(DbDefaultText.PLACE_SCHOOL, DbDefaultIcon.SCHOOL),
    ),
)
