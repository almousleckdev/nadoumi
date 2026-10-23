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

const TYPE_BY_EXTENSION: Record<string, string> = {
  pdf: 'application/pdf',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
}

/**
 * Browsers often report an empty or generic type for Word files (it depends on the system), which the server refuses.
 * The extension is what the person picked, so the file is re-labelled with the matching type before it is sent.
 */
export function withKnownType(file: File): File {
  const known = TYPE_BY_EXTENSION[file.name.split('.').pop()?.toLowerCase() ?? '']
  return known && file.type !== known ? new File([file], file.name, { type: known }) : file
}

/** PDF and Word files, as the server accepts them. */
export const TEMPLATE_ACCEPT = '.pdf,.doc,.docx'
export const TEMPLATE_MAX_MB = 10
