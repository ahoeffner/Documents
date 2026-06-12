import { useI18nStore } from '../stores/i18n'
import { useConfirmStore } from '../stores/confirm'


const VIEWABLE_EXTENSIONS = new Set([
  'pdf', 'png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico',
  'txt', 'csv', 'html', 'htm', 'json', 'xml',
  'mp3', 'wav', 'ogg', 'mp4', 'webm', 'mov'
])


export function isViewable(filename?: string | null): boolean
{
  if (!filename) return(false)
  const ext = filename.split('.').pop()?.toLowerCase()
  return(!!ext && VIEWABLE_EXTENSIONS.has(ext))
}


export async function openOrDownload(id: number, filename: string | undefined | null, confirmMessage: string)
{
  if (isViewable(filename))
  {
    window.open(`/api/content/${id}/file`, '_blank')
    return
  }

  const i18n = useI18nStore()
  const confirm = useConfirmStore()
  const ok = await confirm.ask({
    title: i18n.t('chat.downloadFileTitle'),
    message: confirmMessage,
    confirmLabel: i18n.t('common.download')
  })
  if (ok) window.location.href = `/api/content/${id}/download`
}
