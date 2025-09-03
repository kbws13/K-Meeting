package xyz.kbws.service;

import xyz.kbws.model.entity.Meeting;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.query.MeetingQuery;

import java.util.List;

/**
* @author housenyao
* @description 针对表【meeting(会议表)】的数据库操作Service
* @createDate 2025-06-28 18:08:22
*/
public interface MeetingService extends IService<Meeting> {
    List<Meeting> findListByPage(MeetingQuery meetingQuery);
}
