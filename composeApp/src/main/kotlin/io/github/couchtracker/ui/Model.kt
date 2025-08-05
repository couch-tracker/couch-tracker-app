package io.github.couchtracker.ui

import android.content.Context
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbFileImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest

sealed interface ImagePreloadOptions {
    data object DoNotPreload : ImagePreloadOptions
    data class Preload(val context: Context, val width: Int, val height: Int) : ImagePreloadOptions
}

sealed interface ImageModel {
    val aspectRatio: Float?
    fun getCoilModel(width: Int, height: Int): Any?

    data class TmdbImage(override val aspectRatio: Float?, val tmdbImage: app.moviebase.tmdb.image.TmdbImage) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return TmdbImageUrlBuilder.build(tmdbImage, width, height)
        }
    }

    data class TmdbFileImage(
        override val aspectRatio: Float?,
        val tmdbImage: app.moviebase.tmdb.model.TmdbFileImage,
        val type: TmdbImageType,
    ) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return TmdbImageUrlBuilder.build(
                imagePath = tmdbImage.filePath,
                type = type,
                width = width,
                height = height,
            )
        }
    }

    data class Preloaded(override val aspectRatio: Float?, val coilModel: ImageRequest?) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return coilModel
        }
    }
}

suspend fun prepareImage(
    baseModel: ImageModel,
    options: ImagePreloadOptions = ImagePreloadOptions.DoNotPreload,
): ImageModel {
    return when (options) {
        ImagePreloadOptions.DoNotPreload -> baseModel
        is ImagePreloadOptions.Preload -> {
            val request = ImageRequest.Builder(options.context)
                .data(baseModel.getCoilModel(options.width, options.height))
                .size(options.width, options.height)
                .build()
            val result = SingletonImageLoader.get(options.context).execute(request)
            val aspectRatio = result.image?.let { it.width.toFloat() / it.height } ?: baseModel.aspectRatio
            ImageModel.Preloaded(aspectRatio, request)
        }
    }
}

suspend fun TmdbImage.toImageModel(
    imagePreloadOptions: ImagePreloadOptions = ImagePreloadOptions.DoNotPreload,
): ImageModel {
    return prepareImage(
        baseModel = ImageModel.TmdbImage(null, this),
        options = imagePreloadOptions,
    )
}

suspend fun TmdbFileImage.toImageModel(
    type: TmdbImageType,
    imagePreloadOptions: ImagePreloadOptions = ImagePreloadOptions.DoNotPreload,
): ImageModel {
    return prepareImage(
        baseModel = ImageModel.TmdbFileImage(null, this, type),
        options = imagePreloadOptions,
    )
}
