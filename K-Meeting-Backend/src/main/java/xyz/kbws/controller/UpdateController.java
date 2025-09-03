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
@Api(tags = "更新接口")
@RestController
@RequestMapping("/update")
public class UpdateController {

    @GetMapping("/checkVersion")
    public BaseResponse<Boolean> checkVersion() {
        return ResultUtil.success(false);
    }
}
