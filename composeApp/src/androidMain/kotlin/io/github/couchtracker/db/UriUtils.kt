package io.github.couchtracker.db

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

fun Uri.toDocumentFile(context: Context): DocumentFile {
    return DocumentFile.fromSingleUri(context, this) ?: error("should always be not null on API >= 19")
}
