import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import PassportUploadCard from '~/components/onboarding/PassportUploadCard.vue'
import { fakeFile, pickFile } from '../helpers/files'

const { api } = vi.hoisted(() => ({
  api: { passportStatus: vi.fn(), uploadPassportScan: vi.fn(), savePassport: vi.fn(), passportScanUrl: vi.fn() },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))
vi.mock('~/utils/files', async (original) => ({
  ...(await original<typeof import('~/utils/files')>()),
  readAsDataUrl: async () => 'data:image/png;base64,AAAA',
}))

const EMPTY = {
  passportNo: null, givenName: null, familyName: null, dob: null, issueDate: null, expiryDate: null,
  readMethod: null, edited: false, scanUploaded: false, validForAdmission: false, matchesProfile: false, mismatches: [],
}
const SAVED_OK = {
  ...EMPTY, passportNo: 'L898902C3', givenName: null, familyName: null, dob: null,
  issueDate: '2024-01-01', expiryDate: '2099-01-01', readMethod: 'MANUAL', scanUploaded: true,
  validForAdmission: true, matchesProfile: true,
}

async function mountCard() {
  const w = await mountSuspended(PassportUploadCard, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}
const value = (w: Awaited<ReturnType<typeof mountCard>>, id: string) => (w.find(id).element as HTMLInputElement).value

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  api.passportStatus.mockResolvedValue(EMPTY)
  api.uploadPassportScan.mockResolvedValue({ mediaId: 5 })
  api.savePassport.mockResolvedValue(SAVED_OK)
  api.passportScanUrl.mockResolvedValue({ url: 'https://cdn.example.com/passport-scan.jpg', expiresAt: '2099-01-01' })
})

describe('PassportUploadCard', () => {
  it('starts empty and asks for a passport, with no details form yet', async () => {
    const w = await mountCard()

    expect(w.text()).toContain('No passport uploaded')
    expect(w.find('#passportNo').exists()).toBe(false)
  })

  it('shows a small preview and a form for the number and dates only', async () => {
    const w = await mountCard()

    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()

    expect(w.find('img').attributes('src')).toBe('data:image/png;base64,AAAA')
    expect(value(w, '#passportNo')).toBe('')
    expect(w.find('#passportGivenName').exists()).toBe(false)
    expect(w.find('#passportFamilyName').exists()).toBe(false)
    expect(w.find('#passportDob').exists()).toBe(false)
  })

  it('shows a file card, not an image preview, for a PDF', async () => {
    const w = await mountCard()

    await pickFile(w, fakeFile('passport.pdf', 'application/pdf'))
    await flushPromises()

    expect(w.find('img').exists()).toBe(false)
    expect(w.text()).toContain('passport.pdf')
    expect(w.text()).toContain('PDF')
  })

  it('uploads the scan then saves the details as a manual entry', async () => {
    const w = await mountCard()
    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()
    for (const [id, v] of [['#passportNo', 'L898902C3'], ['#passportIssueDate', '2024-01-01'], ['#passportExpiryDate', '2099-01-01']] as const) {
      await w.find(id).setValue(v)
    }

    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.uploadPassportScan).toHaveBeenCalledOnce()
    expect(api.savePassport).toHaveBeenCalledWith(1, expect.objectContaining({
      passportNo: 'L898902C3', readMethod: 'MANUAL', edited: false,
    }))
    expect(w.emitted('changed')).toHaveLength(1)
    expect(w.text()).not.toContain('matches your profile')
  })

  it('shows the saved number and dates and the saved scan, with the form always available', async () => {
    api.passportStatus.mockResolvedValue(SAVED_OK)
    const w = await mountCard()

    expect(api.passportScanUrl).toHaveBeenCalledWith(1)
    expect(w.find('img').attributes('src')).toBe('https://cdn.example.com/passport-scan.jpg')
    expect(value(w, '#passportNo')).toBe(SAVED_OK.passportNo)
    expect(value(w, '#passportExpiryDate')).toBe(SAVED_OK.expiryDate)
    expect(w.text()).toContain('Complete')
  })

  it('does not fetch a scan URL when nothing has been uploaded yet', async () => {
    api.passportStatus.mockResolvedValue(EMPTY)
    await mountCard()

    expect(api.passportScanUrl).not.toHaveBeenCalled()
  })

  it('tells the student when the passport and the profile do not match', async () => {
    api.passportStatus.mockResolvedValue({
      ...SAVED_OK, matchesProfile: false,
      mismatches: [{ field: 'givenName', passportValue: 'ANNA', profileValue: 'ANA' }],
    })
    const w = await mountCard()

    expect(w.text()).toContain('do not match')
    expect(w.text()).toContain('ANA')
    expect(w.text()).toContain('Needs attention')
  })

  it('shows the server refusing a passport that expires within six months', async () => {
    api.savePassport.mockRejectedValue({ statusCode: 400, data: { detail: 'passport must be valid for more than six months from today' } })
    const w = await mountCard()
    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()
    for (const [id, v] of [['#passportNo', 'L898902C3'], ['#passportIssueDate', '2024-01-01'], ['#passportExpiryDate', '2099-01-01']] as const) {
      await w.find(id).setValue(v)
    }

    await w.find('form').trigger('submit')
    await flushPromises()

    expect(w.text()).toContain('valid for more than six months')
    expect(w.emitted('changed')).toBeUndefined()
  })

  it('jumps back to the profile from the mismatch notice', async () => {
    api.passportStatus.mockResolvedValue({
      ...SAVED_OK, matchesProfile: false, mismatches: [{ field: 'dob', passportValue: '1990-01-01', profileValue: '1990-01-02' }],
    })
    const w = await mountCard()

    await w.findAll('button').find(b => b.text() === 'Edit my profile')!.trigger('click')

    expect(w.emitted('edit-profile')).toHaveLength(1)
  })
})
