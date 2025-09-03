package xyz.kbws.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.service.MeetingService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    @PostMapping("/loadMeeting")
    public BaseResponse<Page<Meeting>> loadMeeting(@RequestBody MeetingQuery meetingQuery, HttpServletRequest request) {
        String token = request.getHeader("token");
        UserVO userVO = redisComponent.getUserVO(token);
        meetingQuery.setUserId(userVO.getId());
        meetingQuery.setSortField("m.createTime");
        meetingQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        meetingQuery.setQueryMeetingMemberCount(true);
        List<Meeting> list = meetingService.findListByPage(meetingQuery);
        Page<Meeting> page = new Page<>();
        page.setRecords(list);
        page.setTotal(list.size());
        page.setCurrent(meetingQuery.getCurrent());
        page.setSize(10);
        return ResultUtil.success(page);
    }
}
