import { defineStore } from 'pinia'

// 定义用户信息接口，根据实际业务需求可自行扩展字段
interface UserInfo {
  [key: string]: any

  // 例如:
  // id?: number;
  // name?: string;
  // avatar?: string;
}

// 定义 Store 的 State 类型
interface UserState {
  userInfo: UserInfo
}

export const useUserInfoStore = defineStore('userInfo', {
  state: (): UserState => {
    return {
      userInfo: {}
    }
  },

  actions: {
    /**
     * 设置用户信息并持久化到 localStorage
     * @param userInfo 用户信息对象
     */
    setInfo(userInfo: UserInfo): void {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    /**
     * 获取当前 state 中的用户信息
     */
    getInfo(): UserInfo {
      return this.userInfo
    }
  }
})
