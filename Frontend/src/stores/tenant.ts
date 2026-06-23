import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getTenant } from '../api/tenant'


export const useTenantStore = defineStore('tenant', () =>
{
  const id = ref('')
  const name = ref('')
  const title = ref('AI Documents')


  async function init()
  {
    try
    {
      const res = await getTenant()
      id.value = res.tenant
      name.value = res.name
      title.value = res.name + ' AI Documents'
      document.title = title.value
    }
    catch
    {
      document.title = title.value
    }
  }


  return({ id, name, title, init })
})
