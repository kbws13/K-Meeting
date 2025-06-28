package xyz.kbws.service;

import xyz.kbws.model.dto.user.UserLoginDto;
import xyz.kbws.model.dto.user.UserRegisterDto;
import xyz.kbws.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.vo.UserVO;

/**
* @author housenyao
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-06-27 21:58:30
*/
public interface UserService extends IService<User> {
    boolean register(UserRegisterDto userRegisterDto);

    UserVO login(UserLoginDto userLoginDto);
}
