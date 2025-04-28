package io.github.couchtracker.db.profile

import io.github.couchtracker.db.common.adapters.TextIntColumnAdapter

/**
 * Collects all the custom metadata in the profile database.
 */
object ProfileDbMetadata {

    val DefaultDataVersion = MetadataItem(
        key = "defaultDataVersion",
        columnAdapter = TextIntColumnAdapter,
    )
}
