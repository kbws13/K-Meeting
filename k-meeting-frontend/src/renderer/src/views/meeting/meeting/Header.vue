<template>
  <div class="header">
    <div class="left">会议详情</div>
    <div class="right">
      <el-popover placement="top-end" :width="260" trigger="click" ref="layoutPopoverRef">
        <template #reference>
          <div class="layout-btn">
            <div :class="['cur-layout-icon iconfont', 'icon-' + currentLayout.icon]"></div>
            <div class="text">切换布局</div>
            <div class="iconfont icon-narrow-down"></div>
          </div>
        </template>

        <div class="layout">
          <div class="layout-item" v-for="item in layoutList" @click="selectLayout(item)">
            <div
              :class="[
                'iconfont',
                'icon-' + item.icon,
                currentLayout.type == item.type ? 'active' : ''
              ]"
            ></div>
            <div class="name">{{ item.name }}</div>
          </div>
        </div>
      </el-popover>

      <el-divider direction="vertical" />
    </div>
  </div>

  <slot></slot>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const layoutList = ref([
  {
    type: 0,
    name: '宫格布局',
    icon: 'layout-grid'
  },
  {
    type: 1,
    name: '顶部缩略图',
    icon: 'layout-top'
  },
  {
    type: 2,
    name: '侧边缩略图',
    icon: 'layout-right'
  }
])

const currentLayout = ref(layoutList.value[0])
const layoutPopoverRef = ref()

/**
 * 切换布局
 * @param {Object} item 选中的布局对象
 */
const selectLayout = (item) => {
  currentLayout.value = item
  // 切换后自动隐藏弹出框
  layoutPopoverRef.value.hide()
}
</script>

<style scoped lang="scss">
.header {
  height: 40px;
  -webkit-app-region: drag; // 窗口可拖拽区域
  border-bottom: 1px solid #ddd;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;

  .left {
    padding-left: 20px;
  }

  .right {
    margin-right: 100px;
    display: flex;
    align-items: center;

    .layout-btn {
      -webkit-app-region: no-drag; // 按钮区域禁止拖拽，确保点击生效
      cursor: pointer;
      padding: 3px 8px;
      border-radius: 5px;
      display: flex;
      align-items: center;
      font-size: 14px;

      .cur-layout-icon {
        font-weight: bold;
        color: var(--text2);
        font-size: 13px;
      }

      .text {
        margin: 0px 3px;
      }

      .icon-narrow-down {
        font-size: 10px;
      }

      &:hover {
        background: #ddd;
      }
    }
  }
}

.layout {
  display: grid;
  grid-template-columns: repeat(3, 1fr); // 三列等宽网格
  grid-gap: 20px;

  .layout-item {
    text-align: center;
    cursor: pointer;

    &:hover {
      .iconfont {
        &::before {
          border: 2px solid var(--blue);
          color: #d0ddfc;
        }
      }
    }

    .iconfont {
      font-size: 50px;
      border-radius: 5px;
      padding: 0px;

      &::before {
        border: 2px solid #fff;
        padding: 2px;
        border-radius: 5px;
        color: #b4b4b4;
      }
    }

    .name {
      font-size: 12px;
    }

    .active {
      &::before {
        border: 2px solid var(--blue);
        color: #d0ddfc;
      }
    }
  }
}
</style>
