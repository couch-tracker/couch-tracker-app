package io.github.couchtracker.model.sort

import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.tmdb.TmdbRating

data class SortableItemModel(
    val name: SortableProperty<String>,
    val tmdbRating: SortableProperty<TmdbRating>,
    val firstPublicRelease: SortableProperty<PartialDateTime.Local>,
)
