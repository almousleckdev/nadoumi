/**
 * Nadoumi admin navigation — the single source of truth for the sidebar and the
 * app routes. Structure follows docs/ADMIN_ARCHITECTURE.md §2.2.
 *
 * The full module map lives here as the roadmap. The sidebar and router render
 * ONLY `status: 'implemented'` items — planned modules are excluded from active
 * navigation entirely (no route, no placeholder page) until they are genuinely
 * built, at which point a single `status` flip lights them up.
 */
export type NavStatus = 'implemented' | 'planned'

export interface NavItem {
  /** i18n key under `nav.items` */
  key: string
  /** route path (used only when `status === 'implemented'`) */
  path: string
  /** Element Plus icon component name (globally registered) */
  icon: string
  /** permission token required to see this item; omitted = any signed-in staff */
  perm?: string
  status: NavStatus
}

export interface NavGroup {
  /** i18n key under `nav.groups`; omitted = no group heading (top-level rows) */
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
      { key: 'documents', path: '/documents', icon: 'Folder', perm: 'nad:document:view', status: 'planned' },
    ],
  },
  {
    key: 'education',
    items: [
      { key: 'universities', path: '/universities', icon: 'School', perm: 'nad:university:list', status: 'implemented' },
      { key: 'programs', path: '/programs', icon: 'Notebook', perm: 'nad:program:list', status: 'implemented' },
      { key: 'scholarships', path: '/scholarships', icon: 'Medal', perm: 'nad:scholarship:list', status: 'implemented' },
    ],
  },
  {
    items: [
      { key: 'partnerships', path: '/partnerships', icon: 'Connection', perm: 'nad:partnership:view', status: 'planned' },
    ],
  },
  {
    key: 'people',
    items: [
      { key: 'employees', path: '/employees', icon: 'UserFilled', perm: 'nad:employee:view', status: 'planned' },
      { key: 'roles', path: '/roles', icon: 'Lock', perm: 'system:role:list', status: 'planned' },
    ],
  },
  {
    key: 'finance',
    items: [
      { key: 'payments', path: '/payments', icon: 'CreditCard', perm: 'nad:payment:view', status: 'planned' },
      { key: 'invoices', path: '/invoices', icon: 'Document', perm: 'nad:payment:view', status: 'planned' },
      { key: 'revenue', path: '/revenue', icon: 'Money', perm: 'nad:report:finance:view', status: 'planned' },
      { key: 'expenses', path: '/expenses', icon: 'Wallet', perm: 'nad:report:finance:view', status: 'planned' },
      { key: 'payroll', path: '/payroll', icon: 'Coin', perm: 'nad:payroll:view', status: 'planned' },
    ],
  },
  {
    key: 'growth',
    items: [
      { key: 'cms', path: '/cms', icon: 'Promotion', perm: 'nad:content:view', status: 'planned' },
    ],
  },
  {
    key: 'communication',
    items: [
      { key: 'conversations', path: '/conversations', icon: 'ChatDotRound', perm: 'nad:conversation:view', status: 'planned' },
      { key: 'notifications', path: '/notifications', icon: 'Bell', perm: 'nad:notification:view', status: 'planned' },
    ],
  },
  {
    items: [
      { key: 'reports', path: '/reports', icon: 'TrendCharts', perm: 'nad:report:application:view', status: 'planned' },
    ],
  },
  {
    key: 'system',
    items: [
      { key: 'configuration', path: '/system/config', icon: 'Setting', perm: 'system:config:list', status: 'planned' },
      { key: 'audit', path: '/system/audit', icon: 'List', perm: 'monitor:operlog:list', status: 'planned' },
    ],
  },
]

/** Paths of implemented items — the router is built from these so nav and routes never drift. */
export function implementedPaths(): string[] {
  return NAV.flatMap(g => g.items).filter(i => i.status === 'implemented').map(i => i.path)
}
