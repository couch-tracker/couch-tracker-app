package io.github.couchtracker.db.user

interface ExternalId {

    val provider: String
    val value: String

    fun serialize(): String {
        return "$provider-$value"
    }

    interface InheritorsCompanion<out EID : ExternalId> {

        val provider: String

        fun ofValue(value: String): EID
    }

    abstract class SealedInterfacesCompanion<out EID : ExternalId>(
        private val inheritors: List<InheritorsCompanion<EID>>,
        private val unknownProvider: (provider: String, value: String) -> EID,
    ) {
        fun parse(serializedValue: String): EID {
            return Companion.parse(serializedValue, decoders = inheritors, unknownProvider = unknownProvider)
        }
    }

    companion object {

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
