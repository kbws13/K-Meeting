import { DownloadUpdatePayload } from '@model/ipc'
import store from './store'
import { AppUserInfo } from '@model/user'
import axios, { AxiosProgressEvent, AxiosRequestConfig } from 'axios'
import { getWindow } from './windowProxy'
import { shell } from 'electron'
import os from 'os'
import fs from 'fs'
import { basename, join } from 'path'
import { pipeline } from 'stream/promises'

const userDir = os.homedir()

const downloadUpdate = async ({ id, downloadUrl, fileName }: DownloadUpdatePayload) => {
  try {
    const token = store.getData<AppUserInfo>('userInfo')?.token
    const headers: Record<string, string> = {}
    if (token) {
      headers.token = token
    }

    const resolvedDownloadUrl = resolveDownloadUrl(downloadUrl, id)
    const config = {
      responseType: 'stream',
      headers,
      onDownloadProgress: (progressEvent: AxiosProgressEvent) => {
        notifyMainWindow('updateDownloadCallback', progressEvent.loaded)
      }
    } as AxiosRequestConfig

    const response = await axios.get<NodeJS.ReadableStream>(resolvedDownloadUrl, config)
    const resolvedFileName = resolveFileName(response.headers['content-disposition'], fileName)
    const localFile = join(userDir, resolvedFileName)
    const stream = fs.createWriteStream(localFile)

    await pipeline(response.data, stream)

    const openResult = await shell.openPath(localFile)
    if (openResult) {
      throw new Error(openResult)
    }

    notifyMainWindow('updateDownloadFinished', `安装包已下载完成：${resolvedFileName}`)
  } catch (error) {
    console.error('download update failed:', error)
    notifyMainWindow('updateDownloadError', getErrorMessage(error))
  }
}

const resolveDownloadUrl = (downloadUrl: string, id: number | string): string => {
  const rawUrl = downloadUrl?.trim()
  if (!rawUrl) {
    throw new Error('更新下载地址为空')
  }

  const url = /^https?:\/\//i.test(rawUrl)
    ? new URL(rawUrl)
    : buildRelativeDownloadUrl(rawUrl)

  if (!url.searchParams.has('id')) {
    url.searchParams.set('id', String(id))
  }

  return url.toString()
}

const buildRelativeDownloadUrl = (downloadUrl: string): URL => {
  const requestBaseUrl = getRequestBaseUrl()
  if (!requestBaseUrl) {
    throw new Error('无法解析更新下载地址，请检查前端域名配置')
  }

  const normalizedPath = downloadUrl.replace(/^\/+/, '')
  if (normalizedPath.startsWith('api/')) {
    return new URL(normalizedPath, `${ensureTrailingSlash(requestBaseUrl)}`)
  }

  return new URL(normalizedPath, `${ensureTrailingSlash(getApiBaseUrl(requestBaseUrl))}`)
}

const getRequestBaseUrl = (): string | null => {
  const rendererUrl = process.env['ELECTRON_RENDERER_URL']
  if (rendererUrl) {
    return new URL(rendererUrl).origin
  }

  const domain = import.meta.env.VITE_DOMAIN?.trim()
  if (domain) {
    return domain.replace(/\/+$/, '')
  }

  return null
}

const getApiBaseUrl = (requestBaseUrl: string): string => {
  return requestBaseUrl.endsWith('/api') ? requestBaseUrl : `${requestBaseUrl}/api`
}

const ensureTrailingSlash = (value: string): string => {
  return value.endsWith('/') ? value : `${value}/`
}

const resolveFileName = (
  contentDisposition: string | string[] | undefined,
  fallbackFileName?: string
): string => {
  const disposition = Array.isArray(contentDisposition) ? contentDisposition[0] : contentDisposition
  const parsedFileName = parseFileNameFromDisposition(disposition)
  return basename(parsedFileName || fallbackFileName || 'K-Meeting-Update.exe')
}

const parseFileNameFromDisposition = (contentDisposition?: string): string | null => {
  if (!contentDisposition) {
    return null
  }

  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].trim().replace(/^"(.*)"$/, '$1'))
  }

  const normalMatch = contentDisposition.match(/filename\s*=\s*"?([^";]+)"?/i)
  if (normalMatch?.[1]) {
    return normalMatch[1].trim()
  }

  return null
}

const notifyMainWindow = (
  channel: 'updateDownloadCallback' | 'updateDownloadError' | 'updateDownloadFinished',
  payload: number | string
): void => {
  const mainWindow = getWindow('main')
  if (!mainWindow) {
    return
  }

  mainWindow.webContents.send(channel, payload)
}

const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const responseMessage =
      typeof error.response?.data === 'string' && error.response.data.trim()
        ? error.response.data
        : undefined
    return responseMessage || error.message || '下载更新失败'
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return '下载更新失败'
}

export { downloadUpdate }
