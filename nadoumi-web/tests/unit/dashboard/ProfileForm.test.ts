import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ProfileForm from '~/components/dashboard/ProfileForm.vue'
import type { ApplicantDto } from '~/types/catalog'

const saved: ApplicantDto = {
  id: 1, givenName: 'SAM', familyName: 'LEE', dob: '2000-01-01', nationality: 'MY', passportNo: 'X1',
  email: 's@x.io', phone: '+601', status: 'ACTIVE', gender: 'MALE', countryOfOrigin: 'MY',
  countryOfResidence: 'CN', nativeLanguage: 'ms', wechatId: null, whatsapp: '+601',
  emailVerified: true, onboardingComplete: false, welcomePending: false,
}

const mountForm = (modelValue: ApplicantDto | null = saved) =>
  mountSuspended(ProfileForm, { props: { modelValue, busy: false } })

describe('ProfileForm', () => {
  it('shows required errors and does not submit an empty form', async () => {
    const w = await mountForm(null)
    await w.find('form').trigger('submit')

    expect(w.emitted('submit')).toBeUndefined()
    expect(w.text()).toContain('required')
  })

  it('prefills every field from the applicant', async () => {
    const w = await mountForm()

    expect((w.find('#givenName').element as HTMLInputElement).value).toBe('SAM')
    expect((w.find('#nationality').element as HTMLSelectElement).value).toBe('MY')
    expect((w.find('#countryOfResidence').element as HTMLSelectElement).value).toBe('CN')
    expect((w.find('#whatsapp').element as HTMLInputElement).value).toBe('+601')
  })

  it('states the passport rule and caps the date of birth at 17 years ago', async () => {
    const w = await mountForm()

    expect(w.text()).toContain('exactly as they appear on your passport')
    expect(w.find('#dob').attributes('max')).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('rejects a date of birth under 17 and does not submit', async () => {
    const w = await mountForm()
    await w.find('#dob').setValue(new Date().toISOString().slice(0, 10))
    await w.find('form').trigger('submit')

    expect(w.emitted('submit')).toBeUndefined()
    expect(w.text()).toContain('at least 17')
  })

  it('requires WeChat or WhatsApp', async () => {
    const w = await mountForm({ ...saved, whatsapp: null })
    await w.find('form').trigger('submit')

    expect(w.text()).toContain('WeChat ID or a WhatsApp number')
  })

  it('emits the full body on a valid submit', async () => {
    const w = await mountForm()
    await w.find('form').trigger('submit')

    expect(w.emitted('submit')?.[0]?.[0]).toMatchObject({
      givenName: 'SAM', familyName: 'LEE', dob: '2000-01-01', gender: 'MALE',
      countryOfOrigin: 'MY', countryOfResidence: 'CN', nativeLanguage: 'ms', email: 's@x.io',
    })
  })

  it('offers verification, and blocks saving, when the email is changed', async () => {
    const w = await mountForm()
    await w.find('#pemail').setValue('new@x.io')
    await w.find('form').trigger('submit')

    expect(w.text()).toContain('Verify email')
    expect(w.emitted('submit')).toBeUndefined()
  })

  it('shows the email as verified when it is the saved, verified one', async () => {
    const w = await mountForm()

    expect(w.text()).toContain('Email verified')
    expect(w.text()).not.toContain('Verify email')
  })
})
