import request from '@/utils/request'

export interface MediaUploadResult {
  mediaId: number
  url: string | null
}

export function uploadMedia(action: string, file: File): Promise<MediaUploadResult> {
  const body = new FormData()
  body.append('file', file)
  return request.post<unknown, MediaUploadResult>(action, body)
}
