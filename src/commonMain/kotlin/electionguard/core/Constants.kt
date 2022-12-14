package electionguard.core

import electionguard.ballot.ElectionConstants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ElectionConstants")

/**
 * Validates whether external data, possibly encrypted using obsolete group context parameters, is
 * compatible with the current group, supported by the current ElectionGuard library.
 */
fun ElectionConstants.isCompatible(other: ElectionConstants): Boolean =
    other.largePrime.contentEquals(largePrime) && other.smallPrime.contentEquals(smallPrime) &&
        other.generator.contentEquals(generator) && other.cofactor.contentEquals(cofactor)

/**
 * Validates whether external data, possibly encrypted using obsolete group context parameters, is
 * compatible with the current group, supported by the current ElectionGuard library. If
 * incompatible, throws a [RuntimeException].
 */
fun ElectionConstants.requireCompatible(other: ElectionConstants) {
    if (!isCompatible(other)) {
        val errStr =
            "other group is incompatible with this group: " +
                Json.encodeToString(mapOf("other" to other, "this" to this))
        logger.warn { errStr }
        throw RuntimeException(errStr)
    }
}