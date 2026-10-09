<template>
  <el-dialog
    v-model="open"
    :title="t('news.editor.settings.title')"
    width="440px"
    append-to-body
  >
    <el-form
      label-position="top"
      :disabled="disabled"
      @submit.prevent
    >
      <el-form-item :label="t('news.language')">
        <el-select
          :model-value="language"
          style="width: 100%"
          @update:model-value="(v: ArticleLanguage) => emit('update:language', v)"
        >
          <el-option
            v-for="l in ARTICLE_LANGUAGES"
            :key="l"
            :label="t(`news.languageMap.${l}`)"
            :value="l"
          />
        </el-select>
        <p class="ss__hint">
          {{ t('news.editor.settings.languageHint') }}
        </p>
      </el-form-item>

      <el-form-item
        v-if="slug"
        :label="t('news.editor.settings.address')"
      >
        <code class="ss__slug">/news/{{ slug }}</code>
        <p class="ss__hint">
          {{ t('news.editor.settings.addressHint') }}
        </p>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ARTICLE_LANGUAGES, type ArticleLanguage } from '@/api/news'

defineProps<{ language: ArticleLanguage, slug: string | null, disabled: boolean }>()
const emit = defineEmits<{ 'update:language': [value: ArticleLanguage] }>()
const open = defineModel<boolean>({ required: true })

const { t } = useI18n()
</script>

<style scoped>
.ss__hint {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.ss__slug {
  font-size: 13px;
  word-break: break-all;
}
</style>
