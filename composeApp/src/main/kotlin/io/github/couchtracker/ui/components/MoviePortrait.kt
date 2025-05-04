package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.toInternalTmdbMovie
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.toImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
                )
            }
        },
        label = {
            val label = when {
                movie == null -> ""
                movie.year != null -> "${movie.title} (${movie.year})" // TODO translate
                else -> movie.title
            }
            Text(
                label,
                textAlign = TextAlign.Center,
                minLines = if (movie == null) 2 else 1,
            )
        },
    )
}

data class MoviePortraitModel(
    val movie: TmdbMovie,
    val title: String,
    val year: Int?,
    val posterModel: ImageModel?,
) {
    companion object {

        suspend fun fromTmdbMovie(
            tmdbCache: TmdbCache,
            movie: TmdbMovie,
            imagePreloadOptions: ImagePreloadOptions,
        ): MoviePortraitModel {
            val details = movie.details(tmdbCache)
            return MoviePortraitModel(
                movie = movie,
                title = details.title,
                year = details.releaseDate?.year,
                posterModel = details.posterImage?.toImageModel(imagePreloadOptions),
            )
        }

        suspend fun fromApiTmdbMovie(
            movie: TmdbMovie,
            details: TmdbApiTmdbMovie,
            imagePreloadOptions: ImagePreloadOptions,
        ): MoviePortraitModel {
            return MoviePortraitModel(
                movie = movie,
                title = details.title,
                year = details.releaseDate?.year,
                posterModel = details.posterImage?.toImageModel(imagePreloadOptions),
            )
        }
    }
}

suspend fun List<TmdbMovie>.toMoviePortraitModels(
    tmdbCache: TmdbCache,
    imagePreloadOptions: ImagePreloadOptions,
): List<MoviePortraitModel> = coroutineScope {
    map {
        async(Dispatchers.IO) {
            MoviePortraitModel.fromTmdbMovie(tmdbCache, it, imagePreloadOptions)
        }
    }.awaitAll()
}

suspend fun TmdbApiTmdbMovie.toMoviePortraitModels(
    language: TmdbLanguage,
    imagePreloadOptions: ImagePreloadOptions,
): MoviePortraitModel {
    return MoviePortraitModel.fromApiTmdbMovie(toInternalTmdbMovie(language), this, imagePreloadOptions)
}
