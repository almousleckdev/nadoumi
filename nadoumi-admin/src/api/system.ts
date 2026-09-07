/**
 * Thin typed wrappers over RuoYi's `/system/**` and `/monitor/**` console
 * endpoints. These return the RuoYi envelope: `request.ts` unwraps `code === 200`
 * and hands back the whole body, so list calls expose `.rows` / `.total` and
 * single-object calls expose `.data`.
 */
import request from '@/utils/request'

export interface RuoYiPage<T> {
  code: number
  rows: T[]
  total: number
  msg?: string
}
export interface RuoYiData<T> {
  code: number
  data: T
  msg?: string
}

export interface SysUserRow {
  userId: number
  userName: string
  nickName: string
  userType?: string | null
  email: string | null
  phonenumber: string | null
  sex: string | null
  status: string
  deptId: number | null
  dept?: { deptName?: string } | null
  createTime: string | null
  loginDate: string | null
}

export interface SysUserForm {
  userId?: number
  userName?: string
  nickName: string
  userType?: string
  password?: string
  email?: string
  phonenumber?: string
  sex?: string
  status?: string
  deptId?: number | null
  postIds?: number[]
  roleIds?: number[]
  remark?: string
}

export interface SysRole {
  roleId: number
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: string
  status: string
  remark: string | null
  createTime: string | null
  menuIds?: number[]
  deptIds?: number[]
  menuCheckStrictly?: boolean
  deptCheckStrictly?: boolean
  admin?: boolean
}

export interface SysMenu {
  menuId: number
  menuName: string
  parentId: number
  orderNum: number
  path: string
  component: string | null
  query: string | null
  isFrame: string
  isCache: string
  menuType: 'M' | 'C' | 'F'
  visible: string
  status: string
  perms: string | null
  icon: string | null
  children?: SysMenu[]
}

export interface SysDept {
  deptId: number
  parentId: number
  deptName: string
  orderNum: number
  leader: string | null
  phone: string | null
  email: string | null
  status: string
  children?: SysDept[]
}

export interface SysPost {
  postId: number
  postCode: string
  postName: string
  postSort: number
  status: string
  remark: string | null
  createTime: string | null
}

export interface SysDictType {
  dictId: number
  dictName: string
  dictType: string
  status: string
  remark: string | null
  createTime: string | null
}

export interface SysDictData {
  dictCode: number
  dictSort: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass: string | null
  listClass: string | null
  isDefault: string
  status: string
  remark: string | null
}

export interface SysConfig {
  configId: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark: string | null
  createTime: string | null
}

/* eslint-disable @typescript-eslint/no-explicit-any */
type Params = Record<string, any>

// ---- users ----------------------------------------------------------------
export const listUsers = (params: Params) =>
  request.get<unknown, RuoYiPage<SysUserRow>>('/system/user/list', { params })
export const getUser = (userId?: number) =>
  request.get<unknown, RuoYiData<SysUserRow> & { roles: SysRole[], posts: SysPost[], roleIds: number[], postIds: number[] }>(
    `/system/user/${userId ?? ''}`)
export const createUser = (body: SysUserForm) => request.post('/system/user', body)
export const updateUser = (body: SysUserForm) => request.put('/system/user', body)
export const deleteUsers = (ids: number[]) => request.delete(`/system/user/${ids.join(',')}`)
export const resetUserPwd = (userId: number, password: string) =>
  request.put('/system/user/resetPwd', { userId, password })
export const changeUserStatus = (userId: number, status: string) =>
  request.put('/system/user/changeStatus', { userId, status })
export const getUserAuthRole = (userId: number) =>
  request.get<unknown, { user: SysUserRow, roles: SysRole[] }>(`/system/user/authRole/${userId}`)
export const updateUserAuthRole = (userId: number, roleIds: number[]) =>
  request.put('/system/user/authRole', null, { params: { userId, roleIds: roleIds.join(',') } })
export const userDeptTree = () =>
  request.get<unknown, { data: SysDept[] }>('/system/user/deptTree')

// ---- roles --------------------------------------------------------------
export const listRoles = (params: Params) =>
  request.get<unknown, RuoYiPage<SysRole>>('/system/role/list', { params })
export const getRole = (roleId: number) => request.get<unknown, RuoYiData<SysRole>>(`/system/role/${roleId}`)
export const createRole = (body: Partial<SysRole>) => request.post('/system/role', body)
export const updateRole = (body: Partial<SysRole>) => request.put('/system/role', body)
export const deleteRoles = (ids: number[]) => request.delete(`/system/role/${ids.join(',')}`)
export const changeRoleStatus = (roleId: number, status: string) =>
  request.put('/system/role/changeStatus', { roleId, status })
export const roleMenuTreeselect = (roleId: number) =>
  request.get<unknown, { menus: SysMenu[], checkedKeys: number[] }>(`/system/menu/roleMenuTreeselect/${roleId}`)

// ---- menus -------------------------------------------------------------
export const listMenus = (params?: Params) =>
  request.get<unknown, RuoYiData<SysMenu[]>>('/system/menu/list', { params })
export const getMenu = (menuId: number) => request.get<unknown, RuoYiData<SysMenu>>(`/system/menu/${menuId}`)
export const menuTreeselect = () => request.get<unknown, RuoYiData<SysMenu[]>>('/system/menu/treeselect')
export const createMenu = (body: Partial<SysMenu>) => request.post('/system/menu', body)
export const updateMenu = (body: Partial<SysMenu>) => request.put('/system/menu', body)
export const deleteMenu = (menuId: number) => request.delete(`/system/menu/${menuId}`)

// ---- departments -----------------------------------------------------
export const listDepts = (params?: Params) =>
  request.get<unknown, RuoYiData<SysDept[]>>('/system/dept/list', { params })
export const getDept = (deptId: number) => request.get<unknown, RuoYiData<SysDept>>(`/system/dept/${deptId}`)
export const deptTreeExcludeChild = (deptId: number) =>
  request.get<unknown, RuoYiData<SysDept[]>>(`/system/dept/list/exclude/${deptId}`)
export const createDept = (body: Partial<SysDept>) => request.post('/system/dept', body)
export const updateDept = (body: Partial<SysDept>) => request.put('/system/dept', body)
export const deleteDept = (deptId: number) => request.delete(`/system/dept/${deptId}`)

// ---- posts -----------------------------------------------------------
export const listPosts = (params: Params) =>
  request.get<unknown, RuoYiPage<SysPost>>('/system/post/list', { params })
export const getPost = (postId: number) => request.get<unknown, RuoYiData<SysPost>>(`/system/post/${postId}`)
export const createPost = (body: Partial<SysPost>) => request.post('/system/post', body)
export const updatePost = (body: Partial<SysPost>) => request.put('/system/post', body)
export const deletePosts = (ids: number[]) => request.delete(`/system/post/${ids.join(',')}`)
export const listPostOptions = () => request.get<unknown, { posts: SysPost[] }>('/system/user/')

// ---- dict ----------------------------------------------------------
export const listDictTypes = (params: Params) =>
  request.get<unknown, RuoYiPage<SysDictType>>('/system/dict/type/list', { params })
export const getDictType = (dictId: number) =>
  request.get<unknown, RuoYiData<SysDictType>>(`/system/dict/type/${dictId}`)
export const createDictType = (body: Partial<SysDictType>) => request.post('/system/dict/type', body)
export const updateDictType = (body: Partial<SysDictType>) => request.put('/system/dict/type', body)
export const deleteDictTypes = (ids: number[]) => request.delete(`/system/dict/type/${ids.join(',')}`)
export const listDictData = (params: Params) =>
  request.get<unknown, RuoYiPage<SysDictData>>('/system/dict/data/list', { params })
export const getDictData = (dictCode: number) =>
  request.get<unknown, RuoYiData<SysDictData>>(`/system/dict/data/${dictCode}`)
export const createDictData = (body: Partial<SysDictData>) => request.post('/system/dict/data', body)
export const updateDictData = (body: Partial<SysDictData>) => request.put('/system/dict/data', body)
export const deleteDictData = (ids: number[]) => request.delete(`/system/dict/data/${ids.join(',')}`)

// ---- config --------------------------------------------------------
export const listConfigs = (params: Params) =>
  request.get<unknown, RuoYiPage<SysConfig>>('/system/config/list', { params })
export const getConfig = (configId: number) =>
  request.get<unknown, RuoYiData<SysConfig>>(`/system/config/${configId}`)
export const createConfig = (body: Partial<SysConfig>) => request.post('/system/config', body)
export const updateConfig = (body: Partial<SysConfig>) => request.put('/system/config', body)
export const deleteConfigs = (ids: number[]) => request.delete(`/system/config/${ids.join(',')}`)
export const refreshConfigCache = () => request.delete('/system/config/refreshCache')
