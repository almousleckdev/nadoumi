import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import ApplyStartStep from '~/components/apply/ApplyStartStep.vue'
import type { ApplyTarget } from '~/types/apply'
import type { ApplicantDto, StudentApplicationDto } from '~/types/catalog'

const api = vi.hoisted(() => ({ start: vi.fn(), createForOther: vi.fn() }))
vi.mock('~/composables/useApplications', () => ({ useApplications: () => ({ start: api.start }) }))
mockNuxtImport('useApplicant', () => () => ({ createForOther: api.createForOther }))

const student = (id: number, given: string) => ({ id, givenName: given, familyName: 'Diallo', email: `${given}@example.test` }) as ApplicantDto
const programme: ApplyTarget = {
  kind: 'programme', id: 5, slug: 'mbbs-fudan', title: 'MBBS', universityName: 'Fudan University',
  intakes: [{ id: 9, term: 'AUTUMN_SEPTEMBER' }, { id: 10, term: 'SPRING_MARCH' }],
}
const scholarship: ApplyTarget = { kind: 'scholarship', id: 7, slug: 'csc-master', title: 'CSC Master' }

beforeEach(() => Object.values(api).forEach(fn => fn.mockReset()))

async function mountStep(target: ApplyTarget, applications: StudentApplicationDto[] = []) {
  const w = await mountSuspended(ApplyStartStep, {
    props: { target, applicants: [student(3, 'Amina'), student(4, 'Moussa')], applications },
  })
  await flushPromises()
  return w
}

describe('application start step', () => {
  it('names the university for a programme and asks for an intake before starting', async () => {
    const w = await mountStep(programme)

    expect(w.find('[data-test="target-university"]').text()).toBe('Fudan University')
    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()
    expect(api.start).not.toHaveBeenCalled() // no intake chosen yet
  })

  it('starts a programme application for the chosen student and intake', async () => {
    api.start.mockResolvedValue({ id: 42 })
    const w = await mountStep(programme)

    await w.find('[data-test="student-4"] input').setValue(true)
    await w.find('select').setValue('10')
    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()

    expect(api.start).toHaveBeenCalledWith({ applicantId: 4, applicationType: 'PROGRAM_ONLY', programId: 5, intakeId: 10 })
    expect(w.emitted('started')![0]).toEqual([42])
  })

  it('never shows a university for a scholarship and starts a scholarship-only application', async () => {
    api.start.mockResolvedValue({ id: 43 })
    const w = await mountStep(scholarship)

    expect(w.find('[data-test="target-university"]').exists()).toBe(false)
    expect(w.find('select').exists()).toBe(false) // a scholarship has no intake
    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()

    expect(api.start).toHaveBeenCalledWith({ applicantId: 3, applicationType: 'SCHOLARSHIP_ONLY', scholarshipId: 7 })
  })

  it('continues an existing draft instead of starting a duplicate', async () => {
    const draft = { id: 11, applicantId: 3, applicationType: 'SCHOLARSHIP_ONLY', programId: null, scholarshipId: 7, currentStatus: 'DRAFT' } as StudentApplicationDto
    const w = await mountStep(scholarship, [draft])

    await w.find('[data-test="start"]').trigger('click')
    await flushPromises()

    expect(api.start).not.toHaveBeenCalled()
    expect(w.emitted('started')![0]).toEqual([11])
  })

  it('adds another student by name and selects them to apply for', async () => {
    api.createForOther.mockResolvedValue(student(9, 'Aminata'))
    const w = await mountStep(scholarship)

    await w.find('[data-test="add-student"]').trigger('click')
    const fields = w.findAll('[data-test="new-student"] input')
    await fields[0]!.setValue('Aminata')
    await fields[1]!.setValue('Diallo')
    await w.find('[data-test="new-student-save"]').trigger('click')
    await flushPromises()

    expect(api.createForOther).toHaveBeenCalledWith({ givenName: 'AMINATA', familyName: 'DIALLO' })
    expect(w.emitted('student-added')![0]![0]).toMatchObject({ id: 9 })
    await w.setProps({ applicants: [student(3, 'Amina'), student(4, 'Moussa'), student(9, 'Aminata')] }) // the page adds the new student to its list
    expect((w.find('[data-test="student-9"] input').element as HTMLInputElement).checked).toBe(true)
  })

  it('asks for both names before adding a student', async () => {
    const w = await mountStep(scholarship)

    await w.find('[data-test="add-student"]').trigger('click')
    await w.find('[data-test="new-student-save"]').trigger('click')
    await flushPromises()

    expect(api.createForOther).not.toHaveBeenCalled()
  })
})
