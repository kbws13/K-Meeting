package xyz.kbws.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.config.AppConfig;
import xyz.kbws.constant.FileConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.ffmpeg.FFmpegComponent;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.dto.user.UserLoginDto;
import xyz.kbws.model.dto.user.UserRegisterDto;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.enums.UserStatusEnum;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.UserService;
import xyz.kbws.utils.JwtUtil;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * @author housenyao
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-06-27 21:58:30
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private RedisComponent redisComponent;
    
    @Resource
    private AppConfig appConfig;
    
    @Resource
    private FFmpegComponent ffmpegComponent;

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
        String secretUserId = UserIdCodec.encode(user.getId());
        userVO.setId(secretUserId);
        userVO.setToken(JwtUtil.createToken(secretUserId));
        LoginUser loginUser = new LoginUser();
        BeanUtil.copyProperties(userVO, loginUser);
        loginUser.setUserId(user.getId());
        redisComponent.saveUserVO(loginUser);
        return userVO;
    }

    @Override
    public Boolean changePassword(Integer userId, String password, String newPassword) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        if (StrUtil.isBlank(password) || StrUtil.isBlank(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        String storedPassword = user.getPassword();
        String changedPassword;
        if (StrUtil.equalsIgnoreCase(storedPassword, password)) {
            changedPassword = newPassword;
        } else if (StrUtil.equalsIgnoreCase(storedPassword, DigestUtil.md5Hex(password))) {
            changedPassword = DigestUtil.md5Hex(newPassword);
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "原密码错误");
        }

        if (StrUtil.equalsIgnoreCase(storedPassword, changedPassword)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新旧密码不能一致");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(changedPassword);
        boolean res = this.updateById(updateUser);
        if (!res) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改密码失败");
        }
        redisComponent.cleanTokenByUserId(userId);
        return true;
    }

    @Override
    public UserVO updateUserInfo(MultipartFile avatar, User user) throws IOException {
        if (avatar != null) {
            String folder = appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_TEMP + FileConstant.FILE_FOLDER_AVATAR_NAME;
            File folderFile = new File(folder);
            if (!folderFile.exists()) {
                folderFile.mkdirs();
            }
            String realFileName = user.getId() + FileConstant.IMAGE_SUFFIX;
            String filePath = folderFile + realFileName;
            File tempFile = new File(appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_TEMP + RandomUtil.randomString(30));
            avatar.transferTo(tempFile);
            ffmpegComponent.createImageThumbnail(tempFile, filePath);
        }
        this.updateById(user);
        LoginUser loginUser = redisComponent.getLoginUserById(user.getId());
        loginUser.setNickName(user.getNickName());
        loginUser.setSex(user.getSex());
        redisComponent.saveUserVO(loginUser);
        return loginUser;
    }
}




