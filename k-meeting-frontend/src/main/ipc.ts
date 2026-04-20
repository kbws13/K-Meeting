import {
  BrowserWindow,
  desktopCapturer,
  dialog,
  ipcMain,
  type OpenDialogOptions,
  shell,
  type SourcesOptions
} from 'electron'
import icon from '../../resources/icon.png?asset'
import {
  ChangeLocalFolderPayload,
  DownloadUpdatePayload,
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
import { delWindow, getWindow, type ManagedBrowserWindow, saveWindow } from './windowProxy'
import { initWs, logout, sendWsData } from './wsClient'
import store from './store'
import { startRecording, stopRecording } from './recording'
import { getSysSetting, saveSysSetting } from './sysSetting'
import { is } from '@electron-toolkit/utils'
import { join } from 'path'
import { downloadUpdate } from './appUpdate'

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

const onDownloadUpdate = () => {
  ipcMain.on('downloadUpdate', (_event, { id, downloadUrl, fileName }: DownloadUpdatePayload) => {
    downloadUpdate({ id, downloadUrl, fileName })
  })
}

/**
 * 注册文件选择的 IPC 处理程序
 */
const onSelectFile = () => {
  ipcMain.handle('selectFile', async () => {
    // 1. 打开原生文件选择对话框
    const { canceled, filePaths } = await dialog.showOpenDialog(BrowserWindow.getFocusedWindow(), {
      title: "选择文件",
      properties: ['openFile']
    });

    // 2. 如果用户取消操作，返回空对象
    if (canceled) {
      return {};
    }

    // 3. 获取第一个选中的文件路径
    const filePath = filePaths[0];

    // 4. 获取文件统计信息（如文件大小）
    const { size } = await fs.promises.stat(filePath);

    // 5. 返回包含文件基本信息的对象
    return {
      fileName: path.basename(filePath), // 获取文件名（带扩展名）
      fileSize: size,                    // 文件大小 (字节)
      filePath                           // 完整路径
    };
  });
};

/**
 * 注册文件上传 IPC 处理程序
 */
const onUploadChatFile = () => {
  ipcMain.on("uploadChatFile", (e, { uploadUrl, messageId, sendTime, filePath }) => {
    console.log(uploadUrl, messageId, sendTime, filePath);

    // 获取会议窗口实例，用于后续向该窗口发送进度信息
    const meetingWin = getWindow("meeting");

    // 从本地存储获取用户令牌
    const token = store.getData("userInfo")?.token;

    // 执行文件上传请求
    // 注意：在 Node.js (Electron 主进程) 中，原生的 Web FormData 不支持直接追加 Node.js Stream。
    // 但 Axios ^1.2.0+ 提供了对对象属性的原生 multipart/form-data 序列化支持，并且完美兼容 Node.js Stream。
    axios.post(uploadUrl, {
      messageId: messageId,
      sendTime: sendTime,
      file: fs.createReadStream(filePath)
    }, {
      headers: { 'Content-Type': 'multipart/form-data', "token": token },
      // 监听上传进度
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          // 计算上传百分比
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);

          // 如果会议窗口存在，将进度同步回前端渲染页面
          if (meetingWin) {
            meetingWin.webContents.send("uploadProgress", { messageId, percent });
          }
        }
      }
    }).catch(error => {
      console.error("文件上传失败", error);
    });
  });
};

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
  onSendPeerConnection,
  onDownloadUpdate,
  onSelectFile,
  onUploadChatFile,
}
