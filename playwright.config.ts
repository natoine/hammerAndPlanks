import { defineConfig, devices } from '@playwright/test';

// Tests d'interface de la version web (TeaVM). Prérequis : npm run web:build
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: true,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:8080',
    // WebGL logiciel pour les navigateurs sans GPU (CI, headless).
    // Caméra simulée par Chromium (mire animée) : pas de vraie webcam ni de fenêtre d'autorisation en test.
    launchOptions: {
      args: [
        '--use-angle=swiftshader',
        '--enable-unsafe-swiftshader',
        '--use-fake-device-for-media-stream',
        '--use-fake-ui-for-media-stream',
      ],
    },
    permissions: ['camera'],
  },
  projects: [
    { name: 'desktop-chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chromium', use: { ...devices['Pixel 7'] } },
  ],
  webServer: {
    // Serveur avec en-têtes COOP/COEP : ONNX Runtime Web peut utiliser plusieurs threads.
    command: 'node scripts/serve-web.mjs platform/web/build/dist/webapp 8080',
    url: 'http://127.0.0.1:8080',
    reuseExistingServer: true,
  },
});
