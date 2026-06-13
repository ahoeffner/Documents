import { api } from './index'

export const synthesizeSpeech = (text: string) =>
  api.post('/tts', { text }, { responseType: 'blob' })
