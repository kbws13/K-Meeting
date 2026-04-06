package xyz.kbws.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.common.BaseResponse;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.ResultUtil;
import xyz.kbws.config.AppConfig;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.entity.AppUpdate;
import xyz.kbws.model.query.AppUpdateQuery;
import xyz.kbws.model.vo.AppUpdateCheckVO;
import xyz.kbws.service.AppUpdateService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author kbws
 * @date 2026/4/6
 * @description:
 */
@Api(tags = "系统更新接口")
@RestController
@RequestMapping("/appUpdate")
public class AppUpdateController {

    @Resource
    private AppUpdateService appUpdateService;

    @Resource
    private AppConfig appConfig;

    @ApiOperation("分页查询更新记录")
    @PostMapping("/load")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Page<AppUpdate>> load(@RequestBody AppUpdateQuery appUpdateQuery) {
        return ResultUtil.success(appUpdateService.findByPage(appUpdateQuery));
    }

    @ApiOperation("保存更新记录")
    @PostMapping("/save")
    @AuthCheck(mustRole = UserConstant.admin)
    public BaseResponse<Boolean> save(@RequestBody AppUpdate appUpdate) {
        return ResultUtil.success(appUpdateService.saveAppUpdate(appUpdate));
    }

    @ApiOperation("检查更新")
    @GetMapping("/checkVersion")
    public BaseResponse<AppUpdateCheckVO> checkVersion(@NotEmpty String version, @NotNull Integer fileType, String grayscaleId) {
        AppUpdateCheckVO checkResult = appUpdateService.checkVersion(version, fileType, grayscaleId);
        return ResultUtil.success(checkResult);
    }

    @ApiOperation("下载安装包")
    @GetMapping("/download")
    public void download(HttpServletResponse response, @NotNull Integer id) throws IOException {
        AppUpdate appUpdate = appUpdateService.getById(id);
        if (appUpdate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新记录不存在");
        }
        if (appUpdate.getOuterLink() != null && !appUpdate.getOuterLink().trim().isEmpty()) {
            response.sendRedirect(appUpdate.getOuterLink());
            return;
        }
        String fileName = CommonConstant.APP_NAME + appUpdate.getVersion() + CommonConstant.APP_EXE_SUFFIX;
        String filePath = appConfig.getProjectFolder() + CommonConstant.APP_UPDATE_FOLDER + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "安装包不存在");
        }
        response.setContentType("application/x-msdownload; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.setContentLengthLong(file.length());
        try (FileInputStream inputStream = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }
}
