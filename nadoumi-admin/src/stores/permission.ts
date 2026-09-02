import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getRouters } from '@/api/auth'
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView.vue'
import Placeholder from '@/views/placeholder.vue'

const viewModules = import.meta.glob('../views/**/*.vue')

function resolveComponent(name?: string) {
  if (!name || name === 'Layout') return Layout
  if (name === 'ParentView' || name === 'InnerLink') return ParentView
  const key = `../views/${name}.vue`
  return viewModules[key] || Placeholder
}

interface BackendRoute {
  name?: string
  path: string
  hidden?: boolean
  redirect?: string
  component?: string
  alwaysShow?: boolean
  meta?: Record<string, any>
  children?: BackendRoute[]
}

function toRoutes(list: BackendRoute[]): RouteRecordRaw[] {
  return list.map((r) => {
    const route: RouteRecordRaw = {
      path: r.path,
      name: r.name,
      redirect: r.redirect as any,
      component: resolveComponent(r.component) as any,
      meta: { ...(r.meta || {}), hidden: r.hidden, alwaysShow: r.alwaysShow },
      children: r.children ? toRoutes(r.children) : undefined,
    }
    return route
  })
}

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const menuRoutes = ref<RouteRecordRaw[]>([])

  async function generateRoutes() {
    const res = await getRouters()
    const dynamic = toRoutes(res.data || [])
    menuRoutes.value = dynamic
    const wildcard: RouteRecordRaw = { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } }
    routes.value = [...dynamic, wildcard]
    return [...dynamic, wildcard]
  }

  return { routes, menuRoutes, generateRoutes }
})
