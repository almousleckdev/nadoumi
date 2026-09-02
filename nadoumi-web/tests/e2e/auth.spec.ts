import { test, expect } from '@playwright/test'
import { readOtp, uniqueEmail } from './helpers/mail'

test('register (2-step OTP) -> auto-login -> dashboard -> sign out', async ({ page }) => {
  const email = uniqueEmail()

  await page.goto('/register')
  await page.fill('#firstName', 'E2E')
  await page.fill('#lastName', 'Tester')
  await page.fill('#otp-email', email)
  await page.getByRole('button', { name: /verify/i }).click()

  const code = await readOtp(email)
  const boxes = page.locator('input[inputmode="numeric"]')
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)

  await page.fill('#password', 'Abcdef1!')
  await page.fill('#confirm', 'Abcdef1!')
  await page.check('#accept-terms')
  await page.check('#accept-privacy')
  await page.getByRole('button', { name: /next/i }).click()

  // server-side auto-login landed us in the dashboard
  await expect(page).toHaveURL(/\/dashboard\/profile/)

  // sign out from the account menu
  await page.locator('button[aria-haspopup="menu"]').last().click()
  await page.locator('[data-test="sign-out"]').click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible()
})
