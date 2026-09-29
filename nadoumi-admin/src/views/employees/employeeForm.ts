import { inject, type InjectionKey } from 'vue'
import type { Employee, EmployeeInput } from '@/api/hr'
import type { SysDept } from '@/api/system'

export const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN', 'TEMPORARY']
export const EMPLOYMENT_STATUSES = ['PROBATION', 'ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED']
export const PAY_FREQUENCIES = ['MONTHLY', 'ANNUAL', 'WEEKLY', 'HOURLY']

export interface DeptOption {
  id: number
  label: string
  children?: DeptOption[]
}

export function blankEmployeeForm(): EmployeeInput {
  return {
    userName: '', password: '', nickName: '', email: '', phone: '', userStatus: '0', roleIds: [],
    positionId: null, positionTitle: '', deptId: null, managerUserId: null,
    employmentType: 'FULL_TIME', employmentStatus: 'PROBATION',
    startDate: new Date().toISOString().slice(0, 10),
    probationEndDate: null, endDate: null, workLocation: '',
    salaryAmount: null, salaryCurrency: 'CNY', payFrequency: 'MONTHLY',
    emergencyContact: '', emergencyContactRelationship: '', emergencyContactPhone: '', emergencyContactEmail: '',
    notes: '',
  }
}

export function formFromEmployee(e: Employee): Partial<EmployeeInput> {
  return {
    nickName: e.nickName, email: e.email ?? '', phone: e.phone ?? '',
    userStatus: e.userStatus, roleIds: e.roleIds ?? [],
    positionId: e.positionId, positionTitle: e.positionTitle ?? '',
    deptId: e.deptId, managerUserId: e.managerUserId,
    employmentType: e.employmentType, employmentStatus: e.employmentStatus,
    startDate: e.startDate, probationEndDate: e.probationEndDate, endDate: e.endDate,
    workLocation: e.workLocation ?? '',
    salaryAmount: e.salaryAmount, salaryCurrency: e.salaryCurrency ?? 'CNY',
    payFrequency: e.payFrequency,
    emergencyContact: e.emergencyContact ?? '',
    emergencyContactRelationship: e.emergencyContactRelationship ?? '',
    emergencyContactPhone: e.emergencyContactPhone ?? '',
    emergencyContactEmail: e.emergencyContactEmail ?? '',
    notes: e.notes ?? '',
  }
}

export function mapDeptTree(nodes: SysDept[] | undefined): DeptOption[] {
  return (nodes || []).map(n => ({
    id: (n as unknown as { id?: number }).id ?? n.deptId,
    label: (n as unknown as { label?: string }).label ?? n.deptName,
    children: n.children ? mapDeptTree(n.children) : undefined,
  }))
}

export const employeeFormKey: InjectionKey<EmployeeInput> = Symbol('employeeForm')

export function useEmployeeForm(): EmployeeInput {
  const form = inject(employeeFormKey)
  if (!form) throw new Error('employee form sections must be rendered inside EmployeeDrawer')
  return form
}
