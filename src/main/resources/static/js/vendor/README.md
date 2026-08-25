# Vendored dependencies

- `jsQR.min.js` — [jsQR](https://github.com/cozmo/jsQR) v1.4.0, Apache-2.0 license.
  Used as a pure-JS QR decode fallback for browsers without the native
  `BarcodeDetector` API (notably iOS/Safari), so camera-based voucher
  scanning works across phones. Vendored locally rather than loaded from a
  CDN so it also works offline via the service worker.
