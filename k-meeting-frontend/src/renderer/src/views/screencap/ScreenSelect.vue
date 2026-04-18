<template>
  <div class="screen-source-list">
    <div
      :class="['source-item', screenDisplayId == item.displayId ? 'active' : '']"
      v-for="item in screenSources"
      @click="selectSource(item)"
    >
      <Cover :source="item.thumbnail" borderRadius="0px"></Cover>
      <div class="name">{{ item.name }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ScreenSource } from '@model/ipc'
import { onMounted, ref } from 'vue'

// 2. 定义组件的 Emit 事件
const emit = defineEmits<{
  (e: 'selectScreenDisplayId', displayId: string | number): void
}>()

// 3. 响应式变量
const screenDisplayId = ref<string | number>()
const screenSources = ref<ScreenSource[]>([])

/**
 * 手动选择屏幕源
 */
const selectSource = async (source: ScreenSource): Promise<void> => {
  screenDisplayId.value = source.displayId
  emit('selectScreenDisplayId', screenDisplayId.value)
}

/**
 * 获取系统屏幕和窗口资源
 */
const getScreen = async (): Promise<void> => {
  // 调用 Electron 主进程接口
  const sources: ScreenSource[] = await window.electron.ipcRenderer.invoke('getScreenSource', {
    types: ['screen'],
    thumbnailSize: {
      width: 600,
      height: 360
    }
  })

  screenSources.value = sources

  // 默认选中第一个屏幕源
  if (sources.length > 0) {
    screenDisplayId.value = sources[0].displayId
    emit('selectScreenDisplayId', screenDisplayId.value)
  }
}

// 4. 生命周期钩子
onMounted(() => {
  getScreen()
})
</script>

<style scoped lang="scss">
.screen-source-list {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-gap: 10px;
  flex-wrap: wrap;

  .source-item {
    overflow: hidden;
    border: 2px solid #ddd;
    border-radius: 5px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    &:hover {
      border-color: var(--blue);
    }

    .name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      text-align: center;
      margin-top: 2px;
      padding: 4px 0px;
    }
  }

  .active {
    border-color: var(--blue);

    .name {
      color: var(--blue);
    }
  }
}
</style>
