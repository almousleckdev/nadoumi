import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ContactSection from '~/components/applicant/ContactSection.vue'

const { api } = vi.hoisted(() => ({
  api: {
    listContacts: vi.fn(), addContact: vi.fn(), updateContact: vi.fn(), deleteContact: vi.fn(),
  },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const row = { id: 1, relation: 'GUARDIAN', name: 'Jane Doe', email: 'jane@example.com', phone: '+8613800000000' }

async function mountSection() {
  const w = await mountSuspended(ContactSection, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}

beforeEach(() => {
  api.listContacts.mockReset().mockResolvedValue([row])
  api.addContact.mockReset().mockResolvedValue(row)
  api.updateContact.mockReset().mockResolvedValue(row)
  api.deleteContact.mockReset().mockResolvedValue(undefined)
})

describe('ContactSection', () => {
  it('renders existing contacts', async () => {
    const w = await mountSection()
    expect(w.text()).toContain('Jane Doe')
  })

  it('adds a new contact and reports the change', async () => {
    api.listContacts.mockResolvedValueOnce([])
    const w = await mountSection()

    await w.find('[data-test="add"]').trigger('click')
    await w.find('#contact-relation').setValue('EMERGENCY')
    await w.find('#contact-name').setValue('John Smith')
    await w.find('#contact-phone').setValue('+8613900000000')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.addContact).toHaveBeenCalledWith(1, expect.objectContaining({ name: 'John Smith' }))
    expect(w.emitted('changed')).toBeTruthy()
  })

  it('removes a contact after confirm', async () => {
    const w = await mountSection()

    await w.find('[data-test="remove-1"]').trigger('click')
    await w.vm.$nextTick()
    const confirm = document.querySelector<HTMLElement>('[data-test="confirm-remove"]')
    expect(confirm).not.toBeNull()
    confirm!.click()
    await flushPromises()

    expect(api.deleteContact).toHaveBeenCalledWith(1, 1)
    expect(w.emitted('changed')).toBeTruthy()
    w.unmount()
  })
})
