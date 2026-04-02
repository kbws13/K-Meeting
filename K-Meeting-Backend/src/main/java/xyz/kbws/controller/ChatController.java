package xyz.kbws.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.annotation.CurrentUser;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.entity.ChatMessage;
import xyz.kbws.model.enums.ReceiveTypeEnum;
import xyz.kbws.model.query.ChatMessageQuery;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.service.ChatMessageService;
import xyz.kbws.utils.UserIdCodec;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * @author kbws
 * @date 2026/3/31
 * @description:
 */
@Api(tags = "聊天接口")
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatMessageService chatMessageService;

    @ApiOperation("分页查询")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/load")
    public BaseResponse<Page<ChatMessage>> load(@RequestBody ChatMessageQuery chatMessageQuery,
                                                @CurrentUser LoginUser loginUser) {
        chatMessageQuery.setCurrentUserId(loginUser.getUserId());
        Page<ChatMessage> page = chatMessageService.findPrivateHistoryByPage(chatMessageQuery);
        return ResultUtil.success(page);
    }

    @ApiOperation("发送消息")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/send")
    public BaseResponse<Boolean> send(@CurrentUser LoginUser loginUser, String message, @NotNull Integer messageType, @NotEmpty String receiveUserId, String fileName, Long fileSize, Integer fileType) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMeetingId(loginUser.getCurrentMeetingId());
        chatMessage.setType(messageType);
        chatMessage.setContent(message);
        chatMessage.setFileSize(fileSize);
        chatMessage.setFileName(fileName);
        chatMessage.setFileType(fileType);
        chatMessage.setSendUserId(loginUser.getUserId());
        chatMessage.setSendUserNickName(loginUser.getNickName());
        if (receiveUserId.equals("all")) {
            chatMessage.setReceiveType(ReceiveTypeEnum.ALL.getValue());
        } else {
            chatMessage.setReceiveType(ReceiveTypeEnum.USER.getValue());
            chatMessage.setReceiveUserId(UserIdCodec.decode(receiveUserId));
        }
        chatMessageService.saveChatMessage(chatMessage);
        return ResultUtil.success(true);
    }

    @ApiOperation("上传聊天文件")
    @AuthCheck(mustRole = UserConstant.user)
    @PostMapping("/uploadFile")
    public BaseResponse<Boolean> uploadFile(@CurrentUser LoginUser loginUser, MultipartFile file, @NotNull Long messageId, @NotEmpty Long sendTime) throws IOException {
        getOwnedMessage(loginUser, messageId);
        chatMessageService.uploadFile(file, loginUser.getCurrentMeetingId(), messageId, sendTime);
        return ResultUtil.success(true);
    }

    private ChatMessage getOwnedMessage(LoginUser loginUser, Long messageId) {
        if (messageId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 不能为空");
        }
        ChatMessage storedMessage = chatMessageService.getById(messageId);
        if (storedMessage == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "聊天消息不存在");
        }
        if (!loginUser.getUserId().equals(storedMessage.getSendUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该聊天消息");
        }
        return storedMessage;
    }
}
