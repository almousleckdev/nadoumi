import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DetailFacts from '~/components/catalog/DetailFacts.vue'

describe('DetailFacts', () => {
  it('renders price facts red and every other fact neutral', async () => {
    const w = await mountSuspended(DetailFacts, {
      props: { items: [
        { label: 'Language', value: 'English', icon: 'messages' },
        { label: 'Tuition', value: '¥ 20,000', tone: 'price' },
      ] },
    })
    const values = w.findAll('dd')
    expect(values[0]!.classes()).toContain('text-slate-900')
    expect(values[1]!.classes()).toContain('text-red-600')
  })

  it('renders nothing when there are no facts', async () => {
    const w = await mountSuspended(DetailFacts, { props: { items: [] } })
    expect(w.find('dl').exists()).toBe(false)
  })
})
