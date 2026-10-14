import { test, expect, Page } from '@playwright/test';

// Le lanceur web expose la position du bateau sur le canvas (data-boat-offset) :
// décalage depuis le centre en fraction de la largeur d'écran, négatif à gauche.
// Mode de contrôle par défaut : élan (arcade) ; ?control=position|velocity|force pour les modes de l'ancien jeu.
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

test.describe('mode élan (par défaut)', () => {
  // Vitesse réduite (?speed=0.5) : le bateau reste loin du bord pendant la mesure, même si le rendu est lent.
  // La croissance de l'accélération est vérifiée par les tests unitaires (BoatMotionTest) : la cadence d'images
  // d'un navigateur de test est trop irrégulière pour la mesurer ici.
  test('flèche maintenue : le bateau file à droite', async ({ page }) => {
    await open(page, '?speed=0.5');
    await holdUntil(page, 'ArrowRight', (o) => o > 0.05);
  });

  test('relâcher : le bateau revient au centre tout seul', async ({ page }) => {
    await open(page);
    await holdUntil(page, 'ArrowLeft', (o) => o < -0.05);
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBe(0);
  });

  test('le bateau ne sort jamais de l’écran', async ({ page }) => {
    await open(page);
    await page.keyboard.down('ArrowLeft');
    for (let i = 0; i < 6; i++) {
      await page.waitForTimeout(400);
      expect(await offset(page)).toBeGreaterThan(-0.5);
    }
    await page.keyboard.up('ArrowLeft');
  });
});

test.describe('mode position (ancien jeu)', () => {
  test('flèche gauche : le bateau part à gauche', async ({ page }) => {
    await open(page, '?control=position');
    await holdUntil(page, 'ArrowLeft', (o) => o < -0.02);
  });

  test('flèche droite : le bateau part à droite', async ({ page }) => {
    await open(page, '?control=position');
    await holdUntil(page, 'ArrowRight', (o) => o > 0.02);
  });

  test('relâcher : le bateau revient au centre', async ({ page }) => {
    await open(page, '?control=position');
    await holdUntil(page, 'ArrowRight', (o) => o > 0.02);
    await expect.poll(() => offset(page), { timeout: 20_000 }).toBeLessThan(0.005);
  });

  test('le bateau ne sort jamais de l’écran', async ({ page }) => {
    await open(page, '?control=position');
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

test('la proue s’oriente progressivement vers la direction prise, puis se redresse', async ({ page }) => {
  // Retour au centre puis redressement : environ 3 s de jeu, beaucoup plus en temps réel quand le rendu
  // logiciel des tests parallèles tombe à une ou deux images par seconde (pas de jeu plafonné à 0,1 s).
  test.setTimeout(120_000);
  await open(page);
  const heading = async () => Number((await page.locator('#canvas').getAttribute('data-boat-heading')) ?? 'NaN');
  await page.keyboard.down('ArrowLeft');
  await expect.poll(heading, { timeout: 20_000 }).toBeGreaterThan(5);   // vers la gauche : cap positif
  await expect.poll(heading, { timeout: 20_000 }).toBeGreaterThan(30);  // se rapproche de la diagonale
  expect(await heading()).toBeLessThanOrEqual(45);
  await page.keyboard.up('ArrowLeft');
  await expect.poll(heading, { timeout: 90_000 }).toBe(0);
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
