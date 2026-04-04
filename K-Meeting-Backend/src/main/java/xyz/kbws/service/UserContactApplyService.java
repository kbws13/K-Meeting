package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import xyz.kbws.model.entity.UserContactApply;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.query.ContactApplyQuery;
import xyz.kbws.model.vo.ContactApplyVO;

/**
 * @author housenyao
 * @description 针对表【userContactApply(联系人申请表)】的数据库操作Service
 * @createDate 2026-03-25 21:40:23
 */
public interface UserContactApplyService extends IService<UserContactApply> {
    Page<ContactApplyVO> findByPage(ContactApplyQuery contactApplyQuery);

    Integer saveApply(UserContactApply userContactApply);

    void deal(Integer applyUserId, Integer userId, String nickName, Integer status);

    Long loadContactApplyCount(Integer userId);
}
