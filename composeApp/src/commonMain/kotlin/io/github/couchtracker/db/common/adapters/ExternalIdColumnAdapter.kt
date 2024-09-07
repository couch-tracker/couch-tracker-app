package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.user.ExternalId

/**
 * Implementation of a [ColumnAdapter] that allows to save and extract [ExternalId]s in the database.
 *
 * @property type the type of [ExternalId]. This should just be the companion object instance of your [ExternalId] type.
 * @see ExternalId
 */
class ExternalIdColumnAdapter<EID : ExternalId>(
    private val type: ExternalId.SealedInterfacesCompanion<EID>,
) : ColumnAdapter<EID, String> {

    override fun decode(databaseValue: String): EID {
        return type.parse(databaseValue)
    }

    override fun encode(value: EID): String {
        return value.serialize()
    }
}

/**
 * Returns an [ExternalIdColumnAdapter] to encode and decode the [EID] external ID type.
 */
fun <EID : ExternalId> ExternalId.SealedInterfacesCompanion<EID>.columnAdapter() = ExternalIdColumnAdapter(this)
