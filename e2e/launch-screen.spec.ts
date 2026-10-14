import { test, expect, Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

const version = JSON.parse(readFileSync('package.json', 'utf8')).version as string;

/** Attend que le jeu ait affiché l'écran de lancement (le lanceur web pose alors un libellé sur le canvas). */
async function waitForLaunchScreen(page: Page) {
  await page.goto('/');
  const canvas = page.locator('#canvas');
  await expect(canvas).toHaveAttribute('aria-label', /Hammer and Planks/, { timeout: 30_000 });
  return canvas;
}

test.describe('écran de lancement', () => {
  test.describe('navigateur en français', () => {
    test.use({ locale: 'fr-FR' });

    test('textes en français et langue de la page', async ({ page }) => {
      const canvas = await waitForLaunchScreen(page);
      await expect(canvas).toHaveAttribute('aria-label', 'Hammer and Planks - Un shooter de pirates qui se pilote avec le corps');
      await expect(page.locator('html')).toHaveAttribute('lang', 'fr');
    });
  });

  test.describe('navigateur en anglais', () => {
    test.use({ locale: 'en-US' });

    test('textes en anglais', async ({ page }) => {
      const canvas = await waitForLaunchScreen(page);
      await expect(canvas).toHaveAttribute('aria-label', 'Hammer and Planks - A pirate shooter you steer with your body');
      await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    });
  });

  test.describe('langue non supportée', () => {
    test.use({ locale: 'de-DE' });

    test('retombe sur l’anglais', async ({ page }) => {
      await waitForLaunchScreen(page);
      await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    });
  });

  test('le canvas occupe tout l’écran et le jeu dessine', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    const canvas = await waitForLaunchScreen(page);
    await page.waitForTimeout(1500); // laisse passer le fondu des textes

    const box = await canvas.boundingBox();
    const viewport = page.viewportSize()!;
    expect(box!.width).toBeGreaterThanOrEqual(viewport.width - 1);
    expect(box!.height).toBeGreaterThanOrEqual(viewport.height - 1);

    // Une image vide ou unie trahirait un rendu WebGL raté : on vérifie qu'elle est variée.
    const png = await canvas.screenshot();
    expect(png.length).toBeGreaterThan(20_000);
    expect(errors).toEqual([]);
  });

  test('pas de défilement horizontal sur mobile', async ({ page }) => {
    await waitForLaunchScreen(page);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
    expect(overflow).toBe(false);
  });
});

test('le changelog est publié sur /changelog avec la version courante', async ({ page }) => {
  await page.goto('/changelog/');
  await expect(page.locator('#changelog')).toContainText('Changelog');
  await expect(page.locator('#changelog')).toContainText(`[${version}]`);
});
