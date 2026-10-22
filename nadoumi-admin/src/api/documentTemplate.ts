import request from '@/utils/request'

/** A form template an applicant can download for a programme or scholarship. No storage location is ever returned. */
export interface DocumentTemplate {
  id: number
  title: string
  fileName: string
}

export type TemplateScope = 'programs' | 'scholarships'

export function listTemplates(scope: TemplateScope, refId: number): Promise<DocumentTemplate[]> {
  return request({ url: `/api/staff/document-templates/${scope}/${refId}`, method: 'get' })
}

export function addTemplate(scope: TemplateScope, refId: number, title: string, file: File): Promise<DocumentTemplate> {
  const body = new FormData()
  body.append('title', title)
  body.append('file', file)
  return request({ url: `/api/staff/document-templates/${scope}/${refId}`, method: 'post', data: body })
}

export function removeTemplate(id: number): Promise<void> {
  return request({ url: `/api/staff/document-templates/${id}`, method: 'delete' })
}

/** PDF and Word files, as the server accepts them. */
export const TEMPLATE_ACCEPT = '.pdf,.doc,.docx'
export const TEMPLATE_MAX_MB = 10
