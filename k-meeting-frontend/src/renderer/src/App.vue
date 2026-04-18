<template>
  <el-config-provider :locale="zhCn">
    <router-view></router-view>
  </el-config-provider>
</template>

<script lang="ts" setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useUserInfoStore } from './stores/UserInfoStore'
import { onMounted } from 'vue'
import { useAppProxy } from './composables/useAppProxy'

const proxy = useAppProxy()
const userInfoStore = useUserInfoStore()

const saveUserInfoStore = (): void => {
  const rawUserInfo = localStorage.getItem('userInfo')
  userInfoStore.setInfo(rawUserInfo ? JSON.parse(rawUserInfo) : {})
}

const onExitMeeting = (): void => {
  window.electron.ipcRenderer.on('closeWindow', async (_event, payload) => {
    const { windowId } = (payload ?? {}) as { windowId?: string }
    if (windowId === 'meeting') {
      await proxy.Request({
        url: proxy.Api.exitMeeting
      })
    }
  })
}

onMounted(() => {
  onExitMeeting()
  saveUserInfoStore()
})
</script>

<style lang="scss"></style>
