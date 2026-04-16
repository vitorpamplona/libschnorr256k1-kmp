# schnorr256k1-kmp

Kotlin Multiplatform bindings for [libschnorr256k1](https://github.com/vitorpamplona/libschnorr256k1) — a high-performance secp256k1 library optimized for Nostr/BIP-340 workflows.

## Supported Platforms

| Platform | Targets | Mechanism |
|----------|---------|-----------|
| **Android** | arm64-v8a, x86_64 | JNI (shared library via CMake) |
| **JVM** | Linux x86_64, macOS arm64/x86_64 | JNI (shared library via CMake) |
| **iOS** | arm64, x64, simulatorArm64 | Kotlin/Native cinterop (static library) |
| **macOS** | arm64, x86_64 | Kotlin/Native cinterop (static library) |
| **Linux** | x86_64 | Kotlin/Native cinterop (static library) |
| **JS** | Browser, Node.js | WASM (Emscripten) |
| **Wasm** | Browser, Node.js | WASM (Emscripten) |

## Gradle Dependency

```kotlin
dependencies {
    implementation("com.vitorpamplona:schnorr256k1-kmp:1.0.0")
}
```

## API Reference

```kotlin
import com.vitorpamplona.schnorr256k1.Schnorr256k1

// Initialize (called automatically, but can be explicit)
Schnorr256k1.ensureLoaded()

// Key operations
val pubkey: ByteArray? = Schnorr256k1.pubkeyCreate(seckey)      // 65-byte uncompressed
val compressed: ByteArray? = Schnorr256k1.pubkeyCompress(pubkey) // 33-byte compressed
val valid: Boolean = Schnorr256k1.seckeyVerify(seckey)

// BIP-340 Schnorr signatures
val sig: ByteArray? = Schnorr256k1.schnorrSign(msg, seckey, auxrand)
val sigFast: ByteArray? = Schnorr256k1.schnorrSignXOnly(msg, seckey, xonlyPub, auxrand)
val ok: Boolean = Schnorr256k1.schnorrVerify(sig, msg, xonlyPub)
val okFast: Boolean = Schnorr256k1.schnorrVerifyFast(sig, msg, xonlyPub)
val batchOk: Boolean = Schnorr256k1.schnorrVerifyBatch(xonlyPub, sigs, msgs)

// Key derivation
val tweaked: ByteArray? = Schnorr256k1.privkeyTweakAdd(seckey, tweak)
val mulResult: ByteArray? = Schnorr256k1.pubkeyTweakMul(pubkey, tweak)

// ECDH
val shared: ByteArray? = Schnorr256k1.ecdhXOnly(xonlyPub, scalar)

// SHA-256
val hash: ByteArray? = Schnorr256k1.sha256(data)
val tagged: ByteArray? = Schnorr256k1.taggedHash("BIP0340/challenge", msg)
```

## Building from Source

### Prerequisites

- JDK 17+
- CMake 3.18+
- Android NDK (for Android targets)
- Xcode command-line tools (for iOS/macOS native targets)
- Emscripten SDK (for JS/Wasm targets)

### Clone with submodules

```bash
git clone --recursive https://github.com/vitorpamplona/libschnorr256k1-kmp.git
cd libschnorr256k1-kmp
```

### Build native libraries

```bash
# Desktop JVM (JNI shared library)
./scripts/build_native.sh

# Desktop + Android
./scripts/build_native.sh --android

# WASM (requires Emscripten)
./scripts/build_wasm.sh
```

### Run tests

```bash
# JVM tests
./gradlew :schnorr256k1:jvmTest

# Native tests (host platform)
./gradlew :schnorr256k1:linuxX64Test      # Linux
./gradlew :schnorr256k1:macosArm64Test     # macOS Apple Silicon
./gradlew :schnorr256k1:macosX64Test       # macOS Intel
```

## Platform-specific notes

### Native (iOS/macOS/Linux)

Native targets use Kotlin/Native's `cinterop` to generate bindings directly from
the C header. The C library is compiled as a static library and linked into the
klib. No JNI overhead — direct C function calls.

The static library is built automatically by Gradle using CMake. For cross-compilation
(e.g., iOS from macOS), CMake uses the appropriate system toolchain.

### JS / Wasm

JS and Wasm targets require the C library to be compiled to WebAssembly using
Emscripten. Run `./scripts/build_wasm.sh` to generate the WASM binary.

Before using the library, the WASM module must be loaded and the bridge must be
set up:

```javascript
import createSchnorr256k1 from './schnorr256k1_wasm.js';
import * as bridge from './schnorr256k1_bridge.mjs';

const module = await createSchnorr256k1();
bridge.setModule(module);
globalThis._schnorr256k1_bridge = bridge;
```

The `schnorr256k1_bridge.mjs` handles all WASM memory management (allocation,
byte copying, deallocation) and is included in the JS resources.

## Comparison Benchmark

The `bench/` directory contains a native C-to-C benchmark comparing libschnorr256k1
against ACINQ's libsecp256k1. Requires ACINQ's shared library:

```bash
cd bench
mkdir -p build && cd build
cmake .. -DACINQ_LIB_DIR=/path/to/acinq/lib
make
./bench_vs_acinq
```

## Project Structure

```
schnorr256k1-kmp/
├── libschnorr256k1/              # Git submodule (C library)
├── jni/
│   ├── CMakeLists.txt            # JNI shared library build
│   └── jni_bridge.c              # JNI bridge (C → JVM/Android)
├── bench/
│   ├── CMakeLists.txt            # Benchmark build
│   └── bench_vs_acinq.c          # Performance comparison
├── schnorr256k1/                 # Kotlin Multiplatform module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/           # expect declarations
│       ├── jvmMain/              # JVM actual (JNI)
│       ├── androidMain/          # Android actual (JNI)
│       ├── nativeMain/           # Native actual (cinterop)
│       ├── nativeInterop/cinterop/  # cinterop definition
│       ├── jsMain/               # JS actual (WASM bridge)
│       ├── wasmJsMain/           # Wasm actual (WASM bridge)
│       └── commonTest/           # Cross-platform tests
├── scripts/
│   ├── build_native.sh           # Build JNI + static libs
│   └── build_wasm.sh             # Build WASM via Emscripten
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

## License

MIT — see [LICENSE](LICENSE).
