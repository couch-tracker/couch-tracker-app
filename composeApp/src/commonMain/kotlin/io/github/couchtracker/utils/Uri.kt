package io.github.couchtracker.utils

interface Uri {
    val scheme: String?
    val schemeSpecificPart: String?
    val authority: String?
    val userInfo: String?
    val host: String?
    val port: Int?
    val path: String?
    val query: String?
    val fragment: String?

    fun serialize(): String
}

class UriParseException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause) {
    constructor(cause: Throwable) : this(cause.message, cause)
}

expect fun parseUri(value: String): Uri
