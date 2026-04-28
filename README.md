# schnorr256k1-kmp

Kotlin Multiplatform bindings for [libschnorr256k1](https://github.com/vitorpamplona/libschnorr256k1) — a high-performance secp256k1 library optimized for Nostr/BIP-340 workflows.

## Supported Platforms

| Platform | Targets | Mechanism |
|----------|---------|-----------|
| **Android** | arm64-v8a, x86_64 | JNI (shared library via CMake) |
| **JVM** | Linux x86_64/aarch64, macOS arm64/x86_64 | JNI (per-OS/arch JAR resource, auto-extracted by `NativeLoader`) |
| **iOS** | arm64, x64, simulatorArm64 | Kotlin/Native cinterop (static library) |
| **macOS** | arm64, x86_64 | Kotlin/Native cinterop (static library) |
| **Linux** | x86_64 | Kotlin/Native cinterop (static library) |
| **JS** | Browser, Node.js | WASM (Emscripten) |
| **Wasm** | Browser, Node.js | WASM (Emscripten) |

## Gradle Dependency

```kotlin
dependencies {
    implementation("com.vitorpamplona.schnorr256k1:schnorr256k1-kmp:1.0.3")
}
```

For JVM consumers, the right native binary is resolved automatically — the
`schnorr256k1-kmp-jvm` artifact pulls in four classifier-style siblings that
each ship a prebuilt `libschnorr256k1_jni.{so,dylib}` as a classpath resource:

- `com.vitorpamplona:schnorr256k1-kmp-jni-jvm-linux-x86_64`
- `com.vitorpamplona:schnorr256k1-kmp-jni-jvm-linux-aarch64`
- `com.vitorpamplona:schnorr256k1-kmp-jni-jvm-darwin-x86_64`
- `com.vitorpamplona:schnorr256k1-kmp-jni-jvm-darwin-aarch64`

`NativeLoader` (in the JVM source set) detects `os.name`/`os.arch` at runtime,
extracts the matching binary from the classpath to a temp directory, and
`System.load`s it. No `java.library.path` setup or manual `build_native.sh`
step is required for downstream JVM consumers — the JVM cross-validation
benchmark also runs out of the box.

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

The desktop JVM JNI shared library is built automatically by Gradle when you
run `:schnorr256k1:jvmTest` (via the host's `:jni-jvm-{os}-{arch}` subproject).
The script below is only needed if you want a standalone CMake build outside
the Gradle flow.

```bash
# Desktop JVM (JNI shared library) — optional, Gradle does this on demand
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

### Current results

Linux x86_64, GCC 13.3.0 with LTO + BMI2/ADX, vs `libsecp256k1-jni.so` from
ACINQ `secp256k1-kmp-jni-jvm-linux` 0.18.0. Lower µs is better.

| Operation                     | ACINQ (µs) | Ours (µs) | Speedup |
|-------------------------------|-----------:|----------:|--------:|
| pubkeyCreate                  |       19.8 |      15.1 |   1.31x |
| sign (full, derive pubkey)    |       38.9 |      31.5 |   1.24x |
| sign (cached pubkey)          |       19.9 |      15.4 |   1.30x |
| verify (BIP-340)              |       38.1 |      40.1 |   0.95x |
| verifyFast (Nostr safe)       |       38.1 |      35.0 |   1.09x |
| ECDH (cached pubkey)          |       38.0 |      33.1 |   1.15x |

Batch verify scales sub-linearly — our batched API vs ACINQ's per-signature verify:

| Batch size | ACINQ per-sig (µs) | Ours batched (µs) | Speedup |
|-----------:|-------------------:|------------------:|--------:|
|          4 |               38.0 |              13.7 |    2.8x |
|          8 |               37.2 |               9.7 |    3.8x |
|         16 |               37.6 |               7.1 |    5.3x |
|         32 |               37.5 |               6.5 |    5.8x |
|         64 |               38.1 |               5.5 |    7.0x |
|        200 |               39.4 |               4.9 |    8.1x |

Cross-verification passes in both directions (signatures produced by each
library verify under the other).

## Project Structure

```
schnorr256k1-kmp/
├── libschnorr256k1/              # Git submodule (C library)
├── jni/
│   ├── CMakeLists.txt            # JNI shared library build
│   ├── jni_bridge.c              # JNI bridge (C → JVM/Android)
│   └── jvm/                      # Per-OS/arch JNI publication subprojects
│       ├── linux-x86_64/build.gradle.kts
│       ├── linux-aarch64/build.gradle.kts
│       ├── darwin-x86_64/build.gradle.kts
│       └── darwin-aarch64/build.gradle.kts
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

## Publishing to Maven Central

Releases are published to the [Sonatype Central Portal](https://central.sonatype.com/)
via the [Vanniktech Maven Publish plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/).

### Required credentials

Configure the following in `~/.gradle/gradle.properties` (or via environment
variables / CI secrets):

```properties
# Sonatype Central Portal user token (https://central.sonatype.com/account)
mavenCentralUsername=<central-portal-token-username>
mavenCentralPassword=<central-portal-token-password>

# GPG signing key (ASCII-armored, single-line with \n escapes, or use in-memory form)
signing.keyId=12345678
signing.password=some_password
signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
```

Equivalent environment variables (for CI):

```
ORG_GRADLE_PROJECT_mavenCentralUsername=username
ORG_GRADLE_PROJECT_mavenCentralPassword=the_password

# see below for how to obtain this
ORG_GRADLE_PROJECT_signingInMemoryKey=exported_ascii_armored_key
# Optional
ORG_GRADLE_PROJECT_signingInMemoryKeyId=12345678
# If key was created with a password.
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=some_password
```

### Dry run locally

```bash
./gradlew :schnorr256k1:publishToMavenLocal
```

### Release to Maven Central

The recommended path is the [`.github/workflows/release.yml`](.github/workflows/release.yml)
GitHub Actions workflow. Push a `v*` tag (or trigger it manually) and it
publishes every artifact for that version in one go:

| Runner          | Artifacts                                                                                                              |
|-----------------|------------------------------------------------------------------------------------------------------------------------|
| `ubuntu-latest` | `:jni-jvm-linux-x86_64` (native), `:jni-jvm-linux-aarch64` (cross via `aarch64-linux-gnu-gcc`)                          |
| `macos-14`      | `:jni-jvm-darwin-aarch64` (native), `:jni-jvm-darwin-x86_64` (cross via `-DCMAKE_OSX_ARCHITECTURES=x86_64`), `:schnorr256k1` (Android/iOS/macOS arm64/JVM/JS/Wasm) |

Each `:jni-jvm-*` coordinate is published from exactly one runner so Maven
Central's immutability never bites. Required repository secrets:

- `MAVEN_CENTRAL_USERNAME` — Central Portal user-token username
- `MAVEN_CENTRAL_PASSWORD` — Central Portal user-token password
- `SIGNING_IN_MEMORY_KEY` — ASCII-armored GPG private key (full block)
- `SIGNING_IN_MEMORY_KEY_PASSWORD` — passphrase for the GPG key (omit if none)

Manual / local publish (skips the JNI subprojects, which is what produced the
broken 1.0.1 release — prefer the workflow):

```bash
# Uploads and releases the multiplatform module only.
./gradlew :schnorr256k1:publishAndReleaseToMavenCentral
```

Each `:jni-jvm-*` subproject's `buildJniLibrary` is gated by
`onlyIf { canBuildHere }`, so running it from the wrong host produces an empty
JAR. The workflow above sidesteps this by routing each classifier to a runner
that can produce a real binary.

## License

MIT — see [LICENSE](LICENSE).
