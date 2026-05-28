import { api } from './index'


export const listFolders = () => api.get('/folders')
export const getFolderDocuments = (id: number) => api.get(`/folders/${id}/documents`)
export const createFolder = (name: string, pid: number | null) => api.post('/folders', { name, pid })
export const renameFolder = (id: number, name: string) => api.put(`/folders/${id}`, { name })
export const deleteFolder = (id: number) => api.delete(`/folders/${id}`)
