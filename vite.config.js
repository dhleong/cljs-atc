import { defineConfig } from "vite";
// import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react-swc";

export default defineConfig({
  plugins: [react(),
    {
      name: "shadow-cljs-compat",
      handleHotUpdate({file, server, modules}) {
        // NOTE: shadow-cljs writes some files even if its contents haven't
        // changed. We don't want to *ignore* it, as vite will fail to
        // update its cache otherwise, but we also don't want to trigger any
        // changes (at this time) as that will cause a full page reload for
        // things that shadow-cljs can handle.
        // This means that if you add a new import dependency, you'll have to
        // refresh the page manually. That's not something super common, so...
        // I'm fine with it. If we can figure out how to make this work
        // "correctly" then that'd be nice, but I don't want to waste more time
        // NOTE: Similarly, we can't "just" ignore cljs-runtime files etc., as
        // changes will be "lost" on refresh due to vite's caching.
        // FIXME: We should load this list from the :external-index and
        // :modules from shadow-cljs.edn
        const ignoredFiles = ["imports.js", "main.js", "speech.enhanced.js", "airports-kjfk.js"];
        const ignoredFilePaths = new Set(ignoredFiles.map((file) => `${server.config.root}/js/${file}`));
        if (ignoredFilePaths.has(file) || file.includes("/cljs-runtime/")) {
          return [];
        }
        return modules;
      }
    }
  ],
  root: "public",

  server: {
    port: 8080,
    watch: {
      // Exclude various files so changes dont trigger multiple reloads
      ignored: [
        "**/*.cljs",
        "**/*.edn",
        "module-loader.json",
      ],
    },
  },
})
