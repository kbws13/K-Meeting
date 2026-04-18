<template>
  <div class="chat-panel">
    <div class="chat-panel-title">
      <div class="iconfont icon-chat">聊天</div>
    </div>

    <div class="chat-list" id="chat-list" ref="chatListRef"></div>

    <ChatSend :sysSetting="sysSetting"></ChatSend>
  </div>
</template>

<script setup lang="ts">

import { getCurrentInstance, onMounted, onUnmounted, ref } from 'vue'
import { useUserInfoStore } from '../../../stores/UserInfoStore'
import { useMeetingStore } from '../../../stores/MeetingStore'
import ChatSend from './ChatSend.vue'

const userInfoStore = useUserInfoStore()
const meetingStore = useMeetingStore()

const { proxy } = getCurrentInstance()
const loading = ref(false)
const dataSource = ref({})

/**
 * 注册聊天消息监听器
 */
const listenMessage = () => {
  // 通过 Electron IPC 监听主进程发送的 'chatMessage' 事件
  window.electron.ipcRenderer.on('chatMessage', async (e, messageObj) => {
    console.log('收到消息', messageObj)
  })
}

// 组件挂载时注册消息监听
onMounted(() => {
  listenMessage()
})

// 组件销毁前清理所有 'chatMessage' 监听器，防止内存泄漏
onUnmounted(() => {
  window.electron.ipcRenderer.removeAllListeners('chatMessage')
})

const sysSetting = ref()

/**
 * 加载系统设置配置
 */
const loadSysSetting = async () => {
  let result = await proxy.Request({
    url: proxy.Api.getSysSetting,
    showLoading: false // 静默加载，不显示 Loading 动画
  })
  if (!result) {
    return
  }
  sysSetting.value = result.data
}

// 初始化时执行加载配置
loadSysSetting()
</script>

<style lang="scss" scoped>
.chat-panel {
  // 设置面板高度，留出顶部和底部空间
  height: calc(100vh - 300px);
  background: #fff;

  .chat-panel-title {
    border-bottom: 1px solid #ddd;
    padding: 10px;
    color: #4e5461;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    // 聊天图标样式
    .icon-chat {
      display: flex;
      align-items: center;
      font-size: 14px;

      &::before {
        margin-right: 3px;
        font-size: 20px;
      }
    }

    // 可点击的功能图标（如切换面板等）
    .icon-transfer {
      cursor: pointer;
    }
  }

  // 聊天消息列表区域
  .chat-list {
    overflow: auto; // 内容溢出时显示滚动条
    height: calc(100vh - 345px); // 减去标题及底部输入框的高度
    border-bottom: 1px solid #ddd;
    padding-bottom: 10px;
  }
}
</style>
