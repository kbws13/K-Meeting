package xyz.kbws.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.annotation.CurrentUser;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.model.entity.UserContactApply;
import xyz.kbws.model.enums.ContactApplyStatusEnum;
import xyz.kbws.model.enums.ContactStatusEnum;
import xyz.kbws.model.query.ContactApplyQuery;
import xyz.kbws.model.query.ContactQuery;
import xyz.kbws.model.vo.ContactApplyVO;
import xyz.kbws.model.vo.ContactVO;
import xyz.kbws.model.vo.UserContactVO;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.UserContactApplyService;
import xyz.kbws.service.UserContactService;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author kbws
 * @date 2025/7/5
 * @description:
 */
@Api(tags = "联系人接口")
@RestController
@RequestMapping("/contact")
public class ContactController {

    @Resource
    private UserContactService userContactService;

    @Resource
    private UserContactApplyService userContactApplyService;

    @ApiOperation("联系人列表")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/load")
    public BaseResponse<Page<ContactVO>> load(@RequestBody ContactQuery contactQuery, @CurrentUser LoginUser loginUser) {
        contactQuery.setUserId(loginUser.getUserId());
        if (contactQuery.getStatus() == null) {
            contactQuery.setStatus(ContactStatusEnum.FRIEND.getValue());
        }
        contactQuery.setSortField("uc.lastUpdateTime");
        contactQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        Page<ContactVO> page = userContactService.findByPage(contactQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation("联系人申请列表")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/loadContactApply")
    public BaseResponse<Page<ContactApplyVO>> loadContactApply(@RequestBody ContactApplyQuery contactApplyQuery,
                                                               @CurrentUser LoginUser loginUser) {
        contactApplyQuery.setReceiveUserId(loginUser.getUserId());
        contactApplyQuery.setSortField("uca.lastApplyTime");
        contactApplyQuery.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        Page<ContactApplyVO> page = userContactApplyService.findByPage(contactApplyQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation("搜索")
    @GetMapping("/search")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<UserContactVO> search(@CurrentUser LoginUser loginUser, @NotEmpty String userId) {
        Integer contactUserId = UserIdCodec.decode(userId);
        UserContactVO userContactVO = userContactService.search(loginUser.getUserId(), contactUserId);
        return ResultUtil.success(userContactVO);
    }

    @ApiOperation("申请")
    @GetMapping("/apply")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<Integer> apply(@CurrentUser LoginUser loginUser, @NotEmpty String receiveUserId) {
        Integer userId = UserIdCodec.decode(receiveUserId);
        UserContactApply userContactApply = new UserContactApply();
        userContactApply.setApplyUserId(loginUser.getUserId());
        userContactApply.setReceiveUserId(userId);
        userContactApply.setStatus(ContactApplyStatusEnum.INIT.getValue());
        Integer save = userContactApplyService.saveApply(userContactApply);
        return ResultUtil.success(save);
    }

    @ApiOperation("处理申请")
    @PostMapping("/deal")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<Object> deal(@CurrentUser LoginUser loginUser, @NotEmpty String applyUserId, @NotNull Integer status) {
        userContactApplyService.deal(UserIdCodec.decode(applyUserId), loginUser.getUserId(), loginUser.getNickName(), status);
        return ResultUtil.success(null);
    }

    @ApiOperation("删除")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<Object> delete(@CurrentUser LoginUser loginUser, @NotEmpty Integer contactId, @NotNull Integer status) {
        userContactService.deleteContact(loginUser.getUserId(), contactId, status);
        return ResultUtil.success(null);
    }

    @ApiOperation("当前未处理的好友申请数量")
    @PostMapping("/loadContactApplyCount")
    @AuthCheck(mustRole = UserConstant.user)
    public BaseResponse<Long> loadContactApplyCount(@CurrentUser LoginUser loginUser) {
        Long Count = userContactApplyService.loadContactApplyCount(loginUser.getUserId());
        return ResultUtil.success(Count);
    }
}
