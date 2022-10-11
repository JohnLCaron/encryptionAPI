package electionguard.encrypt

import electionguard.core.elGamalKeypairs
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.core.uint256s
import electionguard.input.RandomBallotProvider
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptTest {
    val nballots = 10

    @Test
    fun testEncryption() {
        runTest {
            val group = productionGroup()
            val manifest = generateManifestSimple(2, 5)
            val provider = RandomBallotProvider(manifest, nballots).ballots()
            var iter = 0
            checkAll(nballots, elGamalKeypairs(group), uint256s()) { keyPair, qbar ->
                val ballot = provider[iter]

                val encryptor = EncryptorSimple(
                    group,
                    manifest,
                    keyPair.publicKey,
                    qbar
                )
                val result = encryptor.encrypt(ballot, group.TWO_MOD_Q, group.TWO_MOD_Q)

                var first = true
                iter++
                println("$iter result = ${result.cryptoHash} nonce ${result.ballotNonce()}")
                for (contest in result.contests) {
                    // println(" contest ${contest.contestId} = ${contest.cryptoHash} nonce ${contest.contestNonce}")
                    for (selection in contest.selections) {
                        // println("  selection ${selection.selectionId} = ${selection.cryptoHash} nonce ${selection.selectionNonce}")
                        if (first) println("\n*****first ${selection}\n")
                        first = false
                    }
                }
            }
        }
    }

    @Test
    fun testEncryptionWithMasterNonce() {
        runTest {
            val group = productionGroup()
            val manifest = generateManifestSimple(3, 3)
            val provider = RandomBallotProvider(manifest, nballots).ballots()
            var iter = 0

            checkAll(nballots, elGamalKeypairs(group), uint256s()) { keyPair, qbar ->
                val ballot = provider[iter]

                val encryptor = EncryptorSimple(
                    group,
                    manifest,
                    keyPair.publicKey,
                    qbar
                )
                val nonce1 = group.randomElementModQ(minimum = 2)
                val nonce2 = group.randomElementModQ(minimum = 3)
                val result1 = encryptor.encrypt(ballot, nonce1, nonce2, 0)
                val result2 = encryptor.encrypt(ballot, nonce1, nonce2, 0)

                result1.contests.forEachIndexed { index, contest1 ->
                    val contest2 = result2.contests[index]
                    contest1.selections.forEachIndexed { sindex, selection1 ->
                        val selection2 = contest2.selections[sindex]
                        assertEquals(selection1, selection2)
                    }
                    assertEquals(contest1, contest2)
                }
                assertEquals(result1, result2)
                iter++
            }
        }
    }
}