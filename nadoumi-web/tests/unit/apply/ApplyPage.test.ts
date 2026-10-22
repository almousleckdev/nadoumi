import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import ApplyPage from '~/pages/dashboard/apply.vue'

const state = vi.hoisted(() => ({ query: {} as Record<string, string> }))
const api = vi.hoisted(() => ({ load: vi.fn(), list: vi.fn(), start: vi.fn(), listMine: vi.fn(), navigateTo: vi.fn() }))

mockNuxtImport('useRoute', () => () => ({ query: state.query, path: '/dashboard/apply', params: {} }))
mockNuxtImport('useApplyTarget', () => () => ({ load: api.load }))
mockNuxtImport('useApplicant', () => () => ({ listMine: api.listMine }))
mockNuxtImport('navigateTo', () => api.navigateTo)
vi.mock('~/composables/useApplications', () => ({ useApplications: () => ({ list: api.list, start: api.start }) }))

const student = (id: number, given: string) => ({ id, givenName: given, familyName: 'Diallo', email: `${given}@example.test` })
const programme = {
  kind: 'programme', id: 5, slug: 'mbbs-fudan', title: 'MBBS', universityName: 'Fudan University',
  intakes: [{ id: 9, term: 'AUTUMN_SEPTEMBER' }, { id: 10, term: 'SPRING_MARCH' }],
}

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  api.listMine.mockResolvedValue([student(3, 'Amina'), student(4, 'Moussa')])
  api.list.mockResolvedValue([])
})

async function mountPage(query: Record<string, string>) {
  state.query = query
  const w = await mountSuspended(ApplyPage)
  await flushPromises()
  return w
}

describe('apply page', () => {
  it('asks what to apply for when no opportunity is given', async () => {
    const w = await mountPage({})

    expect(w.find('[data-test="start"]').exists()).toBe(false)
    expect(w.text()).toContain('Choose a programme or scholarship')
  })

  it('names the university for a programme and asks for an intake before starting', async () => {
    api.load.mockResolvedValue(programme)
    const w = await mountPage({ programme: 'mbbs-fudan' })

    expect(w.find('[data-test="target-university"]').text()).toBe('Fudan University')
    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()
    expect(api.start).not.toHaveBeenCalled() // no intake chosen yet
  })

  it('starts a programme application for the chosen student and intake, then opens it', async () => {
    api.load.mockResolvedValue(programme)
    api.start.mockResolvedValue({ id: 42 })
    const w = await mountPage({ programme: 'mbbs-fudan' })

    await w.find('[data-test="student-4"] input').setValue(true)
    await w.find('select').setValue('10')
    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()

    expect(api.start).toHaveBeenCalledWith({ applicantId: 4, applicationType: 'PROGRAM_ONLY', programId: 5, intakeId: 10 })
    expect(api.navigateTo).toHaveBeenCalledWith('/dashboard/applications/42')
  })

  it('never shows a university for a scholarship and starts a scholarship-only application', async () => {
    api.load.mockResolvedValue({ kind: 'scholarship', id: 7, slug: 'csc-master', title: 'CSC Master' })
    api.start.mockResolvedValue({ id: 43 })
    const w = await mountPage({ scholarship: 'csc-master' })

    expect(w.find('[data-test="target-university"]').exists()).toBe(false)
    expect(w.find('select').exists()).toBe(false) // a scholarship has no intake
    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()

    expect(api.start).toHaveBeenCalledWith({ applicantId: 3, applicationType: 'SCHOLARSHIP_ONLY', scholarshipId: 7 })
  })

  it('offers to continue an existing draft instead of starting a duplicate', async () => {
    api.load.mockResolvedValue({ kind: 'scholarship', id: 7, slug: 'csc-master', title: 'CSC Master' })
    api.list.mockResolvedValue([{
      id: 11, applicantId: 3, applicationType: 'SCHOLARSHIP_ONLY', programId: null, scholarshipId: 7, intakeId: null,
      currentStageName: 'Draft', currentStatus: 'DRAFT', submittedAt: null, timeline: [],
    }])
    const w = await mountPage({ scholarship: 'csc-master' })

    expect(w.find('[data-test="continue-draft"]').exists()).toBe(true)
    expect(w.find('[data-test="start"]').exists()).toBe(false)
  })

  it('says so when the opportunity cannot be found', async () => {
    api.load.mockResolvedValue(null)
    const w = await mountPage({ programme: 'gone' })

    expect(w.text()).toContain('could not find')
  })
})
