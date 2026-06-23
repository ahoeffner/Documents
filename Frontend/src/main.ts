import './style.css'
import App from './App.vue'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useI18nStore } from './stores/i18n'
import { useTenantStore } from './stores/tenant'
import 'virtual:pwa-register'


const app = createApp(App)
app.use(createPinia())

Promise.all([useTenantStore().init(), useI18nStore().init()]).then(() => app.mount('#app'))
