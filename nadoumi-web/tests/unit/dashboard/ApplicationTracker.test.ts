import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ApplicationTracker from '~/components/dashboard/ApplicationTracker.vue'
import FocusCard from '~/components/dashboard/overview/FocusCard.vue'
import type { StudentApplicationDto } from '~/types/catalog'
import { trackerIndex } from '~/utils/applicationView'

const app = (over: Partial<StudentApplicationDto> = {}): StudentApplicationDto => ({
  id: 7, reference: 'NAD20261024001', applicantId: 1, applicationType: 'PROGRAM_ONLY', programId: 1, scholarshipId: null, intakeId: 1,
  currentStageName: 'Eligibility review', currentStatus: 'SUBMITTED_TO_UNIVERSITY', submittedAt: '2026-10-24T10:00:00', timeline: [],
  title: 'MBBS', universityName: 'Fudan University', intake: 'AUTUMN_SEPTEMBER', applicantName: 'Amina Diallo', paymentNeeded: false, actionNote: null, ...over,
})

describe('trackerIndex', () => {
  it('places each status on the path, and anything else off it', () => {
    expect(trackerIndex('IN_REVIEW')).toBe(1)
    expect(trackerIndex('DOCUMENTS_REQUIRED')).toBe(1)
    expect(trackerIndex('OFFER')).toBe(4)
    expect(trackerIndex('CLOSED_SUCCESS')).toBe(7)
    expect(trackerIndex('CLOSED_UNSUCCESSFUL')).toBe(-1)
    expect(trackerIndex(null)).toBe(-1)
  })
})

describe('ApplicationTracker', () => {
  it('marks earlier steps done, the current step current and the rest to do', async () => {
    const w = await mountSuspended(ApplicationTracker, { props: { status: 'SUBMITTED_TO_UNIVERSITY' } })
    const states = w.findAll('[data-state]').map(el => el.attributes('data-state'))
    expect(states).toEqual(['done', 'done', 'current', 'todo', 'todo', 'todo', 'todo', 'todo'])
    expect(w.find('[aria-current="step"]').text()).toContain('Sent to university')
  })

  it('says so, in red, when the application was unsuccessful', async () => {
    const w = await mountSuspended(ApplicationTracker, { props: { status: 'CLOSED_UNSUCCESSFUL' } })
    expect(w.find('[data-test="tracker-unsuccessful"]').classes()).toContain('text-red-600')
    expect(w.findAll('[data-state="todo"]')).toHaveLength(8)
  })
})

describe('dashboard FocusCard', () => {
  it('follows the latest submitted application with its tracker and what happens next', async () => {
    const w = await mountSuspended(FocusCard, { props: { latest: app(), draft: null, pending: false } })
    expect(w.text()).toContain('NAD20261024001')
    expect(w.text()).toContain('Fudan University')
    expect(w.find('[data-test="tracker"]').exists()).toBe(true)
    expect(w.text()).toContain('sent to the university')
  })

  it('points at the unfinished application when nothing is submitted', async () => {
    const w = await mountSuspended(FocusCard, { props: { latest: null, draft: app({ currentStatus: 'DRAFT' }), pending: false } })
    expect(w.find('[data-test="focus-draft"]').exists()).toBe(true)
    expect(w.find('a[href="/apply/7"]').exists()).toBe(true)
  })

  it('invites the student to apply when there is nothing at all', async () => {
    const w = await mountSuspended(FocusCard, { props: { latest: null, draft: null, pending: false } })
    expect(w.find('[data-test="focus-empty"]').exists()).toBe(true)
  })
})
