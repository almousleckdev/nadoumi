import { test, expect } from '@playwright/test'
import { readOtp, uniqueEmail, REGISTER_SUBJECT } from './helpers/mail'

test('register wizard (OTP) -> auto-login -> onboarding -> sign out', async ({ page }) => {
  const email = uniqueEmail()

  await page.goto('/register')
  await page.waitForLoadState('networkidle')

  // Step 1 · personal information
  await page.fill('#firstName', 'E2E')
  await page.fill('#lastName', 'Tester')
  await page.fill('#otp-email', email)

  const verify = page.getByRole('button', { name: 'Verify', exact: true })
  await expect(verify).toBeEnabled()
  await verify.click()

  // Step 2 · email verification
  const code = await readOtp(email, REGISTER_SUBJECT)
  const boxes = page.locator('input[inputmode="numeric"]')
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)
  await expect(page.getByText(/email verified/i).first()).toBeVisible()

  // Step 3 · password
  await page.fill('#password', 'Abcdef1!')
  await page.fill('#confirm', 'Abcdef1!')
  await page.check('#accept-terms') // single combined Terms & Privacy checkbox

  const next = page.getByRole('button', { name: 'Next', exact: true })
  await expect(next).toBeEnabled()
  await next.click()

  // server-side auto-login → onboarding
  await expect(page).toHaveURL(/\/dashboard\/onboarding/)

  await page.locator('button[aria-haspopup="menu"]').last().click()
  await page.locator('[data-test="sign-out"]').click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('link', { name: /sign in/i }).first()).toBeVisible()
})
