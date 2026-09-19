import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises, type VueWrapper } from '@vue/test-utils'
import Onboarding from '~/pages/onboarding.vue'

const { nav, complete, status, applicant } = vi.hoisted(() => ({
  nav: vi.fn(),
  complete: vi.fn(),
  status: vi.fn(),
  applicant: {
    id: 1, givenName: 'ADA', familyName: 'LOVELACE', dob: '2000-01-01', nationality: 'GB', passportNo: null,
    email: 'a@b.co', phone: '+441', status: 'ACTIVE', gender: 'FEMALE', countryOfOrigin: 'GB',
    countryOfResidence: 'CN', nativeLanguage: 'en', wechatId: null, whatsapp: '+441',
    emailVerified: true, onboardingComplete: false,
  },
}))

vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ activeApplicantId: ref(1), refresh: vi.fn() }),
}))
vi.mock('~/composables/useApplicant', () => ({
  useApplicant: () => ({
    listMine: vi.fn().mockResolvedValue([applicant]),
    get: vi.fn().mockResolvedValue(applicant),
    create: vi.fn(),
    update: vi.fn().mockResolvedValue(applicant),
    completeOnboarding: (...args: unknown[]) => complete(...args),
    onboardingStatus: (...args: unknown[]) => status(...args),
    photoUrl: vi.fn().mockRejectedValue({ statusCode: 404 }),
    uploadPhoto: vi.fn(),
    passportStatus: vi.fn().mockResolvedValue({
      passportNo: null, givenName: null, familyName: null, dob: null, issueDate: null, expiryDate: null,
      readMethod: null, edited: false, scanUploaded: false, validForAdmission: false, matchesProfile: false, mismatches: [],
    }),
    uploadPassportScan: vi.fn(),
    savePassport: vi.fn(),
    listEducation: vi.fn().mockResolvedValue([]),
    addEducation: vi.fn(),
    updateEducation: vi.fn(),
    deleteEducation: vi.fn(),
  }),
}))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

const section = (key: string, complete: boolean) => ({ key, complete, missing: complete ? [] : ['x'] })
const progress = (photo: boolean, passport: boolean) => ({
  complete: false, ready: photo && passport,
  sections: [section('PROFILE', true), section('PHOTO', photo), section('PASSPORT', passport)],
})

const button = (w: VueWrapper, label: string) =>
  w.findAll('button').find(b => b.text().toLowerCase() === label.toLowerCase())

async function reviewStep() {
  status.mockResolvedValue(progress(true, true))
  const w = await mountSuspended(Onboarding)
  await flushPromises()
  await w.find('form').trigger('submit') // Save and continue
  await flushPromises()
  for (let i = 0; i < 5; i++) {
    await button(w, 'next')?.trigger('click')
    await flushPromises()
  }
  return w
}

beforeEach(() => {
  nav.mockReset()
  complete.mockReset()
  status.mockReset().mockResolvedValue(progress(false, false))
})

describe('onboarding wizard', () => {
  it('starts on the profile step with a save-and-continue action', async () => {
    const w = await mountSuspended(Onboarding)
    await flushPromises()

    expect(w.text()).toContain('Create your profile')
    expect(button(w, 'Save and continue')).toBeTruthy()
    expect(button(w, 'Next')).toBeUndefined()
  })

  it('advances past the profile only after it is saved', async () => {
    const w = await mountSuspended(Onboarding)
    await flushPromises()
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(w.text()).toContain('Profile photo')
    expect(w.text()).toContain('Passport')
  })

  it('keeps Next disabled on the identity step until the server reports photo and passport complete', async () => {
    const w = await mountSuspended(Onboarding)
    await flushPromises()
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(button(w, 'Next')!.attributes('disabled')).toBeDefined()
    expect(w.text()).toContain('Add your photo and a matching passport to continue.')
  })

  it('enables Next once the server reports photo and passport complete', async () => {
    status.mockResolvedValue(progress(true, true))
    const w = await mountSuspended(Onboarding)
    await flushPromises()
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(button(w, 'Next')!.attributes('disabled')).toBeUndefined()
    expect(w.text()).not.toContain('Add your photo and a matching passport to continue.')
  })

  it('leaves for the dashboard only after the server accepts Finish', async () => {
    complete.mockResolvedValue({ complete: true, ready: true, sections: [] })
    const w = await reviewStep()

    await button(w, 'Finish')!.trigger('click')
    await flushPromises()

    expect(complete).toHaveBeenCalledWith(1)
    expect(nav).toHaveBeenCalledWith('/dashboard')
  })

  it('stays put, names the missing section and returns to it when the server refuses Finish', async () => {
    const w = await reviewStep()
    complete.mockRejectedValue({ statusCode: 400, data: { detail: 'onboarding is incomplete: PROFILE' } })
    status.mockResolvedValue({ complete: false, ready: false, sections: [{ key: 'PROFILE', complete: false, missing: ['phone'] }] })

    await button(w, 'Finish')!.trigger('click')
    await flushPromises()

    expect(nav).not.toHaveBeenCalled()
    expect(w.text()).toContain('Some required information is missing: Profile')
    expect(w.text()).toContain('Create your profile')
  })
})
