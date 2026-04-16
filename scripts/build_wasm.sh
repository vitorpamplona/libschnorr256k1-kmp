#!/usr/bin/env bash
#
# Build schnorr256k1 as a WebAssembly module using Emscripten.
#
# Prerequisites:
#   - Emscripten SDK (emsdk) installed and activated
#   - Run: source /path/to/emsdk/emsdk_env.sh
#
# Usage: ./scripts/build_wasm.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LIB_DIR="$PROJECT_DIR/libschnorr256k1"
OUTPUT_DIR="$PROJECT_DIR/build/wasm"

if ! command -v emcc &> /dev/null; then
    echo "ERROR: emcc (Emscripten) not found. Install emsdk and run 'source emsdk_env.sh'."
    exit 1
fi

echo "=== Building schnorr256k1 WASM module ==="

mkdir -p "$OUTPUT_DIR"

SOURCES=(
    "$LIB_DIR/src/field.c"
    "$LIB_DIR/src/scalar.c"
    "$LIB_DIR/src/point.c"
    "$LIB_DIR/src/schnorr.c"
    "$LIB_DIR/src/sha256.c"
)

EXPORTED_FUNCTIONS='[
    "_secp256k1c_init",
    "_secp256k1c_pubkey_create",
    "_secp256k1c_pubkey_compress",
    "_secp256k1c_seckey_verify",
    "_secp256k1c_schnorr_sign",
    "_secp256k1c_schnorr_sign_xonly",
    "_secp256k1c_schnorr_verify",
    "_secp256k1c_schnorr_verify_fast",
    "_secp256k1c_schnorr_verify_batch",
    "_secp256k1c_privkey_tweak_add",
    "_secp256k1c_pubkey_tweak_mul",
    "_secp256k1c_ecdh_xonly",
    "_secp256k1c_sha256",
    "_secp256k1c_tagged_hash",
    "_malloc",
    "_free"
]'

emcc "${SOURCES[@]}" \
    -I "$LIB_DIR/include" \
    -I "$LIB_DIR/src" \
    -O3 \
    -s MODULARIZE=1 \
    -s EXPORT_NAME="createSchnorr256k1" \
    -s EXPORTED_FUNCTIONS="$EXPORTED_FUNCTIONS" \
    -s EXPORTED_RUNTIME_METHODS='["HEAPU8","HEAPU32"]' \
    -s ALLOW_MEMORY_GROWTH=1 \
    -s INITIAL_MEMORY=1048576 \
    -s ENVIRONMENT='web,node' \
    -s FILESYSTEM=0 \
    -s SINGLE_FILE=0 \
    -o "$OUTPUT_DIR/schnorr256k1_wasm.js"

echo ""
echo "=== WASM build complete ==="
echo "Output files:"
ls -la "$OUTPUT_DIR"/schnorr256k1_wasm.*
echo ""
echo "To use with Kotlin/JS or Kotlin/wasmJs:"
echo "  1. Copy schnorr256k1_wasm.js and schnorr256k1_wasm.wasm to your project"
echo "  2. Load the module and set globalThis._schnorr256k1_bridge"
echo "  3. See schnorr256k1_bridge.mjs for the bridge API"
