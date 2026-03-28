package xyz.kbws.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.UserContactApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.kbws.model.query.ContactApplyQuery;
import xyz.kbws.model.vo.ContactApplyVO;

import java.util.List;

/**
 * @author housenyao
 * @description 针对表【userContactApply(联系人申请表)】的数据库操作Mapper
 * @createDate 2026-03-25 21:40:23
 * @Entity xyz.kbws.model.entity.userContactApply
 */
public interface UserContactApplyMapper extends BaseMapper<UserContactApply> {
    List<ContactApplyVO> findListByPage(Page<ContactApplyVO> page, @Param("query") ContactApplyQuery query);

    UserContactApply selectOneByApplyUserIdAndReceiveUserId(@Param("applyUserId") Integer applyUserId,
                                                            @Param("receiveUserId") Integer receiveUserId);
}




