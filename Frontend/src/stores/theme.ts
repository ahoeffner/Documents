import { defineStore } from 'pinia'


export interface ThemeInfo
{
  id: string
  name: string
}


const STORAGE_KEY = 'theme'

const THEMES: ThemeInfo[] = [
  { id: 'light', name: 'Light' },
  { id: 'dark', name: 'Dark' },
  { id: 'blue', name: 'Blue' },
  { id: 'forest', name: 'Forest' },
  { id: 'sepia', name: 'Sepia' },
  { id: 'purple', name: 'Purple' }
]


export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: localStorage.getItem(STORAGE_KEY) || 'dark',
    themes: THEMES
  }),
  actions: {
    init()
    {
      this.apply(this.theme)
    },


    setTheme(id: string)
    {
      this.theme = id
      localStorage.setItem(STORAGE_KEY, id)
      this.apply(id)
    },


    apply(id: string)
    {
      document.documentElement.setAttribute('data-theme', id)
    }
  }
})
