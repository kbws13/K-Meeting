import type { AppElectronAPI } from '@model/electron'

declare global {
  interface Window {
    electron: AppElectronAPI
    api: unknown
  }
}

export {}
