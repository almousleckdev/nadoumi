import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { mountOpts } from '../../helpers'
import FieldsEditor from '@/views/scholarships/form/FieldsEditor.vue'
import { blankForm, scholarshipFormKey } from '@/views/scholarships/scholarshipForm'

function mountEditor(levels: string[] = [], fields: { level: string | null, name: string }[] = []) {
  const form = reactive(blankForm())
  form.levels = levels as never
  form.fields = fields as never
  const w = mount(FieldsEditor, {
    global: { ...mountOpts().global, provide: { [scholarshipFormKey as symbol]: form } },
  })
  return { w, form }
}

describe('FieldsEditor', () => {
  it('always offers the every-level field, and one more per level the scholarship offers', () => {
    const { w } = mountEditor(['BACHELOR', 'PHD'])
    expect(w.find('[data-test="fields-all"]').exists()).toBe(true)
    expect(w.find('[data-test="fields-BACHELOR"]').exists()).toBe(true)
    expect(w.find('[data-test="fields-PHD"]').exists()).toBe(true)
    expect(w.find('[data-test="fields-MASTER"]').exists()).toBe(false)
  })

  it('offers no per-level field while no level is chosen', () => {
    const { w } = mountEditor([])
    expect(w.findAll('[data-test^="fields-"]')).toHaveLength(1)
  })

  it('writes several names for every level into the form', async () => {
    const { w, form } = mountEditor(['MASTER'])
    const all = w.find('[data-test="fields-all"]').findComponent({ name: 'ElSelect' })
    all.vm.$emit('update:modelValue', ['Engineering', ' Medicine ', 'Engineering', ''])
    await w.vm.$nextTick()
    expect(form.fields).toEqual([{ level: null, name: 'Engineering' }, { level: null, name: 'Medicine' }])
  })

  it('keeps the other levels untouched when one level changes', async () => {
    const { w, form } = mountEditor(['BACHELOR', 'MASTER'], [
      { level: null, name: 'Law' }, { level: 'BACHELOR', name: 'Architecture' },
    ])
    const master = w.find('[data-test="fields-MASTER"]').findComponent({ name: 'ElSelect' })
    master.vm.$emit('update:modelValue', ['Medicine'])
    await w.vm.$nextTick()
    expect(form.fields).toEqual(expect.arrayContaining([
      { level: null, name: 'Law' }, { level: 'BACHELOR', name: 'Architecture' }, { level: 'MASTER', name: 'Medicine' },
    ]))
    expect(form.fields).toHaveLength(3)
  })

  it('drops the fields of a level that is unticked, and keeps the every-level ones', async () => {
    const { w, form } = mountEditor(['BACHELOR', 'MASTER'], [
      { level: null, name: 'Law' }, { level: 'MASTER', name: 'Medicine' },
    ])
    form.levels = ['BACHELOR'] as never
    await w.vm.$nextTick()
    expect(form.fields).toEqual([{ level: null, name: 'Law' }])
  })

  it('shows the names already stored for a level', () => {
    const { w } = mountEditor(['MASTER'], [{ level: 'MASTER', name: 'Medicine' }])
    expect(w.find('[data-test="fields-MASTER"]').findComponent({ name: 'ElSelect' }).props('modelValue')).toEqual(['Medicine'])
  })
})
