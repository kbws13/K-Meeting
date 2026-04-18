package xyz.kbws.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.common.PageRequest;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.entity.AppUpdate;
import xyz.kbws.model.enums.AppUpdateStatusEnum;
import xyz.kbws.model.query.AppUpdateQuery;
import xyz.kbws.model.vo.AppUpdateCheckVO;
import xyz.kbws.service.AppUpdateService;
import xyz.kbws.mapper.AppUpdateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author housenyao
* @description 针对表【appUpdate(应用更新表)】的数据库操作Service实现
* @createDate 2026-04-06 16:22:25
*/
@Service
public class AppUpdateServiceImpl extends ServiceImpl<AppUpdateMapper, AppUpdate>
    implements AppUpdateService {

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
    public Boolean saveAppUpdate(AppUpdate appUpdate) {
        checkAppUpdate(appUpdate);
        if (appUpdate.getStatus() == null) {
            appUpdate.setStatus(AppUpdateStatusEnum.ENABLE.getValue());
        }
        boolean res = this.saveOrUpdate(appUpdate);
        if (!res) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存更新信息失败");
        }
        return true;
    }

    @Override
    public AppUpdateCheckVO checkVersion(String version, Integer fileType, String grayscaleId) {
        if (StrUtil.isBlank(version)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本号不能为空");
        }
        //if (fileType == null) {
        //    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不能为空");
        //}
        LambdaQueryWrapper<AppUpdate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                //.eq(AppUpdate::getFileType, fileType)
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

    private void checkAppUpdate(AppUpdate appUpdate) {
        if (appUpdate == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新信息不能为空");
        }
        if (StrUtil.isBlank(appUpdate.getVersion())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本号不能为空");
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
}




