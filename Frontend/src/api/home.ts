import { api } from './index'


export const home = (payload: { id: string; query: string }, signal?: AbortSignal) =>
  api.post('/home', payload, { signal })
