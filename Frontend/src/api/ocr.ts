import { api } from './index'

export const scanOcr = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/ocr', fd, { responseType: 'text' })
}
