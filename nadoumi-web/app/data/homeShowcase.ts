export interface ShowcaseTab {
  /** Suffix of the `home.showcase.tabs.*` label. */
  key: string
  /** Query sent to the public list endpoint. */
  query: Record<string, string | boolean>
  /** Query string for the "view all" link on the catalog page, when the catalog supports the filter. */
  viewAll?: string
}

/** Non-empty by type: the first tab is the default selection. */
export type ShowcaseTabs = readonly [ShowcaseTab, ...ShowcaseTab[]]

export const SCHOLARSHIP_TABS: ShowcaseTabs = [
  { key: 'featured', query: { featured: true } },
  { key: 'recommended', query: { recommended: true } },
  { key: 'hot', query: { hot: true } },
  { key: 'full', query: { funding: 'FULLY' }, viewAll: 'funding=FULLY' },
  { key: 'partial', query: { funding: 'PARTIAL' }, viewAll: 'funding=PARTIAL' },
  { key: 'self', query: { funding: 'SELF' }, viewAll: 'funding=SELF' },
]

export const PROGRAM_TABS: ShowcaseTabs = [
  { key: 'featured', query: { featured: true }, viewAll: 'featured=true' },
  { key: 'hot', query: { hot: true } },
  { key: 'newest', query: { sort: 'newest' } },
]
