<template>
  <div
    class="op-btns"
    :style="{
      top: `${styleTop}px`,
      right: `${styleRight}px`,
      'border-radius': `${borderRadius}px`
    }"
  >
    <div
      :style="{ 'border-radius': `${borderRadius}px` }"
      v-if="showMin"
      class="iconfont icon-min"
      @click="minimize()"
      title="最小化"
    ></div>
    <div
      :style="{ 'border-radius': `${borderRadius}px` }"
      v-if="showMax"
      :class="['iconfont', isMax ? 'icon-maximize' : 'icon-max']"
      :title="isMax ? '还原' : '最大化'"
      @click="maximize()"
    ></div>
    <div
      :style="{ 'border-radius': `${borderRadius}px` }"
      v-if="showClose"
      class="iconfont icon-close"
      title="关闭"
      @click="close()"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface Props {
  showMin?: boolean
  showMax?: boolean
  showClose?: boolean
  /** 0: 关闭, 1: 隐藏 */
  closeType?: number
  styleTop?: number
  styleRight?: number
  borderRadius?: number
  forceClose?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showMin: true,
  showMax: false,
  showClose: true,
  closeType: 1,
  styleTop: 0,
  // 图片中 styleRight 的默认值被截断了，此处按需补充
  styleRight: 0,
  borderRadius: 0,
  forceClose: true
})

const isMax = ref<Boolean>(false)

const winOp = (action, data) => {
  window.electron.ipcRenderer.send('winTitleOp', { action, data })
}
const close = () => {
  winOp('close', { closeType: props.closeType, forceClose: props.forceClose })
}
const custClose = () => {
  winOp('close', { closeType: props.closeType, forceClose: true })
}
const minimize = () => {
  winOp('minimize')
}
const maximize = () => {
  if (isMax.value) {
    winOp('unmaximize')
  } else {
    winOp('maximize')
  }
}

onMounted(() => {
  isMax.value = false
  window.electron.ipcRenderer.on('winIsMax', (e, result) => {
    isMax.value = result
  })
})

defineExpose({
  custClose
})
</script>

<style scoped lang="scss">
.op-btns {
  position: absolute;
  -webkit-app-region: no-drag;
  display: flex;

  .iconfont {
    color: var(--text);
    padding: 6px;
    cursor: pointer;

    &:hover {
      background: #ddd;
    }
  }

  .icon-close {
    &:hover {
      background: #fa4e32;
      color: #fff;
    }
  }

  .win-top {
    color: var(--pink);
  }
}
</style>
