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
import app.moviebase.tmdb.model.TmdbShowDetail
import coil3.Image
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ibm.icu.util.ULocale
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImageUrlProvider
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.utils.LocaleData
import io.github.couchtracker.utils.logExecutionTime
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.minutes

private const val LOG_TAG = "Tmdb.Utils"

// See Palette.DEFAULT_RESIZE_BITMAP_AREA
private const val PALETTE_IMAGE_SIZE = 112

suspend fun TmdbImage?.prepareAndExtractColorScheme(
    ctx: Context,
    fallbackColorScheme: ColorScheme,
): Pair<ImageModel?, ColorScheme> {
    if (this != null) {
        return logExecutionTime(LOG_TAG, "Loading backdrop with color palette") {
            val smallUrl = TmdbImageUrlBuilder.build(this, PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE)
            val (smallImage, palette) = loadPalette(ctx, smallUrl, PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE, fallbackColorScheme)
            val imageModel = ImageModel(
                url = ImageUrlProvider.TmdbImage(this),
                placeholderUrl = ImageUrlProvider.Constant(smallUrl),
                aspectRatio = smallImage?.let { it.width.toFloat() / it.height },
            )
            imageModel to palette
        }
    } else {
        return null to fallbackColorScheme
    }
}

private suspend fun loadPalette(
    ctx: Context,
    url: String,
    width: Int,
    height: Int,
    fallbackColorScheme: ColorScheme,
): Pair<Image?, ColorScheme> {
    val imageRequest = ImageRequest.Builder(ctx)
        // Necessary for palette generation
        .allowHardware(false)
        .memoryCacheKey(url)
        .data(url)
        .size(width, height)
        .build()
    val image = ctx.imageLoader.execute(imageRequest).image
    return if (image != null) {
        val bitmap = image.toBitmap()
        logExecutionTime(LOG_TAG, "Generating color palette") {
            val palette = Palette.Builder(bitmap).resizeBitmapArea(PALETTE_IMAGE_SIZE * PALETTE_IMAGE_SIZE).generate()
            image to palette.generateColorScheme(fallbackColorScheme)
        }
    } else {
        image to fallbackColorScheme
    }
}

fun TmdbImages.linearize(): List<TmdbFileImage> {
    return (backdrops + posters).sortedByDescending { it.voteAverage }
}

fun List<TmdbCrew>.directors(): List<TmdbCrew> {
    return filter { it.job == "Director" }
}

private fun language(
    originalLanguage: String,
    originCountry: List<String>,
    allLocales: List<ULocale> = KoinPlatform.getKoin().get<LocaleData>().allLocales,
): Bcp47Language {
    return originCountry
        .map { ULocale(originalLanguage, it) }
        .firstOrNull { it in allLocales }
        ?.let { Bcp47Language(it) }
        ?: Bcp47Language.of(originalLanguage)
}

fun TmdbMovieDetail.language(
    allLocales: List<ULocale> = KoinPlatform.getKoin().get<LocaleData>().allLocales,
): Bcp47Language {
    return language(originalLanguage, originCountry, allLocales)
}

fun TmdbShowDetail.language(
    allLocales: List<ULocale> = KoinPlatform.getKoin().get<LocaleData>().allLocales,
): Bcp47Language {
    return language(originalLanguage, originCountry, allLocales)
}

fun TmdbMovieDetail.runtime() = runtime?.takeIf { it > 0 }?.minutes
