import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

export type DeferredTask = () => Promise<boolean>

interface Flushable {
  flush: (id: number | string) => Promise<boolean>
}

export async function runDeferred(tasks: DeferredTask[]): Promise<number> {
  let failed = 0
  for (const task of tasks) {
    try {
      if (!(await task())) failed += 1
    }
    catch {
      failed += 1
    }
  }
  return failed
}

export function uploaderTask(uploader: Flushable | null | undefined, id: number | string): DeferredTask {
  return () => (uploader ? uploader.flush(id) : Promise.resolve(true))
}

export function useDeferredImages() {
  const { t } = useI18n()

  async function flush(tasks: DeferredTask[]): Promise<boolean> {
    const failed = await runDeferred(tasks)
    if (failed > 0) ElMessage.warning(t('imageUpload.someFailed', { n: failed }))
    return failed === 0
  }

  return { flush }
}
