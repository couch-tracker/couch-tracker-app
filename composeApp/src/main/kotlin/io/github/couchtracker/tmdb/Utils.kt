package io.github.couchtracker.tmdb

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.palette.graphics.Palette
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbShowDetail
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ibm.icu.util.ULocale
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImageUrlProvider
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.LocaleData
import io.github.couchtracker.utils.logExecutionTime
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.minutes

private const val LOG_TAG = "Tmdb.Utils"

// See Palette.DEFAULT_RESIZE_BITMAP_AREA
private const val PALETTE_IMAGE_SIZE = 112

suspend fun ImageModel.extractColorScheme(ctx: Context): ColorScheme? {
    logExecutionTime(LOG_TAG, "Loading backdrop with color palette") {
        val smallUrl = url?.getUrl(PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE) ?: return null
        val imageRequest = ImageRequest.Builder(ctx)
            // Necessary for palette generation
            .allowHardware(false)
            .data(smallUrl)
            .size(PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE)
            .build()
        val image = ctx.imageLoader.execute(imageRequest).image ?: return null
        val bitmap = image.toBitmap()
        return logExecutionTime(LOG_TAG, "Generating color palette") {
            val palette = Palette.Builder(bitmap).resizeBitmapArea(PALETTE_IMAGE_SIZE * PALETTE_IMAGE_SIZE).generate()
            palette.generateColorScheme()
        }
    }
}

/**
 * Converts the image to an [ImageModel], which uses the same image used by [extractColorScheme] as a placeholder.
 */
fun TmdbImage.toImageModelWithPlaceholder(): ImageModel {
    val smallUrl = TmdbImageUrlBuilder.build(this, PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE)
    return toImageModel().copy(placeholderUrl = ImageUrlProvider.Constant(smallUrl))
}

fun List<TmdbCrew>.directors(): List<TmdbCrew> {
    return filter { it.job == "Director" }
}

private fun language(
    originalLanguage: String?,
    originCountry: List<String>,
    allLocales: List<ULocale> = KoinPlatform.getKoin().get<LocaleData>().allLocales,
): Bcp47Language? {
    return if (originalLanguage == null) {
        null
    } else {
        originCountry
            .map { ULocale(originalLanguage, it) }
            .firstOrNull { it in allLocales }
            ?.let { Bcp47Language(it) }
            ?: Bcp47Language.of(originalLanguage)
    }
}

fun TmdbMovieDetail.language(
    allLocales: List<ULocale> = KoinPlatform.getKoin().get<LocaleData>().allLocales,
): Bcp47Language? {
    return language(originalLanguage, originCountry, allLocales)
}

fun TmdbShowDetail.language(
    allLocales: List<ULocale> = KoinPlatform.getKoin().get<LocaleData>().allLocales,
): Bcp47Language? {
    return language(originalLanguage, originCountry, allLocales)
}

fun TmdbMovieDetail.runtime() = runtime?.takeIf { it > 0 }?.minutes

fun TmdbEpisode.runtime() = runtime?.takeIf { it > 0 }?.minutes
