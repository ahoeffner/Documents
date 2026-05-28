import { defineStore } from 'pinia'
import type { ChatMessage, DocumentResult } from '../types'


function generateId(): string
{
  return(Date.now().toString(36) + Math.random().toString(36).substring(2, 6))
}


export const useChatStore = defineStore('chat', {
  state: () => ({
    sessionId: generateId(),
    messages: [] as ChatMessage[]
  }),
  actions: {
    addMessage(role: 'user' | 'ai', text: string, documents: DocumentResult[] | null = null)
    {
      this.messages.push({ id: generateId(), role, text, documents, timestamp: new Date() })
    },
    clear()
    {
      this.messages = []
      this.sessionId = generateId()
    }
  }
})
