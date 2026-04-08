import * as fs from 'fs';
import { spawn, ChildProcessWithoutNullStreams } from 'child_process';
import * as path from 'path';
import { app, screen } from 'electron';
import type { WebContents, Display } from 'electron';

const NODE_ENV = process.env.NODE_ENV;

/** FFmpeg 可执行文件所在的相对路径 */
const ffmpegPath = '/assets/ffmpeg.exe';

/**
 * 获取应用程序所需资源的根路径
 * - 开发环境下，使用 app.getAppPath() 返回由于本地启动挂载的源代码目录。
 * - 生产环境下（打包后），通常需指向实际可执行文件所在的 resouces 目录以获取预制静态文件。
 * @returns {string} 绝对资源路径
 */
const getResourcePath = (): string => {
  let resourcePath = app.getAppPath();
  if (NODE_ENV !== 'development') {
    resourcePath = path.join(path.dirname(app.getPath('exe')), 'resources');
  }
  return resourcePath;
};

/**
 * 获取指定显示器的屏幕信息
 * @param displayId - 目标显示器 ID
 * @returns {Display} 匹配的显示器对象（包含 bounds 与 workArea）
 */
const getScreenInfo = (displayId: string | number): Display => {
  const displays = screen.getAllDisplays();
  const target = displays.find(item => item.id === displayId);
  // 防止未匹配导致后续解构报错，保底返回主显示器
  return target || screen.getPrimaryDisplay();
};

/**
 * 获取 FFmpeg 进程需要的可执行文件绝对路径。
 * @returns {string} FFmpeg 程序的完整文件路径
 */
const getFFmpegPath = (): string => {
  return path.join(getResourcePath(), ffmpegPath);
};

// ==========================================
// 状态管理变量，用于跨函数维持录制流程
// ==========================================

/**
 * FFmpeg 对应的子进程实例记录
 * 使用 ChildProcessWithoutNullStreams 确保 stdout/stderr 可监听
 */
let ffmpegProcess: ChildProcessWithoutNullStreams | null = null;

/**
 * 当前已录制的经过时间（单位：秒）
 * 记录此状态用于减少高频更新，仅在“整秒”变化时与前端通信
 */
let currentTime: number = 0;

/**
 * 与前端通信所使用的 WebContents 对象，也就是触发事件的发送者
 */
let sender: WebContents | null = null;



/**
 * 在主进程启动指定屏幕区域的视频/音频录制操作。
 * @param _sender - ipcMain 的事件通讯对象 WebContents，用于向前端传输时间进度及回传录制产物
 * @param displayId - 前端选定的显示器实例标识符
 * @param mic - 指定采用何种麦克风音频输入（可选，如果为空则纯视频录制）
 */
const startRecording = (_sender: WebContents, displayId: string | number, mic?: string): void => {
  sender = _sender;
  currentTime = 0;

  // 1. 设置被录制视频数据的本地文件保存路径
  // 考虑到后续的文件写入，必须保证上层目录 'C:/Users/Administrator/.easymeeting/' 存在。
  let filePath = 'C:/Users/Muy/.easymeeting/';
  // 通过时间戳保证文件名独一无二。此处会首先带上 `_temp` 后缀以表明录制尚未封包完成。
  filePath = path.join(filePath, `${new Date().getTime()}_temp.mp4`);
  // 考虑到命令行工具对 Windows 分隔符可能水土不服，替换反斜杠为通用正斜杠
  filePath = filePath.replace(/\\/g, '/');

  // 2. 获取所需捕获屏幕的矩形区域与实际操作区域 (原代码逻辑)
  const { bounds, workArea } = getScreenInfo(displayId);
  console.log('取得的待录制屏幕边距参数:', bounds, workArea);

  const ffmpeg = getFFmpegPath();

  // 3. 构建抓取流的基础 FFmpeg 指令配置数组
  let args: string[] = [
    // --- 视频源设定 ---
    '-f', 'gdigrab',             // 使用系统自带 GDI 工具抓取画面
    '-draw_mouse', '1',          // 1 代表保留展示并录制鼠标光标
    '-framerate', '30',          // 捕获屏幕使用的预设帧速率 30 fps
    '-offset_x', `${bounds.x}`,  // 基于抓取设备设定原点截取的 X 轴起始偏置
    '-offset_y', '0',            // 目前固定为 0，若有副屏特殊情况后期可再动态设置
    '-video_size', `${workArea.width}x${workArea.height}`, // 限制捕获并参与重编码的矩形长宽
    '-i', 'desktop',             // 设置桌面为上一步 gdigrab 的信号输入

    // --- 音频源占位设定 ---
                    // 指定多媒体框架 DirectShow 用于接入音频设备
  ];

  // 4. 追加麦克风音频输入设备定义（前提是用户或端上传递了有效麦克风名称）
  if (mic) {
    args.push('-f', 'dshow', '-i', `audio=${mic}`);
  }

  // 5. 追加最终输出流的压缩、编码以及避免文件损坏的核心配置
  const otherArgs: string[] = [
    // ---------------- 视频输出格式 ----------------
    '-c:v', 'libx264',           // 视频转码器变更为主流的 H.264
    '-preset', 'ultrafast',      // 使用极速模式压缩，减轻录制中占用系统和 CPU 开销
    '-crf', '18',                // 图像质量系数 18 (较低的 CRF 值会提供极高的视觉保真度)
    '-g', '60',                  // 设置强制发送关键帧的数据间隔 (每 60 帧一个核心关键帧，即 2 秒)
    '-x264-params', 'nal-hrd=cbr:force-cfr=1', // 基于网络传输兼容性强制固定码率封装

    // ---------------- 音频输出格式 ----------------
    '-c:a', 'aac',               // 采用高级音频编码 AAC 来封装音频数据
    '-b:a', '192k',              // 给定充足的高品质音频位率 192k
    '-ar', '44100',              // 取样频率给定为标准的 44100 Hz
    '-ac', '2',                  // 指定立体声声场宽度 (通常为 2。若报错可降级至单声道 1)

    // ---------------- 播放容忍度配置 --------------
    '-pix_fmt', 'yuv420p',       // 设置通用播放器兼容和流媒体兼容性最好的色彩像素排布形式

    // ---------------- 安全与抗损机制 --------------
    // 通过 fragmented MP4 让实时封包无需等待到结束时一次性写首部 moov, 降低录存意外断死文件风险
    '-movflags', 'frag_keyframe+empty_moov+faststart',
    '-flush_packets', '1',       // 流传输包立刻交接刷新
    '-fflags', '+genpts',        // 迫使底层引擎自主产生音视频对拍的时间戳
    '-max_interleave_delta', '0',// 禁止长延迟造成的音视频流切层混用交错

    // ----------- 执行目的路径（最后一条） -----------
    filePath
  ];

  // 数组并集
  args = args.concat(otherArgs);

  // 6. 执行启动命令行（与当前主进程生命阶段互相独立解耦）
  ffmpegProcess = spawn(ffmpeg, args, {
    stdio: ['ignore', 'pipe', 'pipe'], // 切断不必要的 stdin，而留设 stdout/stderr 用于通讯
    detached: true                     // 组建独立的外部执行进程环境
  });

  // 7. 处理由 FFmpeg 不停吐出的转码时长并响应回界面前端
  if (ffmpegProcess.stderr) {
    ffmpegProcess.stderr.on('data', (data: Buffer | string) => {
      const output = data.toString();
      // 在这里如果不想终端刷屏可以注释掉下行打印。保留原版：
      // console.log(output);

      // 通过正则检索如 time=00:00:23.44 字样，从中提出正在处理的时常
      const timeMatch = output.match(/time=(\S+)/);
      if (timeMatch && timeMatch[1]) {
        const seconds = parseTime(timeMatch[1]);
        if (seconds > currentTime) {
          // 当前时间大于前一个整秒时，说明进度真正往前推进了一秒，安全地发给呈现端
          if (sender && !sender.isDestroyed()) {
            sender.send('recordTime', seconds);
          }
          currentTime = seconds;
        }
      }
    });
  }

  // 8. 异常捕获以保证底层不被致命错误挂盘
  ffmpegProcess.on('error', (err: Error) => {
    console.error('ffmpeg 启动阶段或者流媒体捕获期间遭遇失败', err);
    ffmpegProcess = null;
  });

  // 9. 捕捉进程由于人工关闭或者各种原因中断的事件
  ffmpegProcess.on('exit', () => {
    ffmpegProcess = null;
    // 进程退出后触发视频修复（由片段型转换清理出完整的普通 MP4 文件以结束使命）
    repairVideo(filePath);
  });
};

/**
 * 等待录屏最终结束后执行修补转换动作。
 * 作用是去除由于断电保障（fragment）生成的奇怪封装头，令最后归档版本变得标准化。
 * @param filePath - 包括 `_temp` 临时结尾的工作区素材文件路径
 */
const repairVideo = (filePath: string): void => {
  const ffmpeg = getFFmpegPath();
  const finalFilePath = filePath.replace('_temp', ''); // 剔除后缀作为产物全名

  // 此处尊重原版保持相同逻辑。但注意未指定 `-c copy` 的情况下，FFmpeg 有可能将其进行较慢的低效重压缩。
  const args = [
    '-i', filePath,
    finalFilePath
  ];

  const process = spawn(ffmpeg, args, {
    stdio: ['ignore', 'pipe', 'pipe'], // 捕获并忽略其他输出通道
    detached: true                     // 仍然需要独立子进程维持任务以防 Electron 终止受牵连
  });

  // 捕捉错误消息
  process.on('error', (error: Error) => {
    console.error('修复视频过程阶段检测到崩溃:', error);
  });

  // 在其成功之后擦拭战场并上报前端
  process.on('exit', (code: number | null) => {
    if (code === 0) {
      try {
        // 修补成功（code===0）后，安全地删除不再需要的临时残留媒体垃圾
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath);
        }
      } catch (err) {
        console.error('尝试清除过时的残留影片素材时遇到异常:', err);
      }

      // IPC 将完整的封装文件相对路径返回去让渲染程序可定位展现或者提供下载
      if (sender && !sender.isDestroyed()) {
        sender.send('finishRecording', finalFilePath);
      }
    } else {
      console.error(`视频修复流终止，可能文件发生损坏或被意外中断，结束代号: ${code}`);
    }
  });
};

/**
 * 对 FFmpeg 给出的长文本流时间转换成好用的整数值用作前端处理
 * @param timeStr - 获取的具体分表格式化字串类似于 HH:MM:SS 或带毫秒
 * @returns {number} 取余处理完过后的整秒数
 */
const parseTime = (timeStr: string): number => {
  const parts = timeStr.split(':');
  let seconds = 0;
  if (parts.length === 3) {
    // 根据标准的时：分：秒分别求值计算汇总为以秒做单位的值
    const hours = parseInt(parts[0], 10);
    const minutes = parseInt(parts[1], 10);
    const secs = parseInt(parts[2].split('.')[0], 10); // 小数点后的毫秒截掉处理

    seconds = hours * 3600 + minutes * 60 + secs;
  }
  return seconds;
};

/**
 * 提前干预结束屏幕录制，结束当前工作状态
 * 利用操作系统级的 SIGINT 信号温和终止。
 * FFmpeg 接收到 SIGINT 将自动正确闭合 mp4 输出。
 */
const stopRecording = (): void => {
  if (ffmpegProcess) {
    ffmpegProcess.kill('SIGINT');
  }
};

export {
  startRecording,
  stopRecording
};
