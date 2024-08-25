package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.db.user.CouchTrackerUri

val CouchTrackerUriColumnAdapter = UriColumnAdapter.map(
    encoder = { it.uri },
    decoder = { CouchTrackerUri(it) },
)
