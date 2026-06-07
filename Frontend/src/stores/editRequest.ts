import { defineStore } from 'pinia'


export const useEditRequestStore = defineStore('editRequest', {
  state: () => ({
    pendingId: null as number | null,
    pendingNew: false
  }),
  actions:
  {
    request(id: number)
    {
      this.pendingId = id
    },
    requestNew()
    {
      this.pendingNew = true
    },
    clear()
    {
      this.pendingId = null
      this.pendingNew = false
    }
  }
})
