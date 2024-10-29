package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText

object WatchedItemDimensionStreamingProviderDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.STREAMING_PROVIDER.toDbText(),
    choices = listOf(
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_NETFLIX, DbDefaultIcon.LOGO_NETFLIX),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_PRIME_VIDEO, DbDefaultIcon.LOGO_PRIME_VIDEO),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_DISNEY_PLUS, DbDefaultIcon.LOGO_DISNEY_PLUS),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_APPLE_TV_PLUS, DbDefaultIcon.LOGO_APPLE_TV_PLUS),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_PARAMOUNT_PLUS, DbDefaultIcon.LOGO_PARAMOUNT_PLUS),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_MAX, DbDefaultIcon.LOGO_MAX),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_DISCOVERY_PLUS, DbDefaultIcon.LOGO_DISCOVERY_PLUS),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_HULU, DbDefaultIcon.LOGO_HULU),
        DefaultChoice(DbDefaultText.STREAMING_PROVIDER_CRUNCHYROLL, DbDefaultIcon.LOGO_CRUNCHYROLL),
    ),
) {

    override fun enableIf(db: ProfileData) = listOfNotNull(
        db.watchedItemDimensionChoiceQueries.selectByName(
            dimensionName = DbDefaultText.SOURCE.toDbText(),
            choiceName = DbDefaultText.SOURCE_STREAMING.toDbText(),
        ).executeAsOneOrNull(),
    )
}
