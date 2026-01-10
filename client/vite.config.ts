import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 5173,
    open: false,  // Disable auto-open to avoid xdg-open errors in Docker/container environments
  },
  build: {
    lib: {
      entry: 'src/aem-assistant.ts',
      formats: ['es'],
    },
    rollupOptions: {
      external: /^lit/,
    },
  },
});
