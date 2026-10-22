export interface HomeRail {
  /** Suffix of the `home.rails.*` copy and of the async-data key. */
  id: string
  kind: 'scholarship' | 'program'
  /** Query sent to the public list endpoint. */
  query: Record<string, string | boolean>
  /** Query string for the "view all" link on the catalog page, when the catalog supports the filter. */
  viewAll?: string
}

export const SCHOLARSHIP_HIGHLIGHT_RAILS: HomeRail[] = [
  { id: 'scholarshipFeatured', kind: 'scholarship', query: { featured: true } },
  { id: 'scholarshipRecommended', kind: 'scholarship', query: { recommended: true } },
  { id: 'scholarshipHot', kind: 'scholarship', query: { hot: true } },
]

/** One section per funding type, as the catalog filter understands them. */
export const SCHOLARSHIP_FUNDING_RAILS: HomeRail[] = [
  { id: 'scholarshipFull', kind: 'scholarship', query: { funding: 'FULLY' }, viewAll: 'funding=FULLY' },
  { id: 'scholarshipPartial', kind: 'scholarship', query: { funding: 'PARTIAL' }, viewAll: 'funding=PARTIAL' },
  { id: 'scholarshipSelf', kind: 'scholarship', query: { funding: 'SELF' }, viewAll: 'funding=SELF' },
]

export const PROGRAM_RAILS: HomeRail[] = [
  { id: 'programFeatured', kind: 'program', query: { featured: true }, viewAll: 'featured=true' },
  { id: 'programHot', kind: 'program', query: { hot: true } },
]
