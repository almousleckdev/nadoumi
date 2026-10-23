import type { Major } from '@/api/major'

/** "Computer Science · 计算机科学": the name with its Chinese name when there is one. */
export function majorLabel(major: Pick<Major, 'name' | 'nameCn'>): string {
  return major.nameCn ? `${major.name} · ${major.nameCn}` : major.name
}
