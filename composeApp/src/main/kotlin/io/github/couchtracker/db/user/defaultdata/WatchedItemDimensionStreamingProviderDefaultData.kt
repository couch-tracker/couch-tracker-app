package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.user.UserData
import io.github.couchtracker.db.user.model.icon.DbDefaultIcon
import io.github.couchtracker.db.user.model.text.DbDefaultText
import io.github.couchtracker.db.user.model.text.DbText

object WatchedItemDimensionStreamingProviderDefaultData : AbstractWatchedItemDimensionChoiceDefaultData(
    dimensionName = DbDefaultText.STREAMING_PROVIDER.toDbText(),
    choices = listOf(
        DefaultChoice(DbText.Custom("Netflix"), DbDefaultIcon.LOGO_NETFLIX),
        DefaultChoice(DbText.Custom("Prime video"), DbDefaultIcon.LOGO_AMAZON_PRIME_VIDEO),
        // TODO add more
    ),
) {

    override fun enableIf(db: UserData) = listOfNotNull(
        db.watchedItemDimensionChoiceQueries.selectByName(
            dimensionName = DbDefaultText.SOURCE.toDbText(),
            choiceName = DbDefaultText.SOURCE_STREAMING.toDbText(),
        ).executeAsOneOrNull(),
    )
}
