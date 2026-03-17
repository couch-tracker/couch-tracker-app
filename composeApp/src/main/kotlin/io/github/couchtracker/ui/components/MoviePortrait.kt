package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.tmdbMovieId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.toImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.moviebase.tmdb.model.TmdbMovie as TmdbApiTmdbMovie

@Composable
fun MoviePortrait(
    modifier: Modifier,
    /** A nullable movie will render a placeholder */
    movie: MoviePortraitModel?,
    onClick: (MoviePortraitModel) -> Unit,
) {
    PortraitComposable(
        modifier,
        imageModel = movie?.let {
            { w, h ->
                movie.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.MOVIE.icon,
        label = movie?.titleWithYear.orEmpty(),
        labelMinLines = if (movie == null) 2 else 1,
        onClick = movie?.let {
            {
                onClick(movie)
            }
        },
    )
}

data class MoviePortraitModel(
    val id: TmdbMovieId,
    val title: String?,
    val year: Int?,
    val titleWithYear: String?,
    val posterModel: ImageModel?,
) {
    companion object {

        suspend fun fromApiTmdbMovie(
            context: Context,
            id: TmdbMovieId,
            details: TmdbApiTmdbMovie,
        ): MoviePortraitModel {
            val year = details.releaseDate?.year
            return MoviePortraitModel(
                id = id,
                title = details.title,
                year = year,
                titleWithYear = if (year == null) {
                    details.title
                } else {
                    withContext(Dispatchers.Default) {
                        context.getString(R.string.item_tile_with_year, details.title, year)
                    }
                },
                posterModel = details.posterImage?.toImageModel(),
            )
        }
    }
}

suspend fun TmdbApiTmdbMovie.toMoviePortraitModels(context: Context): MoviePortraitModel {
    return MoviePortraitModel.fromApiTmdbMovie(
        context = context,
        id = tmdbMovieId,
        details = this,
    )
}
