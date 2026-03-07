package xyz.kbws.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.MeetingMapper;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.entity.MeetingMember;
import xyz.kbws.model.enums.*;
import xyz.kbws.model.obj.MeetingJoinObj;
import xyz.kbws.model.obj.MeetingMemberObj;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.MeetingMemberService;
import xyz.kbws.service.MeetingService;
import xyz.kbws.websocket.ChannelContextUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

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
    private RedisComponent redisComponent;

    @Resource
    private ChannelContextUtil channelContextUtil;

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
        channelContextUtil.addMeetingRoom(meetingId.toString(), String.valueOf(loginUser.getId()));
        // 发生 ws 消息
        MeetingJoinObj meetingJoinObj = new MeetingJoinObj();
        meetingJoinObj.setNewMember(redisComponent.getMeetingMember(meetingId, loginUser.getUserId()));
        meetingJoinObj.setMeetingMemberList(redisComponent.getMeetingMemberList(meetingId));
        MessageSendDto<MeetingJoinObj> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageType(MessageTypeEnum.ADD_MEETING_ROOM.getValue());
        messageSendDto.setMessageContent(meetingJoinObj);
        messageSendDto.setMeetingId(meetingId);
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        channelContextUtil.sendMessage(messageSendDto);
    }

    @Override
    public Integer preJoinMeeting(Integer meetingId, LoginUser loginUser, String password) {
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", meetingId)
                .eq("status", MeetingStatusEnum.PENDING.getValue())
                .orderByDesc("createTime");
        List<Meeting> meetingList = this.list(queryWrapper);
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
        this.checkMeetingJoin(meetingId, loginUser.getUserId());

        if (MeetingJoinTypeEnum.PASSWORD.getValue().equals(meeting.getJoinType()) && !meeting.getJoinPassword().equals(password)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "入会密码错误");
        }
        loginUser.setCurrentMeetingId(meetingId);
        redisComponent.resetUserVO(loginUser);
        return meetingId;
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
}




