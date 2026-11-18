import { ref, computed, onMounted, onUnmounted, type Ref } from 'vue'


interface Position
{
  x: number
  y: number
}


// Lets a floating box be dragged around inside an area; the position is remembered per viewer.
// Without a stored position the box keeps its CSS default (upper right corner).
export function useDraggablePanel(area: Ref<HTMLElement | null>, box: Ref<HTMLElement | null>, storageKey: string)
{
  const position = ref<Position | null>(read())


  const style = computed(() =>
  {
    if (!position.value) return({})
    return({ left: position.value.x + 'px', top: position.value.y + 'px', right: 'auto' })
  })


  function read(): Position | null
  {
    try
    {
      const raw = localStorage.getItem(storageKey)
      return(raw ? JSON.parse(raw) as Position : null)
    }
    catch
    {
      return(null)
    }
  }


  function save()
  {
    try
    {
      if (position.value) localStorage.setItem(storageKey, JSON.stringify(position.value))
      else localStorage.removeItem(storageKey)
    }
    catch
    {
      /* ignore */
    }
  }


  function clamp(x: number, y: number): Position
  {
    const a = area.value
    const b = box.value
    // hidden tab (display: none) has no size; keep the position as it is
    if (!a || !b || !a.clientWidth || !b.offsetWidth) return({ x, y })

    const maxX = Math.max(0, a.clientWidth - b.offsetWidth)
    const maxY = Math.max(0, a.clientHeight - b.offsetHeight)
    return({ x: Math.round(Math.min(Math.max(0, x), maxX)), y: Math.round(Math.min(Math.max(0, y), maxY)) })
  }


  function startDrag(e: PointerEvent)
  {
    const a = area.value
    const b = box.value
    if (!a || !b || e.button !== 0) return
    e.preventDefault()

    const areaRect = a.getBoundingClientRect()
    const boxRect = b.getBoundingClientRect()
    const offsetX = e.clientX - boxRect.left
    const offsetY = e.clientY - boxRect.top

    const move = (ev: PointerEvent) =>
    {
      position.value = clamp(ev.clientX - areaRect.left - offsetX, ev.clientY - areaRect.top - offsetY)
    }

    const stop = () =>
    {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', stop)
      window.removeEventListener('pointercancel', stop)
      save()
    }

    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', stop)
    window.addEventListener('pointercancel', stop)
  }


  // Back to the default corner
  function reset()
  {
    position.value = null
    save()
  }


  // Keep the box inside the area when the window shrinks
  function onResize()
  {
    if (position.value) position.value = clamp(position.value.x, position.value.y)
  }


  onMounted(() =>
  {
    window.addEventListener('resize', onResize)
    requestAnimationFrame(onResize)
  })
  onUnmounted(() => window.removeEventListener('resize', onResize))

  return({ style, startDrag, reset })
}
