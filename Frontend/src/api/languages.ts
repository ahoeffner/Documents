import { api } from './index'

export const listLanguages = () => api.get('/languages')
