package xyz.kbws.utils;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * @author kbws
 * @date 2025/6/27
 * @description:
 */
public class TraceIdUtil {
    public static final String TRACE_ID = "trace-id";

    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        return traceId == null ? "" : traceId;
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void remove() {
        MDC.remove(TRACE_ID);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
