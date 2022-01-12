package electionguard.core

/** Any ElectionGuard type can implement this interface, and is then supported by [hashElements]. */
interface CryptoHashable {
    /** Generates a hash given the fields on the implementing instance. */
    fun cryptoHash(): ElementModQ
}

/**
 * Given zero or more elements, calculate their cryptographic hash using SHA256. Specifically
 * handled types are [Element], [String], and [Iterable] instances of those types, as well as
 * anything implementing [CryptoHashable]. Unsupported types yield an `IllegalArgumentException`.
 *
 * Of course, infinitely long iterables cannot be hashed, and will cause this function to run
 * forever or until it runs out of memory.
 *
 * @param elements Zero or more elements of any of the accepted types.
 * @return A cryptographic hash of these elements, concatenated.
 */
fun GroupContext.hashElements(vararg elements: Any?): ElementModQ = hashElementsNoFormula(*elements)

private fun GroupContext.hashElementsNoFormula(vararg elements: Any?): ElementModQ {
    val hashMe =
        elements
            .map {
                when (it) {
                    null -> "null"
                    is Element -> it.base16()
                    is CryptoHashable -> it.cryptoHash().base16()
                    is String -> it
                    is Number, UInt, ULong, UShort, UByte -> it.toString()
                    is Iterable<*> ->
                        // The simplest way to deal with lists and such are to crunch them
                        // recursively.

                        // We special-case the empty list, because it hashes to "null" yet has a
                        // formula of []. Note that empty lists and nulls will never occur in
                        // practice, anywhere in ElectionGuard, but hashElements needs to handle
                        // them correctly, just in case.
                        if (it.none())
                            "null"
                        else
                            hashElementsNoFormula(*(it.toList().toTypedArray())).base16()
                    else ->
                        throw IllegalArgumentException("unknown type in hashElements: ${it::class}")
                }
            }
            .fold("|") { prev, next -> "$prev$next|" }

    val digest = hashMe.encodeToByteArray().sha256()
    return safeBinaryToElementModQ(digest, maxQMinus1 = true)
}

// TODO: do we need to be able to hash anything else that wouldn't already be covered
//   by CryptoHashable? The Python reference code, if it sees an unknown type, just
//   calls str() on it, getting whatever Python implementation-specific string representation
//   might be present. For contrast, we throw an exception here. This might at least
//   become a useful way to detect things that need to be CryptoHashable but aren't.