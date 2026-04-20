<template>
  <div :class="['message-item', data.isMe ? 'my-message' : '']">
    <Avatar :width="30" :avatar="data.sendUserId"></Avatar>

    <div class="message-content">
      <div class="nick-name">{{ data.sendUserNickName }}</div>

      <div class="message" v-if="data.messageType == 5">
        {{ data.messageContent }}
        <div class="direct"></div>
      </div>

      <MessageItemMedia
        v-if="data.messageType == 6 && (data.fileType === 0 || data.fileType == 1)"
        :data="data"
      ></MessageItemMedia>
      <MessageItemFile v-if="data.messageType == 6 && data.fileType == 2" :data="data"></MessageItemFile>
    </div>
  </div>
</template>

<script setup lang="ts">
import MessageItemMedia from './MessageItemMedia.vue'
import MessageItemFile from './MessageItemFile.vue'

const props = defineProps({
  data: {
    type: Object,
    default: {}
  }
})
</script>

<style lang="scss" scoped>
.message-item {
  margin-top: 10px;
  display: flex;
  padding: 0px 15px;

  .message-content {
    margin-left: 10px;

    .nick-name {
      font-size: 12px;
      color: #5d5d5d;
      margin-right: 5px;
    }

    // 基础消息气泡样式 (他人消息)
    .message {
      margin-top: 3px;
      background: #f1f2f4;
      border-radius: 5px;
      padding: 5px 8px;
      font-size: 14px;
      width: fit-content;
      margin-right: 40px;
      position: relative;

      // 气泡左侧小三角
      .direct {
        position: absolute;
        width: 8px;
        height: 8px;
        background: #f1f2f4;
        transform: rotate(45deg);
        top: 10px;
        left: -4px;
      }
    }
  }
}

// 我的消息样式
.my-message {
  flex-direction: row-reverse; // 气泡置右

  .message-content {
    margin-left: 0px;
    margin-right: 10px;
    display: flex;
    flex-direction: column;
    align-items: flex-end;

    .nick-name {
      margin-right: 0px;
      margin-left: 5px;
    }

    // 我的消息气泡 (蓝色背景)
    .message {
      background: #409eff;
      color: #fff;
      margin-left: 40px;
      margin-right: 0px;

      // 气泡右侧小三角
      .direct {
        position: absolute;
        width: 8px;
        height: 8px;
        background: #409eff;
        transform: rotate(45deg);
        top: 10px;
        right: -4px;
        left: auto;
      }
    }
  }
}
</style>
