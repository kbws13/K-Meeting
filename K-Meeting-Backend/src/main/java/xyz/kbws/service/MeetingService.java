package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.enums.MeetingMemberStatus;
import xyz.kbws.model.query.MeetingQuery;
import xyz.kbws.redis.entity.LoginUser;

/**
 * @author housenyao
 * @description 针对表【meeting(会议表)】的数据库操作Service
 * @createDate 2025-06-28 18:08:22
 */
public interface MeetingService extends IService<Meeting> {
    Page<Meeting> findByPage(MeetingQuery meetingQuery);

    void quickMeeting(Meeting meeting, String nickName);

    void join(LoginUser userVO, Integer meetingId, Boolean openVideo);

    Integer preJoinMeeting(Integer meetingId, LoginUser loginUser, String password);

    Boolean exitMeetingRoom(LoginUser loginUser, MeetingMemberStatus meetingMemberStatus);

    Boolean kickOutMeetingRoom(LoginUser loginUser, Integer targetUserId, MeetingMemberStatus meetingMemberStatus);

    Boolean finishMeeting(Integer meetingId, Integer currentUserId);

    void reserveJoinMeeting(Integer meetingId, LoginUser loginUser, String joinPassword);
}
