import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import solid from "vite-plugin-solid";
import { viteStaticCopy } from "vite-plugin-static-copy";

const here = fileURLToPath(new URL(".", import.meta.url));
const fromHere = (relative: string) => path.resolve(here, relative);

export default defineConfig(({ command }) => {
  const development = command === "serve";
  const runtimePackage = development ? "@dev.hiconic/runtime-dev" : "@dev.hiconic/runtime";
  const runtimeFile = fromHere(`node_modules/${runtimePackage}/dist/tf-js.js`);

  return {
    base: "./",
    plugins: [
      solid(),
      viteStaticCopy({ targets: [{ src: runtimeFile, dest: "assets" }] })
    ],
    resolve: {
      alias: {
        "@dev.hiconic/runtime": runtimeFile,
        "@dev.hiconic/runtime-dev": runtimeFile,
        "@dev.hiconic/hc-js-base": fromHere("node_modules/@dev.hiconic/hc-js-base/dist/hc-js-base.js"),
        "@dev.hiconic/gm_gm-core-api": fromHere("node_modules/@dev.hiconic/gm_gm-core-api/dist/gm-core-api.js"),
        "@dev.hiconic/gm_root-model": fromHere("node_modules/@dev.hiconic/gm_root-model/dist/root-model.js"),
        "@dev.hiconic/gm_service-api-model": fromHere("node_modules/@dev.hiconic/gm_service-api-model/dist/service-api-model.js"),
        "@dev.hiconic/gm_logging-model": fromHere("node_modules/@dev.hiconic/gm_logging-model/dist/logging-model.js"),
        "@dev.hiconic/gm_resource-model": fromHere("node_modules/@dev.hiconic/gm_resource-model/dist/resource-model.js"),
        "@dev.hiconic/gm_transient-resource-model": fromHere("node_modules/@dev.hiconic/gm_transient-resource-model/dist/transient-resource-model.js")
      }
    },
    server: {
      port: 5175,
      strictPort: true
    },
    build: {
      outDir: "build/web",
      emptyOutDir: true,
      target: "esnext",
      modulePreload: { polyfill: false },
      rollupOptions: {
        external: ["@dev.hiconic/runtime", "@dev.hiconic/runtime-dev"],
        output: {
          paths: {
            // The importing entry chunk itself lives in assets/, so this path is relative to that directory.
            "@dev.hiconic/runtime": "./tf-js.js",
            "@dev.hiconic/runtime-dev": "./tf-js.js"
          }
        }
      }
    },
    optimizeDeps: {
      exclude: ["@dev.hiconic/runtime", "@dev.hiconic/runtime-dev"]
    }
  };
});
