import { resolve } from 'path'
import { defineConfig } from 'electron-vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  main: {
    resolve: {
      alias: {
        '@model': resolve('src/model')
      }
    }
  },
  preload: {
    resolve: {
      alias: {
        '@model': resolve('src/model')
      }
    }
  },
  renderer: {
    resolve: {
      alias: {
        '@': resolve('src/renderer/src'),
        '@model': resolve('src/model')
      }
    },
    plugins: [vue()],
    server: {
      host: '127.0.0.1',
      hmr: true,
      port: 6001,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8101',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        }
      }
    }
  }
})
