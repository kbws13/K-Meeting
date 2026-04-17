<template>
  <div class="member-panel">
    <div class="member-panel-title">
      <div class="iconfont icon-members">管理成员</div>
    </div>

    <div class="member-list" id="member-list" ref="memberListRef">
      <div class="member-item" v-for="item in meetingStore.allMemberList">
        <Avatar :avatar="item.userId"></Avatar>

        <div class="member-info">
          <div class="nick-name">
            {{ item.nickName }}{{ item.memberType === 1 ? '（主持人）' : '' }}
          </div>
          <div class="status-name" :style="{ color: STATUS_MAP[item.status].color }">
            {{ STATUS_MAP[item.status].desc }}
          </div>
        </div>

        <el-dropdown v-if="isCreater && item.userId !== userInfoStore.userInfo.userId">
          <div class="iconfont icon-more"></div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="kickOutMeeting(item)">踢出会议</el-dropdown-item>
              <el-dropdown-item @click="blackMeeting(item)">拉黑</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

import { useUserInfoStore } from '@/stores/UserInfoStore'
const userInfoStore = useUserInfoStore()

import { useMeetingStore } from '@/stores/MeetingStore'
const meetingStore = useMeetingStore()

/**
 * 权限判定：计算当前用户是否为会议创建者（主持人）
 */
const isCreater = computed(() => {
  // 在成员列表中查找 memberType 为 1 的用户
  const creater = meetingStore.allMemberList.find((item) => {
    return item.memberType == 1
  })
  // 比较创建者的 userId 与当前登录用户的 userId
  if (creater?.userId === userInfoStore.userInfo.userId) {
    return true
  }
  return false
})

/**
 * 会议状态映射表：定义不同状态对应的描述文字和 UI 颜色
 */
const STATUS_MAP = {
  1: {
    desc: '会议中',
    color: '#67C23A' // 绿色
  },
  2: {
    desc: '退出会议',
    color: '#E6A23C' // 橙色
  },
  3: {
    desc: '被踢出会议',
    color: '#F56C6C' // 红色
  },
  4: {
    desc: '被拉黑',
    color: '#909399' // 灰色
  }
}

/**
 * 踢出会议：强制将指定成员移除
 */
const kickOutMeeting = (item) => {
  proxy.Confirm({
    message: `确定要将【${item.nickName}】踢出会议吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.kickOutMeeting,
        params: {
          userId: item.userId
        }
      })
      if (!result) {
        return
      }
      proxy.Message.success('踢出成功')
    }
  })
}

/**
 * 拉黑：将指定成员永久禁入该会议
 */
const blackMeeting = (item) => {
  proxy.Confirm({
    message: `确定要将【${item.nickName}】拉黑吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.blackMeeting,
        params: {
          userId: item.userId
        }
      })
      if (!result) {
        return
      }
      proxy.Message.success('拉黑成功')
    }
  })
}
</script>

<style lang="scss" scoped>
.member-panel {
  // 高度计算：视口总高度减去顶部/底部组件占用的 135px
  height: calc(100vh - 135px);
  background: #fff;

  .member-panel-title {
    border-bottom: 1px solid #ddd;
    padding: 10px;
    color: #4e5461;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .icon-members {
      display: flex;
      align-items: center;
      font-size: 14px;

      &::before {
        margin-right: 3px;
        font-size: 20px;
      }
    }
  }

  .member-list {
    height: 100%;
    overflow: auto; // 开启滚动
    padding-bottom: 10px;

    .member-item {
      display: flex;
      align-items: center;
      padding: 10px 5px;

      .member-info {
        flex: 1;
        margin-left: 10px;
        font-size: 14px;
        width: 0; // 配合 flex: 1 确保文字省略号生效

        .nick-name {
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
        }

        .status-name {
          font-size: 12px;
          margin-top: 2px;
        }
      }
    }
  }
}
</style>
