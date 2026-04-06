import { ElMessage, MessageType } from 'element-plus'

/**
 * 内部通用的消息显示逻辑
 * @param msg 消息内容
 * @param callback 消息关闭后的回调函数（可选）
 * @param type 消息类型：'success' | 'warning' | 'info' | 'error'
 */
const showMessage = (msg: string, callback?: () => void, type?: MessageType): void => {
  ElMessage({
    type: type,
    message: msg,
    duration: 2000,
    offset: 200,
    onClose: () => {
      if (callback) {
        callback()
      }
    }
  })
}

/**
 * 导出封装好的消息对象
 */
const message = {
  error: (msg: string, callback?: () => void): void => {
    showMessage(msg, callback, 'error')
  },
  success: (msg: string, callback?: () => void): void => {
    showMessage(msg, callback, 'success')
  },
  warning: (msg: string, callback?: () => void): void => {
    showMessage(msg, callback, 'warning')
  }
}

export default message
