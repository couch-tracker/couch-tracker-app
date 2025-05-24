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
    return rawPath.split("/")
        .filter { it.isNotEmpty() }
        .map { decodeUriComponent(it) }
}

/**
 * Encodes the given [value] to be used, as a whole, in a URI query component, compliant with
 * [RFC 3986](https://datatracker.ietf.org/doc/html/rfc3986).
 */
fun encodeUriComponent(value: String): String {
    // Unfortunately there is no standard way to escape a URI component in the stdlib
    // URLEncoder.encode encodes spaces as `+`, so that's not RFC 3986 compliant
    return buildString {
        for (char in value) {
            when {
                char.isLetterOrDigit() || "-_.~".contains(char) -> append(char)
                else -> append("%" + char.code.toString(radix = 16).uppercase().padStart(2, '0'))
            }
        }
    }
}

private const val PERCENT_ESCAPE_LENGTH = 3

/**
 * Decodes the given [value] to be used, as a whole, in a URI query component, compliant with
 * [RFC 3986](https://datatracker.ietf.org/doc/html/rfc3986).
 */
fun decodeUriComponent(value: String): String {
    // Unfortunately there is no standard way to escape a URI component in the stdlib
    // URLDecoder.decode decodes `+` as spaces, so that's not RFC 3986 compliant
    return buildString {
        var i = 0
        while (i < value.length) {
            if (value[i] == '%' && i + 2 < value.length) {
                val hex = value.substring(i + 1, i + PERCENT_ESCAPE_LENGTH)
                val decodedChar = hex.toInt(radix = 16).toChar()
                append(decodedChar)
                i += PERCENT_ESCAPE_LENGTH
            } else {
                append(value[i])
                i++
            }
        }
    }
}
