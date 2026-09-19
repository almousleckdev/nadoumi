import { OPTION_GROUPS, type OptionGroup } from '~/constants/applicantOptions'
import type { Option } from '~/utils/countries'

/** The translated choices of one option group, in the order the code list declares them. */
export function useEnumOptions(group: OptionGroup) {
  const { t, te } = useI18n()
  /** A code the student typed themselves (a custom city) has no translation and is shown as entered. */
  const label = (code: string) => (te(`options.${group}.${code}`) ? t(`options.${group}.${code}`) : code)
  const options = computed<Option[]>(() => OPTION_GROUPS[group].map(value => ({ value, label: label(value) })))
  return { options, label }
}
