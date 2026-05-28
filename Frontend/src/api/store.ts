import { api } from './index'


export const storeDocument = (formData: FormData) => api.post('/store', formData)
