<template>
  <div class="contact-apply-list">
    <div class="contact-item" v-for="item in contactApplyList">
      <Avatar :avatar="item.applyUserId"></Avatar>
      <div class="nick-name">{{ item.nickName }}</div>
      <div class="result-tip" v-if="item.status !== 0">{{ item.statusName }}</div>

      <el-dropdown v-if="item.status == 0">
        <el-button type="success" size="small">接受</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="dealWithApply(item.applyUserId, 1)">同意</el-dropdown-item>
            <el-dropdown-item @click="dealWithApply(item.applyUserId, 2)">拒绝</el-dropdown-item>
            <el-dropdown-item @click="dealWithApply(item.applyUserId, 3)">拉黑</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <NoData v-if="contactApplyList.length == 0" msg="暂无联系人申请"></NoData>
  </div>
</template>

<script setup lang="ts">
import type { PageResult } from '@model/common'
import { ref, watch } from 'vue'
import { useAppProxy } from '@/composables/useAppProxy'
import { useContactStore } from '../../stores/UserContactStore'
import { mitter } from '../../eventbus/eventBus'

const proxy = useAppProxy()
const contactStore = useContactStore()

interface ContactApply {
  id: number
  applyUserId: number
  receiveUserId: number
  status: number
  statusName?: string
  lastApplyTime: string
  applyContactUserId: string
  receiveContactUserId: string
  email: string
  nickName: string
  sex: number | null
  userStatus: number
  meetingNo: string
  lastLoginTime: string
  lastOffTime: string
}

const contactApplyList = ref<ContactApply[]>([])
/* 加载联系人申请列表 */
const loadContactApplyList = async (): Promise<void> => {
  const result = await proxy.Request<PageResult<ContactApply>>({
    url: proxy.Api.loadContactApply,
    dataType: 'json',
    params: {}
  })
  if (!result) {
    return
  }
  // API 返回的是分页对象，真正的列表在 records 属性中
  contactApplyList.value = result.data.records
}
loadContactApplyList()

const dealWithApply = async (applyUserId: number | string, status: number): Promise<void> => {
  const result = await proxy.Request({
    url: proxy.Api.dealWithApply,
    params: {
      applyUserId,
      status
    }
  })
  if (!result) {
    return
  }
  mitter.emit('reloadContact')
  contactStore.updateLastUpdateTime()
}

watch(
  () => contactStore.lastUpdateTime,
  (newVal) => {
    if (!newVal) {
      return
    }
    loadContactApplyList()
  },
  { immediate: true, deep: true }
)
</script>

<style scoped lang="scss">
.contact-apply-list {
  height: calc(100vh - 65px);
  overflow: auto;

  .contact-item {
    padding-top: 5px;
    display: flex;
    align-items: center;

    .nick-name {
      flex: 1;
      width: 0;
      margin: 0px 5px;
      font-size: 14px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
    }

    .result-tip {
      font-size: 12px;
      color: #5b5b5b;
    }
  }
}

.el-tooltip__trigger:focus-visible {
  outline: unset;
}
</style>
