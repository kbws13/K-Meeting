package xyz.kbws.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.UserContactApplyMapper;
import xyz.kbws.mapper.UserContactMapper;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.entity.UserContact;
import xyz.kbws.model.entity.UserContactApply;
import xyz.kbws.model.enums.ContactApplyStatusEnum;
import xyz.kbws.model.enums.ContactStatusEnum;
import xyz.kbws.model.query.ContactQuery;
import xyz.kbws.model.vo.ContactVO;
import xyz.kbws.model.vo.UserContactVO;
import xyz.kbws.service.UserContactService;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author housenyao
 * @description 针对表【userContact(联系人表)】的数据库操作Service实现
 * @createDate 2026-03-25 21:40:14
 */
@Service
public class UserContactServiceImpl implements UserContactService {
    @Resource
    private UserContactMapper userContactMapper;

    @Resource
    private UserContactApplyMapper userContactApplyMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public int insert(UserContact userContact) {
        return userContactMapper.insert(userContact);
    }

    @Override
    public Page<ContactVO> findByPage(ContactQuery contactQuery) {
        Page<ContactVO> page = new Page<>(contactQuery.getCurrent(), contactQuery.getPageSize());
        List<ContactVO> records = userContactMapper.findListByPage(page, contactQuery);
        records.forEach(item -> item.setContactUserId(UserIdCodec.encode(item.getContactId())));
        page.setRecords(records);
        return page;
    }

    @Override
    public UserContactVO search(Integer userId, Integer contactUserId) {
        User user = userMapper.selectById(contactUserId);
        if (user == null) {
            return null;
        }
        UserContactVO result = new UserContactVO();
        result.setUserId(UserIdCodec.encode(user.getId()));
        result.setNickName(user.getNickName());
        if (userId.equals(contactUserId)) {
            result.setStatus(-ContactApplyStatusEnum.PASS.getValue());
        }

        UserContactApply userContactApply = userContactApplyMapper.selectOneByApplyUserIdAndReceiveUserId(contactUserId, userId);
        // 查询对方对当前用户的关系（contactUserId -> userId）
        UserContact userContact = userContactMapper.selectByPrimaryKey(contactUserId, userId);

        // 拉黑判断
        if ((userContactApply != null && ContactApplyStatusEnum.BLACKLIST.getValue().equals(userContactApply.getStatus()))
                || (userContact != null && ContactApplyStatusEnum.BLACKLIST.getValue().equals(userContact.getStatus()))) {
            result.setStatus(ContactApplyStatusEnum.BLACKLIST.getValue());
            return result;
        }

        if (userContactApply != null && ContactApplyStatusEnum.INIT.getValue().equals(userContactApply.getStatus())) {
            result.setStatus(ContactApplyStatusEnum.INIT.getValue());
            return result;
        }

        // 查询当前用户对对方的关系（userId -> contactUserId）
        UserContact myContact = userContactMapper.selectByPrimaryKey(userId, contactUserId);
        if (userContact != null && ContactStatusEnum.FRIEND.getValue().equals(userContact.getStatus())
                && myContact != null && ContactStatusEnum.FRIEND.getValue().equals(myContact.getStatus())) {
            result.setStatus(ContactStatusEnum.FRIEND.getValue());
            return result;
        }

        return result;
    }

    @Override
    public void deleteContact(Integer userId, Integer contactId, Integer status) {
        if (!ArrayUtil.contains(new Integer[]{ContactStatusEnum.BLACKLIST.getValue(), ContactStatusEnum.DELETE.getValue()}, status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserContact userContact = new UserContact();
        userContact.setUserId(userId);
        userContact.setContactId(contactId);
        userContact.setStatus(status);
        userContact.setLastUpdateTime(new Date());
        userContactMapper.updateByPrimaryKey(userContact);
    }
}




