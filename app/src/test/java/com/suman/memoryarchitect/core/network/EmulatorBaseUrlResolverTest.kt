package com.suman.memoryarchitect.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class EmulatorBaseUrlResolverTest {

    @Test
    fun `rewrites a LAN IP host to the emulator loopback alias, preserving port, path, and scheme`() {
        assertEquals(
            "http://10.0.2.2:4000/",
            EmulatorBaseUrlResolver.resolveForEmulator("http://192.168.29.120:4000/"),
        )
    }

    @Test
    fun `preserves a non-root path`() {
        assertEquals(
            "https://10.0.2.2:8443/api/v1/",
            EmulatorBaseUrlResolver.resolveForEmulator("https://192.168.1.11:8443/api/v1/"),
        )
    }

    @Test
    fun `an already-emulator-loopback host is rewritten to itself, unchanged`() {
        assertEquals(
            "http://10.0.2.2:4000/",
            EmulatorBaseUrlResolver.resolveForEmulator("http://10.0.2.2:4000/"),
        )
    }

    @Test
    fun `an unparseable value is returned unchanged rather than throwing`() {
        assertEquals("not a url", EmulatorBaseUrlResolver.resolveForEmulator("not a url"))
    }
}
