import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '登录',
      component: () => import('@/views/login/Login.vue')
    },
    {
      path: '/home',
      name: '主页',
      component: () => import('@/views/Layout.vue'),
      redirect: '/meetingMain',
      children: [
        {
          path: '/meetingMain',
          name: '首页',
          component: () => import('@/views/meeting/MeetingMain.vue'),
          meta: {
            code: 'meeting'
          }
        },
        {
          path: '/contact',
          name: '联系人',
          component: () => import('@/views/contact/Contact.vue'),
          meta: {
            code: 'contact'
          }
        },
        {
          path: '/screencap',
          name: '录屏',
          component: () => import('@/views/screencap/ScreenCap.vue'),
          meta: {
            code: 'screencap'
          }
        },
        {
          path: '/setting',
          name: '设置',
          component: () => import('@/views/setting/Setting.vue'),
          meta: {
            code: 'setting'
          }
        }
      ]
    },
    {
      path: '/meeting',
      name: '会议详情',
      component: () => import('@/views/meeting/meeting/Meeting.vue')
    },
    {
      path: '/admin',
      name: '管理后台',
      component: () => import('@/views/admin/AdminLayout.vue'),
      redirect: '/userList',
      children: [
        {
          path: '/userList',
          name: '用户管理',
          component: () => import('@/views/admin/user/UserList.vue')
        },
        {
          path: '/meetingManage',
          name: '会议管理',
          component: () => import('@/views/admin/meeting/MeetingManage.vue')
        },
        {
          path: '/appUpdateManage',
          name: '应用更新',
          component: () => import('@/views/admin/update/AppUpdateManage.vue')
        },
        {
          path: '/systemSetting',
          name: '系统设置',
          component: () => import('@/views/admin/system/SystemSetting.vue')
        }
      ]
    }
  ]
})

export default router
