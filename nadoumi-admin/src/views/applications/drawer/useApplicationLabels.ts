import { useI18n } from 'vue-i18n'

export function useApplicationLabels() {
  const { t, te } = useI18n()

  function label(group: string, value: string | null): string {
    return value && te(`applications.${group}.${value}`) ? t(`applications.${group}.${value}`) : (value ?? '')
  }

  return {
    typeLabel: (type: string | null) => label('typeMap', type),
    statusLabel: (status: string | null) => label('statusMap', status),
    taskStatusLabel: (status: string | null) => label('taskStatusMap', status),
  }
}
