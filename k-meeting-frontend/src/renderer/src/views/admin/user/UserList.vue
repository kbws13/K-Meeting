<template>
  <div class="user-list-page">
    <PageTable
      ref="pageTableRef"
      :load-data="loadUserList"
      :query-params="queryForm"
      row-key="userId"
      default-sort-field="createTime"
      default-sort-order="desc"
    >
      <template #toolbar>
        <div class="toolbar">
          <div class="toolbar-copy">
            <div class="toolbar-title">用户列表</div>
          </div>

          <el-form :model="queryForm" inline class="search-form" @submit.prevent>
            <el-form-item label="邮箱">
              <el-input
                v-model.trim="queryForm.email"
                clearable
                placeholder="请输入邮箱"
                @keyup.enter="search"
              />
            </el-form-item>

            <el-form-item label="昵称">
              <el-input
                v-model.trim="queryForm.nickName"
                clearable
                placeholder="请输入昵称"
                @keyup.enter="search"
              />
            </el-form-item>

            <el-form-item label="状态">
              <el-select v-model="queryForm.status" clearable placeholder="全部状态">
                <el-option
                  v-for="item in userStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="角色">
              <el-select v-model="queryForm.userRole" clearable placeholder="全部角色">
                <el-option
                  v-for="item in userRoleOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item class="action-item">
              <el-button type="primary" @click="search">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <template #default="{ pageInfo, reload }">
        <el-table-column
          label="序号"
          width="72"
          align="center"
          :index="(index) => getRowIndex(index, pageInfo)"
          type="index"
        />

        <el-table-column prop="userId" column-key="id" label="UID" width="130" sortable="custom">
          <template #default="{ row }">
            {{ getDisplayUserId(row) || '--' }}
          </template>
        </el-table-column>

        <el-table-column label="用户信息" min-width="240">
          <template #default="{ row }">
            <div class="user-info">
              <div class="user-badge">
                {{ getUserInitial(getNickName(row), getEmail(row)) }}
              </div>
              <div class="user-meta">
                <div class="nick-name">{{ getNickName(row) || '--' }}</div>
                <div class="email">{{ getEmail(row) || '--' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="meetingNo" label="个人会议号" min-width="140">
          <template #default="{ row }">
            {{ getMeetingNo(row) || '--' }}
          </template>
        </el-table-column>

        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :class="['role-tag', getRoleValue(row) === 'admin' ? 'admin' : 'user']"
              effect="plain"
            >
              {{ getRoleLabel(getRoleValue(row)) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="status"
          column-key="status"
          label="状态"
          width="110"
          align="center"
          sortable="custom"
        >
          <template #default="{ row }">
            <el-tag
              :class="['status-tag', getStatusValue(row) === 1 ? 'enabled' : 'disabled']"
              effect="plain"
            >
              {{ getStatusLabel(getStatusValue(row)) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="lastLoginTime"
          column-key="lastLoginTime"
          label="最后登录"
          min-width="168"
          sortable="custom"
        >
          <template #default="{ row }">
            {{ formatNullableTime(row.lastLoginTime) }}
          </template>
        </el-table-column>

        <el-table-column
          prop="lastOffTime"
          column-key="lastOffTime"
          label="最后离线"
          min-width="168"
          sortable="custom"
        >
          <template #default="{ row }">
            {{ formatNullableTime(row.lastOffTime) }}
          </template>
        </el-table-column>

        <el-table-column
          prop="createTime"
          column-key="createTime"
          label="创建时间"
          min-width="168"
          sortable="custom"
        >
          <template #default="{ row }">
            {{ formatNullableTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="{ row }">
            <template v-if="getActionBlockReason(row)">
              <div class="table-actions">
                <span class="action-disabled-text">{{ getActionBlockReason(row) }}</span>
              </div>
            </template>
            <template v-else>
              <div class="table-actions">
                <el-button link type="primary" @click="toggleUserStatus(row, reload)">
                  {{ getStatusValue(row) === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  :disabled="getStatusValue(row) !== 1"
                  @click="forceOffline(row, reload)"
                >
                  强制下线
                </el-button>
              </div>
            </template>
          </template>
        </el-table-column>
      </template>
    </PageTable>
  </div>
</template>

<script setup lang="ts">
import type { PageInfo, PageLoader, PageResult } from '@model/common'
import type { AppUserInfo } from '@model/user'
import { ref } from 'vue'
import PageTable from '@/components/PageTable.vue'
import { useAppProxy } from '@/composables/useAppProxy'
import { useUserInfoStore } from '@/stores/UserInfoStore'

interface UserListQuery {
  email: string
  nickName: string
  status?: number | ''
  userRole?: string
}

const proxy = useAppProxy()
const userInfoStore = useUserInfoStore()

const createDefaultQuery = (): UserListQuery => ({
  email: '',
  nickName: '',
  status: '',
  userRole: ''
})

const queryForm = ref<UserListQuery>(createDefaultQuery())
const pageTableRef = ref<InstanceType<typeof PageTable> | null>(null)

const userStatusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const userRoleOptions = [
  { label: '管理员', value: 'admin' },
  { label: '普通用户', value: 'user' }
]

const loadUserList: PageLoader<AppUserInfo> = async (params) => {
  const result = await proxy.Request<PageResult<AppUserInfo>>({
    url: proxy.Api.loadUserList,
    dataType: 'json',
    params
  })
  return result?.data
}

const search = (): void => {
  void pageTableRef.value?.search()
}

const resetQuery = (): void => {
  Object.assign(queryForm.value, createDefaultQuery())
  void pageTableRef.value?.reset()
}

const getRoleLabel = (role?: string): string => {
  return role === 'admin' ? '管理员' : '普通用户'
}

const getTextValue = (value: unknown): string => {
  return typeof value === 'string' ? value : ''
}

const getNumberValue = (value: unknown): number | null => {
  return typeof value === 'number' ? value : null
}

const getStatusValue = (row: AppUserInfo): number => {
  return getNumberValue(row.status) ?? 0
}

const getRoleValue = (row: AppUserInfo): string => {
  return getTextValue(row.userRole)
}

const getDisplayUserId = (row: AppUserInfo): string => {
  const userId = row.userId
  return typeof userId === 'string' || typeof userId === 'number' ? String(userId) : ''
}

const getCurrentUserDisplayId = (): string => {
  const currentUserId = userInfoStore.userInfo.id
  return typeof currentUserId === 'string' || typeof currentUserId === 'number'
    ? String(currentUserId)
    : ''
}

const getNickName = (row: AppUserInfo): string => {
  return getTextValue(row.nickName)
}

const getEmail = (row: AppUserInfo): string => {
  return getTextValue(row.email)
}

const getMeetingNo = (row: AppUserInfo): string => {
  return getTextValue(row.meetingNo)
}

const getStatusLabel = (status?: number | null): string => {
  return status === 1 ? '启用' : '禁用'
}

const getUserInitial = (nickName?: string, email?: string): string => {
  const text = (nickName || email || 'U').trim()
  return text.charAt(0).toUpperCase()
}

const formatNullableTime = (value?: unknown): string => {
  if (!value) {
    return '--'
  }
  if (typeof value === 'string' || typeof value === 'number' || value instanceof Date) {
    return proxy.Utils.formatDate2(value, 'YYYY-MM-DD HH:mm')
  }
  return '--'
}

const getRowIndex = (index: number, pageInfo: PageInfo): number => {
  return (pageInfo.current - 1) * pageInfo.pageSize + index + 1
}

const isAdminUser = (row: AppUserInfo): boolean => {
  return getRoleValue(row) === 'admin'
}

const isCurrentUser = (row: AppUserInfo): boolean => {
  const currentUserId = getCurrentUserDisplayId()
  if (currentUserId !== '' && getDisplayUserId(row) === currentUserId) {
    return true
  }
  const currentEmail = getTextValue(userInfoStore.userInfo.email)
  return currentEmail !== '' && getEmail(row) === currentEmail
}

const getActionBlockReason = (row: AppUserInfo): string => {
  if (isCurrentUser(row)) {
    return '当前账号不可操作'
  }
  if (isAdminUser(row)) {
    return '管理员账号不可操作'
  }
  return ''
}

const ensureOperable = (row: AppUserInfo): boolean => {
  const reason = getActionBlockReason(row)
  if (!reason) {
    return true
  }
  proxy.Message.warning(reason)
  return false
}

const toggleUserStatus = (row: AppUserInfo, reload: () => Promise<void>): void => {
  if (!ensureOperable(row)) {
    return
  }
  const nextStatus = getStatusValue(row) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  const userName = getNickName(row) || getEmail(row)

  proxy.Confirm({
    message: `确定${actionText}用户【${userName}】吗？`,
    okfun: async () => {
      const result = await proxy.Request<boolean>({
        url: proxy.Api.updateUserStatus,
        params: {
          userId: getDisplayUserId(row),
          status: nextStatus
        }
      })

      if (!result) {
        return
      }

      proxy.Message.success(`${actionText}成功`)
      await reload()
    }
  })
}

const forceOffline = (row: AppUserInfo, reload: () => Promise<void>): void => {
  if (!ensureOperable(row)) {
    return
  }
  const userName = getNickName(row) || getEmail(row)

  proxy.Confirm({
    message: `确定强制下线用户【${userName}】吗？`,
    okText: '立即下线',
    okfun: async () => {
      const result = await proxy.Request<boolean>({
        url: proxy.Api.forceOffLine,
        params: {
          userId: getDisplayUserId(row)
        }
      })

      if (!result) {
        return
      }

      proxy.Message.success('操作成功')
      await reload()
    }
  })
}
</script>

<style scoped lang="scss">
.user-list-page {
  height: 100%;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-copy {
  min-width: 220px;
  padding-top: 4px;
}

.toolbar-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
}

.toolbar-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #7b8798;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 4px 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-badge {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(4, 113, 255, 0.16), rgba(4, 113, 255, 0.08));
  color: var(--blue);
  font-size: 15px;
  font-weight: 700;
}

.user-meta {
  min-width: 0;
}

.nick-name {
  color: #273449;
  font-weight: 600;
}

.email {
  margin-top: 4px;
  font-size: 12px;
  color: #8792a4;
  word-break: break-all;
}

.table-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.action-disabled-text {
  color: #8b95a7;
  font-size: 12px;
  line-height: 1.5;
}

:deep(.search-form .el-form-item) {
  margin-bottom: 0;
}

:deep(.search-form .el-input),
:deep(.search-form .el-select) {
  width: 180px;
}

:deep(.search-form .action-item .el-form-item__content) {
  gap: 8px;
}

:deep(.role-tag),
:deep(.status-tag) {
  border-radius: 999px;
  padding: 0 10px;
}

:deep(.role-tag.admin) {
  color: #1f5eff;
  border-color: rgba(31, 94, 255, 0.18);
  background: rgba(31, 94, 255, 0.08);
}

:deep(.role-tag.user) {
  color: #5f6c7b;
  border-color: rgba(95, 108, 123, 0.14);
  background: rgba(95, 108, 123, 0.06);
}

:deep(.status-tag.enabled) {
  color: #1e8e5a;
  border-color: rgba(30, 142, 90, 0.18);
  background: rgba(30, 142, 90, 0.08);
}

:deep(.status-tag.disabled) {
  color: #d03050;
  border-color: rgba(208, 48, 80, 0.18);
  background: rgba(208, 48, 80, 0.08);
}
</style>
