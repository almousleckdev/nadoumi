import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { useOnboarding } from '~/composables/useOnboarding'

const { listMine } = vi.hoisted(() => ({ listMine: vi.fn() }))
mockNuxtImport('useApplicant', () => () => ({ listMine }))

describe('useOnboarding', () => {
  beforeEach(() => {
    listMine.mockReset()
    useOnboarding().invalidate()
  })

  it('is complete only when the server says the applicant is onboarded', async () => {
    listMine.mockResolvedValue([{ id: 1, onboardingComplete: true }])

    expect(await useOnboarding().ensure()).toBe('complete')
  })

  it('is incomplete when the server says it is not, however full the profile looks', async () => {
    listMine.mockResolvedValue([{ id: 1, givenName: 'ADA', familyName: 'LOVELACE', onboardingComplete: false }])

    expect(await useOnboarding().ensure()).toBe('incomplete')
  })

  it('is incomplete when the student has no applicant yet', async () => {
    listMine.mockResolvedValue([])

    expect(await useOnboarding().ensure()).toBe('incomplete')
  })

  it('fails closed when the state cannot be read', async () => {
    listMine.mockRejectedValue(new Error('network'))

    expect(await useOnboarding().ensure()).toBe('incomplete')
  })

  it('caches the result until invalidated or forced', async () => {
    listMine.mockResolvedValue([{ id: 1, onboardingComplete: false }])
    const onboarding = useOnboarding()
    await onboarding.ensure()
    listMine.mockResolvedValue([{ id: 1, onboardingComplete: true }])

    expect(await onboarding.ensure()).toBe('incomplete')
    expect(await onboarding.ensure(true)).toBe('complete')
  })

  it('markComplete records completion without another request', async () => {
    const onboarding = useOnboarding()

    onboarding.markComplete()

    expect(await onboarding.ensure()).toBe('complete')
    expect(listMine).not.toHaveBeenCalled()
  })
})
