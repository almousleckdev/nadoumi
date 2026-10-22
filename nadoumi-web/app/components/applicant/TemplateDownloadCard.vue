<script setup lang="ts">
import type { ApplicationTemplateDto } from '~/types/template'

/** The forms the programme or scholarship wants filled in, ready to download. Nothing is shown when there are none. */
const props = defineProps<{ applicationId: number }>()

const { t } = useI18n()
const { templates, template } = useApplications()
const toast = useToast()

const items = ref<ApplicationTemplateDto[]>([])
const downloading = ref<number | null>(null)

async function download(item: ApplicationTemplateDto) {
  downloading.value = item.id
  try {
    const url = URL.createObjectURL(await template(props.applicationId, item.id))
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

onMounted(async () => {
  items.value = await templates(props.applicationId).catch(() => [])
})
</script>

<template>
  <SectionCard v-if="items.length" :title="t('templates.title')" data-test="templates">
    <p class="mb-3 text-sm text-slate-600">{{ t('templates.blurb') }}</p>
    <ul class="divide-y divide-slate-100">
      <li v-for="item in items" :key="item.id" class="flex flex-wrap items-center justify-between gap-2 py-3 first:pt-0 last:pb-0">
        <span class="min-w-0">
          <span class="block font-medium text-slate-900">{{ item.title }}</span>
          <span class="block truncate text-xs text-slate-500">{{ item.fileName }}</span>
        </span>
        <NButton variant="secondary" size="sm" :loading="downloading === item.id" :data-test="`template-${item.id}`" @click="download(item)">
          {{ t('templates.download') }}
        </NButton>
      </li>
    </ul>
  </SectionCard>
</template>
