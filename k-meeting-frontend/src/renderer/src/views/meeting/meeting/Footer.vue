<template>
  <div class="footer">
    <div class="btn-list">
      <div class="btn-item" @click="micClickHandler">
        <MicIcon
          :defaultOpen="deviceInfo.micEnable && deviceInfo.micOpen"
          :size="20"
          :showLabel="false"
          v-model="micInfo"
          ref="micInfoRef"
        >
        </MicIcon>
        <div class="name">
          {{ deviceInfo.micEnable && deviceInfo.micOpen ? '静音' : '取消静音' }}
        </div>
      </div>

      <div class="btn-item" @click="cameraClickHandler">
        <div
          :class="[
            'iconfont',
            deviceInfo.cameraEnable && deviceInfo.cameraOpen ? 'icon-video2' : 'icon-video2-close'
          ]"
        ></div>
        <div class="name">
          {{ deviceInfo.cameraEnable && deviceInfo.cameraOpen ? '关闭视频' : '开启视频' }}
        </div>
      </div>

      <div class="btn-item" @click="shareScreenClickHandler">
        <div
          :class="['iconfont', shareScreen ? 'icon-share-screen2-close' : 'icon-share-screen2']"
        ></div>
        <div class="name">{{ shareScreen ? '取消共享' : '共享屏幕' }}</div>
      </div>

      <div
        :class="['btn-item', item.active ? 'active' : '']"
        v-for="item in buttons"
        @click="clickHandler(item)"
      >
        <el-badge
          :value="meetingStore.noReadChatCount"
          :max="99"
          :hidden="meetingStore.noReadChatCount == 0"
          :offset="[0, 3]"
          v-if="item.btnType === 'chat'"
        >
          <div :class="['iconfont', 'icon-' + item.icon]"></div>
          <div class="name">
            <span>{{ item.name }}</span>
          </div>
        </el-badge>

        <template v-else>
          <div :class="['iconfont', 'icon-' + item.icon]"></div>
          <div class="name">
            <span>{{ item.name }}</span>
            <span v-if="item.btnType == 'members'">({{ meetingStore.allMemberList.length }})</span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import MicIcon from '../../../components/MicIcon.vue'
import { useMeetingStore } from '@/stores/MeetingStore'
import { useRoute } from 'vue-router'
import { mitter } from '../../../eventbus/eventBus'
const meetingStore = useMeetingStore()
const route = useRoute()

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: {}
  }
})

const deviceInfo = ref({})
const shareScreen = ref(route.query.addType == 1)
// 麦克风信息
const micInfoRef = ref()
const micInfo = ref({})

const micClickHandler = () => {
  // 1. 硬件校验：如果麦克风不可用（无权限或未插入），直接返回
  if (!props.deviceInfo.micEnable) {
    return
  }

  // 2. 调用子组件/引用的内部方法来切换麦克风状态
  micInfoRef.value.toggleMic()

  // 3. 取反更新当前 UI 的状态标识
  props.deviceInfo.micOpen = !props.deviceInfo.micOpen
  // 4. 通过全局事件总线（mitt）发送状态变更通知，供主页面或后台逻辑处理音频流
  mitter.emit('micSwitch', props.deviceInfo.micOpen)
}

const cameraClickHandler = () => {
  if (!props.deviceInfo.cameraEnable) {
    return
  }
  props.deviceInfo.cameraOpen = !props.deviceInfo.cameraOpen

  // 4. 通过全局事件总线（mitt）发送状态变更通知，供主页面或后台逻辑处理音频流
  mitter.emit('cameraSwitch', props.deviceInfo.cameraOpen)
}

const buttons = ref([
  {
    btnType: 'invite',
    name: '邀请',
    icon: 'invite'
  },
  {
    btnType: 'members',
    name: '成员',
    icon: 'members',
    showActive: true,
    active: false
  },
  {
    btnType: 'chat',
    name: '聊天',
    icon: 'chat',
    showActive: true,
    active: false
  }
])
</script>

<style scoped lang="scss">
.footer {
  -webkit-app-region: drag; // 底部栏设为可拖动区域
  border-top: 1px solid #ddd;
  height: 50px;
  display: flex;
  justify-content: center;

  .btn-list {
    display: flex;
    align-items: center;

    .btn-item {
      -webkit-app-region: no-drag; // 按钮区域禁止拖拽，确保点击事件穿透
      margin: 0px 5px;
      padding: 5px 0px;
      width: 70px;
      border-radius: 3px;
      cursor: pointer;
      color: var(--text);
      display: flex;
      flex-direction: column;
      align-items: center;

      &:hover {
        background: #efefef;
      }

      .iconfont {
        font-size: 20px;
      }

      .name {
        font-size: 12px;
      }
    }

    .close-screen-share {
      &:hover {
        background: #fff;
      }
    }

    .active {
      background: #efefef;
    }
  }
}
</style>
