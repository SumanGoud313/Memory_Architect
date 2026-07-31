package com.suman.memoryarchitect.core.billing

import android.util.Log
import com.android.billingclient.api.Purchase
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Local RSA signature verification - the Firebase-Spark-compatible replacement for what used to be
 * a Cloud Function calling the Android Publisher API server-side (`verifyRemoveAdsPurchase`/
 * `verifyPremiumPurchase` in `functions/src/index.ts` - both now dead code, no client call site left,
 * see that file's own updated doc). Google's own documented approach for apps without a backend:
 * every purchase Play returns is signed with the developer account's private key, and
 * [BASE64_PUBLIC_KEY] (the matching *public* key - safe to embed client-side, that is literally what
 * "local verification" means) lets this app check that signature itself, with no server round-trip.
 *
 * This is real, meaningful security - it rejects a purchase record that's been tampered with or
 * forged on a compromised device - but it cannot do what a server could (checking a purchase token
 * against Play's own servers for revocation/refund, or catching a token replayed from a genuinely
 * different, legitimately-purchased account - `claimedPurchaseTokens` in Firestore covers the same-
 * token-twice case client-side instead, see `firestore.rules`). That gap is an accepted, explicit
 * trade-off of running on Spark with no backend, not something this class can close.
 */
object PurchaseSignatureVerifier {

    // Play Console > [app] > Monetize > Monetization setup > Licensing public key - verified as a
    // complete, valid RSA-2048 SubjectPublicKeyInfo (294-byte DER, standard 65537 exponent) before
    // being pasted in here, not just copied in blind.
    private const val BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxTGdmsuoR76HOL3+mUJU8ORyVqMXiVhCvBXQxpobopMO9dAzPb07duDGj2Yin6bIyuUZp0uFH8i8yyOVta75g7pwqNkoihLUFu4owpG2NuWOSZEbVeKmCEA51+cTdcpQtwIJKCXR1LJIX9KIbH0v9iVQA3rjWbKsyWgSA2grdfqey+3mEbwBhNgS4Vkcbxn4Y+38WtWMk5UxZHL7blbb2e09lou6q2x/h0zR2i3AX+zTdeWRlWG9azmAmstRkNkvxy6S5c0LQczqs8Uxmi1D0eamX+xNVYYOU0bSLDk+I6v0fMOB3ya7n/sSeHd8ADBo/BYgN1dtiKl3Etmio2R68QIDAQAB"

    sealed interface Result {
        data object Valid : Result
        data object Invalid : Result

        /** [BASE64_PUBLIC_KEY] hasn't been set yet - the caller decides how to treat this (allowed
         * only in debug builds, a hard failure in release - see `BillingManagerImpl.verifyAndGrant`). */
        data object NotConfigured : Result
    }

    fun verify(purchase: Purchase): Result {
        if (BASE64_PUBLIC_KEY.isBlank()) return Result.NotConfigured
        return runCatching {
            if (verifySignature(BASE64_PUBLIC_KEY, purchase.originalJson, purchase.signature)) Result.Valid else Result.Invalid
        }.getOrElse { error ->
            // Never log the purchase's own signature/originalJson/purchaseToken - see this app's
            // "never expose purchase information in logs" requirement. The exception type alone is
            // enough to diagnose a malformed key or corrupted signature bytes.
            Log.w(TAG, "Purchase signature verification failed: ${error::class.simpleName}")
            Result.Invalid
        }
    }

    /** The actual cryptographic check, split out from [verify] as a pure function with no `Purchase`/
     * `android.util.Log` dependency purely so it's directly unit-testable on this project's
     * plain-JVM test runner (no Robolectric anywhere in this codebase) - [verify] is a thin wrapper
     * around this plus the [NotConfigured]/logging concerns. Throws on a malformed key or corrupted
     * signature bytes - [verify] is the only caller, and it already wraps this in `runCatching`. */
    internal fun verifySignature(base64PublicKey: String, signedData: String, signatureBase64: String): Boolean {
        val publicKey = generatePublicKey(base64PublicKey)
        val signatureBytes = Base64.getDecoder().decode(signatureBase64)
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initVerify(publicKey)
        signature.update(signedData.toByteArray())
        return signature.verify(signatureBytes)
    }

    private fun generatePublicKey(base64Key: String): PublicKey {
        val decoded = Base64.getDecoder().decode(base64Key)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(X509EncodedKeySpec(decoded))
    }

    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    private const val TAG = "PurchaseSignatureVerifier"
}
