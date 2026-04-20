<template>
  <div class="media-window">
    <div class="header">
      <div class="media-op no-drag">
        <div
          :class="['iconfont icon-left', currentIndex == 0 ? 'not-allow' : '']"
          @dblclick.stop
          title="上一张"
          @click="next(-1)"
        ></div>
        <div
          :class="['iconfont icon-right', currentIndex >= allFileList.length - 1 ? 'not-allow' : '']"
          @dblclick.stop
          title="下一张"
          @click="next(1)"
        ></div>

        <template v-if="currentFile.fileType == 0">
          <el-divider direction="vertical" />
          <div
            class="iconfont icon-enlarge"
            @click.stop="changeSize(0.1)"
            @dblclick.stop
            title="放大"
          ></div>
          <div
            class="iconfont icon-narrow"
            @click="changeSize(-0.1)"
            @dblclick.stop
            title="缩小"
          ></div>
          <div
            :class="['iconfont', isOne2One ? 'icon-resize' : 'icon-source-size']"
            @dblclick.stop
            @click="resize"
            :title="isOne2One ? '图片适应窗口大小' : '图片原始大小'"
          ></div>
          <div class="iconfont icon-rotate" @dblclick.stop @click="rotate" title="旋转"></div>
          <el-divider direction="vertical" />
        </template>

        <div>
          <div
            class="iconfont icon-download"
            @dblclick.stop
            @click="download"
            title="另存为..."
          ></div>
        </div>
      </div>
      <Titlebar :closeType="0" :styleTop="0" :styleRight="0" ref="titlebarRef"></Titlebar>
    </div>
    <div class="media-panel">
      <viewer
        :options="options"
        @inited="inited"
        :images="[currentFile.url]"
        v-if="currentFile.fileType == 0"
      >
        <img :src="currentFile.url" />
      </viewer>

      <Player
        v-show="currentFile.fileType == 1"
        ref="player"
      ></Player>
    </div>
  </div>
</template>

<script setup lang="ts">
import 'viewerjs/dist/viewer.css'
import Player from '@/components/Player.vue'
import { component as Viewer } from 'v-viewer'
import { getCurrentInstance, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()

const options = ref({
  inline: true,
  toolbar: false,
  navbar: false,
  button: false,
  title: false,
  zoomRatio: 0.1,
  zoomOnWheel: false
})

// 存储 viewer 实例的引用
const viewerMy = ref(null)

// 初始化 viewer 实例
const inited = (e) => {
  viewerMy.value = e
}

// 放大/缩小：传入缩放比率 (zoomRatio)
const changeSize = (zoomRatio) => {
  viewerMy.value.zoom(zoomRatio, true)
}

// 旋转：每次顺时针旋转 90 度
const rotate = () => {
  viewerMy.value.rotate(90, true)
}

// 尺寸切换：在“适应窗口”与“原始大小”之间切换
const isOne2One = ref(false)
const resize = () => {
  isOne2One.value = !isOne2One.value
  if (!isOne2One.value) {
    // 切换回适应窗口模式
    viewerMy.value.zoomTo(viewerMy.value.initialImageData.ratio, true)
  } else {
    // 切换至 1:1 原始大小
    viewerMy.value.zoomTo(1, true)
  }
}

// 鼠标滚轮缩放：监听滚轮事件，实现更自然的缩放交互
const onWheel = (e) => {
  console.log(e.deltaY)
  if (e.deltaY < 0) {
    changeSize(0.1) // 向上滚动放大
  } else {
    changeSize(-0.1) // 向下滚动缩小
  }
}

// 图片切换：用于切换上一张或下一张图片
const next = (index) => {
  // 边界检查：防止越界
  if (currentIndex.value + index < 0 || currentIndex.value + index >= allFileList.value.length) {
    return
  }
  player.value.destroyPlayer()
  currentIndex.value = currentIndex.value + index
  getCurrentFile()
}

const player = ref()
const currentIndex = ref(0)
const allFileList = ref([])
const currentFile = ref({ fileType: 0 })

/**
 * 获取并设置当前选中的文件信息
 */
const getCurrentFile = () => {
  // 根据当前索引从列表中取出文件对象
  const curFile = allFileList.value[currentIndex.value]
  // 调用工具类获取真实的资源访问路径
  const url = proxy.Utils.getResourcePath(curFile)

  // 组装并更新当前文件状态
  currentFile.value = { ...curFile, url }

  // 如果是视频类型，预留视频播放处理逻辑
  if (curFile.fileType == 1) {
    player.value.showPlayer(url)
  }
}

/**
 * 触发文件下载
 */
const download = async () => {
  // 通过 IPC 调用主进程的下载功能，并传入下载所需的上下文信息
  await window.electron.ipcRenderer.invoke('download', {
    url: import.meta.env.VITE_DOMAIN + proxy.Api.downloadFile, // 拼接完整的下载接口地址
    fileName: currentFile.value.fileName,                      // 目标文件名
    messageId: currentFile.value.messageId,                    // 关联的消息 ID
    sendTime: currentFile.value.sendTime                       // 消息发送时间，用于定位资源
  })
}

/**
 * 组件挂载逻辑
 */
onMounted(() => {
  // 注册全局滚轮事件，用于缩放图片
  window.addEventListener('wheel', onWheel)

  // 从路由查询参数中获取媒体列表和当前选中的消息 ID
  const { mediaList, currentMessageId } = route.query
  console.log(mediaList, currentMessageId)

  // 解析通过 URL 传递的加密/编码后的媒体列表 JSON 字符串
  allFileList.value = JSON.parse(decodeURIComponent(mediaList))

  // 根据传入的 ID 自动定位到当前点击的那张媒体文件的索引
  currentIndex.value = allFileList.value.findIndex((item) => item.messageId === currentMessageId)

  // 初始化加载当前文件
  getCurrentFile()
})

/**
 * 组件卸载逻辑
 */
onUnmounted(() => {
  // 必须移除全局监听，防止内存泄漏和逻辑干扰
  window.removeEventListener('wheel', onWheel)
})
</script>

<style lang="scss" scoped>
.media-window {
  padding: 0px;
  background: #fff;
  position: relative;
  overflow: hidden;

  // 顶栏：包含窗口拖拽逻辑及功能按钮
  .header {
    height: 30px;
    -webkit-app-region: drag; // 允许拖拽窗口
    display: flex;

    .media-op {
      -webkit-app-region: no-drag; // 按钮区域禁止拖拽
      height: 100%;
      line-height: 30px;
      display: flex;
      align-items: center;

      .iconfont {
        font-size: 18px;
        padding: 0px 10px;

        &:hover {
          background: #f3f3f3;
          cursor: pointer;
        }
      }

      // 禁用状态样式
      .not-allow {
        cursor: not-allowed;
        color: #ddd;
        text-decoration: none;

        &:hover {
          color: #ddd;
          cursor: not-allowed;
          background: none;
        }
      }
    }
  }

  // 媒体内容展示面板
  .media-panel {
    height: calc(100vh - 32px); // 减去 header 高度
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    // 深度作用选择器：修改图片查看器组件的背景色
    :deep(.viewer-backdrop) {
      background: #f5f5f5;
    }
  }

  // 文件信息面板
  .file-panel {
    .file-item {
      margin-top: 5px;
    }

    .download {
      margin-top: 20px;
      text-align: center;
    }
  }
}
</style>
