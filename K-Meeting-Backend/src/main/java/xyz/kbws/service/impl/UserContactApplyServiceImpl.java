package xyz.kbws.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.UserContactApplyMapper;
import xyz.kbws.mapper.UserContactMapper;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.UserContact;
import xyz.kbws.model.entity.UserContactApply;
import xyz.kbws.model.enums.ContactApplyStatusEnum;
import xyz.kbws.model.enums.ContactStatusEnum;
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.model.enums.MessageTypeEnum;
import xyz.kbws.model.query.ContactApplyQuery;
import xyz.kbws.model.vo.ContactApplyVO;
import xyz.kbws.service.UserContactApplyService;
import xyz.kbws.service.UserContactService;
import xyz.kbws.utils.UserIdCodec;
import xyz.kbws.websocket.message.MessageHandler;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author housenyao
 * @description 针对表【userContactApply(联系人申请表)】的数据库操作Service实现
 * @createDate 2026-03-25 21:40:23
 */
@Service
public class UserContactApplyServiceImpl extends ServiceImpl<UserContactApplyMapper, UserContactApply>
        implements UserContactApplyService {

    @Resource
    private UserContactApplyMapper userContactApplyMapper;

    @Resource
    private UserContactService userContactService;

    @Resource
    private UserContactMapper userContactMapper;

    @Resource
    private MessageHandler messageHandler;

    @Override
    public Page<ContactApplyVO> findByPage(ContactApplyQuery contactApplyQuery) {
        Page<ContactApplyVO> page = new Page<>(contactApplyQuery.getCurrent(), contactApplyQuery.getPageSize());
        List<ContactApplyVO> records = userContactApplyMapper.findListByPage(page, contactApplyQuery);
        records.forEach(item -> {
            item.setApplyContactUserId(UserIdCodec.encode(item.getApplyUserId()));
            item.setReceiveContactUserId(UserIdCodec.encode(item.getReceiveUserId()));
        });
        page.setRecords(records);
        return page;
    }

    @Override
    public Integer saveApply(UserContactApply apply) {
        // 查询对方是否已将申请人拉黑（对方视角：receiveUserId -> applyUserId）
        UserContact userContact = userContactMapper.selectByPrimaryKey(
                apply.getReceiveUserId(), apply.getApplyUserId());
        if (userContact != null && ContactStatusEnum.BLACKLIST.getValue().equals(userContact.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "对方已将你拉黑");
        }

        // 已经是好友，确保申请人侧关系也存在
        if (userContact != null && ContactStatusEnum.FRIEND.getValue().equals(userContact.getStatus())) {
            UserContact myContact = new UserContact();
            myContact.setUserId(apply.getApplyUserId());
            myContact.setContactId(apply.getReceiveUserId());
            myContact.setStatus(ContactStatusEnum.FRIEND.getValue());
            myContact.setLastUpdateTime(new Date());
            userContactMapper.updateByPrimaryKey(myContact);
            return ContactStatusEnum.FRIEND.getValue();
        }

        UserContactApply userContactApply = userContactApplyMapper.selectOneByApplyUserIdAndReceiveUserId(
                apply.getApplyUserId(), apply.getReceiveUserId());
        if (userContactApply == null) {
            apply.setStatus(ContactApplyStatusEnum.INIT.getValue());
            this.save(apply);
        } else {
            LambdaUpdateWrapper<UserContactApply> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(UserContactApply::getApplyUserId, userContactApply.getApplyUserId())
                    .set(UserContactApply::getStatus, ContactApplyStatusEnum.INIT.getValue());
            this.update(updateWrapper);
        }
        // 发送消息
        MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.USER.getType());
        messageSendDto.setMessageType(MessageTypeEnum.USER_CONTACT_APPLY.getValue());
        messageSendDto.setReceiveUserId(apply.getReceiveUserId());
        messageHandler.sendMessage(messageSendDto);
        return ContactApplyStatusEnum.INIT.getValue();
    }

    @Override
    public void deal(Integer applyUserId, Integer userId, String nickName, Integer status) {
        ContactApplyStatusEnum statusEnum = ContactApplyStatusEnum.getByValue(status);
        UserContactApply apply = this.userContactApplyMapper.selectOneByApplyUserIdAndReceiveUserId(applyUserId, userId);
        if (apply == null || statusEnum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (ContactApplyStatusEnum.PASS.getValue().equals(status)) {
            Date now = new Date();
            // 申请人 -> 被申请人
            UserContact contact1 = new UserContact();
            contact1.setUserId(applyUserId);
            contact1.setContactId(userId);
            contact1.setStatus(ContactStatusEnum.FRIEND.getValue());
            contact1.setLastUpdateTime(now);
            userContactMapper.insert(contact1);

            // 被申请人 -> 申请人
            UserContact contact2 = new UserContact();
            contact2.setUserId(userId);
            contact2.setContactId(applyUserId);
            contact2.setStatus(ContactStatusEnum.FRIEND.getValue());
            contact2.setLastUpdateTime(now);
            userContactMapper.insert(contact2);
        }

        apply.setStatus(status);
        this.updateById(apply);

        MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
        messageSendDto.setSendUserNickName(nickName);
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.USER.getType());
        messageSendDto.setMessageType(MessageTypeEnum.USER_CONTACT_ACCESS.getValue());
        messageSendDto.setReceiveUserId(applyUserId);
        messageSendDto.setMessageContent(status);
        messageHandler.sendMessage(messageSendDto);
    }

    @Override
    public Long loadContactApplyCount(Integer userId) {
        LambdaQueryWrapper<UserContactApply> qw = new LambdaQueryWrapper<>();
        qw.eq(UserContactApply::getReceiveUserId, userId)
                .eq(UserContactApply::getStatus, ContactApplyStatusEnum.INIT);
        return this.baseMapper.selectCount(qw);
    }
}




