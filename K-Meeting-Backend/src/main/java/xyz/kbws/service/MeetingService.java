package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import xyz.kbws.model.entity.Meeting;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.model.vo.UserVO;

/**
* @author housenyao
* @description 针对表【meeting(会议表)】的数据库操作Service
* @createDate 2025-06-28 18:08:22
*/
public interface MeetingService extends IService<Meeting> {
    Page<Meeting> findByPage(MeetingQuery meetingQuery);

    void quickMeeting(Meeting meeting, String nickName);

    void join(UserVO userVO, String meetingId, Boolean openVideo);
}
