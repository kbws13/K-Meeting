<template>
  <el-config-provider :locale="zhCn">
    <router-view></router-view>
  </el-config-provider>
</template>

<script lang="ts" setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import { useUserInfoStore } from './stores/UserInfoStore'
import { getCurrentInstance, onMounted } from 'vue'
const { proxy } = getCurrentInstance()

const userInfoStore = useUserInfoStore()
const saveUserInfoStore = async () => {
  userInfoStore.setInfo(JSON.parse(localStorage.getItem('userInfo')) || {})
}

const onExitMeeting = () => {
  window.electron.ipcRenderer.on('closeWindow', async (e, { windowId}) => {
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
