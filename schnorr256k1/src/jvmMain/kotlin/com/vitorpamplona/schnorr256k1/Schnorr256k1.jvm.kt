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

actual object Schnorr256k1 {
    private var loaded = false

    actual fun ensureLoaded() {
        if (!loaded) {
            NativeLoader.load()
            nativeInit()
            loaded = true
        }
    }

    actual fun pubkeyCreate(seckey: ByteArray): ByteArray? {
        ensureLoaded()
        return nativePubkeyCreate(seckey)
    }

    actual fun pubkeyCompress(pubkey: ByteArray): ByteArray? {
        ensureLoaded()
        return nativePubkeyCompress(pubkey)
    }

    actual fun seckeyVerify(seckey: ByteArray): Boolean {
        ensureLoaded()
        return nativeSecKeyVerify(seckey)
    }

    actual fun schnorrSign(msg: ByteArray, seckey: ByteArray, auxrand: ByteArray?): ByteArray? {
        ensureLoaded()
        return nativeSchnorrSign(msg, seckey, auxrand)
    }

    actual fun schnorrSignXOnly(msg: ByteArray, seckey: ByteArray, xonlyPub: ByteArray, auxrand: ByteArray?): ByteArray? {
        ensureLoaded()
        return nativeSchnorrSignXOnly(msg, seckey, xonlyPub, auxrand)
    }

    actual fun schnorrVerify(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        ensureLoaded()
        return nativeSchnorrVerify(sig, msg, pub)
    }

    actual fun schnorrVerifyFast(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        ensureLoaded()
        return nativeSchnorrVerifyFast(sig, msg, pub)
    }

    actual fun schnorrVerifyBatch(pub: ByteArray, sigs: List<ByteArray>, msgs: List<ByteArray>): Boolean {
        ensureLoaded()
        return nativeSchnorrVerifyBatch(pub, sigs.toTypedArray(), msgs.toTypedArray())
    }

    actual fun privkeyTweakAdd(seckey: ByteArray, tweak: ByteArray): ByteArray? {
        ensureLoaded()
        return nativePrivKeyTweakAdd(seckey, tweak)
    }

    actual fun pubkeyTweakMul(pubkey: ByteArray, tweak: ByteArray): ByteArray? {
        ensureLoaded()
        return nativePubKeyTweakMul(pubkey, tweak)
    }

    actual fun ecdhXOnly(xonlyPub: ByteArray, scalar: ByteArray): ByteArray? {
        ensureLoaded()
        return nativeEcdhXOnly(xonlyPub, scalar)
    }

    actual fun sha256(data: ByteArray): ByteArray? {
        ensureLoaded()
        return nativeSha256(data)
    }

    actual fun taggedHash(tag: String, msg: ByteArray): ByteArray? {
        ensureLoaded()
        return nativeTaggedHash(tag, msg)
    }

    @JvmStatic private external fun nativeInit()
    @JvmStatic private external fun nativePubkeyCreate(seckey: ByteArray): ByteArray?
    @JvmStatic private external fun nativePubkeyCompress(pubkey: ByteArray): ByteArray?
    @JvmStatic private external fun nativeSecKeyVerify(seckey: ByteArray): Boolean
    @JvmStatic private external fun nativeSchnorrSign(msg: ByteArray, seckey: ByteArray, auxrand: ByteArray?): ByteArray?
    @JvmStatic private external fun nativeSchnorrSignXOnly(msg: ByteArray, seckey: ByteArray, xonlyPub: ByteArray, auxrand: ByteArray?): ByteArray?
    @JvmStatic private external fun nativeSchnorrVerify(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean
    @JvmStatic private external fun nativeSchnorrVerifyFast(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean
    @JvmStatic private external fun nativeSchnorrVerifyBatch(pub: ByteArray, sigs: Array<ByteArray>, msgs: Array<ByteArray>): Boolean
    @JvmStatic private external fun nativePrivKeyTweakAdd(seckey: ByteArray, tweak: ByteArray): ByteArray?
    @JvmStatic private external fun nativePubKeyTweakMul(pubkey: ByteArray, tweak: ByteArray): ByteArray?
    @JvmStatic private external fun nativeEcdhXOnly(xonlyPub: ByteArray, scalar: ByteArray): ByteArray?
    @JvmStatic private external fun nativeSha256(data: ByteArray): ByteArray?
    @JvmStatic private external fun nativeTaggedHash(tag: String, msg: ByteArray): ByteArray?
}
