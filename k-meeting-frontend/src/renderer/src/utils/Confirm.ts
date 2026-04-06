import { ElMessageBox, MessageBoxState } from 'element-plus'

/**
 * Confirm 函数参数接口定义
 */
interface ConfirmParams {
  message: string
  okfun?: () => void
  showCancelBtn?: boolean
  showClose?: boolean
  okText?: string
  cancelText?: string
  cancelfun?: () => void
}

/**
 * 确认对话框
 */
const Confirm = ({
  message,
  okfun,
  showCancelBtn = true,
  showClose = true,
  okText = '确定',
  cancelText = '取消',
  cancelfun
}: ConfirmParams): void => {
  ElMessageBox.confirm(message, '提示', {
    'close-on-click-modal': false,
    confirmButtonText: okText,
    cancelButtonText: cancelText,
    showCancelButton: showCancelBtn,
    showClose: showClose,
    type: 'info'
  })
    .then(() => {
      if (okfun) {
        okfun()
      }
    })
    .catch((action: string) => {
      // 当点击取消按钮且存在取消回调时执行
      if (action === 'cancel' && cancelfun) {
        cancelfun()
      }
    })
}

/**
 * 警示对话框
 * @param msg 消息内容
 * @param okfun 点击确定后的回调
 */
const Alert = (msg: string, okfun?: () => void): void => {
  ElMessageBox.alert(msg, '确认', {
    confirmButtonText: 'OK',
    showClose: false,
    callback: (action: string) => {
      if (action === 'confirm' && okfun) {
        okfun()
      }
    }
  })
}

export { Confirm, Alert }
