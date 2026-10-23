import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * A visually hidden file input is absolutely positioned. If its wrapper is not itself positioned, the input is placed
 * against a far ancestor and does not scroll with an inner scroll area; focusing it then makes the browser scroll that
 * ancestor, so a drawer or a fixed shell jumps. Every wrapper must be `relative` (or `absolute`).
 */
function vueFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name)
    return statSync(path).isDirectory() ? vueFiles(path) : path.endsWith('.vue') ? [path] : []
  })
}

describe('hidden file inputs', () => {
  it('sit inside a positioned wrapper', () => {
    const offenders: string[] = []
    for (const file of vueFiles('app')) {
      const source = readFileSync(file, 'utf8')
      for (const match of source.matchAll(/<input[^>]*type="file"[^>]*class="sr-only"/g)) {
        const before = source.slice(0, match.index)
        const wrapper = before.slice(before.lastIndexOf('<label'))
        if (!/\b(relative|absolute)\b/.test(wrapper.slice(0, wrapper.indexOf('>') + 1))) offenders.push(file)
      }
    }
    expect(offenders).toEqual([])
  })
})
