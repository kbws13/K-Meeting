package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.entity.MeetingMember;

/**
 * @author housenyao
 * @description 针对表【meetingMember(会议成员表)】的数据库操作Service
 * @createDate 2025-06-28 18:08:26
 */
public interface MeetingMemberService extends IService<MeetingMember> {
    boolean save(MeetingMember meetingMember);
}
