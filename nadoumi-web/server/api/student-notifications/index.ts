// BFF passthrough for the bare `GET /api/student-notifications` (the list
// endpoint). Nitro's [...path] catch-all in ./[...path].ts does not match a
// request with zero segments after the directory, so that file alone always
// 404s the list call — this sibling file covers exactly that case. Both
import { studentNotificationsProxy } from '../../utils/studentNotificationsProxy'

export default defineEventHandler(async event => studentNotificationsProxy(event))
