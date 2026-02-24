package io.github.couchtracker.db.profile

import app.cash.sqldelight.EnumColumnAdapter
import io.github.couchtracker.db.common.adapters.Bcp47LanguageColumnAdapter
import io.github.couchtracker.db.common.adapters.DbIconColumnAdapter
import io.github.couchtracker.db.common.adapters.DbTextColumnAdapter
import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.PartialDateTimeColumnAdapter
import io.github.couchtracker.db.common.adapters.columnAdapter
import io.github.couchtracker.db.common.adapters.jsonAdapter
import io.github.couchtracker.db.common.adapters.jsonSet
import io.github.couchtracker.db.profile.defaultdata.ProfileDefaultDataHandler
import io.github.couchtracker.db.profile.externalids.BookmarkableExternalId
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import org.koin.dsl.module

val ProfileDbCommonModule = module {
    factory<ProfileData> { params ->
        ProfileData(
            driver = params.get(),
            BookmarkedItemAdapter = BookmarkedItem.Adapter(
                itemIdAdapter = ExternalId.columnAdapter<BookmarkableExternalId>(),
                addDateAdapter = InstantColumnAdapter,
            ),
            WatchedEpisodeAdapter = WatchedEpisode.Adapter(
                episodeIdAdapter = ExternalEpisodeId.columnAdapter(),
            ),
            WatchedEpisodeSessionAdapter = WatchedEpisodeSession.Adapter(
                showIdAdapter = ExternalShowId.columnAdapter(),
            ),
            WatchedItemAdapter = WatchedItem.Adapter(
                addedAtAdapter = InstantColumnAdapter,
                watchAtAdapter = PartialDateTimeColumnAdapter,
            ),
            WatchedMovieAdapter = WatchedMovie.Adapter(
                movieIdAdapter = ExternalMovieId.columnAdapter(),
            ),
            WatchedItemDimensionAdapter = WatchedItemDimension.Adapter(
                appliesToAdapter = EnumColumnAdapter<WatchedItemType>().jsonSet(),
                nameAdapter = DbTextColumnAdapter,
                typeAdapter = jsonAdapter<WatchedItemDimensionType>(),
            ),
            WatchedItemDimensionChoiceAdapter = WatchedItemDimensionChoice.Adapter(
                nameAdapter = DbTextColumnAdapter,
                iconAdapter = DbIconColumnAdapter,
            ),
            WatchedItemLanguageAdapter = WatchedItemLanguage.Adapter(
                languageAdapter = Bcp47LanguageColumnAdapter,
            ),
        ).also { db ->
            ProfileDefaultDataHandler.handle(db)
        }
    }
}
