package io.github.couchtracker.db.user

import app.cash.sqldelight.ColumnAdapter

private val KEY_REGEX = "[a-z][A-Za-z]+".toRegex()

/**
 * Represents a single piece of metadata that is stored in the Metadata table.
 *
 * @param key the name of the metadata
 * @param columnAdapter the [ColumnAdapter] used to encode/decode data in the value column of type TEXT
 */
class MetadataItem<V : Any>(
    val key: String,
    private val columnAdapter: ColumnAdapter<V, String>,
) {
    init {
        require(key.matches(KEY_REGEX)) { "key contains invalid characters" }
    }

    /**
     * Gets the current value from [db]
     */
    fun getValue(db: UserData): V? {
        val value = db.metadataQueries.select(key).executeAsOneOrNull()
        return value?.let { columnAdapter.decode(it) }
    }

    /**
     * Sets the given [value] to [db]. If [value] is null, the metadata key is deleted
     */
    fun setValue(db: UserData, value: V?) {
        if (value == null) {
            db.metadataQueries.delete(key)
        } else {
            db.metadataQueries.upsert(key, columnAdapter.encode(value))
        }
    }

    /**
     * Deletes the metadata key, equivalent of calling [setValue] with a `null` value.
     */
    fun delete(db: UserData) {
        setValue(db, value = null)
    }
}
