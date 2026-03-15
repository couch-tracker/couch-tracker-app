package io.github.couchtracker.utils.error

import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str

data class UnsupportedItemError(val unsupportedItem: ExternalId) : CouchTrackerError {
    override val debugMessage = "Unsupported item"
    override val cause = null
    override val title = Text.Resource(R.string.unsupported_item_id)
    override val details = Text.Lambda { R.string.unsupported_item_id_description.str(unsupportedItem.serialize()) }
    override val isRetriable = false
}
