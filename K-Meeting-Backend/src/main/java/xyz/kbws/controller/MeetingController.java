package xyz.kbws.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.annotation.CurrentUser;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.dto.meeting.JoinDto;
import xyz.kbws.model.dto.meeting.PreJoinDto;
import xyz.kbws.model.dto.meeting.QuickMeetingDto;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.entity.MeetingMember;
import xyz.kbws.model.enums.MeetingMemberStatus;
import xyz.kbws.model.enums.MeetingStatusEnum;
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.MeetingMemberService;
import xyz.kbws.service.MeetingService;
import xyz.kbws.utils.UserIdCodec;
import xyz.kbws.websocket.message.MessageHandler;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;

/**
 * @author kbws
 * @date 2025/7/5
 * @description:
 */
@Api(tags = "会议接口")
@RestController
@RequestMapping("/meeting")
public class MeetingController {

    @Resource
    private MeetingService meetingService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private MeetingMemberService meetingMemberService;

    @ApiOperation("当前正在进行的会议")
    @GetMapping("/getCurrentMeeting")
    public BaseResponse<Meeting> getCurrentMeeting(@CurrentUser LoginUser loginUser) {
        if (loginUser.getCurrentMeetingId() == null) {
            return ResultUtil.success(null);
        }
        Meeting meeting = meetingService.getById(loginUser.getCurrentMeetingId());
        if (meeting.getStatus().equals(MeetingStatusEnum.FINISHED.getValue())) {
            return ResultUtil.success(null);
        }
        return ResultUtil.success(meeting);
    }

    @ApiOperation("会议列表")
    @PostMapping("/loadMeeting")
    public BaseResponse<Page<Meeting>> loadMeeting(@RequestBody MeetingQuery meetingQuery, @CurrentUser LoginUser loginUser) {
        meetingQuery.setUserId(loginUser.getUserId());
        meetingQuery.setSortField("m.createTime");
        meetingQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        meetingQuery.setQueryMeetingMemberCount(true);

        Page<Meeting> page = meetingService.findByPage(meetingQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation("创建会议")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/quick")
    public BaseResponse<Integer> quickMeeting(@RequestBody QuickMeetingDto quickMeetingDto, @CurrentUser LoginUser loginUser) {
        if (loginUser.getCurrentMeetingId() != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您有未结束的会议，无法创建新的会议");
        }
        Meeting meeting = new Meeting();
        meeting.setMeetingNo(quickMeetingDto.getMeetingNoType() == 0 ? loginUser.getMeetingNo() : RandomUtil.randomNumbers(6));
        meeting.setName(quickMeetingDto.getMeetingName());
        meeting.setJoinType(quickMeetingDto.getJoinType());
        meeting.setJoinPassword(quickMeetingDto.getJoinPassword());
        meeting.setCreateUserId(loginUser.getUserId());
        meetingService.quickMeeting(meeting, loginUser.getNickName());

        loginUser.setCurrentMeetingId(meeting.getId());
        loginUser.setCurrentNickName(loginUser.getCurrentNickName());
        redisComponent.resetUserVO(loginUser);
        return ResultUtil.success(meeting.getId());
    }

    @ApiOperation("预加入会议")
    @PostMapping("/preJoin")
    public BaseResponse<Integer> preJoin(@RequestBody PreJoinDto preJoinDto, @CurrentUser LoginUser loginUser) {
        loginUser.setCurrentMeetingId(preJoinDto.getMeetingId());
        Integer meetingId = meetingService.preJoinMeeting(preJoinDto.getMeetingId(), loginUser, preJoinDto.getPassword());
        return ResultUtil.success(meetingId);
    }

    @ApiOperation("加入会议")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/join")
    public BaseResponse<Boolean> join(@RequestBody JoinDto joinDto, @CurrentUser LoginUser userVO) {
        meetingService.join(userVO, userVO.getCurrentMeetingId(), joinDto.getVideoOpen());
        return ResultUtil.success(true);
    }

    @ApiOperation("退出会议")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/exit")
    public BaseResponse<Boolean> exit(@CurrentUser LoginUser loginUser) {
        Boolean res = meetingService.exitMeetingRoom(loginUser, MeetingMemberStatus.EXIT_MEETING);
        return ResultUtil.success(res);
    }

    @ApiOperation("踢出会议")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/kickOut")
    public BaseResponse<Boolean> kickOut(@CurrentUser LoginUser loginUser, @NotEmpty String userId) {
        Integer targetUserId = UserIdCodec.decode(userId);
        Boolean res = meetingService.kickOutMeetingRoom(loginUser, targetUserId, MeetingMemberStatus.KICK_OUT);
        return ResultUtil.success(res);
    }

    @ApiOperation("拉黑")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/black")
    public BaseResponse<Boolean> black(@CurrentUser LoginUser loginUser, @NotEmpty String userId) {
        Integer targetUserId = UserIdCodec.decode(userId);
        Boolean res = meetingService.kickOutMeetingRoom(loginUser, targetUserId, MeetingMemberStatus.BLACKLIST);
        return ResultUtil.success(res);
    }

    @ApiOperation("结束会议")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/finish")
    public BaseResponse<Boolean> finish(@CurrentUser LoginUser loginUser, @NotEmpty String userId) {
        Integer targetUserId = UserIdCodec.decode(userId);
        Boolean res = meetingService.kickOutMeetingRoom(loginUser, targetUserId, MeetingMemberStatus.BLACKLIST);
        return ResultUtil.success(res);
    }

    @ApiOperation("删除会议记录")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/deleteRecord")
    public BaseResponse<Boolean> deleteRecord(@CurrentUser LoginUser loginUser, @NotEmpty Integer meetingId) {
        LambdaUpdateWrapper<MeetingMember> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(MeetingMember::getMeetingId, meetingId)
                .eq(MeetingMember::getUserId, loginUser.getUserId())
                .set(MeetingMember::getStatus, MeetingMemberStatus.DEL_MEETING.getStatus());
        boolean res = meetingMemberService.update(null, lambdaUpdateWrapper);
        return ResultUtil.success(res);
    }

    @ApiOperation("会议成员列表")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/members")
    public BaseResponse<List<MeetingMember>> members(@CurrentUser LoginUser loginUser, @NotEmpty Integer meetingId) {
        LambdaQueryWrapper<MeetingMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MeetingMember::getMeetingId, meetingId);
        List<MeetingMember> members = meetingMemberService.list(queryWrapper);
        Optional<MeetingMember> first = members.stream().filter(item -> item.getUserId().equals(loginUser.getUserId())).findFirst();
        if (!first.isPresent()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtil.success(members);
    }

    @ApiOperation("邀请参加会议")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/reserveJoin")
    public BaseResponse<Boolean> reserveJoin(@CurrentUser LoginUser loginUser, @NotEmpty Integer meetingId, @NotEmpty String nickName, String password) {
        loginUser.setNickName(nickName);
        meetingService.reserveJoinMeeting(meetingId, loginUser, password);
        return ResultUtil.success(true);
    }

    @ApiOperation("测试发送消息")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/testSendMessage")
    public BaseResponse<Boolean> testSendMessage(@CurrentUser LoginUser loginUser, @NotEmpty String userId) {
        Integer receiveUserId = UserIdCodec.decode(userId);
        MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.USER.getType());
        messageSendDto.setReceiveUserId(receiveUserId);
        messageSendDto.setMessageContent("测试发送消息");
        messageSendDto.setSendUserId(loginUser.getUserId());
        messageSendDto.setSendUserNickName(loginUser.getNickName());
        messageSendDto.setSendTime(System.currentTimeMillis());
        messageHandler.sendMessage(messageSendDto);
        return ResultUtil.success(true);
    }
}
