package io.github.couchtracker.ui.screens.watchedItem

import android.content.Context
import androidx.compose.material3.ColorScheme
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.db.profile.asWatchable
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.map
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

data class WatchedItemsScreenModel(
    val id: WatchableExternalId,
    val itemType: WatchedItemType,
    val title: String,
    val runtime: Duration?,
    val originalLanguage: Bcp47Language,
    val backdrop: ImageModel?,
    val colorScheme: Deferred<ColorScheme?>,
    val fallbackColorScheme: ColorScheme,
) {

    companion object {
        suspend fun loadTmdbMovie(
            context: Context,
            movie: TmdbMovie,
            coroutineContext: CoroutineContext = Dispatchers.Default,
        ): ApiResult<WatchedItemsScreenModel> = coroutineScope {
            val details = movie.details.first()
            details.map { details ->
                val backdrop = async(coroutineContext) {
                    details.backdropImage?.toImageModelWithPlaceholder()
                }
                WatchedItemsScreenModel(
                    id = movie.id.toExternalId().asWatchable(),
                    itemType = WatchedItemType.MOVIE,
                    title = details.title,
                    runtime = details.runtime(),
                    originalLanguage = Bcp47Language.of(details.originalLanguage),
                    backdrop = backdrop.await(),
                    colorScheme = async { backdrop.await()?.extractColorScheme(context) },
                    fallbackColorScheme = ColorSchemes.Movie,
                )
            }
        }
    }
}
