package electionguard.ballot

import electionguard.core.*
import kotlinx.serialization.Serializable

/**
 * A simple version of the manifest, for encryption.
 */
@Serializable
data class ManifestSimple(
    val electionScopeId: String,
    val contests: List<ContestSimple>,
    val ballotStyles: List<BallotStyle>,
    val cryptoHash: UInt256,
) {

    /** Map of ballotStyleId to all Contests that use it. */
    val styleToContestsMap: Map<String, List<ContestSimple>> by
    lazy {
        val result = mutableMapOf<String, List<ContestSimple>>() // key = ballotStyleId
        val gpuToContests: Map<String, List<ContestSimple>> =
            contests.groupBy { it.geopoliticalUnitId } // key = geopoliticalUnitId
        ballotStyles.forEach { style ->
            val contestSet = mutableSetOf<ContestSimple>()
            style.geopoliticalUnitIds.forEach {
                val contestList = gpuToContests[it]
                if (contestList != null) {
                    contestSet.addAll(contestList)
                }
            }
            result[style.ballotStyleId] = contestSet.toList().sortedBy { it.sequenceOrder }
        }
        result
    }

    /** Map of contestId to contest limit. */
    val contestIdToLimit: Map<String, Int> by
    lazy {
        contests.associate { it.contestId to it.votesAllowed }
    }

    /** Set of "contestId/selectionId" to detect existence. */
    val contestAndSelectionSet: Set<String> by
    lazy {
        contests.map { contest -> contest.selections.map { it -> "${contest.contestId}/${it.selectionId}" } }.flatten()
            .toSet()
    }

    @Serializable
    data class BallotStyle(
        val ballotStyleId: String,
        val geopoliticalUnitIds: List<String>,
    )

    @Serializable
    data class ContestSimple(
        val contestId: String,
        val sequenceOrder: Int,
        val geopoliticalUnitId: String,
        val numberElected: Int,
        val votesAllowed: Int,
        val selections: List<SelectionSimple>,
        val cryptoHash: UInt256,
    )

    @Serializable
    data class SelectionSimple(
        val selectionId: String,
        val sequenceOrder: Int,
        val cryptoHash: UInt256,
    )
}

fun Manifest.makeSimple(): ManifestSimple {
    return ManifestSimple(
        electionScopeId,
        contests.map { it.makeSimple() },
        ballotStyles.map { it.makeSimple() },
        cryptoHash,
    )
}

fun Manifest.BallotStyle.makeSimple(): ManifestSimple.BallotStyle {
    return ManifestSimple.BallotStyle(
        ballotStyleId,
        geopoliticalUnitIds,
    )
}

fun Manifest.ContestDescription.makeSimple(): ManifestSimple.ContestSimple {
    return ManifestSimple.ContestSimple(
        contestId,
        sequenceOrder,
        geopoliticalUnitId,
        numberElected,
        votesAllowed,
        selections.map { it.makeSimple() },
        cryptoHash,
    )
}

fun Manifest.SelectionDescription.makeSimple(): ManifestSimple.SelectionSimple {
    return ManifestSimple.SelectionSimple(
        selectionId,
        sequenceOrder,
        cryptoHash,
    )
}