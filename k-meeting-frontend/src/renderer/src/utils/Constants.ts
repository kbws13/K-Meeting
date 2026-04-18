// 定义文件类型枚举
enum FileCategory {
  IMAGE = 0,
  VIDEO = 1,
  FILE = 2
}

// 文件扩展名映射表
const FILE_TYPE: Record<string, FileCategory> = {
  // 图片
  jpeg: FileCategory.IMAGE,
  jpg: FileCategory.IMAGE,
  png: FileCategory.IMAGE,
  gif: FileCategory.IMAGE,
  bmp: FileCategory.IMAGE,
  webp: FileCategory.IMAGE,
  // 视频
  mp4: FileCategory.VIDEO,
  avi: FileCategory.VIDEO,
  rmvb: FileCategory.VIDEO,
  mkv: FileCategory.VIDEO,
  mov: FileCategory.VIDEO
};

/**
 * 根据文件后缀获取文件类型 ID
 * @param suffix 文件后缀名 (例如: 'jpg', 'mp4')
 * @returns 对应 FileCategory 的数值
 */
const getFileType = (suffix: string | undefined): FileCategory => {
  // 如果后缀不存在，返回默认的“文件”类型
  if (suffix === undefined || suffix === null) {
    return FileCategory.FILE;
  }

  // 转换为小写并查找映射
  const normalizedSuffix = suffix.toLowerCase();
  const fileType = FILE_TYPE[normalizedSuffix];

  // 如果找不到匹配的类型，返回默认的“文件”类型
  return fileType !== undefined ? fileType : FileCategory.FILE;
};

// 导出模块
export {
  getFileType,
  FileCategory
};
