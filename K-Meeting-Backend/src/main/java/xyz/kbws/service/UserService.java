package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import xyz.kbws.model.dto.user.UserLoginDto;
import xyz.kbws.model.dto.user.UserRegisterDto;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.query.UserQuery;
import xyz.kbws.model.vo.UserAdminVO;
import xyz.kbws.model.vo.UserVO;

import java.io.IOException;

/**
 * @author housenyao
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2025-06-27 21:58:30
 */
public interface UserService extends IService<User> {
    boolean register(UserRegisterDto userRegisterDto);

    UserVO login(UserLoginDto userLoginDto);

    Boolean changePassword(Integer userId, String password, String newPassword);

    Page<UserAdminVO> findByPage(UserQuery userQuery);

    Boolean updateStatus(Integer currentUserId, Integer userId, Integer status);
    
    UserVO updateUserInfo(MultipartFile avatar, User user) throws IOException;

    void forceOffLine(Integer currentUserId, Integer userId);
}
