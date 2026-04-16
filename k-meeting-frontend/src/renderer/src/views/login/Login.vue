<template>
  <Header :showMax="true" :closeType="0"></Header>
  <div class="loading-panel" v-if="showLoading">
    <img src="../../assets/images/loading.gif" alt="loading" />
    <div>正在登录......</div>
  </div>
  <div class="login-form" v-else>
    <div class="error-msg">{{ errorMsg }}</div>
    <el-form :model="formData" ref="formDataRef" label-width="0px" @submit.prevent>
      <el-form-item prop="email">
        <el-input clearable placeholder="请输入邮箱" v-model.trim="formData.email" size="large">
          <template #prefix>
            <span class="iconfont icon-email"></span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="nickName" v-if="!isLogin">
        <el-input
          clearable
          placeholder="请输入昵称"
          v-model.trim="formData.nickName"
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
          clearable
          placeholder="请输入密码"
          v-model.trim="formData.password"
          show-password
          :maxlength="18"
        >
          <template #prefix>
            <span class="iconfont icon-password"></span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="rePassword" v-if="!isLogin">
        <el-input
          clearable
          placeholder="请再次输入密码"
          v-model.trim="formData.rePassword"
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
            clearable
            placeholder="请输入验证码"
            v-model.trim="formData.checkCode"
            size="large"
          >
            <template #prefix>
              <span class="iconfont icon-checkcode"></span>
            </template>
          </el-input>
          <img :src="checkCodeUrl" class="check-code" @click="changeCheckCode" />
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" class="login-btn" size="large" @click="submit">
          {{ isLogin ? '登录' : '注册' }}</el-button
        >
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
import { ref, nextTick, getCurrentInstance, type ComponentInternalInstance } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import md5 from 'js-md5'
import { useUserInfoStore } from '@/stores/UserInfoStore'

const userInfoStore = useUserInfoStore()

const { proxy } = getCurrentInstance() as ComponentInternalInstance
const router = useRouter()

/** 验证码响应数据结构 */
interface CheckCodeResponse {
  checkCode: string
  checkCodeKey: string
}

/** 登录/注册请求参数 */
interface AuthParams {
  email: string
  password: string
  checkCode: string
  nickName?: string
  checkCodeKey: string | null
}

const checkCodeUrl = ref<string>('')

/**
 * 刷新/获取验证码
 */
const changeCheckCode = async (): Promise<void> => {
  const result = await (proxy as any).Request<CheckCodeResponse>({
    url: (proxy as any).Api.checkCode
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
const checkValue = (type: string | null, value: string | undefined, msg: string): boolean => {
  if ((proxy as any).Utils.isEmpty(value)) {
    errorMsg.value = msg
    return false
  }
  if (type && !(proxy as any).Verify[type](value)) {
    errorMsg.value = msg
    return false
  }
  return true
}

const submit = async (): Promise<void> => {
  cleanVerify()
  if (!checkValue('checkEmail', formData.value.email, '请输入正确的邮箱')) {
    return
  }
  if (!isLogin.value && !checkValue(null, formData.value.nickName, '请输入昵称')) {
    return
  }
  if (!checkValue('checkPassword', formData.value.password, '密码只能是数字，字母，特殊字符8~18位')) {
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

  // 登录/注册逻辑处理
  // 注册接口后端使用 @RequestBody，需以 JSON 格式发送；登录接口同理
  const result = await (proxy as any).Request({
    url: isLogin.value ? (proxy as any).Api.login : (proxy as any).Api.register,
    dataType: 'json',
    showError: false,
    showLoading: false,
    params: {
      email: formData.value.email,
      password: isLogin.value ? md5(formData.value.password) : formData.value.password,
      checkCode: formData.value.checkCode,
      nickName: formData.value.nickName,
      checkCodeKey: localStorage.getItem('checkCodeKey')
    } as AuthParams,
    errorCallback: (response: { message: string }) => {
      showLoading.value = false
      changeCheckCode()
      errorMsg.value = response.message
    }
  })

  // 如果请求失败（result 为空），中断后续执行
  if (!result) {
    return
  }

  // 成功处理逻辑
  if (isLogin.value) {
    await window.electron.ipcRenderer.invoke('loginSuccess', {
      userInfo: result.data,
      wsUrl: import.meta.env.VITE_WS
    })
    userInfoStore.setInfo(result.data)
    router.push('/home')
  } else {
    // 注册成功提示并切换操作类型（通常是切回登录界面）
    (proxy as any).Message.success('注册成功')
    changeOptype()
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
  padding: 0px 15px;
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
