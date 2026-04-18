<template>
  <div class="line" @mousedown="startDrag">
    <div class="iconfont icon-split"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

// 1. 定义接收的 Props
const props = defineProps({
  initWidth: {
    type: Number
  }
})

// 2. 拖拽状态管理
let isDragging = false

/**
 * 开始拖拽
 */
const startDrag = (e) => {
  isDragging = true
  // 在 document 上监听，确保鼠标移出分隔线范围时仍能捕捉移动
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  e.preventDefault() // 阻止默认的文本选择行为
}

// 3. 定义事件分发
const emit = defineEmits(['widthChange'])

/**
 * 拖拽进行中
 */
const onDrag = (e) => {
  if (!isDragging) {
    return
  }

  const windowWidth = window.innerWidth
  // 计算逻辑：新宽度 = 窗口总宽度 - 鼠标当前 X 坐标
  // 这说明该侧边栏位于屏幕右侧
  let newwidth = windowWidth - e.clientX

  // 边界检查：限制最小和最大宽度
  if (newwidth > 600) {
    newwidth = 600
  } else if (newwidth < props.initWidth) {
    newwidth = props.initWidth
  }

  // 实时通知父组件更新宽度
  emit('widthChange', newwidth)
}

/**
 * 停止拖拽
 */
const stopDrag = () => {
  isDragging = false
  // 移除全局监听，释放资源
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 4. 生命周期清理：防止组件销毁后依然残留全局监听器
onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})
</script>

<style lang="scss" scoped>
.line {
  cursor: w-resize; // 鼠标悬停时显示为左右调整大小的指针
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  width: 8px; // 分隔线的宽度
  background: #ededed; // 浅灰色背景
  position: relative;

  .iconfont {
    font-size: 20px; // 中间图标的大小
  }
}
</style>
