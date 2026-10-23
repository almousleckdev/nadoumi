import type { ApplicationTemplateDto } from '~/types/template'

/** The forms an application's programme or scholarship wants filled in, and downloading them. */
export function useTemplateDownload(applicationId: MaybeRefOrGetter<number>) {
  const { t } = useI18n()
  const { templates, template } = useApplications()
  const toast = useToast()

  const items = ref<ApplicationTemplateDto[]>([])
  const downloading = ref<number | null>(null)

  async function load() {
    items.value = await templates(toValue(applicationId)).catch(() => [])
  }

  async function download(item: ApplicationTemplateDto) {
    downloading.value = item.id
    try {
      const url = URL.createObjectURL(await template(toValue(applicationId), item.id))
      const link = document.createElement('a')
      link.href = url
      link.download = item.fileName
      link.click()
      URL.revokeObjectURL(url)
    }
    catch {
      toast.error(t('templates.failed'))
    }
    finally {
      downloading.value = null
    }
  }

  return { items, downloading, load, download }
}
