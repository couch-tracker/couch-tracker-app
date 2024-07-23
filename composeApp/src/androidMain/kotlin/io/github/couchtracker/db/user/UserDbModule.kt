package io.github.couchtracker.db.user

import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.PartialDateTimeColumnAdapter
import io.github.couchtracker.db.user.movie.ExternalMovieId
import io.github.couchtracker.db.user.show.ExternalShowId
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val UserDbModule = module {
    factory(named("UserDb")) {
        AndroidSqliteDriverFactory(schema = UserData.Schema)
    }.bind<SqliteDriverFactory>()

    factory<UserData> { params ->
        UserData(
            driver = params.get(),
            ShowInCollectionAdapter = ShowInCollection.Adapter(
                showIdAdapter = ExternalShowId.columnAdapter(),
                addDateAdapter = InstantColumnAdapter,
            ),
            WatchedMovieAdapter = WatchedMovie.Adapter(
                movieIdAdapter = ExternalMovieId.columnAdapter(),
                addedAtAdapter = InstantColumnAdapter,
                watchedAtAdapter = PartialDateTimeColumnAdapter,
            ),
        )
    }
}
