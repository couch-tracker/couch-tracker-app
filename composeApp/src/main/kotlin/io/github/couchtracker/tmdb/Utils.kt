package io.github.couchtracker.tmdb

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.palette.graphics.Palette
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ibm.icu.util.ULocale
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.ui.generateColorScheme

suspend fun TmdbImage?.prepareAndExtractColorScheme(
    ctx: Context,
    width: Int,
    height: Int,
    fallbackColorScheme: ColorScheme,
): Pair<ImageRequest?, ColorScheme> {
    if (this != null) {
        val url = TmdbImageUrlBuilder.build(this, width, height)
        val imageRequest = ImageRequest.Builder(ctx)
            // Necessary for palette generation
            .allowHardware(false)
            .data(url)
            .size(width, height)
            .build()
        val image = ctx.imageLoader.execute(imageRequest).image
        if (image != null) {
            val bitmap = image.toBitmap()
            bitmap.prepareToDraw()
            val palette = Palette.Builder(bitmap).generate()
            return imageRequest to palette.generateColorScheme()
        } else {
            return imageRequest to fallbackColorScheme
        }
    } else {
        return null to fallbackColorScheme
    }
}

fun TmdbImages.linearize(): List<TmdbFileImage> {
    return (backdrops + posters).sortedByDescending { it.voteAverage }
}

fun List<TmdbCrew>.directors(): List<TmdbCrew> {
    return filter { it.job == "Director" }
}

fun TmdbMovieDetail.language(): Bcp47Language {
    val allLocales = ULocale.getAvailableLocales()
    return originCountry
        .map { ULocale(originalLanguage, it) }
        .firstOrNull { it in allLocales }
        ?.let { Bcp47Language(it) }
        ?: Bcp47Language.of(originalLanguage)
}
