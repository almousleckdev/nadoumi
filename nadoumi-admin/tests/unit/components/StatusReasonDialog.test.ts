import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { mountOpts } from '../../helpers'
import StatusReasonDialog from '@/components/ui/StatusReasonDialog.vue'

function mountDialog() {
  return mount(StatusReasonDialog, {
    ...mountOpts(),
    props: { modelValue: true, title: 'Suspend', message: 'Suspend Amina?' },
    attachTo: document.body,
  })
}
const confirmButton = () => document.body.querySelector('[data-test="reason-confirm"]') as HTMLButtonElement
const input = () => document.body.querySelector('[data-test="reason-input"]') as HTMLTextAreaElement

async function type(value: string) {
  input().value = value
  input().dispatchEvent(new Event('input'))
  await new Promise(r => setTimeout(r, 0))
}

describe('StatusReasonDialog', () => {
  it('keeps the confirm button disabled until a reason of at least five characters is written', async () => {
    const w = mountDialog()
    await new Promise(r => setTimeout(r, 0))
    expect(confirmButton().disabled).toBe(true)

    await type('no')
    expect(confirmButton().disabled).toBe(true)
    expect(document.body.querySelector('[data-test="reason-hint"]')).not.toBeNull()

    await type('Fake passport')
    expect(confirmButton().disabled).toBe(false)
    w.unmount()
  })

  it('emits the trimmed reason on confirm', async () => {
    const w = mountDialog()
    await new Promise(r => setTimeout(r, 0))
    await type('   Fake passport   ')
    confirmButton().click()
    await new Promise(r => setTimeout(r, 0))
    expect(w.emitted('confirm')?.[0]).toEqual(['Fake passport'])
    w.unmount()
  })

  it('does not emit a whitespace-only reason', async () => {
    const w = mountDialog()
    await new Promise(r => setTimeout(r, 0))
    await type('          ')
    confirmButton().click()
    await new Promise(r => setTimeout(r, 0))
    expect(w.emitted('confirm')).toBeUndefined()
    w.unmount()
  })
})
