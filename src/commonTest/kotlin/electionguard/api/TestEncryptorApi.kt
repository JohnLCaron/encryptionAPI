package electionguard.api

import electionguard.core.elGamalKeypairs
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.core.uint256s
import electionguard.encrypt.VerifyEmbeddedNonces
import electionguard.encrypt.compareBallots
import electionguard.encrypt.generateManifestSimple
import electionguard.input.RandomBallotProvider
import electionguard.serialize.CiphertextBallotSer
import electionguard.serialize.import
import io.kotest.property.checkAll
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertNotNull

class TestEncryptorApi {
    val nballots = 3

    @Test
    fun testEncryptApi() {
        runTest {
            val group = productionGroup()
            val manifest = generateManifestSimple(2, 3)
            var nselections = 0
            manifest.contests.forEach{ nselections += it.selections.size + 1}
            println("$nselections = ${Json.encodeToString(manifest)}\n")

            var iter = 0
            checkAll(3, elGamalKeypairs(group), uint256s()) { keyPair, qbar ->

                setElectionApi(Json.encodeToString(manifest),
                    keyPair.publicKey.cryptoHashString(),
                    qbar.cryptoHashString(),
                )

                val starting = getSystemTimeInMillis()
                RandomBallotProvider(manifest, nballots).ballots().forEach { ballot ->
                    if (iter == 0) {
                        println("${Json.encodeToString(ballot)}\n")
                    }

                    val codeSeed = group.randomElementModQ(minimum = 2)

                    val cballotString = encryptApi(Json.encodeToString(ballot), codeSeed.cryptoHashString())
                    assertNotNull(cballotString)
                    val cballotSer = Json.decodeFromString<CiphertextBallotSer>(cballotString)
                    assertNotNull(cballotSer)
                    val ciphertextBallot = group.import(cballotSer)

                    // decrypt with nonces
                    val decryptionWithNonce = VerifyEmbeddedNonces(group, manifest, keyPair.publicKey)
                    val decryptedBallot = with(decryptionWithNonce) { ciphertextBallot.decrypt() }
                    assertNotNull(decryptedBallot)

                    compareBallots(ballot, decryptedBallot)
                }

                val took = getSystemTimeInMillis() - starting
                val msecsPerBallot = (took.toDouble() / nballots).roundToInt()
                val msecsPerSelection = took.toDouble() / (nballots * nselections)
                iter++
                println("$iter testEncryptApi took $took msecs,  $msecsPerBallot msecs/ballot, $msecsPerSelection msecs/selection")
            }
        }
    }
}