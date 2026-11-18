import { api } from './index'


export const home = (payload: { id: string; query: string }, signal?: AbortSignal) =>
  api.post('/home', payload, { signal })


export const getPanel = () =>
  api.get('/home/panel')


export const controlPanel = (id: string, action: string, value?: number) =>
  api.post('/home/panel', { id, action, value })
