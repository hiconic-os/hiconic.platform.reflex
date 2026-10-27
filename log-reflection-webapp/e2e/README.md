# Log Reflection browser tests

The tests run the real Solid UI against a deterministic in-browser `LogReflectionClient` implementation. They do not use a personal browser profile or a running Reflex server.

```shell
npm ci
npm run test:e2e:install
npm run test:e2e
```

Use `npm run test:e2e:headed` only when a visible, isolated Playwright browser is useful for diagnosis.
