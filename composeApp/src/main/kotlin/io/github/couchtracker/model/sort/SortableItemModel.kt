package io.github.couchtracker.model.sort

import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.tmdb.TmdbRating
import kotlin.time.Duration

data class SortableItemModel(
    val identifier: Any,
    val name: SortableProperty<String>,
    val tmdbRating: SortableProperty<TmdbRating>,
    val firstPublicRelease: SortableProperty<PartialDateTime.Local>,
    val totalRuntime: SortableProperty<Duration>,
    val episodeRuntime: SortableProperty<Duration>,
) {
    companion object {
        fun error(identifier: Any) = SortableItemModel(
            identifier = identifier,
            name = SortableProperty.Error,
            tmdbRating = SortableProperty.Error,
            firstPublicRelease = SortableProperty.Error,
            totalRuntime = SortableProperty.Error,
            episodeRuntime = SortableProperty.Error,
        )
    }
}
