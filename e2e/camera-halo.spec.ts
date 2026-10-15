import { test, expect, Page } from '@playwright/test';

// Halo autour du joueur suivi dans le retour caméra. Caméra simulée par Chromium ; pose synthétique :
// buste centré dans une image 640x480 (épaules de 200 px), pour que le halo ait une forme à entourer.
test.use({ locale: 'fr-FR' });

async function syntheticPose(page: Page) {
  await page.addInitScript(() => {
    const j2 = new Float32Array(140).fill(320);
    const put = (i: number, x: number, y: number) => { j2[2 * i] = x; j2[2 * i + 1] = y; };
    put(6, 220, 300); put(5, 420, 300);   // épaules (droite, gauche)
    put(8, 200, 400); put(7, 440, 400);   // coudes
    put(41, 220, 470); put(62, 420, 470); // poignets
    put(10, 280, 520); put(9, 360, 520);  // hanches
    put(0, 320, 180); put(4, 280, 170); put(3, 360, 170); // nez, oreilles
    let running = false;
    (window as any).hapPose = {
      status: () => (running ? 'RUNNING' : 'OFF'),
      now: () => performance.now() / 1000,
      start: () => { running = true; },
      stop: () => { running = false; },
      time: () => (running ? Math.floor(performance.now() / 50) * 0.05 : -1),
      joint3d: (i: number) => (i === 15 ? 0.2 : i === 18 ? -0.2 : 0), // épaules à plat : pas d'inclinaison
      joint2d: (i: number) => j2[i],
      width: () => 640, height: () => 480, inferenceMs: () => 1,
    };
  });
}

/** Pixels du halo : nombre de pixels visibles et couleur moyenne. */
const haloPixels = (page: Page) => page.evaluate(() => {
  const c = document.getElementById('camera-halo') as HTMLCanvasElement | null;
  if (!c || c.width === 0) return { count: 0, r: 0, g: 0, b: 0, center: 0 };
  const d = c.getContext('2d')!.getImageData(0, 0, c.width, c.height).data;
  let count = 0, r = 0, g = 0, b = 0;
  for (let i = 0; i < d.length; i += 4) if (d[i + 3] > 20) { count++; r += d[i]; g += d[i + 1]; b += d[i + 2]; }
  // Centre de la tête (320, 180 dans l'image 640x480), ramené à l'imagette en recadrage « cover ».
  const scale = Math.max(c.width / 640, c.height / 480);
  const cx = Math.round((c.width - 640 * scale) / 2 + 320 * scale), cy = Math.round((c.height - 480 * scale) / 2 + 180 * scale);
  const center = d[(cy * c.width + cx) * 4 + 3];
  return { count, r: r / Math.max(1, count), g: g / Math.max(1, count), b: b / Math.max(1, count), center };
});

async function toCamera(page: Page) {
  await page.goto('/');
  await expect(page.locator('#input-mode')).toHaveText(/Mode d'interaction : /, { timeout: 30_000 });
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await expect(page.locator('#input-mode')).toHaveText("Mode d'interaction : Caméra");
}

test('un halo doré entoure la silhouette du joueur suivi', async ({ page }) => {
  await syntheticPose(page);
  await toCamera(page);
  await expect(page.locator('#camera-halo')).toBeVisible({ timeout: 15_000 });
  await expect.poll(async () => (await haloPixels(page)).count, { timeout: 15_000 }).toBeGreaterThan(200);
  const px = await haloPixels(page);
  expect(px.r).toBeGreaterThan(px.b + 60); // doré : beaucoup plus de rouge que de bleu
  expect(px.center).toBe(0);               // le visage lui-même n'est pas recouvert
});

test('le halo est posé exactement sur la vidéo, en miroir comme elle', async ({ page }) => {
  await syntheticPose(page);
  await toCamera(page);
  await expect(page.locator('#camera-halo')).toBeVisible({ timeout: 15_000 });
  const [video, halo] = await Promise.all([page.locator('#camera-thumbnail').boundingBox(), page.locator('#camera-halo').boundingBox()]);
  expect(halo).toEqual(video);
  expect(await page.locator('#camera-halo').evaluate((e) => getComputedStyle(e).transform)).toContain('-1');
});

test('hors mode caméra, plus de halo', async ({ page }) => {
  await syntheticPose(page);
  await toCamera(page);
  await expect.poll(async () => (await haloPixels(page)).count, { timeout: 15_000 }).toBeGreaterThan(200);
  await page.keyboard.press('Tab'); // retour au clavier
  await expect(page.locator('#camera-halo')).toBeHidden();
  expect((await haloPixels(page)).count).toBe(0);
});
