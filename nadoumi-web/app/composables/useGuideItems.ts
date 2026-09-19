import type { GuideIconName } from '~/components/guides/icons'

/**
 * Builds a guide section's term/detail rows from its translation keys
 * (`<prefix>.<section>.<key>.term|detail`), so guide pages list only the keys and icons.
 */
export function useGuideItems(prefix: string) {
  const { t } = useI18n()

  return function items(section: string, rows: { key: string, icon: GuideIconName }[]) {
    return rows.map(({ key, icon }) => ({
      icon,
      term: t(`${prefix}.${section}.${key}.term`),
      detail: t(`${prefix}.${section}.${key}.detail`),
    }))
  }
}
