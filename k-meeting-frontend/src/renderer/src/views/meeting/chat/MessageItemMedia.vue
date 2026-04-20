<template>
  <div class="media-panel" @click="showMediaHandler">
    <div class="uploading" v-if="data.status === 0">
      <img src="../../../assets/images/loading.gif" />
      <div class="info">正在上传中...</div>
    </div>

    <template v-else>
      <div class="play-btn" v-if="data.fileType == 1">
        <img src="../../../assets/images/play.png" />
      </div>

      <div class="media-cover">
        <img :src="proxy.Utils.getResourcePath({ ...data, thumbnail: true })" fit="cover" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, inject } from 'vue'

const { proxy } = getCurrentInstance()
const props = defineProps({
  data: {
    type: Object,
    default: {}
  }
})

const showMedia = inject('showMedia')
const showMediaHandler = () => {
  if (props.data.status == 0) {
    return
  }
  showMedia(props.data.messageId)
}
</script>

<style lang="scss" scoped>
.media-panel {
  margin-top: 3px;
  width: 200px;
  height: 130px;
  background: #ddd;
  border-radius: 5px;
  overflow: hidden;
  cursor: pointer;
  position: relative;

  // 上传中的遮罩层（半透明背景+进度提示）
  .uploading {
    width: 100%;
    height: 100%;
    position: absolute;
    left: 0px;
    top: 0px;
    background: rgb(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;

    img {
      width: 30px;
    }

    .info {
      font-size: 12px;
      color: #00a1d6;
    }
  }

  // 播放按钮（位于封面中心）
  .play-btn {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2;
    position: absolute;
    width: 100%;
    left: 0px;
    top: 0px;

    img {
      width: 30px;
      height: 30px;
    }
  }

  // 媒体封面图
  .media-cover {
    width: 100%;
    height: 100%;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover; // 确保图片按比例填充并裁剪，不失真
    }
  }
}
</style>
