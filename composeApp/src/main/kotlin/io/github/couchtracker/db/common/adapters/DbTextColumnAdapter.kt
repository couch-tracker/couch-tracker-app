package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.profile.model.text.DbText

val DbTextColumnAdapter: ColumnAdapter<DbText, String> = CouchTrackerUriColumnAdapter.map(
    encoder = { it.toCouchTrackerUri() },
    decoder = { DbText.fromUri(it) },
)
