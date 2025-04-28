package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText

object WatchedItemDimensionSourceDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.SOURCE.toDbText(),
    choices = listOf(
        DefaultChoice(DbDefaultText.SOURCE_CINEMA, DbDefaultIcon.CINEMA),
        DefaultChoice(DbDefaultText.SOURCE_STREAMING, DbDefaultIcon.STREAM),
        DefaultChoice(DbDefaultText.SOURCE_TV, DbDefaultIcon.LIVE_TV),
        DefaultChoice(DbDefaultText.SOURCE_PHYSICAL_MEDIA, DbDefaultIcon.DISC),
        DefaultChoice(DbDefaultText.SOURCE_DIGITAL_MEDIA, DbDefaultIcon.VIDEO_FILE),
    ),
)
