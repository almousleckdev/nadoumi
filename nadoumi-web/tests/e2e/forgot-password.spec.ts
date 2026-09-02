import { test, expect } from '@playwright/test'
import { readOtp, uniqueEmail, REGISTER_SUBJECT, RESET_SUBJECT } from './helpers/mail'

const OLD_PASSWORD = 'Abcdef1!'
const NEW_PASSWORD = 'Zxcvbn2@'

async function fillOtp(page: import('@playwright/test').Page, email: string, subject: RegExp) {
  const boxes = page.locator('input[inputmode="numeric"]')
  await boxes.first().waitFor()
  const code = await readOtp(email, subject)
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)
}

async function registerAndSignOut(page: import('@playwright/test').Page, email: string) {
  await page.goto('/register')
  await page.waitForLoadState('networkidle')
  await page.fill('#firstName', 'Reset')
  await page.fill('#lastName', 'User')
  await page.fill('#otp-email', email)
  await expect(page.getByRole('button', { name: 'Verify', exact: true })).toBeEnabled()
  await page.getByRole('button', { name: 'Verify', exact: true }).click()
  await fillOtp(page, email, REGISTER_SUBJECT)
  await expect(page.getByText(/email verified/i).first()).toBeVisible()
  await page.fill('#password', OLD_PASSWORD)
  await page.fill('#confirm', OLD_PASSWORD)
  await page.check('#accept-terms')
  await expect(page.getByRole('button', { name: 'Next', exact: true })).toBeEnabled()
  await page.getByRole('button', { name: 'Next', exact: true }).click()
  await expect(page).toHaveURL(/\/onboarding/)
  await page.locator('[data-test="user-menu"]').click()
  await page.locator('[data-test="sign-out"]').click()
  await expect(page).toHaveURL(/\/$/)
}

test('forgot-password -> OTP -> new password -> sign in with the new password', async ({ page }) => {
  const email = uniqueEmail()
  await registerAndSignOut(page, email)

  await page.goto('/forgot-password')
  await page.waitForLoadState('networkidle')
  await page.fill('#otp-email', email)
  await page.getByRole('button', { name: 'Verify', exact: true }).click()
  await fillOtp(page, email, RESET_SUBJECT)
  await page.fill('#reset-new', NEW_PASSWORD)
  await page.fill('#reset-confirm', NEW_PASSWORD)
  await expect(page.getByRole('button', { name: /reset password/i })).toBeEnabled()
  await page.getByRole('button', { name: /reset password/i }).click()
  await expect(page.getByText(/sign in with your new password/i)).toBeVisible()

  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.fill('#email', email)
  await page.fill('#password', NEW_PASSWORD)
  await page.getByRole('button', { name: /sign in/i }).click()
  // lands on the dashboard, or onboarding if this account's profile is incomplete
  await expect(page).toHaveURL(/\/(dashboard|onboarding)/)
})
