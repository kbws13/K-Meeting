package xyz.kbws.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.model.entity.User;

import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("/loadUser")
    public BaseResponse<List<User>> loadUser() {
        return ResultUtil.success(new ArrayList<>());
    }
}
