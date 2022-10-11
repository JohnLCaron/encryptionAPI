package electionguard.serialize

import electionguard.ballot.PlaintextBallot
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaintextBallotJsonTest {

    @Test
    fun roundtripPlaintextBallot() {
        // val group = productionGroup()
        val ballot = generateFakeBallot()
        val s = Json.encodeToString(ballot)
        println(s)
        val rt = Json.decodeFromString<PlaintextBallot>(s)
        println(rt)
        assertEquals(rt, ballot)
    }

    private fun generateFakeBallot(): PlaintextBallot {
        val contests = List(11) { generateFakeContest(it) }
        return PlaintextBallot("ballotId", "ballotIdStyle", contests)
    }

    private fun generateFakeContest(cseq: Int): PlaintextBallot.Contest {
        val selections = List(11) { generateFakeSelection(it) }
        return PlaintextBallot.Contest("contest$cseq", cseq, selections)
    }

    private fun generateFakeSelection(sseq: Int): PlaintextBallot.Selection {
        val vote: Int = if (Random.nextBoolean()) 1 else 0
        return PlaintextBallot.Selection(
            "selection$sseq",
            sseq,
            vote,
            "ExtendedData$sseq",
        )
    }

}