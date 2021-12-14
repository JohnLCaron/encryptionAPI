package electionguard

/**
 * Our own assert function, which isn't available in the Kotlin standard library on JavaScript, even
 * though it's available on JVM and Native. If `condition` is `false`, then an `AssertionError` is
 * thrown with the given message, which defaults to "Assertion failed".
 */
fun assert(condition: Boolean, message: () -> String = { "Assertion failed" }) {
    if (!condition) {
        throw AssertionError(message())
    }
}

/**
 * Throughout our bignum arithmetic, every operation needs to check that its operands are
 * compatible (i.e., that we're not trying to use the test group and the production group
 * interchangeably). This will verify that compatibility and throw an `ArithmeticException`
 * if they're not.
 */
fun GroupContext.assertCompatible(other: GroupContext) {
    if (!this.isCompatible(other)) {
        throw ArithmeticException("incompatible group contexts")
    }
}