/** `GET /api/student/applications/{id}/templates`: a form the programme or scholarship wants filled in. */
export interface ApplicationTemplateDto {
  id: number
  title: string
  fileName: string
}
