import type { SourcesOptions } from 'electron'
import type { SysSetting } from '@model/system'
import type { ChangeLocalFolderPayload, OpenLocalFilePayload } from './system'
import type { LoginSuccessPayload } from './auth'
import type { ScreenSource, StartRecordingPayload } from './media'
import type { PeerConnectionPayload } from './meeting'
import type { OpenWindowPayload, WinOpPayload } from './window'

export interface DownloadUpdatePayload {
  id: number | string
  downloadUrl: string
  fileName?: string
}

export interface OpenUrlPayload {
  url: string
}

export interface CloseWindowPayload {
  windowId: string
}

export interface RealtimeMessage {
  messageType: number
  messageContent: any
  sendUserId?: string | number
  receiveUserId?: string | number
  sendUserNickName?: string
}

export interface IpcSendPayloadMap {
  downloadUpdate: DownloadUpdatePayload
  openLocalFile: OpenLocalFilePayload
  openUrl: OpenUrlPayload
  openWindow: OpenWindowPayload
  ping: void
  sendPeerConnection: PeerConnectionPayload
  winTitleOp: WinOpPayload
}

export interface IpcInvokeRequestMap {
  changeLocalFolder: ChangeLocalFolderPayload
  getScreenSource: SourcesOptions
  getSysSetting: void
  loginOrRegister: boolean
  loginSuccess: LoginSuccessPayload
  logout: void
  saveSysSetting: string | SysSetting
  startRecording: StartRecordingPayload
  stopRecording: void
}

export interface IpcInvokeResponseMap {
  changeLocalFolder: string | undefined
  getScreenSource: ScreenSource[]
  getSysSetting: SysSetting
  loginOrRegister: void
  loginSuccess: void
  logout: void
  saveSysSetting: void
  startRecording: void
  stopRecording: void
}

export interface IpcOnPayloadMap {
  closeWindow: CloseWindowPayload
  finishRecording: string
  logout: void
  mainMessage: RealtimeMessage
  meetingMessage: RealtimeMessage
  preCloseWindow: void
  reconnect: boolean
  recordTime: number
  updateDownloadCallback: number
  winIsMax: boolean
}
