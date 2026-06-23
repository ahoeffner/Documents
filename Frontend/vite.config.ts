import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      devOptions: {
        enabled: true
      },
      workbox: {
        navigateFallback: 'index.html'
      },
      manifest: false
    })
  ],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
