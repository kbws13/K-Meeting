package xyz.kbws.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.dto.user.UserLoginDto;
import xyz.kbws.model.dto.user.UserRegisterDto;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.enums.UserStatusEnum;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.service.UserService;
import xyz.kbws.utils.JwtUtil;

import javax.annotation.Resource;

/**
* @author housenyao
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-06-27 21:58:30
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private RedisComponent redisComponent;

    @Override
    public boolean register(UserRegisterDto userRegisterDto) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", userRegisterDto.getEmail());
        User user = this.getOne(queryWrapper);
        if (user != null) {
            return false;
        }
        int userId = Integer.parseInt(RandomStringUtils.random(5, false, true));
        User newUser = new User();
        newUser.setId(userId);
        newUser.setEmail(userRegisterDto.getEmail());
        newUser.setNickName(userRegisterDto.getNickName());
        newUser.setPassword(DigestUtil.md5Hex(userRegisterDto.getPassword()));
        newUser.setStatus(UserStatusEnum.ENABLE.getValue());
        newUser.setMeetingNo(RandomStringUtils.random(9, false, true));
        return this.save(newUser);
    }

    @Override
    public UserVO login(UserLoginDto userLoginDto) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", userLoginDto.getEmail());
        User user = this.getOne(queryWrapper);
        if (user == null || !user.getPassword().equals(userLoginDto.getPassword())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "账号或密码错误");
        }
        if (UserStatusEnum.DISABLE.getValue().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "账号已被禁用");
        }
        if (user.getLastLoginTime() != null && user.getLastOffTime() < user.getLastLoginTime()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号已在别处登录，请退出后再登录");
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userVO.setToken(JwtUtil.createToken(userVO.getId()));
        redisComponent.saveUserVO(userVO);
        return userVO;
    }
}




