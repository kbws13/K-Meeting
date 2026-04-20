<template>
  <div class="chat-panel">
    <div class="chat-panel-title">
      <div class="iconfont icon-chat">聊天</div>
    </div>

    <div class="chat-list" id="chat-list" ref="chatListRef">
      <MessageItem v-for="item in dataSource.list" :data="item"></MessageItem>
    </div>

    <ChatSend :sysSetting="sysSetting"></ChatSend>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, onMounted, onUnmounted, provide, ref } from 'vue'
import { useUserInfoStore } from '../../../stores/UserInfoStore'
import { useMeetingStore } from '../../../stores/MeetingStore'
import ChatSend from './ChatSend.vue'
import MessageItem from './MessageItem.vue'

const userInfoStore = useUserInfoStore()
const meetingStore = useMeetingStore()

const { proxy } = getCurrentInstance()
const loading = ref(false)
const dataSource = ref({ list: [] })

/**
 * 对聊天消息列表进行排序
 */
const sortMessage = () => {
  dataSource.value.list.sort((a, b) => a.messageId - b.messageId)
}

/**
 * 监听聊天消息的接收
 */
const listenMessage = () => {
  // 通过 Electron IPC 监听主进程发送的 'chatMessage' 事件
  window.electron.ipcRenderer.on('chatMessage', async (e, messageObj) => {
    console.log('收到消息', messageObj)

    switch (messageObj.messageType) {
      case 5: // 文本消息
      case 6: // 媒体消息
              // 增加未读计数
        meetingStore.addNoReadChatCount()

        // 标记消息是否由当前用户发送
        messageObj.isMe = userInfoStore.userInfo.userId == messageObj.sendUserId

        // 将新消息推入列表并排序
        dataSource.value.list.push(messageObj)
        sortMessage()

        // 等待 DOM 更新后执行滚动到底部的操作 (TODO)
        await nextTick()
        break

      case 7: // 消息状态更新（例如：确认消息已送达）
        // 在本地列表中查找对应的消息
        console.log(dataSource.value.list, messageObj)
        const messageItem = dataSource.value.list.find((item) => {
          return item.messageId == messageObj.messageId
        })

        if (!messageItem) {
          return
        }

        // 更新消息状态和内容
        messageItem.status = 1
        messageItem.messageContent = messageObj.messageContent
        break
    }
  })
}

/**
 * 监听文件上传进度事件
 */
const listenUploadProgress = () => {
  // 通过 Electron IPC 监听主进程发出的 'uploadProgress' 事件
  window.electron.ipcRenderer.on('uploadProgress', (e, { messageId, percent }) => {
    // 在本地数据列表中查找对应 messageId 的消息项
    const message = dataSource.value.list.find((item) => {
      return item.messageId == messageId
    })

    // 如果没有找到该消息，则直接返回
    if (!message) {
      return
    }

    // 更新该消息的上传进度，由于 Vue 的响应式特性，UI 会自动更新
    message.uploadProgress = percent
  })
}

// 组件挂载时注册消息监听
onMounted(() => {
  listenMessage()
  listenUploadProgress()
})

// 组件销毁前清理所有 'chatMessage' 监听器，防止内存泄漏
onUnmounted(() => {
  window.electron.ipcRenderer.removeAllListeners('chatMessage')
  window.electron.ipcRenderer.removeAllListeners('uploadProgress')
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

provide('showMedia', (messageId) => {
  // 1. 筛选数据：获取当前列表中的所有图片 (fileType == 0) 和视频 (fileType == 1)
  let mediaList = dataSource.value.list
    .filter((item) => {
      return item.status == 1 && (item.fileType == 0 || item.fileType == 1)
    })
    .map((item) => {
      // 2. 映射数据：提取关键字段，准备传递给媒体详情页
      return {
        messageId: item.messageId + '',
        fileType: item.fileType,
        sendTime: item.sendTime,
        fileName: item.fileName
      }
    })

  // 3. 调用 Electron IPC 打开新窗口
  window.electron.ipcRenderer.send('openWindow', {
    title: '媒体详情',
    windowId: 'media',
    path: '/showMedia',
    width: 960,
    height: 720,
    data: {
      currentMessageId: messageId,
      // 将处理好的列表序列化为字符串以便跨进程传递
      mediaList: JSON.stringify(mediaList)
    }
  })
})
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
