/** Per-region colour used for the province cards and university markers. */
export interface RegionTint {
  grad: string
  fg: string
}

const TINTS: Record<string, RegionTint> = {
  north: { grad: 'linear-gradient(135deg,#be123c,#7f1d1d)', fg: '#be123c' },
  northeast: { grad: 'linear-gradient(135deg,#1d4ed8,#1e3a8a)', fg: '#1d4ed8' },
  east: { grad: 'linear-gradient(135deg,#0d9488,#134e4a)', fg: '#0d9488' },
  central: { grad: 'linear-gradient(135deg,#ea580c,#7c2d12)', fg: '#c2410c' },
  south: { grad: 'linear-gradient(135deg,#059669,#064e3b)', fg: '#059669' },
  southwest: { grad: 'linear-gradient(135deg,#7c3aed,#4c1d95)', fg: '#7c3aed' },
  northwest: { grad: 'linear-gradient(135deg,#b45309,#713f12)', fg: '#b45309' },
}

export const regionTint = (id: string): RegionTint =>
  TINTS[id] ?? { grad: 'linear-gradient(135deg,#334155,#0f172a)', fg: '#334155' }
