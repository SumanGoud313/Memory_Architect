package com.suman.memoryarchitect.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op stub. This app now runs on Firebase Spark with no Cloud Functions (see the project's Spark
 * migration report) - decoding a Play Integrity token requires a server-side credential a client
 * can't hold safely, so there's no Spark-compatible replacement for what this class used to do
 * (request a token and forward it to a `verifyDeviceIntegrity` Cloud Function). Kept as a stub
 * rather than deleted so [com.suman.memoryarchitect.MemoryArchitectApp]'s call site doesn't need to
 * change if device attestation is ever reintroduced behind a real server.
 */
@Singleton
class DeviceIntegrityChecker @Inject constructor() {
    /** Intentionally does nothing - see class doc. */
    suspend fun checkOpportunistically() = Unit
}
