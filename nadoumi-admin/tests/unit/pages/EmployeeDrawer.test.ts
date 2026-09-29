import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'

const hr = vi.hoisted(() => ({ getEmployee: vi.fn(), createEmployee: vi.fn(), updateEmployee: vi.fn() }))
vi.mock('@/api/hr', () => hr)

const system = vi.hoisted(() => ({
  listUsers: vi.fn(), listPosts: vi.fn(), userDeptTree: vi.fn(), listRoles: vi.fn(),
}))
vi.mock('@/api/system', () => system)

import EmployeeDrawer from '@/views/employees/EmployeeDrawer.vue'
import Drawer from '@/components/ui/Drawer.vue'

const employee = {
  id: 4, userId: 12, employeeNo: 'E-004', userName: 'ada', nickName: 'Ada', email: 'ada@nadoumi.com', phone: null,
  userStatus: '0', positionId: 3, positionTitle: 'Advisor', deptId: 6, deptName: 'Admissions', managerUserId: 9,
  managerName: 'Karim', employmentType: 'FULL_TIME', employmentStatus: 'ACTIVE', startDate: '2025-09-01',
  probationEndDate: null, endDate: null, workLocation: null, salaryAmount: 9000, salaryCurrency: 'CNY',
  payFrequency: 'MONTHLY', compensationVisible: true, emergencyContact: null, emergencyContactRelationship: null,
  emergencyContactPhone: null, emergencyContactEmail: null, notes: null, createTime: null, roleIds: [2],
}

async function mountDrawer(props: { employeeId?: number } = {}) {
  const w = mount(EmployeeDrawer, { props: { modelValue: false, ...props }, ...mountOpts() })
  await w.setProps({ modelValue: true })
  await flushPromises()
  return w
}

async function save(w: ReturnType<typeof mount>) {
  w.findComponent(Drawer).vm.$emit('save')
  await flushPromises()
}

describe('EmployeeDrawer', () => {
  beforeEach(() => {
    Object.values(hr).forEach(fn => fn.mockReset())
    Object.values(system).forEach(fn => fn.mockReset())
    system.listPosts.mockResolvedValue({ rows: [{ postId: 3, postName: 'Advisor' }] })
    system.listUsers.mockResolvedValue({ rows: [{ userId: 9, userName: 'karim', nickName: 'Karim' }] })
    system.listRoles.mockResolvedValue({ rows: [
      { roleId: 1, roleName: 'Super', roleKey: 'admin', admin: true },
      { roleId: 2, roleName: 'Staff', roleKey: 'staff', admin: false },
      { roleId: 5, roleName: 'Finance', roleKey: 'finance', admin: false },
    ] })
    system.userDeptTree.mockResolvedValue({ data: [{ deptId: 6, deptName: 'Admissions', children: [{ deptId: 7, deptName: 'Visas' }] }] })
    hr.getEmployee.mockResolvedValue(employee)
    hr.createEmployee.mockResolvedValue(undefined)
    hr.updateEmployee.mockResolvedValue(undefined)
  })

  it('loads the pick lists and starts a new hire with sensible defaults and the baseline staff role', async () => {
    const w = await mountDrawer()
    expect(system.listUsers).toHaveBeenCalledWith({ userType: '00', pageNum: 1, pageSize: 200 })
    expect(hr.getEmployee).not.toHaveBeenCalled()

    await w.findAll('input.el-input__inner')[0]!.setValue('grace')
    await save(w)

    expect(hr.createEmployee).toHaveBeenCalledOnce()
    const body = hr.createEmployee.mock.calls[0]![0]
    expect(body).toMatchObject({
      userName: 'grace', employmentType: 'FULL_TIME', employmentStatus: 'PROBATION', userStatus: '0',
      salaryCurrency: 'CNY', payFrequency: 'MONTHLY', roleIds: [2],
    })
    expect(body.startDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(hr.updateEmployee).not.toHaveBeenCalled()
  })

  it('never offers the super administrator role', async () => {
    const w = await mountDrawer()
    await save(w)
    expect(hr.createEmployee.mock.calls[0]![0].roleIds).not.toContain(1)
    expect(hr.createEmployee.mock.calls[0]![0].roleIds).toEqual([2])
  })

  it('loads an existing employee and updates it rather than creating a new one', async () => {
    const w = await mountDrawer({ employeeId: 4 })
    expect(hr.getEmployee).toHaveBeenCalledWith(4)

    await save(w)

    expect(hr.updateEmployee).toHaveBeenCalledWith(4, expect.objectContaining({
      nickName: 'Ada', email: 'ada@nadoumi.com', phone: '', roleIds: [2], positionId: 3, deptId: 6,
      managerUserId: 9, employmentStatus: 'ACTIVE', startDate: '2025-09-01', salaryAmount: 9000, workLocation: '',
    }))
    expect(hr.createEmployee).not.toHaveBeenCalled()
  })

  it('does not ask for a username or password when editing', async () => {
    const editing = await mountDrawer({ employeeId: 4 })
    expect(editing.findAll('input[type="password"]')).toHaveLength(0)

    const creating = await mountDrawer()
    expect(creating.findAll('input[type="password"]')).toHaveLength(1)
  })

  it('hides the compensation section when the caller may not see it', async () => {
    hr.getEmployee.mockResolvedValue({ ...employee, compensationVisible: false, salaryAmount: null })
    const hidden = await mountDrawer({ employeeId: 4 })
    expect(hidden.text()).not.toContain('Compensation')

    hr.getEmployee.mockResolvedValue(employee)
    const shown = await mountDrawer({ employeeId: 4 })
    expect(shown.text()).toContain('Compensation')
  })

  it('tells the parent when a save succeeds', async () => {
    const w = await mountDrawer()
    await save(w)
    expect(w.emitted('saved')).toHaveLength(1)
  })
})
