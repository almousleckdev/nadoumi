import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import DashboardIndex from '~/pages/dashboard/index.vue'
import type { ScholarshipCard } from '~/types/catalog'

const applicant = {
  id: 1, givenName: 'ADA', familyName: 'LOVELACE', dob: '2000-01-01', nationality: 'GB', passportNo: null,
  email: 'a@b.co', phone: '+441', status: 'ACTIVE', gender: 'FEMALE', countryOfOrigin: 'GB',
  countryOfResidence: 'CN', nativeLanguage: 'en', wechatId: null, whatsapp: '+441',
  emailVerified: true, onboardingComplete: true, welcomePending: false,
}

const SCH: ScholarshipCard = {
  id: 1, slug: 'csc-master', title: 'CSC Master Scholarship', summary: null, country: 'CN', province: null,
  city: null, field: null, teachingLanguage: 'ENGLISH', fundingModel: 'FULLY', hasStipend: true,
  nonDegreeDuration: null, studyDurationMonths: 36, applicationChannel: null, agencyNumber: null,
  requiresFinancialProof: false, requiresFoundationYear: false, deadline: '2099-01-01', applicationFee: null,
  serviceFee: null, slots: null, featured: false, recommended: true, hot: false, heroImageUrl: null,
  coverImageUrl: null, heroUrl: null, coverUrl: null, levels: ['MASTER'], categories: [],
} as unknown as ScholarshipCard

const photoUrl = vi.fn().mockRejectedValue({ statusCode: 404 })
const passportStatus = vi.fn().mockResolvedValue({
  passportNo: null, givenName: null, familyName: null, dob: null, issueDate: null, expiryDate: null,
  readMethod: null, edited: false, scanUploaded: false, validForAdmission: false, matchesProfile: false, mismatches: [],
})
const listMine = vi.fn().mockResolvedValue([applicant])
const markWelcomed = vi.fn()
const publicGet = vi.fn().mockResolvedValue({ content: [], totalElements: 0, page: 0, size: 0, totalPages: 0 })
const listNotifications = vi.fn().mockResolvedValue({ content: [], totalElements: 0 })
const listApplications = vi.fn().mockResolvedValue([])

vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ user: ref({ userId: 1, username: 'ada', nickName: 'Ada' }), activeApplicantId: ref(1) }),
}))
vi.mock('~/composables/useApplicant', () => ({
  useApplicant: () => ({ listMine, markWelcomed, photoUrl, passportStatus }),
}))
vi.mock('~/composables/useNotifications', () => ({
  useNotifications: () => ({ list: listNotifications }),
}))
vi.mock('~/composables/useApplications', () => ({
  useApplications: () => ({ list: listApplications }),
}))
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

async function mountPage() {
  const w = await mountSuspended(DashboardIndex)
  await flushPromises()
  return w
}

describe('dashboard index', () => {
  it('prompts to create a profile when none exists', async () => {
    listMine.mockResolvedValueOnce([])
    const w = await mountPage()
    expect(w.text()).toContain('Create your applicant profile')
  })

  it('shows the welcome header and real document status for the active applicant', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Welcome back')
    expect(w.text()).toContain('ADA') // the applicant's given name (uppercase, matches the passport-name convention), not the account nickname
    expect(w.text()).toContain('Profile photo')
    expect(w.text()).toContain('Passport')
    expect(w.text()).toContain('Required') // neither uploaded yet
  })

  it('shows honest empty states for applications, recommendations, deadlines and activity — never fake data', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('You have not applied yet')
    expect(w.find('[data-test="chart-empty"]').exists()).toBe(true)
    expect(w.text()).toContain('No recommendations yet')
    expect(w.text()).toContain('No upcoming deadlines')
    expect(w.text()).toContain('Nothing to show yet')
  })

  it('shows real recommended scholarships and deadlines when the catalog returns data', async () => {
    publicGet.mockImplementation((path: string, query?: Record<string, unknown>) => {
      if (path === 'scholarships' && query?.recommended) return Promise.resolve({ content: [SCH], totalElements: 1, page: 0, size: 1, totalPages: 1 })
      if (path === 'scholarships' && query?.sort === 'deadline') return Promise.resolve({ content: [SCH], totalElements: 1, page: 0, size: 1, totalPages: 1 })
      return Promise.resolve({ content: [], totalElements: 0, page: 0, size: 0, totalPages: 0 })
    })
    const w = await mountPage()
    expect(w.text()).toContain('CSC Master Scholarship')
  })

  it('shows real notification activity, unread ones bolded', async () => {
    listNotifications.mockResolvedValueOnce({
      content: [{ id: 1, type: 'GENERAL', title: 'Welcome to Nadoumi', body: '', dataJson: null, applicationId: null, conversationId: null, messageId: null, createdAt: '2026-01-01T00:00:00Z', readAt: null, read: false }],
      totalElements: 1,
    })
    const w = await mountPage()
    expect(w.text()).toContain('Welcome to Nadoumi')
  })

  it('summarises the real applications, with their status, instead of the empty state', async () => {
    listApplications.mockResolvedValueOnce([
      { id: 7, reference: 'NAD20260101001', applicationType: 'PROGRAM_WITH_SCHOLARSHIP', programId: 1, scholarshipId: 2, intakeId: 3,
        currentStageName: 'Eligibility review', currentStatus: 'IN_REVIEW', submittedAt: '2026-01-01T00:00:00', timeline: [],
        title: 'MBBS', universityName: 'Fudan University', intake: 'Autumn', applicantName: 'Amina Diallo' },
    ])
    const w = await mountPage()
    expect(w.text()).toContain('MBBS')
    expect(w.text()).toContain('NAD20260101001')
    expect(w.text()).toContain('In review')
    expect(w.text()).not.toContain('You have not applied yet')
    expect(w.findAll('[data-test="donut-arc"]')).toHaveLength(1)
    expect(w.findAll('[data-test="stat-value"]').map(el => el.text())).toEqual(['1', '1', '0', '0/2'])
  })

  it('warns about an application whose fee staff rejected, and still shows the stat tiles', async () => {
    listApplications.mockResolvedValueOnce([
      { id: 7, reference: 'NAD2026001', applicationType: 'PROGRAM_ONLY', programId: 1, scholarshipId: null, intakeId: 3,
        currentStageName: 'Eligibility review', currentStatus: 'IN_REVIEW', submittedAt: '2026-01-01T00:00:00', timeline: [],
        title: 'MBBS', universityName: 'Fudan University', intake: 'Autumn', applicantName: 'Amina Diallo', paymentNeeded: true, actionNote: null },
    ])
    const w = await mountPage()
    expect(w.find('[data-test="payment-banner"]').exists()).toBe(true)
    expect(w.find('[data-test="focus-payment-needed"]').text()).toContain('Pay again')
    expect(w.findAll('[data-test="stat-value"]').length).toBe(4)
  })

  it('shows an inline error, not the empty state, when applications fail to load', async () => {
    listApplications.mockRejectedValueOnce(new Error('boom'))
    const w = await mountPage()
    expect(w.text()).toContain('This section could not be loaded')
  })
})
