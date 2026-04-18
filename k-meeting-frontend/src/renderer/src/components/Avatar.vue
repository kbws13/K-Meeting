<template>
  <div :style="{ width: width + 'px', height: width + 'px' }">
    <Cover :lazy="false" :width="width" :source="avatarUrl" borderRadius="50%" :scale="1"></Cover>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'

// 获取全局代理对象，用于访问挂载在全局的 Utils 工具类
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

// 定义组件 Props
const props = defineProps({
  // 头像宽度（高度与宽度保持一致）
  width: {
    type: Number,
    default: 50
  },
  // 头像标识（通常是 userId 或头像文件名）
  avatar: {
    type: [String, Number]
  },
  // 是否强制更新缓存
  update: {
    type: Boolean,
    default: false
  }
})

// 响应式头像 URL，初始化时调用工具函数获取路径
const avatarUrl = ref(proxy.Utils.getAvatarPath(props.avatar, props.update))

/**
 * 手动更新头像 URL 的方法
 * 通过传递 true 给工具函数，通常用于绕过浏览器图片缓存（例如在 URL 后加时间戳）
 */
const updateAvatarUrl = () => {
  avatarUrl.value = proxy.Utils.getAvatarPath(props.avatar, true)
}

// 将更新方法暴露给父组件
defineExpose({
  updateAvatarUrl
})
</script>

<style scoped lang="scss"></style>
