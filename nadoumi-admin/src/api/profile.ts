import request from '@/utils/request'

export interface ProfileUser {
  userId: number
  userName: string
  nickName: string
  email: string | null
  phonenumber: string | null
  sex: string | null
  avatar: string | null
  deptName?: string | null
  createTime?: string | null
  loginDate?: string | null
  loginIp?: string | null
}

export interface ProfileResponse {
  code: number
  data: ProfileUser
  roleGroup: string
  postGroup: string
}

export interface ProfileInput {
  nickName: string
  email?: string | null
  phonenumber?: string | null
  sex?: string | null
}

/** RuoYi console endpoints — `{ code, data, roleGroup, postGroup }` on 200. */
export const getProfile = () =>
  request.get<unknown, ProfileResponse>('/system/user/profile')

export const updateProfile = (body: ProfileInput) =>
  request.put('/system/user/profile', body)

export const updatePassword = (oldPassword: string, newPassword: string) =>
  request.put('/system/user/profile/updatePwd', { oldPassword, newPassword })

export interface ProfileSession {
  loginTime: number
  expireTime: number
  loggedInForSeconds: number
  expiresInSeconds: number
  ipaddr: string | null
  location: string | null
  browser: string | null
  os: string | null
}

/** Current session detail (Nadoumi endpoint, bare body). */
export const getSession = () =>
  request.get<unknown, ProfileSession>('/api/staff/profile/session')

/** Multipart, field name `avatarfile`. Returns `{ code, imgUrl }`. */
export const uploadAvatar = (file: File) => {
  const fd = new FormData()
  fd.append('avatarfile', file)
  return request.post<unknown, { code: number, imgUrl: string }>(
    '/system/user/profile/avatar', fd,
  )
}
