// Loads the WASM module and the JS bridge, then registers a Mocha
// `before(done)` hook that completes async setup before tests run.
//
// The Kotlin/JS implementation in Schnorr256k1.js.kt looks up
// globalThis._schnorr256k1_bridge in ensureLoaded(); this hook installs it.

const factory = require('./schnorr256k1_wasm.js');

before(function (done) {
    this.timeout(30000);
    Promise.all([
        factory(),
        import('./schnorr256k1_bridge.mjs'),
    ])
        .then(function (results) {
            const wasmModule = results[0];
            const bridge = results[1];
            bridge.setModule(wasmModule);
            globalThis._schnorr256k1_bridge = bridge;
            done();
        })
        .catch(done);
});
