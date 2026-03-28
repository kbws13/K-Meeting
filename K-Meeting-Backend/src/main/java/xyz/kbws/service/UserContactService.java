package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import xyz.kbws.model.entity.UserContact;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.query.ContactQuery;
import xyz.kbws.model.vo.ContactVO;
import xyz.kbws.model.vo.UserContactVO;

/**
 * @author housenyao
 * @description 针对表【userContact(联系人表)】的数据库操作Service
 * @createDate 2026-03-25 21:40:14
 */
public interface UserContactService extends IService<UserContact> {

    Page<ContactVO> findByPage(ContactQuery contactQuery);

    UserContactVO search(Integer userId, Integer contactUserId);

    void deleteContact(Integer userId, Integer contactId, Integer status);
}
