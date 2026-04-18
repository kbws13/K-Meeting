<template>
  <div class="layout">
    <div class="left">
      <div class="top-panel">
        <div class="avatar">
          <Avatar
            ref="avatarRef"
            :width="30"
            :avatar="userInfoStore.userInfo.userId"
            :update="true"
            @click="showUserInfo"
          >
          </Avatar>
        </div>
        <div class="top-menus">
          <div
            :class="['menu-item', item.codes.includes(route.meta.code as string) ? 'active' : '']"
            v-for="item in leftTopMenus"
            @click="jumpMenu(item)"
          >
            <el-badge
              :value="item.messageCount"
              :max="99"
              :hidden="item.messageCount == 0"
              :offset="[-5, 0]"
            >
              <div :class="['iconfont', 'icon-' + item.icon]"></div>
              <div class="name">{{ item.name }}</div>
            </el-badge>
          </div>
        </div>
      </div>
      <div class="bottom-menus">
        <template v-for="item in leftBottomMenus">
          <div
            :class="['menu-item', item.codes.includes(route.meta.code as string) ? 'active' : '']"
            v-if="!item.onlyAdmin || (item.onlyAdmin && userInfoStore.userInfo.admin)"
            @click="jumpMenu(item)"
          >
            <div :class="['iconfont', 'icon-' + item.icon]"></div>
          </div>
        </template>
      </div>
    </div>
    <div class="right">
      <router-view></router-view>
    </div>
  </div>
  <UpdateUser ref="updateUserRef" @reloadInfo="reloadInfoHandler"></UpdateUser>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserInfoStore } from '@/stores/UserInfoStore'
import { useContactStore } from '../stores/UserContactStore'
import { mitter } from '../eventbus/eventBus'
import Avatar from '../components/Avatar.vue'
import UpdateUser from './UpdateUser.vue'
import { useMeetingStore } from '../stores/MeetingStore'

const { proxy } = getCurrentInstance() as any
const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()
const meetingStore = useMeetingStore()
const contactStore = useContactStore()

/**
 * 菜单项接口定义
 */
interface MenuItem {
  name?: string // 菜单名称
  icon: string // 图标
  path?: string // 跳转路径
  codes: string[] // 权限代码
  messageCount?: number // 消息计数
  onlyAdmin?: boolean // 是否仅管理员可见
  btnType?: string // 按钮类型（如管理后台）
}

/**
 * 左侧顶部菜单数据（图片1）
 */
const leftTopMenus = ref<MenuItem[]>([
  {
    name: '会议',
    icon: 'video',
    path: '/meetingMain',
    codes: ['meeting'],
    messageCount: 0
  },
  {
    name: '通讯录',
    icon: 'contact',
    path: '/contact',
    codes: ['contact'],
    messageCount: 0
  },
  {
    name: '录制',
    icon: 'record',
    path: '/screencap',
    codes: ['screencap'],
    messageCount: 0
  }
])

/**
 * 左侧底部菜单数据（图片2）
 */
const leftBottomMenus: MenuItem[] = [
  {
    icon: 'settings',
    path: '/setting',
    codes: ['setting'],
    onlyAdmin: false
  },
  {
    icon: 'admin',
    codes: [],
    btnType: 'admin',
    onlyAdmin: true
  }
]

const jumpMenu = (item: MenuItem) => {
  if (item.btnType === 'admin') {
    return
  }
  router.push(item.path)
}

const listenMessage = () => {
  window.electron.ipcRenderer.on('mainMessage', (e, messageObj) => {
    console.log('收到消息', messageObj)
    switch (messageObj.messageType) {
      case 1: // 加入会议
        const newMember = messageObj.messageContent.newMember
        // 判断加入会议的成员是否为当前登录用户
        if (newMember.userId === userInfoStore.userInfo.userId) {
          meetingStore.updateMeeting(true)
        }
        break
      case 3: // 退出会议
        // 解析信令内容，获取退出状态和对应的用户 ID
        const { exitStatus, exitUserId } = messageObj.messageContent

        // 状态定义参考：3-被踢出会议，4-被拉黑
        // 如果退出的是当前登录用户，且是因为被踢出或被拉黑
        if ((exitStatus == 3 || exitStatus == 4) && exitUserId == userInfoStore.userInfo.userId) {
          proxy.Confirm({
            message: '你被强制退出会议',
            showCancelBtn: false // 强制操作，不显示取消按钮
          })

          // 执行会议状态更新逻辑，将当前用户的会议状态设为 false
          if (exitStatus == 2 || exitStatus == 3 || exitStatus == 4) {
            meetingStore.updateMeeting(false)
          }
        }
        break
      case 8:
        contactStore.updateLastUpdateTime()
        break
      case 9: // 收到会议邀请
        // 如果当前用户已经在会议中，则直接忽略邀请
        if (meetingStore.inMeeting) {
          return
        }

        // 解析邀请信息：包含会议名称、ID、邀请人姓名
        const { meetingName, meetingId, inviteUserName } = messageObj.messageContent

        // 弹出确认对话框
        proxy.Confirm({
          message: `【${inviteUserName}】邀请你加入会议【${meetingName}】`,
          okText: '接受邀请',
          cancelText: '拒绝',
          okfun: () => {
            // 用户点击“接受邀请”后执行的回调函数
            acceptInvite(meetingId)
          }
        })
        break
      case 12:
        let result = ''
        if (messageObj.messageContent == 1) {
          mitter.emit('reloadContact')
          result = '已同意你的申请'
        } else if (messageObj.messageContent == 2) {
          result = '已拒绝你的申请'
        } else if (messageObj.messageContent == 3) {
          result = '已将你拉黑'
        }
        proxy.Alert(`【${messageObj.sendUserNickName}${result}】`)
        break
    }
  })
}

const loadContactApplyCount = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadContactApplyDealWithCount
  })
  if (!result) {
    return
  }
  leftTopMenus.value[1].messageCount = result.data
}

const updateUserRef = ref()
const showUserInfo = () => {
  updateUserRef.value.show()
}

const avatarRef = ref()
const reloadInfoHandler = (data) => {
  userInfoStore.setInfo(data)
  avatarRef.value.updateAvatarUrl()
}

/**
 * 接受会议邀请
 * @param {string} meetingId 会议ID
 */
const acceptInvite = async (meetingId) => {
  // 1. 调用后端接口，确认接受邀请
  let result = await proxy.Request({
    url: proxy.Api.acceptInvite,
    params: {
      meetingId
    }
  })

  // 2. 如果请求失败或被取消，直接返回
  if (!result) {
    return
  }

  // 3. 接口调用成功后，通知 Electron 主进程开启“会议详情”窗口
  window.electron.ipcRenderer.send('openWindow', {
    title: '会议详情',
    windowId: 'meeting',
    path: '/meeting',
    width: 1310,
    height: 800,
    maximizable: true
  })
}

watch(
  () => contactStore.lastUpdateTime,
  (newVal, oldVal) => {
    if (!newVal) {
      return
    }
    loadContactApplyCount()
  },
  { immediate: true, deep: true }
)

onMounted(() => {
  listenMessage()
})

onUnmounted(() => {
  window.electron.ipcRenderer.removeAllListeners('mainMessage')
})
</script>

<style scoped lang="scss">
.layout {
  display: flex;

  .left {
    width: 64px;
    background: #f3f3f4;
    margin: 0px auto;
    display: flex;
    align-items: center;
    flex-direction: column;
    justify-content: space-between;
    -webkit-app-region: drag;

    .top-panel {
      text-align: center;

      .avatar {
        display: flex;
        justify-content: center;
        -webkit-app-region: no-drag;
        margin: 40px 0px 20px 0px;
      }
    }

    .bottom-menus {
      margin-bottom: 30px;
    }

    .menu-item {
      text-align: center;
      -webkit-app-region: no-drag;
      cursor: pointer;
      margin-bottom: 20px;
      color: #4c5262;

      .iconfont {
        font-size: 20px;
      }

      .name {
        margin-top: 5px;
        font-size: 12px;
      }

      &:hover {
        color: #353535;
      }

      &:last-child {
        margin-bottom: 0px;
      }

      &.active {
        .iconfont {
          color: var(--blue);
        }
        .name {
          color: var(--blue);
        }
      }
    }
  }

  .right {
    flex: 1;
    width: 0;
    height: calc(100vh);
  }
}
</style>
