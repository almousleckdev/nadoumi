import type { CreateTicketBody, TicketDetail, TicketMessage, TicketStatus, TicketSummary } from '~/types/support'

/** The signed-in student's own support tickets, via the BFF proxy to `/api/student/support/tickets`. */
export function useSupport() {
  const base = '/api/student/support/tickets'

  function list(params: { status?: TicketStatus, page?: number, size?: number } = {}) {
    return $fetch<TicketSummary[]>(base, { query: params })
  }
  function get(id: number) {
    return $fetch<TicketDetail>(`${base}/${id}`)
  }
  function create(body: CreateTicketBody) {
    return $fetch<TicketDetail>(base, { method: 'POST', body })
  }
  function reply(id: number, body: string) {
    return $fetch<TicketMessage>(`${base}/${id}/messages`, { method: 'POST', body: { body } })
  }
  return { list, get, create, reply }
}
