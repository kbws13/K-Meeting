package xyz.kbws.mapper;

import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.Meeting;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.kbws.model.query.MeetingQuery;

import java.util.List;

/**
* @author housenyao
* @description 针对表【meeting(会议表)】的数据库操作Mapper
* @createDate 2025-06-28 18:08:22
* @Entity xyz.kbws.model.entity.Meeting
*/
public interface MeetingMapper extends BaseMapper<Meeting> {
    List<Meeting> findListByPage(@Param("query") MeetingQuery meetingQuery);
}




