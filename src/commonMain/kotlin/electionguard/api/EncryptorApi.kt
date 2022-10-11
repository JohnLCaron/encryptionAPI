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
private var encryptor : EncryptorSimple? = null

fun setElectionApi(manifestJsonString : String,
                publicKeyBase16String: String,
                qbarBase16String: String) : Boolean {
    val manifest = Json.decodeFromString<ManifestSimple>(manifestJsonString)

    val publicKey = context.safeBase16ToElementModP(publicKeyBase16String)
    val qbar = UInt256(qbarBase16String.fromHex()!!)

    encryptor = EncryptorSimple(context, manifest, ElGamalPublicKey(publicKey), qbar)
    return true
}

fun encryptApi(plaintextJsonString : String, codeSeedBase16String: String) : String? {
    if (encryptor == null) {
        return null
    }

    val ballot = Json.decodeFromString<PlaintextBallot>(plaintextJsonString)
    val codeSeed = context.safeBase16ToElementModQ(codeSeedBase16String)

    with (encryptor!!) {
        val result = ballot.encryptBallot(codeSeed, context.randomElementModQ())
        return Json.encodeToString(result.publish())
    }
}