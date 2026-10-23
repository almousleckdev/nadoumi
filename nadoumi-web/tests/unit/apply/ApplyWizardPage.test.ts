import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import ApplyWizard from '~/pages/apply/[[id]].vue'

const state = vi.hoisted(() => ({ params: {} as Record<string, string>, query: {} as Record<string, string> }))
const api = vi.hoisted(() => ({
  listMine: vi.fn(), get: vi.fn(), update: vi.fn(), completeOnboarding: vi.fn(), onboardingStatus: vi.fn(),
  list: vi.fn(), getApplication: vi.fn(), checklist: vi.fn(), start: vi.fn(),
  load: vi.fn(), readiness: vi.fn(), navigateTo: vi.fn(),
}))

mockNuxtImport('useRoute', () => () => ({ params: state.params, query: state.query, path: '/apply' }))
mockNuxtImport('navigateTo', () => api.navigateTo)
mockNuxtImport('useApplyTarget', () => () => ({ load: api.load }))
mockNuxtImport('useApplicant', () => () => ({
  listMine: api.listMine, get: api.get, update: api.update, completeOnboarding: api.completeOnboarding,
  onboardingStatus: api.onboardingStatus,
}))
mockNuxtImport('useApplicantFamily', () => () => ({ readiness: api.readiness }))
mockNuxtImport('useApplicantSignature', () => () => ({ status: async () => ({ signed: true, signedAt: null }) }))
mockNuxtImport('useApplicationPayment', () => () => ({
  overview: async () => ({ state: 'NONE', required: true, settled: false, canStart: true, amount: 300, currency: 'CNY', payee: '', reference: null, expiresAt: null, secondsLeft: 0, rejectionReason: null }),
}))
vi.mock('~/composables/useApplications', () => ({
  useApplications: () => ({ list: api.list, get: api.getApplication, checklist: api.checklist, start: api.start }),
}))

// the step bodies have their own suites; here they only mark which one is showing
const stub = (id: string) => ({ template: `<div data-test="${id}" />` })
const stubs = {
  ProfileForm: stub('step-profile'), PhotoUploadCard: stub('step-photo'), PassportUploadCard: stub('step-passport'),
  ResidenceSection: stub('step-residence'), PersonalDetailsSection: stub('step-personal'), ContactSection: stub('step-contact'),
  EducationSection: stub('step-education'), WorkSection: stub('step-work'), FamilyMemberSection: stub('step-family'),
  PersonSection: stub('step-person'), InterestsSection: stub('step-interests'), DocumentChecklistCard: stub('step-documents'),
  SignatureCard: stub('step-signature'), ApplyReviewStep: stub('step-review'),
}

const programme = { kind: 'programme', id: 5, slug: 'mbbs-fudan', title: 'MBBS', universityName: 'Fudan University', intakes: [{ id: 9, term: 'AUTUMN_SEPTEMBER' }] }
const draft = { id: 42, reference: 'NAD20261023001', applicantId: 3, applicationType: 'PROGRAM_ONLY', programId: 5, scholarshipId: null, intakeId: 9, currentStatus: 'DRAFT' }
const complete = (sections: string[]) => ({ onboarded: false, ready: false, sections: sections.map(key => ({ key, complete: true })) })
const ALL_SECTIONS = ['PROFILE', 'PHOTO', 'PASSPORT', 'EDUCATION', 'INTERESTS', 'LOCATION', 'CONTACT']

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  state.params = {}
  state.query = {}
  api.listMine.mockResolvedValue([{ id: 3, givenName: 'Amina', familyName: 'Diallo', email: 'a@example.test', onboardingComplete: true }])
  api.get.mockResolvedValue({ id: 3, givenName: 'Amina', familyName: 'Diallo', email: 'a@example.test', onboardingComplete: true })
  api.list.mockResolvedValue([])
  api.getApplication.mockResolvedValue(draft)
  api.load.mockResolvedValue(programme)
  api.checklist.mockResolvedValue([])
  api.onboardingStatus.mockResolvedValue(complete(ALL_SECTIONS))
  api.readiness.mockResolvedValue({ missing: [] })
})

async function mountWizard() {
  const w = await mountSuspended(ApplyWizard, { global: { stubs } })
  await flushPromises()
  return w
}

describe('application wizard', () => {
  it('opens on what and who the application is for, with every other step locked until it is saved', async () => {
    state.query = { programme: 'mbbs-fudan' }
    const w = await mountWizard()

    expect(w.find('[data-test="target-title"]').text()).toBe('MBBS')
    expect(w.find('[data-test="apply-context"]').text()).not.toContain('NAD') // no reference before it is saved
    expect(w.find('[data-test="next"]').exists()).toBe(false)
    await w.findAll('[data-test="step"]')[3]!.trigger('click') // "Address and contact"
    expect(w.find('[data-test="target-title"]').exists()).toBe(true) // still on the first step
  })

  it('shows the reference and the student once the application exists, and resumes where it stopped', async () => {
    state.params = { id: '42' }
    api.onboardingStatus.mockResolvedValue(complete(['PROFILE', 'PHOTO', 'PASSPORT'])) // education is the first open step
    const w = await mountWizard()

    expect(w.find('[data-test="context-reference"]').text()).toBe('NAD20261023001')
    expect(w.find('[data-test="apply-context"]').text()).toContain('Amina Diallo')
    // the address step comes before education, and the server reports LOCATION and CONTACT as not done
    expect(w.find('[data-test="step-residence"]').exists()).toBe(true)
    expect(w.find('[data-test="step-education"]').exists()).toBe(false)
  })

  it('goes to the step in the address, and holds Next until the server says that step is done', async () => {
    state.params = { id: '42' }
    state.query = { step: 'family' }
    api.readiness.mockResolvedValue({ missing: ['FAMILY'] })
    const w = await mountWizard()

    expect(w.find('[data-test="step-family"]').exists()).toBe(true)
    expect(w.find('[data-test="next"]').attributes('disabled')).toBeDefined()
  })

  it('lets the student move on from an optional step without filling it in', async () => {
    state.params = { id: '42' }
    state.query = { step: 'work' }
    const w = await mountWizard()

    expect(w.find('[data-test="step-work"]').exists()).toBe(true)
    expect(w.find('[data-test="next"]').attributes('disabled')).toBeUndefined()
  })

  it('completes the student onboarding on the server before the review, so the application can be submitted', async () => {
    state.params = { id: '42' }
    state.query = { step: 'review' }
    api.get.mockResolvedValue({ id: 3, givenName: 'Moussa', familyName: 'Diallo', email: 'm@example.test', onboardingComplete: false })
    api.onboardingStatus.mockResolvedValue({ onboarded: false, ready: true, sections: ALL_SECTIONS.map(key => ({ key, complete: true })) })
    api.completeOnboarding.mockResolvedValue({ onboarded: true })
    await mountWizard()

    expect(api.completeOnboarding).toHaveBeenCalledWith(3)
  })

  it('tells an agent whom the forms mean when they say "you"', async () => {
    state.params = { id: '42' }
    state.query = { step: 'education' }
    useState('nad-session-applicants').value = [{ applicantId: 3, accessRole: 'AGENT', capabilities: [] }]
    const w = await mountWizard()

    expect(w.find('[data-test="on-behalf"]').text()).toContain('Amina Diallo')
    useState('nad-session-applicants').value = [{ applicantId: 3, accessRole: 'OWNER', capabilities: [] }]
    const own = await mountWizard()
    expect(own.find('[data-test="on-behalf"]').exists()).toBe(false) // a student completing their own details needs no note
  })

  it('says so when the application cannot be found', async () => {
    state.params = { id: '99' }
    api.getApplication.mockRejectedValue({ statusCode: 403 })
    const w = await mountWizard()

    expect(w.text()).toContain('This section could not be loaded')
  })

  it('asks what to apply for when none is given', async () => {
    api.load.mockResolvedValue(null)
    const w = await mountWizard()

    expect(w.text()).toContain('Choose a programme or scholarship')
  })
})
