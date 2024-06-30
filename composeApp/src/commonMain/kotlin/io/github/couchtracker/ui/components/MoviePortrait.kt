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
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbMovie
import kotlin.math.roundToInt

private const val POSTER_ASPECT_RATIO = 2f / 3

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
            val height = (width * POSTER_ASPECT_RATIO).roundToInt()
            val details = movie.details(tmdbCache)
            val poster = details.posterImage
            val posterImageRequest: ImageRequest?
            if (poster != null) {
                val url = TmdbImageUrlBuilder.build(poster, width, height)
                posterImageRequest = ImageRequest.Builder(context)
                    .data(url)
                    .size(width, height)
                    .build()
                context.imageLoader.execute(posterImageRequest)
            } else {
                posterImageRequest = null
            }
            return MoviePortraitModel(
                movie = movie,
                title = details.title,
                year = details.releaseDate?.year,
                poster = posterImageRequest,
            )
        }
    }
}

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
