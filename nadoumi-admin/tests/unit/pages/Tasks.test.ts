import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const hr = vi.hoisted(() => ({
  listTasks: vi.fn(),
  getTask: vi.fn(),
  changeTaskStatus: vi.fn(),
  deleteTask: vi.fn(),
}))
vi.mock('@/api/hr', () => hr)

const confirmMock = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: confirmMock }) }))

vi.mock('@/views/tasks/TaskDrawer.vue', () => ({ default: { template: '<div class="task-drawer-stub" />' } }))

import Tasks from '@/views/tasks/index.vue'
import { useUserStore } from '@/stores/user'

const baseTask = {
  id: 7, title: 'Verify passport', description: 'Check scans', priority: 'HIGH', status: 'PENDING',
  assigneeUserId: 3, assigneeName: 'Sara', createdByUserId: 1, createdByName: 'Omar', dueDate: '2999-01-01',
  startedAt: null, completedAt: null, approvedByName: null, approvedAt: null, relatedType: null, relatedId: null,
  createTime: null,
  events: [{ id: 1, eventType: 'CREATED', fromStatus: null, toStatus: 'PENDING', actorName: 'Omar', note: null, createdAt: '2026-09-01 10:00:00' }],
}

async function mountPage() {
  const w = mount(Tasks, mountOpts())
  await flushPromises()
  return w
}

const openRow = async (w: ReturnType<typeof mount>) => {
  await w.find('tbody tr').trigger('click')
  await flushPromises()
}

const buttons = (w: ReturnType<typeof mount>) => w.findAll('button').map(b => b.text())

describe('Tasks page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(hr).forEach(fn => fn.mockReset())
    confirmMock.mockReset()
    hr.listTasks.mockResolvedValue({ content: [baseTask], totalElements: 1 })
    hr.getTask.mockResolvedValue(baseTask)
    hr.changeTaskStatus.mockResolvedValue(undefined)
    hr.deleteTask.mockResolvedValue(undefined)
  })

  it('lists tasks with the default paging', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    expect(hr.listTasks).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20 }))
    expect(w.text()).toContain('Verify passport')
  })

  it('hides the create button without the add permission', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    expect(w.findAll('button').some(b => b.text() === 'New task')).toBe(false)
  })

  it('opens the detail drawer with the task and its timeline', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    await openRow(w)
    expect(hr.getTask).toHaveBeenCalledWith(7)
    expect(w.text()).toContain('#7 · Verify passport')
    expect(w.text()).toContain('Check scans')
    expect(w.text()).toContain('Sara')
  })

  it('offers no status actions to a caller without progress or edit permission', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    await openRow(w)
    expect(buttons(w)).not.toContain('Delete')
    expect(w.find('.td-actions').findAll('button')).toHaveLength(0)
  })

  it('moves a task to completed and refreshes it', async () => {
    useUserStore().permissions = ['nad:task:progress']
    hr.getTask.mockResolvedValueOnce({ ...baseTask, status: 'IN_PROGRESS' })
    const w = await mountPage()
    await openRow(w)
    const actions = w.find('.td-actions').findAll('button')
    expect(actions).toHaveLength(3)
    await actions[0]!.trigger('click')
    await flushPromises()
    expect(hr.changeTaskStatus).toHaveBeenCalledWith(7, 'COMPLETED', undefined)
    expect(hr.listTasks).toHaveBeenCalledTimes(2)
  })

  it('deletes a task only after confirmation and closes the drawer', async () => {
    useUserStore().permissions = ['nad:task:remove']
    confirmMock.mockResolvedValue(true)
    const w = await mountPage()
    await openRow(w)
    await w.find('.td-actions').findAll('button').find(b => b.text() === 'Delete')!.trigger('click')
    await flushPromises()
    expect(hr.deleteTask).toHaveBeenCalledWith(7)
    expect(w.text()).not.toContain('#7 · Verify passport')
  })

  it('keeps the task when deletion is declined', async () => {
    useUserStore().permissions = ['nad:task:remove']
    confirmMock.mockResolvedValue(false)
    const w = await mountPage()
    await openRow(w)
    await w.find('.td-actions').findAll('button').find(b => b.text() === 'Delete')!.trigger('click')
    await flushPromises()
    expect(hr.deleteTask).not.toHaveBeenCalled()
  })

  it('hides edit on a terminal task', async () => {
    useUserStore().permissions = ['nad:task:edit']
    hr.getTask.mockResolvedValue({ ...baseTask, status: 'APPROVED' })
    const w = await mountPage()
    await openRow(w)
    expect(w.find('.td-actions').findAll('button').some(b => b.text() === 'Edit')).toBe(false)
  })
})
