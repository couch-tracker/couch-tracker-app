package io.github.couchtracker.tmdb

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.moviebase.tmdb.Tmdb3
import app.moviebase.tmdb.model.TmdbPageResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TmdbPagingSource<T : Any, O : Any>(
    val downloader: suspend Tmdb3.(page: Int) -> TmdbPageResult<T>,
    val mapper: suspend (T) -> O?,
) : PagingSource<Int, O>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, O> {
        try {
            val nextPageNumber = params.key ?: 1
            val response = tmdbDownload { tmdb3 ->
                tmdb3.downloader(nextPageNumber)
            }
            val mapped = coroutineScope {
                response.results.map {
                    async { mapper(it) }
                }.awaitAll()
            }
            val loadedBefore = ((response.page - 1) * TMDB_ITEMS_PER_PAGE).coerceAtMost(response.totalResults)
            return LoadResult.Page(
                data = mapped.filterNotNull(),
                prevKey = null,
                nextKey = if (response.page < response.totalPages) response.page + 1 else null,
                itemsBefore = loadedBefore,
                itemsAfter = response.totalResults - loadedBefore - response.results.size,
            )
        } catch (e: TmdbException) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, O>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
