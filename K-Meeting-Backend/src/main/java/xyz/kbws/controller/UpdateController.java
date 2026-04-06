package xyz.kbws.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.model.vo.AppUpdateCheckVO;
import xyz.kbws.service.AppUpdateService;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author kbws
 * @date 2025/7/5
 * @description:
 */
@Api(tags = "更新接口")
@RestController
@RequestMapping("/update")
public class UpdateController {

    @Resource
    private AppUpdateService appUpdateService;

    @ApiOperation("检查更新")
    @GetMapping("/checkVersion")
    public BaseResponse<AppUpdateCheckVO> checkVersion(@NotEmpty String version, @NotNull Integer fileType, String grayscaleId) {
        return ResultUtil.success(appUpdateService.checkVersion(version, fileType, grayscaleId));
    }
}
