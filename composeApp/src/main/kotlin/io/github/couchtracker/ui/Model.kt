package io.github.couchtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbImages
import coil3.request.ImageRequest

data class ImageModel(
    val aspectRatio: Float?,
    val url: ImageUrlProvider?,
    val placeholderUrl: ImageUrlProvider? = null,
) {
    @Composable
    @ReadOnlyComposable
    fun getCoilModel(width: Int, height: Int): ImageRequest {
        return ImageRequest.Builder(LocalContext.current)
            .data(url?.getUrl(width, height))
            .placeholderMemoryCacheKey(placeholderUrl?.getUrl(width, width))
            .build()
    }
}

sealed interface ImageUrlProvider {
    fun getUrl(width: Int, height: Int): String
    data class TmdbImage(val tmdbImage: app.moviebase.tmdb.image.TmdbImage) : ImageUrlProvider {
        override fun getUrl(width: Int, height: Int): String {
            return TmdbImageUrlBuilder.build(tmdbImage, width, height)
        }
    }

    data class TmdbFileImage(
        val tmdbImage: app.moviebase.tmdb.model.TmdbFileImage,
        val type: TmdbImageType,
    ) : ImageUrlProvider {
        override fun getUrl(width: Int, height: Int): String {
            return TmdbImageUrlBuilder.build(
                imagePath = tmdbImage.filePath,
                type = type,
                width = width,
                height = height,
            )
        }
    }

    data class Constant(val url: String) : ImageUrlProvider {
        override fun getUrl(width: Int, height: Int): String {
            return url
        }
    }
}

fun TmdbImage.toImageModel(): ImageModel {
    return ImageModel(
        url = ImageUrlProvider.TmdbImage(this),
        aspectRatio = null,
        placeholderUrl = null,
    )
}

private fun TmdbFileImage.toImageModel(type: TmdbImageType): ImageModel {
    return ImageModel(
        url = ImageUrlProvider.TmdbFileImage(this, type),
        aspectRatio = aspectRatio,
        placeholderUrl = null,
    )
}

fun TmdbImages.toImageModel(
    includeBackdrops: Boolean = true,
    includePosters: Boolean = true,
    includeStills: Boolean = true,
    includeLogos: Boolean = true,
): List<ImageModel> {
    val ret = mutableListOf<Pair<ImageModel, Float?>>()
    if (includeBackdrops) {
        backdrops.mapTo(ret) { it.toImageModel(TmdbImageType.BACKDROP) to it.voteAverage }
    }
    if (includePosters) {
        posters.mapTo(ret) { it.toImageModel(TmdbImageType.POSTER) to it.voteAverage }
    }
    if (includeStills) {
        stills.mapTo(ret) { it.toImageModel(TmdbImageType.BACKDROP) to it.voteAverage }
    }
    if (includeLogos) {
        logos.mapTo(ret) { it.toImageModel(TmdbImageType.LOGO) to it.voteAverage }
    }
    return ret.sortedByDescending { it.second }.map { it.first }
}
