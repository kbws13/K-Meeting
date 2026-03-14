package xyz.kbws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.MeetingReserve;
import xyz.kbws.model.query.MeetingReserveQuery;
import xyz.kbws.model.vo.MeetingReserveVO;

import java.util.List;

/**
 * @author housenyao
 * @description 针对表【meetingReserve(预约会议表)】的数据库操作Mapper
 * @createDate 2026-03-14 14:46:11
 * @Entity xyz.kbws.model.entity.meetingReserve
 */
public interface MeetingReserveMapper extends BaseMapper<MeetingReserve> {
    List<MeetingReserveVO> findListByPage(Page<MeetingReserveVO> page, @Param("query") MeetingReserveQuery query);

    List<MeetingReserveVO> findList(@Param("query") MeetingReserveQuery query);
}




