import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listJobs: vi.fn(),
  getJob: vi.fn(),
  createJob: vi.fn(),
  updateJob: vi.fn(),
  deleteJobs: vi.fn(),
  changeJobStatus: vi.fn(),
  runJob: vi.fn(),
}))
vi.mock('@/api/monitor', () => api)

const confirmMock = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: confirmMock }) }))

import Jobs from '@/views/jobs/index.vue'
import { useUserStore } from '@/stores/user'

const job = {
  jobId: 5, jobName: 'Nightly digest', jobGroup: 'DEFAULT', invokeTarget: 'digest.run()',
  cronExpression: '0 0 2 * * ?', misfirePolicy: '3', concurrent: '1', status: '0',
}

const ALL_PERMS = ['monitor:job:add', 'monitor:job:changeStatus', 'monitor:job:remove']

async function mountPage(perms: string[] = ALL_PERMS) {
  useUserStore().permissions = perms
  const w = mount(Jobs, mountOpts())
  await flushPromises()
  return w
}

const rowButton = (w: ReturnType<typeof mount>, label: string) => w.find('tbody').findAll('button').find(b => b.text() === label)!
const button = (w: ReturnType<typeof mount>, label: string) => w.findAll('button').find(b => b.text() === label)!

describe('Jobs page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    confirmMock.mockReset()
    api.listJobs.mockResolvedValue({ rows: [job], total: 1 })
    api.getJob.mockResolvedValue({ data: job })
    api.createJob.mockResolvedValue(undefined)
    api.updateJob.mockResolvedValue(undefined)
    api.deleteJobs.mockResolvedValue(undefined)
    api.changeJobStatus.mockResolvedValue(undefined)
    api.runJob.mockResolvedValue(undefined)
  })

  it('lists jobs from the first page', async () => {
    const w = await mountPage()
    expect(api.listJobs).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 1, pageSize: 10 }))
    expect(w.text()).toContain('Nightly digest')
  })

  it('hides create, run and delete without the matching permissions', async () => {
    const w = await mountPage([])
    expect(w.findAll('button').some(b => ['New job', 'Run once', 'Delete'].includes(b.text()))).toBe(false)
  })

  it('creates a job with the default form values', async () => {
    const w = await mountPage()
    await button(w, 'New job').trigger('click')
    await flushPromises()
    await button(w, 'Save').trigger('click')
    await flushPromises()
    expect(api.createJob).toHaveBeenCalledWith(expect.objectContaining({
      jobGroup: 'DEFAULT', misfirePolicy: '3', concurrent: '1', status: '1',
    }))
    expect(api.listJobs).toHaveBeenCalledTimes(2)
  })

  it('loads the job into the form and updates it on edit', async () => {
    const w = await mountPage()
    await rowButton(w, 'Edit').trigger('click')
    await flushPromises()
    expect(api.getJob).toHaveBeenCalledWith(5)
    await button(w, 'Save').trigger('click')
    await flushPromises()
    expect(api.updateJob).toHaveBeenCalledWith(expect.objectContaining({ jobId: 5, jobName: 'Nightly digest' }))
    expect(api.createJob).not.toHaveBeenCalled()
  })

  it('pauses a running job through the status switch', async () => {
    const w = await mountPage()
    await w.find('tbody .el-switch').trigger('click')
    await flushPromises()
    expect(api.changeJobStatus).toHaveBeenCalledWith(5, '1')
  })

  it('runs a job once only after confirmation', async () => {
    confirmMock.mockResolvedValue(true)
    const w = await mountPage()
    await rowButton(w, 'Run once').trigger('click')
    await flushPromises()
    expect(api.runJob).toHaveBeenCalledWith(5, 'DEFAULT')
  })

  it('does not run or delete when confirmation is declined', async () => {
    confirmMock.mockResolvedValue(false)
    const w = await mountPage()
    await rowButton(w, 'Run once').trigger('click')
    await rowButton(w, 'Delete').trigger('click')
    await flushPromises()
    expect(api.runJob).not.toHaveBeenCalled()
    expect(api.deleteJobs).not.toHaveBeenCalled()
  })

  it('deletes a confirmed job and reloads', async () => {
    confirmMock.mockResolvedValue(true)
    const w = await mountPage()
    await rowButton(w, 'Delete').trigger('click')
    await flushPromises()
    expect(api.deleteJobs).toHaveBeenCalledWith([5])
    expect(api.listJobs).toHaveBeenCalledTimes(2)
  })
})
