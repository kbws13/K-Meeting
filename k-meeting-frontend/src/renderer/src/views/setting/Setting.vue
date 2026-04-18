<template>
  <AppHeader :show-bottom-border="false" :show-max="true"></AppHeader>
  <div class="body-panel">
    <div class="part-title">常规设置</div>
    <div class="part-content">
      <div>
        <el-checkbox
          v-model="formData.openCamera"
          label="入会开启摄像头"
          size="large"
          @change="saveSetting"
        />
      </div>
      <div>
        <el-checkbox
          v-model="formData.openMic"
          label="入会开启麦克风"
          size="large"
          @change="saveSetting"
        />
      </div>
    </div>

    <div class="part-title">本地录制</div>
    <div class="cap-folder">
      <div>录制文件保存目录</div>
      <div class="folder">{{ formData.screencapFolder }}</div>
      <el-button type="primary" @click="changeLocalFolder">更改</el-button>
      <el-button type="success" @click="openLocalFolder">打开</el-button>
    </div>

    <div class="part-title">账号安全</div>
    <div class="part-content">
      <div class="part-item">
        <div class="part-tips">密码修改后，您需重新登录</div>
        <el-button type="primary" @click="updatePassword">修改密码</el-button>
      </div>
      <div class="part-item">
        <div class="part-tips">退出当前登录账号</div>
        <el-button type="primary" @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="part-title">版本更新</div>
    <div class="part-content">
      <div class="part-item">
        <div class="part-tips">更新版本</div>
        <el-button type="primary" @click="checkUpdate">检查更新</el-button>
      </div>
    </div>
  </div>
  <UpdatePassword ref="updatePasswordRef"></UpdatePassword>

  <AppUpdate ref="appUpdateRef" :auto-update="false"></AppUpdate>
</template>

<script setup lang="ts">
import type { SysSetting } from '@model/system'
import UpdatePassword from './UpdatePassword.vue'
import AppUpdate from '../AppUpdate.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppProxy } from '@/composables/useAppProxy'

const proxy = useAppProxy()
const router = useRouter()

const formData = ref<SysSetting>({
  openCamera: true,
  openMic: true,
  screencapFolder: 'C:/Users/Administrator/.easymeeting/'
})

const getSysSetting = async (): Promise<void> => {
  const sysSetting = await window.electron.ipcRenderer.invoke<Partial<SysSetting>>('getSysSetting')
  formData.value = {
    ...formData.value,
    ...sysSetting
  }
}
getSysSetting()

const saveSetting = async (): Promise<void> => {
  await window.electron.ipcRenderer.invoke('saveSysSetting', JSON.stringify(formData.value))
}

const changeLocalFolder = async (): Promise<void> => {
  const localPath = await window.electron.ipcRenderer.invoke('changeLocalFolder', {
    localFilePath: formData.value.screencapFolder.replaceAll('/', '\\')
  })
  if (localPath) {
    formData.value.screencapFolder = localPath.replaceAll('\\', '/') + '/'
    await saveSetting()
  }
}

const openLocalFolder = (): void => {
  window.electron.ipcRenderer.send('openLocalFile', {
    localFilePath: formData.value.screencapFolder,
    folder: true
  })
}

const updatePasswordRef = ref<{ show: () => void } | null>(null)
const updatePassword = (): void => {
  updatePasswordRef.value?.show()
}

const logout = (): void => {
  proxy.Confirm({
    message: '确定要退出吗?',
    okfun: async () => {
      // 1. 发起后端登出请求，清理服务器 Session/Token
      let result = await proxy.Request({
        url: proxy.Api.logout
      })

      if (!result) {
        return
      }

      // 2. 通知 Electron 主进程处理退出逻辑（如清理本地缓存、关闭窗口等）
      await window.electron.ipcRenderer.invoke('logout')
      router.push('/')
    }
  })
}

const appUpdateRef = ref<{ checkUpdate: () => void } | null>(null)
const checkUpdate = (): void => {
  appUpdateRef.value?.checkUpdate()
}
</script>

<style scoped lang="scss">
.body-panel {
  height: calc(100vh - 32px);
  padding: 20px;

  .part-title {
    font-weight: bold;
    margin-top: 10px;
  }

  .cap-folder {
    display: flex;
    font-size: 14px;
    align-items: center;

    .folder {
      flex: 1;
      width: 0;
      text-overflow: ellipsis;
      overflow: hidden;
      white-space: nowrap;
      padding: 3px;
      border: 1px solid #ddd;
      margin: 0px 10px;
    }
  }

  .part-content {
    .part-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin: 5px 0px;

      .part-tips {
        color: #535353;
        font-size: 14px;
      }
    }
  }
}
</style>
