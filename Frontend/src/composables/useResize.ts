import { ref } from 'vue'


export function useResize(initial: number, min = 120, max = 800)
{
  const width = ref(initial)

  function startResize(e: MouseEvent)
  {
    e.preventDefault()
    const startX = e.clientX
    const startW = width.value
    document.body.style.userSelect = 'none'
    document.body.style.cursor = 'col-resize'

    function onMove(e: MouseEvent)
    {
      width.value = Math.max(min, Math.min(max, startW + (e.clientX - startX)))
    }

    function onUp()
    {
      document.removeEventListener('mousemove', onMove)
      document.removeEventListener('mouseup', onUp)
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
    }

    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
  }

  return({ width, startResize })
}
