export function numberOrNull(value: unknown): number | null {
  return value === '' || value === null || value === undefined ? null : Number(value)
}

export function textOrNull(value: string): string | null {
  return value.trim() === '' ? null : value.trim()
}
