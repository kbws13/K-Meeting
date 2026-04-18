<template>
  <div
    ref="coverRef"
    class="image-panel"
    :style="{
      'border-radius': borderRadius,
      width: width ? width + 'px' : '100%',
      height: scale ? width * scale + 'px' : 'auto'
    }"
  >
    <el-image
      :lazy="lazy"
      :src="fileSource || fileImage"
      :fit="fit"
      preview-teleported
      :preview-src-list="imageListResult"
      :initial-index="initialIndex"
    >
      <template #error>
        <img :src="errorImage" class="el-image__inner" :style="errorImageStyle" alt="404" />
      </template>
    </el-image>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type CSSProperties } from 'vue'
import errorImage from '../assets/images/404.png'

// 1. 定义 Props 接口
interface Props {
  source?: string | File // 支持字符串（URL）或文件对象
  width?: number // 宽度
  fit?: string // 明确 fit 的可选值
  preview?: boolean // 是否开启预览
  borderRadius?: string // 圆角，默认 '5px'
  lazy?: boolean // 是否懒加载，默认 true
  scale?: number // 缩放比例
  initialIndex?: number // 预览初始索引，默认 0
  imageList?: string[] // 补全这一行，通常图片列表是字符串数组
}

// 2. 使用 withDefaults 定义默认值
const props = withDefaults(defineProps<Props>(), {
  fit: 'scale-down',
  preview: false,
  borderRadius: '5px',
  lazy: true,
  initialIndex: 0,
  imageList: () => []
})

const fileImage = ref<string | ArrayBuffer | null>(null)
const errorImageStyle = computed<CSSProperties>(() => ({
  objectFit: props.fit
}))

const fileSource = computed(() => {
  if (!props.source) {
    fileImage.value = null
    return null
  }

  // 处理 File 对象（本地上传预览）
  if (props.source instanceof File) {
    const img = new FileReader()
    img.readAsDataURL(props.source)
    img.onload = ({ target }) => {
      // 将读取到的 Base64 结果赋值给 fileImage
      fileImage.value = target?.result ?? null
    }
    return null // File 对象时，实际渲染依赖 fileImage 变量
  }

  // 处理 字符串（网络链接）
  if (typeof props.source === 'string') {
    return props.source
  }

  return null
})

const imageListResult = computed<string[]>(() => {
  if (!props.preview) {
    return []
  }

  if (props.imageList.length > 0) {
    return props.imageList
  }

  const currentImage = typeof fileSource.value === 'string' ? fileSource.value : fileImage.value
  return currentImage ? [String(currentImage)] : []
})
</script>

<style scoped lang="scss">
.image-panel {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;

  :deep(.el-image) {
    width: 100%;
    height: 100%;
  }

  :deep(.is-loading) {
    display: none;
  }

  :deep(.el-image__wrapper) {
    position: relative;
    vertical-align: top;
    width: 100%;
    height: 100%;
    display: flex;
  }

  .icon-image-error {
    margin: 0 auto;
    font-size: 20px;
    color: #838383;
    height: 100%;
  }

  .loading {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      width: 200px; // 此处根据图片视觉推测，代码中为 20px
    }
  }

  .no-image {
    text-align: center;
    color: #9f9f9f;
  }
}
</style>
