package xyz.kbws.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kbws.mapper.MeetingmemberMapper;
import xyz.kbws.model.entity.MeetingMember;
import xyz.kbws.service.MeetingMemberService;

/**
* @author housenyao
* @description 针对表【meetingMember(会议成员表)】的数据库操作Service实现
* @createDate 2025-06-28 18:08:26
*/
@Service
public class MeetingMemberServiceImpl extends ServiceImpl<MeetingmemberMapper, MeetingMember> implements MeetingMemberService {

    @Override
    public boolean save(MeetingMember meetingMember) {
        return this.baseMapper.insertOrUpdate(meetingMember) > 0;
    }
}




