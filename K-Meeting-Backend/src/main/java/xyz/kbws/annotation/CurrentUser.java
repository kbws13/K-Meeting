package xyz.kbws.annotation;

import java.lang.annotation.*;

/**
 * @author kbws
 * @date 2026/3/6
 * @description:
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
