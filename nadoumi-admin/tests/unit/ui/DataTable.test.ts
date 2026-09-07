import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DataTable from '@/components/ui/DataTable.vue'
import { mountOpts } from '../../helpers'

const columns = [
  { prop: 'name', label: 'Name' },
  { prop: 'status', label: 'Status', width: 120 },
]
const rows = [
  { id: 1, name: 'Ada', status: 'ACTIVE' },
  { id: 2, name: 'Bo', status: 'DRAFT' },
]

describe('DataTable', () => {
  it('renders an error state with a retry that emits', async () => {
    const w = mount(DataTable, { props: { columns, rows: [], error: 'boom' }, ...mountOpts() })
    expect(w.text()).toContain('boom')
    await w.find('button').trigger('click')
    expect(w.emitted('retry')).toBeTruthy()
  })

  it('shows a skeleton while loading with no rows yet', () => {
    const w = mount(DataTable, { props: { columns, rows: [], loading: true }, ...mountOpts() })
    expect(w.find('.skeleton').exists()).toBe(true)
  })

  it('shows an empty state when not loading and no rows', () => {
    const w = mount(DataTable, {
      props: { columns, rows: [], emptyTitle: 'Nothing here' },
      ...mountOpts(),
    })
    expect(w.text()).toContain('Nothing here')
  })

  it('passes the rows to the table and renders a column per definition', () => {
    const w = mount(DataTable, { props: { columns, rows, total: 2, pageSize: 20 }, ...mountOpts() })
    const table = w.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)
    expect(table.props('data')).toEqual(rows)
    expect(w.findAllComponents({ name: 'ElTableColumn' }).length).toBe(columns.length)
  })

  it('shows the shared pagination bar whenever there are rows, hides it when empty', () => {
    // every list screen uses the same always-visible pager (with a page-size selector)
    const withRows = mount(DataTable, { props: { columns, rows, total: 2, pageSize: 20 }, ...mountOpts() })
    expect(withRows.findComponent({ name: 'ElPagination' }).exists()).toBe(true)

    const empty = mount(DataTable, { props: { columns, rows: [], total: 0, pageSize: 20 }, ...mountOpts() })
    expect(empty.findComponent({ name: 'ElPagination' }).exists()).toBe(false)
  })

  it('re-emits update:page-size from the pagination bar', () => {
    const w = mount(DataTable, { props: { columns, rows, total: 50, pageSize: 20 }, ...mountOpts() })
    w.findComponent({ name: 'ElPagination' }).vm.$emit('update:page-size', 50)
    expect(w.emitted('update:page-size')?.[0]).toEqual([50])
  })

  it('emits row-click only when clickableRows is set', async () => {
    const w = mount(DataTable, {
      props: { columns, rows, total: 2, pageSize: 20, clickableRows: true },
      ...mountOpts(),
    })
    w.findComponent({ name: 'ElTable' }).vm.$emit('row-click', rows[0])
    expect(w.emitted('row-click')?.[0]).toEqual([rows[0]])

    const w2 = mount(DataTable, { props: { columns, rows, total: 2, pageSize: 20 }, ...mountOpts() })
    w2.findComponent({ name: 'ElTable' }).vm.$emit('row-click', rows[0])
    expect(w2.emitted('row-click')).toBeFalsy()
  })
})
