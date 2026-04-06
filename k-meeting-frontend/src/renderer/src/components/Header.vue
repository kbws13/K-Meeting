<template>
  <div class="header" :style="{ 'border-bottom': showBottomBorder ? '1px solid #ddd' : 'none'}">
    <div class="title" v-if="title">{{ title }}</div>
    <Titlebar :closeType="closeType" :showMax="showMax"></Titlebar>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

interface Props {
  title: string,
  showMax?: boolean;
  showBottomBorder: boolean;
  /** 0: 关闭, 1: 隐藏 */
  closeType?: number;
}

const props = withDefaults(defineProps<Props>(), {
  showMax: true,
  closeType: 1,
  showBottomBorder: false
});

</script>



<style scoped lang="scss">
.header {
  height: 30px;
  -webkit-app-region: drag;
  text-align: center;
  padding-top: 5px;
}
</style>
