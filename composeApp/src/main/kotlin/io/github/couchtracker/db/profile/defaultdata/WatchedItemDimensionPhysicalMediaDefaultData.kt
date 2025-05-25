package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText

object WatchedItemDimensionPhysicalMediaDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.PHYSICAL_MEDIA_TYPE.toDbText(),
    choices = listOf(
        DefaultChoice(DbDefaultText.PHYSICAL_MEDIA_TYPE_BLURAY, DbDefaultIcon.LOGO_BLURAY),
        DefaultChoice(DbDefaultText.PHYSICAL_MEDIA_TYPE_DVD, DbDefaultIcon.LOGO_DVD),
        DefaultChoice(DbDefaultText.PHYSICAL_MEDIA_TYPE_VHS, DbDefaultIcon.LOGO_VHS),
        DefaultChoice(DbDefaultText.PHYSICAL_MEDIA_TYPE_BETAMAX, DbDefaultIcon.LOGO_BETAMAX),
    ),
) {

    override fun enableIf(db: ProfileData) = listOfNotNull(
        db.watchedItemDimensionChoiceQueries.selectByName(
            dimensionName = DbDefaultText.SOURCE.toDbText(),
            choiceName = DbDefaultText.SOURCE_PHYSICAL_MEDIA.toDbText(),
        ).executeAsOneOrNull(),
    )
}
