package io.github.couchtracker.intl

import android.icu.text.ListFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun formatAndList(items: List<String>): String {
    return withContext(Dispatchers.Default) {
        ListFormatter.getInstance().format(items)
    }
}
