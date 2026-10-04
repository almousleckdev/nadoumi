<template>
  <div class="nad-page">
    <PageHeader
      :title="article ? t('news.edit') : t('news.new')"
      :subtitle="article ? `#${article.id} · ${article.slug}` : t('news.newHint')"
    >
      <template #actions>
        <el-button @click="router.push('/news')">
          {{ t('news.back') }}
        </el-button>
        <el-tag
          v-if="article"
          :type="article.status === 'PUBLISHED' ? 'success' : article.status === 'UNPUBLISHED' ? 'warning' : 'info'"
          effect="plain"
          disable-transitions
        >
          {{ t(`news.statusMap.${article.status}`) }}
        </el-tag>
        <el-button
          v-if="article && canPublish && article.status !== 'PUBLISHED'"
          type="success"
          :disabled="!article.coverMediaId"
          :title="article.coverMediaId ? '' : t('news.coverRequired')"
          @click="onPublish"
        >
          {{ t('news.publish') }}
        </el-button>
        <el-button
          v-if="article && canPublish && article.status === 'PUBLISHED'"
          @click="onUnpublish"
        >
          {{ t('news.unpublish') }}
        </el-button>
        <el-button
          v-if="canSave"
          type="primary"
          :loading="saving"
          @click="save"
        >
          {{ t('common.save') }}
        </el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab">
      <el-tab-pane
        :label="t('news.tabWrite')"
        name="write"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          :disabled="!canSave"
        >
          <div class="ed__meta">
            <div class="ed__fields">
              <el-form-item
                :label="t('news.articleTitle')"
                prop="title"
              >
                <el-input
                  v-model="form.title"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item :label="t('news.subtitleField')">
                <el-input
                  v-model="form.subtitle"
                  maxlength="300"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item :label="t('news.language')">
                <el-select
                  v-model="form.language"
                  style="width: 180px"
                >
                  <el-option
                    v-for="l in ARTICLE_LANGUAGES"
                    :key="l"
                    :label="t(`news.languageMap.${l}`)"
                    :value="l"
                  />
                </el-select>
              </el-form-item>
            </div>
            <div class="ed__cover">
              <p class="ed__label">
                {{ t('news.cover') }}
              </p>
              <ImageUpload
                v-if="article"
                :model-value="article.coverMediaId"
                :action="`/api/staff/news/${article.id}/cover`"
                :preview-url="article.coverUrl"
                aspect="wide"
                :max-mb="8"
                :disabled="!canSave"
                @update:model-value="onCoverUploaded"
              />
              <p
                v-else
                class="ed__hint"
              >
                {{ t('news.saveFirst') }}
              </p>
            </div>
          </div>

          <el-form-item
            :label="t('news.body')"
            prop="bodyMd"
          >
            <div class="ed__toolbar">
              <el-button
                size="small"
                :disabled="!article"
                :loading="uploadingImage"
                @click="imageInput?.click()"
              >
                {{ t('news.insertImage') }}
              </el-button>
              <span class="ed__hint">{{ article ? t('news.markdownHint') : t('news.saveFirstImages') }}</span>
              <input
                ref="imageInput"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                hidden
                @change="onImagePicked"
              >
            </div>
            <div class="ed__split">
              <el-input
                ref="bodyInput"
                v-model="form.bodyMd"
                type="textarea"
                :rows="22"
                resize="none"
                :placeholder="t('news.bodyPlaceholder')"
              />
              <!-- eslint-disable vue/no-v-html -- markdown-it escapes raw HTML; see ./markdown -->
              <div
                class="ed__preview"
                v-html="preview"
              />
              <!-- eslint-enable vue/no-v-html -->
            </div>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane
        v-if="article"
        :label="`${t('news.comments')} (${article.commentCount})`"
        name="comments"
      >
        <NewsComments
          :article-id="article.id"
          @changed="refreshCount"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import {
  ARTICLE_LANGUAGES, createArticle, getArticle, publishArticle, unpublishArticle, updateArticle, uploadArticleImage,
  type Article, type ArticleInput,
} from '@/api/news'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import NewsComments from './NewsComments.vue'
import { imageMarkdown, insertAtSelection, renderPreview } from './markdown'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const MAX_IMAGE_MB = 8

const article = ref<Article | null>(null)
const tab = ref('write')
const saving = ref(false)
const uploadingImage = ref(false)
const formRef = ref<FormInstance>()
const imageInput = ref<HTMLInputElement>()
const bodyInput = ref<{ textarea?: HTMLTextAreaElement }>()

const form = reactive<ArticleInput>({ title: '', subtitle: null, bodyMd: '', language: 'en' })
const preview = computed(() => renderPreview(form.bodyMd))

const canPublish = computed(() => userStore.hasPerm('nad:article:publish'))
const canSave = computed(() => userStore.hasPerm(article.value ? 'nad:article:edit' : 'nad:article:create'))

const rules = {
  title: [{ required: true, trigger: 'blur', message: t('common.required') }],
  bodyMd: [{ required: true, trigger: 'blur', message: t('common.required') }],
}

function adopt(a: Article) {
  article.value = a
  form.title = a.title
  form.subtitle = a.subtitle
  form.bodyMd = a.bodyMd
  form.language = a.language
}

async function load() {
  const id = route.params.id
  if (typeof id === 'string' && /^\d+$/.test(id)) adopt(await getArticle(id))
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload: ArticleInput = { ...form, subtitle: form.subtitle?.trim() || null }
    if (article.value) {
      adopt(await updateArticle(article.value.id, payload))
      ElMessage.success(t('common.saved'))
      return
    }
    const created = await createArticle(payload)
    ElMessage.success(t('news.draftCreated'))
    await router.replace(`/news/${created.id}`)
    adopt(created)
  }
  finally {
    saving.value = false
  }
}

function onCoverUploaded(mediaId: number | null) {
  if (article.value) article.value = { ...article.value, coverMediaId: mediaId }
}

async function onPublish() {
  if (!article.value) return
  await save()
  adopt(await publishArticle(article.value.id))
  ElMessage.success(t('news.published'))
}

async function onUnpublish() {
  if (!article.value) return
  adopt(await unpublishArticle(article.value.id))
  ElMessage.success(t('news.unpublished'))
}

async function refreshCount() {
  if (article.value) adopt({ ...(await getArticle(article.value.id)), bodyMd: form.bodyMd, title: form.title })
}

async function onImagePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !article.value) return
  if (!file.type.startsWith('image/')) return void ElMessage.error(t('imageUpload.badType'))
  if (file.size / 1024 / 1024 > MAX_IMAGE_MB) return void ElMessage.error(t('imageUpload.tooBig', { mb: MAX_IMAGE_MB }))

  uploadingImage.value = true
  try {
    const { url } = await uploadArticleImage(article.value.id, file)
    if (!url) return void ElMessage.error(t('imageUpload.failed'))
    const ta = bodyInput.value?.textarea
    const start = ta?.selectionStart ?? form.bodyMd.length
    const end = ta?.selectionEnd ?? form.bodyMd.length
    const alt = file.name.replace(/\.[^.]+$/, '')
    const spliced = insertAtSelection(form.bodyMd, start, end, imageMarkdown(url, alt))
    form.bodyMd = spliced.value
  }
  catch {
    ElMessage.error(t('imageUpload.failed'))
  }
  finally {
    uploadingImage.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ed__meta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}
.ed__label {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.ed__hint {
  font-size: 12px;
  color: var(--nad-ink-soft, #64748b);
}
.ed__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin-bottom: 8px;
}
.ed__split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  width: 100%;
}
.ed__preview {
  height: 100%;
  min-height: 480px;
  max-height: 540px;
  overflow: auto;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-light, #e5e7eb);
  border-radius: 6px;
  overflow-wrap: anywhere;
  line-height: 1.7;
}
.ed__preview :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}
.ed__preview :deep(blockquote) {
  margin: 12px 0;
  padding-inline-start: 12px;
  border-inline-start: 3px solid var(--el-border-color, #cbd5e1);
  color: var(--el-text-color-secondary);
}
@media (max-width: 1000px) {
  .ed__meta,
  .ed__split {
    grid-template-columns: 1fr;
  }
}
</style>
