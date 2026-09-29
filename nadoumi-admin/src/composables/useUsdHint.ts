import { ref } from 'vue'
import { cnyToUsdRate, DEFAULT_CNY_USD_RATE } from '@/api/fx'
import { usd } from '@/utils/money'

export function useUsdHint() {
  const rate = ref(DEFAULT_CNY_USD_RATE)
  cnyToUsdRate().then((live) => {
    rate.value = live
  })

  function usdHint(amount: number | null | undefined): string {
    return amount ? `≈ ${usd(amount * rate.value)}` : ''
  }

  return { usdHint }
}
