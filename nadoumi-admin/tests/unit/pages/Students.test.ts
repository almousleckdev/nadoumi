import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const system = vi.hoisted(() => ({ listUsers: vi.fn(), changeUserStatus: vi.fn() }))
vi.mock('@/api/system', () => system)

const conversation = vi.hoisted(() => ({ createConversation: vi.fn() }))
vi.mock('@/api/conversation', () => conversation)

const ui = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn(), confirm: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { ...actual.ElMessage, success: ui.success, error: ui.error },
    ElMessageBox: { ...actual.ElMessageBox, confirm: ui.confirm },
  }
})

import Students from '@/views/students/index.vue'

const amina = { userId: 21, userName: 'amina', nickName: 'Amina', email: 'amina@example.com', phonenumber: '', status: '0', createTime: '2026-01-01', loginDate: null, avatar: null }
const karim = { userId: 22, userName: 'karim', nickName: 'Karim', email: null, phonenumber: '', status: '1', createTime: '2026-01-02', loginDate: null, avatar: null }

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }],
})

async function mountStudents() {
  const w = mount(Students, {
    global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router], stubs: { StudentDrawer: true } },
  })
  await flushPromises()
  return w
}

const rowFor = (w: ReturnType<typeof mount>, text: string) => w.findAll('tbody tr').find(r => r.text().includes(text))!

describe('Students', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    Object.values(system).forEach(fn => fn.mockReset())
    conversation.createConversation.mockReset().mockResolvedValue({ conversationId: 5 })
    Object.values(ui).forEach(fn => fn.mockReset())
    system.listUsers.mockResolvedValue({ rows: [amina, karim], total: 2 })
    system.changeUserStatus.mockResolvedValue(undefined)
    ui.confirm.mockResolvedValue('confirm')
    await router.push('/students')
  })

  it('lists only registered students, first page of twenty', async () => {
    const w = await mountStudents()
    expect(system.listUsers).toHaveBeenCalledWith({ userType: '10', userName: undefined, status: undefined, pageNum: 1, pageSize: 20 })
    expect(w.text()).toContain('Amina')
    expect(w.text()).toContain('@karim')
    expect(w.text()).toContain('amina@example.com')
  })

  it('shows the account status of each student', async () => {
    const w = await mountStudents()
    expect(rowFor(w, 'Amina').text()).toContain('Active')
    expect(rowFor(w, 'Karim').text()).toContain('Suspended')
  })

  it('searches by name and status', async () => {
    const w = await mountStudents()
    const search = w.find('input.el-input__inner')
    await search.setValue('ami')
    await search.trigger('keyup.enter')
    await flushPromises()
    expect(system.listUsers).toHaveBeenLastCalledWith(expect.objectContaining({ userType: '10', userName: 'ami', pageNum: 1 }))
  })

  it('requests the page the user asks for, not the first one again', async () => {
    system.listUsers.mockResolvedValue({ rows: [amina], total: 45 })
    const w = await mountStudents()
    await w.findComponent({ name: 'ElPagination' }).vm.$emit('update:current-page', 2)
    await flushPromises()
    expect(system.listUsers).toHaveBeenLastCalledWith(expect.objectContaining({ userType: '10', pageNum: 2 }))
  })

  it('opens the contact dialog for a student with just a message box (no subject)', async () => {
    const w = await mountStudents()
    await rowFor(w, 'Amina').findAll('button').find(b => b.text() === 'Contact Student')!.trigger('click')
    await flushPromises()
    expect(w.find('.el-dialog').text()).toContain('Message Amina')
    expect(w.find('.el-dialog input').exists()).toBe(false)
    expect(w.find('.el-dialog textarea').exists()).toBe(true)
  })

  it('sends a direct message to the chosen student and offers to open the conversation', async () => {
    ui.confirm.mockResolvedValueOnce('confirm')
    const w = await mountStudents()
    await rowFor(w, 'Amina').findAll('button').find(b => b.text() === 'Contact Student')!.trigger('click')
    await flushPromises()

    await w.find('.el-dialog textarea').setValue('  Please upload your passport.  ')
    await w.find('.el-dialog').findAll('button').find(b => b.text() === 'Send Message')!.trigger('click')
    await flushPromises()

    expect(conversation.createConversation).toHaveBeenCalledWith({
      studentUserId: 21, body: 'Please upload your passport.',
    })
    expect(ui.success).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/conversations')
    expect(router.currentRoute.value.query.id).toBe('5')
  })

  it('does not send an empty message', async () => {
    const w = await mountStudents()
    await rowFor(w, 'Amina').findAll('button').find(b => b.text() === 'Contact Student')!.trigger('click')
    await flushPromises()
    const send = w.find('.el-dialog').findAll('button').find(b => b.text() === 'Send Message')!
    expect(send.attributes('disabled')).toBeDefined()
    expect(conversation.createConversation).not.toHaveBeenCalled()
  })

  it('reports a failed message instead of closing silently', async () => {
    conversation.createConversation.mockRejectedValue(new Error('recipient blocked'))
    const w = await mountStudents()
    await rowFor(w, 'Amina').findAll('button').find(b => b.text() === 'Contact Student')!.trigger('click')
    await flushPromises()
    await w.find('.el-dialog textarea').setValue('Hello')
    await w.find('.el-dialog').findAll('button').find(b => b.text() === 'Send Message')!.trigger('click')
    await flushPromises()
    expect(ui.error).toHaveBeenCalledWith('recipient blocked')
  })
})
