package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt
import app.moviebase.tmdb.model.TmdbMovie as TmdbApiTmdbMovie

private const val POSTER_ASPECT_RATIO = 2f / 3

@Composable
fun MoviePortrait(
    modifier: Modifier,
    movie: MoviePortraitModel,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 8.dp, tonalElevation = 8.dp) {
            AsyncImage(
                model = movie.poster,
                modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT_RATIO).clickable {
                    onClick()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(8.dp))
        val label = if (movie.year != null) {
            "${movie.title} (${movie.year})"
        } else {
            movie.title
        }
        Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
    }
}

data class MoviePortraitModel(
    val movie: TmdbMovie,
    val title: String,
    val year: Int?,
    val poster: ImageRequest?,
) {
    companion object {
        val SUGGESTED_WIDTH = 120.dp

        suspend fun fromTmdbMovie(
            context: Context,
            tmdbCache: TmdbCache,
            movie: TmdbMovie,
            width: Int,
        ): MoviePortraitModel {
            val details = movie.details(tmdbCache)
            return MoviePortraitModel(
                movie = movie,
                title = details.title,
                year = details.releaseDate?.year,
                poster = preparePoster(width, details.posterImage, context),
            )
        }

        suspend fun fromApiTmdbMovie(
            context: Context,
            movie: TmdbMovie,
            details: TmdbApiTmdbMovie,
            width: Int,
        ): MoviePortraitModel {
            return MoviePortraitModel(
                movie = movie,
                title = details.title,
                year = details.releaseDate?.year,
                poster = preparePoster(width, details.posterImage, context),
            )
        }

        private suspend fun preparePoster(
            width: Int,
            posterImage: TmdbImage?,
            context: Context,
        ): ImageRequest? {
            val height = (width * POSTER_ASPECT_RATIO).roundToInt()
            return if (posterImage != null) {
                val url = TmdbImageUrlBuilder.build(posterImage, width, height)
                ImageRequest.Builder(context)
                    .data(url)
                    .size(width, height)
                    .build()
                    .also {
                        context.imageLoader.execute(it)
                    }
            } else {
                null
            }
        }
    }
}

suspend fun List<TmdbMovie>.toMoviePortraitModels(
    context: Context,
    tmdbCache: TmdbCache,
    width: Int,
): List<MoviePortraitModel> = coroutineScope {
    map {
        async(Dispatchers.IO) {
            MoviePortraitModel.fromTmdbMovie(context, tmdbCache, it, width)
        }
    }.awaitAll()
}

suspend fun List<TmdbApiTmdbMovie>.toMoviePortraitModels(
    context: Context,
    language: TmdbLanguage,
    width: Int,
): List<MoviePortraitModel> = coroutineScope {
    map {
        val movie = TmdbMovie(TmdbMovieId(it.id), language)
        async(Dispatchers.IO) {
            MoviePortraitModel.fromApiTmdbMovie(context, movie, it, width)
        }
    }.awaitAll()
}
