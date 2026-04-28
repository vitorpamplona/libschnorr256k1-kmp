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

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

private fun ByteArray.toUint8Array(): Uint8Array {
    val result = Uint8Array(size)
    for (i in indices) result[i] = (this[i].toInt() and 0xFF).toByte()
    return result
}

private fun Uint8Array.toByteArray(): ByteArray {
    return ByteArray(length) { this[it] }
}

@Suppress("UNUSED_PARAMETER")
private fun getBridge(): dynamic = js("globalThis._schnorr256k1_bridge")

actual object Schnorr256k1 {
    private var initialized = false

    actual fun ensureLoaded() {
        if (!initialized) {
            val bridge = getBridge()
                ?: error("schnorr256k1 WASM bridge not loaded. Set globalThis._schnorr256k1_bridge before use.")
            bridge.init()
            initialized = true
        }
    }

    private fun bridge(): dynamic {
        ensureLoaded()
        return getBridge()
    }

    actual fun pubkeyCreate(seckey: ByteArray): ByteArray? {
        val result = bridge().pubkeyCreate(seckey.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun pubkeyCompress(pubkey: ByteArray): ByteArray? {
        val result = bridge().pubkeyCompress(pubkey.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun seckeyVerify(seckey: ByteArray): Boolean {
        return bridge().seckeyVerify(seckey.toUint8Array()) as Boolean
    }

    actual fun schnorrSign(msg: ByteArray, seckey: ByteArray, auxrand: ByteArray?): ByteArray? {
        val result = bridge().schnorrSign(
            msg.toUint8Array(),
            seckey.toUint8Array(),
            auxrand?.toUint8Array(),
        )
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun schnorrSignXOnly(
        msg: ByteArray,
        seckey: ByteArray,
        xonlyPub: ByteArray,
        auxrand: ByteArray?,
    ): ByteArray? {
        val result = bridge().schnorrSignXOnly(
            msg.toUint8Array(),
            seckey.toUint8Array(),
            xonlyPub.toUint8Array(),
            auxrand?.toUint8Array(),
        )
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun schnorrVerify(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        return bridge().schnorrVerify(
            sig.toUint8Array(),
            msg.toUint8Array(),
            pub.toUint8Array(),
        ) as Boolean
    }

    actual fun schnorrVerifyFast(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        return bridge().schnorrVerifyFast(
            sig.toUint8Array(),
            msg.toUint8Array(),
            pub.toUint8Array(),
        ) as Boolean
    }

    actual fun schnorrVerifyBatch(
        pub: ByteArray,
        sigs: List<ByteArray>,
        msgs: List<ByteArray>,
    ): Boolean {
        val jsSigs: dynamic = js("[]")
        sigs.forEach { jsSigs.push(it.toUint8Array()) }
        val jsMsgs: dynamic = js("[]")
        msgs.forEach { jsMsgs.push(it.toUint8Array()) }
        return bridge().schnorrVerifyBatch(pub.toUint8Array(), jsSigs, jsMsgs) as Boolean
    }

    actual fun privkeyTweakAdd(seckey: ByteArray, tweak: ByteArray): ByteArray? {
        val result = bridge().privkeyTweakAdd(seckey.toUint8Array(), tweak.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun pubkeyTweakMul(pubkey: ByteArray, tweak: ByteArray): ByteArray? {
        val result = bridge().pubkeyTweakMul(pubkey.toUint8Array(), tweak.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun ecdhXOnly(xonlyPub: ByteArray, scalar: ByteArray): ByteArray? {
        val result = bridge().ecdhXOnly(xonlyPub.toUint8Array(), scalar.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun sha256(data: ByteArray): ByteArray? {
        val result = bridge().sha256(data.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }

    actual fun taggedHash(tag: String, msg: ByteArray): ByteArray? {
        val result = bridge().taggedHash(tag, msg.toUint8Array())
        return (result as? Uint8Array)?.toByteArray()
    }
}
