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
        path: 'applicants/:id(\\d+)',
        name: 'ApplicantDetail',
        component: () => import('@/views/applicants/detail.vue'),
        meta: { title: 'nav.items.applicants', i18n: true, perm: 'nad:applicant:view' },
      },
      {
        path: 'universities',
        name: 'Universities',
        component: () => import('@/views/universities/index.vue'),
        meta: { title: 'nav.items.universities', i18n: true, perm: 'nad:university:list' },
      },
      {
        path: 'universities/:id(\\d+)',
        name: 'UniversityDetail',
        component: () => import('@/views/universities/detail.vue'),
        meta: { title: 'nav.items.universities', i18n: true, perm: 'nad:university:view' },
      },
      {
        path: 'programs',
        name: 'Programs',
        component: () => import('@/views/programs/index.vue'),
        meta: { title: 'nav.items.programs', i18n: true, perm: 'nad:program:list' },
      },
      {
        path: 'scholarships',
        name: 'Scholarships',
        component: () => import('@/views/scholarships/index.vue'),
        meta: { title: 'nav.items.scholarships', i18n: true, perm: 'nad:scholarship:list' },
      },
      {
        path: 'scholarships/:id(\\d+)',
        name: 'ScholarshipDetail',
        component: () => import('@/views/scholarships/detail.vue'),
        meta: { title: 'nav.items.scholarships', i18n: true, perm: 'nad:scholarship:view' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile.vue'),
        meta: { title: 'nav.profile', i18n: true, hidden: true },
      },
      {
        path: 'staff',
        name: 'Staff',
        component: () => import('@/views/users/index.vue'),
        meta: { title: 'nav.items.employees', i18n: true, perm: 'system:user:list', userType: '00', hidden: true },
      },
      {
        path: 'employees',
        name: 'Employees',
        component: () => import('@/views/employees/index.vue'),
        meta: { title: 'nav.items.employees', i18n: true, perm: 'nad:employee:list' },
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/tasks/index.vue'),
        meta: { title: 'nav.items.tasks', i18n: true, perm: 'nad:task:list' },
      },
      {
        path: 'students',
        name: 'Students',
        component: () => import('@/views/users/index.vue'),
        meta: { title: 'nav.items.students', i18n: true, perm: 'system:user:list', userType: '10' },
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/roles/index.vue'),
        meta: { title: 'nav.items.roles', i18n: true, perm: 'system:role:list' },
      },
      {
        path: 'menus',
        name: 'Menus',
        component: () => import('@/views/menus/index.vue'),
        meta: { title: 'nav.items.menus', i18n: true, perm: 'system:menu:list' },
      },
      {
        path: 'departments',
        name: 'Departments',
        component: () => import('@/views/departments/index.vue'),
        meta: { title: 'nav.items.departments', i18n: true, perm: 'system:dept:list' },
      },
      {
        path: 'posts',
        name: 'Posts',
        component: () => import('@/views/posts/index.vue'),
        meta: { title: 'nav.items.posts', i18n: true, perm: 'system:post:list' },
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notifications/index.vue'),
        // any signed-in staff can see their own feed; oversight mode is gated in-screen
        meta: { title: 'nav.items.notifications', i18n: true },
      },
      {
        path: 'dict',
        name: 'Dict',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: 'nav.items.dict', i18n: true, perm: 'system:dict:list' },
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/index.vue'),
        meta: { title: 'nav.items.configuration', i18n: true, perm: 'system:config:list' },
      },
      {
        path: 'jobs',
        name: 'Jobs',
        component: () => import('@/views/jobs/index.vue'),
        meta: { title: 'nav.items.jobs', i18n: true, perm: 'monitor:job:list' },
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/logs/operlog.vue'),
        meta: { title: 'nav.items.audit', i18n: true, perm: 'monitor:operlog:list' },
      },
      {
        path: 'loginlog',
        name: 'LoginLog',
        component: () => import('@/views/logs/logininfor.vue'),
        meta: { title: 'nav.items.loginlog', i18n: true, perm: 'monitor:logininfor:list' },
      },
      {
        path: 'finance',
        name: 'Earnings',
        component: () => import('@/views/finance/summary.vue'),
        meta: { title: 'nav.items.earnings', i18n: true, perm: 'nad:finance:view' },
      },
      {
        path: 'revenue',
        name: 'Revenue',
        component: () => import('@/views/finance/revenue.vue'),
        meta: { title: 'nav.items.revenue', i18n: true, perm: 'nad:revenue:list' },
      },
      {
        path: 'expenses',
        name: 'Expenses',
        component: () => import('@/views/finance/expenses.vue'),
        meta: { title: 'nav.items.expenses', i18n: true, perm: 'nad:expense:list' },
      },
      {
        path: 'payroll',
        name: 'Payroll',
        component: () => import('@/views/finance/payroll.vue'),
        meta: { title: 'nav.items.payroll', i18n: true, perm: 'nad:payroll:view' },
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
