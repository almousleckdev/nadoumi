import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ProfilePhotoUploadCard from '~/components/onboarding/ProfilePhotoUploadCard.vue'
import PassportUploadCard from '~/components/onboarding/PassportUploadCard.vue'

describe('document upload cards (client-only, REQUIRES BACKEND)', () => {
  it('ProfilePhotoUploadCard shows a coming-soon badge and a disabled Upload button', async () => {
    const w = await mountSuspended(ProfilePhotoUploadCard)
    expect(w.text().toLowerCase()).toContain('coming soon')
    const upload = w.findAll('button').find(b => b.text().toLowerCase() === 'upload')!
    expect(upload.attributes('disabled')).toBeDefined()
    expect(w.text().toLowerCase()).toContain('document storage is enabled')
  })

  it('PassportUploadCard shows a coming-soon badge and a disabled Upload button', async () => {
    const w = await mountSuspended(PassportUploadCard)
    expect(w.text().toLowerCase()).toContain('coming soon')
    const upload = w.findAll('button').find(b => b.text().toLowerCase() === 'upload')!
    expect(upload.attributes('disabled')).toBeDefined()
  })
})
