import { defineStore } from 'pinia'

// 定义 State 的类型接口
interface ContactState {
  lastUpdateTime: number | null
}

export const useContactStore = defineStore('userContact', {
  // 显式标注 state 的返回类型为 ContactState
  state: (): ContactState => {
    return {
      lastUpdateTime: null
    }
  },

  actions: {
    /**
     * 更新最后更新时间为当前时间戳
     */
    updateLastUpdateTime(): void {
      this.lastUpdateTime = new Date().getTime()
    }
  }
})
