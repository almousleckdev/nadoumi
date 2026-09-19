<script setup lang="ts">
import type { PassportMismatchDto } from '~/types/catalog'

/** Explains, field by field, where the passport and the profile disagree. */
defineProps<{ mismatches: PassportMismatchDto[] }>()
defineEmits<{ 'edit-profile': [] }>()
const { t } = useI18n()
</script>

<template>
  <NAlert tone="warning" :title="t('passport.mismatch.title')">
    <p>{{ t('passport.mismatch.body') }}</p>
    <table class="mt-3 w-full text-start text-sm">
      <thead>
        <tr class="text-xs uppercase tracking-wide text-amber-800">
          <th scope="col" class="py-1 pe-3 text-start font-semibold">{{ t('passport.mismatch.field') }}</th>
          <th scope="col" class="py-1 pe-3 text-start font-semibold">{{ t('passport.mismatch.onPassport') }}</th>
          <th scope="col" class="py-1 text-start font-semibold">{{ t('passport.mismatch.inProfile') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in mismatches" :key="m.field" class="border-t border-amber-200">
          <th scope="row" class="py-1.5 pe-3 text-start font-medium">{{ t(`passport.fields.${m.field}`) }}</th>
          <td class="py-1.5 pe-3 font-mono">{{ m.passportValue }}</td>
          <td class="py-1.5 font-mono">{{ m.profileValue }}</td>
        </tr>
      </tbody>
    </table>
    <div class="mt-3">
      <NButton size="sm" variant="secondary" @click="$emit('edit-profile')">{{ t('passport.mismatch.editProfile') }}</NButton>
    </div>
  </NAlert>
</template>
