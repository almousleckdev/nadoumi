import Cookies from 'js-cookie'

const TOKEN_KEY = 'nadoumi-admin-token'

export const getToken = () => Cookies.get(TOKEN_KEY)
export const setToken = (token: string) => Cookies.set(TOKEN_KEY, token, { sameSite: 'lax' })
export const removeToken = () => Cookies.remove(TOKEN_KEY)
