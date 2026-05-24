import { api } from './index'

export const listCategories = () => api.get('/categories')
export const createCategory = (name: string) => api.post('/categories', { name })
export const deleteCategory = (id: number) => api.delete(`/categories/${id}`)
