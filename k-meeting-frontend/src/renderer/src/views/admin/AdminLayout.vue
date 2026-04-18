<template>
  <AppHeader title="管理后台" :show-bottom-border="true" />
  <div class="admin-layout">
    <aside class="left-side">
      <div class="side-header">
        <div class="title">后台管理</div>
      </div>

      <div
        v-for="item in leftMenus"
        :key="item.path"
        :class="['menu-item', route.path === item.path ? 'active' : '']"
        @click="jump(item)"
      >
        <div class="menu-name">{{ item.name }}</div>
      </div>
    </aside>

    <main class="right-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/Header.vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

interface AdminMenuItem {
  name: string
  path: string
}

const leftMenus: AdminMenuItem[] = [
  {
    name: '用户管理',
    path: '/userList'
  },
  {
    name: '会议管理',
    path: '/meetingManage'
  },
  {
    name: '应用更新',
    path: '/appUpdateManage'
  },
  {
    name: '系统设置',
    path: '/systemSetting'
  }
]

const jump = (item: AdminMenuItem): void => {
  if (route.path === item.path) {
    return
  }
  void router.push(item.path)
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: calc(100vh - 31px);
  display: flex;
  gap: 16px;
  padding: 16px;
  background:
    radial-gradient(circle at top left, rgba(4, 113, 255, 0.08), transparent 28%),
    linear-gradient(180deg, #f7f9fc 0%, #f3f6fb 100%);
}

.left-side {
  width: 240px;
  padding: 20px 14px 14px;
  border: 1px solid #e6edf6;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.side-header {
  padding: 0 8px 18px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #223047;
}

.menu-item {
  margin-bottom: 10px;
  border: 1px solid transparent;
  border-radius: 16px;
  padding: 14px 16px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.menu-item:hover {
  transform: translateY(-1px);
  border-color: #dce7f6;
  background: #f7faff;
}

.menu-item.active {
  border-color: rgba(4, 113, 255, 0.12);
  background: linear-gradient(135deg, rgba(4, 113, 255, 0.12), rgba(4, 113, 255, 0.04));
}

.menu-item.active:hover {
  background: linear-gradient(135deg, rgba(4, 113, 255, 0.14), rgba(4, 113, 255, 0.05));
}

.menu-name {
  font-size: 15px;
  font-weight: 600;
  color: #273449;
}

.menu-item.active .menu-name {
  color: var(--blue);
}

.right-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
}
</style>
