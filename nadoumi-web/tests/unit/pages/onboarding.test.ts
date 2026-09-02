import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Onboarding from '~/pages/dashboard/onboarding.vue'

vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ activeApplicantId: ref(1), refresh: vi.fn() }),
}))
vi.mock('~/composables/useApplicant', () => ({
  useApplicant: () => ({
    listMine: vi.fn().mockResolvedValue([]),
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    listEducation: vi.fn().mockResolvedValue([]),
    addEducation: vi.fn(),
    updateEducation: vi.fn(),
    deleteEducation: vi.fn(),
  }),
}))
const { nav } = vi.hoisted(() => ({ nav: vi.fn() }))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

beforeEach(() => nav.mockReset())

describe('onboarding wizard', () => {
  it('shows the 7-step progress and walks Next → … → Finish', async () => {
    const w = await mountSuspended(Onboarding)
    await flushPromises()
    expect(w.text()).toContain('Personal')
    expect(w.text()).toContain('Review')

    // step through to Review
    for (let i = 0; i < 6; i++) {
      const nextBtn = w.findAll('button').find(b => b.text().toLowerCase() === 'next')
      if (!nextBtn) break
      await nextBtn.trigger('click')
      await flushPromises()
    }
    const finish = w.findAll('button').find(b => b.text().toLowerCase() === 'finish')!
    expect(finish).toBeTruthy()
    await finish.trigger('click')
    expect(nav).toHaveBeenCalledWith('/dashboard')
  })

  it('marks the Interests and Location steps as coming soon (not saved)', async () => {
    const w = await mountSuspended(Onboarding)
    await flushPromises()
    // advance to Interests (index 3)
    for (let i = 0; i < 3; i++) {
      await w.findAll('button').find(b => b.text().toLowerCase() === 'next')!.trigger('click')
      await flushPromises()
    }
    expect(w.text().toLowerCase()).toContain('not saved yet')
  })
})
