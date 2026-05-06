# Submodule patches

This directory carries libschnorr256k1 commits that the sandbox which
authored them could not push directly (the proxy here forwards only
`libschnorr256k1-kmp`). Replay them on a machine with push credentials
for `vitorpamplona/libschnorr256k1` so the submodule pointer in this
repo resolves on fresh clones.

## libschnorr256k1-applclang-cpuid

Submodule commit `4e6751eccd46c5433e280aa491d4a3257289c010` — fixes the
macOS x86_64 cross-compile failure where AppleClang 15 rejected
`__builtin_cpu_supports("sha")` with *"invalid cpu feature string for
builtin"*. Replaces it with direct CPUID reads via `<cpuid.h>`.

Replay (run from this repo's root):

```sh
# 1. Push the new submodule commit upstream.
cd libschnorr256k1
git fetch ../patches/libschnorr256k1-applclang-cpuid.bundle fix-applclang-cpuid-shani:fix-applclang-cpuid-shani
git push origin fix-applclang-cpuid-shani
# Open a PR against libschnorr256k1 main and merge.

# 2. After the merge SHA lands on libschnorr256k1 main, bump this repo's
#    submodule pointer to it and drop this patches/ entry.
```

If you'd rather apply the patch on top of an existing checkout instead
of fetching the bundle, `libschnorr256k1-applclang-cpuid.patch` is the
same change as a `git format-patch` file.
