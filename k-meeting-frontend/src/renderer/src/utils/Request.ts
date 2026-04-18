import axios, {
  type AxiosInstance,
  type AxiosProgressEvent,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElLoading } from 'element-plus'
import Message from '../utils/Message'
import router from '@/router'

/**
 * 接口返回的基础数据结构
 */
interface ResponseData<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 扩展 Axios 请求配置，添加自定义属性
 */
interface CustomRequestConfig extends InternalAxiosRequestConfig {
  showLoading?: boolean
  showError?: boolean
  errorCallback?: (responseData: ResponseData) => void
}

/**
 * request 函数接收的参数配置
 */
interface RequestParams {
  url: string
  method?: 'get' | 'post'
  params?: Record<string, unknown>
  dataType?: 'json' | string
  showLoading?: boolean
  responseType?: 'json' | 'blob' | 'arraybuffer'
  showError?: boolean
  errorCallback?: (responseData: ResponseData) => void
  uploadProgressCallback?: (event: AxiosProgressEvent) => void
}

const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
const responseTypeJson = 'json'

let loading: ReturnType<typeof ElLoading.service> | null = null

const instance: AxiosInstance = axios.create({
  withCredentials: true,
  baseURL: `${import.meta.env.PROD ? (import.meta.env.VITE_DOMAIN ?? '') : ''}/api`,
  timeout: 10 * 1000
})

// 请求前拦截器
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const customConfig = config as CustomRequestConfig
    if (customConfig.showLoading) {
      loading = ElLoading.service({
        lock: true,
        text: '加载中......',
        background: 'rgba(0, 0, 0, 0.7)'
      })
    }
    return config
  },
  (error) => {
    if (error.config?.showLoading && loading) {
      loading.close()
    }
    Message.error('请求发送失败')
    return Promise.reject('请求发送失败')
  }
)

// 请求后拦截器
instance.interceptors.response.use(
  async (response: AxiosResponse) => {
    const config = response.config as CustomRequestConfig
    const { showLoading, errorCallback, showError = true, responseType } = config

    if (showLoading && loading) {
      loading.close()
    }

    const responseData = response.data

    // 处理二进制数据
    if (responseType === 'arraybuffer' || responseType === 'blob') {
      return responseData
    }

    // 正常请求
    if (responseData.code === 0) {
      return responseData
    } else if (responseData.code === 901) {
      // 登录超时
      await window.electron.ipcRenderer.invoke('logout')
      router.push('/')
      return Promise.reject({ showError: false })
    } else {
      // 其他业务错误
      if (errorCallback) {
        errorCallback(responseData)
      }
      return Promise.reject({ showError: showError, msg: responseData.message })
    }
  },
  (error) => {
    const config = error.config as CustomRequestConfig
    if (config?.showLoading && loading) {
      loading.close()
    }
    return Promise.reject({ showError: true, msg: '网络异常' })
  }
)

/**
 * 封装的请求函数
 */
const request = <T = unknown>(config: RequestParams): Promise<ResponseData<T> | null> => {
  const {
    url,
    method = 'post',
    params,
    dataType,
    showLoading = true,
    responseType = responseTypeJson,
    showError = true
  } = config

  const requestMethod = method.toLowerCase()
  const isJson = dataType != null && dataType === 'json'
  const contentType = isJson ? contentTypeJson : contentTypeForm

  // JSON 模式直接传原始对象；表单模式使用 FormData
  let requestBody: FormData | Record<string, unknown> | undefined
  if (requestMethod !== 'get') {
    if (isJson) {
      requestBody = params
    } else {
      const formData = new FormData()
      if (params) {
        for (const key in params) {
          const value = params[key]
          formData.append(key, value == null ? '' : value instanceof Blob ? value : String(value))
        }
      }
      requestBody = formData
    }
  }

  const userInfoJson = localStorage.getItem('userInfo')
  const token = userInfoJson ? JSON.parse(userInfoJson).token : ''

  const headers: Record<string, string> = {
    'X-Requested-With': 'XMLHttpRequest',
    token: token
  }
  if (requestMethod !== 'get') {
    headers['Content-Type'] = contentType
  }

  const requestConfig = {
    params: requestMethod === 'get' ? params : undefined,
    onUploadProgress:
      requestMethod === 'get'
        ? undefined
        : (event) => {
            if (config.uploadProgressCallback) {
              config.uploadProgressCallback(event)
            }
          },
    responseType: responseType as AxiosRequestConfig['responseType'],
    headers: headers,
    showLoading: showLoading,
    errorCallback: config.errorCallback,
    showError: showError
  } as AxiosRequestConfig & CustomRequestConfig

  const requestPromise: Promise<ResponseData<T>> =
    requestMethod === 'get'
      ? (instance.get(url, requestConfig) as Promise<ResponseData<T>>)
      : (instance.post(url, requestBody, requestConfig) as Promise<ResponseData<T>>)

  return requestPromise.catch((error: { showError?: boolean; msg?: string }) => {
    if (error.showError) {
      Message.error(error.msg ?? '请求失败')
    }
    return null
  })
}

export default request
