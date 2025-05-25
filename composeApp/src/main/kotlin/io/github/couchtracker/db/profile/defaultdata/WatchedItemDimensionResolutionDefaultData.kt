package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.profile.model.text.DbDefaultText

object WatchedItemDimensionResolutionDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.RESOLUTION.toDbText(),
    choices = listOf(
        DefaultChoice(DbDefaultText.RESOLUTION_SD, icon = null),
        DefaultChoice(DbDefaultText.RESOLUTION_HD, icon = null),
        DefaultChoice(DbDefaultText.RESOLUTION_FHD, icon = null),
        DefaultChoice(DbDefaultText.RESOLUTION_4K, icon = null),
        DefaultChoice(DbDefaultText.RESOLUTION_8K, icon = null),
    ),
)
