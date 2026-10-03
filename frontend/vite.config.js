import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// While you work (npm run dev) the page is served on port 5173 and every URL that starts with
// /api is forwarded to the Java backend on port 8080. The browser therefore sees one origin, so
// no CORS configuration is needed on the backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    css: false,
  },
});
