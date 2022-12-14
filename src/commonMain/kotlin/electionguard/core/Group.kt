package electionguard.core

/**
 * Fetches the production-strength [GroupContext] with the desired amount of acceleration via
 * precomputation, which can result in significant extra memory usage.
 *
 * See [PowRadixOption] for the different memory use vs. performance profiles.
 *
 * Also, [ProductionMode] specifies the particular set of cryptographic constants we'll be using.
 */
expect fun productionGroup(
    acceleration: PowRadixOption = PowRadixOption.LOW_MEMORY_USE,
    mode: ProductionMode = ProductionMode.Mode4096
): GroupContext

// Engineering note: when we originally supported Kotlin/JS, this was a suspending function,
// to accommodate some JS libraries that want to do things with Promises or async. That causes
// extra effort for writing the JVM & native code. So we removed this when we removed JS.
// If/when we reintroduce JS, we'll revisit this design.