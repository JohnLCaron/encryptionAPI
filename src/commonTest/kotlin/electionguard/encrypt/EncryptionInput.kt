package electionguard.encrypt

import electionguard.ballot.Manifest
import electionguard.ballot.ManifestSimple
import electionguard.ballot.makeSimple
import electionguard.input.ManifestInputBuilder

fun generateManifestSimple(ncontests : Int, nselection : Int) : ManifestSimple {
    val ebuilder = ManifestInputBuilder("manifestSimple")
    var gselection : Int = 1
    var candididate : Int = 1
    for (contest in 1..ncontests) {
        val cb = ebuilder.addContest("contest$contest")
        for (selection in 1..nselection) {
            cb.addSelection("selection$gselection", "candidate$candididate")
            gselection++
            candididate++
        }
    }
    val manifest: Manifest = ebuilder.build()
    return manifest.makeSimple()
}