import { api } from '../api'
import { getTenants } from '../api/auth'
import { useAuthStore } from '../stores/auth'


const HEARTBEAT_MS = 10 * 60 * 1000


// Logged in while the app is open, logged out when it closes:
// a heartbeat keeps the server session alive, closing the app ends it,
// and if it expired anyway (e.g. long sleep) the login screen is shown instead of failing requests
export function watchSession()
{
  const auth = useAuthStore()

  api.interceptors.response.use(res => res, err =>
  {
    const url: string = err.config?.url ?? ''
    if (err.response?.status === 401 && !url.startsWith('/auth/')) auth.expire()
    return(Promise.reject(err))
  })

  document.addEventListener('visibilitychange', () =>
  {
    if (document.visibilityState === 'visible') check()
  })

  window.setInterval(check, HEARTBEAT_MS)

  window.addEventListener('pagehide', () =>
  {
    if (auth.isLoggedIn) navigator.sendBeacon('/api/auth/logout')
  })
}


// Touches the server session; if it is gone, go to login right away
async function check()
{
  const auth = useAuthStore()
  if (!auth.isLoggedIn) return

  try
  {
    const res = await getTenants()
    if (!res.tenants.length) auth.expire()
  }
  catch
  {
    /* offline or server restarting: leave the state alone */
  }
}
