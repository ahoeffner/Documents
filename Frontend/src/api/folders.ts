import { api } from './index'

export const listFolders = () => api.get('/categories')
export const getFolderDocuments = (id: number) => api.get(`/categories/${id}/documents`)
export const createFolder = (name: string, pid: number | null) => api.post('/categories', { name, pid })
export const deleteFolder = (id: number) => api.delete(`/categories/${id}`)
