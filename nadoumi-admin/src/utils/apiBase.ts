/** Same-origin prefix every admin request (and the SSE stream) goes through to reach the backend. */
export const API_BASE: string = import.meta.env.VITE_APP_BASE_API || '/dev-api'
