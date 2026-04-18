<template>
  <div v-if="showUpdate">
    <div class="update-container drag"></div>
    <div class="update-panel no-drag">
      <div class="update-inner">
        <div class="update-content">
          <div class="update-content-title">更新内容</div>
          <div class="update-list">
            <div v-for="(item, index) in updateList" :key="`${index}-${item}`">
              {{ index + 1 }}、 {{ item }}
            </div>
          </div>
        </div>
        <div class="download-progress" v-if="downloading">
          <div class="download-tips">
            正在下载，请稍候
            <span v-if="downloadLoaded > 0">({{ proxy.Utils.size2Str(downloadLoaded) }})</span>
          </div>
        </div>
        <div class="op-btn" v-else>
          <div class="cancel" @click="cancelUpdateHandler">残忍拒绝</div>
          <div class="update" @click="startDownload">更新</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import config from '../../../../package.json'
import type { AppUpdateCheckVO } from '@model/update'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useAppProxy } from '@/composables/useAppProxy'
import { useUserInfoStore } from '@/stores/UserInfoStore.ts'

const proxy = useAppProxy()
const userInfoStore = useUserInfoStore()

const props = defineProps({
  autoUpdate: {
    type: Boolean,
    default: true
  }
})

const showUpdate = ref(false)
const updateInfo = ref<AppUpdateCheckVO>({
  hasUpdate: false,
  id: null,
  version: null,
  updateDesc: null,
  fileType: null,
  outerLink: null,
  downloadUrl: null
})
const downloading = ref(false)
const downloadLoaded = ref(0)
const updateList = computed(() => {
  const rawText = updateInfo.value.updateDesc
  if (!rawText) {
    return []
  }

  return rawText
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
})

const checkUpdateAuto = async (auto: boolean) => {
  const res = await proxy.Request<AppUpdateCheckVO>({
    method: 'get',
    url: proxy.Api.checkVersion,
    params: {
      version: config.version,
      uid: userInfoStore.getInfo().userId
    }
  })
  if (!res) {
    return
  }
  if (!res.data.hasUpdate) {
    if (!auto) {
      proxy.Confirm({ message: '已经是最新版本！', showCancelBtn: false })
    }
    return
  }
  console.log(res.data.hasUpdate)
  showUpdate.value = true
  updateInfo.value = res.data
}

const cancelUpdateHandler = (): void => {
  showUpdate.value = false
}

const startDownload = (): void => {
  if (
    updateInfo.value.fileType === 0 &&
    updateInfo.value.id != null &&
    updateInfo.value.downloadUrl
  ) {
    downloading.value = true
    downloadLoaded.value = 0
    window.electron.ipcRenderer.send('downloadUpdate', {
      id: updateInfo.value.id,
      downloadUrl: updateInfo.value.downloadUrl
    })
  } else if (updateInfo.value.fileType === 1 && updateInfo.value.outerLink) {
    window.electron.ipcRenderer.send('openUrl', {
      url: updateInfo.value.outerLink
    })
  }
}

const checkUpdate = (): void => {
  void checkUpdateAuto(false)
}

onMounted(() => {
  if (props.autoUpdate) {
    void checkUpdateAuto(true)
  }

  window.electron.ipcRenderer.on('updateDownloadCallback', (_event, loaded) => {
    downloadLoaded.value = loaded
  })

  window.electron.ipcRenderer.on('updateDownloadError', (_event, message) => {
    downloading.value = false
    downloadLoaded.value = 0
    proxy.Message.error(message)
  })

  window.electron.ipcRenderer.on('updateDownloadFinished', (_event, message) => {
    downloading.value = false
    showUpdate.value = false
    proxy.Message.success(message)
  })
})

onUnmounted(() => {
  window.electron.ipcRenderer.removeAllListeners('updateDownloadCallback')
  window.electron.ipcRenderer.removeAllListeners('updateDownloadError')
  window.electron.ipcRenderer.removeAllListeners('updateDownloadFinished')
})

defineExpose({
  checkUpdate
})
</script>

<style scoped lang="scss">
.update-container {
  opacity: 0.2;
  background: #000;
  z-index: 1;
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: calc(100vh);
}

.update-panel {
  -webkit-app-region: no-drag;
  top: 30px;
  left: 0;
  width: 100%;
  position: absolute;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;

  .update-inner {
    background-image: url('@/assets/images/update_bg.png');
    background-size: 100%;
    background-position: top center;
    background-repeat: no-repeat;
    width: 350px;
    min-height: 400px;

    .update-content {
      margin-top: 230px;
      background: #fff;
      padding: 15px;

      .update-content-title {
        font-size: 18px;
        color: #000;
      }

      .update-list {
        margin-top: 5px;
        max-height: 80px;
        overflow: auto;
      }
    }

    .download-progress {
      background: #fff;
      padding: 10px;
      border-radius: 0 0 10px 10px;

      .download-tips {
        margin-top: 5px;
        text-align: center;
        font-size: 14px;
        color: #6e6e6e;
      }

      .finish-tips {
        text-align: center;
        color: #4d4d4d;
        font-size: 13px;
      }
    }

    .op-btn {
      background: #fff;
      border-radius: 0 0 10px 10px;
      border-top: 1px solid #ddd;
      display: flex;
      align-items: center;
      overflow: hidden;
      line-height: 40px;

      .cancel {
        width: 50%;
        text-align: center;
        color: #989898;
        cursor: pointer;
      }

      .update {
        width: 50%;
        border-left: 1px solid #ddd;
        text-align: center;
        background: #07c160;
        color: #fff;
        cursor: pointer;
      }
    }
  }
}
</style>
