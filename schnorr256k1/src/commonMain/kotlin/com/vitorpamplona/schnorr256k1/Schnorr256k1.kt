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

expect object Schnorr256k1 {
    fun ensureLoaded()
    fun pubkeyCreate(seckey: ByteArray): ByteArray?
    fun pubkeyCompress(pubkey: ByteArray): ByteArray?
    fun seckeyVerify(seckey: ByteArray): Boolean
    fun schnorrSign(msg: ByteArray, seckey: ByteArray, auxrand: ByteArray? = null): ByteArray?
    fun schnorrSignXOnly(msg: ByteArray, seckey: ByteArray, xonlyPub: ByteArray, auxrand: ByteArray? = null): ByteArray?
    fun schnorrVerify(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean
    fun schnorrVerifyFast(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean
    fun schnorrVerifyBatch(pub: ByteArray, sigs: List<ByteArray>, msgs: List<ByteArray>): Boolean
    fun privkeyTweakAdd(seckey: ByteArray, tweak: ByteArray): ByteArray?
    fun pubkeyTweakMul(pubkey: ByteArray, tweak: ByteArray): ByteArray?
    fun ecdhXOnly(xonlyPub: ByteArray, scalar: ByteArray): ByteArray?
    fun sha256(data: ByteArray): ByteArray?
    fun taggedHash(tag: String, msg: ByteArray): ByteArray?
}
