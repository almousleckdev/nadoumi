/** Shared categorical palette for the dashboard charts — distinct, calm hues. */
export const CHART_COLORS = [
  '#6366f1', // indigo
  '#10b981', // emerald
  '#f59e0b', // amber
  '#ef4444', // red
  '#0ea5e9', // sky
  '#8b5cf6', // violet
  '#14b8a6', // teal
  '#f97316', // orange
  '#ec4899', // pink
  '#84cc16', // lime
]

export function colorAt(i: number): string {
  return CHART_COLORS[i % CHART_COLORS.length]
}
