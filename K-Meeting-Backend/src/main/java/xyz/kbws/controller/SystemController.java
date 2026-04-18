package xyz.kbws.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.exception.BusinessException;
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
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<SystemSetting> getSetting() {
        SystemSetting systemSetting = redisComponent.getSystemSetting();
        return ResultUtil.success(systemSetting);
    }

    @ApiOperation("保存系统设置")
    @PostMapping("/save")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Boolean> saveSetting(@RequestBody SystemSetting systemSetting) {
        checkSystemSetting(systemSetting);
        redisComponent.saveSystemSetting(systemSetting);
        return ResultUtil.success(true);
    }

    private void checkSystemSetting(SystemSetting systemSetting) {
        if (systemSetting == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统设置不能为空");
        }
        if (systemSetting.getMaxImageSize() == null || systemSetting.getMaxImageSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小限制错误");
        }
        if (systemSetting.getMaxVideoSize() == null || systemSetting.getMaxVideoSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "视频大小限制错误");
        }
        if (systemSetting.getMaxFileSize() == null || systemSetting.getMaxFileSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小限制错误");
        }
    }
}
