import request from '@/utils/request'

/** Matches FxRates.DEFAULT_CNY_USD on the backend. */
export const DEFAULT_CNY_USD_RATE = 0.1381
const TTL_MS = 5 * 60 * 1000

let cached: { rate: number, at: number } | null = null

export async function cnyToUsdRate(force = false): Promise<number> {
  if (!force && cached && Date.now() - cached.at < TTL_MS) return cached.rate
  try {
    const r = await request.get<unknown, { msg?: string }>(
      '/system/config/configKey/nadoumi.fx.cny_usd')
    const n = Number((r as { msg?: string }).msg)
    const rate = Number.isFinite(n) && n > 0 ? n : DEFAULT_CNY_USD_RATE
    cached = { rate, at: Date.now() }
    return rate
  }
  catch {
    return cached?.rate ?? DEFAULT_CNY_USD_RATE
  }
}
