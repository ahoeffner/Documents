import './style.css'
import App from './App.vue'
import router from './router'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useI18nStore } from './stores/i18n'


const app = createApp(App)
app.use(createPinia())
app.use(router)

useI18nStore().init().then(() => app.mount('#app'))
