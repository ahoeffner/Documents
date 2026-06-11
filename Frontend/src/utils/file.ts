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


export function openOrDownload(id: number, filename: string | undefined | null, confirmMessage: string)
{
  if (isViewable(filename))
  {
    window.open(`/api/content/${id}/file`, '_blank')
  }
  else if (window.confirm(confirmMessage))
  {
    window.location.href = `/api/content/${id}/download`
  }
}
