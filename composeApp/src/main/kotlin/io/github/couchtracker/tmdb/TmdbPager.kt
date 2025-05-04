package io.github.couchtracker.tmdb

import androidx.paging.Pager
import androidx.paging.PagingConfig
import app.moviebase.tmdb.Tmdb3
import app.moviebase.tmdb.model.TmdbPageResult

/**
 * Paginated TMDB APIs return a (non-configurable) maximum of 20 items.
 * See https://www.themoviedb.org/talk/587bea71c3a36846c300ff73
 */
const val TMDB_ITEMS_PER_PAGE = 20

fun <T : Any, O : Any> tmdbPager(
    downloader: suspend Tmdb3.(page: Int) -> TmdbPageResult<T>,
    mapper: suspend (T) -> O?,
): Pager<Int, O> {
    return Pager(
        config = PagingConfig(
            pageSize = TMDB_ITEMS_PER_PAGE,
        ),
        initialKey = 1,
        pagingSourceFactory = {
            TmdbPagingSource(downloader, mapper)
        },
    )
}
