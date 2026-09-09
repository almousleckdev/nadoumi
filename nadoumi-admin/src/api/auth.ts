import request from '@/utils/request'

export interface LoginBody {
  username: string
  password: string
  code?: string
  uuid?: string
}

export const getCaptcha = () =>
  request.get<any, { code: number; uuid: string; img: string; captchaEnabled: boolean }>('/captchaImage')

export const login = (body: LoginBody) =>
  request.post<any, { code: number; token: string }>('/login', body)

export const getInfo = () =>
  request.get<any, {
    code: number
    user: Record<string, any>
    roles: string[]
    permissions: string[]
    isDefaultModifyPwd?: boolean
    isPasswordExpired?: boolean
  }>('/getInfo')

export const logout = () => request.post('/logout')

export const updatePassword = (oldPassword: string, newPassword: string) =>
  request.put('/system/user/profile/updatePwd', { oldPassword, newPassword })
