package electionguard.serialize

import electionguard.core.*
import io.kotest.property.checkAll
import kotlin.test.*

class UInt256JsonTest {
    @Test
    fun roundtripTest() {
        runTest {
            checkAll(uint256s()) { u ->
                val urt = importUInt256(u.publishJson())
                assertNotNull(urt)
                assertEquals(u, urt)
            }
        }
    }
}