package xyz.kbws.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.MeetingMapper;
import xyz.kbws.mapper.MeetingReserveMapper;
import xyz.kbws.mapper.MeetingReserveMemberMapper;
import xyz.kbws.mapper.MeetingmemberMapper;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.*;
import xyz.kbws.model.enums.*;
import xyz.kbws.model.obj.MeetingExitObj;
import xyz.kbws.model.obj.MeetingInviteObj;
import xyz.kbws.model.obj.MeetingJoinObj;
import xyz.kbws.model.obj.MeetingMemberObj;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.MeetingMemberService;
import xyz.kbws.service.MeetingService;
import xyz.kbws.service.UserContactService;
import xyz.kbws.websocket.ChannelContextUtil;
import xyz.kbws.websocket.message.MessageHandler;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author housenyao
 * @description 针对表【meeting(会议表)】的数据库操作Service实现
 * @createDate 2025-06-28 18:08:22
 */
@Service
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper, Meeting>
        implements MeetingService {

    @Resource
    private MeetingMapper meetingMapper;

    @Resource
    private MeetingMemberService meetingMemberService;

    @Resource
    private MeetingmemberMapper meetingmemberMapper;

    @Resource
    private MeetingReserveMapper meetingReserveMapper;

    @Resource
    private MeetingReserveMemberMapper meetingReserveMemberMapper;

    @Resource
    private UserContactService userContactService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private ChannelContextUtil channelContextUtil;

    @Resource
    private MessageHandler messageHandler;

    @Override
    public Page<Meeting> findByPage(MeetingQuery meetingQuery) {
        Page<Meeting> page = new Page<>(meetingQuery.getCurrent(), meetingQuery.getPageSize());
        List<Meeting> records = meetingMapper.findListByPage(page, meetingQuery);
        page.setRecords(records);
        return page;
    }

    @Override
    public void quickMeeting(Meeting meeting, String nickName) {
        meeting.setId(Integer.valueOf(RandomUtil.randomNumbers(9)));
        meeting.setStatus(MeetingStatusEnum.PENDING.getValue());
        this.save(meeting);
    }

    @Override
    public void join(LoginUser loginUser, Integer meetingId, Boolean openVideo) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || MeetingStatusEnum.FINISHED.getValue().equals(meeting.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 校验用户
        this.checkMeetingJoin(meetingId, loginUser.getUserId());
        // 加入成员
        MemberTypeEnum memberTypeEnum = meeting.getCreateUserId().equals(loginUser.getUserId()) ? MemberTypeEnum.COMPERE : MemberTypeEnum.NORMAL;
        this.addMeetingMember(meetingId, loginUser.getUserId(), loginUser.getNickName(), memberTypeEnum);
        // 加入会议
        this.addMeeting(meetingId, loginUser.getUserId(), loginUser.getNickName(), loginUser.getSex(), memberTypeEnum, openVideo);
        // 加入 ws 房间
        channelContextUtil.addMeetingRoom(meetingId, loginUser.getUserId());
        // 发生 ws 消息
        MeetingJoinObj meetingJoinObj = new MeetingJoinObj();
        meetingJoinObj.setNewMember(redisComponent.getMeetingMember(meetingId, loginUser.getUserId()));
        meetingJoinObj.setMeetingMemberList(redisComponent.getMeetingMemberList(meetingId));
        MessageSendDto<MeetingJoinObj> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageType(MessageTypeEnum.ADD_MEETING_ROOM.getValue());
        messageSendDto.setMessageContent(meetingJoinObj);
        messageSendDto.setMeetingId(meetingId);
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        messageHandler.sendMessage(messageSendDto);
    }

    @Override
    public Integer preJoinMeeting(Integer meetingNo, LoginUser loginUser, String password) {
        LambdaQueryWrapper<Meeting> qw = new LambdaQueryWrapper<>();
        qw.eq(Meeting::getMeetingNo, meetingNo)
                .eq(Meeting::getStatus, MeetingStatusEnum.PENDING.getValue())
                .orderByDesc(Meeting::getCreateTime);
        List<Meeting> meetingList = this.list(qw);
        if (meetingList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议不存在");
        }
        Meeting meeting = meetingList.get(0);
        if (MeetingStatusEnum.FINISHED.getValue().equals(meeting.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已结束");
        }
        Integer currentMeetingId = loginUser.getCurrentMeetingId();
        if ((currentMeetingId != null && currentMeetingId != 0) && !meeting.getId().equals(currentMeetingId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你有未结束的会议无法加入其他会议");
        }
        this.checkMeetingJoin(meeting.getId(), loginUser.getUserId());

        if (MeetingJoinTypeEnum.PASSWORD.getValue().equals(meeting.getJoinType()) && !meeting.getJoinPassword().equals(password)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "入会密码错误");
        }
        loginUser.setCurrentMeetingId(meeting.getId());
        redisComponent.resetUserVO(loginUser);
        return meeting.getId();
    }

    @Override
    public Boolean exitMeetingRoom(LoginUser loginUser, MeetingMemberStatus meetingMemberStatus) {
        Integer meetingId = loginUser.getCurrentMeetingId();
        if (meetingId == null) {
            return false;
        }
        Integer userId = loginUser.getUserId();
        Boolean exit = redisComponent.exitMeeting(meetingId, userId, meetingMemberStatus);
        if (!exit) {
            loginUser.setCurrentMeetingId(null);
            redisComponent.saveUserVO(loginUser);
            return false;
        }
        // 清空当前正在进行的会议
        loginUser.setCurrentMeetingId(null);
        redisComponent.saveUserVO(loginUser);
        handleMemberExit(meetingId, userId, meetingMemberStatus);
        return true;
    }

    @Override
    public Boolean kickOutMeetingRoom(LoginUser loginUser, Integer targetUserId, MeetingMemberStatus meetingMemberStatus) {
        Integer meetingId = loginUser.getCurrentMeetingId();
        if (meetingId == null || targetUserId == null) {
            return false;
        }
        if (loginUser.getUserId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能操作自己");
        }
        Meeting meeting = this.getById(meetingId);
        if (!meeting.getCreateUserId().equals(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        LoginUser targetUser = redisComponent.getLoginUserById(targetUserId);
        return exitMeetingRoom(targetUser, meetingMemberStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean finishMeeting(Integer meetingId, Integer currentUserId) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议 ID 不能为空");
        }
        Meeting meeting = this.getById(meetingId);
        if (meeting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议不存在");
        }
        if (currentUserId != null && !meeting.getCreateUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (MeetingStatusEnum.FINISHED.getValue().equals(meeting.getStatus())) {
            return true;
        }
        meeting.setStatus(MeetingStatusEnum.FINISHED.getValue());
        meeting.setEndTime(new Date());
        this.updateById(meeting);

        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        messageSendDto.setMessageType(MessageTypeEnum.FINISH_MEETING.getValue());
        messageSendDto.setMeetingId(meetingId);
        messageHandler.sendMessage(messageSendDto);

        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setMeetingStatus(MeetingStatusEnum.FINISHED.getValue());
        meetingMember.setMeetingId(meetingId);
        meetingmemberMapper.updateByMeetingId(meetingMember);

        // 更新预约会议状态
        LambdaUpdateWrapper<MeetingReserve> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MeetingReserve::getMeetingId, meetingId)
                .set(MeetingReserve::getStatus, MeetingReserveStatusEnum.FINISHED.getValue());
        meetingReserveMapper.update(null, updateWrapper);

        List<MeetingMemberObj> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
        for (MeetingMemberObj meetingMemberObj : meetingMemberList) {
            LoginUser loginUser = redisComponent.getLoginUserById(meetingMemberObj.getUserId());
            if (loginUser == null) {
                continue;
            }
            loginUser.setCurrentMeetingId(null);
            redisComponent.saveUserVO(loginUser);
        }

        redisComponent.removeAllMeetingMember(meetingId);
        return true;
    }

    @Override
    public void reserveJoinMeeting(Integer meetingId, LoginUser loginUser, String joinPassword) {
        Integer userId = loginUser.getUserId();
        if (loginUser.getCurrentMeetingId() != null && !meetingId.equals(loginUser.getCurrentMeetingId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "有未结束的会议");
        }
        checkMeetingJoin(meetingId, userId);
        MeetingReserve meetingReserve = this.meetingReserveMapper.selectById(meetingId);
        if (meetingReserve == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        LambdaQueryWrapper<MeetingReserveMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MeetingReserveMember::getMeetingId, meetingId)
                .eq(MeetingReserveMember::getInviteUserId, userId);
        MeetingReserveMember member = meetingReserveMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (MeetingJoinTypeEnum.PASSWORD.getValue().equals(meetingReserve.getJoinType()) && !meetingReserve.getJoinPassword().equals(joinPassword)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "入会密码错误");
        }
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            meeting = new Meeting();
            meeting.setId(meetingId);
            meeting.setMeetingNo(RandomUtil.randomNumbers(6));
            meeting.setCreateUserId(meetingReserve.getCreateUserId());
            meeting.setName(meetingReserve.getName());
            meeting.setJoinType(meetingReserve.getJoinType());
            meeting.setJoinPassword(meetingReserve.getJoinPassword());
            Date currentDate = new Date();
            meeting.setStartTime(currentDate);
            meeting.setStatus(MeetingStatusEnum.PENDING.getValue());
            meetingMapper.insert(meeting);
        }
        loginUser.setCurrentMeetingId(meetingId);
        redisComponent.saveUserVO(loginUser);
    }

    @Override
    public void inviteMember(LoginUser loginUser, String selectContactIds) {
        List<Integer> contactIds = Arrays.stream(selectContactIds.split(",")).map(Integer::valueOf).collect(Collectors.toList());
        List<UserContact> userContacts = userContactService.listByUserId(loginUser.getUserId()).stream()
                .filter(item -> ContactStatusEnum.FRIEND.getValue().equals(item.getStatus()))
                .collect(Collectors.toList());
        List<Integer> contactIdList = userContacts.stream().map(UserContact::getContactId).collect(Collectors.toList());
        if (!new HashSet<>(contactIdList).containsAll(contactIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Meeting meeting = meetingMapper.selectById(loginUser.getCurrentMeetingId());
        for (Integer contactId : contactIds) {
            MeetingMemberObj meetingMemberObj = redisComponent.getMeetingMember(loginUser.getCurrentMeetingId(), contactId);
            if (meetingMemberObj != null && meetingMemberObj.getStatus().equals(MeetingMemberStatus.NORMAL.getStatus())) {
                continue;
            }
            redisComponent.addInviteInfo(meeting.getId(), contactId);

            MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
            messageSendDto.setMessageType(MessageTypeEnum.INVITE_MEMBER_MEETING.getValue());
            messageSendDto.setMessageSend2Type(MessageSendTypeEnum.USER.getType());
            messageSendDto.setReceiveUserId(contactId);
            MeetingInviteObj meetingInviteObj = new MeetingInviteObj();
            meetingInviteObj.setMeetingName(meeting.getName());
            meetingInviteObj.setInviteUserName(loginUser.getNickName());
            meetingInviteObj.setMeetingId(loginUser.getCurrentMeetingId());
            messageSendDto.setMessageContent(meetingInviteObj);
            messageHandler.sendMessage(messageSendDto);
        }
    }

    @Override
    public void acceptInvite(LoginUser loginUser, Integer meetingId) {
        Integer redisMeetingId = redisComponent.getInviteInfo(meetingId, loginUser.getUserId());
        if (redisMeetingId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邀请信息已过期");
        }
        loginUser.setCurrentMeetingId(redisMeetingId);
        loginUser.setCurrentNickName(loginUser.getNickName());
        redisComponent.saveUserVO(loginUser);
    }

    @Override
    public void updateMemberOpenVideo(Integer meetingId, Integer userId, Boolean openVideo) {
        MeetingMemberObj meetingMember = redisComponent.getMeetingMember(meetingId, userId);
        meetingMember.setOpenVideo(openVideo);
        redisComponent.addMeeting(meetingId, meetingMember);

        MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageType(MessageTypeEnum.MEETING_USER_VIDEO_CHANGE.getValue());
        messageSendDto.setMessageContent(openVideo);
        messageSendDto.setSendUserId(userId);
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        messageSendDto.setMeetingId(meetingId);
        messageHandler.sendMessage(messageSendDto);
    }

    private void addMeetingMember(Integer meetingId, Integer userId, String nickName, MemberTypeEnum memberType) {
        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setMeetingId(meetingId);
        meetingMember.setUserId(userId);
        meetingMember.setNickName(nickName);
        meetingMember.setLastJoinTime(new Date());
        meetingMember.setStatus(MeetingMemberStatus.NORMAL.getStatus());
        meetingMember.setMemberType(memberType.getValue());
        meetingMember.setMeetingStatus(MeetingStatusEnum.PENDING.getValue());
        meetingMemberService.save(meetingMember);
    }

    private void addMeeting(Integer meetingId, Integer userId, String nickName, Integer sex, MemberTypeEnum memberType, Boolean videoOpen) {
        MeetingMemberObj meetingMemberObj = new MeetingMemberObj();
        meetingMemberObj.setUserId(userId);
        meetingMemberObj.setNickName(nickName);
        meetingMemberObj.setJoinTime(System.currentTimeMillis());
        meetingMemberObj.setMemberType(memberType.getValue());
        meetingMemberObj.setStatus(MeetingMemberStatus.NORMAL.getStatus());
        meetingMemberObj.setOpenVideo(videoOpen);
        meetingMemberObj.setSex(sex);
        redisComponent.addMeeting(meetingId, meetingMemberObj);
    }

    private void checkMeetingJoin(Integer meetingId, Integer userId) {
        MeetingMemberObj meetingMemberObj = redisComponent.getMeetingMember(meetingId, userId);
        if (meetingMemberObj != null && MeetingMemberStatus.BLACKLIST.getStatus().equals(meetingMemberObj.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "你已被拉黑无法加入会议");
        }
    }

    private void handleMemberExit(Integer meetingId, Integer targetUserId, MeetingMemberStatus meetingMemberStatus) {
        updateMeetingMemberStatus(meetingId, targetUserId, meetingMemberStatus);

        MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageType(MessageTypeEnum.EXIT_MEETING_ROOM.getValue());

        List<MeetingMemberObj> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
        MeetingExitObj meetingExitObj = new MeetingExitObj();
        meetingExitObj.setExitUserId(targetUserId);
        meetingExitObj.setMeetingMemberObjList(meetingMemberList);
        meetingExitObj.setExitStatus(meetingMemberStatus.getStatus());

        messageSendDto.setMessageContent(JSONUtil.toJsonStr(meetingExitObj));
        messageSendDto.setMeetingId(meetingId);
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        messageHandler.sendMessage(messageSendDto);

        List<MeetingMemberObj> onLineMemberList = meetingMemberList.stream()
                .filter(item -> MeetingMemberStatus.NORMAL.getStatus().equals(item.getStatus()))
                .collect(Collectors.toList());
        if (onLineMemberList.isEmpty()) {
            MeetingReserve meetingReserve = meetingReserveMapper.selectById(meetingId);
            if (meetingReserve == null) {
                ((MeetingService) AopContext.currentProxy()).finishMeeting(meetingId, null);
                return;
            }
            if(System.currentTimeMillis() > meetingReserve.getStartTime().getTime() + meetingReserve.getDuration() * 60 * 1000) {
                ((MeetingService) AopContext.currentProxy()).finishMeeting(meetingId, null);
                return;
            }
        }
        if (ArrayUtil.contains(new Integer[]{MeetingMemberStatus.KICK_OUT.getStatus(), MeetingMemberStatus.EXIT_MEETING.getStatus()}, meetingMemberStatus)) {
            LambdaUpdateWrapper<MeetingMember> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(MeetingMember::getStatus, meetingMemberStatus.getStatus())
                    .eq(MeetingMember::getMeetingId, meetingId)
                    .eq(MeetingMember::getUserId, targetUserId);
            meetingmemberMapper.update(null, updateWrapper);
        }
    }

    private void updateMeetingMemberStatus(Integer meetingId, Integer userId, MeetingMemberStatus meetingMemberStatus) {
        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setMeetingId(meetingId);
        meetingMember.setUserId(userId);
        meetingMember.setStatus(meetingMemberStatus.getStatus());
        meetingmemberMapper.updateById(meetingMember);
    }
}




