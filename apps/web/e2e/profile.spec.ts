import { test, expect } from '@playwright/test';

test.describe('Perfil de Usuario', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion');
    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[type="password"]', 'password123');
    await page.getByRole('button', { name: /iniciar sesión/i }).click();
    await expect(page).toHaveURL(/\/inicio/);
  });

  test('editar perfil', async ({ page }) => {
    await page.getByRole('link', { name: /perfil/i }).first().click();
    await page.getByRole('link', { name: /edit/i }).first().click();
    
    await expect(page).toHaveURL(/\/perfil\/editar/);
    await expect(page.getByText(/editar perfil/i)).toBeVisible();
    
    await page.fill('input#nombre', 'Nuevo Nombre');
    await page.getByRole('button', { name: /guardar/i }).click();
    
    await expect(page).toHaveURL(/\/perfil/);
  });

  test('personalizar avatar 3D', async ({ page }) => {
    await page.getByRole('link', { name: /perfil/i }).first().click();
    await page.getByText(/personalizar avatar/i).click();
    
    await expect(page).toHaveURL(/\/perfil\/avatar/);
    await expect(page.getByText(/personalizar avatar/i)).toBeVisible();
    
    const canvas = page.locator('canvas');
    await expect(canvas).toBeVisible({ timeout: 10000 });
  });
});
