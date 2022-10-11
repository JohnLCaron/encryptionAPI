@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.serialize

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.UInt256
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** External representation of an UInt256. */
@Serializable(with = UInt256AsStringSerializer::class)
@SerialName("UInt256Pub")
data class UInt256Pub(val value: ByteArray)

// Custom serializers for UInt256 that know how to convert from ByteArray (our "internal" format)
// to a JSON object which is just a base16 string representation.

/** Custom serializer for [UInt256Pub]. */
object UInt256AsStringSerializer : KSerializer<UInt256Pub> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UInt256", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UInt256Pub) {
        val string = value.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): UInt256Pub {
        val string = decoder.decodeString()
        return UInt256Pub(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

/** Publishes an UInt256 to its external, serializable form. */
fun UInt256.publish(): UInt256Pub = UInt256Pub(this.bytes)

/** Publishes an UInt256 to a JSON AST representation. */
fun UInt256.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a published UInt256. Returns `null` if it's out of bounds. */
fun UInt256Pub.import(): UInt256 = UInt256(this.value)

/** Imports from a published UInt256. Returns `null` if it's out of bounds or malformed.. */
fun importUInt256Pub(element: JsonElement): UInt256? =
    try {
        Json.decodeFromJsonElement<UInt256Pub>(element).import()
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }
