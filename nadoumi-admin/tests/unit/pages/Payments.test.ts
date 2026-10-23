import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listPayments: vi.fn(),
  getPayment: vi.fn(),
  confirmPayment: vi.fn(),
  rejectPayment: vi.fn(),
  fetchPaymentProof: vi.fn(),
}))
vi.mock('@/api/payment', async (orig) => ({ ...(await orig<object>()), ...api }))

const confirmMock = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: confirmMock }) }))

import Payments from '@/views/payments/index.vue'
import PaymentDrawer from '@/views/payments/PaymentDrawer.vue'
import { useUserStore } from '@/stores/user'
import type { StaffPayment } from '@/api/payment'

const PAY: StaffPayment = {
  id: 5, applicationId: 9, applicationRef: 'APP-9', applicantName: 'Amina Diallo', kind: 'APPLICATION_FEE',
  amount: 500, currency: 'CNY', reference: 'NAD-AB12CD34', status: 'PROOF_SUBMITTED', studentTxnNo: 'TXN-777',
  hasProof: true, createdAt: '2026-10-01T08:00:00Z', submittedAt: '2026-10-02T08:00:00Z',
  expiresAt: '2026-10-10T08:00:00Z', verifiedAt: null, rejectionReason: null,
}
const page = { content: [PAY], page: 0, size: 20, totalElements: 1, totalPages: 1 }

function mountDrawer() {
  return mount(PaymentDrawer, {
    ...mountOpts(), props: { modelValue: true, paymentId: 5 }, attachTo: document.body,
  })
}
const find = (sel: string) => document.body.querySelector(sel) as HTMLElement | null

beforeEach(() => {
  document.body.innerHTML = ''
  setActivePinia(createPinia())
  useUserStore().permissions = ['nad:payment:list', 'nad:payment:query', 'nad:payment:verify']
  Object.values(api).forEach(fn => fn.mockReset())
  confirmMock.mockReset().mockResolvedValue(true)
  api.listPayments.mockResolvedValue(page)
  api.getPayment.mockResolvedValue(PAY)
  api.confirmPayment.mockResolvedValue({ ...PAY, status: 'CONFIRMED' })
  api.rejectPayment.mockResolvedValue({ ...PAY, status: 'REJECTED' })
  api.fetchPaymentProof.mockResolvedValue(new Blob(['x'], { type: 'application/pdf' }))
  URL.createObjectURL = vi.fn(() => 'blob:proof')
  URL.revokeObjectURL = vi.fn()
})

describe('Payments list', () => {
  it('renders rows and queries with the chosen status from the first page', async () => {
    const w = mount(Payments, mountOpts())
    await flushPromises()
    expect(api.listPayments).toHaveBeenCalledWith({ q: undefined, status: undefined, page: 0, size: 20 })
    expect(w.text()).toContain('NAD-AB12CD34')
    expect(w.text()).toContain('Amina Diallo')
    expect(w.text()).toContain('¥500')
    expect(w.text()).toContain('Proof submitted')

    const vm = w.vm as unknown as { filters: { status: string }, reload: () => void }
    vm.filters.status = 'CONFIRMED'
    vm.reload()
    await flushPromises()
    expect(api.listPayments).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'CONFIRMED', page: 0 }))
  })
})

describe('PaymentDrawer', () => {
  it('offers Confirm and Reject for PROOF_SUBMITTED with the verify permission', async () => {
    mountDrawer()
    await flushPromises()
    expect(find('[data-test="confirm-open"]')).not.toBeNull()
    expect(find('[data-test="reject-open"]')).not.toBeNull()
  })

  it('hides the decision buttons without the verify permission', async () => {
    useUserStore().permissions = ['nad:payment:query']
    mountDrawer()
    await flushPromises()
    expect(find('[data-test="confirm-open"]')).toBeNull()
    expect(find('[data-test="reject-open"]')).toBeNull()
  })

  it('hides the decision buttons once the payment is no longer awaiting review', async () => {
    api.getPayment.mockResolvedValue({ ...PAY, status: 'CONFIRMED' })
    mountDrawer()
    await flushPromises()
    expect(find('[data-test="confirm-open"]')).toBeNull()
  })

  it('states amount, reference and student in the confirm dialog, then confirms', async () => {
    const w = mountDrawer()
    await flushPromises()
    find('[data-test="confirm-open"]')!.click()
    await flushPromises()
    const msg = confirmMock.mock.calls[0][0].message as string
    expect(msg).toContain('¥500')
    expect(msg).toContain('NAD-AB12CD34')
    expect(msg).toContain('Amina Diallo')
    expect(api.confirmPayment).toHaveBeenCalledWith(5)
    expect(w.emitted('decided')).toBeTruthy()
  })

  it('requires a reason of at least 3 characters before rejecting', async () => {
    const w = mountDrawer()
    await flushPromises()
    find('[data-test="reject-open"]')!.click()
    await flushPromises()
    const input = find('textarea[data-test="reject-reason"]') as HTMLTextAreaElement
    input.value = 'ab'
    input.dispatchEvent(new Event('input'))
    await flushPromises()
    const btn = find('[data-test="reject-confirm"]') as HTMLButtonElement
    expect(btn.disabled).toBe(true)
    expect(find('[data-test="reject-hint"]')).not.toBeNull()
    expect(api.rejectPayment).not.toHaveBeenCalled()

    input.value = 'Blurry screenshot'
    input.dispatchEvent(new Event('input'))
    await flushPromises()
    expect((find('[data-test="reject-confirm"]') as HTMLButtonElement).disabled).toBe(false)
    find('[data-test="reject-confirm"]')!.click()
    await flushPromises()
    expect(api.rejectPayment).toHaveBeenCalledWith(5, 'Blurry screenshot')
    expect(w.emitted('decided')).toBeTruthy()
  })

  it('shows the server detail and refreshes when confirm hits a 409', async () => {
    const spy = vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: vi.fn() }))
    api.confirmPayment.mockRejectedValue({ response: { status: 409, data: { detail: 'Already decided by someone else' } } })
    const w = mountDrawer()
    await flushPromises()
    find('[data-test="confirm-open"]')!.click()
    await flushPromises()
    expect(spy).toHaveBeenCalledWith('Already decided by someone else')
    expect(api.getPayment).toHaveBeenCalledTimes(2)
    expect(w.emitted('decided')).toBeTruthy()
    expect(w.emitted('update:modelValue')).toBeFalsy()
  })

  it('shows a PDF proof as an open-file link and revokes the object URL on unmount', async () => {
    const w = mountDrawer()
    await flushPromises()
    expect(find('[data-test="proof-file"]')).not.toBeNull()
    w.unmount()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:proof')
  })
})
