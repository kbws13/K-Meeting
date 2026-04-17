/**
 * 正则表达式定义
 */
const regs = {
  email: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/,
  number: /^\+?[1-9][0-9]*$/,
  password: /^[\da-zA-Z~!@#$%^&*_]{8,18}$/,
  version: /^[0-9\.]+$/
}

/**
 * 校验规则接口定义 (通常对应 async-validator 的 Rule)
 */
interface VerifyRule {
  message: string

  [key: string]: any
}

/**
 * 回调函数类型定义
 */
type VerifyCallback = (error?: Error) => void

/**
 * 通用正则校验函数
 */
const verify = (rule: VerifyRule, value: any, reg: RegExp, callback: VerifyCallback): void => {
  if (value) {
    if (reg.test(value)) {
      callback()
    } else {
      callback(new Error(rule.message))
    }
  } else {
    callback()
  }
}

/**
 * 外部调用的密码校验（返回布尔值）
 */
const checkPassword = (value: string): boolean => {
  return regs.password.test(value)
}

/**
 * 外部调用的邮箱校验（返回布尔值）
 */
const checkEmail = (value: string): boolean => {
  return regs.email.test(value)
}

/**
 * 表单组件使用的密码校验规则
 */
const password = (rule: VerifyRule, value: any, callback: VerifyCallback): void => {
  return verify(rule, value, regs.password, callback)
}

/**
 * 表单组件使用的数字校验规则
 */
const number = (rule: VerifyRule, value: any, callback: VerifyCallback): void => {
  return verify(rule, value, regs.number, callback)
}

export default {
  checkPassword,
  checkEmail,
  password,
  number
}
