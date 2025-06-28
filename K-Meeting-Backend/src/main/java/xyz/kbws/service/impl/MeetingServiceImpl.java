package xyz.kbws.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.service.MeetingService;
import xyz.kbws.mapper.MeetingMapper;
import org.springframework.stereotype.Service;

/**
* @author housenyao
* @description 针对表【meeting(会议表)】的数据库操作Service实现
* @createDate 2025-06-28 18:08:22
*/
@Service
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper, Meeting>
    implements MeetingService{

}




