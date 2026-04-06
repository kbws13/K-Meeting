import { resolve } from 'path'
import { defineConfig } from 'electron-vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  main: {},
  preload: {},
  renderer: {
    resolve: {
      alias: {
        '@': resolve('src/renderer/src')
      }
    },
    plugins: [vue()],
    server: {
      historyApiFallback: true,
      hmr: true,
      port: 6001,
      proxy: {
        "/api": {
          target: "http://127.0.0.1:6060",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, "/api"),
        }
      }
    }
  }
})
