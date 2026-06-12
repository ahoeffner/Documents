import { api } from './index'


export const transcribeAudio = (blob: Blob) =>
{
  const fd = new FormData()
  fd.append('audio', blob, 'recording.webm')
  return(api.post('/transcribe', fd))
}
