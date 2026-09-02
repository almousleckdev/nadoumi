import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import Sidebar from '@/layout/components/Sidebar.vue'
import { useUserStore } from '@/stores/user'
import { testI18n, ElementPlus } from '../helpers'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/dashboard', component: { template: '<div />' } },
    { path: '/applicants', component: { template: '<div />' } },
    { path: '/:rest(.*)*', component: { template: '<div />' } },
  ],
})

function mountSidebar() {
  return mount(Sidebar, {
    global: {
      plugins: [testI18n(), ElementPlus, router],
      stubs: { ElIcon: { template: '<i><slot /></i>' } },
    },
  })
}

describe('Sidebar', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    await router.push('/dashboard')
    await router.isReady()
  })

  it('renders implemented items as links and planned items as disabled rows with a tag', () => {
    useUserStore().permissions = ['*:*:*']
    const w = mountSidebar()

    const links = w.findAll('a.side__link')
    expect(links.map(l => l.attributes('href'))).toEqual(
      expect.arrayContaining(['/dashboard', '/applicants']),
    )

    const planned = w.findAll('.side__link--planned')
    expect(planned.length).toBeGreaterThan(5)
    for (const p of planned) {
      expect(p.attributes('aria-disabled')).toBe('true')
      expect(p.element.tagName).not.toBe('A')
    }
    expect(w.text()).toContain('Planned')
  })

  it('marks the current route active', () => {
    useUserStore().permissions = ['*:*:*']
    const w = mountSidebar()
    const active = w.find('a.side__link--active')
    expect(active.attributes('href')).toBe('/dashboard')
  })

  it('gates implemented links by permission but always shows the planned roadmap', () => {
    useUserStore().permissions = [] // no tokens at all
    const w = mountSidebar()

    // Dashboard has no perm requirement -> still a usable link
    expect(w.find('a[href="/dashboard"]').exists()).toBe(true)
    // Applicants needs nad:applicant:list -> the link is withheld
    expect(w.find('a[href="/applicants"]').exists()).toBe(false)
    // planned rows (and their groups) are still shown as roadmap
    expect(w.text()).toContain('Payroll')
    expect(w.text()).toContain('Business')
    expect(w.findAll('.side__link--planned').length).toBeGreaterThan(10)
  })
})
