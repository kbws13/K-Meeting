import os from 'os';
import fs from 'fs';
import path from 'path';
import store from './store';

// 1. 初始化本地配置文件夹路径
// 使用 path.join 可以自动处理不同系统的路径分隔符，比 replaceAll 更健壮
const userDir: string = os.homedir();
const localFolder: string = path.join(userDir, '/.easymeeting/');

// 2. 确保文件夹存在
if (!fs.existsSync(localFolder)) {
  fs.mkdirSync(localFolder, { recursive: true });
}

/**
 * 系统设置接口定义
 */
interface SysSetting {
  openCamera: boolean;
  openMic: boolean;
  screencapFolder: string;
}

/**
 * 保存系统设置到本地文件
 * @param sysSetting 序列化后的设置字符串或数据
 */
const saveSysSetting = (sysSetting: string): void => {
  try {
    const userId: string | number = store.getUserId();
    // 拼接配置文件完整路径
    const configFile: string = path.join(localFolder, userId.toString());

    // 同步写入文件
    fs.writeFileSync(configFile, sysSetting, 'utf8');
  } catch (error) {
    console.error('保存系统设置失败:', error);
  }
};

/**
 * 获取系统设置
 * @returns 返回系统配置对象
 */
const getSysSetting = (): SysSetting => {
  // 假设 userId 是字符串或数字
  const userId: string | number = store.getUserId();
  // 使用 path.join 替代字符串拼接，以确保跨平台路径兼容性
  const configFile: string = path.join(localFolder, userId.toString());

  // 1. 如果配置文件不存在，返回默认配置
  if (!fs.existsSync(configFile)) {
    return {
      openCamera: true,
      openMic: true,
      screencapFolder: localFolder
    };
  }

  // 2. 如果文件存在，读取并解析 JSON
  try {
    const content = fs.readFileSync(configFile, "utf8");
    return JSON.parse(content) as SysSetting;
  } catch (error) {
    console.error("读取或解析配置文件失败，返回默认设置:", error);
    return {
      openCamera: true,
      openMic: true,
      screencapFolder: localFolder
    };
  }
};

// 3. 导出模块
export {
  saveSysSetting,
  getSysSetting
};
