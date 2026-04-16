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
          <MemberList :deviceInfo="deviceInfo" @exitMeeting="forceExit"></MemberList>
          <div v-show="layoutType != 0"></div>
        </div>
      </div>
      <Footer :deviceInfo="deviceInfo"></Footer>
    </template>
    <template v-else>
      <div class="check-env">正在检查系统环境......</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import Header from './Header.vue'
import { getCurrentInstance, onMounted, onUnmounted, reactive, ref } from 'vue'
import { mitter } from '../../../eventbus/eventBus'
import Footer from './Footer.vue'
import MemberList from './MemberList.vue'

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
  2: 'layout-right',
}

const layoutType = ref(0)
const layoutChangeHandler = (type: number) => {
  layoutType.value = type
}

const titlebarRef = ref()
const closeMeeting = () => {
  proxy.Confirm({
    message: "确定要退出会议吗？",
    okfun:() => {
      titlebarRef.value.custClose()
    }
  })
}

const forceExit = () => {
  titlebarRef.value.custClose()
}

onMounted(() => {
  mitter.on("layoutChange", layoutChangeHandler)

  window.electron.ipcRenderer.on("preCloseWindow", () => {
    closeMeeting()
  })
})

onUnmounted(() => {
  mitter.off("layoutChange", layoutChangeHandler)

  window.electron.ipcRenderer.removeAllListeners('preCloseWindow')
})
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
