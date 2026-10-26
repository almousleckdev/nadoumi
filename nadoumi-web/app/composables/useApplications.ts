import type { StartApplicationBody } from '~/types/apply'
import type { StudentApplicationDto } from '~/types/catalog'
import type { ChecklistItemDto } from '~/types/checklist'
import type { ApplicationTemplateDto } from '~/types/template'

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
  function templates(id: number) {
    return studentFetch<ApplicationTemplateDto[]>(`applications/${id}/templates`)
  }
  /** A template is a file behind the session, so it is fetched as a blob. */
  function template(id: number, templateId: number) {
    return studentFetch<Blob>(`applications/${id}/templates/${templateId}`, { responseType: 'blob' })
  }
  /** The application form as it stands now (a PDF, marked as a preview), fetched with the session. */
  function formPreview(id: number) {
    return studentFetch<Blob>(`applications/${id}/form-preview`, { responseType: 'blob' })
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
  /** Sends a case that was returned for documents back to review. It is the same application: nothing is paid again. */
  function resubmit(id: number) {
    return studentFetch<StudentApplicationDto>(`applications/${id}/transitions/resubmit`, { method: 'POST', body: {} })
  }
  function acceptOffer(id: number) {
    return studentFetch<StudentApplicationDto>(`applications/${id}/transitions/accept_offer`, { method: 'POST', body: {} })
  }
  function declineOffer(id: number) {
    return studentFetch<StudentApplicationDto>(`applications/${id}/transitions/decline_offer`, { method: 'POST', body: {} })
  }
  return { list, get, start, checklist, templates, template, formPreview, submit, withdraw, resubmit, acceptOffer, declineOffer }
}
