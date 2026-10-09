<script setup lang="ts">
const props = defineProps<{ modelValue: boolean, slug: string, signedIn: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()

const { t, n } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { likers, total, loading, failed, load } = useNewsLikers(props.slug)

// The list is only fetched for a signed-in reader, and every time the dialog opens so it is current.
watch(() => props.modelValue, (open: boolean) => {
  if (open && props.signedIn) void load()
})
</script>

<template>
  <NModal :model-value="modelValue" :title="t('news.likersTitle')" @update:model-value="$emit('update:modelValue', $event)">
    <p v-if="!signedIn" class="text-sm text-slate-700">
      {{ t('news.likersSignIn') }}
      <NuxtLink
        :to="{ path: localePath('/login'), query: { redirect: route.fullPath } }"
        class="ms-1 font-semibold text-brand-700"
      >{{ t('news.signIn') }}</NuxtLink>
    </p>
    <p v-else-if="loading && !likers.length" class="text-sm text-slate-500" role="status">{{ t('news.likersLoading') }}</p>
    <div v-else-if="failed" role="alert" class="text-sm text-red-600">
      {{ t('news.likersError') }}
      <button type="button" class="ms-1 font-semibold underline" @click="load">{{ t('news.retry') }}</button>
    </div>
    <template v-else>
      <p class="mb-3 text-sm text-slate-500">{{ t('news.likeCount', total) }}</p>
      <ul class="max-h-80 space-y-3 overflow-y-auto">
        <li v-for="(liker, i) in likers" :key="i" class="flex items-center gap-3">
          <NAvatar :name="liker.displayName" :src="liker.avatarUrl ?? undefined" size="sm" />
          <span class="text-sm font-medium text-slate-900">{{ liker.displayName }}</span>
        </li>
      </ul>
      <p v-if="total > likers.length" class="mt-3 text-sm text-slate-500">
        {{ t('news.likersMore', { n: n(total - likers.length) }) }}
      </p>
    </template>
    <template #footer>
      <NButton variant="secondary" size="sm" @click="$emit('update:modelValue', false)">{{ t('news.close') }}</NButton>
    </template>
  </NModal>
</template>
