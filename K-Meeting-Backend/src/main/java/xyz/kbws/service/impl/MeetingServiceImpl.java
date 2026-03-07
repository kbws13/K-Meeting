package xyz.kbws.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.enums.MeetingStatusEnum;
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.model.enums.MessageTypeEnum;
import xyz.kbws.model.obj.MeetingJoinObj;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.service.MeetingService;
import xyz.kbws.mapper.MeetingMapper;
import org.springframework.stereotype.Service;
import xyz.kbws.websocket.ChannelContextUtil;

import javax.annotation.Resource;
import java.util.List;

/**
* @author housenyao
* @description 针对表【meeting(会议表)】的数据库操作Service实现
* @createDate 2025-06-28 18:08:22
*/
@Service
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper, Meeting>
    implements MeetingService{
    
    @Resource
    private MeetingMapper meetingMapper;
    
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
    public void join(UserVO userVO, String meetingId, Boolean openVideo) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || MeetingStatusEnum.FINISHED.getValue().equals(meeting.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 校验用户
        // 加入成员
        // 加入会议
        // 加入 ws 房间
        channelContextUtil.addMeetingRoom(meetingId, String.valueOf(userVO.getId()));
        // 发生 ws 消息
        MeetingJoinObj meetingJoinObj = new MeetingJoinObj();
        meetingJoinObj.setMeetingMemberList(null);
        meetingJoinObj.setNewMember(null);
        MessageSendDto<MeetingJoinObj> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageType(MessageTypeEnum.ADD_MEETING_ROOM.getValue());
        messageSendDto.setMessageContent(meetingJoinObj);
        messageSendDto.setMeetingId(meetingId);
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        channelContextUtil.sendMessage(messageSendDto);
    }
}




