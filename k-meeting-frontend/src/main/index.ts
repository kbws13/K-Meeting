import { app, BrowserWindow, ipcMain, Menu, nativeImage, shell, Tray } from 'electron'
import { join } from 'path'
import { electronApp, is, optimizer } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'
import { saveWindow } from './windowProxy'
import {
  onChangeLocalFolder,
  onDownload,
  onDownloadUpdate,
  onGetScreenSource,
  onGetSysSetting,
  onLoginOrRegister,
  onLoginSuccess,
  onLogout,
  onOpenLocalFile,
  onOpenWindow,
  onSaveSysSetting,
  onSelectFile,
  onSendPeerConnection,
  onStartRecoding,
  onStopRecording,
  onUploadChatFile,
  onWinTitleOp
} from './ipc'
import MenuItemConstructorOptions = Electron.MenuItemConstructorOptions
import NativeImage = Electron.NativeImage

let tray: Tray | null = null

function createTrayIcon(): string | NativeImage {
  if (process.platform !== 'darwin') {
    return icon
  }

  const image = nativeImage.createFromPath(icon)
  if (image.isEmpty()) {
    return icon
  }

  return image.resize({ width: 18, height: 18, quality: 'best' })
}

function createWindow(): void {
  // Create the browser window.
  const mainWindow = new BrowserWindow({
    width: 375,
    height: 365,
    show: false,
    autoHideMenuBar: true,
    resizable: false,
    frame: false,
    transparent: false,
    maximizable: false,
    ...(process.platform === 'linux' ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
    }
  })

  saveWindow('main', mainWindow)

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
  })

  /**
   * 初始化系统托盘逻辑
   */

  tray = new Tray(createTrayIcon())

  // 定义菜单模板，使用 MenuItemConstructorOptions 接口进行类型约束
  const contextMenuTemplate: MenuItemConstructorOptions[] = [
    {
      label: '退出',
      click: () => {
        app.quit()
      }
    }
  ]

  const menu = Menu.buildFromTemplate(contextMenuTemplate)

  tray.setToolTip('EasyMeeting')
  tray.setContextMenu(menu)

  // 托盘点击事件
  tray.on('click', () => {
    if (mainWindow) {
      // 从任务栏显示并唤起窗口
      mainWindow.setSkipTaskbar(false)
      mainWindow.show()
    }
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  // HMR for renderer base on electron-vite cli.
  // Load the remote URL for development or the local html file for production.
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

onLoginOrRegister()

onWinTitleOp()

onLoginSuccess()

onGetScreenSource()

onStartRecoding()

onStopRecording()

onOpenLocalFile()

onSaveSysSetting()

onGetSysSetting()

onChangeLocalFolder()

onLogout()

onOpenWindow()

onSendPeerConnection()

onDownloadUpdate()

onSelectFile()

onUploadChatFile()

onDownload()

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.whenReady().then(() => {
  // Set app user model id for windows
  electronApp.setAppUserModelId('com.electron')

  // Default open or close DevTools by F12 in development
  // and ignore CommandOrControl + R in production.
  // see https://github.com/alex8088/electron-toolkit/tree/master/packages/utils
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // IPC test
  ipcMain.on('ping', () => console.log('pong'))

  createWindow()

  app.on('activate', function () {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.
