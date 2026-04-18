<template>
  <div class="app-update-page">
    <PageTable
      ref="pageTableRef"
      :load-data="loadUpdateList"
      :query-params="queryForm"
      row-key="id"
      default-sort-field="createTime"
      default-sort-order="desc"
    >
      <template #toolbar>
        <div class="toolbar">
          <div class="toolbar-copy">
            <div class="toolbar-title">应用更新</div>
          </div>

          <div class="toolbar-actions">
            <el-form :model="queryForm" inline class="search-form" @submit.prevent>
              <el-form-item label="版本号">
                <el-input
                  v-model.trim="queryForm.version"
                  clearable
                  placeholder="例如 1.0.3"
                  @keyup.enter="search"
                />
              </el-form-item>

              <el-form-item label="发布状态">
                <el-select v-model="queryForm.status" clearable placeholder="全部状态">
                  <el-option
                    v-for="item in statusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="更新类型">
                <el-select v-model="queryForm.fileType" clearable placeholder="全部类型">
                  <el-option
                    v-for="item in fileTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item class="action-item">
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-button type="primary" @click="openCreateDialog">发布新更新</el-button>
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

        <el-table-column label="版本号" width="120" align="center">
          <template #default="{ row }">
            <span class="version-text">{{ getTextValue(row.version) || '--' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="更新类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :class="['type-tag', getFileTypeValue(row) === 1 ? 'outer' : 'package']"
              effect="plain"
            >
              {{ getFileTypeLabel(row.fileType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="发布状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :class="['status-tag', getStatusValue(row) === 1 ? 'enabled' : 'disabled']"
              effect="plain"
            >
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="灰度 ID" min-width="170">
          <template #default="{ row }">
            <span class="muted-text">{{ getTextValue(row.grayscaleId) || '全量发布' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="更新说明" min-width="280">
          <template #default="{ row }">
            <div class="desc-text">{{ getTextValue(row.updateDesc) || '--' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="168">
          <template #default="{ row }">
            {{ formatNullableTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="280" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEditDialog(row)">修改</el-button>
              <el-button
                link
                :type="getStatusValue(row) === 1 ? 'warning' : 'success'"
                @click="toggleStatus(row, reload)"
              >
                {{ getStatusValue(row) === 1 ? '撤回' : '重新发布' }}
              </el-button>
              <el-button link type="danger" @click="deleteUpdate(row, reload)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </template>
    </PageTable>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '发布新更新' : '修改更新信息'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px">
        <el-form-item label="版本号" prop="version">
          <el-input v-model.trim="formData.version" placeholder="例如 1.0.3" />
        </el-form-item>

        <el-form-item label="更新类型" prop="fileType">
          <el-radio-group v-model="formData.fileType" @change="handleFileTypeChange">
            <el-radio :value="0">安装包更新</el-radio>
            <el-radio :value="1">外链更新</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发布状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">已发布</el-radio>
            <el-radio :value="0">已撤回</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="灰度 ID">
          <el-input
            v-model.trim="formData.grayscaleId"
            placeholder="多个 ID 用英文逗号分隔，不填表示全量发布"
          />
        </el-form-item>

        <el-form-item v-if="formData.fileType === 0" label="安装包">
          <div class="package-upload">
            <input
              ref="fileInputRef"
              class="package-input"
              type="file"
              @change="handleFileChange"
            />
            <el-button type="primary" plain @click="triggerFileSelect">选择安装包</el-button>
            <el-button v-if="selectedPackageFile" text @click="clearSelectedFile">清除</el-button>
            <div class="package-name">{{ getPackageFieldText() }}</div>
          </div>
        </el-form-item>

        <el-form-item label="外链地址" prop="outerLink">
          <el-input
            v-model.trim="formData.outerLink"
            :disabled="formData.fileType !== 1"
            placeholder="仅外链更新需要填写"
          />
        </el-form-item>

        <el-form-item label="更新说明" prop="updateDesc">
          <el-input
            v-model="formData.updateDesc"
            type="textarea"
            :autosize="{ minRows: 6, maxRows: 10 }"
            placeholder="请输入更新说明，建议一行一个要点"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">
          {{ dialogMode === 'create' ? '发布更新' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { PageInfo, PageLoader, PageResult } from '@model/common'
import type { AdminAppUpdate } from '@model/update'
import type { FormInstance, FormRules } from 'element-plus'
import { ref } from 'vue'
import PageTable from '@/components/PageTable.vue'
import { useAppProxy } from '@/composables/useAppProxy'

interface AppUpdateQuery {
  version: string
  status?: number | ''
  fileType?: number | ''
}

interface AppUpdateFormData {
  id?: number
  version: string
  updateDesc: string
  status: number
  grayscaleId: string
  fileType: number
  outerLink: string
}

type DialogMode = 'create' | 'edit'

const proxy = useAppProxy()

const createDefaultQuery = (): AppUpdateQuery => ({
  version: '',
  status: '',
  fileType: ''
})

const createDefaultForm = (): AppUpdateFormData => ({
  version: '',
  updateDesc: '',
  status: 1,
  grayscaleId: '',
  fileType: 0,
  outerLink: ''
})

const queryForm = ref<AppUpdateQuery>(createDefaultQuery())
const pageTableRef = ref<InstanceType<typeof PageTable> | null>(null)
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('create')
const formRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement | null>(null)
const formData = ref<AppUpdateFormData>(createDefaultForm())
const editingSource = ref<AdminAppUpdate | null>(null)
const selectedPackageFile = ref<File | null>(null)

const statusOptions = [
  { label: '已发布', value: 1 },
  { label: '已撤回', value: 0 }
]

const fileTypeOptions = [
  { label: '安装包更新', value: 0 },
  { label: '外链更新', value: 1 }
]

const formRules: FormRules<AppUpdateFormData> = {
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  fileType: [{ required: true, message: '请选择更新类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择发布状态', trigger: 'change' }],
  updateDesc: [{ required: true, message: '请输入更新说明', trigger: 'blur' }],
  outerLink: [
    {
      validator: (_rule, value: string, callback) => {
        if (formData.value.fileType !== 1) {
          callback()
          return
        }
        if (!value?.trim()) {
          callback(new Error('外链更新必须填写地址'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadUpdateList: PageLoader<AdminAppUpdate> = async (params) => {
  const result = await proxy.Request<PageResult<AdminAppUpdate>>({
    url: proxy.Api.loadUpdateDataList,
    dataType: 'json',
    params
  })
  return result?.data
}

const search = (): void => {
  void pageTableRef.value?.search()
}

const resetQuery = (): void => {
  Object.assign(queryForm.value, createDefaultQuery())
  void pageTableRef.value?.reset()
}

const getTextValue = (value: unknown): string => {
  return typeof value === 'string' ? value : ''
}

const getStatusValue = (row: AdminAppUpdate): number => {
  return typeof row.status === 'number' ? row.status : 0
}

const getFileTypeValue = (row: AdminAppUpdate): number => {
  return typeof row.fileType === 'number' ? row.fileType : 0
}

const getStatusLabel = (status?: unknown): string => {
  return status === 1 ? '已发布' : '已撤回'
}

const getFileTypeLabel = (fileType?: unknown): string => {
  return fileType === 1 ? '外链更新' : '安装包更新'
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

const resetFileState = (): void => {
  selectedPackageFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const triggerFileSelect = (): void => {
  fileInputRef.value?.click()
}

const handleFileChange = (event: Event): void => {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0] ?? null
  selectedPackageFile.value = file
}

const clearSelectedFile = (): void => {
  resetFileState()
}

const handleFileTypeChange = (): void => {
  if (formData.value.fileType === 1) {
    resetFileState()
  } else {
    formData.value.outerLink = ''
  }
  formRef.value?.clearValidate()
}

const openCreateDialog = (): void => {
  dialogMode.value = 'create'
  editingSource.value = null
  formData.value = createDefaultForm()
  resetFileState()
  dialogVisible.value = true
}

const openEditDialog = (row: AdminAppUpdate): void => {
  dialogMode.value = 'edit'
  editingSource.value = row
  formData.value = {
    id: typeof row.id === 'number' ? row.id : undefined,
    version: getTextValue(row.version),
    updateDesc: getTextValue(row.updateDesc),
    status: getStatusValue(row),
    grayscaleId: getTextValue(row.grayscaleId),
    fileType: getFileTypeValue(row),
    outerLink: getTextValue(row.outerLink)
  }
  resetFileState()
  dialogVisible.value = true
}

const needsPackageUpload = (): boolean => {
  if (formData.value.fileType !== 0) {
    return false
  }
  if (dialogMode.value === 'create') {
    return true
  }
  const source = editingSource.value
  if (!source) {
    return true
  }
  if (getFileTypeValue(source) !== 0) {
    return true
  }
  return getTextValue(source.version) !== formData.value.version.trim()
}

const getPackageFieldText = (): string => {
  if (selectedPackageFile.value) {
    return selectedPackageFile.value.name
  }
  if (formData.value.fileType !== 0) {
    return '当前为外链更新'
  }
  if (needsPackageUpload()) {
    return '请选择 .exe 安装包'
  }
  return '未重新选择则保留当前安装包'
}

const buildUpdatePayload = (payload: AppUpdateFormData): Record<string, unknown> => {
  return {
    ...(typeof payload.id === 'number' ? { id: payload.id } : {}),
    version: payload.version.trim(),
    updateDesc: payload.updateDesc.trim(),
    status: payload.status,
    grayscaleId: payload.grayscaleId.trim(),
    fileType: payload.fileType,
    outerLink: payload.fileType === 1 ? payload.outerLink.trim() : '',
    ...(selectedPackageFile.value ? { file: selectedPackageFile.value } : {})
  }
}

const submitForm = async (): Promise<void> => {
  const form = formRef.value
  if (!form) {
    return
  }

  const valid = await form.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (formData.value.fileType === 0 && needsPackageUpload() && !selectedPackageFile.value) {
    proxy.Message.warning('请先上传安装包')
    return
  }

  const result = await proxy.Request<boolean>({
    url: proxy.Api.saveUpdate,
    params: buildUpdatePayload(formData.value)
  })

  if (!result) {
    return
  }

  dialogVisible.value = false
  proxy.Message.success(dialogMode.value === 'create' ? '更新已发布' : '更新信息已保存')
  await pageTableRef.value?.reload()
}

const toggleStatus = (row: AdminAppUpdate, reload: () => Promise<void>): void => {
  if (typeof row.id !== 'number') {
    proxy.Message.warning('更新记录 ID 无效')
    return
  }

  const nextStatus = getStatusValue(row) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '重新发布' : '撤回'

  proxy.Confirm({
    message: `确定${actionText}版本【${getTextValue(row.version)}】吗？`,
    okfun: async () => {
      const result = await proxy.Request<boolean>({
        url: proxy.Api.saveUpdate,
        params: {
          id: row.id,
          version: getTextValue(row.version),
          updateDesc: getTextValue(row.updateDesc),
          status: nextStatus,
          grayscaleId: getTextValue(row.grayscaleId),
          fileType: getFileTypeValue(row),
          outerLink: getTextValue(row.outerLink)
        }
      })

      if (!result) {
        return
      }

      proxy.Message.success(`${actionText}成功`)
      await reload()
    }
  })
}

const deleteUpdate = (row: AdminAppUpdate, reload: () => Promise<void>): void => {
  if (typeof row.id !== 'number') {
    proxy.Message.warning('更新记录 ID 无效')
    return
  }

  proxy.Confirm({
    message: `确定删除版本【${getTextValue(row.version)}】吗？`,
    okText: '确认删除',
    okfun: async () => {
      const result = await proxy.Request<boolean>({
        url: proxy.Api.delUpdate,
        params: {
          id: row.id
        }
      })

      if (!result) {
        return
      }

      proxy.Message.success('更新记录已删除')
      await reload()
    }
  })
}
</script>

<style scoped lang="scss">
.app-update-page {
  height: 100%;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-copy {
  min-width: 280px;
  padding-top: 4px;
}

.toolbar-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.version-text {
  font-weight: 700;
  color: #223047;
}

.muted-text {
  color: #6a7789;
}

.desc-text {
  display: -webkit-box;
  overflow: hidden;
  color: #334155;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.package-upload {
  width: 100%;
}

.package-input {
  display: none;
}

.package-name {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
  word-break: break-all;
}

:deep(.search-form .el-form-item) {
  margin-bottom: 0;
}

:deep(.search-form .el-input),
:deep(.search-form .el-select) {
  width: 180px;
}

:deep(.search-form .action-item .el-form-item__content) {
  gap: 8px;
}

:deep(.type-tag),
:deep(.status-tag) {
  border-radius: 999px;
  padding: 0 10px;
}

:deep(.type-tag.package) {
  color: #1f5eff;
  border-color: rgba(31, 94, 255, 0.18);
  background: rgba(31, 94, 255, 0.08);
}

:deep(.type-tag.outer) {
  color: #7c3aed;
  border-color: rgba(124, 58, 237, 0.18);
  background: rgba(124, 58, 237, 0.08);
}

:deep(.status-tag.enabled) {
  color: #1e8e5a;
  border-color: rgba(30, 142, 90, 0.18);
  background: rgba(30, 142, 90, 0.08);
}

:deep(.status-tag.disabled) {
  color: #d03050;
  border-color: rgba(208, 48, 80, 0.18);
  background: rgba(208, 48, 80, 0.08);
}
</style>
