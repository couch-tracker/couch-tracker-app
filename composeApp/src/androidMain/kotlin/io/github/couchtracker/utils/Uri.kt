package io.github.couchtracker.utils

import java.net.URISyntaxException
import android.net.Uri as AndroidUri
import java.net.URI as JavaURI

private data class AndroidPlatformUri(val javaURI: JavaURI) : Uri {
    override val scheme: String? get() = javaURI.scheme
    override val schemeSpecificPart: String? get() = javaURI.schemeSpecificPart
    override val authority: String? get() = javaURI.authority
    override val userInfo: String? get() = javaURI.userInfo
    override val host: String? get() = javaURI.host
    override val port: Int? get() = javaURI.port.takeIf { it != -1 }
    override val path: String? get() = javaURI.path
    override val query: String? get() = javaURI.query
    override val fragment: String? get() = javaURI.fragment

    override fun toString() = javaURI.toString()
    override fun serialize() = javaURI.toString()
}

actual fun parseUri(value: String): Uri = try {
    AndroidPlatformUri(JavaURI(value))
} catch (e: URISyntaxException) {
    throw UriParseException(e)
}

/**
 * Converts internal URI representation ([io.github.couchtracker.utils.Uri]) to Android URI representation ([android.net.Uri]).
 */
fun Uri.toAndroidUri(): AndroidUri {
    return AndroidUri.parse(serialize())
}

/**
 * Converts Android URI representation ([android.net.Uri]) to the internal URI representation ([io.github.couchtracker.utils.Uri]).
 */
fun AndroidUri.toInternalUri(): Uri {
    return parseUri(toString())
}

actual fun encodeUriQuery(value: String): String {
    return JavaURI("scheme", "authority", "/path", value, "fragment").getRawQuery()
}
