package xyz.kbws.controller;

import cn.hutool.core.util.RandomUtil;
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
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.MeetingService;

import javax.annotation.Resource;

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

    @GetMapping("/getCurrentMeeting")
    public BaseResponse getCurrentMeeting() {
        return ResultUtil.success(null);
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
}
