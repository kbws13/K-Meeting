package xyz.kbws.aop;

import cn.hutool.core.util.StrUtil;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.kbws.annotation.AuthCheck;
import xyz.kbws.annotation.CurrentUser;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.constant.CommonConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author kbws
 * @date 2026/3/6
 * @description:
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    @Resource
    private RedisComponent redisComponent;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 非 Controller 方法，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 当前接口不需要登录，直接放行
        if (!needLogin(handlerMethod)) {
            return true;
        }

        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        LoginUser loginUser = redisComponent.getLoginUser(token);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录已失效，请重新登录");
        }
        request.setAttribute(CommonConstant.CURRENT_USER, loginUser);
        return true;
    }

    /**
     * 判断当前方法是否需要登录
     */
    private boolean needLogin(HandlerMethod handlerMethod) {
        // 1. 方法上有 @AuthCheck
        if (handlerMethod.hasMethodAnnotation(AuthCheck.class)) {
            return true;
        }

        // 2. 类上有 @AuthCheck
        if (handlerMethod.getBeanType().isAnnotationPresent(AuthCheck.class)) {
            return true;
        }

        // 3. 参数上有 @CurrentUser
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(CurrentUser.class)) {
                return true;
            }
        }

        return false;
    }
}
