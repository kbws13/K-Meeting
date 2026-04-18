import * as fs from 'fs'
import { spawn } from 'child_process'
import * as path from 'path'
import type { Display, WebContents } from 'electron'
import { app, screen } from 'electron'
import { getSysSetting } from './sysSetting'

const NODE_ENV = process.env.NODE_ENV

/** FFmpeg 可执行文件所在的相对路径 */
const ffmpegPath = '/assets/ffmpeg.exe'

const getResourcePath = (): string => {
  let resourcePath = app.getAppPath()
  if (NODE_ENV !== 'development') {
    resourcePath = path.join(path.dirname(app.getPath('exe')), 'resources')
  }
  return resourcePath
}

const getScreenInfo = (displayId: string | number): Display => {
  const displays = screen.getAllDisplays()
  const target = displays.find((item) => String(item.id) === String(displayId))
  return target || screen.getPrimaryDisplay()
}

const getFFmpegPath = (): string => {
  return path.join(getResourcePath(), ffmpegPath)
}

let ffmpegProcess: ReturnType<typeof spawn> | null = null
let currentTime = 0
let sender: WebContents | null = null

const startRecording = (_sender: WebContents, displayId: string | number, mic?: string): void => {
  sender = _sender
  currentTime = 0

  const { screencapFolder } = getSysSetting()
  fs.mkdirSync(screencapFolder, { recursive: true })

  let filePath = path.join(screencapFolder, `${Date.now()}_temp.mp4`)
  filePath = filePath.replace(/\\/g, '/')

  const { bounds, workArea } = getScreenInfo(displayId)
  console.log('取得的待录制屏幕边距参数:', bounds, workArea)

  const ffmpeg = getFFmpegPath()

  let args: string[] = [
    '-f',
    'gdigrab',
    '-draw_mouse',
    '1',
    '-framerate',
    '30',
    '-offset_x',
    `${bounds.x}`,
    '-offset_y',
    `${bounds.y}`,
    '-video_size',
    `${workArea.width}x${workArea.height}`,
    '-i',
    'desktop'
  ]

  if (mic) {
    args.push('-f', 'dshow', '-i', `audio=${mic}`)
  }

  const otherArgs: string[] = [
    '-c:v',
    'libx264',
    '-preset',
    'ultrafast',
    '-crf',
    '18',
    '-g',
    '60',
    '-x264-params',
    'nal-hrd=cbr:force-cfr=1',
    '-c:a',
    'aac',
    '-b:a',
    '192k',
    '-ar',
    '44100',
    '-ac',
    '2',
    '-pix_fmt',
    'yuv420p',
    '-movflags',
    'frag_keyframe+empty_moov+faststart',
    '-flush_packets',
    '1',
    '-fflags',
    '+genpts',
    '-max_interleave_delta',
    '0',
    filePath
  ]

  args = args.concat(otherArgs)

  const process = spawn(ffmpeg, args, {
    stdio: ['ignore', 'pipe', 'pipe'], // 切断不必要的 stdin，而留设 stdout/stderr 用于通讯
    detached: true // 组建独立的外部执行进程环境
  })
  ffmpegProcess = process

  process.stderr?.on('data', (data: Buffer | string) => {
    const output = data.toString()
    const timeMatch = output.match(/time=(\S+)/)
    if (timeMatch && timeMatch[1]) {
      const seconds = parseTime(timeMatch[1])
      if (seconds > currentTime) {
        if (sender && !sender.isDestroyed()) {
          sender.send('recordTime', seconds)
        }
        currentTime = seconds
      }
    }
  })

  process.on('error', (err: Error) => {
    console.error('ffmpeg 启动阶段或者流媒体捕获期间遭遇失败', err)
    ffmpegProcess = null
  })

  process.on('exit', () => {
    ffmpegProcess = null
    repairVideo(filePath)
  })
}

const repairVideo = (filePath: string): void => {
  const ffmpeg = getFFmpegPath()
  const finalFilePath = filePath.replace('_temp', '')

  const args = ['-i', filePath, finalFilePath]

  const process = spawn(ffmpeg, args, {
    stdio: ['ignore', 'pipe', 'pipe'], // 捕获并忽略其他输出通道
    detached: true // 仍然需要独立子进程维持任务以防 Electron 终止受牵连
  })

  process.on('error', (error: Error) => {
    console.error('修复视频过程阶段检测到崩溃:', error)
  })

  process.on('exit', (code: number | null) => {
    if (code === 0) {
      try {
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath)
        }
      } catch (err) {
        console.error('尝试清除过时的残留影片素材时遇到异常:', err)
      }
      if (sender && !sender.isDestroyed()) {
        sender.send('finishRecording', finalFilePath)
      }
    } else {
      console.error(`视频修复流终止，可能文件发生损坏或被意外中断，结束代号: ${code}`)
    }
  })
}

const parseTime = (timeStr: string): number => {
  const parts = timeStr.split(':')
  let seconds = 0
  if (parts.length === 3) {
    const hours = parseInt(parts[0], 10)
    const minutes = parseInt(parts[1], 10)
    const secs = parseInt(parts[2].split('.')[0], 10)

    seconds = hours * 3600 + minutes * 60 + secs
  }
  return seconds
}

const stopRecording = (): void => {
  if (ffmpegProcess) {
    ffmpegProcess.kill('SIGINT')
  }
}

export { startRecording, stopRecording }
