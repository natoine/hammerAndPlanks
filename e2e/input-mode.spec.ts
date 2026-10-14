import { test, expect, Page } from '@playwright/test';

// Le lanceur web annonce le mode d'interaction dans une zone aria-live (#input-mode) : on la lit pour vérifier l'état.
test.use({ locale: 'fr-FR' });

async function open(page: Page) {
  await page.goto('/');
  const status = page.locator('#input-mode');
  await expect(status).toHaveText("Mode d'interaction : Clavier (flèches)", { timeout: 30_000 });
  return status;
}

/** Centre approximatif du badge, en bas à gauche (marge de 5 % du petit côté). */
function badgePoint(page: Page) {
  const { width, height } = page.viewportSize()!;
  const margin = Math.min(width, height) * 0.05;
  return { x: margin + 25, y: height - margin - 22 };
}

test('le clavier est le mode par défaut', async ({ page }) => {
  await open(page);
});

test('Tab fait défiler clavier, manette, caméra puis revient au clavier', async ({ page }) => {
  const status = await open(page);
  await page.keyboard.press('Tab');
  await expect(status).toHaveText("Mode d'interaction : Manette");
  await page.keyboard.press('Tab');
  await expect(status).toHaveText("Mode d'interaction : Caméra");
  await page.keyboard.press('Tab');
  await expect(status).toHaveText("Mode d'interaction : Clavier (flèches)");
});

test('Tab ne fait pas quitter le jeu au navigateur (focus conservé)', async ({ page }) => {
  await open(page);
  await page.keyboard.press('Tab');
  const focused = await page.evaluate(() => document.activeElement?.tagName);
  expect(['BODY', 'CANVAS']).toContain(focused);
});

test('un appui sur le badge change de mode, ailleurs non', async ({ page }) => {
  const status = await open(page);
  const { width, height } = page.viewportSize()!;
  await page.mouse.click(width / 2, height / 2); // au centre : rien ne change
  await expect(status).toHaveText("Mode d'interaction : Clavier (flèches)");
  const p = badgePoint(page);
  await page.mouse.click(p.x, p.y);
  await expect(status).toHaveText("Mode d'interaction : Manette");
});

test('le mode choisi est conservé au rechargement', async ({ page }) => {
  const status = await open(page);
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await expect(status).toHaveText("Mode d'interaction : Caméra");
  await page.reload();
  await expect(page.locator('#input-mode')).toHaveText("Mode d'interaction : Caméra", { timeout: 30_000 });
});

test.describe('navigateur en anglais', () => {
  test.use({ locale: 'en-US' });

  test('mode annoncé en anglais', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#input-mode')).toHaveText('Interaction mode: Keyboard (arrow keys)', { timeout: 30_000 });
  });
});
