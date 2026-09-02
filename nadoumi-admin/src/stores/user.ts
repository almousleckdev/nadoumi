import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getInfo, login as loginApi, logout as logoutApi, type LoginBody } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const name = ref('')
  const nickName = ref('')
  const avatar = ref('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const mustChangePassword = ref(false)

  async function login(body: LoginBody) {
    const res = await loginApi(body)
    token.value = res.token
    setToken(res.token)
  }

  async function fetchInfo() {
    const res = await getInfo()
    const user = res.user || {}
    name.value = user.userName || ''
    nickName.value = user.nickName || user.userName || ''
    avatar.value = user.avatar || ''
    roles.value = res.roles?.length ? res.roles : ['ROLE_DEFAULT']
    permissions.value = res.permissions || []
    mustChangePassword.value = Boolean(res.isDefaultModifyPwd || res.isPasswordExpired)
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      reset()
    }
  }

  function reset() {
    token.value = ''
    roles.value = []
    permissions.value = []
    removeToken()
  }

  /** RuoYi convention: `*:*:*` (super admin) satisfies every check. */
  function hasPerm(perm?: string): boolean {
    if (!perm) return true
    return permissions.value.includes('*:*:*') || permissions.value.includes(perm)
  }

  return {
    token, name, nickName, avatar, roles, permissions, mustChangePassword,
    login, fetchInfo, logout, reset, hasPerm,
  }
})
