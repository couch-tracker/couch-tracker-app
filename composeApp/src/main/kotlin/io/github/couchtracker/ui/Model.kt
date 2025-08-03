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

    data class Url(override val aspectRatio: Float?, val urlProvider: (width: Int, height: Int) -> String) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return urlProvider(width, height)
        }
    }

    data class Preloaded(override val aspectRatio: Float?, val coilModel: ImageRequest?) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return coilModel
        }
    }
}

suspend fun prepareImage(
    aspectRatio: Float?,
    options: ImagePreloadOptions = ImagePreloadOptions.DoNotPreload,
    urlProvider: (width: Int, height: Int) -> String,
): ImageModel {
    return when (options) {
        ImagePreloadOptions.DoNotPreload -> ImageModel.Url(aspectRatio, urlProvider)
        is ImagePreloadOptions.Preload -> {
            val request = ImageRequest.Builder(options.context)
                .data(urlProvider(options.width, options.height))
                .size(options.width, options.height)
                .build()
            val result = SingletonImageLoader.get(options.context).execute(request)
            val aspectRatio = result.image?.let { it.width.toFloat() / it.height } ?: aspectRatio
            ImageModel.Preloaded(aspectRatio, request)
        }
    }
}

suspend fun TmdbImage.toImageModel(
    imagePreloadOptions: ImagePreloadOptions = ImagePreloadOptions.DoNotPreload,
): ImageModel {
    return prepareImage(null, imagePreloadOptions) { w, h ->
        TmdbImageUrlBuilder.build(this, w, h)
    }
}

suspend fun TmdbFileImage.toImageModel(
    type: TmdbImageType,
    imagePreloadOptions: ImagePreloadOptions = ImagePreloadOptions.DoNotPreload,
): ImageModel {
    return prepareImage(aspectRation, imagePreloadOptions) { w, h ->
        TmdbImageUrlBuilder.build(
            imagePath = filePath,
            type = type,
            width = w,
            height = h,
        )
    }
}
