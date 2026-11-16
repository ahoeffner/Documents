import { defineStore } from 'pinia'
import type { ChatMessage } from '../types'


function generateId(): string
{
  return(Date.now().toString(36) + Math.random().toString(36).substring(2, 6))
}


export const useHomeStore = defineStore('home', {
  state: () => ({
    sessionId: generateId(),
    messages: [] as ChatMessage[]
  }),
  actions: {
    addMessage(role: 'user' | 'ai', text: string)
    {
      this.messages.push({ id: generateId(), role, text, documents: null, timestamp: new Date() })
    },
    clear()
    {
      this.messages = []
      this.sessionId = generateId()
    }
  }
})
