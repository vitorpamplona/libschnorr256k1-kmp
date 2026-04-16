# schnorr256k1-kmp

Kotlin Multiplatform JNI bindings for [libschnorr256k1](https://github.com/vitorpamplona/libschnorr256k1) — a high-performance secp256k1 library optimized for Nostr/BIP-340 workflows.

## Supported Platforms

- **Android**: arm64-v8a, x86_64
- **JVM**: Linux x86_64, macOS arm64/x86_64

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

### Clone with submodules

```bash
git clone --recursive https://github.com/vitorpamplona/libschnorr256k1-kmp.git
cd libschnorr256k1-kmp
```

### Build native library + Kotlin module

```bash
./scripts/build_native.sh        # Desktop JVM only
./scripts/build_native.sh --android  # Desktop + Android
```

### Run tests

```bash
./gradlew :schnorr256k1:jvmTest
```

## Comparison Benchmark

The `bench/` directory contains a native C-to-C benchmark comparing libschnorr256k1
against ACINQ's libsecp256k1. This requires ACINQ's shared library to be available:

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
├── libschnorr256k1/          # Git submodule (C library)
├── jni/
│   ├── CMakeLists.txt        # Builds JNI shared library
│   └── jni_bridge.c          # JNI bridge (C → Kotlin)
├── bench/
│   ├── CMakeLists.txt        # Benchmark build
│   └── bench_vs_acinq.c      # Performance comparison
├── schnorr256k1/             # Kotlin Multiplatform module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/       # expect declarations
│       ├── jvmMain/          # JVM actual (JNI)
│       ├── androidMain/      # Android actual (JNI)
│       └── commonTest/       # Cross-platform tests
├── build.gradle.kts          # Root build
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

## License

MIT — see [LICENSE](LICENSE).
