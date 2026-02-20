package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.db.profile.externalids.ExternalId.SealedInterfacesCompanion

/**
 * Base interface for all IDs of external providers that are inside the profile's database.
 *
 * It is composed of two main components: [provider] and [value].
 *
 * The sealed interfaces extending [ExternalId] should always have an "unknown" data class inheritor that can be used as a fallback in case
 * the [provider] is not known, either because the app is outdated or because the user/a different app added to the DB unsupported IDs.
 *
 * All inheritors of this interface must be immutable.
 *
 * @property provider the name of the external provider (e.g. `tmdb`).
 * @property value the external ID. It can be anything that uniquely identifies the external resource.
 */
sealed interface ExternalId {

    val provider: String
    val value: String

    /**
     * Serializes this ID to a [String] in a format that also includes its type (i.e. `<type>-<provider>-<value>` (e.g. `movie-tmdb-1234`)).
     *
     * If the type is unnecessary, see [SealedInterfacesCompanion.serialize].
     */
    fun serialize(): String {
        return "${type().companion.typeName}-$provider-$value"
    }

    /**
     * Interface that should be implemented by the companion object of all "leaf" inheritors of [ExternalId].
     */
    interface InheritorsCompanion<out EID : ExternalId> {

        /**
         * The name of the provider represented by [EID].
         */
        val provider: String

        /**
         * Parses the given [value] into an [EID] instance.
         *
         * @throws IllegalArgumentException if the provided value is not valid.
         */
        fun ofValue(value: String): EID
    }

    /**
     * Abstract class that should be extended by the companion object of sealed interfaces extending [ExternalId].
     *
     * For more info see [ExternalId] documentation.
     *
     * @property inheritors the list of known inheritors of [EID]. It must be explicitly provided in order to avoid taking a dependency on
     * kotlin-reflect.
     * @property unknownProvider given a provider and type, must return a fallback [EID] instance that can hold arbitrary values.
     */
    abstract class SealedInterfacesCompanion<EID : ExternalId>(
        val typeName: String,
        private val inheritors: List<InheritorsCompanion<EID>>,
        private val unknownProvider: (provider: String, value: String) -> EID,
    ) {

        /**
         * Serializes this ID to a [String] in the format `<provider>-<value>` (e.g. `tmdb-1234`).
         *
         * If the type is also necessary, see [ExternalId.serialize]
         */
        fun serialize(value: EID): String {
            return "${value.provider}-${value.value}"
        }

        /**
         * Parses the given [value] (output of [serialize]) into the appropriate [EID] instance.
         *
         * In case the given [value] is of an unknown provider, this method will NOT throw and just provide a fallback [EID] that
         * holds arbitrary values. Check [EID]'s inheritors.
         *
         * @throws IllegalArgumentException if the provided value is not valid.
         */
        fun parse(value: String): EID {
            return Companion.parse(value, decoders = inheritors, unknownProvider = unknownProvider)
        }
    }

    companion object {

        /**
         * Parses the given [value] (output of [ExternalId.serialize]) into the given [EID] instance.
         *
         * @throws IllegalArgumentException if the [value] is invalid or if it is a valid [ExternalId] but is not a valid [EID].
         */
        inline fun <reified EID : ExternalId> parse(value: String): EID {
            val (typeStr, externalId) = value.split("-", limit = 2).also {
                require(it.size == 2) { "Invalid serialized TypedExternalId" }
            }
            val type = ExternalIdType.entries.singleOrNull { it.companion.typeName == typeStr }
                ?: throw IllegalArgumentException("Invalid external ID type '$typeStr'")
            when (val id = type.companion.parse(externalId)) {
                is EID -> return id
                else -> throw IllegalArgumentException("External ID type ${id.type()} is not suitable for ${EID::class}")
            }
        }

        private fun <EID : ExternalId> parse(
            serializedValue: String,
            decoders: List<InheritorsCompanion<EID>>,
            unknownProvider: (type: String, value: String) -> EID,
        ): EID {
            val split = serializedValue.split('-', limit = 2)
            require(split.size >= 2) { "Invalid serialized external ID: $serializedValue" }
            val (type, value) = split

            val decoder = decoders.find { it.provider == type }
            if (decoder == null) {
                return unknownProvider(type, value)
            }
            return decoder.ofValue(value)
        }
    }
}

@PublishedApi
internal enum class ExternalIdType(val companion: SealedInterfacesCompanion<*>) {
    MOVIE(ExternalMovieId),
    SHOW(ExternalShowId),
    SEASON(ExternalSeasonId),
    EPISODE(ExternalEpisodeId),
}

@PublishedApi
internal fun ExternalId.type() = when (this) {
    is ExternalMovieId -> ExternalIdType.MOVIE
    is ExternalShowId -> ExternalIdType.SHOW
    is ExternalSeasonId -> ExternalIdType.SEASON
    is ExternalEpisodeId -> ExternalIdType.EPISODE
}
