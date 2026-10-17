package io.github.couchtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.components.MoviePortraitModel.DownloadState
import io.github.couchtracker.ui.components.MoviePortraitModel.SecondaryInfo
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.error.ApiError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.valueOrNull
import app.moviebase.tmdb.model.TmdbMovie as TmdbApiTmdbMovie

@Composable
fun MoviePortrait(
    modifier: Modifier,
    /** A nullable movie will render a placeholder */
    movie: Loadable<MoviePortraitModel>,
    secondaryInfo: Set<SecondaryInfo>,
) {
    val navController = LocalNavController.current
    PortraitComposable(
        modifier,
        imageModel = movie.valueOrNull()?.let {
            { w, h ->
                it.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.MOVIE.icon,
        overlayIcon = when (movie) {
            is Loadable.Loaded -> when (movie.value.downloadState) {
                DownloadState.Downloaded -> null
                DownloadState.Error -> Icons.Outlined.Error
                DownloadState.NotFound -> Icons.Outlined.QuestionMark
            }
            Loadable.Loading -> null
        },
        titles = when (movie) {
            is Loadable.Loaded -> listOfNotNull(movie.value.label)
            // Some default space while downloading
            Loadable.Loading -> listOf(null)
        },
        subtitles = if (secondaryInfo.isNotEmpty()) {
            PortraitComposableDefaults.joinIntoSubtitle(
                tags = movie.map { movie ->
                    SecondaryInfo.entries
                        .filter { it in secondaryInfo }
                        .mapNotNull { it.toLabel(movie) }
                },
            )
        } else {
            emptyList()
        },
        onClick = if (movie is Loadable.Loaded) {
            {
                navController.navigateToMovie(
                    id = movie.value.id,
                    preloadData = movie.value.preloadData,
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun MoviePortrait(
    modifier: Modifier,
    movieId: ExternalMovieId,
    movieResult: CouchTrackerResult<MoviePortraitModel>,
    secondaryInfo: Set<SecondaryInfo>,
) {
    val model = when (movieResult) {
        is Result.Value -> movieResult.value
        is Result.Error -> MoviePortraitModel.forDownloadState(
            id = movieId,
            state = when (movieResult.error) {
                is ApiError.ItemNotFound -> DownloadState.NotFound
                is UnsupportedItemError -> DownloadState.NotFound
                else -> DownloadState.Error
            },
            label = movieId.serialize(),
        )
    }
    MoviePortrait(
        modifier = modifier,
        movie = Loadable.Loaded(model),
        secondaryInfo = secondaryInfo,
    )
}

data class MoviePortraitModel(
    val id: ExternalMovieId,
    val label: String?,
    val yearStr: String?,
    val ratingStr: String?,
    val posterModel: ImageModel?,
    val preloadData: BaseTmdbMovie?,
    val downloadState: DownloadState = DownloadState.Downloaded,
) {

    enum class SecondaryInfo(val toLabel: (MoviePortraitModel) -> String?) {
        Year({ it.yearStr }),
        Rating({ it.ratingStr }),
        ;

        companion object {
            // The year is sometimes necessary to disambiguate movies, e.g. Batman; always display it
            val DEFAULT_SECONDARY_INFO = setOf(Year)
            val EXPLORE_SECONDARY_INFO = setOf(Year, Rating)
        }
    }

    enum class DownloadState {
        Downloaded,
        Error,
        NotFound,
    }

    companion object {

        fun fromApiTmdbMovie(
            details: TmdbApiTmdbMovie,
            preloadData: BaseTmdbMovie?,
            rating: TmdbRating?,
        ): MoviePortraitModel {
            return MoviePortraitModel(
                id = TmdbExternalMovieId(TmdbMovieId(details.id)),
                label = details.title,
                yearStr = details.releaseDate?.year?.toString(),
                ratingStr = rating?.formatted,
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun fromApiTmdbMovie(
            details: TmdbMovieDetail,
            preloadData: BaseTmdbMovie?,
            rating: TmdbRating?,
        ): MoviePortraitModel {
            return MoviePortraitModel(
                id = TmdbExternalMovieId(TmdbMovieId(details.id)),
                label = details.title,
                yearStr = details.releaseDate?.year?.toString(),
                ratingStr = rating?.formatted,
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun forDownloadState(id: ExternalMovieId, state: DownloadState, label: String? = null): MoviePortraitModel {
            return MoviePortraitModel(
                id = id,
                label = label,
                yearStr = null,
                ratingStr = null,
                posterModel = null,
                downloadState = state,
                preloadData = null,
            )
        }
    }
}

fun TmdbApiTmdbMovie.toMoviePortraitModels(language: TmdbLanguage, rating: TmdbRating?): MoviePortraitModel {
    return MoviePortraitModel.fromApiTmdbMovie(this, preloadData = this.toBaseMovie(language), rating = rating)
}

fun TmdbMovieDetail.toMoviePortraitModels(language: TmdbLanguage, rating: TmdbRating?): MoviePortraitModel {
    return MoviePortraitModel.fromApiTmdbMovie(this, preloadData = this.toBaseMovie(language), rating = rating)
}
