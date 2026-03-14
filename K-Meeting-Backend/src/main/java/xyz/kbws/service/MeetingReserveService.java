package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.entity.MeetingReserve;
import xyz.kbws.model.query.MeetingReserveQuery;
import xyz.kbws.model.vo.MeetingReserveVO;

import java.util.List;

/**
 * @author housenyao
 * @description 针对表【meetingReserve(预约会议表)】的数据库操作Service
 * @createDate 2026-03-14 14:46:11
 */
public interface MeetingReserveService extends IService<MeetingReserve> {
    Page<MeetingReserveVO> findByPage(MeetingReserveQuery meetingReserveQuery);

    List<MeetingReserveVO> findList(MeetingReserveQuery meetingReserveQuery);

    void create(MeetingReserve meetingReserve, String inviteUserIds);

    Boolean delete(Integer meetingId, Integer userId);

    Boolean deleteByUser(Integer meetingId, Integer userId);
}
