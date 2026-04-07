import { defineStore } from "pinia";

/**
 * 定义会议状态接口
 */
interface MeetingState {
  lastUpdate: number | null; // 上次更新的时间戳
  inMeeting: boolean;        // 是否正在会议中
}

export const useMeetingStore = defineStore('meetingInfo', {
  state: (): MeetingState => {
    return {
      lastUpdate: null,
      inMeeting: false,
    };
  },

  actions: {
    /**
     * 更新会议状态
     * @param inMeeting 是否进入会议
     */
    updateMeeting(inMeeting: boolean): void {
      // 记录当前操作的时间戳
      this.lastUpdate = new Date().getTime();
      // 更新会议状态（修正原图中的拼写 imMeeting 为 inMeeting）
      this.inMeeting = inMeeting;
    }
  }
});
