import { defineStore } from 'pinia'
import type { Category } from '../types'
import { listCategories } from '../api/categories'


export const useCategoriesStore = defineStore('categories', {
  state: () => ({
    categories: [] as Category[],
    loading: false,
    error: null as string | null
  }),
  actions: {
    async load()
    {
      this.loading = true
      this.error = null
      try
      {
        const res = await listCategories()
        this.categories = (res.data.folders || []) as Category[]
      }
      catch
      {
        this.error = 'Failed to load categories'
      }
      finally
      {
        this.loading = false
      }
    }
  }
})
