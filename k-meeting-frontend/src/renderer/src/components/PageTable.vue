<template>
  <div class="page-table">
    <div v-if="$slots.toolbar" class="page-table-toolbar">
      <slot
        name="toolbar"
        :loading="loading"
        :reload="reload"
        :search="search"
        :reset="reset"
        :pageInfo="pageInfo"
      />
    </div>

    <div class="page-table-card">
      <el-table
        ref="tableRef"
        v-loading="loading"
        class="km-page-table"
        :data="tableData"
        :row-key="rowKey || undefined"
        :border="border"
        :stripe="stripe"
        :height="height"
        :max-height="maxHeight"
        :empty-text="emptyText"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <slot
          :loading="loading"
          :rows="tableData"
          :reload="reload"
          :search="search"
          :reset="reset"
          :pageInfo="pageInfo"
        />

        <template #empty>
          <slot name="empty">
            <div class="page-table-empty">
              <el-empty :description="emptyText" />
            </div>
          </slot>
        </template>
      </el-table>

      <div v-if="showPagination" class="page-table-footer">
        <div class="page-table-footer-left">
          <span class="record-count">共 {{ total }} 条</span>
          <el-button
            v-if="showRefresh"
            class="refresh-btn"
            text
            :loading="loading"
            @click="reload(false)"
          >
            刷新
          </el-button>
        </div>

        <el-pagination
          :current-page="current"
          :page-size="pageSize"
          :page-sizes="pageSizes"
          :total="total"
          :pager-count="7"
          background
          layout="sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  PageInfo,
  PageLoader,
  PageRequestParams,
  PageResult,
  PageSortOrder
} from '@model/common'
import { computed, onMounted, ref, watch } from 'vue'

interface TableSortChangePayload {
  column?: {
    columnKey?: string
  } | null
  prop?: string
  order?: 'ascending' | 'descending' | null
}

interface Props {
  loadData: PageLoader
  queryParams?: Record<string, unknown>
  rowKey?: string | ((row: unknown) => string)
  pageSizes?: number[]
  defaultCurrent?: number
  defaultPageSize?: number
  defaultSortField?: string
  defaultSortOrder?: PageSortOrder
  border?: boolean
  stripe?: boolean
  autoLoad?: boolean
  watchQueryChange?: boolean
  showPagination?: boolean
  showRefresh?: boolean
  emptyText?: string
  height?: string | number
  maxHeight?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  queryParams: () => ({}),
  rowKey: '',
  pageSizes: () => [10, 20, 50, 100],
  defaultCurrent: 1,
  defaultPageSize: 10,
  defaultSortField: '',
  defaultSortOrder: undefined,
  border: false,
  stripe: false,
  autoLoad: true,
  watchQueryChange: false,
  showPagination: true,
  showRefresh: true,
  emptyText: '暂无数据',
  height: undefined,
  maxHeight: undefined
})

const emit = defineEmits<{
  loaded: [result: PageResult<unknown>, params: PageRequestParams & Record<string, unknown>]
  'selection-change': [rows: unknown[]]
  'page-change': [pageInfo: PageInfo]
  'sort-change': [sortInfo: { sortField?: string; sortOrder?: PageSortOrder }]
}>()

const tableRef = ref<{
  clearSelection?: () => void
  clearSort?: () => void
} | null>(null)
const loading = ref(false)
const tableData = ref<unknown[]>([])
const total = ref(0)
const pages = ref(0)
const current = ref(props.defaultCurrent)
const pageSize = ref(props.defaultPageSize)
const sortField = ref(props.defaultSortField)
const sortOrder = ref<PageSortOrder | undefined>(props.defaultSortOrder)

const pageInfo = computed<PageInfo>(() => ({
  current: current.value,
  pageSize: pageSize.value,
  total: total.value,
  pages: pages.value,
  sortField: sortField.value || undefined,
  sortOrder: sortOrder.value
}))

const sanitizeParams = (params: Record<string, unknown>): Record<string, unknown> => {
  return Object.entries(params).reduce<Record<string, unknown>>((result, [key, value]) => {
    if (value == null) {
      return result
    }
    if (typeof value === 'string' && value.trim() === '') {
      return result
    }
    if (Array.isArray(value) && value.length === 0) {
      return result
    }
    result[key] = value
    return result
  }, {})
}

const buildRequestParams = (): PageRequestParams & Record<string, unknown> => {
  const requestParams: PageRequestParams & Record<string, unknown> = {
    ...sanitizeParams(props.queryParams),
    current: current.value,
    pageSize: pageSize.value
  }

  if (sortField.value) {
    requestParams.sortField = sortField.value
  }
  if (sortOrder.value) {
    requestParams.sortOrder = sortOrder.value
  }

  return requestParams
}

const applyPageResult = (result: PageResult<unknown>): void => {
  tableData.value = result.records ?? []
  total.value = Number(result.total ?? 0)

  if (typeof result.current === 'number') {
    current.value = result.current
  }
  if (typeof result.size === 'number') {
    pageSize.value = result.size
  }

  pages.value =
    Number(result.pages ?? 0) ||
    (pageSize.value > 0 ? Math.ceil(total.value / pageSize.value) : 0)
}

const load = async (resetPage = false): Promise<void> => {
  if (resetPage) {
    current.value = props.defaultCurrent
  }

  loading.value = true
  const requestParams = buildRequestParams()

  try {
    const result = await props.loadData(requestParams)
    if (!result) {
      return
    }
    applyPageResult(result)
    emit('loaded', result, requestParams)
  } finally {
    loading.value = false
  }
}

const search = async (): Promise<void> => {
  await load(true)
}

const reload = async (resetPage = false): Promise<void> => {
  await load(resetPage)
}

const clearSortState = (): void => {
  tableRef.value?.clearSort()
  sortField.value = props.defaultSortField || ''
  sortOrder.value = props.defaultSortOrder
}

const reset = async (): Promise<void> => {
  current.value = props.defaultCurrent
  pageSize.value = props.defaultPageSize
  clearSortState()
  await load(false)
}

const handleCurrentChange = (value: number): void => {
  current.value = value
  emit('page-change', pageInfo.value)
  void load(false)
}

const handleSizeChange = (value: number): void => {
  pageSize.value = value
  current.value = props.defaultCurrent
  emit('page-change', pageInfo.value)
  void load(false)
}

const handleSelectionChange = (rows: unknown[]): void => {
  emit('selection-change', rows)
}

const handleSortChange = ({ column, prop, order }: TableSortChangePayload): void => {
  if (!order) {
    sortField.value = props.defaultSortField || ''
    sortOrder.value = props.defaultSortOrder
  } else {
    sortField.value = column?.columnKey || prop || ''
    sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  }

  current.value = props.defaultCurrent
  emit('sort-change', {
    sortField: sortField.value || undefined,
    sortOrder: sortOrder.value
  })
  void load(false)
}

watch(
  () => props.queryParams,
  () => {
    if (!props.watchQueryChange) {
      return
    }
    void search()
  },
  { deep: true }
)

onMounted(() => {
  if (props.autoLoad) {
    void load(false)
  }
})

defineExpose({
  load,
  search,
  reload,
  reset,
  clearSelection: () => tableRef.value?.clearSelection(),
  clearSort: clearSortState,
  getTableData: () => tableData.value,
  getPageInfo: () => pageInfo.value
})
</script>

<style scoped lang="scss">
.page-table {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-table-toolbar {
  padding: 18px 20px;
  border: 1px solid #e7edf5;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}

.page-table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e7edf5;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.km-page-table {
  flex: 1;
}

.page-table-empty {
  padding: 24px 0;
}

.page-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-top: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.page-table-footer-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.record-count {
  color: #7b8798;
  font-size: 13px;
}

.refresh-btn {
  color: var(--blue);
}

:deep(.el-table) {
  --el-table-border-color: #edf1f7;
  --el-table-header-bg-color: #f7faff;
  --el-table-row-hover-bg-color: #f4f8ff;
  --el-table-text-color: var(--text);
  --el-table-header-text-color: #6b7788;
}

:deep(.el-table th.el-table__cell) {
  height: 54px;
  font-weight: 600;
}

:deep(.el-table .el-table__cell) {
  padding: 14px 0;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-pagination) {
  justify-content: flex-end;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next),
:deep(.el-pagination .el-pager li) {
  border-radius: 10px;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: var(--blue);
}

:deep(.el-pagination .el-select__wrapper),
:deep(.el-pagination .el-input__wrapper) {
  border-radius: 10px;
}
</style>
