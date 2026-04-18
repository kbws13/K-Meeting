import {
  BrowserWindow,
  desktopCapturer,
  dialog,
  ipcMain,
  shell,
  type OpenDialogOptions,
  type SourcesOptions
} from 'electron'
import icon from '../../resources/icon.png?asset'
import type {
  ChangeLocalFolderPayload,
  LoginSuccessPayload,
  OpenLocalFilePayload,
  OpenWindowPayload,
  PeerConnectionPayload,
  ScreenSource,
  StartRecordingPayload,
  WindowRouteParams,
  WinOpPayload
} from '@model/ipc'
import type { SysSetting } from '@model/system'
import type { PersistedUserInfo } from '@model/user'
import { delWindow, getWindow, saveWindow, type ManagedBrowserWindow } from './windowProxy'
import { initWs, logout, sendWsData } from './wsClient'
import store from './store'
import { startRecording, stopRecording } from './recording'
import { getSysSetting, saveSysSetting } from './sysSetting'
import { is } from '@electron-toolkit/utils'
import { join } from 'path'

const getMainWindow = (): ManagedBrowserWindow | undefined => {
  return getWindow('main')
}

const appendRouteParams = (routePath: string, data?: WindowRouteParams): string => {
  if (!data) {
    return routePath
  }

  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(data)) {
    if (value != null) {
      params.append(key, String(value))
    }
  }

  if (!params.size) {
    return routePath
  }

  const separator = routePath.includes('?') ? '&' : '?'
  return `${routePath}${separator}${params.toString()}`
}

const loadWindowRoute = async (window: BrowserWindow, routePath: string): Promise<void> => {
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    await window.loadURL(`${process.env['ELECTRON_RENDERER_URL']}/index.html#${routePath}`)
    return
  }

  await window.loadFile(join(__dirname, '../renderer/index.html'), { hash: routePath })
}

const onLoginOrRegister = (): void => {
  ipcMain.handle('loginOrRegister', (_event, isLogin: boolean): void => {
    const loginWidth = 375
    const loginHeight = 365
    const registerHeight = 485
    const mainWindow = getMainWindow()

    if (!mainWindow) {
      return
    }

    mainWindow.setResizable(true)
    mainWindow.setMinimumSize(loginWidth, loginHeight)
    mainWindow.setSize(loginWidth, isLogin ? loginHeight : registerHeight)
    mainWindow.setResizable(false)
  })
}

const onWinTitleOp = (): void => {
  ipcMain.on('winTitleOp', (e: Electron.IpcMainEvent, { action, data }: WinOpPayload) => {
    const win = BrowserWindow.fromWebContents(e.sender) as ManagedBrowserWindow | null
    if (!win) {
      return
    }

    switch (action) {
      case 'close':
        if (data?.closeType === 0) {
          win.forceClose = data.forceClose
          win.close()
        } else {
          win.setSkipTaskbar(true)
          win.hide()
        }
        break
      case 'minimize':
        win.minimize()
        break
      case 'maximize':
        win.maximize()
        break
      case 'unmaximize':
        win.unmaximize()
        break
    }
  })
}

const onLoginSuccess = (): void => {
  ipcMain.handle('loginSuccess', (_event, { userInfo, wsUrl }: LoginSuccessPayload): void => {
    const mainWindow = getMainWindow()
    if (!mainWindow) {
      return
    }

    mainWindow.setResizable(true)
    mainWindow.setMinimumSize(720, 480)
    mainWindow.setSize(720, 480)
    mainWindow.setResizable(false)
    store.initUserId(userInfo.id)
    store.setData('userInfo', userInfo)
    initWs(wsUrl + userInfo.token)
  })
}

const onGetScreenSource = (): void => {
  ipcMain.handle(
    'getScreenSource',
    async (_event, opts: SourcesOptions): Promise<ScreenSource[]> => {
      const sources = await desktopCapturer.getSources(opts)

      return sources
        .filter((source) => {
          const size = source.thumbnail.getSize()
          return size.width > 10 && size.height > 10
        })
        .map((source) => ({
          id: source.id,
          name: source.name,
          displayId: source.display_id,
          thumbnail: source.thumbnail.toDataURL()
        }))
    }
  )
}

const onStartRecoding = (): void => {
  ipcMain.handle('startRecording', (e, { displayId, mic }: StartRecordingPayload): void => {
    const sender = e.sender
    startRecording(sender, displayId, mic)
  })
}

const onStopRecording = (): void => {
  ipcMain.handle('stopRecording', () => {
    stopRecording()
  })
}

const onOpenLocalFile = (): void => {
  ipcMain.on(
    'openLocalFile',
    (_event: Electron.IpcMainEvent, { localFilePath, folder = false }: OpenLocalFilePayload) => {
      if (folder) {
        shell.openPath(localFilePath)
      } else {
        shell.showItemInFolder(localFilePath)
      }
    }
  )
}

const onSaveSysSetting = (): void => {
  ipcMain.handle('saveSysSetting', (_event, sysSetting: string | SysSetting): void => {
    saveSysSetting(sysSetting)
  })
}

const onGetSysSetting = (): void => {
  ipcMain.handle('getSysSetting', (): SysSetting => {
    return getSysSetting()
  })
}

const onChangeLocalFolder = (): void => {
  ipcMain.handle(
    'changeLocalFolder',
    async (_event, { localFilePath }: ChangeLocalFolderPayload): Promise<string | undefined> => {
      const option: OpenDialogOptions = {
        properties: ['openDirectory'],
        defaultPath: localFilePath
      }

      const result = await dialog.showOpenDialog(option)

      if (result.canceled) {
        return
      }

      const selectedPath = result.filePaths[0]
      return process.platform === 'win32' ? selectedPath.replaceAll('/', '\\') : selectedPath
    }
  )
}

const onLogout = (): void => {
  ipcMain.handle('logout', () => {
    logout()
  })
}

const openWindow = async ({
  windowId,
  title = '详情',
  path: routePath,
  width = 960,
  height = 720,
  data,
  maximizable = false
}: OpenWindowPayload): Promise<void> => {
  const targetRoute = appendRouteParams(routePath, data)
  let newwindow = getWindow(windowId)

  if (!newwindow) {
    const createdWindow = new BrowserWindow({
      title,
      width,
      height,
      minHeight: height,
      minWidth: width,
      show: false,
      autoHideMenuBar: true,
      frame: false,
      fullscreenable: false,
      resizable: maximizable,
      maximizable,
      ...(process.platform === 'linux' ? { icon } : {}),
      webPreferences: {
        preload: join(__dirname, '../preload/index.js'),
        sandbox: false
      }
    }) as ManagedBrowserWindow

    newwindow = createdWindow
    saveWindow(windowId, createdWindow)
    await loadWindowRoute(createdWindow, targetRoute)

    createdWindow.on('ready-to-show', () => {
      createdWindow.show()
    })

    createdWindow.on('close', (event) => {
      if (createdWindow.forceClose !== undefined && !createdWindow.forceClose) {
        notifyWindowPreClose(windowId)
        event.preventDefault()
      }
    })

    createdWindow.on('closed', () => {
      closeWindow(windowId)
      delWindow(windowId)
    })

    createdWindow.on('maximize', () => {
      createdWindow.webContents.send('winIsMax', true)
    })

    createdWindow.on('unmaximize', () => {
      createdWindow.webContents.send('winIsMax', false)
    })
  } else {
    const existingWindow = newwindow
    await loadWindowRoute(existingWindow, targetRoute)
    existingWindow.show()
    existingWindow.setSkipTaskbar(false)
  }
}

const closeWindow = (windowId: string): void => {
  const mainWindow = getMainWindow()
  if (mainWindow) {
    mainWindow.webContents.send('closeWindow', { windowId })
  }
}

const notifyWindowPreClose = (windowId: string): void => {
  const win = getWindow(windowId)
  if (win) {
    win.webContents.send('preCloseWindow')
  }
}

const onOpenWindow = (): void => {
  ipcMain.on('openWindow', (_event, payload: OpenWindowPayload) => {
    void openWindow({
      ...payload
    })
  })
}

const onSendPeerConnection = (): void => {
  ipcMain.on('sendPeerConnection', (_event, peerData: PeerConnectionPayload) => {
    const userInfo = store.getData<PersistedUserInfo>('userInfo')
    sendWsData(
      JSON.stringify({
        ...peerData,
        token: userInfo?.token
      })
    )
  })
}

export {
  onLoginOrRegister,
  onWinTitleOp,
  onLoginSuccess,
  onGetScreenSource,
  onStartRecoding,
  onStopRecording,
  onOpenLocalFile,
  onSaveSysSetting,
  onGetSysSetting,
  onChangeLocalFolder,
  onLogout,
  onOpenWindow,
  onSendPeerConnection
}
