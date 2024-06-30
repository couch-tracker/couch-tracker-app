package io.github.couchtracker.utils

import android.os.Looper

fun requireMainThread(lazyMessage: (() -> String)? = null) {
    require(
        value = Looper.getMainLooper().isCurrentThread,
        lazyMessage = lazyMessage ?: { "this method can only be called by the main thread" },
    )
}
