/**
 * The admin session is an httpOnly cookie set by the backend (`POST /staff/session`),
 * so no token ever reaches script. Every request carries this header: the backend
 * only honours the cookie on unsafe methods when it is present, which a cross-site
 * form or simple request cannot add.
 */
export const CLIENT_HEADERS = { 'X-Nadoumi-Client': 'admin' } as const
