# libschnorr256k1-kmp

Kotlin Multiplatform bindings for [libschnorr256k1](https://github.com/vitorpamplona/libschnorr256k1),
the C BIP-340 Schnorr library on secp256k1, which is a git submodule here (`libschnorr256k1/`). See `README.md`.

## Build and test

```bash
git submodule update --init --recursive
./gradlew :schnorr256k1:jvmTest
./gradlew :schnorr256k1:linuxX64Test      # macOS: macosArm64Test / macosX64Test
```

## Remotes and pull requests

The repo is on GitHub, `github.com/vitorpamplona/libschnorr256k1-kmp`, which holds the canonical
`main`, and on nostr as a NIP-34 repository,
`nostr://npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/libschnorr256k1-kmp`:
a push to that remote goes to its GRASP git servers and to GitHub. A PR can go out either way: a
GitHub PR (`gh`), or a nostr proposal (`ngit`, a `pr/…` branch, reviewed on
[gitworkshop.dev](https://gitworkshop.dev/npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/libschnorr256k1-kmp)).
Remote names vary per clone, so identify them by URL. Open a PR only when the human you work for
asks, and before opening, revising or merging one by either path, use the `ngit-pr` skill.
