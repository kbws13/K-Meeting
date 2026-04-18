package xyz.kbws.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.PageRequest;
import xyz.kbws.config.AppConfig;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.AppUpdateMapper;
import xyz.kbws.model.entity.AppUpdate;
import xyz.kbws.model.enums.AppUpdateStatusEnum;
import xyz.kbws.model.query.AppUpdateQuery;
import xyz.kbws.model.vo.AppUpdateCheckVO;
import xyz.kbws.service.AppUpdateService;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
* @author housenyao
* @description 针对表【appUpdate(应用更新表)】的数据库操作Service实现
* @createDate 2026-04-06 16:22:25
*/
@Slf4j
@Service
public class AppUpdateServiceImpl extends ServiceImpl<AppUpdateMapper, AppUpdate>
    implements AppUpdateService {

    @Resource
    private AppConfig appConfig;

    @Override
    public Page<AppUpdate> findByPage(AppUpdateQuery appUpdateQuery) {
        if (appUpdateQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数不能为空");
        }
        Page<AppUpdate> page = new Page<>(appUpdateQuery.getCurrent(), appUpdateQuery.getPageSize());
        LambdaQueryWrapper<AppUpdate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(appUpdateQuery.getVersion()), AppUpdate::getVersion, appUpdateQuery.getVersion())
                .eq(appUpdateQuery.getStatus() != null, AppUpdate::getStatus, appUpdateQuery.getStatus())
                .eq(appUpdateQuery.getFileType() != null, AppUpdate::getFileType, appUpdateQuery.getFileType());
        applySort(queryWrapper, appUpdateQuery);
        this.page(page, queryWrapper);
        return page;
    }

    @Override
    public Boolean saveAppUpdate(AppUpdate appUpdate, MultipartFile file) throws IOException {
        AppUpdate storedAppUpdate = loadStoredAppUpdate(appUpdate);
        checkAppUpdate(appUpdate, storedAppUpdate, file);

        if (appUpdate.getStatus() == null) {
            appUpdate.setStatus(AppUpdateStatusEnum.ENABLE.getValue());
        }
        if (!isPackageUpdate(appUpdate)) {
            appUpdate.setOuterLink(StrUtil.trim(appUpdate.getOuterLink()));
        } else {
            appUpdate.setOuterLink("");
        }
        appUpdate.setGrayscaleId(StrUtil.trim(appUpdate.getGrayscaleId()));
        appUpdate.setVersion(StrUtil.trim(appUpdate.getVersion()));
        appUpdate.setUpdateDesc(StrUtil.trim(appUpdate.getUpdateDesc()));

        boolean uploadNewPackage = file != null && !file.isEmpty() && isPackageUpdate(appUpdate);
        File oldPackageFile = storedAppUpdate == null ? null : getPackageFile(storedAppUpdate.getVersion());
        File newPackageFile = isPackageUpdate(appUpdate) ? getPackageFile(appUpdate.getVersion()) : null;

        if (uploadNewPackage && newPackageFile != null) {
            ensurePackageFolder();
            file.transferTo(newPackageFile);
        }

        boolean res = this.saveOrUpdate(appUpdate);
        if (!res) {
            boolean shouldDeleteNewPackage = uploadNewPackage
                    && newPackageFile != null
                    && (storedAppUpdate == null
                    || !isPackageUpdate(storedAppUpdate)
                    || !StrUtil.equals(storedAppUpdate.getVersion(), appUpdate.getVersion()));
            if (shouldDeleteNewPackage && newPackageFile.exists()) {
                // 仅清理本次新增文件，避免保留无引用安装包
                deleteFileQuietly(newPackageFile);
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存更新信息失败");
        }

        cleanupObsoletePackage(storedAppUpdate, appUpdate, oldPackageFile, uploadNewPackage);
        return true;
    }

    @Override
    public Boolean deleteAppUpdate(Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新记录 ID 不能为空");
        }
        AppUpdate appUpdate = this.getById(id);
        if (appUpdate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新记录不存在");
        }
        boolean res = this.removeById(id);
        if (!res) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除更新记录失败");
        }
        if (isPackageUpdate(appUpdate)) {
            deleteFileQuietly(getPackageFile(appUpdate.getVersion()));
        }
        return true;
    }

    @Override
    public AppUpdateCheckVO checkVersion(String version, Integer fileType, String grayscaleId) {
        if (StrUtil.isBlank(version)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本号不能为空");
        }
        LambdaQueryWrapper<AppUpdate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(AppUpdate::getStatus, AppUpdateStatusEnum.ENABLE.getValue())
                .orderByDesc(AppUpdate::getCreateTime);
        List<AppUpdate> appUpdateList = this.list(queryWrapper);

        AppUpdate appUpdate = appUpdateList.stream()
                .filter(item -> matchGrayscale(item.getGrayscaleId(), grayscaleId))
                .filter(item -> compareVersion(item.getVersion(), version) > 0)
                .max((o1, o2) -> {
                    int compareResult = compareVersion(o1.getVersion(), o2.getVersion());
                    if (compareResult != 0) {
                        return compareResult;
                    }
                    if (o1.getCreateTime() == null || o2.getCreateTime() == null) {
                        return 0;
                    }
                    return o1.getCreateTime().compareTo(o2.getCreateTime());
                })
                .orElse(null);

        AppUpdateCheckVO result = new AppUpdateCheckVO();
        result.setHasUpdate(appUpdate != null);
        if (appUpdate == null) {
            return result;
        }
        BeanUtil.copyProperties(appUpdate, result);
        result.setDownloadUrl(buildDownloadUrl(appUpdate));
        return result;
    }

    private AppUpdate loadStoredAppUpdate(AppUpdate appUpdate) {
        if (appUpdate == null) {
            return null;
        }
        if (appUpdate.getId() == null) {
            return null;
        }
        AppUpdate storedAppUpdate = this.getById(appUpdate.getId());
        if (storedAppUpdate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新记录不存在");
        }
        return storedAppUpdate;
    }

    private void checkAppUpdate(AppUpdate appUpdate, AppUpdate storedAppUpdate, MultipartFile file) {
        if (appUpdate == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新信息不能为空");
        }
        if (StrUtil.isBlank(appUpdate.getVersion())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本号不能为空");
        }
        LambdaQueryWrapper<AppUpdate> duplicateQueryWrapper = new LambdaQueryWrapper<>();
        duplicateQueryWrapper.eq(AppUpdate::getVersion, StrUtil.trim(appUpdate.getVersion()))
                .ne(appUpdate.getId() != null, AppUpdate::getId, appUpdate.getId());
        if (this.count(duplicateQueryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本号已存在");
        }
        if (StrUtil.isBlank(appUpdate.getUpdateDesc())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新说明不能为空");
        }
        if (appUpdate.getFileType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不能为空");
        }
        if (appUpdate.getStatus() != null && AppUpdateStatusEnum.getByValue(appUpdate.getStatus()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新状态错误");
        }
        boolean hasFile = file != null && !file.isEmpty();
        if (isPackageUpdate(appUpdate)) {
            if (hasFile) {
                String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "");
                if (!StrUtil.endWithIgnoreCase(originalFilename, CommonConstant.APP_EXE_SUFFIX)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "安装包必须为 exe 文件");
                }
            }
            boolean needUploadFile = storedAppUpdate == null
                    || !isPackageUpdate(storedAppUpdate)
                    || !StrUtil.equals(storedAppUpdate.getVersion(), appUpdate.getVersion());
            if (needUploadFile && !hasFile) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "安装包更新必须上传安装包");
            }
        } else {
            if (hasFile) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "外链更新无需上传安装包");
            }
            if (StrUtil.isBlank(appUpdate.getOuterLink())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "外链更新必须填写下载地址");
            }
        }
    }

    private void applySort(LambdaQueryWrapper<AppUpdate> queryWrapper, PageRequest pageRequest) {
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(pageRequest.getSortOrder());
        String sortField = pageRequest.getSortField();
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(AppUpdate::getCreateTime);
            return;
        }
        switch (sortField) {
            case "id":
                queryWrapper.orderBy(true, isAsc, AppUpdate::getId);
                break;
            case "version":
                queryWrapper.orderBy(true, isAsc, AppUpdate::getVersion);
                break;
            case "status":
                queryWrapper.orderBy(true, isAsc, AppUpdate::getStatus);
                break;
            case "createTime":
                queryWrapper.orderBy(true, isAsc, AppUpdate::getCreateTime);
                break;
            default:
                queryWrapper.orderByDesc(AppUpdate::getCreateTime);
                break;
        }
    }

    private boolean matchGrayscale(String grayscaleIds, String grayscaleId) {
        if (StrUtil.isBlank(grayscaleIds)) {
            return true;
        }
        if (StrUtil.isBlank(grayscaleId)) {
            return false;
        }
        List<String> grayscaleIdList = StrUtil.splitTrim(grayscaleIds, ',');
        return grayscaleIdList.stream().anyMatch(item -> StrUtil.equals(item, grayscaleId));
    }

    private int compareVersion(String sourceVersion, String targetVersion) {
        String[] sourceArray = StrUtil.splitToArray(StrUtil.blankToDefault(sourceVersion, "0"), '.');
        String[] targetArray = StrUtil.splitToArray(StrUtil.blankToDefault(targetVersion, "0"), '.');
        int maxLength = Math.max(sourceArray.length, targetArray.length);
        for (int i = 0; i < maxLength; i++) {
            int source = i < sourceArray.length ? parseVersionPart(sourceArray[i]) : 0;
            int target = i < targetArray.length ? parseVersionPart(targetArray[i]) : 0;
            if (source != target) {
                return Integer.compare(source, target);
            }
        }
        return 0;
    }

    private int parseVersionPart(String versionPart) {
        if (StrUtil.isBlank(versionPart)) {
            return 0;
        }
        String numberPart = versionPart.replaceAll("[^0-9]", "");
        if (StrUtil.isBlank(numberPart)) {
            return 0;
        }
        return Integer.parseInt(numberPart);
    }

    private String buildDownloadUrl(AppUpdate appUpdate) {
        if (StrUtil.isNotBlank(appUpdate.getOuterLink())) {
            return appUpdate.getOuterLink();
        }
        return "/appUpdate/download?id=" + appUpdate.getId();
    }

    private boolean isPackageUpdate(AppUpdate appUpdate) {
        return appUpdate != null && Integer.valueOf(0).equals(appUpdate.getFileType());
    }

    private void ensurePackageFolder() {
        File packageFolder = new File(appConfig.getProjectFolder() + CommonConstant.APP_UPDATE_FOLDER);
        if (!packageFolder.exists() && !packageFolder.mkdirs()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建安装包目录失败");
        }
    }

    private File getPackageFile(String version) {
        String fileName = CommonConstant.APP_NAME + version + CommonConstant.APP_EXE_SUFFIX;
        return new File(appConfig.getProjectFolder() + CommonConstant.APP_UPDATE_FOLDER + fileName);
    }

    private void cleanupObsoletePackage(AppUpdate storedAppUpdate, AppUpdate latestAppUpdate, File oldPackageFile, boolean uploadNewPackage) {
        if (storedAppUpdate == null || !isPackageUpdate(storedAppUpdate) || oldPackageFile == null) {
            return;
        }
        boolean switchedToOuterLink = !isPackageUpdate(latestAppUpdate);
        boolean versionChanged = !StrUtil.equals(storedAppUpdate.getVersion(), latestAppUpdate.getVersion());
        if (switchedToOuterLink || (versionChanged && uploadNewPackage)) {
            deleteFileQuietly(oldPackageFile);
        }
    }

    private void deleteFileQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!file.delete()) {
            log.warn("delete app update package failed, path={}", file.getAbsolutePath());
        }
    }
}
