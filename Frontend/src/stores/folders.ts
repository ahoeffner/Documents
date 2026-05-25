import { defineStore } from 'pinia'
import { listFolders } from '../api/folders'
import type { Folder } from '../types'

function buildTree(flat: Omit<Folder, 'children'>[]): Folder[] {
  const map = new Map<number, Folder>()
  flat.forEach(f => map.set(f.id, { ...f, children: [] }))
  const roots: Folder[] = []
  map.forEach(f => {
    if (f.pid === null) {
      roots.push(f)
    } else {
      map.get(f.pid)?.children.push(f)
    }
  })
  return roots
}

export const useFoldersStore = defineStore('folders', {
  state: () => ({
    tree: [] as Folder[],
    loading: false,
    error: null as string | null
  }),
  actions: {
    async load() {
      this.loading = true
      this.error = null
      try {
        const res = await listFolders()
        const flat = (res.data.folders || []) as Omit<Folder, 'children'>[]
        this.tree = buildTree(flat)
      } catch {
        this.error = 'Failed to load folders'
      } finally {
        this.loading = false
      }
    }
  }
})
