import { api } from './index'
import type { SearchPayload } from '../types'


export const search = (payload: SearchPayload) => api.post('/search', payload)
export const extractSearchTerms = (query: string, languageIndependent: boolean) =>
  api.post('/search/extract', { query, languageIndependent })
export const listDocuments = (fldid?: number | null, q?: string) =>
  api.get('/documents', { params: { fldid: fldid ?? undefined, q: q || undefined } })
export const getDocument = (id: number) => api.get(`/documents/${id}`)
export const updateDocument = (id: number, fd: FormData) => api.put(`/documents/${id}`, fd)
export const deleteDocument = (id: number) => api.delete(`/documents/${id}`)
export const linkDocuments = (fldid: number, docids: (number | string)[]) => api.post('/links', { fldid, docids })
export const moveDocument = (id: number | string, fldid: number) =>
{
  const fd = new FormData()
  fd.append('fldid', String(fldid))
  return(api.put(`/documents/${id}`, fd))
}
export const deleteLink = (id: number) => api.delete(`/links/${id}`)
