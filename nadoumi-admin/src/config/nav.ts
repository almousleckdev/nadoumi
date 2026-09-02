/**
 * Nadoumi admin navigation — the single source of truth for the sidebar and the
 * app routes. Structure follows docs/ADMIN_ARCHITECTURE.md §2.
 *
 * `status: 'implemented'` items have a real screen and route; `status: 'planned'`
 * items are shown disabled with a "Planned" tag so operators can see the roadmap,
 * but they have no route and no placeholder page. Nothing here is a fake
 * implemented screen.
 */
export type NavStatus = 'implemented' | 'planned'

export interface NavItem {
  /** i18n key under `nav.items` */
  key: string
  /** route path — only navigated to when `status === 'implemented'` */
  path: string
  /** Element Plus icon component name (globally registered) */
  icon: string
  /** permission token required to see this item; omitted = any signed-in staff */
  perm?: string
  status: NavStatus
}

export interface NavGroup {
  /** i18n key under `nav.groups`; omitted = no group heading (top-level items) */
  key?: string
  items: NavItem[]
}

export const NAV: NavGroup[] = [
  {
    items: [
      { key: 'dashboard', path: '/dashboard', icon: 'Odometer', status: 'implemented' },
    ],
  },
  {
    key: 'operations',
    items: [
      { key: 'applicants', path: '/applicants', icon: 'User', perm: 'nad:applicant:list', status: 'implemented' },
      { key: 'applications', path: '/applications', icon: 'Tickets', perm: 'nad:application:list', status: 'planned' },
      { key: 'universities', path: '/universities', icon: 'School', perm: 'nad:university:view', status: 'planned' },
      { key: 'programs', path: '/programs', icon: 'Notebook', perm: 'nad:program:view', status: 'planned' },
      { key: 'scholarships', path: '/scholarships', icon: 'Medal', perm: 'nad:scholarship:view', status: 'planned' },
      { key: 'documents', path: '/documents', icon: 'Folder', perm: 'nad:document:view', status: 'planned' },
      { key: 'partnerships', path: '/partnerships', icon: 'Connection', perm: 'nad:partnership:view', status: 'planned' },
    ],
  },
  {
    key: 'business',
    items: [
      { key: 'employees', path: '/employees', icon: 'UserFilled', perm: 'nad:employee:view', status: 'planned' },
      { key: 'finance', path: '/finance', icon: 'Money', perm: 'nad:report:finance:view', status: 'planned' },
      { key: 'payroll', path: '/payroll', icon: 'Wallet', perm: 'nad:payroll:view', status: 'planned' },
      { key: 'payments', path: '/payments', icon: 'CreditCard', perm: 'nad:payment:view', status: 'planned' },
    ],
  },
  {
    key: 'growth',
    items: [
      { key: 'cms', path: '/cms', icon: 'Promotion', perm: 'nad:content:view', status: 'planned' },
      { key: 'communication', path: '/communication', icon: 'ChatDotRound', perm: 'nad:conversation:view', status: 'planned' },
      { key: 'notifications', path: '/notifications', icon: 'Bell', perm: 'nad:notification:view', status: 'planned' },
      { key: 'reports', path: '/reports', icon: 'TrendCharts', perm: 'nad:report:application:view', status: 'planned' },
    ],
  },
  {
    key: 'platform',
    items: [
      { key: 'system', path: '/system', icon: 'Setting', perm: 'system:user:list', status: 'planned' },
    ],
  },
]

/** Paths of implemented items — the router is built from these so nav and routes never drift. */
export function implementedPaths(): string[] {
  return NAV.flatMap(g => g.items).filter(i => i.status === 'implemented').map(i => i.path)
}
