package xyz.kbws.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.UserContact;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.kbws.model.query.ContactQuery;
import xyz.kbws.model.vo.ContactVO;

import java.util.List;

/**
 * @author housenyao
 * @description 针对表【userContact(联系人表)】的数据库操作Mapper
 * @createDate 2026-03-25 21:40:14
 * @Entity xyz.kbws.model.entity.userContact
 */
public interface UserContactMapper extends BaseMapper<UserContact> {
    List<ContactVO> findListByPage(Page<ContactVO> page, @Param("query") ContactQuery query);
}




