import { test, expect } from '@playwright/test'
import { readOtp, uniqueEmail } from './helpers/mail'

const OLD_PASSWORD = 'Abcdef1!'
const NEW_PASSWORD = 'Zxcvbn2@'

async function fillOtp(page: import('@playwright/test').Page, email: string) {
  const code = await readOtp(email)
  const boxes = page.locator('input[inputmode="numeric"]')
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)
}

test('forgot-password -> OTP -> new password -> sign in with the new password', async ({ page }) => {
  const email = uniqueEmail()

  // arrange: register an account through the UI, then sign out
  await page.goto('/register')
  await page.waitForLoadState('networkidle')
  await page.fill('#firstName', 'Reset')
  await page.fill('#lastName', 'User')
  await page.fill('#otp-email', email)
  await expect(page.getByRole('button', { name: /verify/i })).toBeEnabled()
  await page.getByRole('button', { name: /verify/i }).click()
  await fillOtp(page, email)
  await page.fill('#password', OLD_PASSWORD)
  await page.fill('#confirm', OLD_PASSWORD)
  await page.check('#accept-terms')
  await page.check('#accept-privacy')
  await expect(page.getByRole('button', { name: /next/i })).toBeEnabled()
  await page.getByRole('button', { name: /next/i }).click()
  await expect(page).toHaveURL(/\/dashboard\/profile/)
  await page.locator('button[aria-haspopup="menu"]').last().click()
  await page.locator('[data-test="sign-out"]').click()
  await expect(page).toHaveURL(/\/$/)

  // act: reset the password
  await page.goto('/forgot-password')
  await page.waitForLoadState('networkidle')
  await page.fill('#otp-email', email)
  await page.getByRole('button', { name: /verify/i }).click()
  await fillOtp(page, email)
  await page.fill('#reset-new', NEW_PASSWORD)
  await page.fill('#reset-confirm', NEW_PASSWORD)
  await expect(page.getByRole('button', { name: /reset password/i })).toBeEnabled()
  await page.getByRole('button', { name: /reset password/i }).click()
  await expect(page.getByText(/sign in with your new password/i)).toBeVisible()

  // assert: the new password works
  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.fill('#email', email)
  await page.fill('#password', NEW_PASSWORD)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).toHaveURL(/\/dashboard/)
})
