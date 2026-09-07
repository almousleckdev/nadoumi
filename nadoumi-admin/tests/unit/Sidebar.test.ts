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

  it('renders only implemented items — no planned rows, no placeholder text', () => {
    useUserStore().permissions = ['*:*:*']
    const w = mountSidebar()

    const hrefs = w.findAll('a.side__link').map(l => l.attributes('href'))
    expect(hrefs.sort()).toEqual([
      '/applicants', '/audit', '/config', '/dashboard', '/departments', '/dict', '/employees',
      '/expenses', '/finance', '/jobs', '/loginlog', '/menus', '/notifications', '/payroll', '/posts',
      '/programs', '/revenue', '/roles', '/scholarships', '/students', '/tasks', '/universities',
    ])
    expect(w.text()).not.toMatch(/planned/i)
    expect(w.text()).not.toContain('Payments')
  })

  it('marks the current route active', () => {
    useUserStore().permissions = ['*:*:*']
    const w = mountSidebar()
    expect(w.find('a.side__link--active').attributes('href')).toBe('/dashboard')
  })

  it('withholds an implemented link the user has no permission for, and drops the empty group', () => {
    useUserStore().permissions = [] // no tokens
    const w = mountSidebar()

    expect(w.find('a[href="/dashboard"]').exists()).toBe(true) // no perm required
    expect(w.find('a[href="/applicants"]').exists()).toBe(false) // needs nad:applicant:list
    expect(w.text()).not.toContain('Operations') // group has no visible item
  })
})
