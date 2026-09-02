import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

// Static routes only. Business screens are added here as they are actually built
// (and listed in src/config/nav.ts with status 'implemented'). No dynamic menu
// from the backend, no placeholder pages for unbuilt modules.
export const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/login.vue'), meta: { hidden: true } },
  { path: '/404', component: () => import('@/views/error/404.vue'), meta: { hidden: true } },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard.vue'),
        meta: { title: 'nav.items.dashboard', i18n: true },
      },
      {
        path: 'applicants',
        name: 'Applicants',
        component: () => import('@/views/applicants/index.vue'),
        meta: { title: 'nav.items.applicants', i18n: true, perm: 'nad:applicant:list' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile.vue'),
        meta: { title: 'nav.profile', i18n: true, hidden: true },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
