import { ipcMain, BrowserWindow } from "electron";
import { getWindow } from "./windowProxy";
import { initWs } from './wsClient'

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
    initWs(wsUrl + userInfo.token)
  })
}

export {
  onLoginOrRegister,
  onWinTitleOp,
  onLoginSuccess
};
