package xyz.kbws.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.model.entity.SystemSetting;
import xyz.kbws.redis.RedisComponent;

import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2026/4/2
 * @description:
 */
@Api(tags = "系统接口")
@RestController
@RequestMapping("/system")
public class SystemController {
    
    @Resource
    private RedisComponent redisComponent;
    
    @ApiOperation("获取系统设置")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<SystemSetting> getSetting() {
        SystemSetting systemSetting = redisComponent.getSystemSetting();
        return ResultUtil.success(systemSetting);
    }
}
