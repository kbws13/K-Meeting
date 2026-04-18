export interface WindowCloseData {
  closeType: 0 | 1
  forceClose: boolean
}

export type WindowAction = 'close' | 'minimize' | 'maximize' | 'unmaximize'

export interface WinOpPayload {
  action: WindowAction
  data?: WindowCloseData
}

export type WindowRouteParams = Record<string, string | number | boolean | null | undefined>

export interface OpenWindowPayload {
  windowId: string
  title?: string
  path: string
  width?: number
  height?: number
  data?: WindowRouteParams
  maximizable?: boolean
}
