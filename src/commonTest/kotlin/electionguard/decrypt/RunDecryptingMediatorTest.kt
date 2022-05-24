package electionguard.decrypt

import electionguard.core.productionGroup
import electionguard.decrypt.readDecryptingTrustees
import electionguard.decrypt.runDecryptTally

import kotlin.test.Test

/** Test DecryptingMediator with in-process DecryptingTrustee's. */
class RunDecryptingMediatorTest {
    @Test
    fun testDecryptingMediatorAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingMediatorAll"
        runDecryptTally(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir), "createdBy")
    }

    @Test
    fun testDecryptingMediatorSome() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-trustees",
                "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingMediatorSome",
                "-createdBy",
                "testDecryptingMediatorSome"
            )
        )
    }
}