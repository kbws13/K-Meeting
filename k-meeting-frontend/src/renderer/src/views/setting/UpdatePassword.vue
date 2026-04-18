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
      <el-form-item label="原密码" prop="oldPassword">
        <el-input
          clearable
          placeholder="请输入原密码"
          type="password"
          show-password
          v-model.trim="formData.oldPassword"
        ></el-input>
      </el-form-item>

      <el-form-item label="新密码" prop="password">
        <el-input
          clearable
          placeholder="请输入新密码"
          type="password"
          show-password
          v-model.trim="formData.password"
        ></el-input>
      </el-form-item>

      <el-form-item label="确认密码" prop="rePassword">
        <el-input
          clearable
          placeholder="请再次输入新密码"
          type="password"
          show-password
          v-model.trim="formData.rePassword"
        ></el-input>
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { FormInstance, FormRules } from 'element-plus'
import { useAppProxy } from '@/composables/useAppProxy'

const proxy = useAppProxy()
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

interface UpdatePasswordForm {
  oldPassword: string
  password: string
  rePassword: string
}

const dialogConfig = ref<DialogConfig>({
  show: false,
  title: '标题',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: () => {
        submitForm()
      }
    }
  ]
})

const formData = ref<UpdatePasswordForm>({
  oldPassword: '',
  password: '',
  rePassword: ''
})
const formDataRef = ref<FormInstance>()

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  password: [
    { required: true, message: '请输入新密码' },
    { validator: proxy.Verify.password, message: '密码只能是数字,字母,下划线8-18位' }
  ],
  rePassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void): void => {
        if (value !== formData.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }
    }
  ]
}

const submitForm = (): void => {
  formDataRef.value?.validate(async (valid) => {
    if (!valid) {
      return
    }
    const params = {
      password: formData.value.oldPassword,
      newPassword: formData.value.password
    }
    const result = await proxy.Request({
      url: proxy.Api.updatePassword,
      params
    })
    if (!result) {
      return
    }
    proxy.Message.success('请重新登录')
    dialogConfig.value.show = false
    await window.electron.ipcRenderer.invoke('logout')
    router.push('/')
  })
}

const show = async (): Promise<void> => {
  dialogConfig.value.show = true
  await nextTick()
  formDataRef.value?.resetFields()
}

defineExpose({
  show
})
</script>

<style scoped lang="scss"></style>
