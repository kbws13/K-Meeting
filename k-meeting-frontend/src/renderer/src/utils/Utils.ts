import moment from 'moment'
import LunarCalendar from 'lunar-calendar'
import { Api } from '@/utils/Api'

// 配置 moment 中文语言包
moment.locale('zh-cn', {
  months: '一月_二月_三月_四月_五月_六月_七月_八月_九月_十月_十一月_十二月'.split('_'),
  monthsShort: '1月_2月_3月_4月_5月_6月_7月_8月_9月_10月_11月_12月'.split('_'),
  weekdays: '星期日_星期一_星期二_星期三_星期四_星期五_星期六'.split('_'),
  longDateFormat: {
    LT: 'HH:mm',
    LTS: 'HH:mm:ss',
    L: 'YYYY-MM-DD',
    LL: 'YYYY年MM月DD日',
    LLL: 'YYYY年MM月DD日Ah点mm分',
    LLLL: 'YYYY年MM月DD日ddddAh点mm分',
    l: 'YYYY-M-D',
    ll: 'YYYY年M月D日',
    lll: 'YYYY年M月D日 HH:mm',
    llll: 'YYYY年M月D日dddd HH:mm'
  }
})

/**
 * 判断字符串是否为空
 */
const isEmpty = (str: string | null | undefined): boolean => {
  if (str === null || str === '' || str === undefined) {
    return true
  }
  return false
}

/**
 * 格式化日期（相对时间逻辑）
 */
const formatDate = (timestamp: number | string | Date): string => {
  const timestampTime = moment(timestamp)
  const days =
    Number.parseInt(moment().format('YYYYMMDD')) - Number.parseInt(timestampTime.format('YYYYMMDD'))

  if (days === 0) {
    return timestampTime.format('HH:mm')
  } else if (days === 1) {
    return '昨天'
  } else if (days >= 2 && days < 7) {
    // 大于1天小于7天显示星期几
    return timestampTime.format('dddd')
  } else {
    // 大于等于7天显示年月日
    return timestampTime.format('YYYY-MM-DD')
  }
}

/**
 * 通用日期格式化
 */
const formatDate2 = (timestamp: number | string | Date, patten: string): string => {
  const timestampTime = moment(timestamp)
  return timestampTime.format(patten)
}

/**
 * 获取农历日期和星期
 */
const getChinaDateDay = (): string => {
  const today = moment()
  // 计算星期几（中文）
  const weekday = ['日', '一', '二', '三', '四', '五', '六'][today.day()]
  // 转换为农历日期
  const lunar = LunarCalendar.solarToLunar(today.year(), today.month() + 1, today.date())
  // 处理闰月显示
  const isLeap = lunar.isLeap ? '闰' : ''
  const lunarDate = `${isLeap}${lunar.lunarMonthName}${lunar.lunarDayName}`
  return `星期${weekday} 农历${lunarDate}`
}

/**
 * 获取星期和简易日期
 */
const getWeekAndDate = (timestamp: number | string | Date): string => {
  const today = moment(new Date(timestamp))
  const weekday = ['日', '一', '二', '三', '四', '五', '六'][today.day()]
  return `星期${weekday} ${today.format('M月DD日')}`
}

/**
 * 文件大小转字符串
 */
const size2Str = (Limit: number): string => {
  let size = ''
  if (Limit < 0.1 * 1024) {
    // 小于0.1KB，则转化成B
    size = Limit.toFixed(2) + 'B'
  } else if (Limit < 1024 * 1024) {
    // 小于1MB，则转化成KB
    size = (Limit / 1024).toFixed(2) + 'KB'
  } else if (Limit < 1024 * 1024 * 1024) {
    // 小于1GB，则转化成MB
    size = (Limit / (1024 * 1024)).toFixed(2) + 'MB'
  } else {
    // 其他转化成GB
    size = (Limit / (1024 * 1024 * 1024)).toFixed(2) + 'GB'
  }

  let sizeStr = size + ''
  const index = sizeStr.indexOf('.')
  const dou = sizeStr.substring(index + 1, index + 3) // 获取小数点后两位的值
  if (dou === '00') {
    // 判断后两位是否为00，如果是则删除.00
    return sizeStr.substring(0, index) + sizeStr.substring(index + 3)
  }
  return size
}

/**
 * 秒转时分秒格式
 */
const convertSecondsToHMS = (seconds: number, showHours: boolean = false): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60

  const hourStr = showHours ? '00:' : ''
  return (
    (hours === 0 ? hourStr : hours.toString().padStart(2, '0') + ':') +
    minutes.toString().padStart(2, '0') +
    ':' +
    remainingSeconds.toString().padStart(2, '0')
  )
}

/**
 * 时间增加分钟数
 */
const timeAddMin = (timestamp: number | string | Date, addMin: number): string => {
  return moment(timestamp).add(addMin, 'minutes').format('HH:mm')
}

/**
 * 获取文件名（去掉后缀）
 */
const getFileName = (fileName: string): string => {
  if (!fileName) {
    return fileName
  }
  return fileName.lastIndexOf('.') === -1
    ? fileName
    : fileName.substring(0, fileName.lastIndexOf('.'))
}

/**
 * 获取本地资源路径
 */
const getLocalResource = (resource: string): string => {
  const path = `../assets/${resource}`
  return new URL(path, import.meta.url).href
}

/**
 * 获取用户 Token
 */
const getToken = (): string => {
  const userInfoJson = localStorage.getItem('userInfo')
  return userInfoJson ? JSON.parse(userInfoJson).token : ''
}

/**
 * 构建资源请求路径
 */
interface ResourcePathParams {
  messageId: string | number
  thumbnail?: boolean
  fileType?: number | string
  sendTime?: number | string
}

const getResourcePath = ({
  messageId,
  thumbnail = false,
  fileType,
  sendTime
}: ResourcePathParams): string => {
  const token = getToken() || '';
  // @ts-ignore
  const domain = import.meta.env.PROD ? import.meta.env.VITE_DOMAIN : ''
  return `${domain}${Api.getResource}?token=${token}&messageId=${messageId}&sendTime=${sendTime}&fileType=${fileType}${thumbnail ? '&thumbnail=true' : ''}`
}

// 建议将 userId 转换为字符串，并确保 token 存在
const getAvatarPath = (userId: string | number, forceUpdate = false): string => {
  const token = getToken() || ''
  const baseUrl = import.meta.env.PROD ? import.meta.env.VITE_DOMAIN : ''
  // 确保 userId 是字符串，避免 Vue 类型检查警告
  const userIdStr = String(userId)
  return `${baseUrl}${Api.getAvatar}?userId=${userIdStr}&token=${token}${forceUpdate ? '&v=' + new Date().getTime() : ''}`
}

/**
 * 格式化会议号（三位一空格）
 */
const formatMeetingNo = (meetingNo: string): string => {
  return (
    meetingNo.substring(0, 3) + ' ' + meetingNo.substring(3, 6) + ' ' + meetingNo.substring(6, 10)
  )
}

/**
 * 根据性别获取图标名
 */
const getSexIcon = (sex: number | string): string => {
  if (sex == 0) {
    return 'icon-woman'
  } else if (sex == 1) {
    return 'icon-man'
  } else {
    return 'icon-user-nick'
  }
}

export default {
  isEmpty,
  formatDate,
  formatDate2,
  getChinaDateDay,
  getWeekAndDate,
  size2Str,
  convertSecondsToHMS,
  timeAddMin,
  getFileName,
  getLocalResource,
  getToken,
  getResourcePath,
  getAvatarPath,
  formatMeetingNo,
  getSexIcon
}
