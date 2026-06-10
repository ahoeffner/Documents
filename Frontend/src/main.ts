import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './style.css'
import { useI18nStore } from './stores/i18n'


const app = createApp(App)
app.use(createPinia())
app.use(router)

useI18nStore().init().then(() => app.mount('#app'))
