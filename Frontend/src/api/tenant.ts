import { api } from './index'


export async function getTenant()
{
  const { data } = await api.get('/tenant')
  return(data as { success: boolean; tenant: string; name: string })
}
