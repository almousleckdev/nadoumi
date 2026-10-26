import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import DocumentsRequiredCard from '~/components/dashboard/DocumentsRequiredCard.vue'
import ApplicationInterviewCard from '~/components/dashboard/ApplicationInterviewCard.vue'
import AdmissionDocumentsCard from '~/components/dashboard/AdmissionDocumentsCard.vue'
import { BEIJING_ZONE, currentInterview, formatInZone } from '~/utils/interviewTime'

const resubmit = vi.hoisted(() => vi.fn())
const interviews = vi.hoisted(() => vi.fn())
const letters = vi.hoisted(() => vi.fn())
const letter = vi.hoisted(() => vi.fn())

mockNuxtImport('useApplications', () => () => ({ resubmit }))
mockNuxtImport('useApplicationExtras', () => () => ({ interviews, letters, letter }))
// the service-fee panel has its own suite
mockNuxtImport('useApplicationPayment', () => () => ({
  overview: async () => ({ state: 'NOT_STARTED', required: false, settled: true, canStart: false, amount: 0, currency: 'CNY', payee: '', reference: null, expiresAt: null, secondsLeft: 0, rejectionReason: null }),
}))

beforeEach(() => {
  resubmit.mockReset().mockResolvedValue({})
  interviews.mockReset().mockResolvedValue([])
  letters.mockReset().mockResolvedValue([])
  letter.mockReset()
})

describe('interviewTime', () => {
  it('shows one instant on the Beijing clock and on another clock', () => {
    expect(formatInZone('2026-11-02T07:00:00Z', BEIJING_ZONE, 'en')).toContain('15:00')
    expect(formatInZone('2026-11-02T07:00:00Z', 'Africa/Conakry', 'en')).toContain('07:00')
  })

  it('picks the scheduled interview and ignores cancelled ones', () => {
    expect(currentInterview([{ status: 'CANCELLED' }, { status: 'SCHEDULED' }])).toEqual({ status: 'SCHEDULED' })
    expect(currentInterview([{ status: 'CANCELLED' }])).toBeNull()
  })
})

describe('DocumentsRequiredCard', () => {
  it('shows what Nadoumi wrote, and sends the same application back without paying again', async () => {
    const w = await mountSuspended(DocumentsRequiredCard, { props: { applicationId: 7, note: 'The passport scan is blurred' } })
    expect(w.get('[data-test="action-note"]').text()).toBe('The passport scan is blurred')
    expect(w.text()).toContain('you do not pay again')

    await w.get('[data-test="resubmit"]').trigger('click')
    await flushPromises()
    expect(resubmit).toHaveBeenCalledWith(7)
    expect(w.emitted('resubmitted')).toHaveLength(1)
  })

  it('does not report a send-back that failed', async () => {
    resubmit.mockRejectedValue(new Error('upload the required documents'))
    const w = await mountSuspended(DocumentsRequiredCard, { props: { applicationId: 7, note: null } })
    await w.get('[data-test="resubmit"]').trigger('click')
    await flushPromises()
    expect(w.emitted('resubmitted')).toBeUndefined()
  })
})

describe('ApplicationInterviewCard', () => {
  it('says what to expect while the interview is not scheduled yet', async () => {
    const w = await mountSuspended(ApplicationInterviewCard, { props: { applicationId: 7, status: 'INTERVIEW_REQUIRED' } })
    await flushPromises()
    expect(w.find('[data-test="interview-waiting"]').exists()).toBe(true)
  })

  it('shows nothing for a case that has no interview', async () => {
    const w = await mountSuspended(ApplicationInterviewCard, { props: { applicationId: 7, status: 'UNDER_REVIEW' } })
    await flushPromises()
    expect(w.find('[data-test="interview"]').exists()).toBe(false)
    expect(w.find('[data-test="interview-waiting"]').exists()).toBe(false)
  })

  it('shows the Beijing time and the link of the scheduled interview', async () => {
    interviews.mockResolvedValue([{ id: 1, startsAt: '2030-11-02T07:00:00Z', durationMinutes: 30, platform: 'Zoom', meetingUrl: 'https://meet.example/abc', instructions: 'Bring your passport.', status: 'SCHEDULED' }])
    const w = await mountSuspended(ApplicationInterviewCard, { props: { applicationId: 7, status: 'INTERVIEW_REQUIRED' } })
    await flushPromises()
    expect(w.get('[data-test="interview-beijing"]').text()).toContain('15:00')
    expect(w.get('[data-test="interview-join"]').attributes('href')).toBe('https://meet.example/abc')
    expect(w.text()).toContain('Bring your passport.')
  })
})

describe('AdmissionDocumentsCard', () => {
  it('keeps the letter locked, with no file name and no open button, until the service fee is confirmed', async () => {
    letters.mockResolvedValue([{ kind: 'ADMISSION_LETTER', fileName: null, locked: true, uploadedAt: '2026-11-01T10:00:00' }])
    const w = await mountSuspended(AdmissionDocumentsCard, { props: { applicationId: 7 } })
    await flushPromises()
    expect(w.get('[data-test="letter-ADMISSION_LETTER"]').text()).toContain('Available after you pay')
    expect(w.find('[data-test="open-ADMISSION_LETTER"]').exists()).toBe(false)
    expect(w.get('[data-test="letter-JW202"]').text()).toContain('preparing')
  })

  it('lets the student open a released letter', async () => {
    letters.mockResolvedValue([{ kind: 'JW202', fileName: 'jw202.pdf', locked: false, uploadedAt: '2026-11-01T10:00:00' }])
    letter.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn().mockReturnValue('blob:x'), revokeObjectURL: vi.fn() }))
    const w = await mountSuspended(AdmissionDocumentsCard, { props: { applicationId: 7 } })
    await flushPromises()
    expect(w.get('[data-test="letter-JW202"]').text()).toContain('jw202.pdf')

    await w.get('[data-test="open-JW202"]').trigger('click')
    await flushPromises()
    expect(letter).toHaveBeenCalledWith('JW202')
    expect(open).toHaveBeenCalledWith('blob:x', '_blank', 'noopener')
  })
})
