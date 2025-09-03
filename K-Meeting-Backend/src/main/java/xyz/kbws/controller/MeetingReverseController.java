package xyz.kbws.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ResultUtil;

/**
 * @author kbws
 * @date 2025/7/5
 * @description:
 */
@Api(tags = "预约会议接口")
@RestController
@RequestMapping("/meetingReverse")
public class MeetingReverseController {
    
    @GetMapping("/loadTodyMeeting")
    public BaseResponse<Boolean> loadTodyMeeting() {
        return ResultUtil.success(false);
    }
}
