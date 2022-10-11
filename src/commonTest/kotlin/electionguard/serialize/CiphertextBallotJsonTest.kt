package electionguard.serialize

import electionguard.core.*
import electionguard.encrypt.CiphertextBallot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CiphertextBallotJsonTest {

    @Test
    fun roundtripSelection() {
        val context = productionGroup()
        val selection = generateFakeSelection(42, context)

        // short round-trip from the core classes to Pub and back
        val urt = context.import(selection.publish())
        assertNotNull(urt)
        assertEquals(selection, urt)

        // longer round-trip through serialized JSON strings and back
        assertEquals(selection, context.importCiphertextBallotSelection(selection.publishJson()))
    }

    @Test
    fun roundtripContest() {
        val context = productionGroup()
        val contest = generateFakeContest(42, context)

        // short round-trip from the core classes to Pub and back
        val urt = context.import(contest.publish())
        assertNotNull(urt)
        assertEquals(contest, urt)

        // longer round-trip through serialized JSON strings and back
        assertEquals(contest, context.importCiphertextBallotContest(contest.publishJson()))
    }

    @Test
    fun roundtripBallot() {
        val context = productionGroup()
        val eballot = generateCiphertextBallot(42, context)

        // short round-trip from the core classes to Pub and back
        val urt = context.import(eballot.publish())
        assertNotNull(urt)
        assertEquals(eballot, urt)

        // longer round-trip through serialized JSON strings and back
        assertEquals(context.importCiphertextBallot(eballot.publishJson()), eballot)

        val json = eballot.publishJson()
        val cballotString = json.toString()
        val cballotSer = Json.decodeFromString<CiphertextBallotSer>(cballotString)
        assertNotNull(cballotSer)
        val cballot = context.import(cballotSer)
        assertEquals(cballot, eballot)
    }

    fun generateCiphertextBallot(seq: Int, context: GroupContext): CiphertextBallot {
        val contests = List(9, { generateFakeContest(it, context) })
        return CiphertextBallot(
            "ballotId $seq",
            "ballotIdStyle",
            generateUInt256(context),
            generateUInt256(context),
            generateUInt256(context),
            contests,
            42,
            generateUInt256(context),
            generateElementModQ(context),
        )
    }

    private fun generateFakeContest(cseq: Int, context: GroupContext): CiphertextBallot.Contest {
        val selections = List(11, { generateFakeSelection(it, context) })
        return CiphertextBallot.Contest(
            "contest" + cseq,
            cseq,
            generateUInt256(context),
            selections,
            generateUInt256(context),
            generateConstantChaumPedersenProofKnownNonce(context),
            generateElementModQ(context),
        )
    }

    private fun generateFakeSelection(
        sseq: Int,
        context: GroupContext
    ): CiphertextBallot.Selection {
        return CiphertextBallot.Selection(
            "selection" + sseq,
            sseq,
            generateUInt256(context),
            generateCiphertext(context),
            generateUInt256(context),
            false,
            generateDisjunctiveChaumPedersenProofKnownNonce(context),
            null, // generateHashedCiphertext(context),
            generateElementModQ(context),
        )
    }


    private fun generateConstantChaumPedersenProofKnownNonce(
        context: GroupContext
    ): ConstantChaumPedersenProofKnownNonce {
        return ConstantChaumPedersenProofKnownNonce(
            generateGenericChaumPedersenProof(context),
            Random.nextInt(),
        )
    }
}