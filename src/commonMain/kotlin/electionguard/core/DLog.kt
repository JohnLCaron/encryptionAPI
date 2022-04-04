package electionguard.core

/**
 * Construct a DLog implementation for the given `base` element.
 *
 * For computing DLog with group generator as the base, see [GroupContext.dLogG].
 *
 * For computing DLog with a public key as the base, see [ElGamalPublicKey.dLog].
 */
expect fun dLoggerOf(base: ElementModP): DLog

/** General-purpose discrete-log engine. */
expect class DLog {
    /** Returns the base used for this particular DLog instance. */
    val base: ElementModP

    /**
     * Given an element x for which there exists an e, such that (base)^e = x, this will find e,
     * so long as e is "small enough" (typically under 1 billion). This will consume O(e) time, the
     * first time, after which the results are memoized for all values between 0 and e, for better
     * future performance.
     */
    fun dLog(input: ElementModP): Int?
}
