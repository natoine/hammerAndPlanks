import { test, expect, Page } from '@playwright/test';

// Manette simulée : navigator.getGamepads renvoie une manette dont le stick gauche vaut window.__stick.
test.use({ locale: 'fr-FR' });

const offset = async (page: Page) => Number((await page.locator('#canvas').getAttribute('data-boat-offset')) ?? 'NaN');
const setStick = (page: Page, x: number, y = 0) => page.evaluate(([sx, sy]) => { (window as any).__stick = [sx, sy]; }, [x, y]);

/** Attend que le bateau ne bouge plus (trois relevés identiques à 0,5 s d'intervalle) et renvoie sa position. */
async function settled(page: Page) {
  const samples: number[] = [];
  await expect.poll(async () => {
    samples.push(await offset(page));
    const last = samples.slice(-3);
    return last.length === 3 && last.every((v) => v === last[0]) && last[0] !== 0;
  }, { intervals: [500], timeout: 40_000 }).toBe(true);
  return samples[samples.length - 1];
}

async function openWithGamepad(page: Page, query = '') {
  await page.addInitScript(() => {
    (window as any).__stick = [0, 0];
    Object.defineProperty(navigator, 'getGamepads', {
      value: () => [{ connected: true, id: 'Manette de test', axes: (window as any).__stick, buttons: [] }],
    });
  });
  await page.goto('/' + query);
  await expect(page.locator('#input-mode')).toHaveText(/Mode d'interaction : /, { timeout: 30_000 });
  await page.keyboard.press('Tab'); // clavier -> manette
  await expect(page.locator('#input-mode')).toHaveText("Mode d'interaction : Manette");
  await expect.poll(() => offset(page)).toBe(0);
}

test('stick à gauche : le bateau part à gauche', async ({ page }) => {
  await openWithGamepad(page);
  await setStick(page, -1);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(-0.02);
});

test('stick à droite : le bateau part à droite', async ({ page }) => {
  await openWithGamepad(page);
  await setStick(page, 1);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBeGreaterThan(0.02);
});

test('stick relâché en mode position : le bateau revient au centre', async ({ page }) => {
  await openWithGamepad(page, '?control=position');
  await setStick(page, 1);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBeGreaterThan(0.02);
  await setStick(page, 0);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(0.005);
});

test('stick à mi-course en mode position : le bateau s’arrête à mi-chemin du bord', async ({ page }) => {
  await openWithGamepad(page, '?control=position');
  await setStick(page, 0.575); // (0,575 - 0,15) / 0,85 = 0,5 après zone morte
  const half = await settled(page);
  await setStick(page, 1);
  const full = await settled(page);
  expect(full).toBeGreaterThan(0.05);
  expect(half / full).toBeGreaterThan(0.45);
  expect(half / full).toBeLessThan(0.55);
});

test('mode élan (par défaut) : stick relâché, le bateau revient au centre', async ({ page }) => {
  await openWithGamepad(page);
  await setStick(page, 1);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBeGreaterThan(0.03);
  await setStick(page, 0);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBe(0);
});

test('légère dérive du stick ignorée (zone morte de l’ancien jeu)', async ({ page }) => {
  await openWithGamepad(page);
  await setStick(page, 0.1, 0.1);
  await page.waitForTimeout(1500);
  expect(await offset(page)).toBe(0);
});

test('mode vitesse : stick relâché, le bateau s’arrête sur place', async ({ page }) => {
  await openWithGamepad(page, '?control=velocity');
  await setStick(page, -1);
  await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(-0.03);
  await setStick(page, 0);
  let previous = NaN;
  await expect.poll(async () => { const c = await offset(page); const still = c === previous; previous = c; return still; },
    { intervals: [400], timeout: 20_000 }).toBe(true);
  expect(previous).toBeLessThan(-0.02);
});

test('en mode clavier, le stick ne déplace pas le bateau', async ({ page }) => {
  await openWithGamepad(page);
  await page.keyboard.press('Tab'); await page.keyboard.press('Tab'); // manette -> caméra -> clavier
  await expect(page.locator('#input-mode')).toHaveText("Mode d'interaction : Clavier (flèches)");
  await setStick(page, -1);
  await page.waitForTimeout(1000);
  expect(await offset(page)).toBe(0);
});

test('en mode manette, les flèches ne déplacent pas le bateau', async ({ page }) => {
  await openWithGamepad(page);
  await page.keyboard.down('ArrowLeft');
  await page.waitForTimeout(1000);
  await page.keyboard.up('ArrowLeft');
  expect(await offset(page)).toBe(0);
});
