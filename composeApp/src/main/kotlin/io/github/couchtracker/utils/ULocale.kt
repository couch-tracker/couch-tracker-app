package io.github.couchtracker.utils

import com.ibm.icu.util.ULocale

fun ULocale.stripExtensions(): ULocale {
    val id = toString()
    return ULocale(
        when {
            '@' in id -> id.removeRange(id.indexOf('@')..<id.length)
            else -> id
        },
    )
}
