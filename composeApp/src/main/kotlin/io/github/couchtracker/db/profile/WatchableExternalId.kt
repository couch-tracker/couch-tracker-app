package io.github.couchtracker.db.profile

import io.github.couchtracker.db.profile.episode.ExternalEpisodeId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.movie.ExternalMovieId

/**
 * Sealed interface that can hold an [ExternalId] that can be watched.
 */
sealed interface WatchableExternalId {

    fun serialize(): String

    data class Movie(val movieId: ExternalMovieId) : WatchableExternalId {
        override fun serialize() = "${TYPE.name.lowercase()}:${movieId.serialize()}"

        companion object {
            val TYPE = WatchedItemType.MOVIE
        }
    }

    data class Episode(val episodeId: ExternalEpisodeId) : WatchableExternalId {
        override fun serialize() = "${TYPE.name.lowercase()}:${episodeId.serialize()}"

        companion object {
            val TYPE = WatchedItemType.EPISODE
        }
    }

    companion object {
        fun parse(serializedValue: String): WatchableExternalId {
            val split = serializedValue.split(':', limit = 2)
            require(split.size >= 2) { "Invalid serialized external watchable external: $serializedValue" }

            val (typeString, externalIdString) = split

            val type = WatchedItemType.entries.find { it.name.equals(typeString, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid watchable external ID type $typeString")

            return when (type) {
                WatchedItemType.MOVIE -> Movie(ExternalMovieId.parse(externalIdString))
                WatchedItemType.EPISODE -> Episode(ExternalEpisodeId.parse(externalIdString))
            }
        }
    }
}

fun ExternalMovieId.asWatchable() = WatchableExternalId.Movie(this)
fun ExternalEpisodeId.asWatchable() = WatchableExternalId.Episode(this)
