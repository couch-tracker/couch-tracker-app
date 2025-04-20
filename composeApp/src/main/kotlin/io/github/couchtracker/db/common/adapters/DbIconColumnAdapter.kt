package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.user.model.icon.DbIcon

val DbIconColumnAdapter: ColumnAdapter<DbIcon, String> = CouchTrackerUriColumnAdapter.map(
    encoder = { it.toCouchTrackerUri() },
    decoder = { DbIcon.fromUri(it) },
)
