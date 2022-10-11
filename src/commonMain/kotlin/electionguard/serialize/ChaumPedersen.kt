package electionguard.serialize

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** External representation of an GenericChaumPedersenProof */
@Serializable
@SerialName("GenericChaumPedersen")
data class GenericChaumPedersenProofPub(val c: ElementModQPub, val r: ElementModQPub)

/** Publishes a [GenericChaumPedersenProof] to its external, serializable form. */
fun GenericChaumPedersenProof.publish() = GenericChaumPedersenProofPub(
    this.c.publish(),
    this.r.publish()
)

/** Publishes an GenericChaumPedersenProof to a JSON AST representation. */
fun GenericChaumPedersenProof.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a published [DisjunctiveChaumPedersen]. Returns `null` if it's malformed. */
fun GroupContext.import(proof: GenericChaumPedersenProofPub): GenericChaumPedersenProof? {
    val c = import(proof.c)
    val r = import(proof.r)
    if (c == null || r == null) return null
    return GenericChaumPedersenProof(c, r)
}

/** Imports from a published UInt256. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importUInt256(element: JsonElement): GenericChaumPedersenProof? =
    try {
        this.import(Json.decodeFromJsonElement<GenericChaumPedersenProofPub>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }

/////////////////////////

/** External representation of an DisjunctiveChaumPedersenProofKnownNonce */
@Serializable
@SerialName("DisjunctiveChaumPedersen")
data class DisjunctiveChaumPedersenPub(
    val proof0: GenericChaumPedersenProofPub,
    val proof1: GenericChaumPedersenProofPub,
    val c: ElementModQPub,
)

/** Publishes a [DisjunctiveChaumPedersenProofKnownNonce] to its external, serializable form. */
fun DisjunctiveChaumPedersenProofKnownNonce.publish() = DisjunctiveChaumPedersenPub(
    this.proof0.publish(),
    this.proof1.publish(),
    this.c.publish(),
)

/** Publishes an DisjunctiveChaumPedersenProofKnownNonce to a JSON AST representation. */
fun DisjunctiveChaumPedersenProofKnownNonce.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a published [DisjunctiveChaumPedersen]. Returns `null` if it's malformed. */
fun GroupContext.import(proof: DisjunctiveChaumPedersenPub): DisjunctiveChaumPedersenProofKnownNonce? {
    val proof0 = import(proof.proof0)
    val proof1 = import(proof.proof1)
    val c = import(proof.c)
    if (c == null || proof0 == null || proof1 == null) return null
    return DisjunctiveChaumPedersenProofKnownNonce(proof0, proof1, c)
}

/** Imports from a published DisjunctiveChaumPedersen. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importDisjunctiveChaumPedersen(element: JsonElement): DisjunctiveChaumPedersenProofKnownNonce? =
    try {
        this.import(Json.decodeFromJsonElement<DisjunctiveChaumPedersenPub>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }

/////////////////////////

/** External representation of an ConstantChaumPedersenProofKnownNonce */
@Serializable
@SerialName("ConstantChaumPedersenProof")
data class ConstantChaumPedersenProofPub(
    val proof: GenericChaumPedersenProofPub,
    val constant: Int,
)

/** Publishes a [ConstantChaumPedersenProofKnownNonce] to its external, serializable form. */
fun ConstantChaumPedersenProofKnownNonce.publish() = ConstantChaumPedersenProofPub(
    this.proof.publish(),
    this.constant,
)

/** Publishes an ConstantChaumPedersenProofKnownNonce to a JSON AST representation. */
fun ConstantChaumPedersenProofKnownNonce.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a published [ConstantChaumPedersenProof]. Returns `null` if it's malformed. */
fun GroupContext.import(proofPub: ConstantChaumPedersenProofPub): ConstantChaumPedersenProofKnownNonce? {
    val proof = import(proofPub.proof)
    if (proof == null) return null
    return ConstantChaumPedersenProofKnownNonce(proof, proofPub.constant)
}

/** Imports from a published ConstantChaumPedersenProof. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importConstantChaumPedersenProof(element: JsonElement): ConstantChaumPedersenProofKnownNonce? =
    try {
        this.import(Json.decodeFromJsonElement<ConstantChaumPedersenProofPub>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }