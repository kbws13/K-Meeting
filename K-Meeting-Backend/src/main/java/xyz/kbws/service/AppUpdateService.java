package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import xyz.kbws.model.entity.AppUpdate;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kbws.model.query.AppUpdateQuery;
import xyz.kbws.model.vo.AppUpdateCheckVO;

/**
* @author housenyao
* @description 针对表【appUpdate(应用更新表)】的数据库操作Service
* @createDate 2026-04-06 16:22:25
*/
public interface AppUpdateService extends IService<AppUpdate> {

    Page<AppUpdate> findByPage(AppUpdateQuery appUpdateQuery);

    Boolean saveAppUpdate(AppUpdate appUpdate);

    AppUpdateCheckVO checkVersion(String version, Integer fileType, String grayscaleId);
}
