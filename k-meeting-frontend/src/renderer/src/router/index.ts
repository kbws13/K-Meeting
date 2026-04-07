import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '登录',
      component: () => import('@/pages/login/Login.vue')
    },
    {
      path: '/home',
      name: '主页',
      component: () => import('@/pages/Layout.vue'),
      redirect: '/meetingMain',
      children: [{
        path: '/meetingMain',
        name: "首页",
        component: () => import('@/pages/meeting/MeetingMain.vue'),
        meta: {
          code: "meeting"
        }
      },{
        path: '/contact',
        name: "联系人",
        component: () => import('@/pages/contact/Contact.vue'),
        meta: {
          code: "contact"
        }
      }
      ]
    }
  ]
})

export default router
