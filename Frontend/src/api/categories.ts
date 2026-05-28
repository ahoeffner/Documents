import { api } from './index'


export const listCategories = () => api.get('/folders')
export const createCategory = (name: string) => api.post('/folders', { name })
export const deleteCategory = (id: number) => api.delete(`/folders/${id}`)
