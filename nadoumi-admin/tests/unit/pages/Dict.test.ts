import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listDictTypes: vi.fn(),
  getDictType: vi.fn(),
  createDictType: vi.fn(),
  updateDictType: vi.fn(),
  deleteDictTypes: vi.fn(),
  listDictData: vi.fn(),
  getDictData: vi.fn(),
  createDictData: vi.fn(),
  updateDictData: vi.fn(),
  deleteDictData: vi.fn(),
}))
vi.mock('@/api/system', () => api)

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

const message = vi.hoisted(() => ({ success: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, success: message.success } }
})

import Dict from '@/views/dict/index.vue'
import { useUserStore } from '@/stores/user'

const sexType = { dictId: 1, dictName: 'Sex', dictType: 'sys_user_sex', status: '0', remark: '' }
const statusType = { dictId: 2, dictName: 'Status', dictType: 'sys_status', status: '0', remark: '' }
const entries = [
  { dictCode: 10, dictSort: 1, dictLabel: 'Male', dictValue: '0', isDefault: 'Y', status: '0', remark: '' },
  { dictCode: 11, dictSort: 2, dictLabel: 'Female', dictValue: '1', isDefault: 'N', status: '0', remark: '' },
]

async function mountDict() {
  const w = mount(Dict, mountOpts())
  await flushPromises()
  return w
}

const buttons = (w: ReturnType<typeof mount>, text: string) => w.findAll('button').filter(b => b.text() === text)
const row = (w: ReturnType<typeof mount>, text: string) => w.findAll('tbody tr').find(r => r.text().includes(text))!
const rowButton = (w: ReturnType<typeof mount>, rowText: string, label: string) =>
  row(w, rowText).findAll('button').find(b => b.text() === label)!

describe('Dictionary page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    Object.values(api).forEach(fn => fn.mockReset())
    confirm.mockReset()
    message.success.mockReset()
    api.listDictTypes.mockResolvedValue({ rows: [sexType, statusType] })
    api.listDictData.mockResolvedValue({ rows: entries })
    api.getDictType.mockResolvedValue({ data: sexType })
    api.getDictData.mockResolvedValue({ data: entries[0] })
    for (const name of ['createDictType', 'updateDictType', 'deleteDictTypes', 'createDictData', 'updateDictData', 'deleteDictData'] as const) {
      api[name].mockResolvedValue(undefined)
    }
  })

  it('lists the dictionary types and asks for a type before showing entries', async () => {
    const w = await mountDict()
    expect(api.listDictTypes).toHaveBeenCalledWith({ dictName: undefined, pageNum: 1, pageSize: 100 })
    expect(w.text()).toContain('sys_user_sex')
    expect(w.text()).toContain('sys_status')
    expect(api.listDictData).not.toHaveBeenCalled()
    expect(buttons(w, 'New entry').length + buttons(w, 'New data').length).toBe(0)
  })

  it('loads the entries of the clicked type', async () => {
    const w = await mountDict()
    await row(w, 'sys_user_sex').trigger('click')
    await flushPromises()

    expect(api.listDictData).toHaveBeenCalledWith({ dictType: 'sys_user_sex', pageNum: 1, pageSize: 100 })
    expect(w.text()).toContain('Male')
    expect(w.text()).toContain('Female')
  })

  it('searches types by name when the query is confirmed', async () => {
    const w = await mountDict()
    const search = w.find('input.el-input__inner')
    await search.setValue('Sex')
    await search.trigger('keyup.enter')
    await flushPromises()
    expect(api.listDictTypes).toHaveBeenLastCalledWith({ dictName: 'Sex', pageNum: 1, pageSize: 100 })
  })

  it('creates a type and reloads the list', async () => {
    const w = await mountDict()
    await buttons(w, 'New type')[0]!.trigger('click')
    await flushPromises()
    const drawerSave = w.findAllComponents({ name: 'Drawer' })[0]!
    drawerSave.vm.$emit('save')
    await flushPromises()

    expect(api.createDictType).toHaveBeenCalledOnce()
    expect(api.updateDictType).not.toHaveBeenCalled()
    expect(message.success).toHaveBeenCalled()
    expect(api.listDictTypes).toHaveBeenCalledTimes(2)
  })

  it('edits an existing type by loading it first and then updating', async () => {
    const w = await mountDict()
    await rowButton(w, 'sys_user_sex', 'Edit').trigger('click')
    await flushPromises()
    expect(api.getDictType).toHaveBeenCalledWith(1)

    w.findAllComponents({ name: 'Drawer' })[0]!.vm.$emit('save')
    await flushPromises()
    expect(api.updateDictType).toHaveBeenCalledWith(expect.objectContaining({ dictType: 'sys_user_sex' }))
    expect(api.createDictType).not.toHaveBeenCalled()
  })

  it('deletes a type only after confirmation and clears the entries pane when it was selected', async () => {
    const w = await mountDict()
    await row(w, 'sys_user_sex').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Male')

    confirm.mockResolvedValueOnce(false)
    await rowButton(w, 'sys_user_sex', 'Delete').trigger('click')
    await flushPromises()
    expect(api.deleteDictTypes).not.toHaveBeenCalled()

    confirm.mockResolvedValueOnce(true)
    await rowButton(w, 'sys_user_sex', 'Delete').trigger('click')
    await flushPromises()
    expect(api.deleteDictTypes).toHaveBeenCalledWith([1])
    expect(w.text()).not.toContain('Male')
  })

  it('saves an entry against the selected type and reloads its entries', async () => {
    const w = await mountDict()
    await row(w, 'sys_user_sex').trigger('click')
    await flushPromises()
    await buttons(w, 'New entry').concat(buttons(w, 'New data'))[0]!.trigger('click')
    await flushPromises()

    w.findAllComponents({ name: 'Drawer' })[1]!.vm.$emit('save')
    await flushPromises()

    expect(api.createDictData).toHaveBeenCalledWith(expect.objectContaining({ dictType: 'sys_user_sex' }))
    expect(api.listDictData).toHaveBeenCalledTimes(2)
  })

  it('deletes an entry only after confirmation', async () => {
    const w = await mountDict()
    await row(w, 'sys_user_sex').trigger('click')
    await flushPromises()

    confirm.mockResolvedValueOnce(true)
    await rowButton(w, 'Female', 'Delete').trigger('click')
    await flushPromises()
    expect(api.deleteDictData).toHaveBeenCalledWith([11])
  })

  it('hides add and delete controls without the matching permissions', async () => {
    useUserStore().permissions = ['system:dict:list']
    const w = await mountDict()
    expect(buttons(w, 'New type')).toHaveLength(0)
    expect(buttons(w, 'Delete')).toHaveLength(0)
  })
})
