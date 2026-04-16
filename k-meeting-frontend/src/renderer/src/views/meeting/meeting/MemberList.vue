<template>
  <div :class="['member-list', LIST_MAP[layoutType]]" :style="gridStyle">
    <div
      :class="[
        'member-item',
        currentSelectUserId == userInfoStore.userInfo.userId ? 'active' : '',
        proxy.Utils.isEmpty(screenId) ? 'member-my' : '',
        LAYOUT_MAP[layoutType]
      ]"
    >
      <div
        class="video-panel"
        v-show="
          (props.deviceInfo.cameraEnable && props.deviceInfo.cameraOpen) ||
          !proxy.Utils.isEmpty(screenId)
        "
      >
        <video
          :id="`member_${userInfoStore.userInfo.userId}`"
          ref="localVideoRef"
          autoplay
          playsinline
          loop
          muted
        ></video>

        <div class="video-user-name">
          <div :class="['iconfont', proxy.Utils.getSexIcon(userInfoStore.userInfo.sex)]"></div>
          <div class="user-name">{{ userInfoStore.userInfo.nickName }}</div>
        </div>
      </div>

      <div
        class="user-info"
        v-show="
          !(
            (props.deviceInfo.cameraEnable && props.deviceInfo.cameraOpen) ||
            !proxy.Utils.isEmpty(screenId)
          )
        "
      >
        <Avatar :avatar="userInfoStore.userInfo.userId" :update="true"></Avatar>
        <div :class="['user-name', 'iconfont', proxy.Utils.getSexIcon(userInfoStore.userInfo.sex)]">
          {{ userInfoStore.userInfo.nickName }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserInfoStore } from '../../../stores/UserInfoStore'
const route = useRoute()
const userInfoStore = useUserInfoStore()
const { proxy } = getCurrentInstance()

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: {}
  }
})

const currentSelectUserId = ref()
const screenId = ref(route.query.screenId)

const layoutType = ref(0)
const LIST_MAP = {
  0: 'member-list',
  1: 'member-list-top',
  2: 'member-list-right',
}

const LAYOUT_MAP = {
  0: 'member-item',
  1: 'member-item-top',
  2: 'member-item-right',
}

const memberList = ref([])

const gridStyle = computed(() => {
  // 1. 只有当布局类型为 0 (宫格布局) 时才计算样式，否则返回空字符串
  if (layoutType.value !== 0) {
    return ''
  }

  // 2. 根据当前参会者列表的长度计算行列数
  const { rows, cols } = calculateGrid(memberList.value.length)

  // 3. 返回 CSS Grid 布局样式对象
  return {
    gridTemplateRows: `repeat(${rows}, 1fr)`,
    gridTemplateColumns: `repeat(${cols}, 1fr)`
  }
})

/**
 * 计算宫格行列数的工具函数
 * @param {number} participantCount 参会者人数
 */
const calculateGrid = (participantCount) => {
  if (participantCount <= 0) {
    return { cols: 0, rows: 0 }
  }

  if (participantCount === 1) {
    return { cols: 1, rows: 1 }
  }

  // 核心算法：基于平方根向上取整确定列数，再计算行数
  let cols = Math.ceil(Math.sqrt(participantCount))
  let rows = Math.ceil(participantCount / cols)

  return {
    cols,
    rows
  }
}

// 创建空的视频轨道 (用于没有摄像头时的黑色占位图)
const createEmptyVideoTrack = () => {
  const canvas = document.createElement('canvas')
  canvas.width = 1
  canvas.height = 1
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = 'black'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  const stream = canvas.captureStream(1)
  return stream.getVideoTracks()[0]
}

// 创建一个静音的音轨 (占位轨)
const createEmptyAudioTrack = () => {
  const audioContext = new AudioContext()
  const oscillator = audioContext.createOscillator()
  const dst = oscillator.connect(audioContext.createMediaStreamDestination())
  oscillator.start()
  // 注意：原图中此处为 getVideoTracks()[0]，在逻辑上应为 getAudioTracks()[0]
  const track = dst.stream.getAudioTracks()[0]
  track.enabled = false
  return track
}

// --- 响应式引用与变量定义 ---
const localVideoRef = ref()
let cameraStream = null
let screenStream = null
let localStream = null



/**
 * 核心：初始化本地合成流 (Main logic)
 */
const initLocalStream = async () => {
  await nextTick()
  localStream = new MediaStream()

  // 处理麦克风不可用的情况
  if (!props.deviceInfo.micEnable) {
    const micTrack = createEmptyAudioTrack()
    micTrack.enabled = false
    localStream.addTrack(micTrack)
  }

  // 获取硬件媒体流
  if (props.deviceInfo.cameraEnable || proxy.deviceInfo.micEnable) {
    await initLocalCameraStream(proxy.deviceInfo.cameraEnable, props.deviceInfo.micEnable)
    cameraStream.getTracks().forEach((tarck) => { // ⚠️ 原图变量名: tarck
      tarck.enabled = false
      localStream.addTrack(tarck)
    })
  }

  // 没有摄像头，也不是共享屏幕，添加空的视频轨道占位
  if (!props.deviceInfo.cameraEnable && !screenId.value) {
    const videoTrack = createEmptyVideoTrack();
    videoTrack.enabled = false;
    localStream.addTrack(videoTrack);
  }

  // 屏幕共享逻辑处理
  if (screenId.value) {
    const videoTracks = localStream.getVideoTracks()
    if (videoTracks.length > 0) {
      localStream.removeTrack(videoTracks[0])
      videoTracks[0].stop()
    }
    await initLocalScreenStream()
    localStream.addTrack(screenStream.getVideoTracks()[0])
  } else if (!screenId.value && (props.deviceInfo.cameraEnable || props.deviceInfo.micEnable)) {
    // ⚠️ 原图此处逻辑有误：getAudioTracks 会导致无法获取视频轨
    localStream.getTracks().forEach((track) => {
      if (track.kind == 'audio') {
        track.enabled = props.deviceInfo.micOpen
      }
      if (track.kind == 'video') {
        track.enabled = props.deviceInfo.cameraOpen
      }
    })
  } else if (!screenId.value && !proxy.deviceInfo.cameraEnable) {
    const videoTrack = createEmptyVideoTrack()
    videoTrack.enabled = false
    localStream.addTrack(videoTrack)
  }

  // 绑定预览
  localVideoRef.value.srcObject = localStream
  // // 加入会议
}

// --- 媒体流初始化函数 ---

/**
 * 初始化摄像头/麦克风流
 */
const initLocalCameraStream = async (video, audio) => {
  return new Promise(async (resolve, reject) => {
    if (!props.deviceInfo.cameraEnable && !props.deviceInfo.micEnable) {
      cameraStream = null
      resolve(null)
      return
    }
    const stream = await navigator.mediaDevices
      .getUserMedia({
        video,
        audio
      })
      .catch((error) => {
        console.log(error)
      })
    cameraStream = stream
    resolve(stream)
  })
}

/**
 * 初始化屏幕共享流 (Electron 特有约束)
 */
const initLocalScreenStream = async () => {
  return new Promise(async (resolve, reject) => {
    const constraints = {
      mandatory: {
        chromeMediaSource: 'desktop',
        chromeMediaSourceId: screenId.value,
        minWidth: 1024,
        maxWidth: 1600,
        minHeight: 768,
        maxHeight: 900,
        minFrameRate: 10,
        maxFrameRate: 25
      }
    }
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: constraints
    }).catch(error => {
      console.error(error)
    })
    screenStream = stream;
    resolve(stream)
    return;
  })
}

onMounted(() => {
  initLocalStream()
})
</script>

<style scoped lang="scss">
// 成员列表基础容器
.member-list {
  height: 100%;
  display: grid;
  gap: 8px;
  max-height: 100%;
  padding: 10px;
  background: #fff;
  transition: grid-template 0.3s ease;
  overflow-y: auto;
}

// 顶部布局模式下的列表样式
.member-list-top {
  display: inline-flex;
  grid-gap: 0px;
  padding: 10px 0px 10px 10px;
  overflow-x: auto;
  max-width: 100%;
  height: 120px;

  .member-item {
    cursor: pointer;
  }

  .active {
    border: 2px solid var(--blue);
  }
}

// 右侧布局模式下的列表样式
.member-list-right {
  display: flex;
  flex-direction: column;
  grid-gap: 0px;
  padding: 10px 10px 0px 10px;
  width: 130px;
  align-items: center;
  margin: auto;

  .member-item {
    cursor: pointer;
  }

  .active {
    border: 2px solid var(--blue);
  }
}

// 单个成员卡片组件样式
.member-item {
  background: #f7f7f7;
  position: relative;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 2px solid #fff;

  // 视频容器
  .video-panel {
    height: 100%;
    position: relative;
    video {
      height: 100%;
      width: 100%;
      object-fit: cover;
    }
  }

  // 视频上方的用户信息浮层
  .video-user-name {
    position: absolute;
    top: 0px;
    right: 0px;
    display: flex;
    align-items: center;
    border-radius: 0px 0px 0px 5px;
    overflow: hidden;

    // 性别/状态图标基础样式
    .iconfont {
      width: 20px;
      height: 20px;
      background: var(--blue);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    // 女性图标特殊背景色
    .icon-woman {
      background: #fb7373;
    }

    // 用户名文本样式
    .user-name {
      background: rgba(0, 0, 0, 0.8);
      color: #fff;
      font-size: 12px;
      height: 20px;
      padding: 0px 3px;
      max-width: 80px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
      padding-top: 2px;
    }
  }
}

// 用户信息通用样式（非视频内部）
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
      color: #fb7373;
    }
  }
}

// 本地用户视频镜像处理
.member-my {
  video {
    transform: scaleX(-1);
  }
}

// 顶部列表中的卡片尺寸微调
.member-item-top {
  width: 100px;
  height: 100px;
  margin-right: 10px;

  .video-panel {
    width: 100px;
    height: 100px;
  }
}

// 右侧列表中的卡片尺寸微调
.member-item-right {
  width: 100px;
  height: 100px;
  margin-bottom: 10px;

  .video-panel {
    width: 100px;
    height: 100px;
  }
}
</style>
