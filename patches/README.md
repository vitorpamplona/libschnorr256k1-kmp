# libschnorr256k1 SHA-NI runtime-dispatch patch

This directory carries the upstream `libschnorr256k1` change required by the
kmp branch `claude/fix-schnorr-sha-ni-compat-QwRoy`. It exists because the
sandbox that produced the fix has push access only to `libschnorr256k1-kmp`,
not to the `libschnorr256k1` submodule remote.

The kmp commit on this branch already references submodule SHA `0a02dcf`.
Until that SHA is reachable on `github.com/vitorpamplona/libschnorr256k1`,
`git submodule update --init` will fail for anyone consuming the branch.

## Publishing the submodule commit (recommended: bundle)

From a local clone of `libschnorr256k1-kmp` on a machine with GitHub push
credentials for `vitorpamplona/libschnorr256k1`:

```bash
git checkout claude/fix-schnorr-sha-ni-compat-QwRoy
git submodule update --init --recursive   # pulls the existing 143339c base

cd libschnorr256k1
git fetch ../patches/libschnorr256k1-shani-dispatch.bundle \
    claude/fix-shani-runtime-dispatch:claude/fix-shani-runtime-dispatch
git push origin claude/fix-shani-runtime-dispatch
```

After the push the kmp branch's submodule pointer resolves cleanly. Open a
PR on `vitorpamplona/libschnorr256k1` from `claude/fix-shani-runtime-dispatch`
and merge it whenever you're ready; the commit SHA stays valid either way.

## Alternative: apply as a patch

If you'd rather create a fresh commit (e.g. to re-author or rebase onto a
newer base), use the `.patch` file instead of the bundle:

```bash
cd libschnorr256k1
git checkout -b shani-dispatch     # branch off whatever upstream HEAD you want
git am ../patches/libschnorr256k1-shani-dispatch.patch
git push origin shani-dispatch
# then in the parent repo:
cd ..
git add libschnorr256k1
git commit --amend            # or a fresh commit, updating the pointer
git push --force-with-lease origin claude/fix-schnorr-sha-ni-compat-QwRoy
```

## What the change does

Moves the SHA-NI / ARMv8 Crypto Extensions intrinsics into per-arch
translation units (`src/sha256_hw_x86.c`, `src/sha256_hw_arm.c`) compiled
with their architecture flags scoped to those files. `sha256.c` resolves
the transform once at load time via `__builtin_cpu_supports("sha")` on
x86_64 and `getauxval(AT_HWCAP) & HWCAP_SHA2` on aarch64-Linux.

Performance impact when SHA-NI is present: one indirect call per 64-byte
block (predicted, below benchmark noise).

Verified on a Xeon without `sha_ni`: `ALL 103 TESTS PASSED`, the JNI `.so`
loads and exercises tagged hashes without SIGILL, SHA-NI opcodes in the
binary stay confined to `sha256_transform_shani`.
