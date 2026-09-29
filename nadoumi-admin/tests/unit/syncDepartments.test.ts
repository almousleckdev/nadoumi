import { describe, it, expect, vi, beforeEach } from 'vitest'

const api = vi.hoisted(() => ({
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn(),
}))
vi.mock('@/api/university', () => api)

import { syncDepartments } from '@/views/universities/syncDepartments'

describe('syncDepartments', () => {
  beforeEach(() => Object.values(api).forEach(fn => fn.mockReset().mockResolvedValue(undefined)))

  it('creates rows that have no id and skips blank names', async () => {
    const blocked = await syncDepartments(5, [
      { id: undefined, name: ' Law ', nameCn: '' },
      { id: undefined, name: '   ', nameCn: null },
    ], [])
    expect(api.createDepartment).toHaveBeenCalledOnce()
    expect(api.createDepartment).toHaveBeenCalledWith(5, { name: 'Law', nameCn: null })
    expect(blocked).toBe(0)
  })

  it('updates only the rows whose name or Chinese name changed', async () => {
    const original = [
      { id: 1, name: 'Law', nameCn: '法学' },
      { id: 2, name: 'Arts', nameCn: null },
    ]
    await syncDepartments(5, [
      { id: 1, name: 'Law', nameCn: '法学' },
      { id: 2, name: 'Fine Arts', nameCn: null },
    ], original)
    expect(api.updateDepartment).toHaveBeenCalledOnce()
    expect(api.updateDepartment).toHaveBeenCalledWith(5, 2, { name: 'Fine Arts', nameCn: null })
  })

  it('deletes rows that were removed', async () => {
    await syncDepartments(5, [], [{ id: 1, name: 'Law', nameCn: null }])
    expect(api.deleteDepartment).toHaveBeenCalledWith(5, 1)
  })

  it('counts departments that could not be deleted and keeps going', async () => {
    api.deleteDepartment.mockRejectedValueOnce(new Error('in use'))
    const blocked = await syncDepartments(
      5,
      [{ id: undefined, name: 'New', nameCn: null }],
      [{ id: 1, name: 'Law', nameCn: null }, { id: 2, name: 'Arts', nameCn: null }],
    )
    expect(blocked).toBe(1)
    expect(api.deleteDepartment).toHaveBeenCalledTimes(2)
    expect(api.createDepartment).toHaveBeenCalledOnce()
  })
})
