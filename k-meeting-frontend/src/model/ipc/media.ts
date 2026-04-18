export interface ScreenSource {
  id: string
  name: string
  displayId: string
  thumbnail: string
}

export interface StartRecordingPayload {
  displayId: string | number
  mic?: string
}
