import type { CreateTicketBody, TicketDetail, TicketMessage, TicketStatus, TicketSummary } from '~/types/support'

/** The signed-in student's own support tickets, via the BFF proxy to `/api/student/support/tickets`. */
export function useSupport() {
  const base = '/api/student/support/tickets'
  // See useApi.ts: useRequestFetch() forwards the incoming request's cookies during
  // SSR, which the global $fetch does not.
  const requestFetch = useRequestFetch()

  function list(params: { status?: TicketStatus, page?: number, size?: number } = {}) {
    return requestFetch<TicketSummary[]>(base, { query: params })
  }
  function get(id: number) {
    return requestFetch<TicketDetail>(`${base}/${id}`)
  }
  function create(body: CreateTicketBody) {
    return requestFetch<TicketDetail>(base, { method: 'POST', body })
  }
  function reply(id: number, body: string) {
    return requestFetch<TicketMessage>(`${base}/${id}/messages`, { method: 'POST', body: { body } })
  }
  return { list, get, create, reply }
}
