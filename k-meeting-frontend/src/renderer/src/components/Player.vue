<template>
  <div class="player-panel">
    <div id="player" ref="playerRef" class="player-style"></div>

    <div id="play">
      <img src="../assets/images/play.png" />
    </div>
  </div>
</template>

<script setup lang="ts">
import Artplayer from 'artplayer'
import { ref } from 'vue'

const playerRef = ref()
let player = null

/**
 * 初始化播放器
 * @param {string} url - 视频资源地址
 */
const initPlayer = (url) => {
  // 全局配置：隐藏右键菜单、设置自动回放记录阈值、禁用双击全屏
  Artplayer.CONTEXTMENU = false
  Artplayer.AUTO_PLAYBACK_MAX = 20
  Artplayer.AUTO_PLAYBACK_MIN = 10
  Artplayer.DBCLICK_FULLSCREEN = false

  // 实例化播放器
  player = new Artplayer({
    container: playerRef.value,
    url,
    theme: '#23ade5',            // 播放器主题色
    volume: 0.7,                 // 默认音量
    autoplay: true,              // 是否自动播放
    autoMini: true,              // 滚出视口后进入迷你模式
    fullscreen: false,           // 窗口全屏
    fullscreenWeb: false,        // 网页全屏
    setting: false,              // 设置面板
    pip: false,                  // 画中画
    playbackRate: false,         // 播放速度
    flip: false,                 // 视频翻转
    aspectRatio: true,           // 比例控制
    screenshot: false,           // 截图功能
    autoPlayback: false,         // 自动回放

    // 自定义图标配置
    icons: {
      // loading: '<img src="/assets/img/ploading.gif">',
      state: document.querySelector('#play') // 使用自定义 DOM 元素作为播放状态图标
    }
  })

  // 监听 hover 事件，动态控制底部控制栏的显示/隐藏
  player.on('hover', (state) => {
    let display = 'none'
    if (state) {
      display = 'flex'
    }
    // 直接操作播放器实例内部模板 DOM
    player.template.$bottom.style.display = display
  })
}

/**
 * 显示或切换视频播放
 * @param {string} url - 视频资源地址
 */
const showPlayer = (url) => {
  // 如果播放器实例已存在，直接切换视频源，避免重复初始化
  if (player) {
    player.switch = url
    return
  }
  // 否则，执行初始化
  initPlayer(url)
}

/**
 * 销毁播放器实例
 */
const destroyPlayer = () => {
  if (player) {
    // 调用 Artplayer 的销毁方法
    player.destroy(false)
  }
}

// 将方法暴露给父组件，实现父子通信
defineExpose({
  showPlayer,
  destroyPlayer
})
</script>

<style lang="scss" scoped>
.player-panel {
  width: 100%;
  height: 100%;

  .player-style {
    width: 100%;
    height: 100%;

    // 使用 :deep 穿透组件样式，覆盖播放器内部状态图标的定位
    :deep(.art-video-player.art-mask-show .art-state) {
      // 播放按钮容器定位
      position: absolute;
      right: 40px;
      bottom: 70px;

      // 内部图标样式
      .art-icon-state {
        width: 60px;
        height: 60px;

        img {
          width: 100%;
        }
      }
    }
  }
}
</style>
