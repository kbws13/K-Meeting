package xyz.kbws.ffmpeg;

import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Component;
import xyz.kbws.constant.FileConstant;
import xyz.kbws.utils.ProcessUtil;

import java.io.File;

/**
 * @author kbws
 * @date 2026/4/2
 * @description:
 */
@Component
public class FFmpegComponent {
    public String transferImageType(File tempFile, String filePath) {
        final String CMD_CREATE_IMAGE_THUMBNAIL = "ffmpeg -i \"%s\" \"%s\"";
        String cmd = String.format(CMD_CREATE_IMAGE_THUMBNAIL, tempFile, filePath);
        ProcessUtil.executeCommand(cmd);
        tempFile.delete();
        return filePath;
    }

    public void transferVideoType(File tempFile, String filePath, String fileSuffix) {
        String codec = getVideoCodec(tempFile.getAbsolutePath());
        if (FileConstant.VIDEO_CODE_HEVC.equals(codec) || !FileConstant.VIDEO_SUFFIX.equalsIgnoreCase(fileSuffix)) {
            convertHevc2Mp4(tempFile.getAbsolutePath(), filePath);
        } else {
            FileUtil.copyFile(tempFile.getAbsolutePath(), filePath);
        }
        tempFile.delete();
    }

    public void createImageThumbnail(String filePath) {
        final String CMD_CREATE_IMAGE_THUMBNAIL = "ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\" -y";
        String thumbnail = getImageThumbnail(filePath);
        String cmd = String.format(CMD_CREATE_IMAGE_THUMBNAIL, filePath, thumbnail);
        ProcessUtil.executeCommand(cmd);
    }

    public void createImageThumbnail(File tempFile, String filePath) {
        final String CMD_CREATE_IMAGE_THUMBNAIL = "ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\" -y";
        String cmd = String.format(CMD_CREATE_IMAGE_THUMBNAIL, tempFile, filePath);
        ProcessUtil.executeCommand(cmd);
        tempFile.delete();
    }

    public String getImageThumbnail(String filePath) {
        return filePath + "_thumbnail" + FileConstant.IMAGE_SUFFIX;
    }

    public String getVideoCodec(String videoFilePath) {
        final String CMD_GET_CODE = "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name \"%s\"";
        String cmd = String.format(CMD_GET_CODE, videoFilePath);
        String result = ProcessUtil.executeCommand(cmd);
        result = result.substring(result.indexOf("=") + 1);
        return result.substring(0, result.indexOf("["));
    }

    public void convertHevc2Mp4(String newFileName, String videoFilePath) {
        String CMD_HEVC_264 = "ffmpeg -i %s -c:v libx264 -crf 20 %s";
        String cmd = String.format(CMD_HEVC_264, newFileName, videoFilePath);
        ProcessUtil.executeCommand(cmd);
    }
}
