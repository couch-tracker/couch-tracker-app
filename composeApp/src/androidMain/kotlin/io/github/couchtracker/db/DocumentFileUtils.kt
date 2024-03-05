package io.github.couchtracker.db

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.datetime.Instant

/**
 * Returns a [DocumentFile] representing [this] single [Uri].
 */
fun Uri.toDocumentFile(context: Context): DocumentFile {
    return DocumentFile.fromSingleUri(context, this) ?: error("should always be not null on API >= 19")
}

/**
 * Gets the [DocumentFile.lastModified] value as an [Instant].
 * Returns `null` if it's not available.
 */
fun DocumentFile.lastModifiedInstant(): Instant? {
    return lastModified().takeIf { it != 0L }?.let { Instant.fromEpochMilliseconds(it) }
}
