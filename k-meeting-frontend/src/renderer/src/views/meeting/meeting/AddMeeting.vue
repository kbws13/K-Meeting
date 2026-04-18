<template>
  <AppDialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="400px"
    :showCancel="false"
    @close="dialogConfig.show = false"
  >
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent>
      <el-form-item label="会议号" prop="meetingNo" v-if="!formData.meetingId">
        <el-input clearable placeholder="请输入会议号" v-model.trim="formData.meetingNo"></el-input>
      </el-form-item>

      <el-form-item label="您的名称" prop="nickName">
        <el-input
          clearable
          placeholder="请输入您的名字"
          v-model.trim="formData.nickName"
        ></el-input>
      </el-form-item>

      <el-form-item label="会议密码" prop="">
        <el-radio-group v-model="formData.joinType">
          <el-radio :value="0">免密入会</el-radio>
          <el-radio :value="1">密码入会</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="入会密码" prop="joinPassword" v-if="formData.joinType == 1">
        <el-input
          clearable
          placeholder="请输入密码"
          :maxlength="5"
          show-word-limit
          v-model="formData.joinPassword"
        ></el-input>
      </el-form-item>

      <el-form-item
        label="选择屏幕"
        prop="screenId"
        v-if="formData.addType == 1 && screenSources.length > 1"
      >
        <el-radio-group v-model="formData.screenId">
          <el-radio :value="item.id" size="large" v-for="item in screenSources" :key="item.id">
            <template #default>
              <Cover :width="90" :scale="0.6" :source="item.thumbnail" borderRadius="0px"></Cover>
              <div>{{ item.name }}</div>
            </template>
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<script setup lang="ts">
import type { ScreenSource } from '@model/ipc'
import { useUserInfoStore } from '../../../stores/UserInfoStore'
import { getCurrentInstance, nextTick, ref } from 'vue'

const userInfoStore = useUserInfoStore()
const { proxy } = getCurrentInstance()

const dialogConfig = ref({
  show: false,
  title: '',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: (e) => {
        addMeeting()
      }
    }
  ]
})

const formData = ref({})
const formDataRef = ref()
const rules = {
  meetingNo: [{ required: true, message: '请输入会议号' }],
  nickName: [{ required: true, message: '请输入您的名字' }],
  joinPassword: [{ required: true, message: '请输入入会密码' }],
  screenId: [{ required: true, message: '请选择共享屏幕' }]
}

const emit = defineEmits(['joinMeeting'])

const addMeeting = () => {
  formDataRef.value.validate(async (valid) => {
    // 1. 表单预校验
    if (!valid) {
      return
    }

    let params = {}
    Object.assign(params, formData.value)

    // 2. 发起网络请求
    // 根据是否含有 meetingId 动态决定调用“预约加入”还是“直接加入”接口
    let result = await proxy.Request({
      url: formData.value.meetingNo ? proxy.Api.preJoinMeeting : proxy.Api.reserveJoinMeeting,
      dataType: 'json',
      params
    })

    // 3. 结果处理
    if (!result) {
      return
    }

    // 4. 成功后关闭弹窗，并通知父组件进入会议流程
    dialogConfig.value.show = false
    emit('joinMeeting', formData.value.addType, formData.value.screenId)
  })
}

const screenSources = ref<ScreenSource[]>([])

/**
 * 弹窗显示初始化
 * @param {string} meetingId 会议ID
 * @param {number} addType 0:加入会议, 1:共享屏幕
 */
const show = async ({ meetingId, addType = 0 }) => {
  const textInfo = addType === 0 ? '加入会议' : '共享屏幕'
  dialogConfig.value.show = true
  dialogConfig.value.title = textInfo
  dialogConfig.value.buttons[0].text = textInfo

  let screenId = ''

  if (addType == 1) {
    debugger
    // 调用 Electron 原生接口获取屏幕/窗口源
    screenSources.value = await window.electron.ipcRenderer.invoke('getScreenSource', {
      types: ['screen'],
      thumbnailSize: {
        width: 600,
        height: 360
      }
    })
    // 默认选中第一个屏幕源
    debugger
    screenId = screenSources.value[0].id
  }

  await nextTick()
  formDataRef.value.resetFields()

  // 填充表单初始数据
  formData.value = {
    addType,
    meetingId,
    joinType: 0,
    nickName: userInfoStore.userInfo.nickName,
    screenId
  }
}

defineExpose({
  show
})
</script>

<style scoped lang="scss">
.setting-panel {
  margin-bottom: 0px;

  :deep(.el-checkbox-group) {
    display: flex;
    flex-direction: column;
  }
}
</style>
