import { validateFile } from '~/utils/files'

export interface FilePickRules {
  mime: RegExp
  maxMb: number
  /** Shown when the type is not allowed (it differs per document). */
  badTypeMessage: string
}

/**
 * Validates a chosen or dropped file against a type and size rule before it reaches the parent.
 * Shared by every upload control so the checks and their messages exist once.
 */
export function useFilePick(rules: () => FilePickRules, onPick: (file: File) => void) {
  const { t } = useI18n()
  const error = ref('')

  function handleFile(file: File | undefined) {
    if (!file) return
    const { mime, maxMb, badTypeMessage } = rules()
    const problem = validateFile(file, { mime, maxMb })
    error.value = problem === 'badType' ? badTypeMessage : problem === 'tooBig' ? t('onboarding.upload.tooBig', { mb: maxMb }) : ''
    if (!problem) onPick(file)
  }

  function onInput(event: Event) {
    const input = event.target as HTMLInputElement
    handleFile(input.files?.[0])
    input.value = '' // picking the same file again must still fire
  }

  return { error, handleFile, onInput }
}
