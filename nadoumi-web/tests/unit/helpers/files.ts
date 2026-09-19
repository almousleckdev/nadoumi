import type { VueWrapper } from '@vue/test-utils'

/** A File of the given type and size (contents are irrelevant to the components under test). */
export function fakeFile(name: string, type: string, size = 1024): File {
  return new File([new Uint8Array(size)], name, { type })
}

/** Simulates choosing `file` in the wrapper's file input (jsdom cannot set `files` directly). */
export async function pickFile(wrapper: VueWrapper, file: File, selector = 'input[type="file"]') {
  const input = wrapper.find(selector)
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
}
