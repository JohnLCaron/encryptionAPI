@file:OptIn(ExperimentalCli::class)

package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.encrypt
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.decrypt.DecryptingTrustee
import electionguard.decrypt.computeLagrangeCoefficient
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ExperimentalCli
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test KeyCeremony Trustee generation and recovered decryption. */

class RecoveredDecryptionTest {
    @Test
    fun runFakeKeyCeremonyTrusteeTest() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/RecoveredDecryptionTest"
        val trusteeDir = "testOut/RecoveredDecryptionTest/private_data"

        runRecoveredDecryptionTest(group, configDir, outputDir, trusteeDir)
    }
}

private val writeout = false
private val nguardians = 4
private val quorum = 3

fun runRecoveredDecryptionTest(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
) {
    // class KeyCeremonyTrustee(
    //    val group: GroupContext,
    //    val id: String,
    //    val xCoordinate: UInt,
    //    val quorum: Int,
    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq.toUInt(), quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.sharePublicKeys())
        }
    }

    // exchange SecretKeyShares
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id))
        }
    }

    val dTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }

    val jointPublicKey: ElementModP =
        dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    testEncryptRecoveredDecrypt(group, ElGamalPublicKey(jointPublicKey), group.TWO_MOD_Q, dTrustees, listOf(2U,3U, 4U))

    //////////////////////////////////////////////////////////
    if (writeout) {
        val commitments: MutableList<ElementModP> = mutableListOf()
        trustees.forEach { commitments.addAll(it.polynomial.coefficientCommitments) }
        val commitmentsHash = hashElements(commitments)

        val electionRecordIn = ElectionRecord(configDir, group)
        val config: ElectionConfig = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

        val primes = config.constants
        val crypto_base_hash: UInt256 = hashElements(
            primes.largePrime.toHex(), // LOOK is this the same as converting to ElementMod ??
            primes.smallPrime.toHex(),
            primes.generator.toHex(),
            config.numberOfGuardians,
            config.quorum,
            config.manifest.cryptoHash,
        )

        val cryptoExtendedBaseHash: UInt256 = hashElements(crypto_base_hash, commitmentsHash)
        val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
        val init = ElectionInitialized(
            config,
            jointPublicKey,
            config.manifest.cryptoHash,
            cryptoExtendedBaseHash,
            guardians,
        )
        val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
        publisher.writeElectionInitialized(init)

        val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
        trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }
    }
}

// private
fun makeDecryptingTrustee(ktrustee: KeyCeremonyTrustee): DecryptingTrustee {
    return DecryptingTrustee(
        ktrustee.id,
        ktrustee.xCoordinate,
        ElGamalKeypair(
            ElGamalSecretKey(ktrustee.polynomial.coefficients[0]),
            ElGamalPublicKey(ktrustee.polynomial.coefficientCommitments[0])
        ),
        ktrustee.guardianSecretKeyShares,
        ktrustee.guardianPublicKeys.values.associate { it.guardianId to it.coefficientCommitments },
    )
}

fun testEncryptRecoveredDecrypt(group: GroupContext, publicKey: ElGamalPublicKey, extendedBaseHash: ElementModQ,
                                trustees: List<DecryptingTrustee>, present: List<UInt>) {
    println("present $present")
    val vote = 42
    val evote = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

    val available = trustees.filter {present.contains(it.xCoordinate())}
    val coordsPresent = available.map {it.xCoordinate}
    // once the set of available guardians is deterrmined, the lagrangeCoefficients can be calculated for all decryptions
    val lagrangeCoefficients = available.associate { it.id to group.computeLagrangeCoefficient(it.xCoordinate, coordsPresent) }

    var countDirect = 0
    var countRecovered = 0
    val shares: List<ElementModP> = trustees.map {
        if (available.contains(it)) {
            countDirect++
            evote.pad powP it.electionKeypair.secretKey.key
        } else {
            countRecovered++
            evote.recoverPartialShare(group, it.id, available, extendedBaseHash, lagrangeCoefficients)
        }
    }

    val allSharesProductM: ElementModP = with(group) { shares.multP() }
    val decryptedValue: ElementModP = evote.data / allSharesProductM
    val dlogM: Int = publicKey.dLog(decryptedValue, 100) ?: throw RuntimeException("dlog failed")
    assertEquals(42, dlogM)
    println("The answer is $dlogM direct $countDirect recovered $countRecovered")
}

fun ElGamalCiphertext.recoverPartialShare(
    group: GroupContext,
    missing: String,
    available: List<DecryptingTrustee>,
    extendedBaseHash: ElementModQ,
    lagrange: Map<String, ElementModQ>,
): ElementModP {

    val shares = available.map {
        // M_il
       val partial = it.compensatedDecrypt(group, missing, listOf(this), extendedBaseHash, null)[0]
       val coeff = lagrange[it.id]?: group.ONE_MOD_Q // LOOK??
       // M_il ^ w_l
       partial.partialDecryption powP coeff
    }
    // M_i = Product(M_il ^ w_l) mod p
    return with(group) { shares.multP() }
}

fun GroupContext.computeLagrangeCoefficientOld(coordinate: UInt, present: List<UInt>): ElementModQ {
    val others: List<UInt> = present.filter { !it.equals(coordinate) }
    val numerator: Int = others.reduce { a, b -> a * b }.toInt()

    val diff: List<Int> = others.map { degree -> degree.toInt() - coordinate.toInt() }
    val denominator = diff.reduce { a, b -> a * b }

    val denomQ =
        if (denominator > 0) denominator.toElementModQ(this) else (-denominator).toElementModQ(this).unaryMinus()

    return numerator.toElementModQ(this) / denomQ
}
