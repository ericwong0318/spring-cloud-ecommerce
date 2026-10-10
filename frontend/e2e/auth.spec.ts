import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should navigate to login page', async ({ page }) => {
    await page.click('text=Login');
    await expect(page).toHaveURL(/.*login/);
    await expect(page.locator('h5')).toContainText('Login');
  });

  test('should navigate to register page', async ({ page }) => {
    await page.click('text=Register');
    await expect(page).toHaveURL(/.*register/);
    await expect(page.locator('h5')).toContainText('Register');
  });

  test('should show validation errors on empty login form', async ({ page }) => {
    await page.goto('/login');
    await page.click('button[type="submit"]');
    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();
  });

  test('should show validation errors on empty register form', async ({ page }) => {
    await page.goto('/register');
    await page.click('button[type="submit"]');
    await expect(page.locator('text=First name is required')).toBeVisible();
    await expect(page.locator('text=Last name is required')).toBeVisible();
    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();
  });

  test('should navigate to forgot password page', async ({ page }) => {
    await page.goto('/login');
    await page.click('text=Forgot Password');
    await expect(page).toHaveURL(/.*forgot-password/);
    await expect(page.locator('h5')).toContainText('Forgot Password');
  });
});

test.describe('Product Browsing', () => {
  test('should display product list', async ({ page }) => {
    await page.goto('/products');
    await expect(page.locator('text=Products')).toBeVisible();
    await expect(page.locator('[data-testid="product-card"]').first()).toBeVisible();
  });

  test('should filter products by search', async ({ page }) => {
    await page.goto('/products');
    await page.fill('input[placeholder*="search" i]', 'test');
    await expect(page.locator('text=test')).toBeVisible();
  });

  test('should navigate to product detail', async ({ page }) => {
    await page.goto('/products');
    await page.click('[data-testid="product-card"]:first-child');
    await expect(page).toHaveURL(/.*products\/.*/);
  });
});

test.describe('Cart Functionality', () => {
  test('should add item to cart', async ({ page }) => {
    await page.goto('/products');
    await page.click('[data-testid="product-card"]:first-child');
    await page.click('button:has-text("Add to Cart")');
    await expect(page.locator('text=Added to cart')).toBeVisible();
  });

  test('should display cart page', async ({ page }) => {
    await page.goto('/cart');
    await expect(page.locator('text=Shopping Cart')).toBeVisible();
  });
});

test.describe('Accessibility', () => {
  test('should have skip link', async ({ page }) => {
    await page.goto('/');
    await page.keyboard.press('Tab');
    await expect(page.locator('text=Skip to main content')).toBeFocused();
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/');
    const h1 = page.locator('h1');
    await expect(h1).toHaveCount(1);
  });
});