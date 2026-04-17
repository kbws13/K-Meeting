import {
  BrowserWindow,
  desktopCapturer,
  dialog,
  ipcMain,
  IpcMainInvokeEvent,
  shell,
  SourcesOptions
} from 'electron'
import { delWindow, getWindow, saveWindow } from './windowProxy'
import { initWs, logout, sendWsData } from './wsClient'
import store from './store'
import { startRecording, stopRecording } from './recording'
import { getSysSetting, saveSysSetting } from './sysSetting'
import { is } from '@electron-toolkit/utils'
import { join } from 'path'

/**
 * 处理登录或注册窗口尺寸调整的函数
 */
const onLoginOrRegister = (): void => {
  // 使用 ipcMain.handle 处理渲染进程发来的异步调用
  ipcMain.handle('loginOrRegister', (e: Electron.IpcMainInvokeEvent, isLogin: boolean): void => {
    const login_width: number = 375
    const login_height: number = 365
    const register_height: number = 485

    // 获取主窗口实例，假设 getWindow 返回的是 BrowserWindow 或 null
    const mainWindow = getWindow('main') as BrowserWindow | null

    if (mainWindow) {
      // 1. 临时允许修改尺寸
      mainWindow.setResizable(true)

      // 2. 设置最小尺寸限制
      mainWindow.setMinimumSize(login_width, login_height)

      // 3. 根据状态切换窗口大小
      if (isLogin) {
        mainWindow.setSize(login_width, login_height)
      } else {
        mainWindow.setSize(login_width, register_height)
      }

      // 4. 锁定尺寸，禁止用户手动拖拽
      mainWindow.setResizable(false)
    }
  })
}

const onWinTitleOp = (): void => {
  ipcMain.on('winTitleOp', (e: Electron.IpcMainEvent, { action, data }: WinOpPayload) => {
    const webContents = e.sender
    const win = BrowserWindow.fromWebContents(webContents)

    // 严谨性检查：确保窗口实例存在
    if (!win) return

    switch (action) {
      case 'close':
        if (data.closeType === 0) {
          // forceClose 通常是自定义属性，用于在 close 事件监听中判断是否强制退出
          // 在 TS 中通过 (win as any) 来允许添加非标准属性
          ;(win as any).forceClose = data.forceClose
          win.close()
        } else {
          // 隐藏窗口并从任务栏移除
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

const onLoginSuccess = () => {
  ipcMain.handle('loginSuccess', (e: Electron.IpcMainEvent, { userInfo, wsUrl }) => {
    const mainWindow = getWindow('main') as BrowserWindow | null
    mainWindow.setResizable(true)
    mainWindow.setMinimumSize(720, 480)
    mainWindow.setSize(720, 480)
    mainWindow.setResizable(false)
    store.initUserId(userInfo.id)
    store.setData('userInfo', userInfo)
    initWs(wsUrl + userInfo.token)
  })
}

/**
 * 屏幕源信息接口
 */
interface ScreenSource {
  id: string
  name: string
  displayId: string
  thumbnail: string
}

/**
 * 监听获取屏幕资源的 IPC 请求
 */
const onGetScreenSource = (): void => {
  // 使用 handle 监听，渲染进程通过 invoke 调用
  ipcMain.handle(
    'getScreenSource',
    async (event: IpcMainInvokeEvent, opts: SourcesOptions): Promise<ScreenSource[]> => {
      // 1. 调用 Electron 底层 API 获取屏幕和窗口资源
      const sources = await desktopCapturer.getSources(opts)

      // 2. 过滤并格式化数据
      return sources
        .filter((source) => {
          // 过滤掉尺寸过小（通常是无效或最小化窗口）的资源
          const size = source.thumbnail.getSize()
          return size.width > 10 && size.height > 10
        })
        .map((source) => ({
          id: source.id,
          name: source.name,
          // 注意：底层字段名是 display_id，此处映射为前端易用的 camelCase
          displayId: source.display_id,
          // 将 NativeImage 对象转换为 Base64 字符串，方便渲染进程直接在 <img> 或 <Cover> 中展示
          thumbnail: source.thumbnail.toDataURL()
        }))
    }
  )
}

const onStartRecoding = () => {
  ipcMain.handle('startRecording', (e, { displayId, mic }) => {
    const sender = e.sender
    startRecording(sender, displayId, mic)
  })
}

const onStopRecording = () => {
  ipcMain.handle('stopRecording', () => {
    stopRecording()
  })
}

const onOpenLocalFile = () => {
  ipcMain.on('openLocalFile', (e: Electron.IpcMainEvent, { localFilePath, folder = false }) => {
    if (folder) {
      shell.openPath(localFilePath)
    } else {
      shell.showItemInFolder(localFilePath)
    }
  })
}

const onSaveSysSetting = () => {
  ipcMain.handle('saveSysSetting', (e: Electron.IpcMainEvent, sysSetting) => {
    saveSysSetting(sysSetting)
  })
}

const onGetSysSetting = () => {
  ipcMain.handle('getSysSetting', (e: Electron.IpcMainEvent, sysSetting) => {
    return getSysSetting()
  })
}

const onChangeLocalFolder = () => {
  ipcMain.handle('changeLocalFolder', async (e, { localFilePath }) => {
    const option = {
      properties: ['openDirectory'],
      defaultPath: localFilePath
    }

    // 调用 Electron 原生对话框让用户选择目录
    let result = await dialog.showOpenDialog(option)

    // 如果用户取消了选择，则直接返回
    if (result.canceled) {
      return
    }

    // 返回选中的第一个路径，并将路径中的正斜杠替换为反斜杠（通常用于 Windows 系统适配）
    return result.filePaths[0].replaceAll('/', '\\')
  })
}

const onLogout = () => {
  ipcMain.handle('logout', () => {
    logout()
  })
}

const openWindow = ({
  windowId,
  title = '详情',
  path,
  width = 960,
  height = 720,
  data,
  maximizable = false
}) => {
  let newwindow = getWindow(windowId)
  const paramsArray = []

  // 1. URL 参数拼接逻辑
  if (data && Object.keys(data).length > 0) {
    // 检查 path 是否已有参数，没有则补 "?"，有则补 "&"
    path = path.endsWith('?') ? path : path + '?'
    for (let i in data) {
      paramsArray.push(`${i}=${encodeURIComponent(data[i])}`)
    }
    path = path + paramsArray.join('&')
  }

  // 2. 创建新窗口实例
  if (!newwindow) {
    newwindow = new BrowserWindow({
      width,
      height,
      minHeight: height,
      minWidth: width,
      show: false, // 初始化不显示，等 ready-to-show 再显示，防止白屏
      autoHideMenuBar: true, // 自动隐藏菜单栏
      frame: false, // 无边框窗口
      fullscreenable: false,
      resizable: maximizable,
      maximizable,
      // Linux 平台下特殊处理图标
      ...(process.platform === 'linux' ? { icon } : {}),
      webPreferences: {
        preload: join(__dirname, '../preload/index.js'), // 注入预加载脚本
        sandbox: false // 禁用沙箱以允许特定权限
      }
    })

    saveWindow(windowId, newwindow)
    // 3. 根据环境加载内容
    if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
      // 开发环境：加载 Vite/Webpack 的热更新地址
      newwindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}/index.html#${path}`)
    } else {
      // 生产环境：加载本地 HTML 文件
      newwindow.loadFile(join(__dirname, '../renderer/index.html'), { hash: `${path}` })
    }

    // 4. 窗口生命周期监听
    newwindow.on('ready-to-show', () => {
      newwindow.show()
    })

    // 拦截关闭事件（实现点击关闭时隐藏窗口而非真正销毁，或执行清理逻辑）
    newwindow.on('close', (event) => {
      if (newwindow.forceClose !== undefined && !newwindow.forceClose) {
        preCloseWindow(windowId)
        event.preventDefault() // 阻止默认关闭行为
      }
    })

    newwindow.on('closed', () => {
      closewindow(windowId)
      delWindow(windowId)
    })

    // 监听最大化/还原，通知渲染进程更新 UI（例如切换最大化图标）
    newwindow.on('maximize', (e) => {
      newwindow.webContents.send('winIsMax', true)
    })

    newwindow.on('unmaximize', (e) => {
      newwindow.webContents.send('winIsMax', false)
    })
  } else {
    if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
      // 开发环境：加载 Vite/Webpack 的热更新地址
      newwindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}/index.html#${path}`)
    } else {
      // 生产环境：加载本地 HTML 文件
      newwindow.loadFile(join(__dirname, '../renderer/index.html'), { hash: `${path}` })
    }
    newwindow.show()
    newwindow.setSkipTaskbar(false)
  }
}

const closewindow = (windowId) => {
  const mainwindow = getWindow('main')
  if (mainwindow) {
    mainwindow.webContents.send('closeWindow', { windowId })
  }
}

const preCloseWindow = (windowId) => {
  const win = getWindow(windowId)
  if (win) {
    win.webContents.send('preCloseWindow')
  }
}

const onOpenWindow = () => {
  ipcMain.on('openWindow', (e, { title, windowId, path, width, height, data, maximizable }) => {
    openWindow({
      title,
      windowId,
      path,
      width,
      height,
      data,
      maximizable
    })
  })
}

const onSendPeerConnection = () => {
  ipcMain.on('sendPeerConnection', (e, peerData) => {
    peerData.token = store.getData('userInfo')?.token
    sendWsData(JSON.stringify(peerData))
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
