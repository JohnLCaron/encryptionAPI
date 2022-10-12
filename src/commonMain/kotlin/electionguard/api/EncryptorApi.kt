package electionguard.api

import electionguard.ballot.ManifestSimple
import electionguard.ballot.PlaintextBallot
import electionguard.core.Base16.fromHex
import electionguard.core.ElGamalPublicKey
import electionguard.core.UInt256
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.safeBase16ToElementModP
import electionguard.core.safeBase16ToElementModQ
import electionguard.encrypt.EncryptorSimple
import electionguard.serialize.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val context = productionGroup()
private var encryptor: EncryptorSimple? = null
private const val debug = true
fun setElectionApi(
    manifestJsonString: String,
    publicKeyBase16String: String,
    qbarBase16String: String
): Boolean {

    if (debug) println("manifestJsonString = $manifestJsonString")
    val manifest = Json.decodeFromString<ManifestSimple>(manifestJsonString)

    if (debug) println("publicKeyBase16String = $publicKeyBase16String")
    val publicKey = context.safeBase16ToElementModP(publicKeyBase16String)

    if (debug) println("qbarBase16String = $qbarBase16String")
    val qbar = UInt256(qbarBase16String.fromHex()!!)

    encryptor = EncryptorSimple(context, manifest, ElGamalPublicKey(publicKey), qbar)
    return true
}

fun encryptApi(plaintextJsonString: String, codeSeedBase16String: String): String? {
    if (encryptor == null) {
        return null
    }

    if (debug) println("plaintextJsonString = $plaintextJsonString")
    val ballot = Json.decodeFromString<PlaintextBallot>(plaintextJsonString)

    if (debug) println("codeSeedBase16String = $codeSeedBase16String")
    val codeSeed = context.safeBase16ToElementModQ(codeSeedBase16String)

    val master = context.randomElementModQ()
    if (debug) println("call encryptBallot $master")
    val result = encryptor!!.encrypt(ballot, codeSeed, master, null)
    if (debug) println(" encryptBallot returns = $result")
    val ret = Json.encodeToString(result.publish())
    if (debug) println("return value = $ret")
    return ret
}