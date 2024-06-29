package io.github.couchtracker.utils

import android.icu.text.ListFormatter

actual fun formatAndList(items: List<String>): String {
    return ListFormatter.getInstance().format(items)
}
