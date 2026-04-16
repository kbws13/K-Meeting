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
      <div class="video-panel"
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
    <div
      :class="[
        'member-item',
        currentSelectUserId == item.userId ? 'active' : '',
        LAYOUT_MAP[layoutType]
      ]"
      v-for="(item, index) in memberList"
    >
      <div class="video-panel" v-show="item.openVideo">
        <video :id="`member_${item.userId}`" autoplay playsinline loop></video>
        <div class="video-user-name">
          <div :class="['iconfont', proxy.Utils.getSexIcon(item.sex)]"></div>
          <div class="user-name">{{ item.nickName }}</div>
        </div>
      </div>

      <div class="user-info" v-show="!item.openVideo">
        <Avatar :avatar="item.userId" :udpate="true"></Avatar>
        <div :class="['user-name', 'iconfont', proxy.Utils.getSexIcon(item.sex)]">
          {{ item.nickName }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserInfoStore } from '@/stores/UserInfoStore'
import { useMeetingStore } from '@/stores/MeetingStore'
const route = useRoute()
const userInfoStore = useUserInfoStore()
const meetingStore = useMeetingStore()
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
  const { rows, cols } = calculateGrid(memberList.value.length + 1)

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
    cameraStream.getTracks().forEach((tarck) => {
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
  // 加入会议
  joinMeeting(
    (props.deviceInfo.cameraEnable && props.deviceInfo.cameraOpen) ||
    !proxy.Utils.isEmpty(screenId.value)
  )
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
    const stream = await navigator.mediaDevices
      .getUserMedia({
        audio: false,
        video: constraints
      })
      .catch((error) => {
        console.error(error)
      })
    screenStream = stream;
    resolve(stream)
    return;
  })
}

/**
 * 加入会议请求
 */
const joinMeeting = async (videoOpen) => {
  let result = await proxy.Request({
    url: proxy.Api.joinMeeting,
    params: {
      videoOpen
    },
    dataType: 'json',
    showLoading: false
  })
  if (!result) {
    return
  }
}

const peerConnectionMap = new Map()
const SIGNAL_TYPE_OFFER = 'offer'
const SIGNAL_TYPE_ANSWER = 'answer'
const SIGNAL_TYPE_CANDIDATE = 'candidate'

/**
 * 为指定成员创建 RTCPeerConnection 实例
 * @param {Object} member 会议成员对象
 */
const createPeerConnection = (member) => {
  let peerConnection = peerConnectionMap.get(member.userId)
  if (peerConnection) {
    return peerConnection
  }

  // 初始化 RTCPeerConnection 配置
  peerConnection = new RTCPeerConnection({
    sdpSemantics: 'unified-plan', // 明确使用现代标准
    codecs: { video: 'VP8' },      // 强制优先使用 VP8
    bundlePolicy: 'balanced',      // 优化媒体传输通道的绑定策略
    rtcpMuxPolicy: 'require',      // 强制 RTP/RTCP 多路复用
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
  })

  // 增加候选信息缓冲队列，避免远端描述设置前就收到候选信息导致丢失
  peerConnection.candidateQueue = []

  // 1. 处理收发器（Transceiver）
  // 如果本地摄像头不可用，预先添加一个“只收不发”的视频收发器，用于接收对方画面
  if (!props.deviceInfo.cameraEnable) {
    peerConnection.addTransceiver('video', { direction: 'recvonly' })
  }

  // 如果本地麦克风不可用，预先添加一个“只收不发”的音频收发器
  if (!props.deviceInfo.micEnable) {
    peerConnection.addTransceiver('audio', { direction: 'recvonly' })
  }

  // Debug：监听链接状态
  peerConnection.onconnectionstatechange = () => {
    console.log(`[WebRTC - ${member.userId}] Connection State 改变: ${peerConnection.connectionState}`);
  }

  peerConnection.oniceconnectionstatechange = () => {
    console.log(`[WebRTC - ${member.userId}] ICE Connection State 改变: ${peerConnection.iceConnectionState}`);
  }

  // 2. 监听 ICE 候选者（Candidate）
  peerConnection.onicecandidate = (e) => {
    if (e.candidate) {
      console.log(`[WebRTC - ${member.userId}] 发现本机 Candidate，准备发送`, e.candidate.candidate);
      sendPeerMessage({
        sendUserId: userInfoStore.userInfo.userId,
        signalType: SIGNAL_TYPE_CANDIDATE,
        signalData: e.candidate,
        receiveUserId: member.userId
      })
    } else {
      console.log(`[WebRTC - ${member.userId}] ICE 收集完成`);
    }
  }

  // 3. 监听远程媒体轨道（Track）
  peerConnection.ontrack = (event) => {
    console.log(`[WebRTC - ${member.userId}] 收到远程流 Track`, event.track.kind);
    nextTick(() => {
      const remoteVideo = document.querySelector('#member_' + member.userId)
      if (remoteVideo) {
        console.log(`[WebRTC - ${member.userId}] 将收到的流挂载到 video DOM 上`);
        remoteVideo.srcObject = event.streams[0]
      } else {
        console.error(`[WebRTC - ${member.userId}] 未能在页面中找到 ID 为 #member_${member.userId} 的 video 节点！`);
      }
    });
  }

  // 4. 将本地所有音视频轨道添加到连接中发送给对方
  if (localStream) {
    localStream.getTracks().forEach((track) => {
      console.log(`[WebRTC - ${member.userId}] 将本地 Track 添加到对等连接中:`, track.kind);
      peerConnection.addTrack(track, localStream)
    })
  } else {
    console.warn(`[WebRTC - ${member.userId}] 本地流为空，无法分享音视频！`);
  }

  // 5. 缓存连接实例并返回
  peerConnectionMap.set(member.userId, peerConnection)
  return peerConnection
}

/**
 * 处理新用户加入会议的逻辑
 */
const onUserJoin = async (messageContent) => {
  console.log(messageContent)
  const newMember = messageContent.newMember
  // 1. 获取所有成员并按加入时间排序
  const allMemberList = messageContent.meetingMemberList.sort((a, b) => a.joinTime - b.joinTime)

  // 2. 过滤掉自己，并只保留状态为 1 (在线) 的成员
  memberList.value = allMemberList.filter((item) => {
    return item.userId !== userInfoStore.userInfo.userId && item.status == 1
  })

  // 3. 更新 Pinia/Vuex 仓库中的成员列表
  meetingStore.setMemberList(memberList.value)
  meetingStore.setAllMemberList(allMemberList)

  await nextTick()

  // 4. 如果加入的是别人，弹出成功提示并准备建立 WebRTC 连接
  if (newMember.userId !== userInfoStore.userInfo.userId) {
    proxy.Message.success(`用户${newMember.nickName}加入了会议`)
    createPeerConnection(newMember)
    return
  }
  memberList.value.forEach((member) => {
    const peerConnection = createPeerConnection(member)
    sendOffer(peerConnection, userInfoStore.userInfo.userId, member.userId)
  })
}

/**
 * 发送 Offer (通常由呼叫方触发)
 */
const sendOffer = async (peerConnection, sendUserId, receiveUserId) => {
  // 创建 Offer，iceRestart: true 确保在网络切换时可以重新协商
  let offer = await peerConnection.createOffer({ iceRestart: true })
  await peerConnection.setLocalDescription(offer)

  sendPeerMessage({
    sendUserId,
    receiveUserId,
    signalType: SIGNAL_TYPE_OFFER,
    signalData: offer
  })
}

/**
 * 通过 Electron IPC 发送 P2P 信令消息
 */
const sendPeerMessage = ({sendUserId, receiveUserId, signalType, signalData}) => {
  window.electron.ipcRenderer.send('sendPeerConnection', {
    sendUserId,
    receiveUserId,
    signalType,
    // 将复杂的信令对象转为字符串传输，避免 IPC 序列化问题
    signalData: JSON.stringify(signalData)
  })
}

/**
 * 处理接收到的远程 P2P 信令
 */
const onPeerConnection = async ({ sendUserId, receiveUserId, messageContent }) => {
  // 1. 安全校验：确保消息是发给自己的
  if (receiveUserId != userInfoStore.userInfo.userId) {
    return
  }

  // 2. 解析信令数据
  const signalData = messageContent.signalData ? JSON.parse(messageContent.signalData) : {}

  // 3. 查找对应的成员并获取/创建连接实例
  const member = memberList.value.find((item) => {
    return item.userId == sendUserId
  })

  if (!member) {
    console.error(`[WebRTC] 找不到发送消息的用户: ${sendUserId}，忽略该信令。`);
    return;
  }

  const peerConnection = createPeerConnection(member)

  console.log(`[信令交互 - 收到 ${sendUserId}] 信令类型: ${messageContent.signalType}`);

  try {
    switch (messageContent.signalType) {
      case SIGNAL_TYPE_OFFER: {
        // 收到 Offer：设置远端描述 -> 创建应答 -> 设置本地描述 -> 发回 Answer
        await peerConnection.setRemoteDescription(signalData)
        const answer = await peerConnection.createAnswer()
        await peerConnection.setLocalDescription(answer)

        console.log(`[信令交互 - 收到 Offer] 发送给 ${sendUserId} 回复 Answer`);
        sendPeerMessage({
          sendUserId: receiveUserId,
          receiveUserId: sendUserId,
          signalType: SIGNAL_TYPE_ANSWER,
          signalData: answer
        })

        // 执行在 offer 之前被压入队列的 candidate
        if (peerConnection.candidateQueue && peerConnection.candidateQueue.length > 0) {
          console.log(`[信令交互] 合并之前收到的 ${peerConnection.candidateQueue.length} 个候选者`);
          for (const c of peerConnection.candidateQueue) {
            await peerConnection.addIceCandidate(c).catch(e => console.error(e));
          }
          peerConnection.candidateQueue = [];
        }
        break
      }

      case SIGNAL_TYPE_ANSWER: {
        // 收到 Answer：设置远端描述，完成 SDP 握手
        await peerConnection.setRemoteDescription(signalData)

        // 同样执行候选者队列
        if (peerConnection.candidateQueue && peerConnection.candidateQueue.length > 0) {
          console.log(`[信令交互] 合并之前收到的 ${peerConnection.candidateQueue.length} 个候选者`);
          for (const c of peerConnection.candidateQueue) {
            await peerConnection.addIceCandidate(c).catch(e => console.error(e));
          }
          peerConnection.candidateQueue = [];
        }
        break
      }

      case SIGNAL_TYPE_CANDIDATE: {
        // 收到 Candidate：必须在 setRemoteDescription 之后才能添加
        if (!peerConnection.remoteDescription) {
          console.log(`[信令交互 - ${sendUserId}] 未收到 Offer，先将 Candidate 放入队列`);
          peerConnection.candidateQueue.push(signalData);
          return
        }
        await peerConnection.addIceCandidate(signalData).catch(e => console.error(e));
        break
      }
    }
  } catch (error) {
    console.error('[WebRTC ERROR]', error)
  }
}

/**
 * 初始化会议消息监听（Electron IPC）
 */
const initMeetingListener = () => {
  window.electron.ipcRenderer.on(
    'meetingMessage',
    (e, { sendUserId, receiveUserId, messageContent, messageType }) => {
      switch (messageType) {
        case 1: // 用户加入
          onUserJoin(messageContent)
          break
        case 2: // 建立 peerConnection (WebRTC 信号交换)
          onPeerConnection({ sendUserId, receiveUserId, messageContent })
          break
      }
    }
  )
}

onMounted(() => {
  initMeetingListener()
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
