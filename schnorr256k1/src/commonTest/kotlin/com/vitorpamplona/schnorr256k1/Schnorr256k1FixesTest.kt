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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The libschnorr256k1 fixes this binding ships, each with a case 1.0.5 got wrong, and
 * [Schnorr256k1.schnorrSignWithPubKey].
 */
@OptIn(ExperimentalStdlibApi::class)
class Schnorr256k1FixesTest {
    private fun hex(s: String): ByteArray = s.hexToByteArray()

    private fun seckey(i: Int): ByteArray = ByteArray(32).also {
        it[0] = 0x5a
        it[30] = (i shr 8).toByte()
        it[31] = i.toByte()
    }

    /** The first secret keys from [from] on whose public key has odd (or even) y. */
    private fun keysWithParity(odd: Boolean, count: Int, from: Int = 1): List<ByteArray> =
        generateSequence(from) { it + 1 }
            .map { seckey(it) }
            .filter { (Schnorr256k1.pubkeyCreate(it)!![64].toInt() and 1 == 1) == odd }
            .take(count)
            .toList()

    private fun compressed(seckey: ByteArray): ByteArray =
        Schnorr256k1.pubkeyCompress(Schnorr256k1.pubkeyCreate(seckey)!!)!!

    private fun xOnly(seckey: ByteArray): ByteArray = Schnorr256k1.pubkeyCreate(seckey)!!.copyOfRange(1, 33)

    private val msg = hex("243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89")
    private val aux = hex("0000000000000000000000000000000000000000000000000000000000000001")

    @Test
    fun bip340Vector1() {
        val sk = hex("b7e151628aed2a6abf7158809cf4f3c762e7160f38b4da56a784d9045190cfef")
        val expected = hex(
            "6896bd60eeae296db48a229ff71dfe071bde413e6d43f917dc8dcf8c78de3341" +
                "8906d11ac976abccb20b091292bff4ea897efcb639ea871cfa95f6de339e4b0a",
        )
        assertContentEquals(expected, Schnorr256k1.schnorrSign(msg, sk, aux))
        assertContentEquals(expected, Schnorr256k1.schnorrSignWithPubKey(msg, sk, compressed(sk), aux))
        assertContentEquals(expected, Schnorr256k1.schnorrSignXOnly(msg, sk, xOnly(sk), aux))
    }

    /** Null aux randomness is BIP-340's 32 zero bytes (1.0.5 skipped the aux step instead). */
    @Test
    fun nullAuxIsZeroAux() {
        val sk = hex("0000000000000000000000000000000000000000000000000000000000000003")
        val zero = ByteArray(32)
        val expected = hex(
            "e907831f80848d1069a5371b402410364bdf1c5f8307b0084c55f1ce2dca8215" +
                "25f66a4a85ea8b71e482a74f382d2ce5ebeee8fdb2172f477df4900d310536c0",
        )
        assertContentEquals(expected, Schnorr256k1.schnorrSign(zero, sk, zero))
        assertContentEquals(expected, Schnorr256k1.schnorrSign(zero, sk, null))
        assertContentEquals(expected, Schnorr256k1.schnorrSignWithPubKey(zero, sk, compressed(sk), null))
    }

    @Test
    fun signWithPubKeyMatchesSignForBothParities() {
        for (sk in keysWithParity(odd = true, count = 4) + keysWithParity(odd = false, count = 4)) {
            val expected = Schnorr256k1.schnorrSign(msg, sk, aux)
            val sig = Schnorr256k1.schnorrSignWithPubKey(msg, sk, compressed(sk), aux)
            assertContentEquals(expected, sig)
            assertTrue(Schnorr256k1.schnorrVerify(sig!!, msg, xOnly(sk)))
        }
    }

    @Test
    fun signWithPubKeyRejectsMalformedKeys() {
        val sk = seckey(1)
        val pub33 = compressed(sk)
        assertNull(Schnorr256k1.schnorrSignWithPubKey(msg, sk, pub33.copyOf().also { it[0] = 0x04 }, aux))
        assertNull(Schnorr256k1.schnorrSignWithPubKey(msg, sk, pub33.copyOfRange(1, 33), aux))
        assertNull(Schnorr256k1.schnorrSignWithPubKey(msg, sk, Schnorr256k1.pubkeyCreate(sk)!!, aux))
        assertNull(Schnorr256k1.schnorrSignWithPubKey(msg, ByteArray(31), pub33, aux))
    }

    /** KI-2: 1.0.5's schnorrSignXOnly signed with the wrong secret for odd-y keys. */
    @Test
    fun signXOnlyWorksForOddYKeys() {
        for (sk in keysWithParity(odd = true, count = 4)) {
            val sig = Schnorr256k1.schnorrSignXOnly(msg, sk, xOnly(sk), aux)
            assertNotNull(sig)
            assertContentEquals(Schnorr256k1.schnorrSign(msg, sk, aux), sig)
            assertTrue(Schnorr256k1.schnorrVerify(sig, msg, xOnly(sk)))
        }
    }

    @Test
    fun signXOnlyRejectsSomeoneElsesKey() {
        assertNull(Schnorr256k1.schnorrSignXOnly(msg, seckey(1), xOnly(seckey(2)), aux))
    }

    /**
     * KI-1: 1.0.5's batch had no weights, so adding 1 to one signature's s and subtracting 1
     * from another's kept the sum of the batch equation and was accepted.
     */
    @Test
    fun batchRejectsSignaturesThatCancel() {
        val sk = seckey(7)
        val pub = xOnly(sk)
        val msgs = (0 until 4).map { i -> ByteArray(32) { (i * 37 + it).toByte() } }
        val sigs = msgs.map { Schnorr256k1.schnorrSign(it, sk, aux)!! }
        assertTrue(Schnorr256k1.schnorrVerifyBatch(pub, sigs, msgs))

        val one = ByteArray(32).also { it[31] = 1 }
        val minusOne = hex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140")
        val forged = sigs.map { it.copyOf() }
        Schnorr256k1.privkeyTweakAdd(forged[0].copyOfRange(32, 64), one)!!.copyInto(forged[0], 32)
        Schnorr256k1.privkeyTweakAdd(forged[1].copyOfRange(32, 64), minusOne)!!.copyInto(forged[1], 32)

        assertFalse(Schnorr256k1.schnorrVerify(forged[0], msgs[0], pub))
        assertFalse(Schnorr256k1.schnorrVerify(forged[1], msgs[1], pub))
        assertFalse(Schnorr256k1.schnorrVerifyBatch(pub, forged, msgs))
    }

    @Test
    fun batchOfNoneOrOne() {
        val sk = seckey(8)
        val sig = Schnorr256k1.schnorrSign(msg, sk, aux)!!
        assertTrue(Schnorr256k1.schnorrVerifyBatch(xOnly(sk), emptyList(), emptyList()))
        assertTrue(Schnorr256k1.schnorrVerifyBatch(xOnly(sk), listOf(sig), listOf(msg)))
        assertFalse(Schnorr256k1.schnorrVerifyBatch(xOnly(sk), listOf(sig), listOf(aux)))
    }

    /**
     * KI-3: in 1.0.5 a tweak-mul on a key's odd-y form left a table, keyed by x alone, that
     * verify then used for the even-y lift of the same x, rejecting valid signatures.
     */
    @Test
    fun tweakMulOnOddYKeyDoesNotBreakVerify() {
        val sk = keysWithParity(odd = true, count = 1, from = 1000).single()
        val pub33 = compressed(sk)
        assertEquals(0x03, pub33[0].toInt())
        val tweak = seckey(99)

        val before = Schnorr256k1.pubkeyTweakMul(pub33, tweak)
        assertNotNull(before)
        val sig = Schnorr256k1.schnorrSign(msg, sk, aux)!!
        assertTrue(Schnorr256k1.schnorrVerify(sig, msg, xOnly(sk)))
        assertTrue(Schnorr256k1.schnorrVerifyFast(sig, msg, xOnly(sk)))
        assertContentEquals(before, Schnorr256k1.pubkeyTweakMul(pub33, tweak))
    }
}
