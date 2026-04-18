<template>
  <div class="meeting-manage-page">
    <PageTable
      ref="pageTableRef"
      :load-data="loadMeetingList"
      row-key="id"
      default-sort-field="createTime"
      default-sort-order="desc"
    >
      <template #toolbar>
        <div class="toolbar">
          <div class="toolbar-copy">
            <div class="toolbar-title">会议管理</div>
          </div>
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

        <el-table-column label="会议号" min-width="150" align="center">
          <template #default="{ row }">
            {{ getMeetingNoText(row) }}
          </template>
        </el-table-column>

        <el-table-column label="会议名称" min-width="220">
          <template #default="{ row }">
            <div class="meeting-name">{{ getTextValue(row.name) || '--' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="创建人 ID" width="120" align="center">
          <template #default="{ row }">
            {{ getNumberText(row.createUserId) }}
          </template>
        </el-table-column>

        <el-table-column label="加入方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :class="['join-tag', getJoinTypeValue(row) === 1 ? 'password' : 'open']"
              effect="plain"
            >
              {{ getJoinTypeLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="成员数" width="90" align="center">
          <template #default="{ row }">
            {{ getNumberText(row.memberCount) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :class="['status-tag', isMeetingFinished(row) ? 'finished' : 'pending']"
              effect="plain"
            >
              {{ getMeetingStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="开始时间" min-width="168">
          <template #default="{ row }">
            {{ formatNullableTime(row.startTime) }}
          </template>
        </el-table-column>

        <el-table-column label="结束时间" min-width="168">
          <template #default="{ row }">
            {{ formatNullableTime(row.endTime) }}
          </template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="168">
          <template #default="{ row }">
            {{ formatNullableTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="130" align="center">
          <template #default="{ row }">
            <span v-if="isMeetingFinished(row)" class="action-disabled-text">会议已结束</span>
            <el-button v-else link type="danger" @click="finishMeeting(row, reload)">
              结束会议
            </el-button>
          </template>
        </el-table-column>
      </template>
    </PageTable>
  </div>
</template>

<script setup lang="ts">
import type { PageInfo, PageLoader, PageResult } from '@model/common'
import { ref } from 'vue'
import PageTable from '@/components/PageTable.vue'
import { useAppProxy } from '@/composables/useAppProxy'

interface AdminMeeting {
  id?: number
  meetingNo?: string
  name?: string
  createUserId?: number
  joinType?: number
  memberCount?: number
  status?: number
  startTime?: string | number | Date
  endTime?: string | number | Date
  createTime?: string | number | Date
}

const proxy = useAppProxy()
const pageTableRef = ref<InstanceType<typeof PageTable> | null>(null)

const loadMeetingList: PageLoader<AdminMeeting> = async (params) => {
  const result = await proxy.Request<PageResult<AdminMeeting>>({
    url: proxy.Api.loadAdminMeeting,
    dataType: 'json',
    params
  })
  return result?.data
}

const getTextValue = (value: unknown): string => {
  return typeof value === 'string' ? value : ''
}

const getJoinTypeValue = (row: AdminMeeting): number => {
  return typeof row.joinType === 'number' ? row.joinType : 0
}

const getMeetingNoText = (row: AdminMeeting): string => {
  const meetingNo = getTextValue(row.meetingNo)
  return meetingNo.length === 10 ? proxy.Utils.formatMeetingNo(meetingNo) : meetingNo || '--'
}

const getNumberText = (value: unknown): string => {
  return typeof value === 'number' ? String(value) : '--'
}

const getJoinTypeLabel = (row: AdminMeeting): string => {
  return getJoinTypeValue(row) === 1 ? '密码加入' : '无需密码'
}

const isMeetingFinished = (row: AdminMeeting): boolean => {
  return row.status === 1
}

const getMeetingStatusLabel = (row: AdminMeeting): string => {
  return isMeetingFinished(row) ? '已结束' : '进行中'
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

const finishMeeting = (row: AdminMeeting, reload: () => Promise<void>): void => {
  if (typeof row.id !== 'number') {
    proxy.Message.warning('会议 ID 无效')
    return
  }

  proxy.Confirm({
    message: `确定结束会议【${getTextValue(row.name) || getMeetingNoText(row)}】吗？`,
    okText: '立即结束',
    okfun: async () => {
      const result = await proxy.Request<boolean>({
        url: proxy.Api.adminFinishMeeting,
        params: {
          meetingId: row.id
        }
      })

      if (!result) {
        return
      }

      proxy.Message.success('会议已结束')
      await reload()
    }
  })
}
</script>

<style scoped lang="scss">
.meeting-manage-page {
  height: 100%;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-copy {
  min-width: 260px;
  padding-top: 4px;
}

.toolbar-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
}

.meeting-name {
  font-weight: 600;
  color: #273449;
}

.action-disabled-text {
  font-size: 12px;
  color: #8b95a7;
}

:deep(.join-tag),
:deep(.status-tag) {
  border-radius: 999px;
  padding: 0 10px;
}

:deep(.join-tag.open) {
  color: #2563eb;
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.08);
}

:deep(.join-tag.password) {
  color: #b45309;
  border-color: rgba(180, 83, 9, 0.18);
  background: rgba(180, 83, 9, 0.08);
}

:deep(.status-tag.pending) {
  color: #1e8e5a;
  border-color: rgba(30, 142, 90, 0.18);
  background: rgba(30, 142, 90, 0.08);
}

:deep(.status-tag.finished) {
  color: #5f6c7b;
  border-color: rgba(95, 108, 123, 0.18);
  background: rgba(95, 108, 123, 0.08);
}
</style>
