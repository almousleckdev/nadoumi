import Cookies from 'js-cookie'

const TOKEN_KEY = 'nadoumi-admin-token'
// Client-side courtesy expiry; the server is the real authority (TOKEN_EXPIRE_TIME,
// sliding on activity) — this just keeps a forgotten cookie from living forever.
const TOKEN_COOKIE_DAYS = 1

export const getToken = () => Cookies.get(TOKEN_KEY)
export const setToken = (token: string) =>
  Cookies.set(TOKEN_KEY, token, {
    sameSite: 'lax',
    expires: TOKEN_COOKIE_DAYS,
    // secure would reject the cookie entirely over plain http (e.g. localhost dev)
    secure: window.location.protocol === 'https:',
  })
export const removeToken = () => Cookies.remove(TOKEN_KEY)
