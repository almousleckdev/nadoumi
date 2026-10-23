import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import ApplyReviewStep from '~/components/apply/ApplyReviewStep.vue'
import type { ApplyStep } from '~/constants/apply'

const api = vi.hoisted(() => ({ formPreview: vi.fn(), submit: vi.fn() }))
vi.mock('~/composables/useApplications', () => ({ useApplications: () => api }))
mockNuxtImport('useApplicationPayment', () => () => ({
  overview: async () => ({ state: 'NONE', required: true, settled: false, canStart: true, amount: 300, currency: 'CNY', payee: '', reference: null, expiresAt: null, secondsLeft: 0, rejectionReason: null }),
  documents: async () => [],
}))

beforeEach(() => Object.values(api).forEach(fn => fn.mockReset()))

async function mountReview(openSteps: ApplyStep[], feeSettled: boolean) {
  const w = await mountSuspended(ApplyReviewStep, { props: { applicationId: 7, openSteps, feeSettled } })
  await flushPromises()
  return w
}

describe('application review step', () => {
  it('locks Submit and names the open steps while anything is missing', async () => {
    const w = await mountReview(['identity', 'documents'], true)

    expect(w.find('[data-test="submit"]').attributes('disabled')).toBeDefined()
    expect(w.find('[data-test="review-status"]').text()).toContain('Photo and passport')
    expect(w.find('[data-test="review-status"]').text()).toContain('Documents')
    await w.find('[data-test="open-identity"]').trigger('click')
    expect(w.emitted('goto')![0]).toEqual(['identity'])
  })

  it('locks Submit until the fee is paid, and says why', async () => {
    const w = await mountReview([], false)

    expect(w.find('[data-test="submit"]').attributes('disabled')).toBeDefined()
    expect(w.find('[data-test="submit-hint"]').text()).toContain('application fee')
  })

  it('submits when every step is done and the fee is paid', async () => {
    api.submit.mockResolvedValue({ id: 7, reference: 'NAD20261023001' })
    const w = await mountReview([], true)

    await w.find('[data-test="submit"]').trigger('click')
    await flushPromises()

    expect(api.submit).toHaveBeenCalledWith(7)
    expect(w.emitted('submitted')![0]![0]).toMatchObject({ reference: 'NAD20261023001' })
  })

  it('shows the server reason when Submit is refused', async () => {
    api.submit.mockRejectedValue({ statusCode: 409, data: { detail: 'finish the applicant\'s profile before submitting: FAMILY' } })
    const w = await mountReview([], true)

    await w.find('[data-test="submit"]').trigger('click')
    await flushPromises()

    expect(w.text()).toContain("The student's profile is not finished")
    expect(w.emitted('submitted')).toBeUndefined()
  })

  it('previews the application form exactly as it will be sent', async () => {
    const create = vi.fn(() => 'blob:form-preview')
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: create, revokeObjectURL: vi.fn() }))
    api.formPreview.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))
    const w = await mountReview([], true)

    await w.find('[data-test="preview-form"]').trigger('click')
    await flushPromises()

    expect(api.formPreview).toHaveBeenCalledWith(7)
    expect(document.body.querySelector('[data-test="preview-pdf"]')?.getAttribute('src')).toBe('blob:form-preview')
    vi.unstubAllGlobals()
  })
})
