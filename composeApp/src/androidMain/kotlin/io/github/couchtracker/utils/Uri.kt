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

    override fun serialize() = javaURI.toString()
}

actual fun parseUri(value: String): Uri = try {
    AndroidPlatformUri(JavaURI(value))
} catch (e: URISyntaxException) {
    throw UriParseException(e)
}

fun Uri.toAndroidUri(): AndroidUri {
    return AndroidUri.parse(serialize())
}

fun AndroidUri.toCouchTrackerUri(): Uri {
    return parseUri(toString())
}
