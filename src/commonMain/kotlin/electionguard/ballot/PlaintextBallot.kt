package electionguard.ballot

import kotlinx.serialization.*

/**
 * The plaintext representation of a voter's ballot selections as input to the system.
 * The ballotId is a unique Ballot ID created by the external system.
 * Only the contests and selections voted for need be present.
 */
@Serializable
data class PlaintextBallot(
    val ballotId: String,       // a unique ballot ID created by the external system
    val ballotStyleId: String,  // matches BallotStyle.ballotStyleId
    val contests: List<Contest>,
    val errors: String? = null, // error messages from processing, eg when invalid
) {
    init {
        require(ballotId.isNotEmpty())
    }

    constructor(org: PlaintextBallot, errors: String):
        this(org.ballotId, org.ballotStyleId, org.contests, errors)

    /** The plaintext representation of a voter's selections for one contest. */
    @Serializable
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val sequenceOrder: Int,
        val selections: List<Selection>,
    ) {
        init {
            require(contestId.isNotEmpty())
        }
    }

    /** The plaintext representation of one selection for a particular contest. */
    @Serializable
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int,
        val vote: Int,
        val extendedData: String?,
    )  {
        init {
            require(selectionId.isNotEmpty())
            require(vote >= 0)
        }
    }
}