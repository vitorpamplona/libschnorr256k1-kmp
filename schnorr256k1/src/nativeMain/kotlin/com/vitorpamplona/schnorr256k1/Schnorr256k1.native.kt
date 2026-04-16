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

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pin
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import platform.posix.size_tVar
import schnorr256k1.secp256k1c_ecdh_xonly
import schnorr256k1.secp256k1c_init
import schnorr256k1.secp256k1c_privkey_tweak_add
import schnorr256k1.secp256k1c_pubkey_compress
import schnorr256k1.secp256k1c_pubkey_create
import schnorr256k1.secp256k1c_pubkey_tweak_mul
import schnorr256k1.secp256k1c_schnorr_sign
import schnorr256k1.secp256k1c_schnorr_sign_xonly
import schnorr256k1.secp256k1c_schnorr_verify
import schnorr256k1.secp256k1c_schnorr_verify_batch
import schnorr256k1.secp256k1c_schnorr_verify_fast
import schnorr256k1.secp256k1c_seckey_verify
import schnorr256k1.secp256k1c_sha256
import schnorr256k1.secp256k1c_tagged_hash

@OptIn(ExperimentalForeignApi::class)
actual object Schnorr256k1 {
    private var initialized = false

    actual fun ensureLoaded() {
        if (!initialized) {
            secp256k1c_init()
            initialized = true
        }
    }

    actual fun pubkeyCreate(seckey: ByteArray): ByteArray? {
        ensureLoaded()
        if (seckey.size != 32) return null
        val pub = UByteArray(65)
        val ok = pub.usePinned { p ->
            seckey.asUByteArray().usePinned { sk ->
                secp256k1c_pubkey_create(p.addressOf(0), sk.addressOf(0))
            }
        }
        return if (ok == 1) pub.asByteArray() else null
    }

    actual fun pubkeyCompress(pubkey: ByteArray): ByteArray? {
        ensureLoaded()
        if (pubkey.size != 65) return null
        val pub33 = UByteArray(33)
        val ok = pub33.usePinned { out ->
            pubkey.asUByteArray().usePinned { inp ->
                secp256k1c_pubkey_compress(out.addressOf(0), inp.addressOf(0))
            }
        }
        return if (ok == 1) pub33.asByteArray() else null
    }

    actual fun seckeyVerify(seckey: ByteArray): Boolean {
        ensureLoaded()
        if (seckey.size != 32) return false
        return seckey.asUByteArray().usePinned { sk ->
            secp256k1c_seckey_verify(sk.addressOf(0))
        } == 1
    }

    actual fun schnorrSign(msg: ByteArray, seckey: ByteArray, auxrand: ByteArray?): ByteArray? {
        ensureLoaded()
        if (seckey.size != 32) return null
        val sig = UByteArray(64)
        val ok = sig.usePinned { s ->
            seckey.asUByteArray().usePinned { sk ->
                msg.asUByteArray().usePinned { m ->
                    if (auxrand != null && auxrand.size == 32) {
                        auxrand.asUByteArray().usePinned { aux ->
                            secp256k1c_schnorr_sign(
                                s.addressOf(0), m.addressOf(0),
                                msg.size.convert(), sk.addressOf(0), aux.addressOf(0),
                            )
                        }
                    } else {
                        secp256k1c_schnorr_sign(
                            s.addressOf(0), m.addressOf(0),
                            msg.size.convert(), sk.addressOf(0), null,
                        )
                    }
                }
            }
        }
        return if (ok == 1) sig.asByteArray() else null
    }

    actual fun schnorrSignXOnly(
        msg: ByteArray,
        seckey: ByteArray,
        xonlyPub: ByteArray,
        auxrand: ByteArray?,
    ): ByteArray? {
        ensureLoaded()
        if (seckey.size != 32 || xonlyPub.size != 32) return null
        val sig = UByteArray(64)
        val ok = sig.usePinned { s ->
            seckey.asUByteArray().usePinned { sk ->
                xonlyPub.asUByteArray().usePinned { xp ->
                    msg.asUByteArray().usePinned { m ->
                        if (auxrand != null && auxrand.size == 32) {
                            auxrand.asUByteArray().usePinned { aux ->
                                secp256k1c_schnorr_sign_xonly(
                                    s.addressOf(0), m.addressOf(0),
                                    msg.size.convert(), sk.addressOf(0),
                                    xp.addressOf(0), aux.addressOf(0),
                                )
                            }
                        } else {
                            secp256k1c_schnorr_sign_xonly(
                                s.addressOf(0), m.addressOf(0),
                                msg.size.convert(), sk.addressOf(0),
                                xp.addressOf(0), null,
                            )
                        }
                    }
                }
            }
        }
        return if (ok == 1) sig.asByteArray() else null
    }

    actual fun schnorrVerify(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        ensureLoaded()
        if (sig.size != 64 || pub.size != 32) return false
        return sig.asUByteArray().usePinned { s ->
            msg.asUByteArray().usePinned { m ->
                pub.asUByteArray().usePinned { p ->
                    secp256k1c_schnorr_verify(
                        s.addressOf(0), m.addressOf(0),
                        msg.size.convert(), p.addressOf(0),
                    )
                }
            }
        } == 1
    }

    actual fun schnorrVerifyFast(sig: ByteArray, msg: ByteArray, pub: ByteArray): Boolean {
        ensureLoaded()
        if (sig.size != 64 || pub.size != 32) return false
        return sig.asUByteArray().usePinned { s ->
            msg.asUByteArray().usePinned { m ->
                pub.asUByteArray().usePinned { p ->
                    secp256k1c_schnorr_verify_fast(
                        s.addressOf(0), m.addressOf(0),
                        msg.size.convert(), p.addressOf(0),
                    )
                }
            }
        } == 1
    }

    actual fun schnorrVerifyBatch(
        pub: ByteArray,
        sigs: List<ByteArray>,
        msgs: List<ByteArray>,
    ): Boolean {
        ensureLoaded()
        if (pub.size != 32) return false
        val count = sigs.size
        if (count != msgs.size) return false
        if (count == 0) return true

        val pinnedSigs = sigs.map { it.asUByteArray().pin() }
        val pinnedMsgs = msgs.map { it.asUByteArray().pin() }
        try {
            return memScoped {
                val sigsPtr = allocArray<CPointerVar<UByteVar>>(count)
                val msgsPtr = allocArray<CPointerVar<UByteVar>>(count)
                val lensPtr = allocArray<size_tVar>(count)
                for (i in 0 until count) {
                    sigsPtr[i] = pinnedSigs[i].addressOf(0)
                    msgsPtr[i] = pinnedMsgs[i].addressOf(0)
                    lensPtr[i] = msgs[i].size.convert()
                }
                pub.asUByteArray().usePinned { p ->
                    secp256k1c_schnorr_verify_batch(
                        p.addressOf(0), sigsPtr, msgsPtr,
                        lensPtr, count.convert(),
                    )
                } == 1
            }
        } finally {
            pinnedSigs.forEach { it.unpin() }
            pinnedMsgs.forEach { it.unpin() }
        }
    }

    actual fun privkeyTweakAdd(seckey: ByteArray, tweak: ByteArray): ByteArray? {
        ensureLoaded()
        if (seckey.size != 32 || tweak.size != 32) return null
        val result = UByteArray(32)
        val ok = result.usePinned { r ->
            seckey.asUByteArray().usePinned { sk ->
                tweak.asUByteArray().usePinned { tw ->
                    secp256k1c_privkey_tweak_add(
                        r.addressOf(0), sk.addressOf(0), tw.addressOf(0),
                    )
                }
            }
        }
        return if (ok == 1) result.asByteArray() else null
    }

    actual fun pubkeyTweakMul(pubkey: ByteArray, tweak: ByteArray): ByteArray? {
        ensureLoaded()
        if (tweak.size != 32) return null
        if (pubkey.size != 33 && pubkey.size != 65) return null
        val result = UByteArray(pubkey.size)
        val ok = result.usePinned { r ->
            pubkey.asUByteArray().usePinned { pk ->
                tweak.asUByteArray().usePinned { tw ->
                    secp256k1c_pubkey_tweak_mul(
                        r.addressOf(0), pubkey.size.convert(),
                        pk.addressOf(0), pubkey.size.convert(),
                        tw.addressOf(0),
                    )
                }
            }
        }
        return if (ok == 1) result.asByteArray() else null
    }

    actual fun ecdhXOnly(xonlyPub: ByteArray, scalar: ByteArray): ByteArray? {
        ensureLoaded()
        if (xonlyPub.size != 32 || scalar.size != 32) return null
        val result = UByteArray(32)
        val ok = result.usePinned { r ->
            xonlyPub.asUByteArray().usePinned { pk ->
                scalar.asUByteArray().usePinned { sc ->
                    secp256k1c_ecdh_xonly(
                        r.addressOf(0), pk.addressOf(0), sc.addressOf(0),
                    )
                }
            }
        }
        return if (ok == 1) result.asByteArray() else null
    }

    actual fun sha256(data: ByteArray): ByteArray? {
        ensureLoaded()
        val out = UByteArray(32)
        out.usePinned { o ->
            data.asUByteArray().usePinned { d ->
                secp256k1c_sha256(o.addressOf(0), d.addressOf(0), data.size.convert())
            }
        }
        return out.asByteArray()
    }

    actual fun taggedHash(tag: String, msg: ByteArray): ByteArray? {
        ensureLoaded()
        val out = UByteArray(32)
        out.usePinned { o ->
            msg.asUByteArray().usePinned { m ->
                secp256k1c_tagged_hash(o.addressOf(0), tag, m.addressOf(0), msg.size.convert())
            }
        }
        return out.asByteArray()
    }
}
