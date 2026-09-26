// BFF passthrough for the authenticated student notifications API. Attaches the bearer token
// from the httpOnly cookie; the browser never sees the raw JWT.
//
// This file only ever matches a request with at least one segment after
// student-notifications/ (Nitro's [...path] catch-all convention) — the bare
// list endpoint is handled by the sibling ./index.ts. See studentNotificationsProxy
import { studentNotificationsProxy } from '../../utils/studentNotificationsProxy'

export default defineEventHandler(async event => studentNotificationsProxy(event))
