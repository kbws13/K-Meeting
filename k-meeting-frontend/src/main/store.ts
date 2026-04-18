import ElectronStore from 'electron-store'
import type { UserId } from '@model/user'

type StoreSchema = Record<string, unknown>

const StoreClass =
  (ElectronStore as unknown as { default?: typeof ElectronStore }).default ?? ElectronStore
const store = new StoreClass<StoreSchema>()

let userId: string | null = null

const getScopedKey = (key: string): string => {
  return `${userId ?? 'null'}:${key}`
}

const initUserId = (_userId: UserId | null): void => {
  userId = _userId == null ? null : String(_userId)
}

const setData = <T>(key: string, value: T): void => {
  store.set(getScopedKey(key), value)
}

const getData = <T = unknown>(key: string): T | undefined => {
  return store.get(getScopedKey(key)) as T | undefined
}

const getUserId = (): string | null => {
  return userId
}

export default {
  initUserId,
  setData,
  getData,
  getUserId
}
