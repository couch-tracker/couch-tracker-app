package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText

object WatchedItemDimensionDeviceTypeDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.DEVICE_TYPE.toDbText(),
    choices = listOf(
        DefaultChoice(DbDefaultText.DEVICE_TYPE_TV, DbDefaultIcon.TV),
        DefaultChoice(DbDefaultText.DEVICE_TYPE_TABLET, DbDefaultIcon.TABLET),
        DefaultChoice(DbDefaultText.DEVICE_TYPE_PHONE, DbDefaultIcon.SMARTPHONE),
        DefaultChoice(DbDefaultText.DEVICE_TYPE_LAPTOP, DbDefaultIcon.LAPTOP),
        DefaultChoice(DbDefaultText.DEVICE_TYPE_MONITOR, DbDefaultIcon.MONITOR),
        DefaultChoice(DbDefaultText.DEVICE_TYPE_PROJECTOR, DbDefaultIcon.PROJECTOR),
        DefaultChoice(DbDefaultText.DEVICE_TYPE_VR, DbDefaultIcon.VR_HEADSET),
    ),
)
