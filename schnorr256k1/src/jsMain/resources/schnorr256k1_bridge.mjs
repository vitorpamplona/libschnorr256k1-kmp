/**
 * JavaScript bridge for schnorr256k1 WASM module.
 *
 * This module wraps the Emscripten-compiled WASM binary and provides
 * high-level functions that handle memory management (allocate, copy, free).
 *
 * Usage:
 *   import createModule from './schnorr256k1_wasm.mjs'; // Emscripten output
 *   import * as bridge from './schnorr256k1_bridge.mjs';
 *   const module = await createModule();
 *   bridge.setModule(module);
 *   bridge.init();
 *   const pub = bridge.pubkeyCreate(seckey);
 */

let M = null;

export function setModule(module) {
    M = module;
}

export function isLoaded() {
    return M !== null;
}

function writeBytes(ptr, bytes) {
    M.HEAPU8.set(bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes), ptr);
}

function readBytes(ptr, len) {
    return M.HEAPU8.slice(ptr, ptr + len);
}

function writeString(str) {
    const encoder = new TextEncoder();
    const bytes = encoder.encode(str);
    const ptr = M._malloc(bytes.length + 1);
    M.HEAPU8.set(bytes, ptr);
    M.HEAPU8[ptr + bytes.length] = 0;
    return { ptr, len: bytes.length };
}

export function init() {
    M._secp256k1c_init();
}

export function pubkeyCreate(seckey) {
    const skPtr = M._malloc(32);
    const pubPtr = M._malloc(65);
    writeBytes(skPtr, seckey);
    const ok = M._secp256k1c_pubkey_create(pubPtr, skPtr);
    const result = ok === 1 ? readBytes(pubPtr, 65) : null;
    M._free(skPtr);
    M._free(pubPtr);
    return result;
}

export function pubkeyCompress(pubkey) {
    const inPtr = M._malloc(65);
    const outPtr = M._malloc(33);
    writeBytes(inPtr, pubkey);
    const ok = M._secp256k1c_pubkey_compress(outPtr, inPtr);
    const result = ok === 1 ? readBytes(outPtr, 33) : null;
    M._free(inPtr);
    M._free(outPtr);
    return result;
}

export function seckeyVerify(seckey) {
    const ptr = M._malloc(32);
    writeBytes(ptr, seckey);
    const ok = M._secp256k1c_seckey_verify(ptr);
    M._free(ptr);
    return ok === 1;
}

export function schnorrSign(msg, seckey, auxrand) {
    const sigPtr = M._malloc(64);
    const skPtr = M._malloc(32);
    const msgPtr = M._malloc(msg.length);
    writeBytes(skPtr, seckey);
    writeBytes(msgPtr, msg);
    let auxPtr = 0;
    if (auxrand && auxrand.length === 32) {
        auxPtr = M._malloc(32);
        writeBytes(auxPtr, auxrand);
    }
    const ok = M._secp256k1c_schnorr_sign(sigPtr, msgPtr, msg.length, skPtr, auxPtr || 0);
    const result = ok === 1 ? readBytes(sigPtr, 64) : null;
    M._free(sigPtr);
    M._free(skPtr);
    M._free(msgPtr);
    if (auxPtr) M._free(auxPtr);
    return result;
}

export function schnorrSignXOnly(msg, seckey, xonlyPub, auxrand) {
    const sigPtr = M._malloc(64);
    const skPtr = M._malloc(32);
    const xpPtr = M._malloc(32);
    const msgPtr = M._malloc(msg.length);
    writeBytes(skPtr, seckey);
    writeBytes(xpPtr, xonlyPub);
    writeBytes(msgPtr, msg);
    let auxPtr = 0;
    if (auxrand && auxrand.length === 32) {
        auxPtr = M._malloc(32);
        writeBytes(auxPtr, auxrand);
    }
    const ok = M._secp256k1c_schnorr_sign_xonly(sigPtr, msgPtr, msg.length, skPtr, xpPtr, auxPtr || 0);
    const result = ok === 1 ? readBytes(sigPtr, 64) : null;
    M._free(sigPtr);
    M._free(skPtr);
    M._free(xpPtr);
    M._free(msgPtr);
    if (auxPtr) M._free(auxPtr);
    return result;
}

export function schnorrVerify(sig, msg, pub) {
    const sigPtr = M._malloc(64);
    const msgPtr = M._malloc(msg.length);
    const pubPtr = M._malloc(32);
    writeBytes(sigPtr, sig);
    writeBytes(msgPtr, msg);
    writeBytes(pubPtr, pub);
    const ok = M._secp256k1c_schnorr_verify(sigPtr, msgPtr, msg.length, pubPtr);
    M._free(sigPtr);
    M._free(msgPtr);
    M._free(pubPtr);
    return ok === 1;
}

export function schnorrVerifyFast(sig, msg, pub) {
    const sigPtr = M._malloc(64);
    const msgPtr = M._malloc(msg.length);
    const pubPtr = M._malloc(32);
    writeBytes(sigPtr, sig);
    writeBytes(msgPtr, msg);
    writeBytes(pubPtr, pub);
    const ok = M._secp256k1c_schnorr_verify_fast(sigPtr, msgPtr, msg.length, pubPtr);
    M._free(sigPtr);
    M._free(msgPtr);
    M._free(pubPtr);
    return ok === 1;
}

export function schnorrVerifyBatch(pub, sigs, msgs) {
    const count = sigs.length;
    if (count !== msgs.length) return false;
    if (count === 0) return true;

    const pubPtr = M._malloc(32);
    writeBytes(pubPtr, pub);

    const PTR_SIZE = 4;
    const sigsPtrArray = M._malloc(count * PTR_SIZE);
    const msgsPtrArray = M._malloc(count * PTR_SIZE);
    const lensPtr = M._malloc(count * PTR_SIZE);

    const sigPtrs = [];
    const msgPtrs = [];

    for (let i = 0; i < count; i++) {
        const sp = M._malloc(64);
        writeBytes(sp, sigs[i]);
        sigPtrs.push(sp);
        M.HEAPU32[(sigsPtrArray >> 2) + i] = sp;

        const mp = M._malloc(msgs[i].length);
        writeBytes(mp, msgs[i]);
        msgPtrs.push(mp);
        M.HEAPU32[(msgsPtrArray >> 2) + i] = mp;

        M.HEAPU32[(lensPtr >> 2) + i] = msgs[i].length;
    }

    const ok = M._secp256k1c_schnorr_verify_batch(pubPtr, sigsPtrArray, msgsPtrArray, lensPtr, count);

    M._free(pubPtr);
    M._free(sigsPtrArray);
    M._free(msgsPtrArray);
    M._free(lensPtr);
    sigPtrs.forEach(p => M._free(p));
    msgPtrs.forEach(p => M._free(p));

    return ok === 1;
}

export function privkeyTweakAdd(seckey, tweak) {
    const resultPtr = M._malloc(32);
    const skPtr = M._malloc(32);
    const twPtr = M._malloc(32);
    writeBytes(skPtr, seckey);
    writeBytes(twPtr, tweak);
    const ok = M._secp256k1c_privkey_tweak_add(resultPtr, skPtr, twPtr);
    const result = ok === 1 ? readBytes(resultPtr, 32) : null;
    M._free(resultPtr);
    M._free(skPtr);
    M._free(twPtr);
    return result;
}

export function pubkeyTweakMul(pubkey, tweak) {
    const outLen = pubkey.length;
    const resultPtr = M._malloc(outLen);
    const pkPtr = M._malloc(pubkey.length);
    const twPtr = M._malloc(32);
    writeBytes(pkPtr, pubkey);
    writeBytes(twPtr, tweak);
    const ok = M._secp256k1c_pubkey_tweak_mul(resultPtr, outLen, pkPtr, pubkey.length, twPtr);
    const result = ok === 1 ? readBytes(resultPtr, outLen) : null;
    M._free(resultPtr);
    M._free(pkPtr);
    M._free(twPtr);
    return result;
}

export function ecdhXOnly(xonlyPub, scalar) {
    const resultPtr = M._malloc(32);
    const pkPtr = M._malloc(32);
    const scPtr = M._malloc(32);
    writeBytes(pkPtr, xonlyPub);
    writeBytes(scPtr, scalar);
    const ok = M._secp256k1c_ecdh_xonly(resultPtr, pkPtr, scPtr);
    const result = ok === 1 ? readBytes(resultPtr, 32) : null;
    M._free(resultPtr);
    M._free(pkPtr);
    M._free(scPtr);
    return result;
}

export function sha256(data) {
    const outPtr = M._malloc(32);
    const dataPtr = M._malloc(data.length);
    writeBytes(dataPtr, data);
    M._secp256k1c_sha256(outPtr, dataPtr, data.length);
    const result = readBytes(outPtr, 32);
    M._free(outPtr);
    M._free(dataPtr);
    return result;
}

export function taggedHash(tag, msg) {
    const outPtr = M._malloc(32);
    const { ptr: tagPtr } = writeString(tag);
    const msgPtr = M._malloc(msg.length);
    writeBytes(msgPtr, msg);
    M._secp256k1c_tagged_hash(outPtr, tagPtr, msgPtr, msg.length);
    const result = readBytes(outPtr, 32);
    M._free(outPtr);
    M._free(tagPtr);
    M._free(msgPtr);
    return result;
}
