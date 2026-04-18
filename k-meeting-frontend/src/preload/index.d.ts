import type { ElectronAPI } from '@electron-toolkit/preload'
import type { IpcRendererEvent } from 'electron'

interface AppIpcRenderer {
  send(channel: string, ...args: unknown[]): void
  invoke<T = any>(channel: string, ...args: unknown[]): Promise<T>
  on(channel: string, listener: (event: IpcRendererEvent, ...args: any[]) => void): void
  removeAllListeners(channel: string): void
}

type AppElectronAPI = Omit<ElectronAPI, 'ipcRenderer'> & {
  ipcRenderer: AppIpcRenderer
}

declare global {
  interface Window {
    electron: AppElectronAPI
    api: unknown
  }
}
