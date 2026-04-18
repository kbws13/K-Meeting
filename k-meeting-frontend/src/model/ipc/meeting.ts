import type { UserId } from '@model/user'

export interface PeerConnectionPayload {
  sendUserId: UserId
  receiveUserId: UserId
  signalType: string | number
  signalData: string
  token?: string
}
