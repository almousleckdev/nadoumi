import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getInfo, login as loginApi, logout as logoutApi, type LoginBody } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // The session cookie is httpOnly, so "signed in" is learned by asking the server once.
  const loaded = ref(false)
  const userId = ref<number | null>(null)
  const name = ref('')
  const nickName = ref('')
  const avatar = ref('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const mustChangePassword = ref(false)

  async function login(body: LoginBody) {
    await loginApi(body)
    await fetchInfo()
  }

  async function fetchInfo() {
    const res = await getInfo()
    const user = res.user || {}
    userId.value = typeof user.userId === 'number' ? user.userId : null
    name.value = user.userName || ''
    nickName.value = user.nickName || user.userName || ''
    avatar.value = user.avatar || ''
    roles.value = res.roles?.length ? res.roles : ['ROLE_DEFAULT']
    permissions.value = res.permissions || []
    mustChangePassword.value = Boolean(res.isDefaultModifyPwd || res.isPasswordExpired)
  }

  /** Resolve the session once per page load; a missing or expired cookie just leaves it signed out. */
  async function restore() {
    if (loaded.value) return
    try {
      await fetchInfo()
    }
    catch {
      // not signed in
    }
    finally {
      loaded.value = true
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      reset()
    }
  }

  function reset() {
    userId.value = null
    roles.value = []
    permissions.value = []
  }

  const signedIn = computed(() => roles.value.length > 0)

  /** RuoYi convention: `*:*:*` (super admin) satisfies every check. */
  function hasPerm(perm?: string): boolean {
    if (!perm) return true
    return permissions.value.includes('*:*:*') || permissions.value.includes(perm)
  }

  return {
    loaded, userId, name, nickName, avatar, roles, permissions, mustChangePassword, signedIn,
    login, fetchInfo, restore, logout, reset, hasPerm,
  }
})
