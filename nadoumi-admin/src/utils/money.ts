/**
 * Catalog money is entered by staff in RMB (CNY) only. The USD figure shown next
 * to it is computed on the server from the editable `nadoumi.fx.cny_usd` rate
 * (System > Configuration) and delivered alongside the RMB amount.
 */

/** "¥12,000" — grouped, no decimals. Empty string for null. */
export function cny(amount: number | null | undefined): string {
  if (amount == null) return ''
  return `¥${Number(amount).toLocaleString('en-US', { maximumFractionDigits: 0 })}`
}

/** "$1,657" — grouped, no decimals. Empty string for null. */
export function usd(amount: number | null | undefined): string {
  if (amount == null) return ''
  return `$${Number(amount).toLocaleString('en-US', { maximumFractionDigits: 0 })}`
}

/**
 * "¥12,000 · $1,657" when both are known, otherwise just the part we have.
 * `usdAmount` is the server-computed conversion; pass null to show RMB only.
 */
export function dualMoney(
  rmbAmount: number | null | undefined,
  usdAmount: number | null | undefined,
): string {
  if (rmbAmount == null && usdAmount == null) return ''
  const parts: string[] = []
  if (rmbAmount != null) parts.push(cny(rmbAmount))
  if (usdAmount != null) parts.push(usd(usdAmount))
  return parts.join(' · ')
}
