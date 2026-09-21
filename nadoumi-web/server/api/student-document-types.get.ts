import type { DocumentTypeOption } from '~/types/documents'

/** RuoYi dictionary holding the configurable document types (seeded by the Document domain). */
const DOCUMENT_TYPE_DICT = 'nad_document_type'

interface DictEntry { dictLabel: string, dictValue: string }

// BFF: the type dictionary lives in RuoYi's dict tables, outside `/api/student/**`, so it
// gets its own route. It attaches the student bearer token like every other BFF route.
export default defineEventHandler(async (event): Promise<DocumentTypeOption[]> => {
  const token = studentToken(event)
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Not signed in' })
  }
  const res = await $fetch<{ data?: DictEntry[] }>(
    `${backendBaseUrl(event)}/system/dict/data/type/${DOCUMENT_TYPE_DICT}`,
    { headers: { authorization: `Bearer ${token}` } },
  )
  return (res.data ?? []).map(entry => ({ value: entry.dictValue, label: entry.dictLabel }))
})
