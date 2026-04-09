<template>
  <div class="mic-panel">
    <div class="mic-show" :style="{ width: size + 'px', height: size + 'px' }">
      <div
        class="iconfont icon-mic-close"
        v-if="!micDeviceInfo.open || !micDeviceInfo.enable"
      ></div>
      <div class="iconfont icon-mic" v-else></div>

      <div class="volume" :style="{ height: volume * 1.5 + 'px' }"></div>
    </div>

    <div
      v-if="showLabel"
      :class="['mic-label', micDeviceInfo.open ? 'active' : '']"
    >
      {{ micDeviceInfo.label }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

// 1. 定义 Props 接口
interface Props {
  size?: number;             // 尺寸，默认 30
  modelValue?: object;       // 数据对象，默认 {}
  showLabel?: boolean;       // 是否显示标签，默认 true
  defaultOpen?: boolean;     // 默认是否开启，默认 true
}

// 2. 使用 withDefaults 定义默认值
const props = withDefaults(defineProps<Props>(), {
  size: 30,
  modelValue: () => ({}),    // 注意：对象默认值在 TS 中必须使用工厂函数
  showLabel: true,
  defaultOpen: true
});

const emit =defineEmits(['update:modelValue'])
const micDeviceInfo = ref({})
let stream = null

const getMicrophones = async () => {
  let devices = []
  try {
    // 获取所有媒体设备
    devices = await navigator.mediaDevices.enumerateDevices()
    console.log(devices)
  } finally {
    // 查找默认设备
    // 注意：此处原图中查找的是 'videoinput'，但函数名是 getMicrophones，
    // 通常麦克风应该是 'audioinput'。请根据实际业务需求确认。
    let defaultMic = devices.find((device) => {
      return (device.kind === 'audioinput' && device.deviceId === 'default')
    })

    if (!defaultMic) {
      micDeviceInfo.value = {
        deviceId: '0',
        label: '未获取到麦克风',
        open: false,
        enable: false // 原图此处手误拼写为 fasle
      }
      emit('update:modelValue', micDeviceInfo.value)
      return
    }

    const label = getDevicesLabel(defaultMic)
    const constraints = {
      audio: {
        deviceId: defaultMic.deviceId ? { exact: defaultMic.deviceId } : undefined
      },
      video: false
    }

    stream = await navigator.mediaDevices.getUserMedia(constraints).catch((error) => {
      // 捕获权限拒绝或设备占用等错误
      return null
    })

    micDeviceInfo.value = {
      deviceId: defaultMic.deviceId,
      label,
      open: props.defaultOpen,
      enable: stream !== null
    }

    emit('update:modelValue', micDeviceInfo.value)

    if (!micDeviceInfo.value.enable) {
      return
    }

    if (micDeviceInfo.value.enable && props.defaultOpen) {
      showAnimation()
    }
  }
}

/**
 * 格式化设备标签，去除冗余字符
 */
const getDevicesLabel = (device) => {
  let label = device.label

  // 去除 "Default - " 或 "Communications - " 前缀
  label = label.replace(/^(Default|Communications)\s*-\s*/i, '')

  // 去除末尾括号中的硬件 ID 信息 (例如: (045e:0779))
  label = label.replace(/\s*\(([0-9a-fA-F]+:[0-9a-fA-F]+)\)$/, '')

  // 匹配并获取核心设备名称
  const matches = label.match(/^([^()]+\([^()]+\))/)
  if (matches) {
    label = matches[0]
  }

  return label
}

onMounted(() => {
  getMicrophones()
})

let analyser
let microphone
const showAnimation = () => {
  if (!stream) {
    return // 原图中为 retrun，此处建议修正
  }
  const audioContext = new (window.AudioContext || window.webkitAudioContext)()
  analyser = audioContext.createAnalyser()
  analyser.fftSize = 2048
  microphone = audioContext.createMediaStreamSource(stream)
  microphone.connect(analyser)
  animate()
}

const animate = () => {
  const bufferLength = analyser.frequencyBinCount
  const dataArray = new Uint8Array(bufferLength)
  analyser.getByteTimeDomainData(dataArray)
  analyser.getByteFrequencyData(dataArray)
  calculateVolume(dataArray)
  requestAnimationFrame(() => {
    animate()
  })
}

const stopAnimation = () => {
  if (microphone && analyser) {
    microphone.disconnect(analyser)
  }
}

const volume = ref(0)
const calculateVolume = (dataArray) => {
  let sum = 0
  for (let i = 0; i < dataArray.length; i++) {
    sum += dataArray[i]
  }
  const average = sum / dataArray.length
  volume.value = Math.min(100, Math.round((average / 255) * 100))
}

const toggleMic = () => {
  if (!micDeviceInfo.value.enable) {
    return
  }
  micDeviceInfo.value.open = !micDeviceInfo.value.open
  emit('update:modelValue', micDeviceInfo.value)
  if (micDeviceInfo.value.open) {
    showAnimation()
  } else {
    stopAnimation();
  }
}

defineExpose({
  toggleMic
})
</script>

<style scoped lang="scss">
.mic-panel {
  display: flex;
  align-items: center;

  .mic-show {
    background: #ddd;
    border-radius: 5px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
    cursor: pointer;

    .icon-mic {
      color: var(--blue);
    }

    .icon-mic-close {
      color: #5b5b5b;
    }

    .volume {
      position: absolute;
      left: 0px;
      right: 0px;
      bottom: 0px;
      background: rgb(4, 91, 241, 0.3);
    }
  }

  .mic-label {
    margin-left: 5px;
    font-size: 14px;
    color: #8b8b8b;
  }

  .active {
    color: #494949;
  }
}
</style>
