export type PageSortOrder = 'asc' | 'desc'

export interface PageRequestParams {
  current: number
  pageSize: number
  sortField?: string
  sortOrder?: PageSortOrder
}

export interface PageInfo extends PageRequestParams {
  total: number
  pages: number
}

export interface PageResult<T> {
  records: T[]
  total?: number
  size?: number
  current?: number
  pages?: number
}

export type PageLoader<T = unknown> = (
  params: PageRequestParams & Record<string, unknown>
) => Promise<PageResult<T> | null | undefined>
