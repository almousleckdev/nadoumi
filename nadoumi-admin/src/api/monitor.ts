import request from '@/utils/request'
import type { RuoYiPage, RuoYiData } from './system'

export interface SysJob {
  jobId: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy: string
  concurrent: string
  status: string
  createTime: string | null
  nextValidTime?: string | null
  remark?: string | null
}

export interface SysOperLog {
  operId: number
  title: string
  businessType: number
  method: string
  requestMethod: string
  operName: string
  operUrl: string
  operIp: string
  operLocation: string
  status: number
  errorMsg: string | null
  operTime: string
  costTime: number
}

export interface SysLogininfor {
  infoId: number
  userName: string
  ipaddr: string
  loginLocation: string
  browser: string
  os: string
  status: string
  msg: string
  loginTime: string
}

/* eslint-disable @typescript-eslint/no-explicit-any */
type Params = Record<string, any>

// ---- scheduled jobs ----
export const listJobs = (params: Params) =>
  request.get<unknown, RuoYiPage<SysJob>>('/monitor/job/list', { params })
export const getJob = (jobId: number) => request.get<unknown, RuoYiData<SysJob>>(`/monitor/job/${jobId}`)
export const createJob = (body: Partial<SysJob>) => request.post('/monitor/job', body)
export const updateJob = (body: Partial<SysJob>) => request.put('/monitor/job', body)
export const deleteJobs = (ids: number[]) => request.delete(`/monitor/job/${ids.join(',')}`)
export const changeJobStatus = (jobId: number, status: string) =>
  request.put('/monitor/job/changeStatus', { jobId, status })
export const runJob = (jobId: number, jobGroup: string) =>
  request.put('/monitor/job/run', { jobId, jobGroup })

// ---- operation log ----
export const listOperLogs = (params: Params) =>
  request.get<unknown, RuoYiPage<SysOperLog>>('/monitor/operlog/list', { params })
export const deleteOperLogs = (ids: number[]) => request.delete(`/monitor/operlog/${ids.join(',')}`)
export const cleanOperLogs = () => request.delete('/monitor/operlog/clean')

// ---- sign-in log ----
export const listLoginLogs = (params: Params) =>
  request.get<unknown, RuoYiPage<SysLogininfor>>('/monitor/logininfor/list', { params })
export const deleteLoginLogs = (ids: number[]) => request.delete(`/monitor/logininfor/${ids.join(',')}`)
export const cleanLoginLogs = () => request.delete('/monitor/logininfor/clean')
export const unlockUser = (userName: string) =>
  request.get(`/monitor/logininfor/unlock/${userName}`)
