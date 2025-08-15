package io.github.couchtracker.tmdb

import android.content.Context
import androidx.compose.ui.graphics.Color
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
import io.github.couchtracker.ui.mainColor
import io.github.couchtracker.utils.ALL_ULOCALES
import io.github.couchtracker.utils.logExecutionTime
import kotlin.time.Duration.Companion.minutes

private const val LOG_TAG = "Tmdb.Utils"

// See Palette.DEFAULT_RESIZE_BITMAP_AREA
private const val PALETTE_IMAGE_SIZE = 112

suspend fun TmdbImage.prepareAndExtractColorScheme(ctx: Context): Color? {
    logExecutionTime(LOG_TAG, "Loading backdrop with color palette") {
        val smallUrl = TmdbImageUrlBuilder.build(this, PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE)
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
            palette.mainColor()
        }
    }
}

fun TmdbImage.prepareMainImageRequest(
    ctx: Context,
    width: Int,
    height: Int,
): ImageRequest {
    val smallUrl = TmdbImageUrlBuilder.build(this, PALETTE_IMAGE_SIZE, PALETTE_IMAGE_SIZE)
    val url = TmdbImageUrlBuilder.build(this, width, height)
    return ImageRequest.Builder(ctx)
        .placeholderMemoryCacheKey(smallUrl)
        .data(url)
        .size(width, height)
        .build()
}

fun TmdbImages.linearize(): List<TmdbFileImage> {
    return (backdrops + posters).sortedByDescending { it.voteAverage }
}

fun List<TmdbCrew>.directors(): List<TmdbCrew> {
    return filter { it.job == "Director" }
}

suspend fun TmdbMovieDetail.language(): Bcp47Language {
    val allLocales = ALL_ULOCALES.await()
    return originCountry
        .map { ULocale(originalLanguage, it) }
        .firstOrNull { it in allLocales }
        ?.let { Bcp47Language(it) }
        ?: Bcp47Language.of(originalLanguage)
}

fun TmdbMovieDetail.runtime() = runtime?.takeIf { it > 0 }?.minutes
