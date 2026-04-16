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

private external interface WasmBridge : JsAny {
    fun init()
    fun pubkeyCreate(seckey: Uint8Array): Uint8Array?
    fun pubkeyCompress(pubkey: Uint8Array): Uint8Array?
    fun seckeyVerify(seckey: Uint8Array): JsBoolean
    fun schnorrSign(msg: Uint8Array, seckey: Uint8Array, auxrand: Uint8Array?): Uint8Array?
    fun schnorrSignXOnly(msg: Uint8Array, seckey: Uint8Array, xonlyPub: Uint8Array, auxrand: Uint8Array?): Uint8Array?
    fun schnorrVerify(sig: Uint8Array, msg: Uint8Array, pub: Uint8Array): JsBoolean
    fun schnorrVerifyFast(sig: Uint8Array, msg: Uint8Array, pub: Uint8Array): JsBoolean
    fun schnorrVerifyBatch(pub: Uint8Array, sigs: JsArray<Uint8Array>, msgs: JsArray<Uint8Array>): JsBoolean
    fun privkeyTweakAdd(seckey: Uint8Array, tweak: Uint8Array): Uint8Array?
    fun pubkeyTweakMul(pubkey: Uint8Array, tweak: Uint8Array): Uint8Array?
    fun ecdhXOnly(xonlyPub: Uint8Array, scalar: Uint8Array): Uint8Array?
    fun sha256(data: Uint8Array): Uint8Array?
    fun taggedHash(tag: JsString, msg: Uint8Array): Uint8Array?
}

@JsFun("() => globalThis._schnorr256k1_bridge")
private external fun getBridge(): WasmBridge?

private fun ByteArray.toUint8Array(): Uint8Array {
    val int8 = Int8Array(size)
    for (i in indices) {
        int8[i] = this[i]
    }
    return Uint8Array(int8.buffer)
}

private fun Uint8Array.toByteArray(): ByteArray {
    return ByteArray(length) { this[it] }
}

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

    private fun bridge(): WasmBridge {
        ensureLoaded()
        return getBridge()!!
    }

    actual fun pubkeyCreate(seckey: ByteArray): ByteArray? {
        return bridge().pubkeyCreate(seckey.toUint8Array())?.toByteArray()
    }

    actual fun pubkeyCompress(pubkey: ByteArray): ByteArray? {
        return bridge().pubkeyCompress(pubkey.toUint8Array())?.toByteArray()
    }

    actual fun seckeyVerify(seckey: ByteArray): Boolean {
        return bridge().seckeyVerify(seckey.toUint8Array()).toBoolean()
    }

    actual fun schnorrSign(msg: ByteArray, seckey: ByteArray, auxrand: ByteArray?): ByteArray? {
        return bridge().schnorrSign(
            msg.toUint8Array(),
            seckey.toUint8Array(),
            auxrand?.toUint8Array(),
        )?.toByteArray()
    }

    actual fun schnorrSignXOnly(
        msg: ByteArray,
        seckey: ByteArray,
        xonlyPub: ByteArray,
        auxrand: ByteArray?,
    ): ByteArray? {
        return bridge().schnorrSignXOnly(
            msg.toUint8Array(),
            seckey.toUint8Array(),
            xonlyPub.toUint8Array(),
            auxrand?.toUint8Array(),
        )?.toByteArray()
    }

    actual fun schnorrVerify(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        return bridge().schnorrVerify(
            sig.toUint8Array(),
            msg.toUint8Array(),
            pub.toUint8Array(),
        ).toBoolean()
    }

    actual fun schnorrVerifyFast(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        return bridge().schnorrVerifyFast(
            sig.toUint8Array(),
            msg.toUint8Array(),
            pub.toUint8Array(),
        ).toBoolean()
    }

    actual fun schnorrVerifyBatch(
        pub: ByteArray,
        sigs: List<ByteArray>,
        msgs: List<ByteArray>,
    ): Boolean {
        val jsSigs = JsArray<Uint8Array>()
        sigs.forEach { jsSigs.push(it.toUint8Array()) }
        val jsMsgs = JsArray<Uint8Array>()
        msgs.forEach { jsMsgs.push(it.toUint8Array()) }
        return bridge().schnorrVerifyBatch(
            pub.toUint8Array(),
            jsSigs,
            jsMsgs,
        ).toBoolean()
    }

    actual fun privkeyTweakAdd(seckey: ByteArray, tweak: ByteArray): ByteArray? {
        return bridge().privkeyTweakAdd(
            seckey.toUint8Array(),
            tweak.toUint8Array(),
        )?.toByteArray()
    }

    actual fun pubkeyTweakMul(pubkey: ByteArray, tweak: ByteArray): ByteArray? {
        return bridge().pubkeyTweakMul(
            pubkey.toUint8Array(),
            tweak.toUint8Array(),
        )?.toByteArray()
    }

    actual fun ecdhXOnly(xonlyPub: ByteArray, scalar: ByteArray): ByteArray? {
        return bridge().ecdhXOnly(
            xonlyPub.toUint8Array(),
            scalar.toUint8Array(),
        )?.toByteArray()
    }

    actual fun sha256(data: ByteArray): ByteArray? {
        return bridge().sha256(data.toUint8Array())?.toByteArray()
    }

    actual fun taggedHash(tag: String, msg: ByteArray): ByteArray? {
        return bridge().taggedHash(tag.toJsString(), msg.toUint8Array())?.toByteArray()
    }
}

private fun <T : JsAny> JsArray<T>.push(value: T) {
    set(length, value)
}
