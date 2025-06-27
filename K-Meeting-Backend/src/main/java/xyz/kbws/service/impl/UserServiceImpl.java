package xyz.kbws.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.kbws.model.entity.User;
import xyz.kbws.service.UserService;
import xyz.kbws.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author housenyao
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-06-27 21:58:30
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




