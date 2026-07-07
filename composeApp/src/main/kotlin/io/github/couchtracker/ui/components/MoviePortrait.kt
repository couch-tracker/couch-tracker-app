package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.components.MoviePortraitModel.DownloadState
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.error.ApiError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.moviebase.tmdb.model.TmdbMovie as TmdbApiTmdbMovie

@Composable
fun MoviePortrait(
    modifier: Modifier,
    /** A nullable movie will render a placeholder */
    movie: MoviePortraitModel?,
) {
    val navController = LocalNavController.current
    PortraitComposable(
        modifier,
        imageModel = movie?.let {
            { w, h ->
                movie.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.MOVIE.icon,
        overlayIcon = when (movie?.downloadState) {
            null, DownloadState.Downloaded -> null
            DownloadState.Error -> Icons.Outlined.Error
            DownloadState.NotFound -> Icons.Outlined.QuestionMark
        },
        label = movie?.label.orEmpty(),
        labelMinLines = if (movie == null) 2 else 1,
        onClick = if (movie?.id != null) {
            {
                navController.navigateToMovie(
                    id = movie.id,
                    preloadData = movie.preloadData,
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
        movie = model,
    )
}

data class MoviePortraitModel(
    val id: ExternalMovieId,
    val label: String?,
    val posterModel: ImageModel?,
    val preloadData: BaseTmdbMovie?,
    val downloadState: DownloadState = DownloadState.Downloaded,
) {

    enum class DownloadState {
        Downloaded,
        Error,
        NotFound,
    }

    companion object {

        suspend fun fromApiTmdbMovie(
            context: Context,
            details: TmdbApiTmdbMovie,
            preloadData: BaseTmdbMovie?,
        ): MoviePortraitModel {
            val year = details.releaseDate?.year
            return MoviePortraitModel(
                id = TmdbExternalMovieId(TmdbMovieId(details.id)),
                label = labelFromNameYear(context, details.title, year),
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        suspend fun fromApiTmdbMovie(
            context: Context,
            details: TmdbMovieDetail,
            preloadData: BaseTmdbMovie?,
        ): MoviePortraitModel {
            val year = details.releaseDate?.year
            return MoviePortraitModel(
                id = TmdbExternalMovieId(TmdbMovieId(details.id)),
                label = labelFromNameYear(context, details.title, year),
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun forDownloadState(id: ExternalMovieId, state: DownloadState, label: String? = null): MoviePortraitModel {
            return MoviePortraitModel(
                id = id,
                label = label,
                posterModel = null,
                downloadState = state,
                preloadData = null,
            )
        }

        private suspend fun labelFromNameYear(context: Context, title: String?, year: Int?): String? {
            return if (year == null) {
                title
            } else {
                withContext(Dispatchers.Default) {
                    context.getString(R.string.item_tile_with_year, title, year)
                }
            }
        }
    }
}

suspend fun TmdbApiTmdbMovie.toMoviePortraitModels(context: Context, language: TmdbLanguage): MoviePortraitModel {
    return MoviePortraitModel.fromApiTmdbMovie(context, this, preloadData = this.toBaseMovie(language))
}

suspend fun TmdbMovieDetail.toMoviePortraitModels(context: Context, language: TmdbLanguage): MoviePortraitModel {
    return MoviePortraitModel.fromApiTmdbMovie(context, this, preloadData = this.toBaseMovie(language))
}
