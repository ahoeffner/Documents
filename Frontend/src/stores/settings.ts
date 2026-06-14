import { defineStore } from 'pinia'


const STORAGE_KEY = 'languageIndependent'


export const useSettingsStore = defineStore('settings', {
  state: () => ({
    languageIndependent: localStorage.getItem(STORAGE_KEY) !== 'false'
  }),
  actions: {
    setLanguageIndependent(value: boolean)
    {
      this.languageIndependent = value
      localStorage.setItem(STORAGE_KEY, String(value))
    }
  }
})
