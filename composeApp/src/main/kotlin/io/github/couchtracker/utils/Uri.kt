package io.github.couchtracker.utils

import android.net.Uri
import androidx.core.net.toUri
import java.net.URI

/**
 * Converts Java [URI] to an Android [Uri].
 */
fun URI.toAndroidUri(): Uri {
    return toString().toUri()
}

/**
 * Converts an Android [Uri] to a Java [URI].
 */
fun Uri.toJavaUri(): URI {
    return URI(toString())
}

fun URI.pathSegments(): List<String> {
    return path
        ?.removePrefix("/")
        ?.removeSuffix("/")
        ?.takeIf { it.isNotEmpty() }
        ?.split('/')
        .orEmpty()
}

/**
 * Encodes the given [value] to be used, as a whole, in a URI query component.
 *
 * Warning: this function will not escape `&`, `=` or other similar characters, as this is NOT encoding for
 * `application/x-www-form-urlencoded`.
 */
fun encodeUriQuery(value: String): String {
    return URI("scheme", "authority", "/path", value, "fragment").rawQuery
}
