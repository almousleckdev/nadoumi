import { ElMessageBox } from 'element-plus'

export interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  /** 'danger' styles the confirm button as a destructive action */
  tone?: 'primary' | 'danger'
}

/**
 * One confirmation surface for the whole admin — a thin, typed wrapper over
 * ElMessageBox so modules never hand-roll their own confirm dialog.
 * Resolves `true` on confirm, `false` on cancel/dismiss (never rejects).
 */
export function useConfirm() {
  async function confirm(opts: ConfirmOptions): Promise<boolean> {
    try {
      await ElMessageBox.confirm(opts.message, opts.title ?? 'Please confirm', {
        confirmButtonText: opts.confirmText ?? 'Confirm',
        cancelButtonText: opts.cancelText ?? 'Cancel',
        type: opts.tone === 'danger' ? 'warning' : 'info',
        confirmButtonClass: opts.tone === 'danger' ? 'el-button--danger' : '',
        draggable: true,
      })
      return true
    }
    catch {
      return false
    }
  }

  return { confirm }
}
