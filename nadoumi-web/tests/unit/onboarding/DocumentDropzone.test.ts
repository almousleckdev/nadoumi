import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DocumentDropzone from '~/components/onboarding/DocumentDropzone.vue'
import { fakeFile, pickFile } from '../helpers/files'

const props = {
  accept: 'image/png,application/pdf', mime: /^(image\/png|application\/pdf)$/, maxMb: 1,
  badTypeMessage: 'Choose a PNG or PDF.', hint: 'PNG or PDF, up to 1 MB',
}

describe('DocumentDropzone', () => {
  it('emits the file when it is an allowed type within the size cap', async () => {
    const w = await mountSuspended(DocumentDropzone, { props })
    const file = fakeFile('p.png', 'image/png')

    await pickFile(w, file)

    expect(w.emitted('select')?.[0]).toEqual([file])
    expect(w.find('[role="alert"]').exists()).toBe(false)
  })

  it('refuses a disallowed type with the given message', async () => {
    const w = await mountSuspended(DocumentDropzone, { props })

    await pickFile(w, fakeFile('p.gif', 'image/gif'))

    expect(w.emitted('select')).toBeUndefined()
    expect(w.text()).toContain('Choose a PNG or PDF.')
  })

  it('refuses a file over the size cap', async () => {
    const w = await mountSuspended(DocumentDropzone, { props })

    await pickFile(w, fakeFile('big.png', 'image/png', 2 * 1024 * 1024))

    expect(w.emitted('select')).toBeUndefined()
    expect(w.text()).toContain('1 MB')
  })

  it('shows the format hint and switches its label to Replace', async () => {
    const w = await mountSuspended(DocumentDropzone, { props: { ...props, replace: true } })

    expect(w.text()).toContain('PNG or PDF, up to 1 MB')
    expect(w.text()).toContain('Replace')
  })
})
