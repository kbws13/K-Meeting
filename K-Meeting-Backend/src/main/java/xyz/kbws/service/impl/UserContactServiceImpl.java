package xyz.kbws.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kbws.mapper.UserContactApplyMapper;
import xyz.kbws.mapper.UserContactMapper;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.query.ContactQuery;
import xyz.kbws.model.vo.ContactVO;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.entity.UserContact;
import xyz.kbws.model.entity.UserContactApply;
import xyz.kbws.model.enums.ContactApplyStatusEnum;
import xyz.kbws.model.enums.ContactStatusEnum;
import xyz.kbws.model.vo.UserContactVO;
import xyz.kbws.service.UserContactService;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author housenyao
 * @description 针对表【userContact(联系人表)】的数据库操作Service实现
 * @createDate 2026-03-25 21:40:14
 */
@Service
public class UserContactServiceImpl extends ServiceImpl<UserContactMapper, UserContact>
        implements UserContactService {
    @Resource
    private UserContactMapper userContactMapper;

    @Resource
    private UserContactApplyMapper userContactApplyMapper;

    @Resource
    private UserMapper userMapper;

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
        LambdaQueryWrapper<UserContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserContact::getUserId, contactUserId)
                .eq(UserContact::getContactId, userId);
        UserContact userContact = userContactMapper.selectOne(queryWrapper);

        // 拉黑
        if (userContactApply != null && ContactApplyStatusEnum.BLACKLIST.getValue().equals(userContactApply.getStatus()) || userContact != null && ContactApplyStatusEnum.BLACKLIST.getValue().equals(userContact.getStatus())) {
            result.setStatus(ContactApplyStatusEnum.BLACKLIST.getValue());
            return result;
        }

        if (userContactApply != null && ContactApplyStatusEnum.INIT.getValue().equals(userContactApply.getStatus())) {
            result.setStatus(ContactApplyStatusEnum.INIT.getValue());
            return result;
        }
        queryWrapper.clear();
        queryWrapper.eq(UserContact::getUserId, userId)
                .eq(UserContact::getContactId, contactUserId);
        UserContact myContact = userContactMapper.selectOne(queryWrapper);
        if (userContact != null && ContactStatusEnum.FRIEND.getValue().equals(userContact.getStatus()) && myContact != null && ContactStatusEnum.FRIEND.getValue().equals(myContact.getStatus())) {
            result.setStatus(ContactStatusEnum.FRIEND.getValue());
            return result;
        }

        return result;
    }
}




