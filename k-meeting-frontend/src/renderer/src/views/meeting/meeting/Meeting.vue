<template>
  <div>
    <Header>
      <Titlebar
        :showMax="true"
        :closeType="0"
        :styleTop="6"
        :styleRight="10"
        :borderRadius="5"
        ref="titlebarRef"
        :forceClose="false"
      ></Titlebar>
    </Header>
    <template v-if="inited">
      <div class="meeting-panel">
        <div :class="['layout', LAYOUT_CLASS[layoutType]]">
          <MemberList :deviceInfo="deviceInfo" @exitMeeting="forceExit" @selectMember="selectMemberHandler"></MemberList>

          <div
            v-show="layoutType != 0"
            :class="['show-panel', transformShowPanelVideo && !screenId ? 'transform-video' : '']"
            :style="{ height: `calc(100vh - ${(layoutType == 1 ? 123 : 0) + 90}px)` }"
          >
            <video
              autoplay
              playsinline
              loop
              muted
              ref="centerScreenRef"
              v-show="openVideoRef"
            ></video>

            <div v-show="!openVideoRef" class="user-info">
              <Avatar :avatar="selectUserInfo.userId"></Avatar>
              <div :class="['user-name', 'iconfont', proxy.Utils.getSexIcon(selectUserInfo.sex)]">
                {{ selectUserInfo.nickName }}
              </div>
            </div>
          </div>
        </div>
        <SplitLine
          v-show="memberOpened || chatOpened"
          :initWidth="initRightWidth"
          @widthChange="widthChange"
        ></SplitLine>

        <div
          v-show="memberOpened || chatOpened"
          :style="{ width: rightWidth + 'px' }"
        >
          <MemberPanel v-show="memberOpened" ref="memberPanelRef"></MemberPanel>
        </div>
      </div>

      <Footer
        :deviceInfo="deviceInfo"
        @openChat="openChatHandler"
        @openMember="openMemberHandler"
      ></Footer>
    </template>
    <template v-else>
      <div class="check-env">正在检查系统环境......</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import Header from './Header.vue'
import { getCurrentInstance, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { mitter } from '@/eventbus/eventBus'
import Footer from './Footer.vue'
import MemberList from './MemberList.vue'
import SplitLine from './SplitLine.vue'
import MemberPanel from '../member/MemberPanel.vue'
import { useUserInfoStore } from '@/stores/UserInfoStore'
import { useMeetingStore } from '@/stores/MeetingStore'
const userInfoStore = useUserInfoStore()
const meetingStore = useMeetingStore()

const { proxy } = getCurrentInstance()

const inited = ref(false)
const deviceInfo = reactive({
  micEnable: false,
  cameraEnable: false,
  micOpen: false,
  cameraOpen: false
})

const initEnv = async () => {
  // 1. 获取所有媒体设备并筛选出默认麦克风
  const devices = await navigator.mediaDevices.enumerateDevices()
  const defaultMic = devices.find((device) => device.kind == 'audioinput')

  // 2. 通过 IPC 从 Electron 主进程获取系统设置
  const sysSetting = await window.electron.ipcRenderer.invoke('getSysSetting')

  // 3. 尝试获取摄像头媒体流以检测摄像头可用性
  const stream = await navigator.mediaDevices
    .getUserMedia({
      video: true,
      audio: false
    })
    .catch((error) => {
      console.error('获取摄像头失败', error)
    })

  // 4. 整合设备状态信息
  Object.assign(deviceInfo, {
    micEnable: defaultMic != null,
    cameraEnable: stream != null,
    micOpen: sysSetting.openMic,
    cameraOpen: sysSetting.openCamera
  })

  // 5. 标记初始化完成
  inited.value = true
}
initEnv()

const LAYOUT_CLASS = {
  0: 'layout-grid',
  1: 'layout-top',
  2: 'layout-right'
}

const layoutType = ref(0)
const layoutChangeHandler = (type: number) => {
  layoutType.value = type
}

const titlebarRef = ref()
const closeMeeting = () => {
  proxy.Confirm({
    message: '确定要退出会议吗？',
    okfun: () => {
      titlebarRef.value.custClose()
    }
  })
}

const forceExit = () => {
  titlebarRef.value.custClose()
}

const screenId = ref()
const shareScreenHandler = (_screenId) => {
  screenId.value = _screenId
}

const centerScreenRef = ref()
const transformShowPanelVideo = ref(false)
const openVideoRef = ref(true)
const selectUserInfo = ref({})

/**
 * 选中成员处理函数
 * @param {Object} memberData 包含流对象、ID、性别、昵称及视频开启状态
 */
const selectMemberHandler = async ({ srcObject, userId, sex, nickName, openVideo }) => {
  // 1. 如果当前是宫格布局 (layoutType == 0)，则不执行聚焦逻辑
  if (layoutType.value == 0) {
    return
  }

  // 2. 更新当前聚焦用户的信息
  selectUserInfo.value = {
    userId,
    nickName,
    sex
  }

  // 3. 同步视频开启状态
  openVideoRef.value = openVideo

  // 4. 等待 DOM 更新后，将视频流绑定到主播放器引用
  await nextTick()
  centerScreenRef.value.srcObject = srcObject

  // 5. 镜像处理：如果是看自己的摄像头，则开启水平翻转（镜像）
  if (userId == userInfoStore.userInfo.userId) {
    transformShowPanelVideo.value = true
  } else {
    transformShowPanelVideo.value = false
  }
}

onMounted(() => {
  mitter.on('layoutChange', layoutChangeHandler)
  mitter.on('shareScreen', shareScreenHandler)
  window.electron.ipcRenderer.on('preCloseWindow', () => {
    closeMeeting()
  })
})

onUnmounted(() => {
  mitter.off('layoutChange', layoutChangeHandler)
  mitter.off('shareScreen', shareScreenHandler)
  window.electron.ipcRenderer.removeAllListeners('preCloseWindow')
})

// 右侧容器
const initRightWidth = 400
const rightWidth = ref(initRightWidth)

/**
 * 处理侧边栏宽度拖拽变更
 * @param {number} width 拖拽后的新宽度
 */
const widthChange = (width) => {
  rightWidth.value = width
}

// 成员列表开关逻辑
const memberOpened = ref(false)
const openMemberHandler = () => {
  chatOpened.value = false // 互斥逻辑：打开成员列表时关闭聊天
  memberOpened.value = !memberOpened.value
}

// 聊天窗口开关逻辑
const chatOpened = ref(false)
const openChatHandler = () => {
  memberOpened.value = false // 互斥逻辑：打开聊天时关闭成员列表
  chatOpened.value = !chatOpened.value
}
</script>

<style scoped lang="scss">
.meeting-panel {
  display: flex;

  .layout {
    flex: 1;
    height: calc(100vh - 92px);

    .show-panel {
      display: flex;
      align-items: center;
      justify-content: center;

      video {
        height: 100%;
        width: 100%;
        object-fit: contain; // 保持视频比例，不拉伸
      }
    }

    .user-info {
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;

      .user-name {
        margin-top: 5px;
        font-size: 13px;
        color: #575757;
        display: flex;
        align-items: center;

        &::before {
          color: var(--blue);
          margin-right: 1px;
          font-size: 16px;
        }
      }

      .icon-woman {
        &::before {
          color: #fb7373; // 女性用户图标颜色
        }
      }
    }
  }
}

// 视频镜像处理
.transform-video {
  video {
    transform: scaleX(-1);
  }
}

// 布局模式：顶部缩略图
.layout-top {
  margin: 0px auto;
  text-align: center;

  .show-panel {
    border-top: 1px solid #ddd;
  }
}

// 布局模式：侧边缩略图
.layout-right {
  display: flex;
  flex-direction: row-reverse; // 侧边栏模式下内容反向排列

  .show-panel {
    border-right: 1px solid #ddd;
    flex: 1;
  }
}

// 环境检测/缺省状态样式
.check-env {
  height: calc(100vh - 42px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #797979;
}
</style>
