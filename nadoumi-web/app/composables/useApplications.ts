import type { StartApplicationBody } from '~/types/apply'
import type { StudentApplicationDto } from '~/types/catalog'
import type { ChecklistItemDto } from '~/types/checklist'

/** The signed-in student's own applications, via the BFF proxy to `/api/student/applications`. */
export function useApplications() {
  const { studentFetch } = useApi()

  function list() {
    return studentFetch<StudentApplicationDto[]>('applications')
  }
  function start(body: StartApplicationBody) {
    return studentFetch<StudentApplicationDto>('applications', { method: 'POST', body })
  }
  function checklist(id: number) {
    return studentFetch<ChecklistItemDto[]>(`applications/${id}/documents/checklist`)
  }
  function get(id: number) {
    return studentFetch<StudentApplicationDto>(`applications/${id}`)
  }
  function submit(id: number) {
    return studentFetch<StudentApplicationDto>(`applications/${id}/submit`, { method: 'POST' })
  }
  function withdraw(id: number, reason?: string) {
    return studentFetch<StudentApplicationDto>(`applications/${id}/transitions/withdraw`, {
      method: 'POST',
      body: reason ? { reason } : {},
    })
  }
  return { list, get, start, checklist, submit, withdraw }
}
