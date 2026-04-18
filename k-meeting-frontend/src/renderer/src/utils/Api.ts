/**
 * API 接口地址定义
 */
const Api = {
  // --- 用户与账户 ---
  addMeeting: '/meeting/join', // 加入会议
  checkCode: '/user/checkCode', // 验证码
  login: '/user/login', // 登录
  register: '/user/register', // 注册
  logout: '/user/logout', // 退出
  updatePassword: '/user/changePassword', // 修改密码
  getSysSetting: '/account/getSysSetting', // 获取系统设置
  updateUserInfo: '/user/update', // 更新用户信息

  // --- 文件与资源 ---
  getResource: '/api/file/getResource', // 资源
  getAvatar: '/api/file/getAvatar', // 获取头像
  uploadChatFile: '/api/chat/uploadFile', // 上传聊天资源
  downloadFile: '/api/file/downloadFile', // 下载文件

  // --- 会议相关 ---
  quickMeeting: '/meeting/quick', // 快速会议
  preJoinMeeting: '/meeting/preJoin', // 预加入会议
  joinMeeting: '/meeting/join', // 加入会议
  exitMeeting: '/meeting/exit', // 退出会议
  inviteMember: '/meeting/inviteMember', // 邀请成员
  acceptInvite: '/meeting/accept', // 接受邀请
  finishMeeting: '/meeting/finish', // 结束会议
  kickOutMeeting: '/meeting/kickOut', // 踢出会议
  blackMeeting: '/meeting/black', // 拉黑
  loadMeeting: '/meeting/loadMeeting', // 历史会议
  loadMeetingMembers: '/meeting/loadMeetingMembers', // 会议成员
  getCurrentMeeting: '/meeting/getCurrentMeeting', // 获取会议信息
  delMeetingRecord: '/meeting/delMeetingRecord', // 删除会议记录
  reserveJoinMeeting: '/meeting/reserveJoin', // 预约加入会议
  sendOpenVideoChangeMessage: '/meeting/sendOpenVideoChangeMessage', // 用户开启关闭摄像头

  // --- 聊天与消息 ---
  sendChatMessage: '/chat/sendMessage', // 发送聊天消息
  loadMessage: '/chat/loadMessage', // 聊天消息
  loadHistoryMessage: '/chat/loadHistoryMessage', // 获取历史聊天记录

  // --- 联系人相关 ---
  searchContact: '/contact/search', // 搜索联系人
  contactApply: '/contact/apply', // 申请联系人
  loadContactUser: '/contact/load', // 获取联系人
  loadContactApply: '/contact/loadContactApply', // 申请列表
  dealWithApply: '/contact/deal', // 处理联系人
  delContact: '/contact/delete', // 删除联系人
  loadContactApplyDealWithCount: '/contact/loadContactApplyCount', // 获取未处理的联系人数

  // --- 会议预约 ---
  createMeetingReserve: '/meetingReserve/createMeetingReserve', // 会议预约
  loadMeetingReserve: '/meetingReserve/loadMeetingReserve', // 预约的会议
  loadTodayMeeting: '/meetingReserve/loadTodayMeeting', // 今天待开会议
  delMeetingReserveByUser: '/meetingReserve/delMeetingReserveByUser', // 用户删除会议
  delMeetingReserve: '/meetingReserve/delMeetingReserve', // 创建人删除会议

  // --- 管理后台 ---
  loadUserList: '/admin/loadUserList', // 用户列表
  updateUserStatus: '/admin/updateUserStatus', // 修改用户状态
  forceOffLine: '/admin/forceOffLine', // 强制下线
  loadUpdateDataList: '/admin/loadUpdateDataList', // 获取更新列表
  delUpdate: '/admin/delUpdate', // 删除更新
  saveUpdate: '/admin/saveUpdate', // 保存更新
  postUpdate: '/admin/postUpdate', // 发布更新
  getSysSetting4Admin: '/admin/getSysSetting', // 管理员获取系统设置
  saveSysSetting: '/admin/saveSysSetting', // 保存设置
  loadAdminMeeting: '/admin/loadAdminMeeting', // 获取会议
  adminFinishMeeting: '/admin/adminFinishMeeting', // 管理员结束会议

  // --- 系统更新 ---
  checkVersion: '/update/checkVersion', // 更新检测
  downloadUpdate: '/api/update/download' // 下载更新
} as const

// 导出 Api 类型供其他地方使用（可选）
export type ApiType = typeof Api

export { Api }
