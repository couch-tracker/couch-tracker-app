package io.github.couchtracker.ui.screens.main.search

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.movieGenres
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.tmdb.tmdbMovieId
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.tmdb.tmdbShowId
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.tmdb.tvGenres
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.collectWithPrevious
import io.github.couchtracker.utils.emptyPager
import io.github.couchtracker.utils.error.ApiResult
import io.github.couchtracker.utils.flatMap
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel(
    initialMediaFilters: SearchMediaFilters,
) : ViewModel() {
    val retryContext: TmdbFlowRetryContext = tmdbFlowRetryContext()

    var lazyGridState by mutableStateOf(LazyGridState(0, 0))
        private set
    var searchFieldState by mutableStateOf(TextFieldState())
    var mediaFilters by mutableStateOf(initialMediaFilters)
    private var searchRequestId by mutableStateOf(0)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val currentSearchInstance = snapshotFlow {
        SearchParameters(
            query = searchFieldState.text.toString(),
            filters = mediaFilters,
            searchRequestId = 0,
        )
    }
        // The current SearchParameters, combined with the previous request id
        .collectWithPrevious { previous: Pair<Int, SearchParameters>?, value ->
            (previous?.second?.searchRequestId ?: 0) to value
        }
        // Debounce if search id did not change
        .debounce { (prevId, params) -> if (prevId == params.searchRequestId) 300.milliseconds else 0.milliseconds }
        .map { (_, params) -> params.query to params.filters }
        .distinctUntilChanged()
        .combine(AppSettings.get { Tmdb.Languages }) { searchParameters, tmdbLanguages ->
            SearchInstance(
                query = searchParameters.first,
                filters = searchParameters.second,
                tmdbLanguage = tmdbLanguages.current.apiLanguage,
                lazyGridState = LazyGridState(0, 0),
            )
        }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<SearchResultItem>> = currentSearchInstance
        .flatMapLatest { (query, filters, tmdbLanguage) ->
            val pager = if (query.isBlank()) {
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

    val explorePageModel: Loadable<ApiResult<ExplorePageModel>> by retryContext { languages ->
        val movie = movieGenres(languages.apiLanguage)
        val tv = tvGenres(languages.apiLanguage)
        movie.combine(tv) { m, t ->
            m.flatMap { movieGenres ->
                t.map { tvGenres ->
                    ExplorePageModel(
                        movieGenres = movieGenres.sortedBy { it.name },
                        tvGenres = tvGenres.sortedBy { it.name },
                    )
                }
            }
        }
    }.collectAsLoadable()

    fun search() {
        searchRequestId++
        lazyGridState = LazyGridState(0, 0)
    }

    fun clearResults() {
        searchFieldState.setTextAndPlaceCursorAtEnd("")
        search()
    }

    fun retryMainPage() {
        viewModelScope.launch { retryContext.retryAll() }
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
