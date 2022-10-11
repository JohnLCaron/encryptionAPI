package electionguard.serialize

import electionguard.ballot.Manifest
import electionguard.ballot.ManifestSimple
import electionguard.ballot.makeSimple
import electionguard.input.ManifestInputBuilder
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ManifestSimpleJsonTest {

    @Test
    fun roundtripManifestSimple() {
        // val group = productionGroup()
        val manifest = generateManifestSimple()
        val s = Json.encodeToString(manifest)
        println(s)
        val rt = Json.decodeFromString<ManifestSimple>(s)
        println(rt)
        assertEquals(manifest, rt)
    }

}

fun generateManifestSimple() : ManifestSimple {
    val ebuilder = ManifestInputBuilder("test_manifest")
    val manifest: Manifest = ebuilder.addContest("contest_id")
        .addSelection("selection_id", "candidate_1")
        .addSelection("selection_id2", "candidate_2")
        .done()
        .build()
    return manifest.makeSimple()
}