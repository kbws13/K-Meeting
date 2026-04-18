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
      <el-form-item label="UID">
        {{ formData.userId }}
      </el-form-item>

      <el-form-item label="头像" prop="">
        <div class="avatar-upload">
          <Cover :width="80" :scale="1" :source="formData.avatar" defaultImg="user.png"></Cover>
          <el-upload
            :multiple="false"
            :show-file-list="false"
            :http-request="selectFile"
            :accept="proxy.imageAccept"
          >
            <el-button type="primary" class="select-btn">选择</el-button>
          </el-upload>
        </div>
      </el-form-item>

      <el-form-item label="昵称" prop="nickName">
        <el-input clearable placeholder="请输入昵称" v-model.trim="formData.nickName"></el-input>
      </el-form-item>

      <el-form-item label="性别" prop="sex">
        <el-radio-group v-model="formData.sex">
          <el-radio :value="0">女</el-radio>
          <el-radio :value="1">男</el-radio>
          <el-radio :value="2">保密</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<script setup lang="ts">
import { useUserInfoStore } from '../stores/UserInfoStore'
import { getCurrentInstance, ref } from 'vue'
const { proxy } = getCurrentInstance()

const userInfoStore = useUserInfoStore()
const dialogConfig = ref({
  show: false,
  title: '修改用户信息',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: (e) => {
        submitForm()
      }
    }
  ]
})

const selectFile = (file) => {
  file = file.file
  formData.value.avatar = file
}

const formData = ref({})
const formDataRef = ref()

// 表单校验规则
const rules = {
  avatar: [{ required: true, message: '请选择头像' }],
  nickName: [{ required: true, message: '请输入昵称' }],
  sex: [{ required: true, message: '请选择性别' }]
}

/**
 * 显示修改用户信息弹窗
 */
const show = () => {
  // 1. 开启弹窗显示
  dialogConfig.value.show = true

  // 2. 从用户信息仓库（Pinia/Vuex）深拷贝当前用户信息到表单对象
  formData.value = userInfoStore.userInfo

  // 3. 处理头像路径
  // 通过工具函数获取最新头像地址，传递 true 强制刷新缓存
  formData.value.avatar = proxy.Utils.getAvatarPath(formData.value.userId, true)
}

const emit = defineEmits(['reloadInfo'])
const submitForm = () => {
  // 1. 调用 Element Plus 表单组件的 validate 方法进行预校验
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    // 2. 准备请求参数，将响应式表单数据浅拷贝至 params
    let params = {}
    Object.assign(params, formData.value)

    // 3. 发起异步请求更新用户信息
    let result = await proxy.Request({
      url: proxy.Api.updateUserInfo,
      params
    })

    // 4. 业务逻辑处理
    if (!result) {
      return
    }

    // 5. 修改成功后的交互逻辑：
    // - 弹出成功提示
    // - 关闭当前弹窗
    // - 触发 'reloadInfo' 事件，通知父组件同步更新后的用户信息
    proxy.Message.success('修改成功')
    dialogConfig.value.show = false
    emit('reloadInfo', formData.value)
  })
}

defineExpose({
  show
})
</script>

<style lang="scss" scoped>
.avatar-upload {
  display: flex;
  align-items: flex-end;
  .select-btn {
    margin-left: 5px;
  }
}
</style>
