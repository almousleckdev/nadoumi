import type { StudentApplicationDto } from '~/types/catalog'

/** The signed-in student's own applications, via the BFF proxy to `/api/student/applications`. */
export function useApplications() {
  const { studentFetch } = useApi()

  function list() {
    return studentFetch<StudentApplicationDto[]>('applications')
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
  return { list, get, submit, withdraw }
}
