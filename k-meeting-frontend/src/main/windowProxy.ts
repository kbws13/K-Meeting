import type { BrowserWindow } from 'electron'

export type ManagedBrowserWindow = BrowserWindow & {
  forceClose?: boolean
}

const windowManage: Record<string, ManagedBrowserWindow> = {}

const saveWindow = (id: string, window: ManagedBrowserWindow): void => {
  windowManage[id] = window
}

const getWindow = (id: string): ManagedBrowserWindow | undefined => {
  return windowManage[id]
}

const delWindow = (id: string): void => {
  delete windowManage[id]
}

const getWindowManage = (): Record<string, ManagedBrowserWindow> => {
  return windowManage
}

export { saveWindow, getWindow, delWindow, getWindowManage }
