import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ReviewStep from '~/components/onboarding/ReviewStep.vue'

const api = vi.hoisted(() => ({
  passportStatus: vi.fn(), listEducation: vi.fn(), getInterests: vi.fn(), getResidence: vi.fn(),
  listContacts: vi.fn(), listWork: vi.fn(), photoUrl: vi.fn(), passportScanUrl: vi.fn(),
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const applicant = {
  id: 1, givenName: 'ADA', familyName: 'LOVELACE', dob: '2000-01-01', nationality: 'GB', passportNo: null,
  email: 'ada@example.com', phone: '+441', status: 'ACTIVE', gender: 'FEMALE', countryOfOrigin: 'GB',
  countryOfResidence: 'CN', nativeLanguage: 'en', wechatId: 'ada_wx', whatsapp: '+441', emailVerified: true,
  onboardingComplete: false,
}
const status = { complete: false, ready: true, sections: [] }

async function mountReview() {
  const w = await mountSuspended(ReviewStep, { props: { applicant: applicant as never, status } })
  await flushPromises()
  return w
}

beforeEach(() => {
  api.passportStatus.mockReset().mockResolvedValue({
    passportNo: 'X1234567', givenName: 'ADA', familyName: 'LOVELACE', dob: '2000-01-01', issueDate: '2024-01-01',
    expiryDate: '2034-01-01', scanUploaded: true,
  })
  api.photoUrl.mockReset().mockResolvedValue({ url: 'https://cdn.example/photo.jpg', expiresAt: 'x' })
  api.passportScanUrl.mockReset().mockResolvedValue({ url: 'https://cdn.example/passport.jpg', expiresAt: 'x' })
  api.listEducation.mockReset().mockResolvedValue([])
  api.getInterests.mockReset().mockResolvedValue({
    desiredLevel: 'MASTER', fields: ['Computer Science', 'Law'], cities: ['Beijing', 'Sichuan', 'Shanghai'],
    scholarshipInterest: 'REQUIRED', intakeYear: 2027, intakeTerm: 'FALL', teachingLanguage: 'EN_ZH', notes: 'Near the sea',
  })
  api.getResidence.mockReset().mockResolvedValue(null)
  api.listContacts.mockReset().mockResolvedValue([])
  api.listWork.mockReset().mockResolvedValue([])
})

describe('ReviewStep', () => {
  it('shows the profile picture and the passport scan so the student can check them', async () => {
    const w = await mountReview()

    expect(w.find('[data-test="review-photo"] img').attributes('src')).toBe('https://cdn.example/photo.jpg')
    expect(w.find('[data-test="review-passport"] img').attributes('src')).toBe('https://cdn.example/passport.jpg')
    expect(api.passportScanUrl).toHaveBeenCalledWith(1)
  })

  it('says plainly when the photo or the passport has not been uploaded', async () => {
    api.photoUrl.mockRejectedValue({ statusCode: 404 })
    api.passportStatus.mockResolvedValue({ passportNo: null, scanUploaded: false })
    const w = await mountReview()

    expect(w.find('[data-test="review-photo-missing"]').text()).toBe('Not uploaded yet')
    expect(w.find('[data-test="review-passport-missing"]').exists()).toBe(true)
    expect(api.passportScanUrl).not.toHaveBeenCalled()
  })

  it('lists the passport details and every interest the student typed', async () => {
    const w = await mountReview()
    const text = w.text()

    expect(text).toContain('X1234567')
    expect(text).toContain('2034-01-01')
    expect(text).toContain('Computer Science, Law')
    expect(text).toContain('Beijing, Sichuan, Shanghai')
    expect(text).toContain('English and Chinese')
    expect(text).toContain('Near the sea')
    expect(text).toContain('ada_wx')
  })

  it('asks the server for everything it shows, in one pass', async () => {
    await mountReview()
    for (const call of [api.passportStatus, api.listEducation, api.getInterests, api.getResidence, api.listContacts, api.listWork, api.photoUrl]) {
      expect(call).toHaveBeenCalledWith(1)
    }
  })
})
