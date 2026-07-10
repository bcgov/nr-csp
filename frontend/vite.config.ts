import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';
import { VitePWA } from 'vite-plugin-pwa';
import path from 'path';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), ['VITE_', 'REACT_APP_']);
  const publicUrl = env.VITE_PUBLIC_URL ?? '/';

  return {
    plugins: [
      react(),
      tsconfigPaths(),
      VitePWA({
        registerType: 'autoUpdate',
        workbox: {
          maximumFileSizeToCacheInBytes: 6 * 1024 * 1024,
          // Prevent the service worker from intercepting /api/* navigation
          // requests (e.g. Swagger UI). Without this, Workbox applies the
          // navigation fallback and serves the React shell on first load.
          navigateFallbackDenylist: [/^\/api\//],
        },
        manifest: {
          name: 'NR CSP',
          short_name: 'NR CSP',
          theme_color: '#ffffff',
          icons: [
            { src: 'icons/android-chrome-192x192.png', sizes: '192x192', type: 'image/png' },
            { src: 'icons/android-chrome-512x512.png', sizes: '512x512', type: 'image/png' },
          ],
        },
      }),
    ],
    base: publicUrl,
    resolve: {
      alias: { '@': path.resolve(__dirname, 'src') },
    },
    server: {
      host: true,
      port: 3000,
      watch: {
        usePolling: env.VITE_USE_POLLING === 'true',
      },
      proxy: {
        '/api': {
          target: env.REACT_APP_API_TARGET ?? 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: 'build',
    },
    css: {
      preprocessorOptions: {
        scss: { quietDeps: true },
      },
    },
    test: {
      globals: true,
      // happy-dom is the installed DOM environment (jsdom is not a dependency);
      // the node project overrides this anyway and the browser project uses
      // real Chromium, so this only serves as the default.
      environment: 'happy-dom',
      setupFiles: './src/test-setup.ts',
      coverage: {
        provider: 'v8',
        reportsDirectory: './coverage',
        reporter: ['text', 'json', 'html', 'clover', 'lcov'],
        // Only measure source files; without this, compiled output in build/
        // and static scripts in public/ show up as 0%-covered files.
        include: ['src/**'],
      },
      projects: [
        {
          extends: true,
          test: {
            name: 'node',
            environment: 'happy-dom',
            include: ['src/**/*.unit.test.{ts,tsx}'],
            setupFiles: ['src/config/tests/setup-env.ts'],
          },
        },
        // Browser project is opt-in: set VITEST_BROWSER_ENABLED=true to include it.
        // Without this guard, Vitest initialises the Playwright provider at startup
        // even when --project node is passed, which triggers a Chromium download.
        ...(process.env.VITEST_BROWSER_ENABLED === 'true'
          ? [
              {
                extends: true,
                test: {
                  name: 'browser',
                  browser: {
                    enabled: true,
                    provider: 'playwright',
                    // PLAYWRIGHT_CHANNEL lets environments without a bundled Chromium
                    // (e.g. WSL/Ubuntu releases Playwright doesn't support yet) fall back
                    // to a system-installed browser, e.g. `PLAYWRIGHT_CHANNEL=chrome`.
                    instances: [
                      {
                        browser: 'chromium',
                        launch: { channel: process.env.PLAYWRIGHT_CHANNEL || undefined },
                      },
                    ],
                  },
                  include: ['src/**/*.browser.test.{ts,tsx}'],
                  setupFiles: ['src/config/tests/setup-browser.ts'],
                },
              },
            ]
          : []),
      ],
    },
  };
});
