package io.github.couchtracker.tmdb

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/**
 * A language, compatible with TMDB APIs.
 * Specification is on https://developer.themoviedb.org/docs/languages
 *
 * @property language ISO 639-1
 * @property country ISO 3166-1
 */
@Serializable(with = TmdbLanguage.Serializer::class)
data class TmdbLanguage(
    val language: String,
    val country: String?,
) : Parcelable {

    object Serializer : KSerializer<TmdbLanguage> {
        override val descriptor = PrimitiveSerialDescriptor("TmdbLanguage", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): TmdbLanguage {
            return TmdbLanguage.parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: TmdbLanguage) {
            encoder.encodeString(value.serialize())
        }

    }

    init {
        require(language.length == 2) { "Invalid language '$language'" }
        require(language.all { it in 'a'..'z' }) { "Invalid language '$language'" }
        if (country != null) {
            require(country.length == 2) { "Invalid country '$country'" }
            require(country.all { it in 'A'..'Z' }) { "Invalid country '$country'" }
        }
    }

    val apiParameter: String
        get() = if (country != null) {
            "$language-$country"
        } else {
            language
        }

    fun serialize() = apiParameter

    override fun toString() = serialize()

    companion object {
        val ENGLISH = TmdbLanguage("en", null)

        val COLUMN_ADAPTER = object : ColumnAdapter<TmdbLanguage, String> {
            override fun decode(databaseValue: String) = parse(databaseValue)
            override fun encode(value: TmdbLanguage) = value.serialize()
        }

        /**
         * Parses a TmdbLanguage. Supported formats are either `language-COUNTRY` or `language`, where
         * `language` uses ISO 639-1, and `country` uses ISO 3166-1.
         */
        @Suppress("MagicNumber")
        fun parse(serializedValue: String): TmdbLanguage {
            val tokens = serializedValue.split('-', limit = 2)
            return when (tokens.size) {
                1 -> TmdbLanguage(serializedValue, null)
                2 -> TmdbLanguage(tokens[0], tokens[1])
                else -> throw IllegalArgumentException("Invalid tmdb language '$serializedValue'")
            }
        }
    }
}

inline fun <reified T : Parcelable> parcelableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = json.encodeToString(value)

    override fun put(bundle: Bundle, key: String, value: T) = bundle.putParcelable(key, value)
}
