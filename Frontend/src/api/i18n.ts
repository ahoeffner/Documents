import { api } from './index'


export const listLocales = () => api.get('/i18n/locales')
export const getLocale = (id: string) => api.get(`/i18n/${id}`)
