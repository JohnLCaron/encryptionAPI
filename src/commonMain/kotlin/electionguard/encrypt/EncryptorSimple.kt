package electionguard.encrypt

import electionguard.ballot.ManifestSimple
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.constantChaumPedersenProofKnownNonce
import electionguard.core.disjunctiveChaumPedersenProofKnownNonce
import electionguard.core.encrypt
import electionguard.core.encryptedSum
import electionguard.core.get
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots, using ManifestSimple.
 */
class EncryptorSimple(
    val group: GroupContext,
    val manifest: ManifestSimple,
    private val elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHash: UInt256,
) {
    private val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)

    /** Encrypt ballots in a chain with starting codeSeed, and random masterNonce */
    fun encrypt(ballots: Iterable<PlaintextBallot>, codeSeed: ElementModQ): List<CiphertextBallot> {
        var previousTrackingHash = codeSeed
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            val encryptedBallot = ballot.encryptBallot(previousTrackingHash, group.randomElementModQ())
            encryptedBallots.add(encryptedBallot)
            previousTrackingHash = encryptedBallot.code.toElementModQ(group)
        }
        return encryptedBallots
    }

    /** Encrypt ballots with fixed codeSeed, masterNonce, and timestamp. */
    fun encryptWithFixedNonces(
        ballots: Iterable<PlaintextBallot>,
        codeSeed: ElementModQ,
        masterNonce: ElementModQ
    ): List<CiphertextBallot> {
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            encryptedBallots.add(ballot.encryptBallot(codeSeed, masterNonce, 0))
        }
        return encryptedBallots
    }

    /** Encrypt the ballot with the given codeSeed and master nonce and an optional timestamp override. */
    fun encrypt(
        ballot: PlaintextBallot,
        codeSeed: ElementModQ,
        masterNonce: ElementModQ,
        timestampOverride: Long? = null // if null, use getSystemTimeInMillis()
    ): CiphertextBallot {
        println("check encrypt")
        return ballot.encryptBallot(codeSeed, masterNonce, timestampOverride)
    }

    fun PlaintextBallot.encryptBallot(
        codeSeed: ElementModQ,
        masterNonce: ElementModQ, // random
        timestampOverride: Long? = null,
    ): CiphertextBallot {
        println("chek encryptBallot")
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHash, this.ballotId, masterNonce)
        val plaintextContests = this.contests.associateBy { it.contestId }
        println("chek 1")

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        for (mcontest in manifest.contests) {
            println(" contest ${mcontest.contestId}")
            // If no contest on the ballot, create a placeholder
            val pcontest: PlaintextBallot.Contest = plaintextContests[mcontest.contestId] ?: contestFrom(mcontest)
            encryptedContests.add(pcontest.encryptContest(mcontest, ballotNonce))
            println(" contest ${mcontest.contestId} done")
        }
        val sortedContests = encryptedContests.sortedBy { it.sequenceOrder }
        println("chek 2")

        // arbitrary choice about how to calculate Hi, the trackingCode (aka confirmation code), and Bi
        // may not be spec compliant
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000)
        val cryptoHash = hashElements(ballotId, manifest.cryptoHash, sortedContests) // B_i
        val trackingCode = hashElements(codeSeed, timestamp, cryptoHash)
        println("chek 3")

        return CiphertextBallot(
            ballotId,
            ballotStyleId,
            manifest.cryptoHash,
            codeSeed.toUInt256(),
            trackingCode,
            sortedContests,
            timestamp,
            cryptoHash,
            masterNonce,
        )
    }

    private fun contestFrom(mcontest: ManifestSimple.ContestSimple): PlaintextBallot.Contest {
        println(" contestFrom ${mcontest.contestId}")
        val selections = mcontest.selections.map { selectionFrom(it.selectionId, it.sequenceOrder, false) }
        return PlaintextBallot.Contest(mcontest.contestId, mcontest.sequenceOrder, selections)
    }

    /**
     * Encrypt a PlaintextBallotContest into CiphertextBallot.Contest.
     * @param mcontest:    the corresponding Manifest.ContestDescription
     * @param ballotNonce: the seed for this contest.
     */
    private fun PlaintextBallot.Contest.encryptContest(
        mcontest: ManifestSimple.ContestSimple,
        ballotNonce: UInt256,
    ): CiphertextBallot.Contest {
        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[mcontest.sequenceOrder]
        val chaumPedersenNonce = nonceSequence[0]

        val encryptedSelections = mutableListOf<CiphertextBallot.Selection>()
        val plaintextSelections = this.selections.associateBy { it.selectionId }
        println(" encrypyContest check1")

        // only use selections that match the manifest.
        var votes = 0
        for (mselection: ManifestSimple.SelectionSimple in mcontest.selections) {
            println("  select ${mselection.selectionId}")

            //Find the actual selection matching the contest description.
            val plaintextSelection = plaintextSelections[mselection.selectionId] ?:
            //No selection was made for this possible value so we explicitly set it to false
            selectionFrom(mselection.selectionId, mselection.sequenceOrder, false)

            //track the votes so we can append the appropriate number of true placeholder votes
            votes += plaintextSelection.vote
            val encryptedSelection = plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                false,
            )
            encryptedSelections.add(encryptedSelection)
            println("  select ${mselection.selectionId} done")
        }
        println(" encrypyContest check2")

        // Add a placeholder selection for each possible vote in the contest
        val limit = mcontest.votesAllowed
        val selectionSequenceOrderMax = mcontest.selections.maxOf { it.sequenceOrder }
        for (placeholder in 1..limit) {
            val sequenceNo = selectionSequenceOrderMax + placeholder
            val plaintextSelection = selectionFrom(
                "${mcontest.contestId}-$sequenceNo", sequenceNo, votes < limit
            )
            val cryptoHash = hashElements(plaintextSelection.selectionId, plaintextSelection.sequenceOrder, plaintextSelection.selectionId)
            val mselection = ManifestSimple.SelectionSimple(
                plaintextSelection.selectionId,
                plaintextSelection.sequenceOrder,
                cryptoHash,
            )
            val encryptedPlaceholder = plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                true,
            )
            encryptedSelections.add(encryptedPlaceholder)
            votes++
        }

        println(" encrypyContest check3")
        return mcontest.encryptContest(
            group,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            contestNonce,
            chaumPedersenNonce,
            encryptedSelections.sortedBy { it.sequenceOrder },
        )
    }

    private fun selectionFrom(
        selectionId: String, sequenceOrder: Int, is_affirmative: Boolean
    ): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selectionId,
            sequenceOrder,
            if (is_affirmative) 1 else 0,
            null,
        )
    }

    /**
     * Encrypt a PlaintextBallot.Selection into a CiphertextBallot.Selection
     *
     * @param selectionDescription:         the Manifest selection
     * @param contestNonce:                 aka "nonce seed"
     * @param isPlaceholder:                if this is a placeholder selection
     */
    private fun PlaintextBallot.Selection.encryptSelection(
        selectionDescription: ManifestSimple.SelectionSimple,
        contestNonce: ElementModQ,
        isPlaceholder: Boolean = false,
    ): CiphertextBallot.Selection {
        val nonceSequence = Nonces(selectionDescription.cryptoHash.toElementModQ(group), contestNonce)
        val disjunctiveChaumPedersenNonce: ElementModQ = nonceSequence[0]
        val selectionNonce: ElementModQ = nonceSequence[selectionDescription.sequenceOrder]

        // TODO: need to test
        val extendedDataCiphertext =
            if (extendedData != null) {
                val extendedDataBytes = extendedData.encodeToByteArray()
                val extendedDataNonce = Nonces(selectionNonce, "extended-data")[0]
                extendedDataBytes.hashedElGamalEncrypt(elgamalPublicKey, extendedDataNonce)
            } else null

        return selectionDescription.encryptSelection(
            this.vote,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            disjunctiveChaumPedersenNonce,
            selectionNonce,
            isPlaceholder,
            extendedDataCiphertext,
        )
    }
}

////  share with Encryptor, BallotPrecompute, ContestPrecompute
private fun ManifestSimple.ContestSimple.encryptContest(
    group: GroupContext,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    contestNonce: ElementModQ,
    chaumPedersenNonce: ElementModQ,
    encryptedSelections: List<CiphertextBallot.Selection>,
): CiphertextBallot.Contest {

    val cryptoHash = hashElements(this.contestId, this.cryptoHash, encryptedSelections)
    val texts: List<ElGamalCiphertext> = encryptedSelections.map { it.ciphertext }
    val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
    val nonces: Iterable<ElementModQ> = encryptedSelections.map { it.selectionNonce }
    val aggNonce: ElementModQ = with(group) { nonces.addQ() }

    val proof = ciphertextAccumulation.constantChaumPedersenProofKnownNonce(
        this.votesAllowed,
        aggNonce,
        elgamalPublicKey,
        chaumPedersenNonce,
        cryptoExtendedBaseHashQ,
    )

    return CiphertextBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.cryptoHash, // manifest contest cryptohash
        encryptedSelections,
        cryptoHash,      // CiphertextBallot.Contest cryptohash
        proof,
        contestNonce,
    )
}

private fun ManifestSimple.SelectionSimple.encryptSelection(
    vote: Int,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    disjunctiveChaumPedersenNonce: ElementModQ,
    selectionNonce: ElementModQ,
    isPlaceholder: Boolean = false,
    extendedDataCiphertext: HashedElGamalCiphertext?,
): CiphertextBallot.Selection {
    val elgamalEncryption: ElGamalCiphertext = vote.encrypt(elgamalPublicKey, selectionNonce)

    val proof = elgamalEncryption.disjunctiveChaumPedersenProofKnownNonce(
        vote,
        selectionNonce,
        elgamalPublicKey,
        disjunctiveChaumPedersenNonce,
        cryptoExtendedBaseHashQ
    )

    val cryptoHash = hashElements(this.selectionId, this.cryptoHash, elgamalEncryption.cryptoHashUInt256())

    return CiphertextBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.cryptoHash,
        elgamalEncryption,
        cryptoHash,
        isPlaceholder,
        proof,
        extendedDataCiphertext,
        selectionNonce,
    )
}