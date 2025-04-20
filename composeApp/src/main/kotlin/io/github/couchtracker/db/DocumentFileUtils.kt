package io.github.couchtracker.db

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.github.couchtracker.utils.toAndroidUri
import kotlinx.datetime.Instant
import java.net.URI

/**
 * Returns a [DocumentFile] representing [this] single [URI].
 */
fun URI.toDocumentFile(context: Context): DocumentFile {
    return DocumentFile.fromSingleUri(context, this.toAndroidUri()) ?: error("should always be not null on API >= 19")
}

/**
 * Gets the [DocumentFile.lastModified] value as an [Instant].
 * Returns `null` if it's not available.
 */
fun DocumentFile.lastModifiedInstant(): Instant? {
    return lastModified().takeIf { it != 0L }?.let { Instant.fromEpochMilliseconds(it) }
}
