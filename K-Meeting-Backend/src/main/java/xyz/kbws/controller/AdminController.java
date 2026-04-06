package xyz.kbws.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.model.query.UserQuery;
import xyz.kbws.model.vo.UserAdminVO;
import xyz.kbws.service.MeetingService;
import xyz.kbws.service.UserService;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author kbws
 * @date 2025/9/7
 * @description:
 */
@Slf4j
@Api(tags = "管理员接口")
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private MeetingService meetingService;

    @ApiOperation(value = "获取用户列表")
    @PostMapping("/loadUser")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Page<UserAdminVO>> loadUser(@RequestBody UserQuery userQuery) {
        Page<UserAdminVO> page = userService.findByPage(userQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation(value = "修改用户状态")
    @PostMapping("/updateUserStatus")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Boolean> updateUserStatus(@NotEmpty String userId, @NotNull Integer status) {
        Boolean res = userService.updateStatus(UserIdCodec.decode(userId), status);
        return ResultUtil.success(res);
    }

    @ApiOperation(value = "强制下线")
    @PostMapping("/forceOffLine")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Boolean> forceOffLine(@NotEmpty String userId) {
        userService.forceOffLine(UserIdCodec.decode(userId));
        return ResultUtil.success(true);
    }

    @ApiOperation(value = "获取会议列表")
    @PostMapping("/loadMeeting")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Page<Meeting>> loadMeeting(@RequestBody MeetingQuery meetingQuery) {
        meetingQuery.setSortField("m.createTime");
        meetingQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        meetingQuery.setQueryMeetingMemberCount(true);
        Page<Meeting> page = meetingService.findByPage(meetingQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation(value = "结束会议")
    @PostMapping("/finishMeeting")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Boolean> finishMeeting(@NotNull Integer meetingId) {
        Boolean res = meetingService.finishMeeting(meetingId, null);
        return ResultUtil.success(res);
    }
}
