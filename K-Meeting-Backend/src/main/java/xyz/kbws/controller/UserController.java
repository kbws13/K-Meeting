package xyz.kbws.controller;

import com.wf.captcha.ArithmeticCaptcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.dto.user.UserLoginDto;
import xyz.kbws.model.dto.user.UserRegisterDto;
import xyz.kbws.model.vo.CheckCodeVO;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.service.UserService;

import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2025/6/27
 * @description:
 */
@Slf4j
@Api(tags = "用户接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisComponent redisComponent;

    @ApiOperation("获取验证码")
    @GetMapping("/checkCode")
    public BaseResponse<CheckCodeVO> checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String code = captcha.text();
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();
        log.info("code: {}", code);
        CheckCodeVO checkCodeVO = new CheckCodeVO();
        checkCodeVO.setCheckCodeKey(checkCodeKey);
        checkCodeVO.setCheckCode(checkCodeBase64);
        return ResultUtil.success(checkCodeVO);
    }

    @ApiOperation("注册")
    @PostMapping("/register")
    public BaseResponse<Boolean> register(@RequestBody UserRegisterDto userRegisterDto) {
        String checkCodeKey = userRegisterDto.getCheckCodeKey();
        try {
            String checkCode = redisComponent.getCheckCode(checkCodeKey);
            if (!checkCode.equalsIgnoreCase(userRegisterDto.getCheckCode())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
            }
            boolean res = userService.register(userRegisterDto);
            return ResultUtil.success(res);
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @ApiOperation("登录")
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginDto userLoginDto) {
        UserVO userVO = userService.login(userLoginDto);
        return ResultUtil.success(userVO);
    }
}
