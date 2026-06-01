import { api } from './index'
import type { ChatPayload } from '../types'


export const chat = (payload: ChatPayload, signal?: AbortSignal) =>
  api.post('/ai', payload, { signal })
