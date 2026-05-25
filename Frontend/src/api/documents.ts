import { api } from './index'
import type { SearchPayload } from '../types'

export const search = (payload: SearchPayload) => api.post('/search', payload)
export const listDocuments = (fldid?: number | null, q?: string) =>
  api.get('/documents', { params: { fldid: fldid ?? undefined, q: q || undefined } })
export const getDocument = (id: number) => api.get(`/documents/${id}`)
export const updateDocument = (id: number, fd: FormData) => api.put(`/documents/${id}`, fd)
export const deleteDocument = (id: number) => api.delete(`/documents/${id}`)
