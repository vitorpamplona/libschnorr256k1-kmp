/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.schnorr256k1

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class Schnorr256k1Test {
    private fun hex(s: String): ByteArray = s.hexToByteArray()
    private fun ByteArray.toHex(): String = this.toHexString()

    @Test
    fun testEnsureLoaded() {
        Schnorr256k1.ensureLoaded()
    }

    @Test
    fun testSeckeyVerify() {
        val validKey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        assertTrue(Schnorr256k1.seckeyVerify(validKey))
    }

    @Test
    fun testPubkeyCreate() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val pub = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub)
        assertEquals(65, pub.size)
        assertEquals(0x04, pub[0].toInt() and 0xFF)
    }

    @Test
    fun testPubkeyCompress() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)

        val pub33 = Schnorr256k1.pubkeyCompress(pub65)
        assertNotNull(pub33)
        assertEquals(33, pub33.size)
        assertTrue(pub33[0].toInt() == 0x02 || pub33[0].toInt() == 0x03)
    }

    @Test
    fun testSchnorrSignVerifyRoundTrip() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val msg = hex("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35")

        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)
        val xonlyPub = pub65.copyOfRange(1, 33)

        val sig = Schnorr256k1.schnorrSign(msg, seckey, null)
        assertNotNull(sig)
        assertEquals(64, sig.size)

        assertTrue(Schnorr256k1.schnorrVerify(sig, msg, xonlyPub))
    }

    @Test
    fun testSchnorrVerifyFast() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val msg = hex("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35")

        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)
        val xonlyPub = pub65.copyOfRange(1, 33)

        val sig = Schnorr256k1.schnorrSign(msg, seckey, null)
        assertNotNull(sig)

        assertTrue(Schnorr256k1.schnorrVerifyFast(sig, msg, xonlyPub))
    }

    @Test
    fun testSchnorrSignXOnly() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val msg = hex("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35")

        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)
        val xonlyPub = pub65.copyOfRange(1, 33)

        val sig = Schnorr256k1.schnorrSignXOnly(msg, seckey, xonlyPub, null)
        assertNotNull(sig)
        assertEquals(64, sig.size)

        assertTrue(Schnorr256k1.schnorrVerify(sig, msg, xonlyPub))
    }

    @Test
    fun testDeterministicSigning() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val msg = hex("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35")

        val sig1 = Schnorr256k1.schnorrSign(msg, seckey, null)
        val sig2 = Schnorr256k1.schnorrSign(msg, seckey, null)
        assertNotNull(sig1)
        assertNotNull(sig2)
        assertContentEquals(sig1, sig2)
    }

    @Test
    fun testBatchVerification() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)
        val xonlyPub = pub65.copyOfRange(1, 33)

        val msgs = listOf(
            hex("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35"),
            hex("a8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35"),
            hex("b8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35"),
        )

        val sigs = msgs.map { msg ->
            val sig = Schnorr256k1.schnorrSign(msg, seckey, null)
            assertNotNull(sig)
            sig
        }

        assertTrue(Schnorr256k1.schnorrVerifyBatch(xonlyPub, sigs, msgs))
    }

    @Test
    fun testPrivkeyTweakAdd() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val tweak = hex("0000000000000000000000000000000000000000000000000000000000000001")

        val result = Schnorr256k1.privkeyTweakAdd(seckey, tweak)
        assertNotNull(result)
        assertEquals(32, result.size)

        assertTrue(Schnorr256k1.seckeyVerify(result))
    }

    @Test
    fun testPubkeyTweakMul() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val tweak = hex("0000000000000000000000000000000000000000000000000000000000000002")
        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)
        val pub33 = Schnorr256k1.pubkeyCompress(pub65)
        assertNotNull(pub33)

        val result = Schnorr256k1.pubkeyTweakMul(pub33, tweak)
        assertNotNull(result)
        assertEquals(pub33.size, result.size)
    }

    @Test
    fun testEcdhXOnlySymmetry() {
        val seckey1 = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val seckey2 = hex("a8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35")

        val pub1 = Schnorr256k1.pubkeyCreate(seckey1)
        val pub2 = Schnorr256k1.pubkeyCreate(seckey2)
        assertNotNull(pub1)
        assertNotNull(pub2)

        val xonly1 = pub1.copyOfRange(1, 33)
        val xonly2 = pub2.copyOfRange(1, 33)

        val shared1 = Schnorr256k1.ecdhXOnly(xonly2, seckey1)
        val shared2 = Schnorr256k1.ecdhXOnly(xonly1, seckey2)
        assertNotNull(shared1)
        assertNotNull(shared2)

        assertContentEquals(shared1, shared2)
    }

    @Test
    fun testSha256() {
        val data = "hello".encodeToByteArray()
        val hash = Schnorr256k1.sha256(data)
        assertNotNull(hash)
        assertEquals(32, hash.size)
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            hash.toHex(),
        )
    }

    @Test
    fun testTaggedHash() {
        val msg = "hello".encodeToByteArray()
        val hash = Schnorr256k1.taggedHash("BIP0340/challenge", msg)
        assertNotNull(hash)
        assertEquals(32, hash.size)

        val hash2 = Schnorr256k1.taggedHash("BIP0340/challenge", msg)
        assertNotNull(hash2)
        assertContentEquals(hash, hash2)
    }

    @Test
    fun testVariableLengthMessages() {
        val seckey = hex("67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa")
        val pub65 = Schnorr256k1.pubkeyCreate(seckey)
        assertNotNull(pub65)
        val xonlyPub = pub65.copyOfRange(1, 33)

        for (len in listOf(1, 16, 32, 64, 100, 256, 1024)) {
            val msg = ByteArray(len) { it.toByte() }
            val sig = Schnorr256k1.schnorrSign(msg, seckey, null)
            assertNotNull(sig, "Sign failed for msg length $len")
            assertTrue(
                Schnorr256k1.schnorrVerify(sig, msg, xonlyPub),
                "Verify failed for msg length $len",
            )
        }
    }
}
