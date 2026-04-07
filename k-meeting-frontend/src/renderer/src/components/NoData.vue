<template>
  <div class="no-data">
    <div class="iconfont icon-coffe"></div>
    <div class="msg">{{ msg }}</div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  msg?: string; // 声明 msg 为可选的 string 类型
}

const props = defineProps<Props>();
</script>

<style scoped lang="scss">
.no-data {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;

  .icon-coffe {
    color: #dadada;
    font-size: 50px;
  }

  .msg {
    margin-top: 10px;
    color: #909399;
    font-size: 14px;
  }
}
</style>
