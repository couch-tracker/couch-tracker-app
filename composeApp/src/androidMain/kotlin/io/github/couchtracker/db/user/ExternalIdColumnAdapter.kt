package io.github.couchtracker.db.user

import app.cash.sqldelight.ColumnAdapter

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
fun <EID : ExternalId> ExternalId.SealedInterfacesCompanion<EID>.columnAdapter() = ExternalIdColumnAdapter(this)
