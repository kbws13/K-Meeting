<template>
  <el-dialog
    :show-close="showClose"
    :draggable="draggable"
    :model-value="show"
    :close-on-click-modal="false"
    class="cust-dialog"
    :top="top + 'px'"
    :width="width"
    @close="close"
    @open="open"
  >
    <template #header="{ close, titleId, titleClass }">
      <div v-if="title" class="title">{{ title }}</div>
      <slot v-else name="header"></slot>
    </template>

    <div class="dialog-body" :style="{ 'max-height': maxHeight + 'px', padding: padding + 'px' }">
      <slot></slot>
    </div>

    <template v-if="(buttons && buttons.length > 0) || showCancel">
      <div class="dialog-footer">
        <el-button link @click="close" v-if="showCancel"> 取消 </el-button>
        <el-button
          v-for="btn in buttons"
          :type="btn.type || 'primary'"
          @click="btn.click"
        >
          {{ btn.text }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';

// 1. 定义按钮接口
interface DialogButton {
  text: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'text';
  click: () => void;
}

// 2. 定义 Props 接口
interface Props {
  draggable?: boolean;
  title?: string;
  show?: boolean;
  showClose?: boolean;
  showCancel?: boolean;
  top?: number;
  width?: string;
  buttons?: DialogButton[];
  padding?: number;
}

// 3. 使用 withDefaults 设置默认值
const props = withDefaults(defineProps<Props>(), {
  draggable: true,
  show: false,
  showClose: true,
  showCancel: true,
  top: 50,
  width: '30%',
  padding: 15,
  buttons: () => [] // 数组默认值需通过工厂函数返回
});

// 4. 定义 Emits
const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'open'): void;
}>();

// 5. 响应式计算最大高度
// 注意：window.innerHeight 是非响应式的，建议放入 ref 或监听 resize
const innerHeight = ref(window.innerHeight);
const updateHeight = () => {
  innerHeight.value = window.innerHeight;
};

onMounted(() => window.addEventListener('resize', updateHeight));
onUnmounted(() => window.removeEventListener('resize', updateHeight));

const maxHeight = computed(() => {
  const footerHeight = (!props.buttons || props.buttons.length === 0) ? 50 : 90;
  return innerHeight.value - props.top - footerHeight;
});

// 6. 定义方法
const close = (): void => {
  emit('close');
};

const open = (): void => {
  emit('open');
};
</script>

<style lang="scss">
.cust-dialog {
  padding: 0px !important;
  margin-bottom: 5px !important;
  -webkit-app-region: no-drag;

  .el-dialog__header {
    padding: 5px 0px 5px 10px;
    border-bottom: 1px solid #ddd;
    -webkit-app-region: no-drag;
  }

  .el-dialog__body {
    padding: 0px;
  }

  .title {
    font-size: 20px;
  }

  .dialog-body {
    min-height: 80px;
    overflow: auto;
    overflow-x: hidden;
  }

  .dialog-footer {
    border-top: 1px solid #ddd;
    text-align: right;
    padding: 5px 20px;
  }
}
</style>
