<template>
  <div class="chat-send-panel">
    <div class="chat-send-bar">
      <div class="user-select">
        发送至
        <el-select v-model="receiveUserId" placeholder="所有人" class="select" filterable>
          <el-option label="所有人" value="0" />
          <el-option
            v-for="item in meetingStore.memberList"
            :label="item.nickName"
            :value="item.userId"
          />
        </el-select>
      </div>

      <el-popover
        :visible="showEmojiPopover"
        trigger="click"
        placement="top"
        :teleported="false"
        @show="openPopover"
        @hide="closePopover"
        :popper-style="{ padding: '0px 10px 10px 10px', width: '490px' }"
      >
        <template #default>
          <el-tabs v-model="activeEmoji" @click.stop>
            <el-tab-pane v-for="emoji in emojiList" :label="emoji.name" :name="emoji.name">
              <div class="emoji-list">
                <div class="emoji-item" v-for="item in emoji.emojiList" @click="sendEmoji(item)">
                  {{ item }}
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>

        <template #reference>
          <div class="iconfont icon-emoji" @click="showEmojiPopoverHandler" title="发送表情"></div>
        </template>
      </el-popover>

      <div class="iconfont icon-folder" @click="uploadFile" title="发送文件"></div>
    </div>

    <div class="input-area">
      <el-input
        type="textarea"
        clearable
        placeholder="请输入消息..."
        v-model="message"
        resize="none"
        :rows="6"
      ></el-input>
    </div>

    <div class="send-btn-panel">
      <div class="tips">Ctrl+Enter可以直接发送</div>
      <el-button type="primary" :disabled="!message" @click="sendMessage">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import emojiList from '@/utils/Emoji.js'
import { useMeetingStore } from '@/stores/MeetingStore'
import { getFileType } from '@/utils/Constants.js'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const meetingStore = useMeetingStore()

// Props 定义
const props = defineProps({
  sysSetting: {
    type: Object,
    default: () => ({})
  }
})

// 状态管理
const activeEmoji = ref('笑脸')
const message = ref('')
const receiveUserId = ref('0')
const showEmojiPopover = ref(false)

// 表情处理
const sendEmoji = (emoji) => {
  message.value = message.value + emoji
}

const showEmojiPopoverHandler = () => {
  showEmojiPopover.value = true
}

const hidePopover = () => {
  showEmojiPopover.value = false
}

const openPopover = () => {
  document.addEventListener('click', hidePopover, false)
}

const closePopover = () => {
  document.removeEventListener('click', hidePopover, false)
}

// 消息发送核心逻辑
const sendMessage = () => {
  if (!message.value) {
    return
  }
  // 类型 5 通常定义为文本消息
  sendMessagesDo({
    messageContent: message.value,
    messageType: 5
  })
}

/**
 * 底层消息发送请求封装
 */
const sendMessagesDo = async ({
  messageContent,
  messageType,
  fileSize,
  fileName,
  fileType,
  callback
}) => {
  let result = await proxy.Request({
    url: proxy.Api.sendChatMessage,
    showLoading: false,
    params: {
      receiveUserId: receiveUserId.value,
      message: message.value,
      messageType,
      fileSize,
      fileName,
      fileType
    }
  })

  if (!result) {
    return
  }

  // 成功后清空输入框
  if (messageContent) {
    message.value = ''
  }

  // 执行回调（如：更新本地聊天列表）
  if (callback) {
    callback(result.data)
  }
}
</script>

<style lang="scss" scoped>
// 表情选择列表样式
.emoji-list {
  .emoji-item {
    float: left;
    font-size: 23px;
    padding: 2px;
    text-align: center;
    border-radius: 3px;
    margin-left: 10px;
    margin-top: 5px;
    cursor: pointer;
    &:hover {
      background: #ddd; // 悬停高亮效果
    }
  }
}

// 聊天发送区域总面板
.chat-send-panel {
  padding: 8px;

  // 发送栏顶部（包含用户选择、功能图标）
  .chat-send-bar {
    display: flex;
    align-items: center;

    // 接收用户选择器
    .user-select {
      display: flex;
      align-items: center;
      font-size: 14px;
      .select {
        width: 150px;
        margin-left: 5px;
      }
    }

    // 顶部功能图标样式
    .iconfont {
      margin: 0px 5px;
      font-size: 20px;
      color: #4e5461;
      cursor: pointer;
    }
  }

  // 文本输入区域
  .input-area {
    margin-top: 5px;
    :deep {
      // 深度重置 Element Plus 文本域默认样式
      .el-textarea__inner {
        box-shadow: none;
        padding: 0px;
      }
    }
  }

  // 底部发送按钮及提示信息面板
  .send-btn-panel {
    display: flex;
    justify-content: end; // 右对齐
    align-items: center;

    // 输入提示文字（如“按 Enter 发送”）
    .tips {
      margin-right: 5px;
      font-size: 13px;
      color: #8c8c8c;
    }
  }
}
</style>
