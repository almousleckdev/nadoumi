const ACTIVE = '0'
const SUSPENDED = '1'

export function statusTone(status: string): string {
  if (status === ACTIVE) return 'ACTIVE'
  if (status === SUSPENDED) return 'PENDING'
  return 'FAILED'
}

export function statusLabelKey(status: string): string {
  if (status === ACTIVE) return 'students.statusActive'
  if (status === SUSPENDED) return 'students.statusSuspended'
  return 'students.statusBlocked'
}

interface ChangeCopy {
  title: string
  confirm: string
  tone: 'info' | 'warning'
}

const CHANGE_COPY: Record<string, ChangeCopy> = {
  '1': { title: 'students.suspend', confirm: 'students.suspendConfirm', tone: 'warning' },
  '2': { title: 'students.block', confirm: 'students.blockConfirm', tone: 'warning' },
  '0': { title: 'students.unblock', confirm: 'students.unblockConfirm', tone: 'info' },
}

export function statusChangeCopy(newStatus: string): ChangeCopy {
  return CHANGE_COPY[newStatus] ?? CHANGE_COPY[ACTIVE]!
}
