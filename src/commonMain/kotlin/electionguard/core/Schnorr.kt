package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("Schnorr")

/**
 * Representation of a proof that the prover know the private key corresponding to the given public
 * key. (The public key is not included, to keep the proof small.)
 */
data class SchnorrProof(
    val challenge: ElementModQ,
    val response: ElementModQ
)

/**
 * Given an ElGamal keypair (public and private key), this generates a proof
 * that the author of the proof knew the public and corresponding private keys. This proof is deterministically
 * generated based on the randomness provided by `nonce`, so don't use the same nonce twice, or if the argument
 * is not specified, its default value is chosen at random.
 * Not using the crypto base hash (Q), see https://github.com/microsoft/electionguard/issues/253
 */
fun ElGamalKeypair.schnorrProof(
    nonce: ElementModQ = context.randomElementModQ()
): SchnorrProof {
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce)
    val h = context.gPowP(nonce)
    val c = hashElements(publicKey, h).toElementModQ(context)
    val u = nonce + secretKey.key * c

    return SchnorrProof(c, u)
}

/**
 * Check validity of the proof for proving possession of the private key corresponding to the
 * given ElGamalPublicKey.
 */
fun ElGamalPublicKey.hasValidSchnorrProof(proof: SchnorrProof): Boolean {
    val (challenge, u) = proof
    val context = compatibleContextOrFail(this.key, challenge, u)

    val gPowU = context.gPowP(u)
    val h = gPowU / this.powP(challenge)
    val c = hashElements(this, h).toElementModQ(context)

    val inBoundsU = u.inBounds()
    val validChallenge = c == challenge
    val success = inBoundsU && validChallenge

    if (!success) {
        val resultMap =
            mapOf(
                "inBoundsU" to inBoundsU,
                "validChallenge" to validChallenge,
                "proof" to this
            )
        logger.warn { "found an invalid Schnorr proof: $resultMap" }
    }

    return success
}