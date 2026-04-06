import WebSocket from "ws";
import { getWindow, getWindowManage } from './windowProxy'
import { BrowserWindow, ipcMain } from 'electron'

// 定义变量类型
let ws: WebSocket | null = null;
const maxRetries: number = 5;
const retryInterval: number = 2000;
let retryCount: number = 0;
const HEARTBEAT_INTERVAL: number = 5000;
let heartBeatTimer: any = null; // 通常为 NodeJS.Timeout
let wsUrl: string | null = null;
let neetReconnect: boolean | null = null;

/**
 * 初始化 WebSocket
 * @param _wsUrl 连接地址
 */
const initWs = (_wsUrl: string): void => {
  wsUrl = _wsUrl;
  neetReconnect = true;
  connectWs();
};

/**
 * 连接检查
 */
const wsCheck = () => {
  return import.meta.env.VITE_WS_CHECK === "true";
}

/**
 * 执行连接逻辑
 */
const connectWs = (): void => {
  // 检查是否已经处于连接或正在连接状态
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    console.log("已经连接上");
    return;
  }

  console.log(`尝试连接....(重试次数:${retryCount}/${maxRetries}),连接地址:${wsUrl}`);

  if (!wsUrl) return;

  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    // 如果是重连成功且开启了检查逻辑
    if (retryCount > 0 && wsCheck()) {
      const mainwindow = getWindow("main");
      if (mainwindow) {
        // 通知渲染进程重连成功
        mainwindow.webContents.send("reconnect", true);
      }
    }

    retryCount = 0;
    console.log('websocket连接成功');

    // 开启心跳检测
    if (typeof startHeartBeat === 'function') {
      startHeartBeat();
    }
  };

  ws.onmessage = (event: MessageEvent) => {
    const data = JSON.parse(event.data);
    console.log('收到ws消息', data);
  };

  ws.onerror = () => {
    // 发生错误时关闭连接，触发 onclose 进入重连逻辑
    ws?.close();
  };

  ws.onclose = () => {
    // 清除心跳并尝试重连
    clearHeartbeatTimers();
    handleReconnect();
  };
};

/**
 * 重连逻辑处理 (对应 图片 2)
 */
const handleReconnect = (): void => {
  // 如果不需要重连则直接返回
  if (!neetReconnect) {
    return;
  }

  // 检查重试次数是否超过最大限制
  if (retryCount >= maxRetries) {
    console.error("已经到达最大重试次数,停止重试");
    retryCount = 0;
    // 执行校验并退出登录（此处 wsCheck 假定为函数调用）
    if (typeof wsCheck === 'function' && wsCheck()) {
      logout(false);
    }
    return;
  }

  retryCount += 1;

  // 图片 2 底部代码被截断，显示为 "const delay = netr"
  // 按照常规逻辑，此处通常为延迟一段时间后调用 connectWs()
  const delay: number = retryInterval * Math.pow(1.5, retryCount - 1); // 这是一个常见的指数退避或线性延迟逻辑
  console.log(`连接断开，等待${delay / 1000}秒后重试`)
  if (wsCheck()) {
    const mainWindow = getWindow("main")
    mainWindow.webContents.send("reconnect", false);
  }
  setTimeout(() => {
    connectWs();
  }, delay);
};

/**
 * 心跳机制逻辑 (对应 图片 3)
 */
const startHeartBeat = (): void => {
  // 定时发送心跳包
  heartBeatTimer = setInterval(() => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send("ping");
    }
  }, HEARTBEAT_INTERVAL);
};

/**
 * 清除心跳定时器 (对应 图片 3)
 */
const clearHeartbeatTimers = (): void => {
  if (heartBeatTimer) {
    clearInterval(heartBeatTimer);
  }
  heartBeatTimer = null;
};

const logout = (closeWs = true) => {
  const login_width: number = 375;
  const login_height: number = 365;

  // 获取主窗口实例
  const mainWindow = getWindow("main") as BrowserWindow | null;

  if (closeWs) {
    neetReconnect = false;
    ws?.close()
  }

  // 关闭所有非主窗口
  const windows = getWindowManage()
  for (let winKey in windows) {
    const win = windows[winKey];
    if (winKey !== "main") {
      win.close();
    }
  }

  if (mainWindow) {
    // 切换回登录窗口大小
    mainWindow.setResizable(true);
    mainWindow.setMinimumSize(login_width, login_height);
    mainWindow.setSize(login_width, login_height);
    mainWindow.setResizable(false);
    // 通知渲染进程执行登出
    mainWindow.webContents.send("logout")
  }
}

const sendWsData = (data: any): void => {
  if (!ws) {
    return
  }
  ws.send(data)
}

export {
  initWs,
  logout,
  sendWsData
}
