import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import DeleteWithReasonDialog from '@/components/ui/DeleteWithReasonDialog.vue'
import { mountOpts } from '../helpers'

async function mountDialog() {
  const w = mount(DeleteWithReasonDialog, {
    props: { modelValue: true, title: 'Delete application', message: 'Delete NAD2026001?' },
    global: { ...mountOpts().global, stubs: { teleport: true } },
    attachTo: document.body,
  })
  await nextTick()
  return w
}

describe('DeleteWithReasonDialog', () => {
  it('keeps Delete disabled until a reason of five characters is written, then emits it trimmed', async () => {
    const w = await mountDialog()
    const confirm = () => document.body.querySelector('[data-test="delete-confirm"]') as HTMLButtonElement
    const input = () => document.body.querySelector('[data-test="delete-reason"]') as HTMLTextAreaElement

    expect(confirm().disabled).toBe(true)
    input().value = 'oops'
    input().dispatchEvent(new Event('input'))
    await nextTick()
    expect(confirm().disabled).toBe(true)

    input().value = '  created while testing  '
    input().dispatchEvent(new Event('input'))
    await nextTick()
    expect(confirm().disabled).toBe(false)
    confirm().click()
    await nextTick()

    expect(w.emitted('confirm')?.[0]).toEqual(['created while testing'])
    w.unmount()
  })
})
