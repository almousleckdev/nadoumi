import { createDepartment, updateDepartment, deleteDepartment } from '@/api/university'
import type { DeptRow } from './universityForm'

export async function syncDepartments(universityId: number, current: DeptRow[], original: DeptRow[]): Promise<number> {
  const rows = current.filter(d => d.name.trim())
  const keptIds = new Set(rows.filter(d => d.id != null).map(d => d.id))
  let blocked = 0
  for (const orig of original) {
    if (orig.id != null && !keptIds.has(orig.id)) {
      try {
        await deleteDepartment(universityId, orig.id)
      }
      catch {
        blocked++
      }
    }
  }
  for (const d of rows) {
    const body = { name: d.name.trim(), nameCn: d.nameCn?.trim() || null }
    if (d.id == null) {
      await createDepartment(universityId, body)
    }
    else {
      const orig = original.find(o => o.id === d.id)
      if (!orig || orig.name !== body.name || (orig.nameCn || null) !== body.nameCn) {
        await updateDepartment(universityId, d.id, body)
      }
    }
  }
  return blocked
}
