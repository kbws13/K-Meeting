import { defineStore } from 'pinia'
import type { AppUserInfo } from '@model/user'

interface UserState {
  userInfo: AppUserInfo
}

export const useUserInfoStore = defineStore('userInfo', {
  state: (): UserState => {
    return {
      userInfo: {}
    }
  },

  actions: {
    setInfo(userInfo: AppUserInfo): void {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    getInfo(): AppUserInfo {
      return this.userInfo
    }
  }
})
