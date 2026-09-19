import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'

export interface DrawerFormOptions<T extends object> {
  /** Whether the drawer is open; opening starts a fresh load. */
  isOpen: () => boolean
  /** Empty form values, used when the drawer opens and when it closes. */
  blank: () => T
  /** Load lookups and, when editing, the record; runs with `loading` on, after the form is reset. */
  load?: (form: T) => Promise<void>
  /** Create or update from the form. Throwing keeps the drawer open. */
  submit: (form: T) => Promise<void>
  /** Called after a successful save (usually `emit('saved')`). */
  onSaved: () => void
  /** Extra state to clear whenever the form is reset. */
  onReset?: () => void
}

/**
 * The lifecycle every create/edit drawer shares: reset and load when opened, validate then save
 * then confirm then notify, and a `saving` flag around the request. A drawer supplies only what
 * differs (what to load, how to build the request) so none re-implements the skeleton.
 */
export function useDrawerForm<T extends object>(options: DrawerFormOptions<T>) {
  const { t } = useI18n()
  const formRef = ref<FormInstance>()
  const form = reactive(options.blank()) as T
  const loading = ref(false)
  const saving = ref(false)

  function reset() {
    Object.assign(form, options.blank())
    options.onReset?.()
  }

  async function open() {
    reset()
    loading.value = true
    try {
      await options.load?.(form)
    }
    finally {
      loading.value = false
    }
  }

  async function save() {
    await formRef.value?.validate()
    saving.value = true
    try {
      await options.submit(form)
      ElMessage.success(t('common.saved'))
      options.onSaved()
    }
    finally {
      saving.value = false
    }
  }

  // A failed load is already toasted by the request layer; the drawer stays open and usable.
  watch(options.isOpen, (opened) => { if (opened) open().catch(() => undefined) })

  return { formRef, form, loading, saving, save, reset }
}
