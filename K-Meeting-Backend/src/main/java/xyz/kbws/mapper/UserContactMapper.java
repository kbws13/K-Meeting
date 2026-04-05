package xyz.kbws.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.UserContact;
import xyz.kbws.model.query.ContactQuery;
import xyz.kbws.model.vo.ContactVO;

import java.util.List;

/**
 * @author housenyao
 * @description 针对表【userContact(联系人表)】的数据库操作Mapper
 * @createDate 2026-03-25 21:40:14
 * @Entity xyz.kbws.model.entity.userContact
 */
public interface UserContactMapper {
    /** 插入一条联系人记录 */
    int insert(UserContact userContact);

    /** 根据双主键查询 */
    UserContact selectByPrimaryKey(@Param("userId") Integer userId,
                                   @Param("contactId") Integer contactId);

    /** 查询某用户的所有联系人 */
    List<UserContact> selectByUserId(@Param("userId") Integer userId);

    /** 根据双主键更新状态 */
    int updateByPrimaryKey(UserContact userContact);

    /** 根据双主键删除 */
    int deleteByPrimaryKey(@Param("userId") Integer userId,
                           @Param("contactId") Integer contactId);

    /** 分页查询联系人列表（附带用户信息） */
    List<ContactVO> findListByPage(Page<ContactVO> page, @Param("query") ContactQuery query);
}




