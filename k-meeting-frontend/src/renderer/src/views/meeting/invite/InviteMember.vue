<template>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="660px"
    @close="dialogConfig.show = false"
  >
    <el-form :model="formData" ref="formDataRef" label-width="0px" @submit.prevent>
      <el-form-item label="" prop="">
        <el-transfer
          v-model="formData.selectContactIds"
          :titles="['全部', '已选']"
          :format="{
            noChecked: '${total}',
            hasChecked: '${checked}/${total}'
          }"
          :data="contactList"
          :props="{
            key: 'contactId',
            label: 'nickName'
          }"
          filterable
          :filter-method="search"
        >
          <template #default="{ option }">
            <div class="nick-name">{{ option.nickName }}</div>
          </template>
        </el-transfer>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

import { useMeetingStore } from '@/stores/MeetingStore'
const meetingStore = useMeetingStore()

// 1. 弹窗配置
const dialogConfig = ref({
  show: false,
  title: '选择联系人',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: (e) => {
        submitData()
      }
    }
  ]
})

/**
 * 穿梭框搜索过滤函数
 */
const search = (query, item) => {
  return item.nickName.toLowerCase().includes(query.toLowerCase())
}

// 2. 联系人数据处理
const contactList = ref([])

/**
 * 加载并处理联系人列表
 */
const loadContactList = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadContactUser,
    dataType: 'json',
    params: {}
  })

  if (!result) {
    return
  }

  // 获取当前已经在会议中的成员 ID 集合
  const inMeetingMembers = meetingStore.memberList.map((item) => {
    return item.userId
  })

  // 格式化列表：添加状态后缀并设置禁用项
  contactList.value = result.data.records.map((item) => {
    if (inMeetingMembers.includes(item.contactId)) {
      // 已在会议中：禁用选择并标注
      item.disabled = true
      item.nickName = item.nickName + '(已入会)'
    } else if (item.onlineType == 1) {
      // 在线：可选择并标注
      item.nickName = item.nickName + '(在线)'
    } else if (item.onlineType == 0) {
      // 离线：禁用选择并标注
      item.disabled = true
      item.nickName = item.nickName + '(离线)'
    }
    return item
  })
}

// 3. 表单数据与弹窗控制
const formData = ref({
  selectContactIds: []
});

/**
 * 暴露给外部调用的显示方法
 */
const show = () => {
  loadContactList();
  dialogConfig.value.show = true;
  formData.value = {
    selectContactIds: []
  }
}

defineExpose({
  show
})

/**
 * 提交邀请数据
 */
const submitData = async () => {
  // 校验：必须选择至少一个联系人
  if (formData.value.selectContactIds.length == 0) {
    proxy.Message.warning('请选择联系人')
    return
  }

  let params = {
    ...formData.value,
    selectContactIds: formData.value.selectContactIds.join(',')
  }

  let result = await proxy.Request({
    url: proxy.Api.inviteMember,
    params
  })

  if (!result) {
    return
  }

  // 成功处理：关闭弹窗并提示
  dialogConfig.value.show = false
  proxy.Message.success('发送邀请成功')
}
</script>

<style lang="scss" scoped>
.el-transfer {
  width: 100%;
  display: block !important;
  display: flex;

  // 穿梭框面板样式定制
  :deep(.el-transfer-panel) {
    width: 280px;
  }

  // 列表项布局定制
  :deep(.el-transfer-panel__item) {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 5px;
  }
}

// 穿梭操作按钮区域定制
:deep(.el-transfer__buttons) {
  width: 60px;
  flex-direction: column; // 按钮纵向排列
  text-align: center;
  padding: 0px;

  .el-transfer__button {
    text-align: center;
    margin-left: 0px;
    margin-right: 0px;
    margin-top: 5px;
    padding: 10px;
    height: 36px;
    border-radius: 50%; // 按钮设为圆形
  }
}

// 自定义列表项内部布局
.select-item {
  display: flex;

  .avatar {
    width: 30px;
    height: 30px;
  }

  .nick-name {
    flex: 1;
    margin-left: 5px;
  }
}
</style>
