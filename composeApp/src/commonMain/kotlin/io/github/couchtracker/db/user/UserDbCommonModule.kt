package io.github.couchtracker.db.user

import app.cash.sqldelight.EnumColumnAdapter
import io.github.couchtracker.db.common.adapters.DbIconColumnAdapter
import io.github.couchtracker.db.common.adapters.DbTextColumnAdapter
import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.PartialDateTimeColumnAdapter
import io.github.couchtracker.db.common.adapters.WatchableExternalIdColumnAdapter
import io.github.couchtracker.db.common.adapters.columnAdapter
import io.github.couchtracker.db.common.adapters.jsonAdapter
import io.github.couchtracker.db.common.adapters.jsonSet
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.user.show.ExternalShowId
import org.koin.dsl.module

val UserDbCommonModule = module {
    factory<UserData> { params ->
        UserData(
            driver = params.get(),
            ShowInCollectionAdapter = ShowInCollection.Adapter(
                showIdAdapter = ExternalShowId.columnAdapter(),
                addDateAdapter = InstantColumnAdapter,
            ),
            WatchedItemAdapter = WatchedItem.Adapter(
                itemIdAdapter = WatchableExternalIdColumnAdapter,
                addedAtAdapter = InstantColumnAdapter,
                watchAtAdapter = PartialDateTimeColumnAdapter,
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
        )
    }
}
