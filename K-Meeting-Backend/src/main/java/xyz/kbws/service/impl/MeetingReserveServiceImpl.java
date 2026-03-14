package xyz.kbws.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.MeetingReserveMapper;
import xyz.kbws.mapper.MeetingReserveMemberMapper;
import xyz.kbws.model.dto.mettingReserve.CreateReserveDto;
import xyz.kbws.model.entity.Meeting;
import xyz.kbws.model.entity.MeetingReserve;
import xyz.kbws.model.entity.MeetingReserveMember;
import xyz.kbws.model.enums.MeetingReserveStatusEnum;
import xyz.kbws.model.query.MeetingReserveQuery;
import xyz.kbws.model.vo.MeetingReserveVO;
import xyz.kbws.service.MeetingReserveMemberService;
import xyz.kbws.service.MeetingReserveService;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author housenyao
 * @description 针对表【meetingReserve(预约会议表)】的数据库操作Service实现
 * @createDate 2026-03-14 14:46:11
 */
@Service
public class MeetingReserveServiceImpl extends ServiceImpl<MeetingReserveMapper, MeetingReserve>
        implements MeetingReserveService {

    @Resource
    private MeetingReserveMapper meetingReserveMapper;

    @Resource
    private MeetingReserveMemberService meetingReserveMemberService;

    @Resource
    private MeetingReserveMemberMapper meetingReserveMemberMapper;

    @Override
    public Page<MeetingReserveVO> findByPage(MeetingReserveQuery meetingReserveQuery) {
        Page<MeetingReserveVO> page = new Page<>(meetingReserveQuery.getCurrent(), meetingReserveQuery.getPageSize());
        List<MeetingReserveVO> records = meetingReserveMapper.findListByPage(page, meetingReserveQuery);
        page.setRecords(records);
        return page;
    }

    @Override
    public List<MeetingReserveVO> findList(MeetingReserveQuery meetingReserveQuery) {
        return meetingReserveMapper.findList(meetingReserveQuery);
    }

    @Override
    public void create(MeetingReserve meetingReserve, String inviteUserIds) {
        meetingReserve.setMeetingId(Integer.valueOf(RandomUtil.randomNumbers(9)));
        meetingReserve.setStatus(MeetingReserveStatusEnum.NO_START.getValue());
        this.save(meetingReserve);

        List<MeetingReserveMember> meetingReserveMemberList = new ArrayList<>();
        if (!StrUtil.isEmpty(inviteUserIds)) {
            String[] inviteUserIdArray = inviteUserIds.split(",");
            for (String userId : inviteUserIdArray) {
                Integer realUserId = UserIdCodec.decode(userId);
                MeetingReserveMember member = new MeetingReserveMember();
                member.setMeetingId(meetingReserve.getMeetingId());
                member.setInviteUserId(realUserId);
                meetingReserveMemberList.add(member);
            }
        }
        MeetingReserveMember member = new MeetingReserveMember();
        member.setMeetingId(meetingReserve.getMeetingId());
        member.setInviteUserId(meetingReserve.getCreateUserId());
        meetingReserveMemberList.add(member);
        meetingReserveMemberService.saveBatch(meetingReserveMemberList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean delete(Integer meetingId, Integer userId) {
        LambdaQueryWrapper<MeetingReserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingReserve::getMeetingId, meetingId)
                .eq(MeetingReserve::getCreateUserId, userId);
        int count = meetingReserveMapper.delete(wrapper);
        if (count > 0) {
            LambdaQueryWrapper<MeetingReserveMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(MeetingReserveMember::getMeetingId, meetingId);
            meetingReserveMemberMapper.delete(memberWrapper);
        }
        return true;
    }

    @Override
    public Boolean deleteByUser(Integer meetingId, Integer userId) {
        MeetingReserve meetingReserve = this.getById(meetingId);
        if (meetingReserve == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (meetingReserve.getCreateUserId().equals(userId)) {
            delete(meetingId, userId);
        } else {
            LambdaQueryWrapper<MeetingReserveMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(MeetingReserveMember::getMeetingId, meetingId);
            memberWrapper.eq(MeetingReserveMember::getInviteUserId, userId);
            meetingReserveMemberMapper.delete(memberWrapper);
        }
        return true;
    }
}




