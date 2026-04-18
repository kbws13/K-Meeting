<template>
  <div class="system-setting-page">
    <div class="setting-card">
      <div class="card-head">
        <div>
          <div class="card-title">系统设置</div>
        </div>

        <div class="card-actions">
          <el-button @click="loadSettings">刷新</el-button>
          <el-button type="primary" @click="saveSettings">保存设置</el-button>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="140px"
        class="setting-form"
      >
        <el-form-item label="图片大小上限" prop="maxImageSize">
          <el-input-number
            v-model="formData.maxImageSize"
            :min="1"
            :max="100"
            controls-position="right"
          />
          <span class="unit-text">MB</span>
        </el-form-item>

        <el-form-item label="视频大小上限" prop="maxVideoSize">
          <el-input-number
            v-model="formData.maxVideoSize"
            :min="1"
            :max="2048"
            controls-position="right"
          />
          <span class="unit-text">MB</span>
        </el-form-item>

        <el-form-item label="文件大小上限" prop="maxFileSize">
          <el-input-number
            v-model="formData.maxFileSize"
            :min="1"
            :max="2048"
            controls-position="right"
          />
          <span class="unit-text">MB</span>
        </el-form-item>
      </el-form>

      <div class="preview-panel">
        <div class="preview-title">当前生效策略</div>
        <div class="preview-list">
          <div class="preview-item">图片文件不超过 {{ formData.maxImageSize }} MB</div>
          <div class="preview-item">视频文件不超过 {{ formData.maxVideoSize }} MB</div>
          <div class="preview-item">普通文件不超过 {{ formData.maxFileSize }} MB</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AdminSystemSetting } from '@model/system'
import type { FormInstance, FormRules } from 'element-plus'
import { ref } from 'vue'
import { useAppProxy } from '@/composables/useAppProxy'

const proxy = useAppProxy()
const formRef = ref<FormInstance>()

const createDefaultSetting = (): AdminSystemSetting => ({
  maxImageSize: 2,
  maxVideoSize: 5,
  maxFileSize: 5
})

const formData = ref<AdminSystemSetting>(createDefaultSetting())

const formRules: FormRules<AdminSystemSetting> = {
  maxImageSize: [{ required: true, message: '请输入图片大小上限', trigger: 'blur' }],
  maxVideoSize: [{ required: true, message: '请输入视频大小上限', trigger: 'blur' }],
  maxFileSize: [{ required: true, message: '请输入文件大小上限', trigger: 'blur' }]
}

const loadSettings = async (): Promise<void> => {
  const result = await proxy.Request<AdminSystemSetting>({
    url: proxy.Api.getSysSetting4Admin,
    method: 'get',
    showLoading: false
  })

  if (!result?.data) {
    return
  }

  formData.value = {
    ...createDefaultSetting(),
    ...result.data
  }
}

void loadSettings()

const saveSettings = async (): Promise<void> => {
  const form = formRef.value
  if (!form) {
    return
  }

  const valid = await form.validate().catch(() => false)
  if (!valid) {
    return
  }

  const result = await proxy.Request<boolean>({
    url: proxy.Api.saveSysSetting,
    dataType: 'json',
    params: formData.value
  })

  if (!result) {
    return
  }

  proxy.Message.success('系统设置已保存')
}
</script>

<style scoped lang="scss">
.system-setting-page {
  height: 100%;
}

.setting-card {
  border: 1px solid #e6edf6;
  border-radius: 24px;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(4, 113, 255, 0.08), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.96));
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2d3d;
}

.card-actions {
  display: flex;
  gap: 12px;
}

.setting-form {
  margin-top: 28px;
  max-width: 560px;
}

.unit-text {
  margin-left: 12px;
  color: #6b7280;
}

.preview-panel {
  margin-top: 28px;
  border: 1px solid rgba(34, 48, 71, 0.08);
  border-radius: 18px;
  padding: 18px 20px;
  background: rgba(244, 247, 252, 0.8);
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #273449;
}

.preview-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.preview-item {
  font-size: 13px;
  color: #627083;
}
</style>
