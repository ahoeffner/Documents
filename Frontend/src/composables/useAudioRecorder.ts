import { ref } from 'vue'


const SILENCE_THRESHOLD = 0.02
const SILENCE_DURATION = 2400


export function useAudioRecorder()
{
  const recording = ref(false)
  let mediaRecorder: MediaRecorder | null = null
  let chunks: Blob[] = []
  let audioCtx: AudioContext | null = null
  let rafId: number | null = null
  let silenceStart: number | null = null


  function watchSilence(stream: MediaStream, onSilence: () => void)
  {
    audioCtx = new AudioContext()
    const source = audioCtx.createMediaStreamSource(stream)
    const analyser = audioCtx.createAnalyser()
    analyser.fftSize = 512
    source.connect(analyser)
    const data = new Uint8Array(analyser.frequencyBinCount)

    function check()
    {
      analyser.getByteTimeDomainData(data)
      let sum = 0
      for (const v of data)
      {
        const d = (v - 128) / 128
        sum += d * d
      }
      const rms = Math.sqrt(sum / data.length)

      if (rms < SILENCE_THRESHOLD)
      {
        if (silenceStart === null) silenceStart = performance.now()
        else if (performance.now() - silenceStart > SILENCE_DURATION)
        {
          onSilence()
          return
        }
      }
      else
      {
        silenceStart = null
      }
      rafId = requestAnimationFrame(check)
    }

    rafId = requestAnimationFrame(check)
  }


  function cleanup()
  {
    if (rafId !== null) { cancelAnimationFrame(rafId); rafId = null }
    if (audioCtx) { audioCtx.close(); audioCtx = null }
    silenceStart = null
  }


  function start(): Promise<Blob>
  {
    return(new Promise((resolve, reject) =>
    {
      navigator.mediaDevices.getUserMedia({ audio: true }).then(stream =>
      {
        mediaRecorder = new MediaRecorder(stream)
        chunks = []
        mediaRecorder.ondataavailable = e => chunks.push(e.data)
        mediaRecorder.onstop = () =>
        {
          recording.value = false
          cleanup()
          stream.getTracks().forEach(t => t.stop())
          resolve(new Blob(chunks, { type: 'audio/webm' }))
        }
        mediaRecorder.start()
        recording.value = true
        watchSilence(stream, () => stop())
      }).catch(reject)
    }))
  }


  function stop()
  {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop()
  }


  return({ recording, start, stop })
}
