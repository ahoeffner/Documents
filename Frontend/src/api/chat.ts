import { api } from './index'
import type { ChatPayload } from '../types'

export const chat = (payload: ChatPayload) => api.post('/ai', payload)
