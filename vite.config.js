import { defineConfig } from "vite";
// import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react-swc";

export default defineConfig({
  plugins: [react()],
  root: "public",

  server: {
    port: 8080,
    watch: {
      // Exclude .cljs files
      // so changes dont trigger multiple reloads
      ignored: "**/*.cljs",
    },
  },
})
