export interface AppUpdateCheckVO {
  hasUpdate: boolean
  id: number | null
  version: string | null
  updateDesc: string | null
  fileType: number | null
  outerLink: string | null
  downloadUrl: string | null
}

export interface AdminAppUpdate {
  id?: number
  version?: string
  updateDesc?: string
  status?: number
  grayscaleId?: string
  fileType?: number
  outerLink?: string | null
  createTime?: string | number | Date
}
