<template>
  <div class="ne ne-theme">
    <EditorTopBar
      :label="topBarLabel"
      :status="statusText"
      :status-tone="autosave.state.value === 'error' ? 'error' : 'normal'"
      :user-name="userStore.nickName"
      :user-avatar="assetUrl(userStore.avatar) || undefined"
      :primary-visible="primaryVisible"
      :primary-label="isLive ? t('news.editor.saveChanges') : t('news.editor.publish')"
      :primary-idle="primaryIdle"
      :primary-busy="publishing"
      :menu="menu"
      @back="goBack"
      @preview="openPreview"
      @primary="onPrimary"
      @command="onCommand"
    />

    <main class="ne__page">
      <AutoGrowTextarea
        ref="titleRef"
        v-model="form.title"
        class="ne__title"
        :placeholder="t('news.editor.titlePlaceholder')"
        :label="t('news.editor.titleLabel')"
        :maxlength="TITLE_MAX"
        :readonly="!canSave"
        @enter="onTitleEnter"
        @blur="onTitleBlur"
      />
      <AutoGrowTextarea
        ref="subtitleRef"
        v-model="form.subtitle"
        class="ne__subtitle"
        :placeholder="t('news.editor.subtitlePlaceholder')"
        :label="t('news.editor.subtitleLabel')"
        :maxlength="SUBTITLE_MAX"
        :readonly="!canSave"
        @enter="editor?.commands.focus('start')"
      />

      <CoverBlock
        ref="coverRef"
        :cover-url="coverUrl"
        :uploading="coverUploading"
        :disabled="!canSave"
        @select="onCoverSelected"
      />

      <div class="ne__body">
        <editor-content :editor="editor" />
        <template v-if="editor && canSave">
          <SelectionToolbar :editor="editor" />
          <BlockInserter
            :editor="editor"
            @image="imageInput?.click()"
          />
        </template>
      </div>
    </main>

    <div
      v-if="imageUploading"
      class="ne__upload"
      role="status"
      data-test="image-uploading"
    >
      <el-icon
        class="is-loading"
        :size="18"
      >
        <Loading />
      </el-icon>
      {{ t('news.editor.imageUploading') }}
    </div>

    <input
      ref="imageInput"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      hidden
      @change="onImagePicked"
    >

    <StorySettingsDialog
      v-model="settingsOpen"
      :language="form.language"
      :slug="article?.slug ?? null"
      :disabled="!canSave"
      @update:language="(v) => (form.language = v)"
    />
    <ShortcutsDialog v-model="shortcutsOpen" />
    <ArticlePreview
      v-if="previewOpen"
      :title="form.title"
      :subtitle="form.subtitle"
      :cover-url="coverUrl"
      :author-name="article?.authorName || userStore.nickName"
      :author-avatar="article?.authorAvatarUrl || assetUrl(userStore.avatar) || undefined"
      :published-at="article?.publishedAt ?? null"
      :markdown="previewMarkdown"
      @close="closePreview"
    />
    <el-drawer
      v-model="commentsOpen"
      :title="t('news.comments')"
      size="520px"
      append-to-body
    >
      <NewsComments
        v-if="article"
        :article-id="article.id"
        @changed="refreshCommentCount"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import '@fontsource-variable/source-serif-4'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import {
  createArticle, deleteArticle, getArticle, publishArticle, unpublishArticle, updateArticle,
  uploadArticleCover, uploadArticleImage,
  type Article, type ArticleInput,
} from '@/api/news'
import { assetUrl } from '@/utils/asset'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import './editor/editor.css'
import ArticlePreview from './editor/ArticlePreview.vue'
import AutoGrowTextarea from './editor/AutoGrowTextarea.vue'
import BlockInserter from './editor/BlockInserter.vue'
import { Loading } from '@element-plus/icons-vue'
import CoverBlock from './editor/CoverBlock.vue'
import EditorTopBar, { type TopBarMenuItem } from './editor/EditorTopBar.vue'
import SelectionToolbar from './editor/SelectionToolbar.vue'
import ShortcutsDialog from './editor/ShortcutsDialog.vue'
import StorySettingsDialog from './editor/StorySettingsDialog.vue'
import NewsComments from './NewsComments.vue'
import { wordCount } from './editor/dialect'
import { createNewsExtensions, markdownOf } from './editor/extensions'
import { newsNodeViews } from './editor/nodeViews'
import { useAutosave } from './editor/useAutosave'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const TITLE_MAX = 200
const SUBTITLE_MAX = 300
const MAX_IMAGE_MB = 8
const BYTES_PER_MB = 1024 * 1024
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const article = ref<Article | null>(null)
const form = reactive<{ title: string, subtitle: string, language: ArticleInput['language'] }>({
  title: '',
  subtitle: '',
  language: 'en',
})

const titleRef = ref<InstanceType<typeof AutoGrowTextarea>>()
const subtitleRef = ref<InstanceType<typeof AutoGrowTextarea>>()
const coverRef = ref<InstanceType<typeof CoverBlock>>()
const imageInput = ref<HTMLInputElement>()

const settingsOpen = ref(false)
const shortcutsOpen = ref(false)
const commentsOpen = ref(false)
const previewOpen = ref(false)
// the document as it is when the preview opens, including edits that are not saved yet
const previewMarkdown = ref('')

const bodyWords = ref(0)
const coverUploading = ref(false)
const imageUploading = ref(false)
const publishing = ref(false)
// Edits to a live article are saved on demand (never behind the writer's back), so they are tracked here.
const unsavedLive = ref(false)
// The slug is derived from the title when the draft is first saved, so a draft is only created once the
// title is finished: on leaving the field, on Enter, or as soon as the writer moves on to the body.
const titleCommitted = ref(false)
// set while the editor content is replaced programmatically, so loading never counts as an edit
let hydrating = false
// the draft is created from inside a save; the route change that follows must not wait for that save
let navigatingToDraft = false

const canSave = computed(() => userStore.hasPerm(article.value || route.params.id ? 'nad:article:edit' : 'nad:article:create'))
const canPublish = computed(() => userStore.hasPerm('nad:article:publish'))
const canRemove = computed(() => userStore.hasPerm('nad:article:remove'))
const isLive = computed(() => article.value?.status === 'PUBLISHED')
const coverUrl = computed(() => assetUrl(article.value?.coverUrl))

const editor = useEditor({
  extensions: createNewsExtensions({ placeholder: t('news.editor.bodyPlaceholder'), nodeViews: newsNodeViews }),
  content: '',
  editable: canSave.value,
  editorProps: {
    attributes: { class: 'ne-prose', 'aria-label': t('news.editor.bodyLabel') },
    handlePaste: (_view, event) => takeImage(event.clipboardData?.files),
    handleDrop: (_view, event, _slice, moved) => !moved && takeImage(event.dataTransfer?.files),
  },
  onFocus: () => { titleCommitted.value = true },
  onUpdate: ({ editor: e }) => {
    bodyWords.value = wordCount(e.getText())
    markEdited()
  },
})

// ---- saving -------------------------------------------------------------------------------------

function payload(): ArticleInput {
  return {
    title: form.title.trim(),
    subtitle: form.subtitle.trim() || null,
    bodyMd: editor.value ? markdownOf(editor.value) : '',
    language: form.language,
  }
}

async function persist(): Promise<void> {
  const body = payload()
  if (!article.value) {
    if (!titleCommitted.value || !body.title) return
    const created = await createArticle(body)
    article.value = created
    navigatingToDraft = true
    try {
      await router.replace({ name: 'NewsEdit', params: { id: created.id } })
    }
    finally {
      navigatingToDraft = false
    }
    return
  }
  // Only the metadata is taken back: the fields stay as the writer left them, even mid-save.
  article.value = await updateArticle(article.value.id, body)
}

const autosave = useAutosave({ save: persist })

const autosaveEnabled = computed(() => !isLive.value)

function markEdited() {
  if (hydrating || !canSave.value) return
  if (autosaveEnabled.value) autosave.markDirty()
  else unsavedLive.value = true
}

watch(() => [form.title, form.subtitle, form.language], markEdited)

/** Makes sure the story exists on the server (media uploads need its id), or tells the writer what is missing. */
async function ensureDraft(): Promise<Article | null> {
  if (!article.value) {
    if (!form.title.trim()) {
      ElMessage.warning(t('news.editor.needTitleFirst'))
      titleRef.value?.focus()
      return null
    }
    titleCommitted.value = true
    try {
      await autosave.flush()
    }
    catch {
      return null // the request layer has already told the writer why
    }
  }
  return article.value
}

// ---- status line ---------------------------------------------------------------------------------

const topBarLabel = computed(() => t(isLive.value ? 'news.editor.liveIn' : 'news.editor.draftIn', { name: userStore.nickName }))

const statusText = computed(() => {
  if (!canSave.value) return ''
  if (!article.value) {
    return bodyWords.value > 0 && !form.title.trim() ? t('news.editor.status.needTitle') : ''
  }
  if (isLive.value) return unsavedLive.value ? t('news.editor.status.unsaved') : t('news.editor.status.live')
  switch (autosave.state.value) {
    case 'saving': return t('news.editor.status.saving')
    case 'error': return t('news.editor.status.error')
    case 'dirty': return t('news.editor.status.words', { n: bodyWords.value }, bodyWords.value)
    default: return t('news.editor.status.saved')
  }
})

// ---- publishing ----------------------------------------------------------------------------------

const primaryVisible = computed(() => canSave.value && (isLive.value || canPublish.value))
const primaryIdle = computed(() => (isLive.value ? !unsavedLive.value : !(form.title.trim() && bodyWords.value > 0)))

async function onPrimary() {
  if (isLive.value) await saveLiveChanges()
  else await publish()
}

async function saveLiveChanges() {
  if (!unsavedLive.value) return
  publishing.value = true
  try {
    autosave.markDirty()
    await autosave.flush()
    unsavedLive.value = false
    ElMessage.success(t('news.editor.saveChangesDone'))
  }
  catch {
    // the request layer has already shown the reason; the changes stay marked as unsaved
  }
  finally {
    publishing.value = false
  }
}

async function publish() {
  titleCommitted.value = true
  publishing.value = true
  try {
    await autosave.flush()
    const current = article.value
    if (!current) {
      ElMessage.warning(t('news.editor.needTitleFirst'))
      titleRef.value?.focus()
      return
    }
    if (bodyWords.value === 0) {
      ElMessage.warning(t('news.editor.needBody'))
      editor.value?.commands.focus('end')
      return
    }
    if (!current.coverMediaId) {
      ElMessage.warning(t('news.editor.needCover'))
      window.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }
    if (!current.publishedAt) {
      const ok = await confirm({
        title: t('news.editor.publishConfirmTitle'),
        message: t('news.editor.publishConfirm', { title: current.title }),
        confirmText: t('news.publish'),
      })
      if (!ok) return
    }
    article.value = await publishArticle(current.id)
    ElMessage.success(t('news.published'))
    // the story is out: leave the writing surface for the list, where it now shows as Published
    await router.push('/news')
  }
  catch {
    // the request layer has already shown the reason (e.g. the server's publish rules)
  }
  finally {
    publishing.value = false
  }
}

async function unpublish() {
  const current = article.value
  if (!current) return
  const ok = await confirm({
    title: t('news.unpublishTitle'),
    message: t('news.unpublishConfirm', { title: current.title }),
    confirmText: t('news.unpublish'),
  })
  if (!ok) return
  article.value = await unpublishArticle(current.id)
  unsavedLive.value = false
  ElMessage.success(t('news.unpublished'))
}

async function removeDraft() {
  const current = article.value
  if (!current) return
  const ok = await confirm({
    title: t('news.deleteTitle'),
    message: t('news.deleteConfirm', { title: current.title }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteArticle(current.id)
  article.value = null // nothing left to save
  await router.replace('/news')
}

// ---- media ---------------------------------------------------------------------------------------

function imageProblem(file: File): string | null {
  if (!file.type.startsWith('image/')) return t('imageUpload.badType')
  if (file.size / BYTES_PER_MB > MAX_IMAGE_MB) return t('imageUpload.tooBig', { mb: MAX_IMAGE_MB })
  return null
}

async function onCoverSelected(file: File) {
  const problem = imageProblem(file)
  if (problem) return void ElMessage.error(problem)
  // Loading starts at once: creating the draft first can take a moment on a slow connection.
  coverUploading.value = true
  try {
    const draft = await ensureDraft()
    if (!draft) return
    const { mediaId, url } = await uploadArticleCover(draft.id, file)
    const fresh = url ? null : await getArticle(draft.id)
    article.value = {
      ...(article.value ?? draft),
      coverMediaId: mediaId,
      coverUrl: url ?? fresh?.coverUrl ?? null,
    }
  }
  catch {
    // the request layer has already shown the reason
  }
  finally {
    coverUploading.value = false
  }
}

async function insertImageFile(file: File) {
  const problem = imageProblem(file)
  if (problem) return void ElMessage.error(problem)
  imageUploading.value = true
  try {
    const draft = await ensureDraft()
    if (!draft || !editor.value) return
    const { url } = await uploadArticleImage(draft.id, file)
    if (!url) return void ElMessage.error(t('imageUpload.failed'))
    editor.value.chain().focus()
      .insertContent([{ type: 'image', attrs: { src: url, alt: '' } }, { type: 'paragraph' }])
      .run()
  }
  catch {
    // the request layer has already shown the reason
  }
  finally {
    imageUploading.value = false
  }
}

function onImagePicked(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) void insertImageFile(file)
}

/** Pasted or dropped pictures go through the same upload as the + menu. Returns true when it took the event. */
function takeImage(files: FileList | undefined | null): boolean {
  const file = Array.from(files ?? []).find(f => f.type.startsWith('image/'))
  if (!file || !canSave.value) return false
  void insertImageFile(file)
  return true
}

// ---- menu ----------------------------------------------------------------------------------------

const menu = computed<TopBarMenuItem[]>(() => {
  const items: TopBarMenuItem[] = []
  if (canSave.value) {
    items.push({ key: 'cover', label: t('news.editor.menu.cover') })
    items.push({ key: 'settings', label: t('news.editor.menu.settings') })
  }
  if (article.value) {
    items.push({ key: 'comments', label: t('news.editor.menu.comments', { n: article.value.commentCount }) })
    if (isLive.value && canPublish.value) items.push({ key: 'unpublish', label: t('news.editor.menu.unpublish') })
  }
  items.push({ key: 'shortcuts', label: t('news.editor.menu.shortcuts'), divided: true })
  items.push({ key: 'back', label: t('news.editor.menu.back') })
  if (article.value && !isLive.value && canRemove.value) {
    items.push({ key: 'delete', label: t('news.editor.menu.delete'), divided: true, danger: true })
  }
  return items
})

function onCommand(key: string) {
  switch (key) {
    case 'cover': return coverRef.value?.open()
    case 'settings': settingsOpen.value = true; return
    case 'comments': commentsOpen.value = true; return
    case 'unpublish': return void unpublish()
    case 'shortcuts': shortcutsOpen.value = true; return
    case 'back': return void goBack()
    case 'delete': return void removeDraft()
  }
}

async function refreshCommentCount() {
  if (!article.value) return
  const fresh = await getArticle(article.value.id)
  article.value = { ...article.value, commentCount: fresh.commentCount }
}

// ---- title flow ----------------------------------------------------------------------------------

function onTitleEnter() {
  titleCommitted.value = true
  subtitleRef.value?.focus()
}

function onTitleBlur() {
  titleCommitted.value = true
  if (!article.value && form.title.trim()) void autosave.flush().catch(() => undefined)
}

// ---- preview -------------------------------------------------------------------------------------

function openPreview() {
  previewMarkdown.value = editor.value ? markdownOf(editor.value) : ''
  previewOpen.value = true
}

async function closePreview() {
  previewOpen.value = false
  // a link straight into the preview (from the list) must not reopen it on the next reload
  if (route.query.preview) await router.replace({ query: {} })
}

// ---- loading, leaving ----------------------------------------------------------------------------

function hydrate(a: Article) {
  hydrating = true
  article.value = a
  form.title = a.title
  form.subtitle = a.subtitle ?? ''
  form.language = a.language
  editor.value?.commands.setContent(a.bodyMd, false)
  bodyWords.value = wordCount(editor.value?.getText() ?? '')
  titleCommitted.value = true
  hydrating = false
}

async function load() {
  const id = route.params.id
  if (typeof id !== 'string' || !UUID_PATTERN.test(id)) return
  try {
    hydrate(await getArticle(id))
    if (route.query.preview === '1') openPreview()
  }
  catch {
    await router.replace('/news')
  }
}

const hasUnsavedWork = computed(() => {
  if (!canSave.value) return false
  if (!article.value) return !!form.title.trim() || bodyWords.value > 0
  return unsavedLive.value || ['dirty', 'saving', 'error'].includes(autosave.state.value)
})

function onBeforeUnload(event: BeforeUnloadEvent) {
  if (!hasUnsavedWork.value) return
  event.preventDefault()
  event.returnValue = ''
}

async function goBack() {
  await router.push('/news')
}

onBeforeRouteLeave(async () => {
  if (navigatingToDraft || !hasUnsavedWork.value) return true
  if (autosaveEnabled.value && article.value) {
    try {
      await autosave.flush()
      return true
    }
    catch {
      // falls through to the confirmation below
    }
  }
  return confirm({
    title: t('news.editor.leaveTitle'),
    message: t('news.editor.leaveConfirm'),
    confirmText: t('news.editor.leave'),
    tone: 'danger',
  })
})

watch(canSave, (value) => editor.value?.setEditable(value))

onMounted(() => {
  window.addEventListener('beforeunload', onBeforeUnload)
  void load()
})
onBeforeUnmount(() => window.removeEventListener('beforeunload', onBeforeUnload))
</script>

<style scoped>
.ne__upload {
  position: fixed;
  inset-block-end: 28px;
  inset-inline-start: 50%;
  transform: translateX(-50%);
  z-index: 20;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  border-radius: 999px;
  background: #242424;
  color: #fff;
  font: 400 14px/20px var(--ne-sans);
  box-shadow: 0 6px 24px rgb(0 0 0 / 25%);
}
.ne {
  min-height: 100vh;
  /* full-width images extend past the column; never let them add a horizontal scrollbar */
  overflow-x: clip;
}
.ne__page {
  max-width: 680px;
  margin: 0 auto;
  padding: 46px 16px 120px;
}
.ne__title {
  font: 400 42px/52px var(--ne-serif);
  letter-spacing: -0.011em;
}
.ne__subtitle {
  margin-top: 10px;
  color: var(--ne-muted);
  font: 400 22px/30px var(--ne-serif);
}
.ne__body {
  position: relative;
  margin-top: 28px;
}

@media (max-width: 860px) {
  .ne__title {
    font-size: 34px;
    line-height: 42px;
  }
}
</style>
