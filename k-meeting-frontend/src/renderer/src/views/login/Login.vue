<template>
  <AppHeader :show-max="true" :close-type="0" title=""></AppHeader>
  <div v-if="showLoading" class="loading-panel">
    <img src="../../assets/images/loading.gif" alt="loading" />
    <div>正在登录......</div>
  </div>
  <div v-else class="login-form">
    <div class="error-msg">{{ errorMsg }}</div>
    <el-form ref="formDataRef" :model="formData" label-width="0px" @submit.prevent>
      <el-form-item prop="email">
        <el-input v-model.trim="formData.email" clearable placeholder="请输入邮箱" size="large">
          <template #prefix>
            <span class="iconfont icon-email"></span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="nickName">
        <el-input
          v-model.trim="formData.nickName"
          clearable
          placeholder="请输入昵称"
          maxlength="15"
          size="large"
        >
          <template #prefix>
            <span class="iconfont icon-user-nick"></span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password" size="large">
        <el-input
          v-model.trim="formData.password"
          clearable
          placeholder="请输入密码"
          show-password
          :maxlength="18"
        >
          <template #prefix>
            <span class="iconfont icon-password"></span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item v-if="!isLogin" prop="rePassword">
        <el-input
          v-model.trim="formData.rePassword"
          clearable
          placeholder="请再次输入密码"
          show-password
          size="large"
        >
          <template #prefix>
            <span class="iconfont icon-password"></span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="checkCode">
        <div class="check-code-panel">
          <el-input
            v-model.trim="formData.checkCode"
            clearable
            placeholder="请输入验证码"
            size="large"
          >
            <template #prefix>
              <span class="iconfont icon-checkcode"></span>
            </template>
          </el-input>
          <img :src="checkCodeUrl" class="check-code" alt="check-code" @click="changeCheckCode" />
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" class="login-btn" size="large" @click="submit">
          {{ isLogin ? '登录' : '注册' }}
        </el-button>
      </el-form-item>

      <div class="bottom-link">
        <span class="a-link no-account" @click="changeOptype">{{
          isLogin ? '没有账号' : '已有账号？'
        }}</span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { md5 } from 'js-md5'
import type { PersistedUserInfo } from '@model/user'
import { useUserInfoStore } from '@/stores/UserInfoStore'
import { useAppProxy } from '@/composables/useAppProxy'

const userInfoStore = useUserInfoStore()
const proxy = useAppProxy()
const router = useRouter()

/** 验证码响应数据结构 */
interface CheckCodeResponse {
  checkCode: string
  checkCodeKey: string
}

/** 登录/注册请求参数 */
interface AuthParams extends Record<string, unknown> {
  email: string
  password: string
  checkCode: string
  nickName?: string
  checkCodeKey: string | null
}

type VerifyMethod = 'checkEmail' | 'checkPassword'

const checkCodeUrl = ref<string>('')

/**
 * 刷新/获取验证码
 */
const changeCheckCode = async (): Promise<void> => {
  const result = await proxy.Request<CheckCodeResponse>({
    url: proxy.Api.checkCode,
    method: 'get'
  })
  if (!result) {
    return
  }
  checkCodeUrl.value = result.data.checkCode
  localStorage.setItem('checkCodeKey', result.data.checkCodeKey)
}
changeCheckCode()

const isLogin = ref<boolean>(true)
const formData = ref<Record<string, string>>({})
const formDataRef = ref<FormInstance>()

/* 更改窗口大小 */
const changeOptype = async (): Promise<void> => {
  await window.electron.ipcRenderer.invoke('loginOrRegister', !isLogin.value)
  isLogin.value = !isLogin.value
  nextTick(() => {
    formDataRef.value?.resetFields()
    formData.value = {}
    changeCheckCode()
    cleanVerify()
  })
}

const showLoading = ref<boolean>(false)
const errorMsg = ref<string>('')

/* 清空错误提示 */
const cleanVerify = (): void => {
  errorMsg.value = ''
}

/**
 * 校验表单字段
 * @param type  校验类型（对应 proxy.Verify 上的方法名），传 null 则仅做非空校验
 * @param value 待校验的值
 * @param msg   校验失败时的提示信息
 */
const checkValue = (type: VerifyMethod | null, value: string | undefined, msg: string): boolean => {
  if (proxy.Utils.isEmpty(value)) {
    errorMsg.value = msg
    return false
  }
  if (type && !proxy.Verify[type](value)) {
    errorMsg.value = msg
    return false
  }
  return true
}

const buildAuthParams = (): AuthParams => {
  return {
    email: formData.value.email,
    password: isLogin.value ? md5(formData.value.password) : formData.value.password,
    checkCode: formData.value.checkCode,
    nickName: formData.value.nickName,
    checkCodeKey: localStorage.getItem('checkCodeKey')
  } as AuthParams
}

const submit = async (): Promise<void> => {
  cleanVerify()
  if (!checkValue('checkEmail', formData.value.email, '请输入正确的邮箱')) {
    return
  }
  if (!isLogin.value && !checkValue(null, formData.value.nickName, '请输入昵称')) {
    return
  }
  if (
    !checkValue('checkPassword', formData.value.password, '密码只能是数字，字母，特殊字符8~18位')
  ) {
    return
  }
  if (!checkValue(null, formData.value.checkCode, '请输入验证码')) {
    return
  }
  if (!isLogin.value && !checkValue(null, formData.value.rePassword, '请再次输入密码')) {
    return
  }
  if (!isLogin.value && formData.value.password !== formData.value.rePassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  if (isLogin.value) {
    showLoading.value = true
  }

  if (isLogin.value) {
    const result = await proxy.Request<PersistedUserInfo>({
      url: proxy.Api.login,
      dataType: 'json',
      showError: false,
      showLoading: false,
      params: buildAuthParams(),
      errorCallback: (response: { message: string }) => {
        showLoading.value = false
        changeCheckCode()
        errorMsg.value = response.message
      }
    })
    if (!result) {
      return
    }
    await window.electron.ipcRenderer.invoke('loginSuccess', {
      userInfo: result.data,
      wsUrl: import.meta.env.VITE_WS
    })
    userInfoStore.setInfo(result.data)
    await router.push('/home')
  } else {
    const result = await proxy.Request<boolean>({
      url: proxy.Api.register,
      dataType: 'json',
      showError: false,
      showLoading: false,
      params: buildAuthParams(),
      errorCallback: (response: { message: string }) => {
        showLoading.value = false
        changeCheckCode()
        errorMsg.value = response.message
      }
    })
    if (!result) {
      return
    }
    proxy.Message.success('注册成功')
    await changeOptype()
  }
}
</script>

<style scoped lang="scss">
.title {
  height: 30px;
  -webkit-app-region: drag;
}

.email-select {
  width: 250px;
}

.loading-panel {
  height: calc(100vh - 32px);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  font-size: 14px;
  color: #727272;

  img {
    width: 30px;
    margin-right: 3px;
  }
}

/* 来自图片 1 的代码 */
.login-form {
  padding: 0 15px;
  height: calc(100vh - 32px);

  :deep(.el-input__wrapper) {
    box-shadow: none;
    border-radius: none;
  }

  .el-form-item {
    border-bottom: 1px solid #ddd;
  }

  .email-panel {
    align-items: center;
    width: 100%;
    display: flex;

    .input {
      flex: 1;
    }

    .icon-down {
      margin-left: 3px;
      width: 16px;
      cursor: pointer;
      border: none;
    }
  }

  .error-msg {
    line-height: 30px;
    height: 30px;
    color: #fb7373;
  }

  .check-code-panel {
    display: flex;

    .check-code {
      cursor: pointer;
      width: 120px;
      margin-left: 5px;
    }
  }

  .login-btn {
    margin-top: 20px;
    width: 100%;
  }

  .bottom-link {
    text-align: right;
    font-size: 13px;
  }
}
</style>
