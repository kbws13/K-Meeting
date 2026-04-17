<template>
  <div class="search-panel">
    <el-input
      clearable
      placeholder="输入联系人搜索"
      v-model="keywords"
      :prefix-icon="Search"
      @keyup="search"
    ></el-input>
    <div class="iconfont icon-invite" @click="applyContact"></div>
  </div>
  <div class="contact-list">
    <div class="contact-item" v-for="item in contactList">
      <Avatar :avatar="item.contactId"></Avatar>
      <div class="nick-name">{{ item.nickName }}</div>
      <el-dropdown>
        <div class="iconfont icon-more"></div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="delContact(item.contactId, 2)">删除</el-dropdown-item>
            <el-dropdown-item @click="delContact(item.contactId, 3)">拉黑</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <NoData v-if="contactList.length == 0" msg="暂无联系人"></NoData>
  </div>
  <ApplyContact ref="applyContactRef" @reload="loadContactUser"></ApplyContact>
</template>

<script setup lang="ts">
import ApplyContact from './ApplyContact.vue'
import { Search } from '@element-plus/icons-vue'
import { ComponentInternalInstance, getCurrentInstance, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mitter } from '@/eventbus/eventBus.ts'

// 定义联系人对象接口
interface ContactUser {
  id: string | number
  nickName: string
  avatar?: string

  [key: string]: any
}

// 获取组件实例并进行类型断言
// 注意：在 TS 中直接解构 proxy 需要定义实例类型
const { proxy } = getCurrentInstance() as ComponentInternalInstance & { proxy: any }
const router = useRouter()
const route = useRoute()

// 搜索关键词
const keywords = ref<string>('')

/**
 * 搜索函数占位
 */
const search = (): void => {
  // 逻辑实现
}

// 源联系人列表（备份用）
const sourceContactList = ref<ContactUser[]>([])
// 展示用的联系人列表
const contactList = ref<ContactUser[]>([])

/**
 * 异步加载联系人用户列表
 */
const loadContactUser = async (): Promise<void> => {
  let result = await proxy.Request({
    url: proxy.Api.loadContactUser,
    dataType: 'json',
    params: {}
  })

  if (!result) {
    return
  }

  // 赋值原始数据及展示数据
  sourceContactList.value = result.data.records
  contactList.value = result.data.records
}

// 初始化加载数据
loadContactUser()

const applyContactRef = ref()
const applyContact = () => {
  applyContactRef.value.show()
}

onMounted(() => {
  mitter.on('reloadContact', loadContactUser)
})

onUnmounted(() => {
  mitter.off('reloadContact', loadContactUser)
})
</script>

<style scoped lang="scss">
.search-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .icon-invite {
    margin-left: 10px;
    background: #eeeeee;
    border-radius: 5px;
    padding: 8px;
    color: #747474;
    cursor: pointer;

    &::before {
      font-size: 18px;
    }
  }
}

.contact-list {
  height: calc(100vh - 100px);
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

    .icon-more {
      cursor: pointer;
    }
  }
}

.el-tooltip__trigger:focus-visible {
  outline: unset;
}
</style>
