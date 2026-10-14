import { test, expect, Page } from '@playwright/test';

// Mode caméra : InstantHMR (webapp/pose.js) → inclinaison du buste → bateau.
// La caméra de test (mire Chromium) ne montre personne : pour piloter, on remplace window.hapPose par une pose
// synthétique dont l'inclinaison des épaules vaut window.__tilt (degrés, > 0 = le joueur penche vers SA droite).
test.use({ locale: 'fr-FR' });

const offset = async (page: Page) => Number((await page.locator('#canvas').getAttribute('data-boat-offset')) ?? 'NaN');
const motionStatus = (page: Page) => page.locator('#canvas').getAttribute('data-motion-status');

async function toCameraMode(page: Page, query = '') {
  await page.goto('/' + query);
  await expect(page.locator('#input-mode')).toHaveText(/Mode d'interaction : /, { timeout: 30_000 });
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await expect(page.locator('#input-mode')).toHaveText("Mode d'interaction : Caméra");
}

/** Pose synthétique : épaules (5, 6) et oreilles (3, 4) inclinées de window.__tilt, axes caméra, y vers le bas. */
async function syntheticPose(page: Page) {
  await page.addInitScript(() => {
    const w = window as any;
    w.__tilt = 0;
    let running = false;
    const joint = (i: number) => {
      const a = (w.__tilt * Math.PI) / 180, line = (left: number, right: number, half: number, cy: number) => {
        const dx = half * Math.cos(a), dy = half * Math.sin(a);
        if (i === 3 * right) return -dx; if (i === 3 * right + 1) return cy + dy;
        if (i === 3 * left) return dx; if (i === 3 * left + 1) return cy - dy;
        return null;
      };
      return line(5, 6, 0.2, 0) ?? line(3, 4, 0.08, -0.25) ?? 0;
    };
    w.hapPose = {
      status: () => (running ? 'RUNNING' : 'OFF'),
      now: () => performance.now() / 1000,
      start: () => { running = true; },
      stop: () => { running = false; },
      time: () => (running ? Math.floor(performance.now() / 50) * 0.05 : -1), // nouvelle pose toutes les 50 ms
      joint3d: joint,
      joint2d: () => 0,
      width: () => 640,
      height: () => 480,
      inferenceMs: () => 1,
    };
  });
}

test.describe('pilotage par le corps (pose synthétique)', () => {
  test('calibration puis pencher vers sa droite envoie le bateau à droite', async ({ page }) => {
    await syntheticPose(page);
    await toCameraMode(page);
    await expect(page.locator('#motion-status')).toHaveText(/Tenez-vous droit|Penchez le buste/, { timeout: 15_000 });
    await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('RUNNING');
    await expect(page.locator('#motion-status')).toHaveText('Penchez le buste pour diriger');
    await page.evaluate(() => { (window as any).__tilt = 20; });
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeGreaterThan(0.02);
  });

  test('mode position : pencher à gauche envoie le bateau à gauche, se redresser le ramène au centre', async ({ page }) => {
    await syntheticPose(page);
    await toCameraMode(page, '?control=position');
    await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('RUNNING');
    await page.evaluate(() => { (window as any).__tilt = -20; });
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(-0.02);
    await page.evaluate(() => { (window as any).__tilt = 0; });
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeGreaterThan(-0.005);
  });

  test('mode élan (par défaut) : pencher à gauche fait filer le bateau à gauche', async ({ page }) => {
    await syntheticPose(page);
    await toCameraMode(page);
    await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('RUNNING');
    await page.evaluate(() => { (window as any).__tilt = -20; });
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(-0.1);
  });

  test('petite inclinaison ignorée (zone morte)', async ({ page }) => {
    await syntheticPose(page);
    await toCameraMode(page);
    await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('RUNNING');
    await page.evaluate(() => { (window as any).__tilt = 2; });
    await page.waitForTimeout(1500);
    expect(await offset(page)).toBe(0);
  });

  test('suivi de la tête avec ?tilt=head', async ({ page }) => {
    await syntheticPose(page);
    await toCameraMode(page, '?tilt=head');
    await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('RUNNING');
    await expect(page.locator('#motion-status')).toHaveText('Penchez la tête pour diriger');
    await page.evaluate(() => { (window as any).__tilt = 20; });
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeGreaterThan(0.02);
  });

  test('hors mode caméra, l’inclinaison ne pilote pas', async ({ page }) => {
    await syntheticPose(page);
    await toCameraMode(page, '?control=position');
    await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('RUNNING');
    await page.keyboard.press('Tab'); // retour au clavier
    await expect.poll(() => motionStatus(page)).toBe('OFF');
    await page.evaluate(() => { (window as any).__tilt = 25; });
    await page.waitForTimeout(1500);
    expect(Math.abs(await offset(page))).toBeLessThan(0.005);
  });
});

test('modèle absent : le badge le signale', async ({ page }) => {
  await page.route('**/models/instanthmr.onnx', (route) => route.fulfill({ status: 404, body: '' }));
  await toCameraMode(page);
  await expect.poll(() => motionStatus(page), { timeout: 15_000 }).toBe('NO_MODEL');
  await expect(page.locator('#motion-status')).toHaveText('Modèle de pose absent');
});

test('le vrai modèle InstantHMR se charge et tourne dans le navigateur', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop-chromium', 'Chargement de 77 Mo : un seul profil suffit');
  test.setTimeout(180_000);
  await toCameraMode(page);
  // Personne dans la mire de test : l'estimation tourne mais la calibration n'aboutit pas.
  await expect.poll(() => page.evaluate(() => (window as any).hapPose.status()), { timeout: 150_000 }).toBe('RUNNING');
  const sample = await page.evaluate(() => {
    const p = (window as any).hapPose;
    return { ms: p.inferenceMs(), finite: Array.from({ length: 210 }, (_, i) => p.joint3d(i)).every(Number.isFinite) };
  });
  expect(sample.finite).toBe(true);
  expect(sample.ms).toBeGreaterThan(0);
  await expect(page.locator('#motion-status')).toHaveText('Tenez-vous droit...');
});
