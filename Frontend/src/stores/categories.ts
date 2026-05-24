import { defineStore } from 'pinia'
import { listCategories } from '../api/categories'
import type { Category } from '../types'

export const useCategoriesStore = defineStore('categories', {
  state: () => ({
    categories: [] as Category[],
    loading: false,
    error: null as string | null
  }),
  actions: {
    async load() {
      this.loading = true
      this.error = null
      try {
        const res = await listCategories()
        this.categories = (res.data.categories || []) as Category[]
      } catch {
        this.error = 'Failed to load categories'
      } finally {
        this.loading = false
      }
    }
  }
})
