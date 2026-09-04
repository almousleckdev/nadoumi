import request from '@/utils/request'

/** Outcome of a module image upload. PROTECTED assets (applicant photo) carry no `url`. */
export interface MediaUploadResult {
  mediaId: number
  url: string | null
}

/**
 * POST one image file (multipart, field name `file`) to a module media endpoint
 * such as `/api/staff/universities/12/logo`. The Nadoumi controllers return the
 * record raw, so the resolved value is `{ mediaId, url? }` directly.
 */
export function uploadMedia(action: string, file: File): Promise<MediaUploadResult> {
  const body = new FormData()
  body.append('file', file)
  // No explicit Content-Type: the browser must set `multipart/form-data` itself
  // so it can append the `boundary=…` the server needs to parse the parts.
  return request.post<unknown, MediaUploadResult>(action, body)
}
