<template>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="400px"
    :showCancel="false"
    @close="dialogConfig.show = false"
  >
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent>
      <el-form-item label="会议号" prop="">
        <el-radio-group v-model="formData.meetingNoType">
          <el-radio :value="0">使用个人会议号</el-radio>
          <el-radio :value="1">系统生成</el-radio>
        </el-radio-group>
        <el-input
          clearable
          placeholder="请输入会议号"
          v-model="formData.meetingNo"
          disabled
          v-if="formData.meetingNoType == 0"
        ></el-input>
      </el-form-item>

      <el-form-item label="会议主题" prop="meetingName">
        <el-input
          clearable
          placeholder="请输入会议主题"
          :maxlength="100"
          :show-word-limit="true"
          v-model="formData.meetingName"
        ></el-input>
      </el-form-item>

      <el-form-item label="入会密码" prop="joinType">
        <el-radio-group v-model="formData.joinType">
          <el-radio :value="0">无需密码</el-radio>
          <el-radio :value="1">密码入会</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item prop="joinPassword" v-if="formData.joinType == 1">
        <el-input
          clearable
          placeholder="请输入入会密码"
          :maxlength="5"
          :show-word-limit="true"
          v-model="formData.joinPassword"
        ></el-input>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup lang="ts">
import { ComponentInternalInstance, getCurrentInstance, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { FormInstance, FormRules } from 'element-plus'
import { useUserInfoStore } from '@/stores/UserInfoStore'

const userInfoStore = useUserInfoStore()

const { proxy } = getCurrentInstance() as ComponentInternalInstance & { proxy: any }
const router = useRouter()

interface DialogConfig {
  show: boolean
  title: string
  buttons: Array<{
    type: string
    text: string
    click: (e?: MouseEvent) => void
  }>
}

const dialogConfig = ref<DialogConfig>({
  show: false,
  title: '快速会议',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: () => {
        quickMeeting()
      }
    }
  ]
})

const formData = ref<any>({})
const formDataRef = ref<FormInstance>()

const rules: FormRules = {
  meetingName: [{ required: true, message: '请输入会议主题' }],
  joinPassword: [{ required: true, message: '请输入入会密码' }]
}

const show = () => {
  dialogConfig.value.show = true
  nextTick(() => {
    formDataRef.value.resetFields()
    formData.value = {
      meetingNoType: 0,
      meetingNo: userInfoStore.userInfo.meetingNo,
      joinType: 0
    }
  })
}

const emit = defineEmits(['joinMeeting'])
const quickMeeting = async () => {
  formDataRef.value.validate(async (valid) => {
    // 1. 表单校验
    if (!valid) {
      return
    }

    // 2. 发起网络请求，提交会议配置数据
    let result = await proxy.Request({
      url: proxy.Api.quickMeeting,
      dataType: 'json',
      params: Object.assign({}, formData.value)
    })

    // 3. 结果处理
    if (!result) {
      return
    }

    // 4. 成功后关闭弹窗并通知父组件进入会议
    dialogConfig.value.show = false
    emit('joinMeeting')
  })
}

defineExpose({
  show
})
</script>

<style scoped lang="scss"></style>
