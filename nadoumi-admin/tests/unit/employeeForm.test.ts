import { describe, it, expect } from 'vitest'
import { blankEmployeeForm, formFromEmployee, mapDeptTree } from '@/views/employees/employeeForm'
import type { Employee } from '@/api/hr'

const employee = {
  id: 4, userId: 12, employeeNo: 'E-004', userName: 'ada', nickName: 'Ada', email: null, phone: null,
  userStatus: '0', positionId: 3, positionTitle: null, deptId: 6, deptName: 'Admissions', managerUserId: 9,
  managerName: 'Karim', employmentType: 'CONTRACT', employmentStatus: 'ACTIVE', startDate: '2025-09-01',
  probationEndDate: '2025-12-01', endDate: null, workLocation: null, salaryAmount: 9000, salaryCurrency: null,
  payFrequency: 'ANNUAL', compensationVisible: true, emergencyContact: 'Bob', emergencyContactRelationship: null,
  emergencyContactPhone: null, emergencyContactEmail: null, notes: null, createTime: null, roleIds: [2, 5],
} as unknown as Employee

describe('blankEmployeeForm', () => {
  it('starts a full-time hire on probation, active, paid monthly in yuan, starting today', () => {
    const form = blankEmployeeForm()
    expect(form).toMatchObject({
      userName: '', nickName: '', userStatus: '0', roleIds: [], employmentType: 'FULL_TIME',
      employmentStatus: 'PROBATION', salaryCurrency: 'CNY', payFrequency: 'MONTHLY', salaryAmount: null,
    })
    expect(form.startDate).toBe(new Date().toISOString().slice(0, 10))
  })

  it('returns independent objects on every call', () => {
    const a = blankEmployeeForm()
    a.roleIds!.push(9)
    expect(blankEmployeeForm().roleIds).toEqual([])
  })
})

describe('formFromEmployee', () => {
  it('copies the employee and turns missing text into empty strings', () => {
    const form = formFromEmployee(employee)
    expect(form).toMatchObject({
      nickName: 'Ada', email: '', phone: '', positionId: 3, positionTitle: '', deptId: 6, managerUserId: 9,
      employmentType: 'CONTRACT', employmentStatus: 'ACTIVE', startDate: '2025-09-01', probationEndDate: '2025-12-01',
      workLocation: '', salaryAmount: 9000, salaryCurrency: 'CNY', payFrequency: 'ANNUAL',
      emergencyContact: 'Bob', emergencyContactRelationship: '', notes: '', roleIds: [2, 5],
    })
  })

  it('does not carry the account credentials', () => {
    const form = formFromEmployee(employee)
    expect(form.userName).toBeUndefined()
    expect(form.password).toBeUndefined()
  })

  it('treats missing role ids as none', () => {
    expect(formFromEmployee({ ...employee, roleIds: undefined as unknown as number[] }).roleIds).toEqual([])
  })
})

describe('mapDeptTree', () => {
  it('maps departments to id and label recursively', () => {
    const tree = mapDeptTree([{ deptId: 6, deptName: 'Admissions', children: [{ deptId: 7, deptName: 'Visas' }] }] as never)
    expect(tree).toEqual([{ id: 6, label: 'Admissions', children: [{ id: 7, label: 'Visas', children: undefined }] }])
  })

  it('accepts nodes that already carry id and label', () => {
    expect(mapDeptTree([{ id: 1, label: 'HQ' }] as never)).toEqual([{ id: 1, label: 'HQ', children: undefined }])
  })

  it('returns an empty tree for a missing list', () => {
    expect(mapDeptTree(undefined as never)).toEqual([])
  })
})
