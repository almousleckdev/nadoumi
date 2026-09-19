import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import PassportUploadCard from '~/components/onboarding/PassportUploadCard.vue'
import { fakeFile, pickFile } from '../helpers/files'

const { api, read } = vi.hoisted(() => ({
  read: vi.fn(),
  api: { passportStatus: vi.fn(), uploadPassportScan: vi.fn(), savePassport: vi.fn() },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))
mockNuxtImport('usePassportReader', () => () => ({ read }))
// FileReader completes on a later macro-task than the tests wait for, so it is replaced here
vi.mock('~/utils/files', async (original) => ({
  ...(await original<typeof import('~/utils/files')>()),
  readAsDataUrl: async () => 'data:image/png;base64,AAAA',
}))

const EMPTY = {
  passportNo: null, givenName: null, familyName: null, dob: null, issueDate: null, expiryDate: null,
  readMethod: null, edited: false, scanUploaded: false, validForAdmission: false, matchesProfile: false, mismatches: [],
}
const SAVED_OK = {
  ...EMPTY, passportNo: 'L898902C3', givenName: 'ANNA', familyName: 'ERIKSSON', dob: '1990-01-01',
  issueDate: '2024-01-01', expiryDate: '2099-01-01', readMethod: 'MRZ', scanUploaded: true,
  validForAdmission: true, matchesProfile: true,
}
const READING = {
  documentNumber: 'L898902C3', surname: 'ERIKSSON', givenNames: 'ANNA', dateOfBirth: '1990-01-01',
  expiryDate: '2099-01-01', issuingCountry: null, nationality: null,
}

async function mountCard() {
  const w = await mountSuspended(PassportUploadCard, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}
const value = (w: Awaited<ReturnType<typeof mountCard>>, id: string) => (w.find(id).element as HTMLInputElement).value

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  read.mockReset()
  api.passportStatus.mockResolvedValue(EMPTY)
  api.uploadPassportScan.mockResolvedValue({ mediaId: 5 })
  api.savePassport.mockResolvedValue(SAVED_OK)
})

describe('PassportUploadCard', () => {
  it('starts empty and asks for a passport, with no details form yet', async () => {
    const w = await mountCard()

    expect(w.text()).toContain('No passport uploaded')
    expect(w.find('#passportNo').exists()).toBe(false)
  })

  it('reads the passport in the browser and prefills the details for the student to confirm', async () => {
    read.mockResolvedValue(READING)
    const w = await mountCard()

    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()

    expect(read).toHaveBeenCalledOnce()
    expect(value(w, '#passportNo')).toBe('L898902C3')
    expect(value(w, '#passportFamilyName')).toBe('ERIKSSON')
    expect(w.text()).toContain('We read these details')
  })

  it('falls back to typing the details when the passport cannot be read', async () => {
    read.mockResolvedValue(null)
    const w = await mountCard()

    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()

    expect(w.text()).toContain('could not read the passport automatically')
    expect(w.find('#passportNo').exists()).toBe(true)
    expect(value(w, '#passportNo')).toBe('')
  })

  it('does not try to read a PDF', async () => {
    const w = await mountCard()

    await pickFile(w, fakeFile('passport.pdf', 'application/pdf'))
    await flushPromises()

    expect(read).not.toHaveBeenCalled()
    expect(w.text()).toContain('PDF files cannot be read automatically')
  })

  it('uploads the scan then saves the details, recording that they were read and unedited', async () => {
    read.mockResolvedValue(READING)
    const w = await mountCard()
    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()
    await w.find('#passportIssueDate').setValue('2024-01-01')

    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.uploadPassportScan).toHaveBeenCalledOnce()
    expect(api.savePassport).toHaveBeenCalledWith(1, expect.objectContaining({
      passportNo: 'L898902C3', readMethod: 'MRZ', edited: false,
    }))
    expect(w.emitted('changed')).toHaveLength(1)
    expect(w.text()).toContain('matches your profile')
  })

  it('records that the student edited what was read', async () => {
    read.mockResolvedValue(READING)
    const w = await mountCard()
    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()
    await w.find('#passportIssueDate').setValue('2024-01-01')
    await w.find('#passportGivenName').setValue('ANNE')

    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.savePassport).toHaveBeenCalledWith(1, expect.objectContaining({ readMethod: 'MRZ', edited: true }))
  })

  it('records a typed passport as manual', async () => {
    read.mockResolvedValue(null)
    const w = await mountCard()
    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()
    for (const [id, v] of [['#passportNo', 'L898902C3'], ['#passportGivenName', 'ANNA'], ['#passportFamilyName', 'ERIKSSON'],
      ['#passportDob', '1990-01-01'], ['#passportIssueDate', '2024-01-01'], ['#passportExpiryDate', '2099-01-01']] as const) {
      await w.find(id).setValue(v)
    }

    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.savePassport).toHaveBeenCalledWith(1, expect.objectContaining({ readMethod: 'MANUAL', edited: false }))
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
    read.mockResolvedValue(READING)
    api.savePassport.mockRejectedValue({ statusCode: 400, data: { detail: 'passport must be valid for more than six months from today' } })
    const w = await mountCard()
    await pickFile(w, fakeFile('passport.png', 'image/png'))
    await flushPromises()
    await w.find('#passportIssueDate').setValue('2024-01-01')

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
