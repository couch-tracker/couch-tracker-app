package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.tmdbMovieId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
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
        image = { w, h ->
            if (movie != null) {
                AsyncImage(
                    model = movie.posterModel?.getCoilModel(w, h),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onClick(movie)
                        },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = rememberPlaceholderPainter(PlaceholdersDefaults.MOVIE.icon, isError = false),
                    error = rememberPlaceholderPainter(PlaceholdersDefaults.MOVIE.icon, isError = true),
                )
            }
        },
        label = {
            Text(
                movie?.titleWithYear.orEmpty(),
                textAlign = TextAlign.Center,
                minLines = if (movie == null) 2 else 1,
            )
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
