package electionguard.serialize

import electionguard.core.*
import electionguard.encrypt.CiphertextBallot
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.random.Random
import kotlin.random.nextUInt

// A serializable version of CiphertextBallot
@Serializable
data class CiphertextBallotSer(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val codeSeed: UInt256,
    val code: UInt256,
    val contests: List<ContestSer>,
    val timestamp: Long,
    val cryptoHash: UInt256,
    val masterNonce: ElementModQPub,
)

/** Publishes an CiphertextBallot to its external, serializable form. */
fun CiphertextBallot.publish() = CiphertextBallotSer(
    this.ballotId,
    this.ballotStyleId,
    this.manifestHash,
    this.codeSeed,
    this.code,
    this.contests.map {it.publish()},
    this.timestamp,
    this.cryptoHash,
    this.masterNonce.publish(),
)

/** Publishes a Contest to a JSON AST representation. */
fun CiphertextBallot.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a ContestPub. Returns `null` if it's out of bounds. */
fun GroupContext.import(pub: CiphertextBallotSer) = CiphertextBallot(
    pub.ballotId,
    pub.ballotStyleId,
    pub.manifestHash,
    pub.codeSeed,
    pub.code,
    pub.contests.map {this.import(it)},
    pub.timestamp,
    pub.cryptoHash,
    this.import(pub.masterNonce)!!,
)

/** Imports from a published Contest. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importCiphertextBallot(element: JsonElement): CiphertextBallot? =
    try {
        this.import(Json.decodeFromJsonElement<CiphertextBallotSer>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }

@Serializable
data class ContestSer(
    val contestId: String, // matches ContestDescription.contestIdd
    val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
    val contestHash: UInt256, // matches ContestDescription.cryptoHash
    val selections: List<SelectionSer>,
    val cryptoHash: UInt256,
    val proof: ConstantChaumPedersenProofPub,
    val contestNonce: ElementModQPub,
)

/** Publishes an CiphertextBallot.Contest to its external, serializable form. */
fun CiphertextBallot.Contest.publish() = ContestSer(
    this.contestId,
    this.sequenceOrder,
    this.contestHash,
    this.selections.map {it.publish()},
    this.cryptoHash,
    this.proof.publish(),
    this.contestNonce.publish(),
)

/** Publishes a Contest to a JSON AST representation. */
fun CiphertextBallot.Contest.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a ContestPub. Returns `null` if it's out of bounds. */
fun GroupContext.import(pub: ContestSer) = CiphertextBallot.Contest(
    pub.contestId,
    pub.sequenceOrder,
    pub.contestHash,
    pub.selections.map {this.import(it)},
    pub.cryptoHash,
    this.import(pub.proof)!!,
    this.import(pub.contestNonce)!!,
)

/** Imports from a published Contest. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importCiphertextBallotContest(element: JsonElement): CiphertextBallot.Contest? =
    try {
        this.import(Json.decodeFromJsonElement<ContestSer>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }

@Serializable
data class SelectionSer(
    val selectionId: String, // matches SelectionDescription.selectionId
    val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
    val selectionHash: UInt256, // matches SelectionDescription.cryptoHash
    val ciphertext: ElGamalCiphertextPub,
    val cryptoHash: UInt256,
    val isPlaceholderSelection: Boolean,
    val proof: DisjunctiveChaumPedersenPub,
    // val extendedData: HashedElGamalCiphertext?,
    val selectionNonce: ElementModQPub,
)

/** Publishes an CiphertextBallot.Selection to its external, serializable form. */
fun CiphertextBallot.Selection.publish() = SelectionSer(
    this.selectionId,
    this.sequenceOrder,
    this.selectionHash,
    this.ciphertext.publish(),
    this.cryptoHash,
    this.isPlaceholderSelection,
    this.proof.publish(),
    this.selectionNonce.publish(),
)

/** Publishes an Selection to a JSON AST representation. */
fun CiphertextBallot.Selection.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a SelectionPub. Returns `null` if it's out of bounds. */
fun GroupContext.import(pub: SelectionSer) = CiphertextBallot.Selection(
    pub.selectionId,
    pub.sequenceOrder,
    pub.selectionHash,
    this.import(pub.ciphertext)!!,
    pub.cryptoHash,
    pub.isPlaceholderSelection,
    this.import(pub.proof)!!,
    null,
    this.import(pub.selectionNonce)!!,
    )

/** Imports from a published Selection. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importCiphertextBallotSelection(element: JsonElement): CiphertextBallot.Selection? =
    try {
        this.import(Json.decodeFromJsonElement<SelectionSer>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }

//// temp

fun generateDisjunctiveChaumPedersenProofKnownNonce(
    context: GroupContext
): DisjunctiveChaumPedersenProofKnownNonce {
    return DisjunctiveChaumPedersenProofKnownNonce(
        generateGenericChaumPedersenProof(context),
        generateGenericChaumPedersenProof(context),
        generateElementModQ(context),
    )
}

fun generateGenericChaumPedersenProof(context: GroupContext): GenericChaumPedersenProof {
    return GenericChaumPedersenProof(generateElementModQ(context), generateElementModQ(context),)
}

fun generateSchnorrProof(context: GroupContext): SchnorrProof {
    return SchnorrProof(
        generateElementModQ(context),
        generateElementModQ(context),
    )
}

fun generateCiphertext(context: GroupContext): ElGamalCiphertext {
    return ElGamalCiphertext(generateElementModP(context), generateElementModP(context))
}

fun generateHashedCiphertext(context: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(generateElementModP(context), "what".encodeToByteArray(), generateUInt256(context), 42)
}

fun generateElementModQ(context: GroupContext): ElementModQ {
    return context.uIntToElementModQ(Random.nextUInt(134217689.toUInt()))
}

fun generateUInt256(context: GroupContext): UInt256 {
    return generateElementModQ(context).toUInt256();
}

fun generateElementModP(context: GroupContext): ElementModP {
    return context.uIntToElementModP(Random.nextUInt(1879047647.toUInt()))
}