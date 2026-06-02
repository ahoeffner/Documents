import { defineStore } from 'pinia'


export const useEditRequestStore = defineStore('editRequest', {
  state: () => ({
    pendingId: null as number | null
  }),
  actions:
  {
    request(id: number)
    {
      this.pendingId = id
    },
    clear()
    {
      this.pendingId = null
    }
  }
})
