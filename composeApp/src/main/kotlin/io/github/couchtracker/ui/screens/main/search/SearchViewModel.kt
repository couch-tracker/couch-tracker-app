package io.github.couchtracker.ui.screens.main.search

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.moviebase.tmdb.model.TmdbCollection
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbMovie
import app.moviebase.tmdb.model.TmdbPerson
import app.moviebase.tmdb.model.TmdbSearchableListItem
import app.moviebase.tmdb.model.TmdbShow
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.TmdbApiContext
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.movieGenres
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.tmdbApiContext
import io.github.couchtracker.tmdb.tmdbMovieId
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.tmdb.tmdbShowId
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.tmdb.tvGenres
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.FlowToStateCollector
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.api.ApiResult
import io.github.couchtracker.utils.collectFlow
import io.github.couchtracker.utils.emptyPager
import io.github.couchtracker.utils.flatMap
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel(
    initialMediaFilters: SearchMediaFilters,
) : ViewModel() {
    val apiContext: TmdbApiContext = tmdbApiContext()
    val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(viewModelScope)

    var lazyGridState by mutableStateOf(LazyGridState(0, 0))
        private set
    var searchParameters by mutableStateOf(
        SearchParameters(
            query = "",
            filters = initialMediaFilters,
            incomplete = true,
        ),
    )
        private set

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val currentSearchInstance = snapshotFlow { searchParameters }
        .debounce { if (it.incomplete) 300.milliseconds else 0.milliseconds }
        .distinctUntilChangedBy { it.query to it.filters }
        .combine(AppSettings.get { Tmdb.Languages }) { searchParameters, tmdbLanguages ->
            SearchInstance(
                searchParameters = searchParameters,
                tmdbLanguage = tmdbLanguages.current.apiLanguage,
                lazyGridState = LazyGridState(0, 0),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<SearchResultItem>> = currentSearchInstance
        .flatMapLatest { (searchParameters, tmdbLanguage) ->
            val (query, filters) = searchParameters

            val pager = if (searchParameters.isEmpty()) {
                emptyPager()
            } else {
                tmdbPager(
                    downloader = { page ->
                        when (filters.singleOrNull()) {
                            SearchableMediaType.MOVIE -> search.findMovies(
                                query,
                                page = page,
                                language = tmdbLanguage.apiParameter,
                            )

                            SearchableMediaType.SHOW -> search.findShows(
                                query,
                                page = page,
                                language = tmdbLanguage.apiParameter,
                            )

                            SearchableMediaType.PERSON -> search.findPeople(
                                query,
                                page = page,
                                language = tmdbLanguage.apiParameter,
                            )

                            null -> search.findMulti(query, page = page)
                        }
                    },
                    mapper = {
                        if (it.type !in filters) {
                            null
                        } else {
                            it.toModel(tmdbLanguage)
                        }
                    },
                )
            }
            pager.flow
        }
        .cachedIn(viewModelScope)

    data class ExplorePageModel(
        val movieGenres: List<TmdbGenre>,
        val tvGenres: List<TmdbGenre>,
    )

    val explorePageModel: Loadable<ApiResult<ExplorePageModel>> by flowCollector.collectFlow(
        flow = apiContext { languages ->
            val movie = movieGenres(languages.apiLanguage)
            val tv = tvGenres(languages.apiLanguage)
            movie.combine(tv) { m, t ->
                m.flatMap { movieGenres ->
                    t.map { tvGenres ->
                        ExplorePageModel(
                            movieGenres = movieGenres,
                            tvGenres = tvGenres,
                        )
                    }
                }
            }
        },
    )

    fun search(
        incomplete: Boolean,
        query: String = searchParameters.query,
        filters: SearchMediaFilters = searchParameters.filters,
    ) {
        searchParameters = SearchParameters(
            query = query,
            filters = filters,
            incomplete = incomplete,
        )
        lazyGridState = LazyGridState(0, 0)
    }

    fun clearResults() {
        search(incomplete = false, query = "")
    }

    fun retryMainPage() {
        viewModelScope.launch { apiContext.retryAll() }
    }
}

private val TmdbSearchableListItem.type
    get() = when (this) {
        is TmdbMovie -> SearchableMediaType.MOVIE
        is TmdbShow -> SearchableMediaType.SHOW
        is TmdbPerson -> SearchableMediaType.PERSON
        is TmdbCollection -> null
    }

private suspend fun TmdbSearchableListItem.toModel(language: TmdbLanguage): SearchResultItem? {
    return when (this) {
        is TmdbCollection -> null

        is TmdbMovie -> SearchResultItem(
            posterModel = posterImage?.toImageModel(),
            title = title,
            type = SearchableMediaType.MOVIE,
            tags = listOfNotNull(
                rating()?.formatted,
            ),
            trailingInfo = releaseDate?.year?.toString(),
            navigate = { it.navigateToMovie(tmdbMovieId, toBaseMovie(language)) },
        )

        is TmdbPerson -> SearchResultItem(
            posterModel = profileImage?.toImageModel(),
            title = name,
            type = SearchableMediaType.PERSON,
            tags = emptyList(),
            trailingInfo = null,
            navigate = { /* TODO */ },
        )

        is TmdbShow -> SearchResultItem(
            posterModel = posterImage?.toImageModel(),
            title = name,
            type = SearchableMediaType.SHOW,
            tags = emptyList(),
            trailingInfo = firstAirDate?.year?.toString(),
            navigate = { it.navigateToShow(tmdbShowId, toBaseShow(language)) },
        )
    }
}
