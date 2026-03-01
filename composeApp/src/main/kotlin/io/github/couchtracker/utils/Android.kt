package io.github.couchtracker.utils

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

fun requireMainThread(lazyMessage: (() -> String)? = null) {
    require(
        value = Looper.getMainLooper().isCurrentThread,
        lazyMessage = lazyMessage ?: { "this method can only be called by the main thread" },
    )
}

fun CreationExtras.viewModelApplication(): Application {
    return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
}
