package xyz.kbws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.MeetingMember;

/**
* @author housenyao
* @description 针对表【meetingMember(会议成员表)】的数据库操作Mapper
* @createDate 2025-06-28 18:08:26
* @Entity xyz.kbws.model.entity.Meetingmember
*/
public interface MeetingmemberMapper extends BaseMapper<MeetingMember> {

    @Override
    int updateById(@Param(Constants.ENTITY) MeetingMember entity);

    int updateByMeetingId(@Param(Constants.ENTITY) MeetingMember entity);

    int insertOrUpdate(MeetingMember meetingMember);
}




