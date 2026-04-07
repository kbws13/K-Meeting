import Store from 'electron-store';

// 初始化 electron-store 实例
// 处理 ESM 模块在 CommonJS 环境下的默认导出兼容性
const StoreClass = (Store as any).default || Store;
const store = new StoreClass();

// 定义 userId 的类型，初始为 null，后续为 string
let userId: string | null = null;

/**
 * 初始化用户 ID
 */
const initUserId = (_userId: string | null): void => {
  userId = _userId;
};

/**
 * 设置数据
 * 使用 userId + key 作为唯一的存储键
 */
const setData = (key: string, value: any): void => {
  // 保持原逻辑：userId 为 null 时，JS 会将其转为字符串 "null" 进行拼接
  // 在 TS 中为了严谨且不改变功能，使用 String() 转换
  store.set(String(userId) + key, value);
};

/**
 * 获取数据
 */
const getData = (key: string): any => {
  return store.get(String(userId) + key);
};

/**
 * 获取当前用户 ID
 */
const getUserId = (): string | null => {
  return userId;
};

export default {
  initUserId,
  setData,
  getData,
  getUserId,
};
