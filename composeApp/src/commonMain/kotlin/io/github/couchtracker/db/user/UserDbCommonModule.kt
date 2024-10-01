package io.github.couchtracker.db.user

import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.PartialDateTimeColumnAdapter
import io.github.couchtracker.db.common.adapters.WatchableExternalIdColumnAdapter
import io.github.couchtracker.db.common.adapters.columnAdapter
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
        )
    }
}
