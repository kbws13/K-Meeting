import type { PersistedUserInfo } from '@model/user'

export interface LoginSuccessPayload {
  userInfo: PersistedUserInfo
  wsUrl: string
}
