<template>
  <AppDialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="640px"
    :showCancel="false"
    @close="dialogConfig.show = false"
  >
    <div class="screen-source-list">
      <div
        :class="['source-item', screenSource?.displayId == item.displayId ? 'active' : '']"
        v-for="item in screenSources"
        @click="selectSource(item)"
      >
        <Cover :width="125" :scale="0.6" :source="item.thumbnail" borderRadius="0px"></Cover>
        <div class="name">{{ item.name }}</div>
      </div>
    </div>
  </AppDialog>
</template>

<script setup lang="ts">
import type { ScreenSource } from '@model/ipc'
import { mitter } from '@/eventbus/eventBus.ts'
import { nextTick, ref } from 'vue'

const dialogConfig = ref({
  show: false,
  title: '共享',
  buttons: [
    {
      type: 'primary',
      text: '开始共享',
      click: (e) => {
        submitForm()
      }
    }
  ]
})

const screenSources = ref<ScreenSource[]>([])
const screenSource = ref<ScreenSource>()

/**
 * 显示弹窗并加载屏幕资源
 */
const show = () => {
  dialogConfig.value.show = true
  nextTick(async () => {
    // 通过 Electron IPC 渲染进程接口获取桌面资源
    screenSources.value = await window.electron.ipcRenderer.invoke('getScreenSource', {
      types: ['screen'], // 同时显示屏幕和窗口
      thumbnailSize: {
        width: 640,
        height: 360
      }
    })
    // 默认选中第一个资源
    screenSource.value = screenSources.value[0]
  })
}

/**
 * 选择共享源
 */
const selectSource = async (source: ScreenSource): Promise<void> => {
  screenSource.value = source
}

const emit = defineEmits(['shareScreen'])

/**
 * 提交表单，触发共享逻辑
 */
const submitForm = () => {
  // 通过 mitt 广播事件并传递选中的屏幕 ID
  mitter.emit('shareScreen', screenSource.value.id)
  // 关闭弹窗
  dialogConfig.value.show = false
}

// 将 show 方法暴露给父组件调用
defineExpose({
  show
})
</script>

<style scoped lang="scss">
.screen-source-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr); // 四列等分布局
  grid-gap: 10px;
  flex-wrap: wrap;

  .source-item {
    border: 1px solid #ddd;
    border-radius: 5px;
    padding: 8px 10px;
    width: 145px;
    cursor: pointer; // 补充建议：通常项应有手型指针

    &:hover {
      border-color: var(--blue);
    }

    .name {
      overflow: hidden;
      text-overflow: ellipsis; // 文字过长显示省略号
      white-space: nowrap;
      text-align: center;
      margin-top: 2px;
    }
  }

  // 选中状态样式
  .active {
    background: var(--blue);
    border-color: var(--blue);

    .name {
      color: #fff;
    }
  }
}
</style>
