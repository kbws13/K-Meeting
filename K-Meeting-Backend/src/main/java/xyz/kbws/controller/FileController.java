package xyz.kbws.controller;

import cn.hutool.core.date.DateUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.annotation.CurrentUser;
import xyz.kbws.config.AppConfig;
import xyz.kbws.constant.FileConstant;
import xyz.kbws.constant.UserConstant;
import xyz.kbws.ffmpeg.FFmpegComponent;
import xyz.kbws.model.enums.FileTypeEnum;
import xyz.kbws.redis.entity.LoginUser;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.Date;

/**
 * @author kbws
 * @date 2026/4/2
 * @description:
 */
@Slf4j
@Api(tags = "文件接口")
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private FFmpegComponent fFmpegComponent;

    @RequestMapping("/getResource")
    @AuthCheck(mustRole = UserConstant.user)
    public void getResource(HttpServletResponse response, @RequestHeader(required = false, name = "range") String range, @NotNull Long messageId, @NotNull Long sendTime, @NotNull Integer fileType, Boolean thumbnail) {
        FileTypeEnum fileTypeEnum = FileTypeEnum.getByValue(fileType);
        thumbnail = thumbnail != null && thumbnail;
        String month = DateUtil.format(new Date(sendTime), "yyyyMM");
        String filePath = appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_FILE + month + "/" + messageId + fileTypeEnum.getSuffix();
        if (fileTypeEnum == FileTypeEnum.IMAGE) {
            response.setHeader("Cache-Control", "max-age=" + 30 * 24 * 60 * 60);
            response.setContentType("image/jpg");
        }
        readFile(response, range, filePath, thumbnail);
    }

    @RequestMapping("/downloadFIle")
    @AuthCheck(mustRole = UserConstant.user)
    public void downloadFIle(HttpServletResponse response, @NotNull Long messageId, @NotNull Long sendTime, @NotEmpty String suffix) throws IOException {
        String month = DateUtil.format(new Date(sendTime), "yyyyMM");
        String filePath = appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_FILE + month + "/" + messageId + suffix;
        File file = new File(filePath);
        response.setContentType("application/x-msdownload; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment");
        response.setContentLengthLong(file.length());
        try (FileInputStream in = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
            byte[] byteData = new byte[1024];
            int len;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        }
    }

    @RequestMapping("/getAvatar")
    @AuthCheck(mustRole = UserConstant.user)
    public void getAvatar(HttpServletResponse response, @CurrentUser LoginUser loginUser, @NotEmpty String userId) {
        String filePath = appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_FILE + FileConstant.FILE_FOLDER_AVATAR_NAME + userId + FileConstant.IMAGE_SUFFIX;
        response.setContentType("image/jpg");
        File file = new File(filePath);
        if (!file.exists()) {
            readLocalFile(response);
        }
        readFile(response, null, filePath, false);
    }

    protected void readFile(HttpServletResponse response, String range, String filePath, Boolean thumbnail) {
        filePath = thumbnail ? fFmpegComponent.getImageThumbnail(filePath) : filePath;
        File file = new File(filePath);
        try (ServletOutputStream out = response.getOutputStream()) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            long contentLength = randomAccessFile.length();
            int start = 0, end = 0;
            if (range != null && range.startsWith("bytes=")) {
                String[] values = range.split("=")[1].split("-");
                start = Integer.parseInt(values[0]);
                if (values.length > 1) {
                    end = Integer.parseInt(values[1]);
                }
            }
            int requestSize = 0;
            if (end != 0 && end > start) {
                requestSize = end - start + 1;
            } else {
                requestSize = Integer.MAX_VALUE;
            }

            byte[] buffer = new byte[4096];
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Last-Modified", new Date().toString());
            if (range == null) {
                response.setHeader("Content-Length", contentLength + "");
            } else {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                long requestStart = 0, requestEnd = 0;
                String[] ranges = range.split("=");
                if (ranges.length > 1) {
                    String[] rangeData = ranges[1].split("-");
                    requestStart = Integer.parseInt(rangeData[0]);
                    if (rangeData.length > 1) {
                        requestEnd = Integer.parseInt(rangeData[1]);
                    }
                }
                long length = 0;
                if (requestEnd > 0) {
                    length = requestEnd - requestStart + 1;
                    response.setHeader("Content-Length", "" + length);
                    response.setHeader("Content-Range", "bytes " + requestStart + "-" + requestEnd + "/" + contentLength);
                } else {
                    length = requestEnd - requestStart;
                    response.setHeader("Content-Length", "" + length);
                    response.setHeader("Content-Range", "bytes " + requestStart + "-" + (contentLength - 1) + "/" + contentLength);
                }
            }
            int needSize = requestSize;
            randomAccessFile.seek(start);
            while (needSize > 0) {
                int len = randomAccessFile.read(buffer);
                if (needSize < buffer.length) {
                    out.write(buffer, 0, needSize);
                } else {
                    out.write(buffer, 0, len);
                    if (len < buffer.length) {
                        break;
                    }
                }
                needSize -= buffer.length;
            }
            randomAccessFile.close();
        } catch (Exception e) {
            log.error("读取文件失败");
        }
    }

    private void readLocalFile(HttpServletResponse response) {
        response.setHeader("Cache-Control", "max-age=" + 30 * 24 * 60 * 60);
        response.setContentType("image/jpg");
        ClassPathResource classPathResource = new ClassPathResource(FileConstant.DEFAULT_AVATAR);
        try (OutputStream out = response.getOutputStream(); InputStream in = classPathResource.getInputStream()) {
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取本地文件异常", e);
        }
    }
}
