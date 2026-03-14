package xyz.kbws.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.annotation.CurrentUser;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.DeleteRequest;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.model.dto.mettingReserve.CreateReserveDto;
import xyz.kbws.model.entity.MeetingReserve;
import xyz.kbws.model.enums.MeetingReserveStatusEnum;
import xyz.kbws.model.query.MeetingReserveQuery;
import xyz.kbws.model.vo.MeetingReserveVO;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.MeetingReserveService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author kbws
 * @date 2025/7/5
 * @description:
 */
@Api(tags = "预约会议接口")
@RestController
@RequestMapping("/meetingReserve")
public class MeetingReserveController {

    @Resource
    private MeetingReserveService meetingReserveService;

    @ApiOperation("今日会议")
    @AuthCheck(mustRole = UserConstant.user)
    @GetMapping("/today")
    public BaseResponse<List<MeetingReserveVO>> today(@CurrentUser LoginUser loginUser) {
        String currentDate = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm");
        MeetingReserveQuery meetingReserveQuery = new MeetingReserveQuery();
        meetingReserveQuery.setUserId(loginUser.getUserId());
        meetingReserveQuery.setStatus(MeetingReserveStatusEnum.NO_START.getValue());
        meetingReserveQuery.setStartTimeStart(currentDate);
        meetingReserveQuery.setStartTimeEnd(currentDate);
        meetingReserveQuery.setSortField("startTime");
        meetingReserveQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        meetingReserveQuery.setQueryUserInfo(true);
        List<MeetingReserveVO> list = meetingReserveService.findList(meetingReserveQuery);
        return ResultUtil.success(list);
    }

    @ApiOperation("查看预约会议")
    @AuthCheck(mustRole = UserConstant.user)
    @GetMapping("/load")
    public BaseResponse<Page<MeetingReserveVO>> load(@CurrentUser LoginUser loginUser) {
        MeetingReserveQuery meetingReserveQuery = new MeetingReserveQuery();
        meetingReserveQuery.setUserId(loginUser.getUserId());
        meetingReserveQuery.setStatus(MeetingReserveStatusEnum.NO_START.getValue());
        meetingReserveQuery.setSortField("startTime");
        meetingReserveQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        meetingReserveQuery.setQueryUserInfo(true);

        Page<MeetingReserveVO> page = meetingReserveService.findByPage(meetingReserveQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation("创建预约")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/create")
    public BaseResponse<MeetingReserve> create(@CurrentUser LoginUser loginUser, @RequestBody CreateReserveDto createReserveDto) {
        MeetingReserve meetingReserve = new MeetingReserve();
        BeanUtil.copyProperties(createReserveDto, meetingReserve);
        meetingReserve.setCreateUserId(loginUser.getUserId());
        meetingReserveService.create(meetingReserve, createReserveDto.getInviteUserIds());
        return ResultUtil.success(meetingReserve);
    }

    @ApiOperation("删除预约")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@CurrentUser LoginUser loginUser, @RequestBody DeleteRequest deleteRequest) {
        boolean res = meetingReserveService.delete(deleteRequest.getId(), loginUser.getUserId());
        return ResultUtil.success(res);
    }

    @ApiOperation("删除自己的预约")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/deleteByUser")
    public BaseResponse<Boolean> deleteByUser(@CurrentUser LoginUser loginUser, @RequestBody DeleteRequest deleteRequest) {
        boolean res = meetingReserveService.deleteByUser(deleteRequest.getId(), loginUser.getUserId());
        return ResultUtil.success(res);
    }
}
