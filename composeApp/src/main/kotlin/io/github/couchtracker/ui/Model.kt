package io.github.couchtracker.ui

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest

sealed interface ImagePreloadOptions {
    data object DoNotPreload : ImagePreloadOptions
    data class Preload(val context: Context, val width: Int, val height: Int) : ImagePreloadOptions
}

sealed interface ImageModel {
    fun getCoilModel(width: Int, height: Int): Any?

    data class Url(val urlProvider: (width: Int, height: Int) -> String) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return urlProvider(width, height)
        }
    }

    data class Preloaded(val imageRequest: ImageRequest) : ImageModel {
        override fun getCoilModel(width: Int, height: Int): Any? {
            return imageRequest
        }
    }
}

suspend fun prepareImage(
    options: ImagePreloadOptions,
    urlProvider: (width: Int, height: Int) -> String,
): ImageModel? {
    return when (options) {
        ImagePreloadOptions.DoNotPreload -> ImageModel.Url(urlProvider)
        is ImagePreloadOptions.Preload -> {
            val request = ImageRequest.Builder(options.context)
                .data(urlProvider(options.width, options.height))
                .size(options.width, options.height)
                .build()
            SingletonImageLoader.get(options.context).execute(request)
            ImageModel.Preloaded(request)
        }
    }
}
