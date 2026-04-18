export interface AppUpdateCheckVO {
  hasUpdate: boolean
  id: number | null
  version: string | null
  updateDesc: string | null
  fileType: number | null
  outerLink: string | null
  downloadUrl: string | null
}
