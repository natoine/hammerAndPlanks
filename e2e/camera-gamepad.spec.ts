import { test, expect, Page } from '@playwright/test';

test.use({ locale: 'fr-FR' });

const status = (page: Page) => page.locator('#input-mode');
const cameraStatus = (page: Page) => page.evaluate(() => (window as any).hapCamera.status() as string);

async function open(page: Page) {
  await page.goto('/');
  await expect(status(page)).toHaveText(/Mode d'interaction : /, { timeout: 30_000 });
}

/** Passe au mode caméra depuis le clavier (clavier -> manette -> caméra). */
async function toCamera(page: Page) {
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await expect(status(page)).toHaveText("Mode d'interaction : Caméra");
}

test.describe('manette', () => {
  test('sans manette branchée, le mode manette est signalé comme non détecté', async ({ page }) => {
    await open(page);
    await page.keyboard.press('Tab');
    await expect(status(page)).toHaveText("Mode d'interaction : Manette non détectée");
  });

  test('avec une manette branchée, le mode manette est normal', async ({ page }) => {
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'getGamepads', { value: () => [{ connected: true, id: 'Manette de test' }] });
    });
    await open(page);
    await page.keyboard.press('Tab');
    await expect(status(page)).toHaveText("Mode d'interaction : Manette");
  });
});

test.describe('caméra', () => {
  test('la caméra reste éteinte tant que le mode caméra n’est pas choisi', async ({ page }) => {
    await open(page);
    await page.waitForTimeout(1000);
    expect(await cameraStatus(page)).toBe('OFF');
    await expect(page.locator('#camera-thumbnail')).toHaveCount(0);
  });

  test('en mode caméra, l’imagette s’affiche en bas à droite avec le flux', async ({ page }) => {
    await open(page);
    await toCamera(page);
    await expect.poll(() => cameraStatus(page), { timeout: 15_000 }).toBe('LIVE');

    const video = page.locator('#camera-thumbnail');
    await expect(video).toBeVisible();
    expect(await video.evaluate((v: HTMLVideoElement) => v.readyState)).toBeGreaterThanOrEqual(2);

    const box = (await video.boundingBox())!;
    const { width, height } = page.viewportSize()!;
    expect(box.x).toBeGreaterThan(width / 2);
    expect(box.y).toBeGreaterThan(height / 2);
    expect(box.x + box.width).toBeLessThanOrEqual(width);
    expect(box.y + box.height).toBeLessThanOrEqual(height);
  });

  test('quitter le mode caméra coupe le flux et masque l’imagette', async ({ page }) => {
    await open(page);
    await toCamera(page);
    await expect.poll(() => cameraStatus(page), { timeout: 15_000 }).toBe('LIVE');
    await page.keyboard.press('Tab');
    await expect(status(page)).toHaveText("Mode d'interaction : Clavier (flèches)");
    await expect.poll(() => cameraStatus(page)).toBe('OFF');
    await expect(page.locator('#camera-thumbnail')).toBeHidden();
    const tracksLive = await page.evaluate(() => {
      const v = document.getElementById('camera-thumbnail') as HTMLVideoElement;
      return v.srcObject !== null;
    });
    expect(tracksLive).toBe(false);
  });

  test('accès refusé : pas d’imagette vidéo, état refusé', async ({ page }) => {
    await page.addInitScript(() => {
      navigator.mediaDevices.getUserMedia = () => Promise.reject(new DOMException('refus', 'NotAllowedError'));
    });
    await open(page);
    await toCamera(page);
    await expect.poll(() => cameraStatus(page)).toBe('DENIED');
    await expect(page.locator('#camera-thumbnail')).toBeHidden();
  });

  test('navigateur sans caméra : état indisponible', async ({ page }) => {
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'mediaDevices', { value: undefined });
    });
    await open(page);
    await toCamera(page);
    await expect.poll(() => cameraStatus(page)).toBe('UNAVAILABLE');
  });
});
