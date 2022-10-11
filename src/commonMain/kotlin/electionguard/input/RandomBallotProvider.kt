package electionguard.input

import electionguard.ballot.ManifestSimple
import electionguard.ballot.PlaintextBallot
import kotlin.random.Random

/** Create nballots randomly generated fake Ballots, used for testing.  */
class RandomBallotProvider(election: ManifestSimple, nballots: Int?) {
    private val nballots: Int
    private val manifest: ManifestSimple

    init {
        this.manifest = election
        this.nballots = if (nballots != null && nballots > 0) nballots else 11
    }

    fun ballots(ballotStyleId : String? = null): List<PlaintextBallot> {
        val ballotFactory = BallotFactory()
        val ballots: MutableList<PlaintextBallot> = ArrayList()
        val useStyle = ballotStyleId ?: manifest.ballotStyles[0].ballotStyleId
        for (i in 0 until nballots) {
            val ballot_id = "ballot-id-" + Random.nextInt()
            ballots.add(ballotFactory.getFakeBallot(manifest, useStyle, ballot_id))
        }
        return ballots
    }

    private class BallotFactory {
        fun getFakeBallot(manifest: ManifestSimple, ballotStyleId : String, ballotId: String): PlaintextBallot {
            val contests: MutableList<PlaintextBallot.Contest> = ArrayList()
            for (contestp in manifest.contests) {
                contests.add(getRandomContestFrom(contestp))
            }
            return PlaintextBallot(ballotId, ballotStyleId, contests)
        }

        fun getRandomContestFrom(contest: ManifestSimple.ContestSimple) : PlaintextBallot.Contest {
            var voted = 0
            val selections: MutableList<PlaintextBallot.Selection> = ArrayList()
            for (selection_description in contest.selections) {
                val selection: PlaintextBallot.Selection = getRandomSelectionFrom(selection_description)
                voted += selection.vote
                if (voted <= contest.votesAllowed) {
                    selections.add(selection)
                }
            }
            return PlaintextBallot.Contest(
                contest.contestId,
                contest.sequenceOrder,
                selections
            )
        }

        companion object {
            fun getRandomSelectionFrom(description: ManifestSimple.SelectionSimple): PlaintextBallot.Selection {
                val choice: Boolean = Random.nextBoolean()
                return PlaintextBallot.Selection(
                    description.selectionId, description.sequenceOrder,
                    if (choice) 1 else 0, null
                )
            }
        }
    }
}