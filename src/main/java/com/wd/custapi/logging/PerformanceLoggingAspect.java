package com.wd.custapi.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that measures execution time for all REST controller and service methods.
 *
 * If execution time exceeds SLOW_API_THRESHOLD_MS (1000ms), logs to performance.log:
 *   SLOW_API | /api/payment | UserController.createPayment | 2400ms | traceId=REQ-abc123 | userId=5
 *
 * This is a non-invasive way to detect slow endpoints without modifying any controller code.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger PERF_LOG = LoggerFactory.getLogger(LoggingConstants.PERFORMANCE_LOGGER);

    /**
     * Monitor all public methods in @RestController classes.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object monitorControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureAndLog(joinPoint, "CONTROLLER");
    }

    /**
     * Monitor all public methods in @Service classes.
     * Only logs if execution exceeds SLOW_SERVICE_THRESHOLD_MS to reduce noise.
     */
    @Around("within(@org.springframework.stereotype.Service *) " +
            "&& !within(com.wd.custapi.logging..*)")
    public Object monitorServices(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        if (duration >= LoggingConstants.SLOW_SERVICE_THRESHOLD_MS) {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = sig.getName();

            PERF_LOG.warn("{} | SERVICE | {}.{} | {}ms | traceId={} | userId={}",
                    LoggingConstants.PREFIX_SLOW_API,
                    className, methodName,
                    duration,
                    getTraceId(), getUserId());
        }
        return result;
    }

    private Object measureAndLog(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        long start = System.currentTimeMillis();
        boolean hadException = false;

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            hadException = true;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;

            if (duration >= LoggingConstants.SLOW_API_THRESHOLD_MS) {
                MethodSignature sig = (MethodSignature) joinPoint.getSignature();
                String className  = joinPoint.getTarget().getClass().getSimpleName();
                String methodName = sig.getName();
                String path       = MDC.get(LoggingConstants.MDC_PATH);

                PERF_LOG.warn("{} | {} | {} | {}.{} | {}ms | exception={} | traceId={} | userId={}",
                        LoggingConstants.PREFIX_SLOW_API,
                        layer,
                        path != null ? path : "unknown",
                        className, methodName,
                        duration,
                        hadException,
                        getTraceId(), getUserId());
            }
        }
    }

    private String getTraceId() {
        String v = MDC.get(LoggingConstants.MDC_TRACE_ID);
        return v != null ? v : "NO-TRACE";
    }

    private String getUserId() {
        String v = MDC.get(LoggingConstants.MDC_USER_ID);
        return v != null ? v : "-";
    }
}
