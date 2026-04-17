<template>
  <Header :showBottomBorder="true"></Header>
  <div class="body-panel">
    <div class="contact-user-panel">
      <div class="contact-user">
        <div class="part-title">我的联系人</div>
        <ContactList></ContactList>
      </div>
      <div class="contact-apply">
        <div class="part-title">联系人申请</div>
        <ContactApplyList></ContactApplyList>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ContactList from './ContactList.vue'
import ContactApplyList from './ContactApplyList.vue'
</script>

<style scoped lang="scss">
.body-panel {
  height: calc(100vh - 32px);

  .contact-user-panel {
    display: flex;

    .contact-user {
      border-right: 1px solid #ddd;
    }

    .contact-user,
    .contact-apply {
      .part-title {
        margin-bottom: 10px;
      }

      width: 50%;
      padding: 0px 15px;
    }
  }
}
</style>
