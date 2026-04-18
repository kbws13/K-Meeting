export type UserId = string | number

export interface AppUserInfo {
  id?: UserId
  userId?: UserId
  token?: string
  meetingNo?: string
  nickName?: string
  sex?: number | string
  admin?: boolean
  [key: string]: unknown
}

export interface PersistedUserInfo extends AppUserInfo {
  id: UserId
  token: string
}
