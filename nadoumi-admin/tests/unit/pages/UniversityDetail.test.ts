import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const universityApi = vi.hoisted(() => ({ getUniversity: vi.fn() }))
vi.mock('@/api/university', async orig => ({ ...(await orig<typeof import('@/api/university')>()), ...universityApi }))

const programApi = vi.hoisted(() => ({ listPrograms: vi.fn(), deleteProgram: vi.fn() }))
vi.mock('@/api/program', async orig => ({ ...(await orig<typeof import('@/api/program')>()), ...programApi }))

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

const message = vi.hoisted(() => ({ success: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, success: message.success } }
})

import UniversityDetail from '@/views/universities/detail.vue'
import { useUserStore } from '@/stores/user'

const university = {
  id: 5, slug: 'fudan', name: 'Fudan University', nameCn: '复旦大学', country: 'CN', type: 'PUBLIC',
  city: 'Shanghai', province: 'Shanghai', foundedYear: 1905, totalStudents: 35000,
  internationalStudents: 4000, facultyCount: 3000, website: 'https://fudan.edu.cn',
  rankingTier: 'Top 50', introduction: 'A leading university.', history: null, campusInfo: 'Green campus.',
  accommodationInfo: null, nearbyInfo: null, admissionsEmail: 'admit@fudan.edu.cn', officePhone: null,
  logoUrl: 'https://res.cloudinary.com/logo.png', bannerUrl: null, logoImageUrl: null, coverImageUrl: null,
  featured: true, status: 'ACTIVE', publishStatus: 'PUBLISHED',
  createdAt: '2026-01-01 10:00:00', updatedAt: null,
  rankings: [{ id: 1, source: 'QS', rankPosition: 34, rankYear: 2026, note: null }],
  highlights: [
    { id: 1, kind: 'HIGHLIGHT', text: 'C9 League member' },
    { id: 2, kind: 'ADVANTAGE', text: 'Strong research output' },
  ],
  gallery: [{ id: 1, imageUrl: 'https://img/campus.jpg', mediaId: 9, url: 'https://cdn/campus.jpg', caption: 'Main campus' }],
}

const program = {
  id: 20, name: 'Computer Science', programType: 'DEGREE', levels: ['MASTER'], teachingLanguage: 'ENGLISH',
  publishStatus: 'PUBLISHED',
}

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/universities/:id', component: { template: '<div />' } }],
})

async function mountDetail() {
  await router.push('/universities/5')
  const w = mount(UniversityDetail, {
    global: {
      ...mountOpts().global,
      plugins: [...mountOpts().global.plugins, router],
      stubs: { UniversityDrawer: true, ProgramDrawer: true, DepartmentSection: true },
    },
  })
  await flushPromises()
  return w
}

describe('University detail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    universityApi.getUniversity.mockReset().mockResolvedValue(university)
    programApi.listPrograms.mockReset().mockResolvedValue({ content: [program], page: 0, size: 100, totalElements: 1, totalPages: 1 })
    programApi.deleteProgram.mockReset().mockResolvedValue(undefined)
    confirm.mockReset()
    message.success.mockReset()
  })

  it('loads the university by route id and renders its profile', async () => {
    const w = await mountDetail()
    expect(universityApi.getUniversity).toHaveBeenCalledWith('5')
    expect(programApi.listPrograms).toHaveBeenCalledWith({ universityId: 5, page: 0, size: 100 })
    expect(w.text()).toContain('Fudan University')
    expect(w.text()).toContain('复旦大学')
    expect(w.text()).toContain('Shanghai')
    expect(w.text()).toContain('35,000')
    expect(w.text()).toContain('A leading university.')
    expect(w.text()).toContain('Green campus.')
  })

  it('links the website safely', async () => {
    const w = await mountDetail()
    const link = w.find('a.link')
    expect(link.attributes('href')).toBe('https://fudan.edu.cn')
    expect(link.attributes('rel')).toContain('noopener')
  })

  it('shows highlights, rankings and gallery when present', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('C9 League member')
    expect(w.text()).toContain('Strong research output')
    expect(w.text()).toContain('QS')
    expect(w.text()).toContain('#34')
    expect(w.find('.gal img').attributes('src')).toContain('campus.jpg')
    expect(w.text()).toContain('Main campus')
  })

  it('hides highlights, rankings and gallery when there are none', async () => {
    universityApi.getUniversity.mockResolvedValue({ ...university, highlights: [], rankings: [], gallery: [] })
    const w = await mountDetail()
    expect(w.text()).not.toContain('C9 League member')
    expect(w.find('.gal').exists()).toBe(false)
  })

  it('lists the programmes of the university', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('Computer Science')
  })

  it('shows an error state when the university cannot be loaded', async () => {
    universityApi.getUniversity.mockRejectedValue(new Error('boom'))
    const w = await mountDetail()
    expect(w.text()).toContain('boom')
    expect(w.text()).not.toContain('Fudan University')
  })

  it('deletes a programme only after the confirm resolves true and reloads the list', async () => {
    confirm.mockResolvedValueOnce(false).mockResolvedValueOnce(true)
    const w = await mountDetail()
    const deleteButton = () => w.findAll('button').find(b => b.text() === 'Delete')!

    await deleteButton().trigger('click')
    await flushPromises()
    expect(programApi.deleteProgram).not.toHaveBeenCalled()

    await deleteButton().trigger('click')
    await flushPromises()
    expect(programApi.deleteProgram).toHaveBeenCalledWith(20)
    expect(message.success).toHaveBeenCalled()
    expect(programApi.listPrograms).toHaveBeenCalledTimes(2)
  })

  it('hides edit and delete controls without the matching permissions', async () => {
    useUserStore().permissions = ['nad:university:list']
    const w = await mountDetail()
    const labels = w.findAll('button').map(b => b.text())
    expect(labels).not.toContain('Edit')
    expect(labels).not.toContain('Delete')
  })
})
