<script setup lang="ts">
defineProps<{ code: string; uuid: string }>()
const emit = defineEmits<{ 'update:code': [v: string]; 'update:uuid': [v: string] }>()
const { t } = useI18n()

const enabled = ref(false)
const img = ref('')

async function load() {
  const res = await $fetch<{ captchaEnabled: boolean; uuid?: string; img?: string }>('/api/public/captcha')
  enabled.value = res.captchaEnabled
  if (res.captchaEnabled) {
    img.value = res.img ?? ''
    emit('update:uuid', res.uuid ?? '')
  }
  else {
    emit('update:uuid', '')
  }
}
onMounted(load)
defineExpose({ enabled, reload: load })
</script>

<template>
  <NField v-if="enabled" :label="t('auth.captcha')" for="captcha">
    <div class="flex items-center gap-2">
      <NInput id="captcha" class="flex-1" :model-value="code" autocomplete="off" @update:model-value="$emit('update:code', $event)" />
      <button type="button" class="shrink-0" :aria-label="t('auth.captchaReload')" @click="load">
        <img :src="`data:image/jpeg;base64,${img}`" alt="" class="h-10 rounded border border-slate-200">
      </button>
    </div>
  </NField>
</template>
