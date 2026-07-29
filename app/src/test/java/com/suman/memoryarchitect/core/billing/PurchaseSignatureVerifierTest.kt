package com.suman.memoryarchitect.core.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/**
 * Exercises [PurchaseSignatureVerifier.verifySignature] - the pure cryptographic core split out from
 * [PurchaseSignatureVerifier.verify] specifically so it's testable without a real [com.android.billingclient.api.Purchase]
 * (this project has no Mockito/MockK and no Robolectric, so constructing or mocking one isn't
 * practical - see that class's own doc). Generates a real RSA key pair per test rather than a
 * checked-in fixture, so these tests never depend on (and can never accidentally leak) any real key
 * material.
 *
 * [PurchaseSignatureVerifier.verify] itself - the `Purchase`-accepting wrapper, and the
 * [PurchaseSignatureVerifier.BASE64_PUBLIC_KEY]-is-blank `NotConfigured` path specifically - isn't
 * covered here for that same reason; that gap is real and worth knowing about, not silently
 * papered over.
 */
class PurchaseSignatureVerifierTest {

    private fun generateKeyPair() = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun sign(privateKey: java.security.PrivateKey, data: String): String {
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    @Test
    fun `a genuine signature over the exact signed data verifies true`() {
        val keyPair = generateKeyPair()
        val signedData = """{"orderId":"GPA.1234-5678","productId":"royal_collection"}"""
        val signatureBase64 = sign(keyPair.private, signedData)
        val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        assertTrue(PurchaseSignatureVerifier.verifySignature(publicKeyBase64, signedData, signatureBase64))
    }

    @Test
    fun `tampered signed data - a forged productId - fails verification`() {
        val keyPair = generateKeyPair()
        val originalData = """{"orderId":"GPA.1234-5678","productId":"starter_bundle"}"""
        val signatureBase64 = sign(keyPair.private, originalData)
        val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val tamperedData = """{"orderId":"GPA.1234-5678","productId":"royal_collection"}"""
        assertFalse(PurchaseSignatureVerifier.verifySignature(publicKeyBase64, tamperedData, signatureBase64))
    }

    @Test
    fun `a signature from a different key pair fails verification`() {
        val realKeyPair = generateKeyPair()
        val attackerKeyPair = generateKeyPair()
        val signedData = """{"orderId":"GPA.1234-5678","productId":"remove_ads_lifetime"}"""
        val forgedSignature = sign(attackerKeyPair.private, signedData)
        val realPublicKeyBase64 = Base64.getEncoder().encodeToString(realKeyPair.public.encoded)

        assertFalse(PurchaseSignatureVerifier.verifySignature(realPublicKeyBase64, signedData, forgedSignature))
    }

    @Test(expected = Exception::class)
    fun `a malformed public key throws rather than silently passing`() {
        PurchaseSignatureVerifier.verifySignature("not-valid-base64-key-material", "data", "c2ln")
    }

    @Test(expected = Exception::class)
    fun `corrupted signature bytes throw rather than silently passing`() {
        val keyPair = generateKeyPair()
        val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        PurchaseSignatureVerifier.verifySignature(publicKeyBase64, "data", "not-valid-base64-signature!!!")
    }
}
