import os from 'os'
import fs from 'fs'
import path from 'path'
import type { SysSetting } from '@model/system'
import store from './store'

const userDir = os.homedir()
const localFolder = path.join(userDir, '.easymeeting')
const defaultScreencapFolder = localFolder.endsWith(path.sep)
  ? localFolder
  : `${localFolder}${path.sep}`
const defaultSysSetting: SysSetting = {
  openCamera: true,
  openMic: true,
  screencapFolder: defaultScreencapFolder
}

if (!fs.existsSync(localFolder)) {
  fs.mkdirSync(localFolder, { recursive: true })
}

const getConfigFilePath = (): string => {
  return path.join(localFolder, String(store.getUserId() ?? 'null'))
}

const saveSysSetting = (sysSetting: string | SysSetting): void => {
  try {
    const serialized = typeof sysSetting === 'string' ? sysSetting : JSON.stringify(sysSetting)
    fs.writeFileSync(getConfigFilePath(), serialized, 'utf8')
  } catch (error) {
    console.error('保存系统设置失败:', error)
  }
}

const getSysSetting = (): SysSetting => {
  const configFile = getConfigFilePath()

  if (!fs.existsSync(configFile)) {
    return defaultSysSetting
  }

  try {
    const content = fs.readFileSync(configFile, 'utf8')
    const parsed = JSON.parse(content) as Partial<SysSetting>
    return {
      ...defaultSysSetting,
      ...parsed
    }
  } catch (error) {
    console.error('读取或解析配置文件失败，返回默认设置:', error)
    return defaultSysSetting
  }
}

export { saveSysSetting, getSysSetting }
