package io.github.couchtracker.db.user

import io.github.couchtracker.db.common.adapters.TextIntColumnAdapter

/**
 * Collects all the custom metadata in the user database.
 */
object UserDbMetadata {

    val DefaultDataVersion = MetadataItem(
        key = "defaultDataVersion",
        columnAdapter = TextIntColumnAdapter,
    )
}
