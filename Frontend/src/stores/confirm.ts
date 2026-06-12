import { defineStore } from 'pinia'


interface ConfirmOptions
{
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}


export const useConfirmStore = defineStore('confirm', {
  state: () => ({
    visible: false,
    title: '',
    message: '',
    confirmLabel: '',
    cancelLabel: '',
    danger: false,
    resolver: null as ((value: boolean) => void) | null
  }),
  actions: {
    ask(options: ConfirmOptions | string): Promise<boolean>
    {
      const opts = typeof options === 'string' ? { message: options } : options
      this.title = opts.title ?? ''
      this.message = opts.message
      this.confirmLabel = opts.confirmLabel ?? ''
      this.cancelLabel = opts.cancelLabel ?? ''
      this.danger = opts.danger ?? false
      this.visible = true
      return(new Promise(resolve => { this.resolver = resolve }))
    },


    resolve(value: boolean)
    {
      this.visible = false
      this.resolver?.(value)
      this.resolver = null
    }
  }
})
