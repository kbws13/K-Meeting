import type { ElectronAPI } from '@electron-toolkit/preload'
import type { IpcRendererEvent } from 'electron'
import type {
  IpcInvokeRequestMap,
  IpcInvokeResponseMap,
  IpcOnPayloadMap,
  IpcSendPayloadMap
} from '@model/ipc'

type OptionalArg<T> = [T] extends [void] ? [] : [payload: T]
type ListenerArgs<T> = [T] extends [void] ? [] : [payload: T]

export interface AppIpcRenderer {
  send<C extends keyof IpcSendPayloadMap>(
    channel: C,
    ...args: OptionalArg<IpcSendPayloadMap[C]>
  ): void
  invoke<C extends keyof IpcInvokeRequestMap>(
    channel: C,
    ...args: OptionalArg<IpcInvokeRequestMap[C]>
  ): Promise<IpcInvokeResponseMap[C]>
  on<C extends keyof IpcOnPayloadMap>(
    channel: C,
    listener: (event: IpcRendererEvent, ...args: ListenerArgs<IpcOnPayloadMap[C]>) => void
  ): void
  removeAllListeners(channel: keyof IpcOnPayloadMap): void
}

export type AppElectronAPI = Omit<ElectronAPI, 'ipcRenderer'> & {
  ipcRenderer: AppIpcRenderer
}
