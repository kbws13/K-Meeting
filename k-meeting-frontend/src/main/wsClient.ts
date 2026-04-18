import WebSocket from 'ws'
import { getWindow, getWindowManage } from './windowProxy'

interface WsMessagePayload {
  messageType: number
  [key: string]: unknown
}

let ws: WebSocket | null = null
const maxRetries = 5
const retryInterval = 2000
let retryCount = 0
const HEARTBEAT_INTERVAL = 5000
let heartBeatTimer: NodeJS.Timeout | null = null
let wsUrl: string | null = null
let needReconnect = false

const parseWsMessage = (rawData: WebSocket.RawData): WsMessagePayload | null => {
  try {
    return JSON.parse(rawData.toString()) as WsMessagePayload
  } catch (error) {
    console.error('解析 websocket 消息失败:', error)
    return null
  }
}

const initWs = (_wsUrl: string): void => {
  wsUrl = _wsUrl
  needReconnect = true
  connectWs()
}

const wsCheck = (): boolean => {
  return import.meta.env.VITE_WS_CHECK === 'true'
}

const connectWs = (): void => {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }

  if (!wsUrl) {
    return
  }

  console.log(`尝试连接....(重试次数:${retryCount}/${maxRetries}),连接地址:${wsUrl}`)
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    if (retryCount > 0 && wsCheck()) {
      getWindow('main')?.webContents.send('reconnect', true)
    }

    retryCount = 0
    console.log('websocket连接成功')
    startHeartBeat()
  }

  ws.onmessage = (event) => {
    const data = parseWsMessage(event.data)
    if (!data) {
      return
    }

    console.log('收到ws消息', data)
    const meetingWin = getWindow('meeting')
    const mainWin = getWindow('main')

    switch (data.messageType) {
      case 1:
      case 2:
      case 3:
        if (mainWin && (data.messageType === 1 || data.messageType === 3)) {
          mainWin.webContents.send('mainMessage', data)
        }
        meetingWin?.webContents.send('meetingMessage', data)
        break
      case 8:
      case 9:
      case 10:
      case 12:
        mainWin?.webContents.send('mainMessage', data)
        break
      case 11:
        meetingWin?.webContents.send('meetingMessage', data)
        break
      default:
        break
    }
  }

  ws.onerror = () => {
    ws?.close()
  }

  ws.onclose = () => {
    clearHeartbeatTimers()
    ws = null
    handleReconnect()
  }
}

const handleReconnect = (): void => {
  if (!needReconnect) {
    return
  }

  if (retryCount >= maxRetries) {
    console.error('已经到达最大重试次数,停止重试')
    retryCount = 0
    if (wsCheck()) {
      logout(false)
    }
    return
  }

  retryCount += 1
  const delay = retryInterval * Math.pow(1.5, retryCount - 1)
  console.log(`连接断开，等待${delay / 1000}秒后重试`)

  if (wsCheck()) {
    getWindow('main')?.webContents.send('reconnect', false)
  }

  setTimeout(() => {
    connectWs()
  }, delay)
}

const startHeartBeat = (): void => {
  clearHeartbeatTimers()
  heartBeatTimer = setInterval(() => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send('ping')
    }
  }, HEARTBEAT_INTERVAL)
}

const clearHeartbeatTimers = (): void => {
  if (heartBeatTimer) {
    clearInterval(heartBeatTimer)
  }
  heartBeatTimer = null
}

const logout = (closeWs = true): void => {
  const loginWidth = 375
  const loginHeight = 365
  const mainWindow = getWindow('main')

  if (closeWs) {
    needReconnect = false
    ws?.close()
    ws = null
  }

  clearHeartbeatTimers()

  const windows = getWindowManage()
  for (const [winKey, win] of Object.entries(windows)) {
    if (winKey !== 'main') {
      win.forceClose = true
      win.close()
    }
  }

  if (!mainWindow) {
    return
  }

  mainWindow.setResizable(true)
  mainWindow.setMinimumSize(loginWidth, loginHeight)
  mainWindow.setSize(loginWidth, loginHeight)
  mainWindow.setResizable(false)
  mainWindow.webContents.send('logout')
}

const sendWsData = (data: string): void => {
  if (ws?.readyState !== WebSocket.OPEN) {
    return
  }

  ws.send(data)
}

export { initWs, logout, sendWsData }
