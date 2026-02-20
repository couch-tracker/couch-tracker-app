package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId

/**
 * Implementation of a [ColumnAdapter] that allows to save and extract [ExternalId]s in the database.
 *
 * @property type the type of [ExternalId]. This should just be the companion object instance of your [ExternalId] type.
 * @see ExternalId
 */
private class TypelessExternalIdColumnAdapter<EID : ExternalId>(
    private val type: ExternalId.SealedInterfacesCompanion<EID>,
) : ColumnAdapter<EID, String> {

    override fun decode(databaseValue: String): EID {
        return type.parse(databaseValue)
    }

    override fun encode(value: EID): String {
        return type.serialize(value)
    }
}

/**
 * Returns a [ColumnAdapter] that uses serialization that includes the type, suitable when there is more than one possible type of
 * [ExternalId] for a column (e.g. show or movie)
 *
 * @see ExternalId.serialize
 * @see ExternalId.parse
 */
inline fun <reified EID : ExternalId> ExternalId.Companion.columnAdapter() = object : ColumnAdapter<EID, String> {
    override fun decode(databaseValue: String): EID = parse<EID>(databaseValue)
    override fun encode(value: EID) = value.serialize()
}

/**
 * Returns an [ColumnAdapter] to that uses partial (type-less) serialization.
 * Prefer using this rather than [ExternalId.Companion.columnAdapter] when only a single type (e.g. [ExternalMovieId]) is needed.
 *
 * @see ExternalId.SealedInterfacesCompanion.serialize
 * @see ExternalId.SealedInterfacesCompanion.parse
 */
fun <EID : ExternalId> ExternalId.SealedInterfacesCompanion<EID>.columnAdapter(): ColumnAdapter<EID, String> =
    TypelessExternalIdColumnAdapter(this)
