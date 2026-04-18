import { getCurrentInstance } from 'vue'

type AppProxy = NonNullable<NonNullable<ReturnType<typeof getCurrentInstance>>['proxy']>

export const useAppProxy = (): AppProxy => {
  const instance = getCurrentInstance()
  if (!instance?.proxy) {
    throw new Error('useAppProxy must be called inside setup().')
  }

  return instance.proxy
}
