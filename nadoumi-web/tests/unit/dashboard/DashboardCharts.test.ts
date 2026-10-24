import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DonutChart from '~/components/dashboard/DonutChart.vue'
import ProgressRing from '~/components/dashboard/ProgressRing.vue'
import StageMeter from '~/components/dashboard/StageMeter.vue'
import { applicationStats, stageDistribution } from '~/utils/applicationView'

describe('applicationStats', () => {
  it('counts sent, moving, offered and unfinished applications, ignoring withdrawn ones', () => {
    const stats = applicationStats(['DRAFT', 'IN_REVIEW', 'DECISION', 'OFFER', 'CLOSED_SUCCESS', 'CLOSED_UNSUCCESSFUL', 'CLOSED_WITHDRAWN'])
    expect(stats).toEqual({ submitted: 5, inProgress: 2, offers: 2, drafts: 1 })
  })
})

describe('stageDistribution', () => {
  it('lists stages in path order and leaves empty ones out', () => {
    expect(stageDistribution(['DECISION', 'IN_REVIEW', 'IN_REVIEW', 'DRAFT'])).toEqual([
      { status: 'IN_REVIEW', count: 2 }, { status: 'DECISION', count: 1 },
    ])
  })
})

describe('DonutChart', () => {
  it('draws one arc per segment and lists each value', async () => {
    const w = await mountSuspended(DonutChart, { props: { segments: [{ key: 'a', label: 'In review', value: 3 }, { key: 'b', label: 'Offer', value: 1 }] } })
    expect(w.findAll('[data-test="donut-arc"]')).toHaveLength(2)
    expect(w.text()).toContain('In review')
  })
})

describe('ProgressRing', () => {
  it('clamps the percentage into 0 to 100', async () => {
    const w = await mountSuspended(ProgressRing, { props: { percent: 140, label: 'Profile' } })
    expect(w.text()).toContain('100%')
  })
})

describe('StageMeter', () => {
  it('shows how many of the six steps are reached', async () => {
    const w = await mountSuspended(StageMeter, { props: { status: 'DECISION' } })
    expect(w.text()).toContain('3/6')
  })
})
