import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Drawer from '@/components/ui/Drawer.vue'
import { testI18n, ElementPlus } from '../../helpers'

// Stub el-drawer to render its default + footer slots inline so we can assert
// on the drawer's own markup without fighting Element Plus's teleport.
const ElDrawerStub = {
  name: 'ElDrawer',
  props: ['modelValue', 'title'],
  template: '<div class="d"><h2>{{ title }}</h2><slot /><footer><slot name="footer" /></footer></div>',
}

function mountDrawer(props: Record<string, unknown>) {
  return mount(Drawer, {
    props: { modelValue: true, title: 'Edit thing', ...props },
    slots: { default: '<p class="body">hello</p>' },
    global: { plugins: [testI18n(), ElementPlus], stubs: { ElDrawer: ElDrawerStub } },
  })
}

function button(w: ReturnType<typeof mountDrawer>, re: RegExp) {
  return w.findAll('button').find(b => re.test(b.text()))!
}

describe('Drawer', () => {
  it('renders the title and default slot', () => {
    const w = mountDrawer({})
    expect(w.text()).toContain('Edit thing')
    expect(w.find('.body').text()).toBe('hello')
  })

  it('emits save on the primary action and closes on cancel', async () => {
    const w = mountDrawer({})
    await button(w, /save/i).trigger('click')
    expect(w.emitted('save')).toBeTruthy()

    await button(w, /cancel/i).trigger('click')
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([false])
  })

  it('disables both actions while saving', () => {
    const w = mountDrawer({ saving: true })
    expect(button(w, /cancel/i).attributes('disabled')).toBeDefined()
    expect(button(w, /save/i).attributes('disabled')).toBeDefined()
  })
})
