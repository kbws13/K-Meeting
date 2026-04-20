package xyz.kbws.model.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum FileTypeEnum {
    IMAGE(0, new String[]{"jpeg", "jpg", "png", "gif", "bmp", "webp"}, ".jpg", "图片"),
    VIDEO(1, new String[]{"mp4", "avi", "rmvb", "mkv", "mov"}, ".mp4", "视频"),
    ;

    private Integer value;
    private String[] suffixArray;
    private String suffix;
    private String desc;

    FileTypeEnum(Integer value, String[] suffixArray, String suffix, String desc) {
        this.value = value;
        this.suffixArray = suffixArray;
        this.suffix = suffix;
        this.desc = desc;
    }

    public static FileTypeEnum getBySuffix(String value) {
        for (FileTypeEnum anEnum : FileTypeEnum.values()) {
            if (Arrays.asList(anEnum.suffixArray).contains(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public static FileTypeEnum getByValue(Integer value) {
        for (FileTypeEnum anEnum : FileTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
