import { defineStore } from 'pinia'
import { listLocales, getLocale } from '../api/i18n'


interface LocaleInfo
{
  id: string
  name: string
}

type Messages = Record<string, unknown>

const STORAGE_KEY = 'locale'


export const useI18nStore = defineStore('i18n', {
  state: () => ({
    locale: localStorage.getItem(STORAGE_KEY) || 'en',
    locales: [] as LocaleInfo[],
    messages: {} as Messages
  }),
  actions: {
    async init()
    {
      try
      {
        const res = await listLocales()
        this.locales = (res.data.locales || []) as LocaleInfo[]
      }
      catch
      {
        this.locales = []
      }
      await this.loadMessages()
    },


    async setLocale(id: string)
    {
      this.locale = id
      localStorage.setItem(STORAGE_KEY, id)
      await this.loadMessages()
    },


    async loadMessages()
    {
      try
      {
        const res = await getLocale(this.locale)
        this.messages = (res.data.messages || {}) as Messages
      }
      catch
      {
        this.messages = {}
      }
    },


    t(key: string, params?: Record<string, string | number>): string
    {
      const parts = key.split('.')
      let value: unknown = this.messages
      for (const part of parts)
      {
        if (typeof value !== 'object' || value === null) return(key)
        value = (value as Record<string, unknown>)[part]
      }

      if (typeof value !== 'string') return(key)

      if (!params) return(value)
      return(value.replace(/\{(\w+)\}/g, (_, name) => String(params[name] ?? `{${name}}`)))
    }
  }
})
