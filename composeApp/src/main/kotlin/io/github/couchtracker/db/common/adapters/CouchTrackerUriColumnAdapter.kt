package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.db.profile.CouchTrackerUri

val CouchTrackerUriColumnAdapter = URIColumnAdapter.map(
    encoder = { it.uri },
    decoder = { CouchTrackerUri(it) },
)
