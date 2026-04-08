import { ipcMain, BrowserWindow, desktopCapturer, SourcesOptions, IpcMainInvokeEvent, shell } from 'electron'
import { getWindow } from "./windowProxy";
import { initWs } from './wsClient'
import store from './store'
import { startRecording, stopRecording } from './recording'

/**
 * 处理登录或注册窗口尺寸调整的函数
 */
const onLoginOrRegister = (): void => {
  // 使用 ipcMain.handle 处理渲染进程发来的异步调用
  ipcMain.handle("loginOrRegister", (e: Electron.IpcMainInvokeEvent, isLogin: boolean): void => {
    const login_width: number = 375;
    const login_height: number = 365;
    const register_height: number = 485;

    // 获取主窗口实例，假设 getWindow 返回的是 BrowserWindow 或 null
    const mainWindow = getWindow("main") as BrowserWindow | null;

    if (mainWindow) {
      // 1. 临时允许修改尺寸
      mainWindow.setResizable(true);

      // 2. 设置最小尺寸限制
      mainWindow.setMinimumSize(login_width, login_height);

      // 3. 根据状态切换窗口大小
      if (isLogin) {
        mainWindow.setSize(login_width, login_height);
      } else {
        mainWindow.setSize(login_width, register_height);
      }

      // 4. 锁定尺寸，禁止用户手动拖拽
      mainWindow.setResizable(false);
    }
  });
};

const onWinTitleOp = (): void => {
  ipcMain.on("winTitleOp", (e: Electron.IpcMainEvent, { action, data }: WinOpPayload) => {
    const webContents = e.sender;
    const win = BrowserWindow.fromWebContents(webContents);

    // 严谨性检查：确保窗口实例存在
    if (!win) return;

    switch (action) {
      case "close":
        if (data.closeType === 0) {
          // forceClose 通常是自定义属性，用于在 close 事件监听中判断是否强制退出
          // 在 TS 中通过 (win as any) 来允许添加非标准属性
          (win as any).forceClose = data.forceClose;
          win.close();
        } else {
          // 隐藏窗口并从任务栏移除
          win.setSkipTaskbar(true);
          win.hide();
        }
        break;
      case "minimize":
        win.minimize();
        break;
      case "maximize":
        win.maximize();
        break;
      case "unmaximize":
        win.unmaximize();
        break;
    }
  });
};

const onLoginSuccess = () => {
  ipcMain.handle("loginSuccess", (e: Electron.IpcMainEvent, { userInfo, wsUrl }) => {
    const mainWindow = getWindow("main") as BrowserWindow | null;
    mainWindow.setResizable(true);
    mainWindow.setMinimumSize(720, 480)
    mainWindow.setSize(720, 480)
    mainWindow.setResizable(false)
    store.initUserId(userInfo.userId)
    store.setData("userInfo", userInfo)
    initWs(wsUrl + userInfo.token)
  })
}

const onSaveSysSetting = () => {
  ipcMain.handle("saveSysSetting", (e: Electron.IpcMainEvent, sysSetting) => {
    saveSysSetting(sysSetting);
  })
}

/**
 * 屏幕源信息接口
 */
interface ScreenSource {
  id: string;
  name: string;
  displayId: string;
  thumbnail: string;
}

/**
 * 监听获取屏幕资源的 IPC 请求
 */
const onGetScreenSource = (): void => {
  // 使用 handle 监听，渲染进程通过 invoke 调用
  ipcMain.handle("getScreenSource", async (
    event: IpcMainInvokeEvent,
    opts: SourcesOptions
  ): Promise<ScreenSource[]> => {

    // 1. 调用 Electron 底层 API 获取屏幕和窗口资源
    const sources = await desktopCapturer.getSources(opts);

    // 2. 过滤并格式化数据
    return sources
      .filter(source => {
        // 过滤掉尺寸过小（通常是无效或最小化窗口）的资源
        const size = source.thumbnail.getSize();
        return size.width > 10 && size.height > 10;
      })
      .map(source => ({
        id: source.id,
        name: source.name,
        // 注意：底层字段名是 display_id，此处映射为前端易用的 camelCase
        displayId: source.display_id,
        // 将 NativeImage 对象转换为 Base64 字符串，方便渲染进程直接在 <img> 或 <Cover> 中展示
        thumbnail: source.thumbnail.toDataURL()
      }));
  });
};

const onStartRecoding = () => {
  ipcMain.handle("startRecording", (e, { displayId, mic }) => {
    const sender = e.sender
    startRecording(sender, displayId, mic)
  })
}

const onStopRecording = () => {
  ipcMain.handle("stopRecording", () => {
    stopRecording()
  })
}

const onOpenLocalFile = () => {
  ipcMain.on("openLocalFile", (e: Electron.IpcMainEvent, { localFilePath, folder = false }) => {
    if (folder) {
      shell.openPath(localFilePath);
    } else {
      shell.showItemInFolder(localFilePath);
    }
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
};
