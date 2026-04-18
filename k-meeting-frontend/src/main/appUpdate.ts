import { DownloadUpdatePayload } from '@model/ipc'
import store from './store'
import { AppUserInfo } from '@model/user'
import axios, { AxiosProgressEvent, AxiosRequestConfig } from 'axios'
import { getWindow } from './windowProxy'
import os from 'os'
import fs from 'fs'
import { exec } from 'child_process'

const userDir = os.homedir()

const downloadUpdate = async ({ id, downloadUrl, fileName }: DownloadUpdatePayload) => {
  const token = store.getData<AppUserInfo>('userInfo')?.token
  const config = {
    responseType: 'stream',
    headers: {
      'Content-Type': 'multipart/form-data',
      token: token
    },
    onDownloadProgress: (progressEvent: AxiosProgressEvent) => {
      const loaded = progressEvent.loaded
      const mainWindow = getWindow('main')
      if (!mainWindow) return
      mainWindow.webContents.send('updateDownloadCallback', loaded)
    }
  } as AxiosRequestConfig
  const response = await axios.post(downloadUrl, { id }, config)
  const localFile = userDir + '/' + fileName
  const stream = fs.createWriteStream(localFile)
  response.data.pipe(stream)
  stream.on('finish', () => {
    stream.close()
    const command = `${localFile}`
    execCommand(command)
  })
}

const execCommand = (command: string) => {
  return new Promise((resolve) => {
    exec(command, (_, stdout, __) => {
      resolve(stdout)
    })
  })
}

export { downloadUpdate }
