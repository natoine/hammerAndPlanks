import { test, expect, Page } from '@playwright/test';

// Le lanceur web expose la position du bateau sur le canvas (data-boat-offset) :
// décalage depuis le centre en fraction de la largeur d'écran, négatif à gauche.
// Mode de contrôle par défaut : position (comme l'ancien jeu) ; ?control=velocity|force pour les autres.
test.use({ locale: 'fr-FR' });

const offset = async (page: Page) => Number((await page.locator('#canvas').getAttribute('data-boat-offset')) ?? 'NaN');

async function open(page: Page, query = '') {
  await page.goto('/' + query);
  await expect(page.locator('#input-mode')).toHaveText("Mode d'interaction : Clavier (flèches)", { timeout: 30_000 });
  await expect.poll(() => offset(page)).toBe(0);
}

async function hold(page: Page, key: string, ms: number) {
  await page.keyboard.down(key);
  await page.waitForTimeout(ms);
  await page.keyboard.up(key);
}

/** Maintient [key] jusqu'à ce que la condition sur la position soit vraie (rendu logiciel parfois lent en test). */
async function holdUntil(page: Page, key: string, check: (o: number) => boolean) {
  await page.keyboard.down(key);
  await expect.poll(async () => check(await offset(page)), { timeout: 20_000 }).toBe(true);
  await page.keyboard.up(key);
}

/** Attend que le bateau ne bouge plus et renvoie sa position. */
async function settled(page: Page) {
  let previous = NaN;
  await expect.poll(async () => {
    const current = await offset(page);
    const still = current === previous;
    previous = current;
    return still;
  }, { intervals: [400], timeout: 20_000 }).toBe(true);
  return previous;
}

test.describe('mode position (par défaut)', () => {
  test('flèche gauche : le bateau part à gauche', async ({ page }) => {
    await open(page);
    await holdUntil(page, 'ArrowLeft', (o) => o < -0.02);
  });

  test('flèche droite : le bateau part à droite', async ({ page }) => {
    await open(page);
    await holdUntil(page, 'ArrowRight', (o) => o > 0.02);
  });

  test('relâcher : le bateau revient au centre', async ({ page }) => {
    await open(page);
    await holdUntil(page, 'ArrowRight', (o) => o > 0.02);
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(0.005);
  });

  test('le bateau ne sort jamais de l’écran', async ({ page }) => {
    await open(page);
    await page.keyboard.down('ArrowLeft');
    for (let i = 0; i < 8; i++) {
      await page.waitForTimeout(500);
      expect(await offset(page)).toBeGreaterThan(-0.5);
    }
    await page.keyboard.up('ArrowLeft');
  });
});

test.describe('mode vitesse', () => {
  test('relâcher : le bateau s’arrête sur place, sans revenir au centre', async ({ page }) => {
    await open(page, '?control=velocity');
    await holdUntil(page, 'ArrowLeft', (o) => o < -0.03);
    const stop = await settled(page);
    expect(stop).toBeLessThan(-0.02);
  });
});

test.describe('mode force', () => {
  test('relâcher : le bateau garde sa lancée', async ({ page }) => {
    await open(page, '?control=force');
    await holdUntil(page, 'ArrowRight', (o) => o > 0.02);
    const released = await offset(page);
    await page.waitForTimeout(800);
    expect(await offset(page)).toBeGreaterThan(released);
  });
});

test('un facteur de vitesse nul immobilise le bateau', async ({ page }) => {
  await open(page, '?speed=0');
  await hold(page, 'ArrowLeft', 800);
  await page.waitForTimeout(300);
  expect(await offset(page)).toBe(0);
});

test('les flèches ne font pas défiler la page', async ({ page }) => {
  await open(page);
  await hold(page, 'ArrowRight', 300);
  expect(await page.evaluate(() => [window.scrollX, window.scrollY])).toEqual([0, 0]);
});

test('hors mode clavier, les flèches ne déplacent pas le bateau', async ({ page }) => {
  await open(page);
  await page.keyboard.press('Tab'); // manette
  await expect(page.locator('#input-mode')).toHaveText(/Manette/);
  await hold(page, 'ArrowLeft', 600);
  await page.waitForTimeout(300);
  expect(await offset(page)).toBe(0);
});
